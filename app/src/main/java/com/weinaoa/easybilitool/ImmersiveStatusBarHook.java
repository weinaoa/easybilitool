package com.weinaoa.easybilitool;

import android.app.*;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import java.util.*;
import java.util.function.Supplier;

/** Transparent status bar in the verified 8.95 video and live-room windows. */
final class ImmersiveStatusBarHook {
    private static final int MENU_OFFSET_PX=80;
    private static final int UI_MASK=View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
    private static final int FLAGS_MASK=WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS|WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
    private final Supplier<Config> settings;
    private final Map<Activity,State> activities=new WeakHashMap<>();
    private final Map<View,State> decors=new WeakHashMap<>();
    private final Map<Window,State> windows=new WeakHashMap<>();
    private final Map<WindowInsetsController,State> controllers=new WeakHashMap<>();
    ImmersiveStatusBarHook(Supplier<Config> settings){this.settings=settings;}
    void install(Application application)throws Exception {
        Reflector.findAndHookMethod(View.class,"dispatchApplyWindowInsets",WindowInsets.class,new MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){
                State state=decors.get(p.thisObject);if(state==null||!state.eligible())return;
                WindowInsets insets=(WindowInsets)p.args[0];
                // Keep the real safe area for controls before removing it from the video layout.
                int top=Build.VERSION.SDK_INT>=30?insets.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()|WindowInsets.Type.displayCutout()).top:insets.getSystemWindowInsetTop();
                if(top>0)state.statusHeight=top;
                if(Build.VERSION.SDK_INT>=30){
                    p.args[0]=new WindowInsets.Builder(insets).setInsets(WindowInsets.Type.statusBars(),Insets.NONE)
                        .setInsetsIgnoringVisibility(WindowInsets.Type.statusBars(),Insets.NONE).setDisplayCutout(null).build();
                }else{
                    p.args[0]=insets.replaceSystemWindowInsets(new Rect(insets.getSystemWindowInsetLeft(),0,insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom()));
                }
            }
        });
        Reflector.findAndHookMethod(View.class,"setSystemUiVisibility",int.class,new MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){
                State state=decors.get(p.thisObject);if(state==null||state.changing||!state.applied)return;
                // A host may read our layout flags back and then change icon tint.
                // Retain the pre-feature layout bits so disabling really restores them.
                state.originalUi=(state.originalUi&~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)|((Integer)p.args[0]&View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                if(state.eligible())p.args[0]=((Integer)p.args[0]|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE)&~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
        });
        Reflector.findAndHookMethod(Class.forName("com.android.internal.policy.PhoneWindow"),"setStatusBarColor",int.class,new MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){
                State state=windows.get(p.thisObject);if(state==null||state.changing||!state.applied)return;
                state.originalColor=(Integer)p.args[0];if(state.eligible())p.args[0]=Color.TRANSPARENT;
            }
        });
        if(Build.VERSION.SDK_INT>=30)Reflector.findAndHookMethod(Class.forName("android.view.InsetsController"),"setSystemBarsAppearance",int.class,int.class,new MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){
                State state=controllers.get(p.thisObject);if(state==null||state.changing||!state.applied)return;
                int value=(Integer)p.args[0],mask=(Integer)p.args[1];state.appearance=(state.appearance&~mask)|(value&mask);
                if(state.eligible())p.args[0]=value&~WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS;
            }
        });
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks(){
            public void onActivityCreated(Activity a,Bundle b){
                String name=a.getClass().getName();
                if(!name.equals("com.bilibili.ship.theseus.detail.UnitedBizDetailsActivity")
                    &&!name.equals("com.bilibili.ship.theseus.playlist.UnitedPlaylistActivity")
                    &&!name.equals("com.bilibili.bililive.room.ui.roomv3.LiveRoomActivityV3"))return;
                State state=new State(a);activities.put(a,state);decors.put(state.decor,state);windows.put(state.window,state);
                state.decor.getViewTreeObserver().addOnGlobalLayoutListener(state.layout);
                state.decor.getViewTreeObserver().addOnPreDrawListener(state.preDraw);
            }
            public void onActivityResumed(Activity a){State s=activities.get(a);if(s!=null){s.resumed=true;s.update();s.handler.post(s.tick);}}
            public void onActivityPaused(Activity a){State s=activities.get(a);if(s!=null){s.resumed=false;s.handler.removeCallbacks(s.tick);s.restore();}}
            public void onActivityDestroyed(Activity a){State s=activities.remove(a);if(s!=null){s.handler.removeCallbacks(s.tick);if(s.decor.getViewTreeObserver().isAlive()){s.decor.getViewTreeObserver().removeOnGlobalLayoutListener(s.layout);s.decor.getViewTreeObserver().removeOnPreDrawListener(s.preDraw);}decors.remove(s.decor);windows.remove(s.window);controllers.values().removeIf(value->value==s);}}
            public void onActivityStarted(Activity a){} public void onActivityStopped(Activity a){} public void onActivitySaveInstanceState(Activity a,Bundle b){}
        });
        HookRuntime.log("[EasyBiliTool] IMMERSIVE_STATUS_READY 8.95 video and live");
    }
    private final class State {
        final Activity activity;final Window window;final View decor;final Handler handler=new Handler(Looper.getMainLooper());
        boolean resumed,applied,changing,contrast;int originalColor,originalUi,originalFlags,fitTypes,cutout,appearance;
        final GradientDrawable scrim=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x88000000,0x00000000});
        int statusHeight;
        final boolean live;
        final int topMenuId,liveHeaderId,liveOperationsId,liveSecondLineId,liveBusinessId;
        final Map<View,MenuInset> menus=new WeakHashMap<>();
        final ViewTreeObserver.OnGlobalLayoutListener layout=this::update;
        final ViewTreeObserver.OnPreDrawListener preDraw=()->{update();return !adjustMenu();};
        final Runnable tick=new Runnable(){public void run(){if(!resumed)return;update();handler.postDelayed(this,1000);}};
        State(Activity activity){this.activity=activity;window=activity.getWindow();decor=window.getDecorView();
            live=activity.getClass().getName().equals("com.bilibili.bililive.room.ui.roomv3.LiveRoomActivityV3");
            topMenuId=id("nav_top_bar");liveHeaderId=id("native_top_container");liveOperationsId=id("top_operation_container");
            liveSecondLineId=id("ll_lynx_top_left_second_line");liveBusinessId=id("business_container");}
        int id(String name){return activity.getResources().getIdentifier(name,"id",activity.getPackageName());}
        boolean eligible(){return resumed&&settings.get().b("IMMERSIVE_STATUS_BAR")&&!activity.isFinishing()&&!activity.isDestroyed()
            &&activity.getResources().getConfiguration().orientation==Configuration.ORIENTATION_PORTRAIT
            &&!activity.isInPictureInPictureMode()&&!activity.isInMultiWindowMode()
            &&(window.getAttributes().flags&WindowManager.LayoutParams.FLAG_FULLSCREEN)==0;}
        void update(){
            if(changing)return;
            try{if(!eligible()){restore();return;}
                changing=true;
                if(!applied){
                    originalColor=window.getStatusBarColor();originalUi=decor.getSystemUiVisibility();originalFlags=window.getAttributes().flags;
                    if(Build.VERSION.SDK_INT>=29)contrast=window.isStatusBarContrastEnforced();
                    if(Build.VERSION.SDK_INT>=30){fitTypes=window.getAttributes().getFitInsetsTypes();cutout=window.getAttributes().layoutInDisplayCutoutMode;WindowInsetsController c=window.getInsetsController();appearance=c==null?0:c.getSystemBarsAppearance();}
                    applied=true;decor.getOverlay().add(scrim);
                    if(statusHeight<=0){int id=activity.getResources().getIdentifier("status_bar_height","dimen","android");statusHeight=id==0?Math.round(24*activity.getResources().getDisplayMetrics().density):activity.getResources().getDimensionPixelSize(id);}
                    decor.requestApplyInsets();
                }
                int ui=(decor.getSystemUiVisibility()|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE)&~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if(decor.getSystemUiVisibility()!=ui)decor.setSystemUiVisibility(ui);
                int flags=window.getAttributes().flags;
                if((flags&FLAGS_MASK)!=WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)window.setFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,FLAGS_MASK);
                if(window.getStatusBarColor()!=Color.TRANSPARENT)window.setStatusBarColor(Color.TRANSPARENT);
                if(Build.VERSION.SDK_INT>=29&&window.isStatusBarContrastEnforced())window.setStatusBarContrastEnforced(false);
                if(Build.VERSION.SDK_INT>=30){
                    WindowManager.LayoutParams p=window.getAttributes();int types=p.getFitInsetsTypes()&~WindowInsets.Type.statusBars();
                    if(types!=p.getFitInsetsTypes()||p.layoutInDisplayCutoutMode!=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS){p.setFitInsetsTypes(types);p.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;window.setAttributes(p);}
                    WindowInsetsController c=window.getInsetsController();if(c!=null){controllers.put(c,this);if((c.getSystemBarsAppearance()&WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)!=0)c.setSystemBarsAppearance(0,WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);}
                }
                scrim.setBounds(0,0,decor.getWidth(),statusHeight+Math.round(12*activity.getResources().getDisplayMetrics().density));
                if(!decor.isLaidOut())decor.requestApplyInsets();
            }catch(Throwable e){changing=false;restore();HookRuntime.log("[EasyBiliTool] IMMERSIVE_STATUS_BYPASS "+e.getClass().getSimpleName());}
            finally{changing=false;}
        }
        boolean adjustMenu(){
            if(!applied||!eligible())return false;
            if(live){
                // Extend the room background, while keeping its native top
                // control layers below the real status/cutout safe area.
                boolean changed=offsetMenu(liveHeaderId==0?null:decor.findViewById(liveHeaderId),statusHeight);
                changed=offsetMenu(liveSecondaryHeader(),statusHeight)||changed;
                return offsetMenu(liveOperationsId==0?null:decor.findViewById(liveOperationsId),statusHeight)||changed;
            }
            if(topMenuId==0)return false;
            View menu=decor.findViewById(topMenuId);
            // 8.95 uses a Theseus toolbar above the Gemini player controller.
            // Reserve its safe area without moving the video or bottom controls.
            if(menu==null||!menu.getClass().getName().equals("com.bilibili.ship.theseus.united.widget.TouchAwareToolbar")
                ||!(menu.getLayoutParams() instanceof ViewGroup.MarginLayoutParams p))return false;
            return offsetMenu(menu,MENU_OFFSET_PX);
        }
        View liveSecondaryHeader(){
            View line=liveSecondLineId==0?null:decor.findViewById(liveSecondLineId);
            if(line==null||liveBusinessId==0)return null;
            // The Lynx second row belongs to a separate header layer. Move that
            // whole layer so its own clipping bounds still contain the row.
            for(View current=line;current!=decor;){
                if(current.getId()==liveHeaderId)return null;
                if(!(current.getParent() instanceof View parent))return null;
                if(parent.getId()==liveBusinessId)return current;
                current=parent;
            }
            return null;
        }
        boolean offsetMenu(View menu,int offset){
            if(menu==null||menu.getHeight()<=0||!(menu.getLayoutParams() instanceof ViewGroup.MarginLayoutParams p))return false;
            MenuInset saved=menus.get(menu);
            if(saved==null){saved=new MenuInset(p.topMargin);menus.put(menu,saved);}
            else if(p.topMargin!=saved.applied)saved.original=p.topMargin;
            // Keep the host's own margin; do not accumulate our offset.
            int target=saved.original+offset;
            saved.applied=target;
            if(p.topMargin==target)return false;
            p.topMargin=target;menu.setLayoutParams(p);return true;
        }
        void restoreMenus(){
            for(Map.Entry<View,MenuInset> entry:menus.entrySet()){
                View menu=entry.getKey();MenuInset saved=entry.getValue();
                if(menu.getLayoutParams() instanceof ViewGroup.MarginLayoutParams p&&p.topMargin==saved.applied){p.topMargin=saved.original;menu.setLayoutParams(p);}
            }
            menus.clear();
        }
        void restore(){
            if(!applied||changing)return;changing=true;applied=false;
            try{
                restoreMenus();
                decor.getOverlay().remove(scrim);
                window.setStatusBarColor(originalColor);window.setFlags(originalFlags,FLAGS_MASK);
                decor.setSystemUiVisibility((decor.getSystemUiVisibility()&~UI_MASK)|(originalUi&UI_MASK));
                if(Build.VERSION.SDK_INT>=29)window.setStatusBarContrastEnforced(contrast);
                if(Build.VERSION.SDK_INT>=30){WindowManager.LayoutParams p=window.getAttributes();int status=WindowInsets.Type.statusBars();p.setFitInsetsTypes((p.getFitInsetsTypes()&~status)|(fitTypes&status));p.layoutInDisplayCutoutMode=cutout;window.setAttributes(p);WindowInsetsController c=window.getInsetsController();if(c!=null)c.setSystemBarsAppearance(appearance,WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);}
                decor.requestApplyInsets();
            }catch(Throwable e){HookRuntime.log("[EasyBiliTool] IMMERSIVE_STATUS_RESTORE "+e.getClass().getSimpleName());}finally{changing=false;}
        }
    }
    private static final class MenuInset {
        int original,applied;
        MenuInset(int margin){original=margin;applied=margin;}
    }
}
