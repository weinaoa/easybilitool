package com.weinaoa.easybilitool;

import java.lang.reflect.*;

final class Reflector {
    private Reflector() {}
    static MethodHook.Unhook findAndHookMethod(Class<?> type, String name, Object... signature) throws ReflectiveOperationException {
        if (signature.length == 0 || !(signature[signature.length - 1] instanceof MethodHook callback))
            throw new IllegalArgumentException("Missing hook callback");
        Class<?>[] parameters = new Class<?>[signature.length - 1];
        for (int i = 0; i < parameters.length; i++) parameters[i] = (Class<?>)signature[i];
        Method method = type.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return HookRuntime.hookMethod(method, callback);
    }
    static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(type.getName() + "." + name);
    }
    static Object callMethod(Object target, String name) throws ReflectiveOperationException {
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            try {
                Method method = current.getDeclaredMethod(name);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (NoSuchMethodException ignored) {}
        }
        throw new NoSuchMethodException(target.getClass().getName() + "." + name);
    }
}
