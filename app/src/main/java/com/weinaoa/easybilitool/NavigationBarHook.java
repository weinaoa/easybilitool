package com.weinaoa.easybilitool;

import android.app.*;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.drawable.ColorDrawable;
import android.os.*;
import android.util.TypedValue;
import android.view.*;
import java.util.*;
import java.util.function.Supplier;

/** Navigation-only edge-to-edge. Preserve host status-bar and keyboard ownership. */
final class NavigationBarHook {
    private static final int UI_MASK=View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
    private static final int FLAGS_MASK=WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION|WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
    private final Supplier<Config> settings;
    private final Map<Activity,State> activities=new WeakHashMap<>();
    private final Map<View,State> decors=new WeakHashMap<>();
    private final Map<Window,State> windows=new WeakHashMap<>();
    private final Map<WindowInsetsController,State> controllers=new WeakHashMap<>();
    NavigationBarHook(Supplier<Config> settings){this.settings=settings;}
    void install(Application application)throws Exception {
        Reflector.findAndHookMethod(View.class,"dispatchApplyWindowInsets",WindowInsets.class,new MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){
                State s=decors.get(p.thisObject);if(s==null)return;
                WindowInsets insets=(WindowInsets)p.args[0];s.captureInsets(insets);
                if(!s.applied||!s.eligible())return;
                if(Build.VERSION.SDK_INT>=30)p.args[0]=new WindowInsets.Builder(insets)
                    .setInsets(WindowInsets.Type.navigationBars(),Insets.NONE)
                    .setInsetsIgnoringVisibility(WindowInsets.Type.navigationBars(),Insets.NONE).build();
                else p.args[0]=insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),0);
            }
        });
        Class<?> phoneWindow=Class.forName("com.android.internal.policy.PhoneWindow");
        Reflector.findAndHookMethod(phoneWindow,"setNavigationBarColor",int.class,new MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){State s=windows.get(p.thisObject);if(s!=null&&s.applied&&!s.changing){s.originalColor=(Integer)p.args[0];if(s.eligible())p.args[0]=Color.TRANSPARENT;}}
        });
        if(Build.VERSION.SDK_INT>=28)Reflector.findAndHookMethod(phoneWindow,"setNavigationBarDividerColor",int.class,new MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){State s=windows.get(p.thisObject);if(s!=null&&s.applied&&!s.changing){s.originalDivider=(Integer)p.args[0];if(s.eligible())p.args[0]=Color.TRANSPARENT;}}
        });
        Reflector.findAndHookMethod(View.class,"setSystemUiVisibility",int.class,new MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){
                State s=decors.get(p.thisObject);if(s==null||!s.applied||s.changing)return;
                int requested=(Integer)p.args[0];s.originalUi=(s.originalUi&~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)|(requested&View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
                if(s.eligible())p.args[0]=s.navigationUi(requested);
            }
        });
        if(Build.VERSION.SDK_INT>=30)Reflector.findAndHookMethod(Class.forName("android.view.InsetsController"),"setSystemBarsAppearance",int.class,int.class,new MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){
                State s=controllers.get(p.thisObject);if(s==null||!s.applied||s.changing)return;
                int mask=(Integer)p.args[1],requested=(Integer)p.args[0];
                if((mask&WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)==0)return;
                s.originalAppearance=(s.originalAppearance&~mask)|(requested&mask);
                if(s.eligible())p.args[0]=(requested&~WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)|(s.lightBackground()?WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS:0);
            }
        });
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks(){
            public void onActivityCreated(Activity a,Bundle b){State s=new State(a);activities.put(a,s);decors.put(s.decor,s);windows.put(s.window,s);s.decor.getViewTreeObserver().addOnPreDrawListener(s.preDraw);}
            public void onActivityResumed(Activity a){State s=activities.get(a);if(s!=null){s.resumed=true;s.handler.removeCallbacks(s.tick);s.handler.post(s.tick);}}
            public void onActivityPaused(Activity a){State s=activities.get(a);if(s!=null){s.resumed=false;s.handler.removeCallbacks(s.tick);s.restore();}}
            public void onActivityDestroyed(Activity a){State s=activities.remove(a);if(s!=null){s.resumed=false;s.handler.removeCallbacks(s.tick);s.restore();if(s.decor.getViewTreeObserver().isAlive())s.decor.getViewTreeObserver().removeOnPreDrawListener(s.preDraw);decors.remove(s.decor);windows.remove(s.window);controllers.values().removeIf(value->value==s);}}
            public void onActivityStarted(Activity a){}public void onActivityStopped(Activity a){}public void onActivitySaveInstanceState(Activity a,Bundle b){}
        });
        HookRuntime.log("[EasyBiliTool] NAVIGATION_EDGE_READY bottom insets preserved");
    }
    private final class State {
        final Activity activity;final Window window;final View decor;
        final Handler handler=new Handler(Looper.getMainLooper());
        final ViewTreeObserver.OnPreDrawListener preDraw=()->!update();
        final Runnable tick=new Runnable(){public void run(){update();if(resumed)handler.postDelayed(this,500);}};
        View content;
        final MainNavigationInsets mainInsets;
        final SettingsNavigationInsets settingsInsets;
        final DetailNavigationInsets detailInsets;
        final PageNavigationInsets pageInsets=new PageNavigationInsets();
        boolean resumed,changing,applied,contrast,imeVisible,failed,topApplied;
        int bottomInset,statusInset,originalTop,lastTop,originalColor,originalDivider,originalUi,originalFlags,originalFitTypes,originalAppearance;
        final int[] contentLocation=new int[2];
        State(Activity a){activity=a;window=a.getWindow();decor=window.getDecorView();mainInsets=new MainNavigationInsets(a);settingsInsets=new SettingsNavigationInsets(a);detailInsets=new DetailNavigationInsets(a);}
        void captureInsets(WindowInsets insets){
            if(Build.VERSION.SDK_INT>=30){
                bottomInset=insets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars()).bottom;
                statusInset=insets.getInsets(WindowInsets.Type.statusBars()).top;
                // An edge-to-edge host or another bar manager may consume navigation
                // insets. The visible system gesture area still requires safe controls.
                if(insets.isVisible(WindowInsets.Type.navigationBars()))
                    bottomInset=Math.max(bottomInset,insets.getInsets(WindowInsets.Type.mandatorySystemGestures()).bottom);
                imeVisible=insets.isVisible(WindowInsets.Type.ime());
            }
            else{bottomInset=insets.getStableInsetBottom();statusInset=insets.getSystemWindowInsetTop();imeVisible=insets.getSystemWindowInsetBottom()>bottomInset+Math.round(80*activity.getResources().getDisplayMetrics().density);}
        }
        boolean eligible(){return resumed&&settings.get().b("NAVIGATION_EDGE_TO_EDGE")&&!activity.isFinishing()&&!activity.isDestroyed()&&!imeVisible
            &&activity.getResources().getConfiguration().orientation==Configuration.ORIENTATION_PORTRAIT
            &&!activity.isInPictureInPictureMode()&&!activity.isInMultiWindowMode()
            &&(decor.getSystemUiVisibility()&View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)==0;}
        boolean lightBackground(){
            if(detailInsets.darkSurface())return false;
            if(content!=null&&content.getBackground() instanceof ColorDrawable color)return Color.luminance(color.getColor())>.5;
            TypedValue color=new TypedValue();
            if(activity.getTheme().resolveAttribute(android.R.attr.colorBackground,color,true)&&color.type>=TypedValue.TYPE_FIRST_COLOR_INT&&color.type<=TypedValue.TYPE_LAST_COLOR_INT)return Color.luminance(color.data)>.5;
            return (activity.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)!=Configuration.UI_MODE_NIGHT_YES;
        }
        int navigationUi(int ui){return ((ui|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)&~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)|(lightBackground()?View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR:0);}
        boolean preserveVideoStatusInset(){
            // Theseus repositions its content when navigation layout flags change.
            // Reserve the native status area unless a fullscreen layout owns it.
            if(!detailInsets.videoSurface()||(decor.getSystemUiVisibility()&View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)!=0){return restoreTop();}
            content.getLocationInWindow(contentLocation);
            int safe=Math.max(0,statusInset-contentLocation[1]);
            if(!topApplied){if(content.getPaddingTop()>=safe)return false;originalTop=content.getPaddingTop();topApplied=true;}
            else if(content.getPaddingTop()!=lastTop)originalTop=content.getPaddingTop();
            lastTop=Math.max(originalTop,safe);
            if(content.getPaddingTop()==lastTop)return false;
            content.setPadding(content.getPaddingLeft(),lastTop,content.getPaddingRight(),content.getPaddingBottom());return true;
        }
        boolean restoreTop(){
            if(!topApplied)return false;topApplied=false;
            if(content.getPaddingTop()!=lastTop)return false;
            content.setPadding(content.getPaddingLeft(),originalTop,content.getPaddingRight(),content.getPaddingBottom());return true;
        }
        boolean update(){
            if(changing||failed)return false;
            boolean layoutChanged=false;changing=true;
            try{
                WindowInsets insets=decor.getRootWindowInsets();if(insets!=null)captureInsets(insets);
                if(!eligible()){restore();return false;}
                View next=decor.findViewById(android.R.id.content);
                if(next==null||!next.isLaidOut()||bottomInset<=0){restore();return false;}
                if(content!=next){restore();content=next;}
                if(!applied){
                    originalColor=window.getNavigationBarColor();originalUi=decor.getSystemUiVisibility();originalFlags=window.getAttributes().flags;
                    if(Build.VERSION.SDK_INT>=28)originalDivider=window.getNavigationBarDividerColor();
                    if(Build.VERSION.SDK_INT>=29)contrast=window.isNavigationBarContrastEnforced();
                    if(Build.VERSION.SDK_INT>=30){originalFitTypes=window.getAttributes().getFitInsetsTypes();WindowInsetsController c=window.getInsetsController();originalAppearance=c==null?0:c.getSystemBarsAppearance();}
                    applied=true;decor.requestApplyInsets();
                }
                int ui=navigationUi(decor.getSystemUiVisibility());if(ui!=decor.getSystemUiVisibility())decor.setSystemUiVisibility(ui);
                if((window.getAttributes().flags&FLAGS_MASK)!=WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)window.setFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,FLAGS_MASK);
                if(window.getNavigationBarColor()!=Color.TRANSPARENT)window.setNavigationBarColor(Color.TRANSPARENT);
                if(Build.VERSION.SDK_INT>=28&&window.getNavigationBarDividerColor()!=Color.TRANSPARENT)window.setNavigationBarDividerColor(Color.TRANSPARENT);
                if(Build.VERSION.SDK_INT>=29&&window.isNavigationBarContrastEnforced())window.setNavigationBarContrastEnforced(false);
                if(Build.VERSION.SDK_INT>=30){
                    WindowManager.LayoutParams p=window.getAttributes();int fit=p.getFitInsetsTypes()&~WindowInsets.Type.navigationBars();
                    if(fit!=p.getFitInsetsTypes()){p.setFitInsetsTypes(fit);window.setAttributes(p);}
                    WindowInsetsController c=window.getInsetsController();if(c!=null){controllers.put(c,this);int appearance=lightBackground()?WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS:0;if((c.getSystemBarsAppearance()&WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)!=appearance)c.setSystemBarsAppearance(appearance,WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);}
                }
                boolean main=mainInsets.bind(content),preferences=!main&&settingsInsets.bind(content),detail=!main&&!preferences&&detailInsets.bind(content);
                if(main||preferences||detail){
                    pageInsets.restore();
                    layoutChanged=(main?mainInsets.apply(bottomInset):preferences?settingsInsets.apply(bottomInset):detailInsets.apply(content,bottomInset))||layoutChanged;
                }
                else layoutChanged=pageInsets.apply(content,bottomInset)||layoutChanged;
                layoutChanged=preserveVideoStatusInset()||layoutChanged;
            }catch(Throwable e){restore();failed=true;HookRuntime.log("[EasyBiliTool] NAVIGATION_EDGE_BYPASS: "+e.getClass().getSimpleName());}
            finally{changing=false;}
            return layoutChanged;
        }
        void restore(){
            if(!applied)return;
            boolean previous=changing;changing=true;applied=false;
            try{
                mainInsets.restore();
                settingsInsets.restore();
                detailInsets.restore();
                pageInsets.restore();
                restoreTop();
                window.setNavigationBarColor(originalColor);if(Build.VERSION.SDK_INT>=28)window.setNavigationBarDividerColor(originalDivider);
                window.setFlags(originalFlags,FLAGS_MASK);
                decor.setSystemUiVisibility((decor.getSystemUiVisibility()&~UI_MASK)|(originalUi&UI_MASK));
                if(Build.VERSION.SDK_INT>=29)window.setNavigationBarContrastEnforced(contrast);
                if(Build.VERSION.SDK_INT>=30){WindowManager.LayoutParams p=window.getAttributes();int nav=WindowInsets.Type.navigationBars();p.setFitInsetsTypes((p.getFitInsetsTypes()&~nav)|(originalFitTypes&nav));window.setAttributes(p);WindowInsetsController c=window.getInsetsController();if(c!=null)c.setSystemBarsAppearance(originalAppearance,WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);}
                decor.requestApplyInsets();
            }catch(Throwable e){HookRuntime.log("[EasyBiliTool] NAVIGATION_EDGE_RESTORE: "+e.getClass().getSimpleName());}
            finally{changing=previous;}
        }
    }
}
