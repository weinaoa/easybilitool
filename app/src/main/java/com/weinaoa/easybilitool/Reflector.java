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
