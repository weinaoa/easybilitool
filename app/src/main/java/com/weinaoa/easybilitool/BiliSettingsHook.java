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

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import java.lang.reflect.*;
import java.lang.ref.WeakReference;
import java.util.*;

/** Adds a separate entry without replacing host or other modules' preferences. */
public final class BiliSettingsHook {
    private static final Map<Activity,WeakReference<InAppSettings>> windows=new WeakHashMap<>();
    private static void open(Context context){
        Activity activity=HostActions.activity(context);
        WeakReference<InAppSettings> reference=windows.get(activity);
        InAppSettings existing=reference==null?null:reference.get();
        if(existing!=null&&existing.isShowing())return;
        InAppSettings settings=new InAppSettings(activity);windows.put(activity,new WeakReference<>(settings));settings.show();
    }
    public static void install(Application application,ClassLoader loader)throws Exception{
        installRoute(application,loader);
        Class<?> preference=Class.forName("androidx.preference.Preference",false,loader);
        Class<?> group=Class.forName("androidx.preference.PreferenceGroup",false,loader);
        Method listenerSetter=null;
        for(Method m:preference.getMethods())if(m.getName().equals("setOnPreferenceClickListener")&&m.getParameterCount()==1)listenerSetter=m;
        if(listenerSetter==null)throw new NoSuchMethodException("preference listener");
        final Method setter=listenerSetter;int installed=0;
        for(String name:new String[]{"com.bilibili.app.preferences.BiliPreferencesActivity$BiliPreferencesFragment","com.bilibili.app.preferences.fragment.WideBiliPreferencesFragment"}){
            try{Class<?> fragment=Class.forName(name,false,loader);
                HookRuntime.hookMethod(fragment.getDeclaredMethod("onCreatePreferences",Bundle.class,String.class),new MethodHook(){
                    @Override protected void afterHookedMethod(MethodHookParam p){
                        if(p.hasThrowable())return;
                        try{
                            Object screen=Reflector.callMethod(p.thisObject,"getPreferenceScreen");if(screen==null)return;
                            if(group.getMethod("findPreference",CharSequence.class).invoke(screen,"easy_bili_tool_entry")!=null)return;
                            Context context=(Context)Reflector.callMethod(p.thisObject,"getContext");if(context==null)return;
                            Object entry=preference.getConstructor(Context.class).newInstance(context);
                            preference.getMethod("setKey",String.class).invoke(entry,"easy_bili_tool_entry");
                            preference.getMethod("setTitle",CharSequence.class).invoke(entry,"简单bili小工具");
                            preference.getMethod("setSummary",CharSequence.class).invoke(entry,"沉浸状态栏 · 滑动时收起底栏 · 沉浸导航栏");
                            preference.getMethod("setOrder",int.class).invoke(entry,-100);
                            Class<?> listener=setter.getParameterTypes()[0];
                            setter.invoke(entry,Proxy.newProxyInstance(loader,new Class<?>[]{listener},(proxy,method,args)->{
                                if(method.getName().equals("onPreferenceClick")){open(context);return true;}
                                if(method.getName().equals("hashCode"))return System.identityHashCode(proxy);
                                if(method.getName().equals("equals"))return proxy==args[0];
                                if(method.getName().equals("toString"))return "EasyBiliToolPreferenceClick";
                                return null;
                            }));
                            group.getMethod("addPreference",preference).invoke(screen,entry);
                            HookRuntime.log("[EasyBiliTool] SETTINGS_ENTRY_ADDED");
                        }catch(Throwable e){HookRuntime.log("[EasyBiliTool] SETTINGS_ENTRY_ERROR "+e);}
                    }
                });installed++;
            }catch(ClassNotFoundException ignored){}
        }
        if(installed==0)throw new ClassNotFoundException("Bili preferences fragment");
    }
    private static void installRoute(Application application,ClassLoader loader)throws Exception{
        // The host preference activity is not exported. Enter through its exported
        // launcher, then use a host-context intent to reach the original settings.
        Set<Method> methods=new HashSet<>();
        for(String name:new String[]{"tv.danmaku.bili.MainActivityV2",SettingsRoute.PREFERENCES}){
            Class<?> type=Class.forName(name,false,loader);
            for(Class<?> c=type;c!=null&&Activity.class.isAssignableFrom(c);c=c.getSuperclass()){
                for(Method method:c.getDeclaredMethods()){
                    Class<?>[] parameters=method.getParameterTypes();
                    if(!method.getName().equals("onNewIntent")||parameters.length<1||parameters[0]!=Intent.class||!methods.add(method))continue;
                    HookRuntime.hookMethod(method,new MethodHook(){
                        @Override protected void afterHookedMethod(MethodHookParam p){
                            Activity activity=(Activity)p.thisObject;Intent intent=(Intent)p.args[0];
                            if(intent!=null&&SettingsRoute.ACTION.equals(intent.getAction())&&intent.getBooleanExtra(SettingsRoute.EXTRA,false))activity.setIntent(intent);
                        }
                    });
                }
            }
        }
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks(){
            @Override public void onActivityResumed(Activity activity){
                if(!SettingsRoute.consume(activity.getIntent()))return;
                activity.getWindow().getDecorView().post(()->{
                    if(activity.isFinishing()||activity.isDestroyed())return;
                    try{
                        if(SettingsRoute.PREFERENCES.equals(activity.getClass().getName()))open(activity);
                        else activity.startActivity(SettingsRoute.mark(new Intent().setClassName(SettingsRoute.HOST,SettingsRoute.PREFERENCES)));
                        android.util.Log.i("EasyBiliTool","SETTINGS_ROUTE_OPENED "+activity.getClass().getSimpleName());
                    }catch(Throwable e){HookRuntime.log("[EasyBiliTool] SETTINGS_ROUTE_ERROR "+e.getClass().getSimpleName());}
                });
            }
            @Override public void onActivityDestroyed(Activity activity){windows.remove(activity);}
            @Override public void onActivityCreated(Activity a,Bundle b){}
            @Override public void onActivityStarted(Activity a){}
            @Override public void onActivityPaused(Activity a){}
            @Override public void onActivityStopped(Activity a){}
            @Override public void onActivitySaveInstanceState(Activity a,Bundle b){}
        });
    }
}
