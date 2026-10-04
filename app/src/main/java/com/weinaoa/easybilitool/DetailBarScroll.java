package com.weinaoa.easybilitool;

/** Ignore layout/restored positions; follow only a touched detail list and its fling. */
final class DetailBarScroll {
    private final MainTabBarScroll progress=new MainTabBarScroll();
    private Object surface;
    private long scrollUntil;
    void bind(Object next){if(surface!=next){surface=next;reset();}}
    void touch(long now){if(surface!=null)scrollUntil=now+1500;}
    float fraction(){return progress.fraction();}
    float scrolled(int dy,int height,boolean atTop,long now){
        if(surface==null||scrollUntil==0||now>scrollUntil||dy==0)return fraction();
        scrollUntil=now+1500;
        return progress.scrolled(dy,height,atTop);
    }
    void reset(){scrollUntil=0;progress.reset();}
}
