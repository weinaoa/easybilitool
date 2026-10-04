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

/** Directional progress for main pages without a collapsible home AppBar. */
final class MainTabBarScroll {
    private float fraction;
    static boolean supports(String page){return page.equals("关注")||page.equals("动态")||page.equals("会员购")||page.equals("我的")||page.equals("我");}
    float fraction(){return fraction;}
    float scrolled(int dy,int height,boolean atTop){
        if(height<=0||atTop){reset();return fraction;}
        fraction=Math.max(0,Math.min(1,fraction+dy/(float)height));return fraction;
    }
    void reset(){fraction=0;}
}
