/*
 * Copyright (c) 2018 the original author or authors.
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

/**
 * @author cuiyi
 */
public abstract class Constant {

    public static final int SIZE = 4;

    public static final byte BOOLEAN_FALSE = 0x0;

    public static final byte BOOLEAN_TRUE = 0x1;

    public static final int BYTE_SIZE = Byte.SIZE / Byte.SIZE;

    public static final int CHAR_SIZE = Character.SIZE / Byte.SIZE;

    public static final int SHORT_SIZE = Short.SIZE / Byte.SIZE;

    public static final int INT_SIZE = Integer.SIZE / Byte.SIZE;

    public static final int LONG_SIZE = Long.SIZE / Byte.SIZE;

    public static final int CACHE_LINE_SIZE = Integer.getInteger("CACHE_LINE_SIZE", 64);

}
