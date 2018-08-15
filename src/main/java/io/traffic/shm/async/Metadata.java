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

package io.traffic.shm.async;

import io.traffic.util.Constant;
import io.traffic.util.UNSAFE;

/**
 *
 *  | ------------------------------------------------------------------------------- metadata ------------------------------------------------------------------------------ |
 *  +-------------+---------------+---------------+---------------+---------------------+-------------------------------+---------------------+-------------------------------+
 *  |0x414E|0x4E41|0x1|0x2|0x3|0x4|0x1|0x2|0x3|0x4|0x1|0x2|0x3|0x4|- cache line padding-|0x1|0x2|0x3|0x4|0x5|0x6|0x7|0x8|- cache line padding-|0x1|0x2|0x3|0x4|0x5|0x6|0x7|0x8|
 *  +-------------+---------------+---------------+---------------+---------------------+-------------------------------+---------------------+-------------------------------+
 *  |-magic number|-minor-|-major-| ----- id ---- | --- index --- |                     |-------- read  cursor -------- |                     | -------- write cursor-------- |
 *  |             | -- version -- |               |
 *  0             4               8               12
 *
 * @author cuiyi
 */
public class Metadata {

    public static final int ORIGIN_OFFSET = Constant.CACHE_LINE_SIZE * 4;

    private static final int MAGIC_NUMBER = 0x414E4E41;
    private static final int MAGIC_NUMBER_OFFSET = 0;
    private static final int MINOR_VERSION_OFFSET = 4;
    private static final int MAJOR_VERSION_OFFSET = 6;
    private static final int ID_OFFSET = 8;
    private static final int INDEX_OFFSET = 12;
    private static final int READ_OFFSET = Constant.CACHE_LINE_SIZE;
    private static final int WRITE_OFFSET = Constant.CACHE_LINE_SIZE * 3;

    private static final int READ_INITIAL_VALUE = ORIGIN_OFFSET;
    private static final int WRITE_INITIAL_VALUE = ORIGIN_OFFSET;

    private final long address;
    private final Cursor read;
    private final Cursor write;


    public Metadata(long address) {
        this.address = address;
        this.read = new Cursor(this.address, READ_OFFSET);
        this.write = new Cursor(this.address, WRITE_OFFSET);
    }


    public void initialize(int id, int index) {
        UNSAFE.putInt(address + MAGIC_NUMBER_OFFSET, MAGIC_NUMBER);

        if (setId(id) && setIndex(index)
                && read.update(0, Metadata.READ_INITIAL_VALUE)
                && write.update(0, Metadata.WRITE_INITIAL_VALUE)) {
        }
    }

    public boolean setId(int id) {
        return UNSAFE.compareAndSwapInt(address + ID_OFFSET, 0, id);
    }

    public int getId() {
        return UNSAFE.getIntVolatile(address + ID_OFFSET);
    }

    public boolean setIndex(int index) {
        return UNSAFE.compareAndSwapInt(address + INDEX_OFFSET, 0, index);
    }

    public int getIndex() {
        return UNSAFE.getIntVolatile(address + INDEX_OFFSET);
    }

    public String getVersion() {
        int minor = UNSAFE.getUnsignedShort(address + MINOR_VERSION_OFFSET);
        int major = UNSAFE.getUnsignedShort(address + MAJOR_VERSION_OFFSET);
        return major + "." + minor;
    }

    public Cursor readCursor() {
        return read;
    }

    public Cursor writeCursor() {
        return write;
    }
}
