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
