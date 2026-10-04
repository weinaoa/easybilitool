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
import android.os.*;
import android.view.*;
import android.widget.TextView;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Supplier;

/** Home follows its AppBar; other main tabs follow actual vertical scrolling. */
final class HomeBottomBarHook {
    private static final String APP_BAR="tv.danmaku.bili.ui.main2.widget.HomeAppBarLayout";
    private static final String TAB_HOST="com.bilibili.lib.homepage.widget.TabHost";
    private final Supplier<Config> settings;
    private final Map<Activity,State> activities=new WeakHashMap<>();
    private Class<?> listenerType;
    private Method addListener,removeListener,scrollRange;
    private Field currentOffset;

    HomeBottomBarHook(Supplier<Config> settings){this.settings=settings;}
    void install(Application application)throws Exception {
        ClassLoader loader=application.getClassLoader();
        Class<?> nativeBar=Class.forName("com.google.android.material.appbar.AppBarLayout",false,loader);
        Class.forName(APP_BAR,false,loader);Class.forName(TAB_HOST,false,loader);
        listenerType=Class.forName("com.google.android.material.appbar.AppBarLayout$OnOffsetChangedListener",false,loader);
        addListener=nativeBar.getMethod("addOnOffsetChangedListener",listenerType);
        removeListener=nativeBar.getMethod("removeOnOffsetChangedListener",listenerType);
        scrollRange=nativeBar.getMethod("getTotalScrollRange");
        currentOffset=Reflector.findField(nativeBar,"currentOffset");
        Reflector.findAndHookMethod(Activity.class,"dispatchTouchEvent",MotionEvent.class,new MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){State state=activities.get(p.thisObject);if(state!=null)state.touch((MotionEvent)p.args[0]);}
        });
        // Observe completed scroll dispatches without replacing the host listeners.
        Class<?> recycler=Class.forName("androidx.recyclerview.widget.RecyclerView",false,loader);
        Reflector.findAndHookMethod(recycler,"dispatchOnScrolled",int.class,int.class,new MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam p){scrolled((View)p.thisObject,(Integer)p.args[1]);}
        });
        for(String type:new String[]{"android.widget.ScrollView","android.webkit.WebView","androidx.core.widget.NestedScrollView"}){
            try{Reflector.findAndHookMethod(Class.forName(type,false,loader),"onScrollChanged",int.class,int.class,int.class,int.class,new MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam p){scrolled((View)p.thisObject,(Integer)p.args[1]-(Integer)p.args[3]);}
            });}catch(ReflectiveOperationException unsupported){HookRuntime.log("[EasyBiliTool] MAIN_SCROLL_UNAVAILABLE "+type);}
        }
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks(){
            public void onActivityCreated(Activity a,Bundle b){
                if(!a.getClass().getName().equals("tv.danmaku.bili.MainActivityV2"))return;
                State s=new State(a);activities.put(a,s);
                s.decor.getViewTreeObserver().addOnGlobalLayoutListener(s.layout);
                s.decor.getViewTreeObserver().addOnPreDrawListener(s.preDraw);
            }
            public void onActivityResumed(Activity a){State s=activities.get(a);if(s!=null){s.resumed=true;s.handler.removeCallbacks(s.tick);s.handler.post(s.tick);}}
            public void onActivityPaused(Activity a){State s=activities.get(a);if(s!=null){s.resumed=false;s.handler.removeCallbacks(s.tick);s.restore();}}
            public void onActivityDestroyed(Activity a){State s=activities.remove(a);if(s!=null)s.destroy();}
            public void onActivityStarted(Activity a){}public void onActivityStopped(Activity a){}public void onActivitySaveInstanceState(Activity a,Bundle b){}
        });
        HookRuntime.log("[EasyBiliTool] HOME_BOTTOM_BAR_READY 8.95 native offset sync");
    }

    void scrolled(View view,int dy){
        if(dy==0||!settings.get().b("HOME_HIDE_BOTTOM_BAR"))return;
        for(State state:activities.values())if(view.getRootView()==state.decor){state.scrolled(view,dy);break;}
    }

    private final class State {
        final Activity activity;final View decor;
        final int barId,bottomId,labelId;
        final Handler handler=new Handler(Looper.getMainLooper());
        final ViewTreeObserver.OnGlobalLayoutListener layout=this::update;
        final ViewTreeObserver.OnPreDrawListener preDraw=()->{update();return true;};
        final Runnable tick=new Runnable(){public void run(){update();if(resumed)handler.postDelayed(this,500);}};
        View appBar,bottom;Object listener;
        boolean resumed,updating,failed,applied,opacityApplied;
        float originalTranslation,lastTranslation,originalAlpha,lastAlpha;
        int originalAccessibility;
        String page="";final MainTabBarScroll scroll=new MainTabBarScroll();long scrollUntil;
        State(Activity a){
            activity=a;decor=a.getWindow().getDecorView();
            barId=id("app_bar");bottomId=id("bottom_navigation");labelId=id("tab_text");
        }
        int id(String name){return activity.getResources().getIdentifier(name,"id","tv.danmaku.bili");}
        void bind()throws Exception {
            if(bottom!=null&&bottom.isAttachedToWindow()){
                if(appBar!=null&&appBar.isAttachedToWindow())return;
                if(appBar==null&&(barId==0||decor.findViewById(barId)==null))return;
            }
            detach();
            View bar=barId==0?null:decor.findViewById(barId),nav=bottomId==0?null:decor.findViewById(bottomId);
            if(nav==null||!nav.getClass().getName().equals(TAB_HOST))return;
            bottom=nav;if(bar==null||!bar.getClass().getName().equals(APP_BAR))return;appBar=bar;
            listener=Proxy.newProxyInstance(listenerType.getClassLoader(),new Class<?>[]{listenerType},(proxy,method,args)->{
                if(method.getName().equals("onOffsetChanged")){update();return null;}
                if(method.getName().equals("equals"))return proxy==args[0];
                if(method.getName().equals("hashCode"))return System.identityHashCode(proxy);
                if(method.getName().equals("toString"))return "EasyBiliToolHomeOffsetListener";
                return null;
            });
            addListener.invoke(appBar,listener);
        }
        String selectedPage(View view){
            if(view.getId()==labelId&&view instanceof TextView label&&view.isSelected())return label.getText().toString();
            if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){String found=selectedPage(group.getChildAt(i));if(!found.isEmpty())return found;}
            return "";
        }
        void touch(MotionEvent event){if(event.getActionMasked()==MotionEvent.ACTION_DOWN||event.getActionMasked()==MotionEvent.ACTION_MOVE)scrollUntil=SystemClock.uptimeMillis()+1500;}
        void scrolled(View view,int dy){
            if(!resumed||!view.isShown()||view.getWidth()<decor.getWidth()*.6||view.getHeight()<decor.getHeight()*.25)return;
            update();
            if(bottom==null||!applied||!MainTabBarScroll.supports(page)||SystemClock.uptimeMillis()>scrollUntil)return;
            // Keep following a fling, while ignoring unrelated initial layout,
            // refresh and restored scroll positions after a page switch.
            scrollUntil=SystemClock.uptimeMillis()+1500;
            float fraction=scroll.scrolled(dy,bottom.getHeight(),!view.canScrollVertically(-1));
            apply(fraction,bottom.getHeight());
        }
        void update(){
            if(updating||failed)return;
            updating=true;
            try{
                if(!resumed||!settings.get().b("HOME_HIDE_BOTTOM_BAR")){restore();return;}
                bind();
                if(bottom==null||!bottom.isShown()
                    ||activity.getResources().getConfiguration().orientation!=Configuration.ORIENTATION_PORTRAIT
                    ||activity.isInMultiWindowMode()||activity.isInPictureInPictureMode()){restore();return;}
                String selected=selectedPage(bottom);
                if(!page.equals(selected)){restore();page=selected;scrollUntil=0;}
                int height=bottom.getHeight();if(height<=0){restore();return;}
                float fraction;
                if(page.equals("首页")&&appBar!=null&&appBar.isShown()){
                    int range=(Integer)scrollRange.invoke(appBar);fraction=HomeBarSync.fraction(currentOffset.getInt(appBar),range);
                }else if(MainTabBarScroll.supports(page))fraction=scroll.fraction();
                else{restore();return;}
                apply(fraction,height);
            }catch(Throwable e){
                restore();failed=true;
                HookRuntime.log("[EasyBiliTool] HOME_BOTTOM_BAR_BYPASS: "+e.getClass().getSimpleName());
            }finally{updating=false;}
        }
        void apply(float fraction,int height){
                float translation=fraction*height;
                if(!applied){originalTranslation=bottom.getTranslationY();originalAccessibility=bottom.getImportantForAccessibility();applied=true;}
                else if(Math.abs(bottom.getTranslationY()-lastTranslation)>.5f)originalTranslation=bottom.getTranslationY();
                lastTranslation=originalTranslation+translation;
                if(Math.abs(bottom.getTranslationY()-lastTranslation)>.01f)bottom.setTranslationY(lastTranslation);
                {if(!opacityApplied){originalAlpha=bottom.getAlpha();opacityApplied=true;}else if(Math.abs(bottom.getAlpha()-lastAlpha)>.0001f)originalAlpha=bottom.getAlpha();lastAlpha=originalAlpha*HomeBarSync.opacity(fraction);if(Math.abs(bottom.getAlpha()-lastAlpha)>.0001f)bottom.setAlpha(lastAlpha);}
                int accessibility=translation>=height-.01f?View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS:originalAccessibility;
                if(bottom.getImportantForAccessibility()!=accessibility)bottom.setImportantForAccessibility(accessibility);
        }
        void restoreOpacity(){if(opacityApplied&&bottom!=null){if(Math.abs(bottom.getAlpha()-lastAlpha)<.0001f)bottom.setAlpha(originalAlpha);opacityApplied=false;}}
        void restore(){
            scroll.reset();
            restoreOpacity();
            if(!applied||bottom==null)return;
            if(Math.abs(bottom.getTranslationY()-lastTranslation)<.5f)bottom.setTranslationY(originalTranslation);
            if(bottom.getImportantForAccessibility()==View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS)bottom.setImportantForAccessibility(originalAccessibility);
            applied=false;
        }
        void detach(){
            restore();
            if(appBar!=null&&listener!=null)try{removeListener.invoke(appBar,listener);}catch(Exception ignored){}
            appBar=null;bottom=null;listener=null;
        }
        void destroy(){
            resumed=false;handler.removeCallbacks(tick);detach();
            if(decor.getViewTreeObserver().isAlive()){
                decor.getViewTreeObserver().removeOnGlobalLayoutListener(layout);
                decor.getViewTreeObserver().removeOnPreDrawListener(preDraw);
            }
        }
    }
}
