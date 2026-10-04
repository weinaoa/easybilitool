/*
Copyright (c) 2026 weinaoa
EasyBiliTool is licensed under Mulan PubL v2.
You can use this software according to the terms and conditions of the Mulan PubL v2.
You may obtain a copy of Mulan PubL v2 at:
    http://license.coscl.org.cn/MulanPubL-2.0
THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
See the Mulan PubL v2 for more details.
*/

package com.weinaoa.easybilitool;

import android.app.*;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.*;
import android.view.*;
import java.util.*;
import java.util.function.Supplier;

/** Detail list scrolling drives the native toolbar. */
final class DetailBottomBarHook {
    private static final Map<View,Float> fractions=new WeakHashMap<>(),translations=new WeakHashMap<>();
    private static final String OPUS_SCROLL="com.bilibili.bplus.followinglist.page.opus.OpusNestedScrollParent";
    static float collapseFraction(View bar){return fractions.getOrDefault(bar,0f);}
    static boolean managed(View bar){return fractions.containsKey(bar);}
    static float translation(View bar){return translations.getOrDefault(bar,0f);}
    private final Supplier<Config> settings;
    private final Map<Activity,State> states=new WeakHashMap<>();
    DetailBottomBarHook(Supplier<Config> settings){this.settings=settings;}
    void install(Application application)throws Exception {
        ClassLoader loader=application.getClassLoader();
        Reflector.findAndHookMethod(Activity.class,"dispatchTouchEvent",MotionEvent.class,new MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){State s=states.get(p.thisObject);if(s!=null)s.touch((MotionEvent)p.args[0]);}
        });
        try{Reflector.findAndHookMethod(Class.forName("androidx.recyclerview.widget.RecyclerView",false,loader),"dispatchOnScrolled",int.class,int.class,new MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam p){scrolled((View)p.thisObject,(Integer)p.args[1]);}
        });}catch(ReflectiveOperationException unavailable){HookRuntime.log("[EasyBiliTool] DETAIL_RECYCLER_SCROLL_UNAVAILABLE");}
        for(String type:new String[]{"android.widget.ScrollView","androidx.core.widget.NestedScrollView"}){
            try{Reflector.findAndHookMethod(Class.forName(type,false,loader),"onScrollChanged",int.class,int.class,int.class,int.class,new MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam p){scrolled((View)p.thisObject,(Integer)p.args[1]-(Integer)p.args[3]);}
            });}catch(ReflectiveOperationException unavailable){HookRuntime.log("[EasyBiliTool] DETAIL_SCROLL_UNAVAILABLE "+type);}
        }
        // 8.95 Opus scrolls a LinearLayout via View.scrollTo, not a NestedScrollView.
        // Observe its completed, clamped position; child recycler deltas remain separate.
        Reflector.findAndHookMethod(View.class,"onScrollChanged",int.class,int.class,int.class,int.class,new MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam p){if(p.thisObject.getClass().getName().equals(OPUS_SCROLL))scrolled((View)p.thisObject,(Integer)p.args[1]-(Integer)p.args[3]);}
        });
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks(){
            public void onActivityCreated(Activity a,Bundle b){if(a.getClass().getName().equals("tv.danmaku.bili.MainActivityV2"))return;State s=new State(a);states.put(a,s);s.decor.getViewTreeObserver().addOnPreDrawListener(s.draw);}
            public void onActivityResumed(Activity a){State s=states.get(a);if(s!=null){s.resumed=true;s.handler.removeCallbacks(s.tick);s.handler.post(s.tick);}}
            public void onActivityPaused(Activity a){State s=states.get(a);if(s!=null){s.resumed=false;s.handler.removeCallbacks(s.tick);s.restore();}}
            public void onActivityDestroyed(Activity a){State s=states.remove(a);if(s!=null){s.handler.removeCallbacks(s.tick);s.restore();if(s.decor.getViewTreeObserver().isAlive())s.decor.getViewTreeObserver().removeOnPreDrawListener(s.draw);}}
            public void onActivityStarted(Activity a){}public void onActivityStopped(Activity a){}public void onActivitySaveInstanceState(Activity a,Bundle b){}
        });
    }
    private void scrolled(View view,int dy){
        if(dy==0||!settings.get().b("HOME_HIDE_BOTTOM_BAR"))return;
        for(State s:states.values())if(view.getRootView()==s.decor){s.scrolled(view,dy);break;}
    }
    private final class State {
        final Activity activity;final View decor;final Handler handler=new Handler(Looper.getMainLooper());
        final DetailBarScroll scroll=new DetailBarScroll();
        final int opusId,commentsId;
        final ViewTreeObserver.OnPreDrawListener draw=()->{update(false);return true;};
        final Runnable tick=new Runnable(){public void run(){update(true);if(resumed)handler.postDelayed(this,100);}};
        ViewGroup bar;View surface;
        boolean resumed,changing,applied;long checked;
        float originalTranslation,lastTranslation,originalAlpha,lastAlpha;int originalAccessibility;
        State(Activity activity){this.activity=activity;decor=activity.getWindow().getDecorView();opusId=id("opus_nested_scroll");commentsId=id("cmt3_recycler");}
        int id(String name){return activity.getResources().getIdentifier(name,"id","tv.danmaku.bili");}
        boolean eligible(){
            if(!resumed||!settings.get().b("HOME_HIDE_BOTTOM_BAR")||!decor.hasWindowFocus()||activity.isFinishing()||activity.isDestroyed()
                ||activity.getResources().getConfiguration().orientation!=Configuration.ORIENTATION_PORTRAIT||activity.isInMultiWindowMode()||activity.isInPictureInPictureMode())return false;
            WindowInsets insets=decor.getRootWindowInsets();
            if(insets==null)return true;
            if(Build.VERSION.SDK_INT>=30)return !insets.isVisible(WindowInsets.Type.ime());
            return insets.getSystemWindowInsetBottom()<=insets.getStableInsetBottom()+80*activity.getResources().getDisplayMetrics().density;
        }
        View visibleContent(View view,int id){
            if(id==0||!view.isShown())return null;
            if(view.getId()==id)return view;
            if(view instanceof ViewGroup group)for(int i=group.getChildCount()-1;i>=0;i--){View found=visibleContent(group.getChildAt(i),id);if(found!=null)return found;}
            return null;
        }
        void update(boolean force){
            if(changing)return;
            if(!eligible()){restore();return;}
            long now=SystemClock.uptimeMillis();if(!force&&now-checked<100)return;
            changing=true;checked=now;
            try{
                ViewGroup next=DetailBarLocator.findBar(activity,decor);
                if(next!=bar){restore();bar=next;}
                if(bar==null)return;
                boolean video=bar.getId()==id("cmt3_fake_input_bar_container");
                View nextSurface=visibleContent(decor,video?commentsId:opusId);
                if(nextSurface==null&&!video)nextSurface=visibleContent(decor,commentsId);
                if(surface!=nextSurface){restoreNative();surface=nextSurface;scroll.bind(surface);}
                if(surface==null){restore();return;}
                apply();
            }catch(Throwable error){restore();HookRuntime.log("[EasyBiliTool] DETAIL_BOTTOM_BAR_BYPASS "+error.getClass().getSimpleName());}
            finally{changing=false;}
        }
        void touch(MotionEvent event){
            int action=event.getActionMasked();if(action!=MotionEvent.ACTION_DOWN&&action!=MotionEvent.ACTION_MOVE)return;
            update(action==MotionEvent.ACTION_DOWN);
            if(surface==null||!eligible())return;
            Rect bounds=new Rect();if(surface.getGlobalVisibleRect(bounds)&&bounds.contains((int)event.getRawX(),(int)event.getRawY()))scroll.touch(SystemClock.uptimeMillis());
        }
        boolean belongs(View view){for(View current=view;current!=null;){if(current==surface)return true;ViewParent parent=current.getParent();current=parent instanceof View next?next:null;}return false;}
        void scrolled(View view,int dy){
            update(false);
            if(bar==null||surface==null||!view.isShown()||!belongs(view)||!eligible())return;
            boolean atTop=!view.canScrollVertically(-1)&&!surface.canScrollVertically(-1);
            scroll.scrolled(dy,bar.getHeight(),atTop,SystemClock.uptimeMillis());apply();
        }
        void apply(){
            if(bar==null)return;
            float fraction=scroll.fraction();
            {
                if(!applied){originalTranslation=bar.getTranslationY();originalAlpha=bar.getAlpha();originalAccessibility=bar.getImportantForAccessibility();applied=true;}
                else{if(Math.abs(bar.getTranslationY()-lastTranslation)>.5f)originalTranslation=bar.getTranslationY();if(Math.abs(bar.getAlpha()-lastAlpha)>.0001f)originalAlpha=bar.getAlpha();}
                float distance=fraction*bar.getHeight();lastTranslation=originalTranslation+distance;lastAlpha=originalAlpha*HomeBarSync.opacity(fraction);
                if(Math.abs(bar.getTranslationY()-lastTranslation)>.01f)bar.setTranslationY(lastTranslation);
                if(Math.abs(bar.getAlpha()-lastAlpha)>.0001f)bar.setAlpha(lastAlpha);
                translations.put(bar,distance);
                int accessibility=fraction>=.999f?View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS:originalAccessibility;
                if(bar.getImportantForAccessibility()!=accessibility)bar.setImportantForAccessibility(accessibility);
            }
            fractions.put(bar,fraction);
        }
        void restoreNative(){
            if(bar!=null)translations.remove(bar);
            if(!applied||bar==null)return;
            if(Math.abs(bar.getTranslationY()-lastTranslation)<.5f)bar.setTranslationY(originalTranslation);
            if(Math.abs(bar.getAlpha()-lastAlpha)<.0001f)bar.setAlpha(originalAlpha);
            if(bar.getImportantForAccessibility()==View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS)bar.setImportantForAccessibility(originalAccessibility);
            applied=false;
        }
        void restore(){restoreNative();if(bar!=null)fractions.remove(bar);scroll.bind(null);surface=null;bar=null;}
    }
}
