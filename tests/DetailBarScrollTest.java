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


public final class DetailBarScrollTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        DetailBarScroll scroll=new DetailBarScroll();Object opus=new Object(),comments=new Object();
        scroll.touch(100);check(scroll.scrolled(200,100,false,101)==0,"no toolbar/list is a no-op");
        scroll.bind(opus);check(scroll.scrolled(100,100,false,200)==0,"initial layout does not hide");
        scroll.touch(300);check(scroll.scrolled(25,100,false,320)==.25f,"Opus parent scroll collapses");
        check(scroll.scrolled(25,100,false,350)==.5f,"child-consumed portion adds to parent portion");
        check(scroll.scrolled(100,100,false,1000)==1,"fling finishes collapse");
        check(scroll.scrolled(-25,100,false,2300)==.75f,"ongoing fling extends observation window");
        check(scroll.scrolled(100,100,false,3901)==.75f,"late programmatic scroll is ignored");
        scroll.touch(4000);check(scroll.scrolled(-25,100,false,4020)==.5f,"reverse swipe restores immediately");
        scroll.bind(opus);check(scroll.fraction()==.5f,"same surface retains progress");
        scroll.bind(comments);check(scroll.fraction()==0,"new comment panel starts visible");
        check(scroll.scrolled(100,100,false,4050)==0,"restored position of new panel is ignored");
        scroll.touch(4100);check(scroll.scrolled(25,100,false,4150)==.25f,"new panel reacts after touch");
        check(scroll.scrolled(-1,100,true,4200)==0,"at list top the bar is fully visible");
        scroll.scrolled(100,100,false,4250);scroll.reset();check(scroll.fraction()==0,"keyboard/pause/disabled resets progress");
        check(scroll.scrolled(100,100,false,4300)==0,"reset also clears old touch authorization");
        scroll.touch(4400);check(scroll.scrolled(100,0,false,4420)==0,"unmeasured toolbar stays visible");
        scroll.bind(null);scroll.touch(4500);check(scroll.scrolled(100,100,false,4520)==0,"leaving supported page stays visible");
        System.out.println("Detail scroll tests passed: "+checks);
    }
}
