package com.weinaoa.easybilitool;

public final class MainTabBarScrollTest {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        MainTabBarScroll s=new MainTabBarScroll();
        for(String page:new String[]{"关注","动态","会员购","我的","我"})check(MainTabBarScroll.supports(page),page);
        check(!MainTabBarScroll.supports("首页")&&!MainTabBarScroll.supports("发布"),"home offset and publish stay separate");
        check(s.scrolled(50,200,false)==.25f,"actual content scroll collapses proportionally");
        check(s.scrolled(500,200,false)==1,"long scroll clamps fully hidden");
        check(s.scrolled(-50,200,false)==.75f,"reverse starts showing immediately");
        check(s.scrolled(-500,200,false)==0,"long reverse clamps fully visible");
        s.scrolled(80,200,false);check(s.scrolled(1,200,true)==0,"top and overscroll show bar");
        s.scrolled(80,200,false);s.reset();check(s.fraction()==0,"page switch or disabling resets progress");
        check(s.scrolled(80,0,false)==0,"unmeasured bar stays visible");
        System.out.println("Main tab scroll tests passed: "+checks);
    }
}
