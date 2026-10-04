package com.weinaoa.easybilitool;

import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Executable;
import java.util.function.Consumer;

/** Local before/after convention backed exclusively by the modern interceptor chain. */
abstract class MethodHook {
    interface Unhook { void unhook(); }
    static final class MethodHookParam {
        final Executable method;
        final Object thisObject;
        Object[] args;
        private Object result;
        private Throwable throwable;
        private boolean returnEarly;
        MethodHookParam(XposedInterface.Chain chain){method=chain.getExecutable();thisObject=chain.getThisObject();args=chain.getArgs().toArray();}
        Object getResult(){return result;}
        boolean hasThrowable(){return throwable!=null;}
        Throwable getThrowable(){return throwable;}
        void setResult(Object value){result=value;throwable=null;returnEarly=true;}
        void setThrowable(Throwable value){throwable=value;result=null;returnEarly=true;}
    }
    protected void beforeHookedMethod(MethodHookParam param)throws Throwable {}
    protected void afterHookedMethod(MethodHookParam param)throws Throwable {}
    final Object intercept(XposedInterface.Chain chain,Consumer<Throwable> logger)throws Throwable {
        MethodHookParam param=new MethodHookParam(chain);
        Object[] originalArgs=param.args.clone();
        try{beforeHookedMethod(param);}catch(Throwable e){logger.accept(e);param.args=originalArgs;param.result=null;param.throwable=null;param.returnEarly=false;}
        if(!param.returnEarly){
            try{param.result=chain.proceed(param.args);}catch(Throwable e){param.throwable=e;}
        }
        Object result=param.result;Throwable throwable=param.throwable;
        try{afterHookedMethod(param);}catch(Throwable e){logger.accept(e);param.result=result;param.throwable=throwable;}
        if(param.throwable!=null)throw param.throwable;
        return param.result;
    }
}
