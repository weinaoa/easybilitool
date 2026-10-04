package com.weinaoa.easybilitool;

import android.app.Activity;
import android.view.*;
import android.widget.ImageView;

/** Let the main feed draw behind navigation; inset only the fixed tab controls. */
final class MainNavigationInsets {
    private final Activity activity;
    private View bar,background,controls;
    private boolean applied,clip;
    private int height,padding,backgroundHeight,backgroundMargin;
    private int lastHeight,lastPadding,lastBackgroundHeight,lastBackgroundMargin;
    private int controlsHeight,controlsMeasuredHeight,lastControlsHeight;
    private ImageView.ScaleType backgroundScale;
    MainNavigationInsets(Activity activity){this.activity=activity;}
    boolean bind(View content){
        if(!activity.getClass().getName().equals("tv.danmaku.bili.MainActivityV2"))return false;
        int id=activity.getResources().getIdentifier("bottom_navigation","id","tv.danmaku.bili");
        View next=id==0?null:content.findViewById(id);
        if(next==null||!next.getClass().getName().equals("com.bilibili.lib.homepage.widget.TabHost"))return false;
        int bgId=activity.getResources().getIdentifier("tab_background","id","tv.danmaku.bili");
        int controlsId=activity.getResources().getIdentifier("container","id","tv.danmaku.bili");
        View nextBackground=bgId==0?null:next.findViewById(bgId),nextControls=controlsId==0?null:next.findViewById(controlsId);
        if(bar!=next||background!=nextBackground||controls!=nextControls){restore();bar=next;background=nextBackground;controls=nextControls;}
        return controls instanceof ViewGroup&&background!=null&&background.getLayoutParams() instanceof ViewGroup.MarginLayoutParams;
    }
    boolean apply(int inset){
        if(controls==null||!controls.isLaidOut()||controls.getHeight()<=0)return false;
        ViewGroup.LayoutParams p=bar.getLayoutParams();
        ViewGroup.LayoutParams cp=controls.getLayoutParams();
        ViewGroup.MarginLayoutParams bg=(ViewGroup.MarginLayoutParams)background.getLayoutParams();
        if(!applied){height=p.height;padding=controls.getPaddingBottom();controlsHeight=cp.height;controlsMeasuredHeight=controls.getHeight();backgroundHeight=bg.height;backgroundMargin=bg.bottomMargin;backgroundScale=background instanceof ImageView image?image.getScaleType():null;clip=((ViewGroup)bar).getClipToPadding();applied=true;}
        else{
            if(p.height!=lastHeight)height=p.height;
            if(controls.getPaddingBottom()!=lastPadding)padding=controls.getPaddingBottom();
            if(cp.height!=lastControlsHeight)controlsHeight=cp.height;
            if(bg.height!=lastBackgroundHeight)backgroundHeight=bg.height;
            if(bg.bottomMargin!=lastBackgroundMargin)backgroundMargin=bg.bottomMargin;
        }
        lastHeight=height>0?height+inset:height;lastPadding=padding+inset;
        lastControlsHeight=(controlsHeight>0?controlsHeight:controlsMeasuredHeight)+inset;
        lastBackgroundHeight=backgroundHeight>0?backgroundHeight+inset:backgroundHeight;lastBackgroundMargin=backgroundMargin;
        boolean changed=false;
        if(p.height!=lastHeight){p.height=lastHeight;bar.setLayoutParams(p);changed=true;}
        if(cp.height!=lastControlsHeight){cp.height=lastControlsHeight;controls.setLayoutParams(cp);changed=true;}
        // Padding the container extends its native default/theme background while
        // keeping the actual tab buttons above the gesture area.
        if(controls.getPaddingBottom()!=lastPadding){controls.setPadding(controls.getPaddingLeft(),controls.getPaddingTop(),controls.getPaddingRight(),lastPadding);changed=true;}
        if(bg.height!=lastBackgroundHeight||bg.bottomMargin!=lastBackgroundMargin){bg.height=lastBackgroundHeight;bg.bottomMargin=lastBackgroundMargin;background.setLayoutParams(bg);changed=true;}
        if(((ViewGroup)bar).getClipToPadding())((ViewGroup)bar).setClipToPadding(false);
        if(background instanceof ImageView image&&image.getScaleType()!=ImageView.ScaleType.FIT_XY)image.setScaleType(ImageView.ScaleType.FIT_XY);
        return changed;
    }
    void restore(){
        if(!applied)return;
        applied=false;
        ViewGroup.LayoutParams p=bar.getLayoutParams();
        if(p.height==lastHeight){p.height=height;bar.setLayoutParams(p);}
        if(controls.getPaddingBottom()==lastPadding)controls.setPadding(controls.getPaddingLeft(),controls.getPaddingTop(),controls.getPaddingRight(),padding);
        ViewGroup.LayoutParams cp=controls.getLayoutParams();
        if(cp.height==lastControlsHeight){cp.height=controlsHeight;controls.setLayoutParams(cp);}
        if(background.getLayoutParams() instanceof ViewGroup.MarginLayoutParams bg){
            boolean changed=false;
            if(bg.height==lastBackgroundHeight){bg.height=backgroundHeight;changed=true;}
            if(bg.bottomMargin==lastBackgroundMargin){bg.bottomMargin=backgroundMargin;changed=true;}
            if(changed)background.setLayoutParams(bg);
        }
        if(background instanceof ImageView image&&image.getScaleType()==ImageView.ScaleType.FIT_XY&&backgroundScale!=null)image.setScaleType(backgroundScale);
        if(!((ViewGroup)bar).getClipToPadding())((ViewGroup)bar).setClipToPadding(clip);
    }
}
