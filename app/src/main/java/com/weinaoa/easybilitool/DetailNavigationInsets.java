package com.weinaoa.easybilitool;

import android.app.Activity;
import android.view.*;
import java.util.*;
import android.graphics.drawable.Drawable;

/** Keep native detail/live surfaces full height; inset only lists or fixed controls. */
final class DetailNavigationInsets {
    private final Activity activity;
    private final boolean live,video;
    private final int controlsId,commentsId,introId,interactionId,bannersId;
    private final Map<View,Adjustment> adjustments=new IdentityHashMap<>();

    DetailNavigationInsets(Activity activity){
        this.activity=activity;
        String name=activity.getClass().getName();
        live=name.equals("com.bilibili.bililive.room.ui.roomv3.LiveRoomActivityV3");
        video=name.equals("com.bilibili.ship.theseus.detail.UnitedBizDetailsActivity")
            ||name.equals("com.bilibili.ship.theseus.playlist.UnitedPlaylistActivity")
            ||name.equals("com.bilibili.video.videodetail.VideoDetailsActivity");
        controlsId=id("bottom_controllers_group");commentsId=id("cmt3_fake_input_bar_container");introId=id("recycler");
        interactionId=id("interaction_container");bannersId=id("live_operation_banner_container");
    }
    private int id(String name){return activity.getResources().getIdentifier(name,"id","tv.danmaku.bili");}
    boolean bind(View content){return live||video||detailSurface(content);}
    boolean detailSurface(View content){return content!=null&&DetailBarLocator.findBar(activity,content)!=null;}
    boolean darkSurface(){return live;}
    boolean videoSurface(){return video;}
    boolean apply(View content,int inset){
        Set<View> present=Collections.newSetFromMap(new IdentityHashMap<>());
        boolean changed=false;
        if(live){
            changed=adjust(content,controlsId,true,inset,present);
            changed=adjustSafeArea(content,interactionId,inset,present)||changed;
            changed=adjustSafeArea(content,bannersId,inset,present)||changed;
        }
        if(video){
            changed=adjust(content,commentsId,true,inset,present)||changed;
            changed=adjust(content,introId,false,inset,present)||changed;
        }
        if(!video&&!live){ViewGroup bar=DetailBarLocator.findBar(activity,content);if(bar!=null){present.add(bar);changed=adjustments.computeIfAbsent(bar,v->new Adjustment(bar,true,false)).apply(inset)||changed;}}
        // A pager may replace its list or comment panel without recreating the Activity.
        for(Iterator<Map.Entry<View,Adjustment>> it=adjustments.entrySet().iterator();it.hasNext();){
            var entry=it.next();if(!present.contains(entry.getKey())){changed=entry.getValue().restore()||changed;it.remove();}
        }
        return changed;
    }
    private boolean adjust(View content,int id,boolean fixed,int inset,Set<View> present){
        View view=id==0?null:content.findViewById(id);
        if(!(view instanceof ViewGroup)||view.getLayoutParams()==null||!view.isLaidOut())return false;
        if(!fixed&&!isRecycler(view))return false;
        present.add(view);
        Adjustment adjustment=adjustments.computeIfAbsent(view,v->new Adjustment((ViewGroup)v,fixed,!fixed));
        return adjustment.apply(inset);
    }
    private boolean adjustSafeArea(View content,int id,int inset,Set<View> present){
        View view=id==0?null:content.findViewById(id);
        if(!(view instanceof ViewGroup)||view.getLayoutParams()==null||!view.isLaidOut())return false;
        present.add(view);
        return adjustments.computeIfAbsent(view,v->new Adjustment((ViewGroup)v,false,false)).apply(inset);
    }
    private boolean isRecycler(View view){
        for(Class<?> type=view.getClass();type!=null;type=type.getSuperclass())
            if(type.getName().equals("androidx.recyclerview.widget.RecyclerView"))return true;
        return false;
    }
    void restore(){for(Adjustment adjustment:adjustments.values())adjustment.restore();adjustments.clear();}

    private static final class Adjustment {
        final ViewGroup view;final boolean fixed,scroll;final boolean clip;
        int padding,height,measuredHeight,lastPadding,lastHeight;
        boolean applied;
        Drawable originalBackground,extendedBackground;
        Adjustment(ViewGroup view,boolean fixed,boolean scroll){this.view=view;this.fixed=fixed;this.scroll=scroll;clip=view.getClipToPadding();}
        boolean apply(int inset){
            ViewGroup.LayoutParams params=view.getLayoutParams();
            if(!applied){padding=view.getPaddingBottom();height=params.height;measuredHeight=view.getHeight();applied=true;originalBackground=view.getBackground();}
            else{
                if(view.getPaddingBottom()!=lastPadding)padding=view.getPaddingBottom();
                if(params.height!=lastHeight)height=params.height;
            }
            // The input's fill often belongs to its child, leaving a transparent
            // outer container. Extend that same native fill through the safe area.
            if(fixed&&view.getBackground()==null){Drawable fill=childBackground(view);if(fill!=null){Drawable.ConstantState state=fill.getConstantState();extendedBackground=state==null?fill:state.newDrawable(view.getResources()).mutate();view.setBackground(extendedBackground);}}
            // Fixed bars grow by the safe inset; their native content keeps its height.
            lastPadding=padding+inset;lastHeight=fixed?(height>0?height:measuredHeight)+inset:height;
            boolean changed=false;
            if(view.getPaddingBottom()!=lastPadding){view.setPadding(view.getPaddingLeft(),view.getPaddingTop(),view.getPaddingRight(),lastPadding);changed=true;}
            if(params.height!=lastHeight){params.height=lastHeight;view.setLayoutParams(params);changed=true;}
            if(scroll&&view.getClipToPadding()){view.setClipToPadding(false);changed=true;}
            return changed;
        }
        private Drawable childBackground(ViewGroup parent){for(int i=0;i<parent.getChildCount();i++){View child=parent.getChildAt(i);if(child.getBackground()!=null)return child.getBackground();if(child instanceof ViewGroup group){Drawable fill=childBackground(group);if(fill!=null)return fill;}}return null;}
        boolean restore(){
            if(!applied)return false;applied=false;boolean changed=false;
            if(extendedBackground!=null&&view.getBackground()==extendedBackground){view.setBackground(originalBackground);extendedBackground=null;changed=true;}
            if(view.getPaddingBottom()==lastPadding){view.setPadding(view.getPaddingLeft(),view.getPaddingTop(),view.getPaddingRight(),padding);changed=true;}
            ViewGroup.LayoutParams params=view.getLayoutParams();
            if(params!=null&&params.height==lastHeight&&lastHeight!=height){params.height=height;view.setLayoutParams(params);changed=true;}
            if(scroll&&!view.getClipToPadding()&&clip){view.setClipToPadding(clip);changed=true;}
            return changed;
        }
    }
}
