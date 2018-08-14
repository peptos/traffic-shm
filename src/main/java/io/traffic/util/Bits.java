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

/**
 * @author cuiyi
 */
public abstract class Bits {

    static short swap(short value) {
        return Short.reverseBytes(value);
    }

    static char swap(char value) {
        return Character.reverseBytes(value);
    }

    static int swap(int value) {
        return Integer.reverseBytes(value);
    }

    static long swap(long value) {
        return Long.reverseBytes(value);
    }

    static char toChar(char value) {
        return (char) ((value << 8) | ((value & 0xFF00) >> 8));
    }

    static short toShort(short value) {
        return (short) ((value << 8) | ((value & 0xFF00) >> 8));
    }

    static int toInt(int value) {
        return (((value) << 24) |
                ((value << 8) & 0xFF0000) |
                ((value >> 8) & 0xFF00) |
                ((value >>> 24)));
    }

    static long toLong(long value) {
        long b0 = (value >>  0) & 0xff;
        long b1 = (value >>  8) & 0xff;
        long b2 = (value >> 16) & 0xff;
        long b3 = (value >> 24) & 0xff;
        long b4 = (value >> 32) & 0xff;
        long b5 = (value >> 40) & 0xff;
        long b6 = (value >> 48) & 0xff;
        long b7 = (value >> 56) & 0xff;

        return b0 << 56 |
                b1 << 48 |
                b2 << 40 |
                b3 << 32 |
                b4 << 24 |
                b5 << 16 |
                b6 <<  8 |
                b7 <<  0;
    }

    static char toChar(byte b1, byte b0) {
        return (char) ((b1 << 8) | (b0 & 0xff));
    }
    
    static short toShort(byte b1, byte b0) {
        return (short) ((b1 << 8) | (b0 & 0xff));
    }

    static int toInt(byte b3, byte b2, byte b1, byte b0) {
        return (((b3) << 24) |
                ((b2 & 0xff) << 16) |
                ((b1 & 0xff) << 8) |
                ((b0 & 0xff)));
    }

    static long toLong(byte b7, byte b6, byte b5, byte b4,
                       byte b3, byte b2, byte b1, byte b0) {
        return ((((long) b7) << 56) |
                (((long) b6 & 0xff) << 48) |
                (((long) b5 & 0xff) << 40) |
                (((long) b4 & 0xff) << 32) |
                (((long) b3 & 0xff) << 24) |
                (((long) b2 & 0xff) << 16) |
                (((long) b1 & 0xff) << 8) |
                (((long) b0 & 0xff)));
    }


    static byte short1(short x) {
        return (byte) (x >> 8);
    }

    static byte short0(short x) {
        return (byte) (x);
    }

    static byte char1(char x) {
        return (byte) (x >> 8);
    }

    static byte char0(char x) {
        return (byte) (x);
    }


    static byte int3(int x) {
        return (byte) (x >> 24);
    }

    static byte int2(int x) {
        return (byte) (x >> 16);
    }

    static byte int1(int x) {
        return (byte) (x >> 8);
    }

    static byte int0(int x) {
        return (byte) (x);
    }


    static byte long7(long x) {
        return (byte) (x >> 56);
    }

    static byte long6(long x) {
        return (byte) (x >> 48);
    }

    static byte long5(long x) {
        return (byte) (x >> 40);
    }

    static byte long4(long x) {
        return (byte) (x >> 32);
    }

    static byte long3(long x) {
        return (byte) (x >> 24);
    }

    static byte long2(long x) {
        return (byte) (x >> 16);
    }

    static byte long1(long x) {
        return (byte) (x >> 8);
    }

    static byte long0(long x) {
        return (byte) (x);
    }

}

