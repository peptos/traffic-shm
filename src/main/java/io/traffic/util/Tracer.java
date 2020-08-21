/*
 * Copyright (c) 2019 the original author or authors.
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

public class Tracer {

    private volatile static boolean enable = false;

    public static void print(Object object) {
        if (enable) {
            System.out.print(object);
        }
    }

    public static void println(Object object) {
        if (enable) {
            System.out.println(object);
        }
    }

    public static void printf(String format, Object ... args) {
        if (enable) {
            System.out.printf(format, args);
        }
    }

    public static boolean isTraceEnabled() {
        return enable;
    }

    public static void enable() {
        enable = true;
    }

    public static void disable() {
        enable = false;
    }


}
