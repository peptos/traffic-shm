/*
 * Copyright (c) 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.traffic.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.locks.LockSupport;

/**
 * @author cuiyi
 */
public abstract class Util {

    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();

    private static final boolean IS_WINDOWS = OS_NAME.startsWith("win");

    public static boolean isWindows() {
        return IS_WINDOWS;
    }

    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... params) {
        Assert.notNull(clazz, "Class must not be null");
        Assert.notNull(methodName, "Method name must not be null");

        try {
            Method method = clazz.getDeclaredMethod(methodName, params);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Expected method not found: " + ex);
        }
    }

    public static <T> T getProperty(Object target, String fieldName, Class<T> type) {
        Assert.notNull(target, "Target must not be null");
        Assert.notNull(fieldName, "Field name must not be null");

        Class<?> searchType = target.getClass();
        while (Object.class != searchType && searchType != null) {
            Field[] fields = getDeclaredFields(searchType);
            for (Field field : fields) {
                if ((fieldName == null || fieldName.equals(field.getName())) &&
                        (type == null || type.equals(field.getType()))) {

                    field.setAccessible(true);

                    try {
                        return (T) field.get(target);
                    } catch (IllegalAccessException ex) {
                        throw new IllegalStateException("Unexpected reflection exception - "
                                + ex.getClass().getName() + ": " + ex.getMessage());
                    }
                }
            }
            searchType = searchType.getSuperclass();
        }
        return null;
    }

    private static Field[] getDeclaredFields(Class<?> clazz) {
        Assert.notNull(clazz, "Class must not be null");

        try {
            Field[] result = clazz.getDeclaredFields();
            return result;
        } catch (Throwable ex) {
            throw new IllegalStateException("Failed to introspect Class [" + clazz.getName() +
                    "] from ClassLoader [" + clazz.getClassLoader() + "]", ex);
        }
    }

    public static Object invokeMethod(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static long align(long value, long alignment) {
        long mask = alignment - 1;
        return (value + mask) & ~mask; //((value + mask) / alignment) * alignment
    }

    public static int align(int value, int alignment) {
        int mask = alignment - 1;
        return (value + mask) & ~mask; //((value + mask) / alignment) * alignment
    }

    public static long pageAlign(long size) {
        return align(size, Constant.PAGE_SIZE);
    }

    public static int pageAlign(int size) {
        return align(size, Constant.PAGE_SIZE);
    }

    public static void pause(long millis) {
        long timeNanos = millis * 1000000;
        if (timeNanos > 10e6) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            LockSupport.parkNanos(timeNanos);
        }
    }

}
