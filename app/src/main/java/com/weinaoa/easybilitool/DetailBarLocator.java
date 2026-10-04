package com.weinaoa.easybilitool;

import android.app.Activity;
import android.graphics.Rect;
import android.view.*;
import java.util.*;

/** Find the current fixed native detail toolbar, including a collapsed one. */
final class DetailBarLocator {
    private DetailBarLocator() {}
    static ViewGroup findBar(Activity activity,View root){
        // Home feed cards reuse lay_action. They are not fixed detail toolbars.
        if(activity.getClass().getName().equals("tv.danmaku.bili.MainActivityV2"))return null;
        String activityName=activity.getClass().getName();
        if(activityName.equals("com.bilibili.ship.theseus.detail.UnitedBizDetailsActivity")
            ||activityName.equals("com.bilibili.ship.theseus.playlist.UnitedPlaylistActivity")){
            // Theseus retains the comment toolbar/list when the introduction
            // tab is active. Visibility alone cannot identify that retained page.
            int pagerId=activity.getResources().getIdentifier("pager","id","tv.danmaku.bili");
            View pager=pagerId==0?null:root.findViewById(pagerId);
            if(pager!=null)try{
                Object current=pager.getClass().getMethod("getCurrentItem").invoke(pager);
                if(current instanceof Number page&&page.intValue()!=1)return null;
            }catch(ReflectiveOperationException unavailable){/* Fall back to native bounds. */}
        }
        Set<Integer> ids=new HashSet<>();for(String name:new String[]{"cmt3_fake_input_bar_container","opus_bottom_layout","lay_action"}){int id=activity.getResources().getIdentifier(name,"id","tv.danmaku.bili");if(id!=0)ids.add(id);}
        return visibleBar(root,root,ids,activity.getResources().getDisplayMetrics().density);
    }
    static ViewGroup visibleBar(View view,View root,Set<Integer> ids,float density){
        if(!view.isShown()||view.getAlpha()<=.01f&&!DetailBottomBarHook.managed(view))return null;
        if(view instanceof ViewGroup group){
            // Opus keeps another same-ID bar in its scrollable content. Select the
            // fixed, visible controls, not the first ID match or its loading stub.
            if(ids.contains(view.getId())&&view.getWidth()>root.getWidth()*.65&&view.getHeight()>=24*density&&view.getHeight()<180*density){
                int[] at=new int[2];view.getLocationInWindow(at);Rect visible=new Rect();
                float nativeTop=at[1]-DetailBottomBarHook.translation(view);
                boolean collapsed=DetailBottomBarHook.collapseFraction(view)>0;
                if(nativeTop>root.getHeight()*.65&&nativeTop+view.getHeight()<=root.getHeight()+1&&(collapsed||view.getGlobalVisibleRect(visible)&&visible.height()>=view.getHeight()*.8))return group;
            }
            for(int i=group.getChildCount()-1;i>=0;i--){ViewGroup found=visibleBar(group.getChildAt(i),root,ids,density);if(found!=null)return found;}
        }return null;
    }
}
