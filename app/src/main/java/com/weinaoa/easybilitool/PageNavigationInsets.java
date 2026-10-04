package com.weinaoa.easybilitool;

import android.view.*;

/** Inset an unrecognized page's controls, keeping its own root background full height. */
final class PageNavigationInsets {
    private ViewGroup root;
    private boolean applied,clip;
    private int padding,lastPadding;
    boolean apply(View content,int inset){
        ViewGroup next=null;
        if(content instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){
            View child=group.getChildAt(i);
            if(child.getVisibility()==View.VISIBLE&&child instanceof ViewGroup candidate){next=candidate;break;}
        }
        if(root!=next){restore();root=next;}
        if(root==null)return false;
        if(!applied){padding=root.getPaddingBottom();clip=root.getClipToPadding();applied=true;}
        else if(root.getPaddingBottom()!=lastPadding)padding=root.getPaddingBottom();
        lastPadding=padding+inset;
        boolean changed=root.getPaddingBottom()!=lastPadding;
        if(changed)root.setPadding(root.getPaddingLeft(),root.getPaddingTop(),root.getPaddingRight(),lastPadding);
        if(root.getClipToPadding()){root.setClipToPadding(false);changed=true;}
        return changed;
    }
    void restore(){
        if(!applied)return;applied=false;
        if(root.getPaddingBottom()==lastPadding)root.setPadding(root.getPaddingLeft(),root.getPaddingTop(),root.getPaddingRight(),padding);
        if(!root.getClipToPadding())root.setClipToPadding(clip);
    }
}
