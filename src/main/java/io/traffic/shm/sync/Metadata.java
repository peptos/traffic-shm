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

package io.traffic.shm.sync;

import io.traffic.util.CRC16;
import io.traffic.util.Constant;
import io.traffic.util.UNSAFE;


/**
 *
 *  | ---------------------------------------- metadata ----------------------------------------- |
 *  +-------------+---------------+---------------+---------------+-------------------------------+
 *  |0x414E|0x4E41|0x1|0x2|0x3|0x4|0x1|0x2|0x3|0x4|0x1|0x2|0x3|0x4|0x1|0x2|0x3| ---- bitMap ----- |
 *  +-------------+---------------+---------------+---------------+-------------------------------+
 *  |-magic number|-minor-|-major-| -- threads -- | - stackSize - |
 *  |             | -- version -- |               |
 *  0             4               8               12             16
 *
 * @author cuiyi
 */
public class Metadata {

    public static final int ORIGIN_OFFSET = 16;

    private static final int MAGIC_NUMBER = 0x414E4E41;
    private static final int MAGIC_NUMBER_OFFSET = 0;
    private static final int MAJOR_VERSION_OFFSET = 6;
    private static final int THREADS_OFFSET = 8;
    private static final int STACK_SIZE_OFFSET = 12;
    private static final int BITMAP_OFFSET = 16;

    private final long address;
    private final int threads;
    private final int stackSize;

    public Metadata(long address, int threads, int stackSize) {
        this.address = address;
        this.threads = threads;
        this.stackSize = stackSize;
    }

    public void bind() {
        UNSAFE.putInt(address + MAGIC_NUMBER_OFFSET, MAGIC_NUMBER);
        setMajorVersion(CRC16.hash(Constant.MAJOR_VERSION_SYNC));
        setThreads(this.threads);
        setStackSize(this.stackSize);
    }

    public void map() {
        if (CRC16.hash(Constant.MAJOR_VERSION_SYNC) != getMajorVersion()) {
            throw new IllegalArgumentException("Illegal Major Version Argument Exception");
        }
        if (this.threads != getThreads()) {
            throw new IllegalArgumentException("Illegal Threads Argument Exception");
        }
        if (this.stackSize != getStackSize()) {
            throw new IllegalArgumentException("Illegal Stack Size Argument Exception");
        }
    }

    public void mark(int index, byte value) {
        UNSAFE.putByteVolatile(address + BITMAP_OFFSET + index, value);
    }


    public void setMajorVersion(int value) {
        UNSAFE.putUnsignedShort(address + MAJOR_VERSION_OFFSET, value);
    }

    public void setThreads(int threads) {
        UNSAFE.putOrderedInt(address + THREADS_OFFSET, threads);
    }

    public void setStackSize(int stackSize) {
        UNSAFE.putOrderedInt(address + STACK_SIZE_OFFSET, stackSize);
    }

    public int getMajorVersion() {
        return UNSAFE.getUnsignedShort(address + MAJOR_VERSION_OFFSET);
    }

    public int getThreads() {
        return UNSAFE.getInt(address + THREADS_OFFSET);
    }

    public int getStackSize() {
        return UNSAFE.getInt(address + STACK_SIZE_OFFSET);
    }
}
