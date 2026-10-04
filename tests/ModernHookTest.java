package com.weinaoa.easybilitool;

import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.*;
import java.util.*;

public final class ModernHookTest {
    static int checks;
    static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    public static int original(int x){return x*2;}
    static final class Chain implements XposedInterface.Chain {
        int calls;Object[] received;Throwable failure;
        public Executable getExecutable(){try{return ModernHookTest.class.getMethod("original",int.class);}catch(Exception e){throw new AssertionError(e);}}
        public Object getThisObject(){return null;}
        public List<Object> getArgs(){return Collections.singletonList(3);}
        public Object getArg(int index){return getArgs().get(index);}
        public Object proceed()throws Throwable{return proceed(3);}
        public Object proceed(Object... args)throws Throwable{calls++;received=args;if(failure!=null)throw failure;return original((Integer)args[0]);}
        public Object proceedWith(Object receiver)throws Throwable{return proceed();}
        public Object proceedWith(Object receiver,Object[] args)throws Throwable{return proceed(args);}
    }
    static class Parent {private int code=17;private String inherited(){return "parent";}}
    static final class Host extends Parent {private String own(){return "host";}}
    public static void main(String[] args)throws Throwable {
        List<Throwable> errors=new ArrayList<>();Chain chain=new Chain();
        Object result=new MethodHook(){}.intercept(chain,errors::add);
        check(result.equals(6)&&chain.calls==1,"unchanged hook proceeds once");check(errors.isEmpty(),"no spurious failure");
        chain=new Chain();result=new MethodHook(){protected void beforeHookedMethod(MethodHookParam p){p.args[0]=8;}protected void afterHookedMethod(MethodHookParam p){p.setResult((Integer)p.getResult()+1);}}.intercept(chain,errors::add);
        check(result.equals(17),"changed args and after result");check(chain.calls==1&&chain.received[0].equals(8),"changed args forwarded to next chain");
        chain=new Chain();result=new MethodHook(){protected void beforeHookedMethod(MethodHookParam p){p.setResult(21);}protected void afterHookedMethod(MethodHookParam p){p.setResult((Integer)p.getResult()+1);}}.intercept(chain,errors::add);
        check(chain.calls==0&&result.equals(22),"short circuit still runs after callback");
        chain=new Chain();result=new MethodHook(){protected void beforeHookedMethod(MethodHookParam p){p.args[0]=9;p.setResult(99);throw new IllegalStateException();}}.intercept(chain,errors::add);
        check(chain.calls==1&&chain.received[0].equals(3)&&result.equals(6),"before failure restores native arguments and result");check(errors.size()==1,"before failure logged");
        chain=new Chain();result=new MethodHook(){protected void afterHookedMethod(MethodHookParam p){p.setResult(99);throw new IllegalStateException();}}.intercept(chain,errors::add);
        check(result.equals(6)&&chain.calls==1,"after failure retains proceeded result");
        IllegalArgumentException nativeFailure=new IllegalArgumentException("native");chain=new Chain();chain.failure=nativeFailure;
        try{new MethodHook(){}.intercept(chain,errors::add);throw new AssertionError("Native failure swallowed");}catch(IllegalArgumentException e){check(e==nativeFailure,"native exception preserved");}
        chain=new Chain();chain.failure=nativeFailure;result=new MethodHook(){protected void afterHookedMethod(MethodHookParam p){if(p.hasThrowable())p.setResult(44);}}.intercept(chain,errors::add);
        check(result.equals(44),"after callback may handle native failure");
        chain=new Chain();chain.failure=nativeFailure;
        try{new MethodHook(){protected void afterHookedMethod(MethodHookParam p){p.setResult(99);throw new IllegalStateException();}}.intercept(chain,errors::add);throw new AssertionError();}catch(IllegalArgumentException e){check(e==nativeFailure,"after failure retains native throwable");}
        Host host=new Host();
        check(Reflector.findField(Host.class,"code").getInt(host)==17,"inherited private field");
        check(Reflector.callMethod(host,"inherited").equals("parent"),"inherited no-argument method");
        check(Reflector.callMethod(host,"own").equals("host"),"host no-argument method");
        try{Reflector.callMethod(host,"missing");throw new AssertionError("Missing method accepted");}
        catch(NoSuchMethodException expected){check(true,"missing method fails safely");}
        System.out.println("Modern API and reflection tests passed: "+checks);
    }
}
