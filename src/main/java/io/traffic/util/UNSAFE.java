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

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.ByteOrder;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;

/**
 *
 * @author cuiyi
 */
public abstract class UNSAFE {

    private static final Unsafe unsafe;

    private static final boolean BIG_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;

    private static final boolean ALIGNED = aligned();

    public static final int CACHE_LINE_LENGTH = 64;

    private static final long UNSAFE_COPY_THRESHOLD = 1024L * 1024L;

    static {
        try {
            final PrivilegedExceptionAction<Unsafe> action = new PrivilegedExceptionAction<Unsafe>() {
                public Unsafe run() throws Exception {
                    Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                    theUnsafe.setAccessible(true);
                    return (Unsafe) theUnsafe.get(null);
                }
            };

            unsafe = AccessController.doPrivileged(action);
        } catch (Exception ex) {
            throw new RuntimeException("Unable to load unsafe", ex);
        }
    }

    public static Unsafe getUnsafe() {
        return unsafe;
    }


    public static final int OBJECT_REFERENCE_ALIGN = unsafe.arrayIndexScale(Object[].class);


    private static boolean aligned() {
        return !Boolean.getBoolean("UNALIGNED");
    }

    private static boolean supportUnalignedAccess() {
        String arch = AccessController.doPrivileged(new sun.security.action.GetPropertyAction("os.arch"));
        return arch.equals("i386") || arch.equals("x86") || arch.equals("amd64") || arch.equals("x86_64");
    }

    private static String getProperty(final String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key must not be empty.");
        }

        try {
            if (System.getSecurityManager() == null) {
                return System.getProperty(key);
            } else {
                return AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        return System.getProperty(key);
                    }
                });
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static ByteOrder nativeOrder() {
        return ByteOrder.nativeOrder();
    }

    public static ByteOrder getOrder() {
        return ByteOrder.BIG_ENDIAN;
    }

    public static int getAddressSize() {
        return unsafe.addressSize();
    }

    public static int getPageSize() {
        return unsafe.pageSize();
    }

    public static int arrayBaseOffset(Class clazz) {
        return unsafe.arrayBaseOffset(clazz);
    }

    public static int arrayIndexScale(Class clazz) {
        return unsafe.arrayIndexScale(clazz);
    }

    public static boolean is2BytesAligned(long address) {
        return (address & 0x01) == 0;
    }

    public static boolean is4BytesAligned(long address) {
        return (address & 0x03) == 0;
    }

    public static boolean is8BytesAligned(long address) {
        return (address & 0x07) == 0;
    }

    public static boolean isReferenceAligned(long address) {
        return (address & (OBJECT_REFERENCE_ALIGN - 1)) == 0;
    }

    public static void assertReferenceAligned(long address) {
        if (!isReferenceAligned(address)) {
            throw new IllegalArgumentException("Memory access to object references must be "
                    + OBJECT_REFERENCE_ALIGN + "-bytes aligned, but the address usecd was " + address);
        }
    }

    public static void assert2BytesAligned(long address) {
        if (!is2BytesAligned(address)) {
            throw new IllegalArgumentException("Atomic memory access must be aligned, but the address used was " + address);
        }
    }

    public static void assert4BytesAligned(long address) {
        if (!is4BytesAligned(address)) {
            throw new IllegalArgumentException("Atomic memory access must be aligned, but the address used was " + address);
        }
    }

    public static void assert8BytesAligned(long address) {
        if (!is8BytesAligned(address)) {
            throw new IllegalArgumentException("Atomic memory access must be aligned, but the address used was " + address);
        }
    }


    public static byte getByte(long address) {
        return unsafe.getByte(address);
    }

    public static void putByte(long address, byte value) {
        unsafe.putByte(address, value);
    }

    public static byte getByte(Object object, long offset) {
        return unsafe.getByte(object, offset);
    }

    public static void putByte(Object object, long offset, byte value) {
        unsafe.putByte(object, offset, value);
    }

    public static byte getByteVolatile(Object object, long offset) {
        return unsafe.getByteVolatile(object, offset);
    }

    public static void putByteVolatile(Object object, long offset, byte value) {
        unsafe.putByteVolatile(object, offset, value);
    }

    public static byte getByteVolatile(long address) {
        return unsafe.getByteVolatile(null, address);
    }

    public static void putByteVolatile(long address, byte value) {
        unsafe.putByteVolatile(null, address, value);
    }


    private static short getShortByByte(Object object, long offset, boolean bigEndian) {
        if (bigEndian) {
            return Bits.toShort(getByte(object, offset), getByte(object, offset + 1));
        } else {
            return Bits.toShort(getByte(object, offset + 1), getByte(object, offset));
        }
    }

    private static void putShortByByte(Object object, long offset, short value, boolean bigEndian) {
        if (bigEndian) {
            putByte(object, offset, Bits.short1(value));
            putByte(object, offset + 1, Bits.short0(value));
        } else {
            putByte(object, offset, Bits.short0(value));
            putByte(object, offset + 1, Bits.short1(value));
        }
    }

    private static short getShortByByte(long address, boolean bigEndian) {
        if (bigEndian) {
            return Bits.toShort(getByte(address), getByte(address + 1));
        } else {
            return Bits.toShort(getByte(address + 1), getByte(address));
        }
    }


    private static void putShortByByte(long address, short value, boolean bigEndian) {
        if (bigEndian) {
            putByte(address, Bits.short1(value));
            putByte(address + 1, Bits.short0(value));
        } else {
            putByte(address, Bits.short0(value));
            putByte(address + 1, Bits.short1(value));
        }
    }


    private static char getCharByByte(Object object, long offset, boolean bigEndian) {
        if (bigEndian) {
            return Bits.toChar(getByte(object, offset), getByte(object, offset + 1));
        } else {
            return Bits.toChar(getByte(object, offset + 1), getByte(object, offset));
        }
    }


    private static void putCharByByte(Object object, long offset, char value, boolean bigEndian) {
        if (bigEndian) {
            putByte(object, offset, Bits.char1(value));
            putByte(object, offset + 1, Bits.char0(value));
        } else {
            putByte(object, offset, Bits.char0(value));
            putByte(object, offset + 1, Bits.char1(value));
        }
    }

    private static char getCharByByte(long address, boolean bigEndian) {
        if (bigEndian) {
            return Bits.toChar(getByte(address), getByte(address + 1));
        } else {
            return Bits.toChar(getByte(address + 1), getByte(address));
        }
    }


    private static void putCharByByte(long address, char value, boolean bigEndian) {
        if (bigEndian) {
            putByte(address, Bits.char1(value));
            putByte(address + 1, Bits.char0(value));
        } else {
            putByte(address, Bits.char0(value));
            putByte(address + 1, Bits.char1(value));
        }
    }


    private static int getIntByByte(Object object, long offset, boolean bigEndian) {
        if (bigEndian) {
            return Bits.toInt(getByte(object, offset),
                    getByte(object, offset + 1),
                    getByte(object, offset + 2),
                    getByte(object, offset + 3));
        } else {
            return Bits.toInt(getByte(object, offset + 3),
                    getByte(object, offset + 2),
                    getByte(object, offset + 1),
                    getByte(object, offset));
        }
    }

    private static void putIntByByte(Object object, long offset, int value, boolean bigEndian) {
        if (bigEndian) {
            putByte(object, offset, Bits.int3(value));
            putByte(object, offset + 1, Bits.int2(value));
            putByte(object, offset + 2, Bits.int1(value));
            putByte(object, offset + 3, Bits.int0(value));
        } else {
            putByte(object, offset + 3, Bits.int3(value));
            putByte(object, offset + 2, Bits.int2(value));
            putByte(object, offset + 1, Bits.int1(value));
            putByte(object, offset, Bits.int0(value));
        }
    }

    private static int getIntByByte(long address, boolean bigEndian) {
        if (bigEndian) {
            return Bits.toInt(getByte(address),
                    getByte(address + 1),
                    getByte(address + 2),
                    getByte(address + 3));
        } else {
            return Bits.toInt(getByte(address + 3),
                    getByte(address + 2),
                    getByte(address + 1),
                    getByte(address));
        }
    }

    private static void putIntByByte(long address, int value, boolean bigEndian) {
        if (bigEndian) {
            putByte(address, Bits.int3(value));
            putByte(address + 1, Bits.int2(value));
            putByte(address + 2, Bits.int1(value));
            putByte(address + 3, Bits.int0(value));
        } else {
            putByte(address + 3, Bits.int3(value));
            putByte(address + 2, Bits.int2(value));
            putByte(address + 1, Bits.int1(value));
            putByte(address, Bits.int0(value));
        }
    }


    private static long getLongByByte(Object object, long offset, boolean bigEndian) {
        if (bigEndian) {
            return Bits.toLong(getByte(object, offset),
                    getByte(object, offset + 1),
                    getByte(object, offset + 2),
                    getByte(object, offset + 3),
                    getByte(object, offset + 4),
                    getByte(object, offset + 5),
                    getByte(object, offset + 6),
                    getByte(object, offset + 7));
        } else {
            return Bits.toLong(getByte(object, offset + 7),
                    getByte(object, offset + 6),
                    getByte(object, offset + 5),
                    getByte(object, offset + 4),
                    getByte(object, offset + 3),
                    getByte(object, offset + 2),
                    getByte(object, offset + 1),
                    getByte(object, offset));
        }
    }


    private static void putLongByByte(Object object, long offset, long value, boolean bigEndian) {
        if (bigEndian) {
            putByte(object, offset, Bits.long7(value));
            putByte(object, offset + 1, Bits.long6(value));
            putByte(object, offset + 2, Bits.long5(value));
            putByte(object, offset + 3, Bits.long4(value));
            putByte(object, offset + 4, Bits.long3(value));
            putByte(object, offset + 5, Bits.long2(value));
            putByte(object, offset + 6, Bits.long1(value));
            putByte(object, offset + 7, Bits.long0(value));
        } else {
            putByte(object, offset + 7, Bits.long7(value));
            putByte(object, offset + 6, Bits.long6(value));
            putByte(object, offset + 5, Bits.long5(value));
            putByte(object, offset + 4, Bits.long4(value));
            putByte(object, offset + 3, Bits.long3(value));
            putByte(object, offset + 2, Bits.long2(value));
            putByte(object, offset + 1, Bits.long1(value));
            putByte(object, offset, Bits.long0(value));
        }
    }


    private static long getLongByByte(long address, boolean bigEndian) {
        if (bigEndian) {
            return Bits.toLong(getByte(address),
                    getByte(address + 1),
                    getByte(address + 2),
                    getByte(address + 3),
                    getByte(address + 4),
                    getByte(address + 5),
                    getByte(address + 6),
                    getByte(address + 7));
        } else {
            return Bits.toLong(getByte(address + 7),
                    getByte(address + 6),
                    getByte(address + 5),
                    getByte(address + 4),
                    getByte(address + 3),
                    getByte(address + 2),
                    getByte(address + 1),
                    getByte(address));
        }
    }


    private static void putLongByByte(long address, long value, boolean bigEndian) {
        if (bigEndian) {
            putByte(address, Bits.long7(value));
            putByte(address + 1, Bits.long6(value));
            putByte(address + 2, Bits.long5(value));
            putByte(address + 3, Bits.long4(value));
            putByte(address + 4, Bits.long3(value));
            putByte(address + 5, Bits.long2(value));
            putByte(address + 6, Bits.long1(value));
            putByte(address + 7, Bits.long0(value));
        } else {
            putByte(address + 7, Bits.long7(value));
            putByte(address + 6, Bits.long6(value));
            putByte(address + 5, Bits.long5(value));
            putByte(address + 4, Bits.long4(value));
            putByte(address + 3, Bits.long3(value));
            putByte(address + 2, Bits.long2(value));
            putByte(address + 1, Bits.long1(value));
            putByte(address, Bits.long0(value));
        }
    }

    public static boolean getBoolean(Object object, long offset) {
        return unsafe.getBoolean(object, offset);
    }

    public static void putBoolean(Object object, long offset, boolean value) {
        unsafe.putBoolean(object, offset, value);
    }

    public static short getShort(Object object, long offset) {
        assert2BytesAligned(offset);
        if (ALIGNED) {
            short value = unsafe.getShort(object, offset);
            return (BIG_ENDIAN ? value : Bits.swap(value));
        }
        return getShortByByte(object, offset, true);
    }

    public static void putShort(Object object, long offset, short value) {
        assert2BytesAligned(offset);
        if (ALIGNED) {
            unsafe.putShort(object, offset, (BIG_ENDIAN ? value : Bits.swap(value)));
        } else {
            putShortByByte(object, offset, value, true);
        }
    }

    public static char getChar(Object object, long offset) {
        assert2BytesAligned(offset);
        if (ALIGNED) {
            char value = unsafe.getChar(object, offset);
            return (BIG_ENDIAN ? value : Bits.swap(value));
        }
        return getCharByByte(object, offset, true);
    }

    public static void putChar(Object object, long offset, char value) {
        assert2BytesAligned(offset);
        if (ALIGNED) {
            unsafe.putChar(object, offset, (BIG_ENDIAN ? value : Bits.swap(value)));
        } else {
            putCharByByte(object, offset, value, true);
        }
    }

    public static int getInt(Object object, long offset) {
        assert4BytesAligned(offset);
        if (ALIGNED) {
            int value = unsafe.getInt(object, offset);
            return (BIG_ENDIAN ? value : Bits.swap(value));
        }
        return getIntByByte(object, offset, true);
    }

    public static void putInt(Object object, long offset, int value) {
        assert4BytesAligned(offset);
        if (ALIGNED) {
            unsafe.putInt(object, offset, (BIG_ENDIAN ? value : Bits.swap(value)));
        } else {
            putIntByByte(object, offset, value, true);
        }
    }

    public static long getLong(Object object, long offset) {
        assert8BytesAligned(offset);
        if (ALIGNED) {
            long value = unsafe.getLong(object, offset);
            return (BIG_ENDIAN ? value : Bits.swap(value));
        }
        return getLongByByte(object, offset, true);
    }

    public static void putLong(Object object, long offset, long value) {
        assert8BytesAligned(offset);
        if (ALIGNED) {
            unsafe.putLong(object, offset, (BIG_ENDIAN ? value : Bits.swap(value)));
        } else {
            putLongByByte(object, offset, value, true);
        }
    }


    public static short getShort(long address) {
        assert2BytesAligned(address);
        if (ALIGNED) {
            short value = unsafe.getShort(address);
            return (BIG_ENDIAN ? value : Bits.swap(value));
        }
        return getShortByByte(address, true);
    }

    public static int getUnsignedShort(long address) {
        assert2BytesAligned(address);
        return getShort(address) & 0xffff;
    }

    public static void putShort(long address, short value) {
        assert2BytesAligned(address);
        if (ALIGNED) {
            unsafe.putShort(address, (BIG_ENDIAN ? value : Bits.swap(value)));
        } else {
            putShortByByte(address, value, true);
        }
    }

    public static void putUnsignedShort(long address, int value) {
        assert2BytesAligned(address);
        putShort(address, (short) value);
    }

    public static char getChar(long address) {
        assert2BytesAligned(address);
        if (ALIGNED) {
            char value = unsafe.getChar(address);
            return (BIG_ENDIAN ? value : Bits.swap(value));
        }
        return getCharByByte(address, true);
    }

    public static void putChar(long address, char value) {
        assert2BytesAligned(address);
        if (ALIGNED) {
            unsafe.putChar(address, (BIG_ENDIAN ? value : Bits.swap(value)));
        } else {
            putCharByByte(address, value, true);
        }
    }

    public static int getInt(long address) {
        assert4BytesAligned(address);
        if (ALIGNED) {
            int value = unsafe.getInt(address);
            return (BIG_ENDIAN ? value : Bits.swap(value));
        }
        return getIntByByte(address, true);
    }

    public static void putInt(long address, int value) {
        assert4BytesAligned(address);
        if (ALIGNED) {
            unsafe.putInt(address, (BIG_ENDIAN ? value : Bits.swap(value)));
        } else {
            putIntByByte(address, value, true);
        }
    }

    public static long getLong(long address) {
        assert8BytesAligned(address);
        if (ALIGNED) {
            long value = unsafe.getLong(address);
            return (BIG_ENDIAN ? value : Bits.swap(value));
        }
        return getLongByByte(address, true);
    }

    public static void putLong(long address, long value) {
        assert8BytesAligned(address);
        if (ALIGNED) {
            unsafe.putLong(address, (BIG_ENDIAN ? value : Bits.swap(value)));
        } else {
            putLongByByte(address, value, true);
        }
    }

    public static boolean compareAndSwapInt(Object object, long offset, int expected, int value) {
        assert4BytesAligned(offset);
        if (BIG_ENDIAN) {
            return unsafe.compareAndSwapInt(object, offset, expected, value);
        }
        return unsafe.compareAndSwapInt(object, offset, Bits.swap(expected), Bits.swap(value));
    }

    public static boolean compareAndSwapLong(Object object, long offset, long expected, long value) {
        assert8BytesAligned(offset);
        if (BIG_ENDIAN) {
            return unsafe.compareAndSwapLong(object, offset, expected, value);
        }
        return unsafe.compareAndSwapLong(object, offset, Bits.swap(expected), Bits.swap(value));
    }

    public static boolean compareAndSwapInt(long address, int expected, int value) {
        return compareAndSwapInt(null, address, expected, value);
    }

    public static boolean compareAndSwapLong(long address, long expected, long value) {
        return compareAndSwapLong(null, address, expected, value);
    }


    public static boolean getBooleanVolatile(Object object, long offset) {
        return unsafe.getBooleanVolatile(object, offset);
    }

    public static void putBooleanVolatile(Object object, long offset, boolean value) {
        unsafe.putBooleanVolatile(object, offset, value);
    }

    public static boolean getBooleanVolatile(long address) {
        return unsafe.getBooleanVolatile(null, address);
    }

    public static void putBooleanVolatile(long address, boolean value) {
        unsafe.putBooleanVolatile(null, address, value);
    }

    public static short getShortVolatile(Object object, long offset) {
        assert2BytesAligned(offset);
        short value = unsafe.getShortVolatile(object, offset);
        return (BIG_ENDIAN ? value : Bits.swap(value));
    }

    public static short getShortVolatile(long address) {
        return getShortVolatile(null, address);
    }

    public static void putShortVolatile(Object object, long offset, short value) {
        assert2BytesAligned(offset);
        if (BIG_ENDIAN) {
            unsafe.putShortVolatile(object, offset, value);
        } else {
            unsafe.putShortVolatile(object, offset, Bits.swap(value));
        }
    }

    public static void putShortVolatile(long address, short value) {
        putShortVolatile(null, address, value);
    }

    public static char getCharVolatile(Object object, long offset) {
        assert2BytesAligned(offset);
        char value = unsafe.getCharVolatile(object, offset);
        return (BIG_ENDIAN ? value : Bits.swap(value));
    }

    public static char getCharVolatile(long address) {
        return getCharVolatile(null, address);
    }

    public static void putCharVolatile(Object object, long offset, char value) {
        assert2BytesAligned(offset);
        if (BIG_ENDIAN) {
            unsafe.putCharVolatile(object, offset, value);
        } else {
            unsafe.putCharVolatile(object, offset, Bits.swap(value));
        }
    }

    public static void putCharVolatile(long address, char value) {
        putCharVolatile(null, address, value);
    }

    public static int getIntVolatile(Object object, long offset) {
        assert4BytesAligned(offset);
        int value = unsafe.getIntVolatile(object, offset);
        return (BIG_ENDIAN ? value : Bits.swap(value));
    }

    public static int getIntVolatile(long address) {
        return getIntVolatile(null, address);
    }

    public static void putIntVolatile(Object object, long offset, int value) {
        assert4BytesAligned(offset);
        if (BIG_ENDIAN) {
            unsafe.putIntVolatile(object, offset, value);
        } else {
            unsafe.putIntVolatile(object, offset, Bits.swap(value));
        }
    }

    public static void putIntVolatile(long address, int value) {
        putIntVolatile(null, address, value);
    }

    public static long getLongVolatile(Object object, long offset) {
        assert8BytesAligned(offset);
        long value = unsafe.getLongVolatile(object, offset);
        return (BIG_ENDIAN ? value : Bits.swap(value));
    }

    public static long getLongVolatile(long address) {
        return getLongVolatile(null, address);
    }

    public static void putLongVolatile(Object object, long offset, long value) {
        assert8BytesAligned(offset);
        if (BIG_ENDIAN) {
            unsafe.putLongVolatile(object, offset, value);
        } else {
            unsafe.putLongVolatile(object, offset, Bits.swap(value));
        }
    }

    public static void putLongVolatile(long address, long value) {
        putLongVolatile(null, address, value);
    }


    public static void putOrderedInt(Object object, long offset, int value) {
        assert4BytesAligned(offset);
        if (BIG_ENDIAN) {
            unsafe.putOrderedInt(object, offset, value);
        } else {
            unsafe.putOrderedInt(object, offset, Bits.swap(value));
        }
    }

    public static void putOrderedInt(long address, int value) {
        putOrderedInt(null, address, value);
    }

    public static void putOrderedLong(Object object, long offset, long value) {
        assert8BytesAligned(offset);
        if (BIG_ENDIAN) {
            unsafe.putOrderedLong(object, offset, value);
        } else {
            unsafe.putOrderedLong(object, offset, Bits.swap(value));
        }
    }

    public static void putOrderedLong(long address, long value) {
        putOrderedLong(null, address, value);
    }


    public static int getAndAddInt(Object object, long offset, int delta) {
        int value;
        do {
            value = getIntVolatile(object, offset);
        } while (!compareAndSwapInt(object, offset, value, value + delta));

        return value;
    }

    public static int getAndAddInt(long address, int delta) {
        return getAndAddInt(null, address, delta);
    }

    public static long getAndAddLong(Object object, long offset, long delta) {
        long value;
        do {
            value = getLongVolatile(object, offset);
        } while (!compareAndSwapLong(object, offset, value, value + delta));

        return value;
    }

    public static long getAndAddLong(long address, long delta) {
        return getAndAddLong(null, address, delta);
    }

    public static int getAndSetInt(Object object, long offset, int delta) {
        int value;
        do {
            value = getIntVolatile(object, offset);
        } while (!compareAndSwapInt(object, offset, value, delta));

        return value;
    }

    public static int getAndSetInt(long address, int delta) {
        return getAndSetInt(null, address, delta);
    }

    public static long getAndSetLong(Object object, long offset, long delta) {
        long value;
        do {
            value = getLongVolatile(object, offset);
        } while (!compareAndSwapLong(object, offset, value, delta));

        return value;
    }

    public static void copyMemory(Object src, long srcOffset, Object dst, long dstOffset, long length) {
        while (length > 0) {
            long size = Math.min(length, UNSAFE_COPY_THRESHOLD);
            unsafe.copyMemory(src, srcOffset, dst, dstOffset, size);
            length -= size;
            srcOffset += size;
            dstOffset += size;
        }
    }

    public static long getAndSetLong(long address, long delta) {
        return getAndSetLong(null, address, delta);
    }

    public static Object getObject(Object object, long offset) {
        assertReferenceAligned(offset);
        return UNSAFE.getObject(object, offset);
    }


}
