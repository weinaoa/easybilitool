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
import android.view.*;

/** The native preferences list can scroll through the inset, with a safe final row. */
final class SettingsNavigationInsets {
    private final Activity activity;
    private ViewGroup list;
    private boolean applied,clip;
    private int padding,lastPadding;
    SettingsNavigationInsets(Activity activity){this.activity=activity;}
    boolean bind(View content){
        if(!activity.getClass().getName().equals("com.bilibili.app.preferences.BiliPreferencesActivity"))return false;
        int id=activity.getResources().getIdentifier("recycler_view","id","tv.danmaku.bili");
        View next=id==0?null:content.findViewById(id);
        if(!(next instanceof ViewGroup group))return false;
        if(list!=group){restore();list=group;}
        return true;
    }
    boolean apply(int inset){
        if(!applied){padding=list.getPaddingBottom();clip=list.getClipToPadding();applied=true;}
        else if(list.getPaddingBottom()!=lastPadding)padding=list.getPaddingBottom();
        lastPadding=padding+inset;
        boolean changed=list.getPaddingBottom()!=lastPadding;
        if(changed)list.setPadding(list.getPaddingLeft(),list.getPaddingTop(),list.getPaddingRight(),lastPadding);
        if(list.getClipToPadding())list.setClipToPadding(false);
        return changed;
    }
    void restore(){
        if(!applied)return;applied=false;
        if(list.getPaddingBottom()==lastPadding)list.setPadding(list.getPaddingLeft(),list.getPaddingTop(),list.getPaddingRight(),padding);
        if(!list.getClipToPadding())list.setClipToPadding(clip);
    }
}
