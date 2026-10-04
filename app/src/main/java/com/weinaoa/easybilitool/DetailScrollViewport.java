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
import java.lang.reflect.Field;
import java.util.*;

/** Let the actual list draw behind a collapsing native bar, with stable end padding. */
final class DetailScrollViewport {
    private final Activity activity;
    private final ViewGroup bar;
    private final View decor;
    private final Map<View,Anchor> anchors=new IdentityHashMap<>();
    private final Map<ViewGroup,SafeScroll> lists=new IdentityHashMap<>();
    DetailScrollViewport(Activity activity,ViewGroup bar,View decor){this.activity=activity;this.bar=bar;this.decor=decor;}
    void apply(){
        for(String name:new String[]{"opus_nested_scroll","comment_main"}){
            int id=id(name);View content=id==0?null:decor.findViewById(id);
            if(content!=null&&content.getParent()==bar.getParent()){
                Anchor anchor=anchors.computeIfAbsent(content,v->Anchor.read(v,bar.getId()));
                if(anchor!=null)anchor.apply();
            }
        }
        int[] at=new int[2];bar.getLocationOnScreen(at);
        int nativeTop=at[1]-Math.round(DetailBottomBarHook.translation(bar));
        reserve(decor,id("cmt3_recycler"),nativeTop);
    }
    private int id(String name){return activity.getResources().getIdentifier(name,"id","tv.danmaku.bili");}
    private void reserve(View view,int id,int barTop){
        if(id==0)return;
        if(view.getId()==id&&view instanceof ViewGroup group&&view.isShown()){
            int[] at=new int[2];view.getLocationOnScreen(at);
            lists.computeIfAbsent(group,SafeScroll::new).apply(Math.max(0,at[1]+view.getHeight()-barTop));
        }
        if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++)reserve(group.getChildAt(i),id,barTop);
    }
    void restore(){
        for(Anchor anchor:anchors.values())if(anchor!=null)anchor.restore();anchors.clear();
        for(SafeScroll list:lists.values())list.restore();lists.clear();
    }
    private static final class Anchor {
        final View view;final Object params;final Field top,bottom;final int originalTop,originalBottom;
        Anchor(View view,Object params,Field top,Field bottom,int originalTop,int originalBottom){this.view=view;this.params=params;this.top=top;this.bottom=bottom;this.originalTop=originalTop;this.originalBottom=originalBottom;}
        static Anchor read(View view,int barId){
            try{
                Object params=view.getLayoutParams();
                Field top=Reflector.findField(params.getClass(),"bottomToTop"),bottom=Reflector.findField(params.getClass(),"bottomToBottom");
                int t=top.getInt(params),b=bottom.getInt(params);
                return t==barId?new Anchor(view,params,top,bottom,t,b):null;
            }catch(ReflectiveOperationException unsupported){return null;}
        }
        void apply(){
            if(view.getLayoutParams()!=params)return;
            try{if(top.getInt(params)==originalTop&&bottom.getInt(params)==originalBottom){top.setInt(params,-1);bottom.setInt(params,0);view.setLayoutParams((ViewGroup.LayoutParams)params);}}
            catch(ReflectiveOperationException ignored){}
        }
        void restore(){
            if(view.getLayoutParams()!=params)return;
            try{if(top.getInt(params)==-1&&bottom.getInt(params)==0){top.setInt(params,originalTop);bottom.setInt(params,originalBottom);view.setLayoutParams((ViewGroup.LayoutParams)params);}}
            catch(ReflectiveOperationException ignored){}
        }
    }
    private static final class SafeScroll {
        final ViewGroup view;final boolean clip;int original,last;boolean applied;
        SafeScroll(ViewGroup view){this.view=view;clip=view.getClipToPadding();original=view.getPaddingBottom();}
        void apply(int inset){
            if(applied&&view.getPaddingBottom()!=last)original=view.getPaddingBottom();
            last=original+inset;applied=true;
            if(view.getPaddingBottom()!=last)view.setPadding(view.getPaddingLeft(),view.getPaddingTop(),view.getPaddingRight(),last);
            if(view.getClipToPadding())view.setClipToPadding(false);
        }
        void restore(){
            if(applied&&view.getPaddingBottom()==last)view.setPadding(view.getPaddingLeft(),view.getPaddingTop(),view.getPaddingRight(),original);
            if(!view.getClipToPadding()&&clip)view.setClipToPadding(clip);
        }
    }
}
