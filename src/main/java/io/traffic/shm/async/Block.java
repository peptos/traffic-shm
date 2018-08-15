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
import io.traffic.util.Assert;
import io.traffic.util.UNSAFE;
import io.traffic.util.Util;

/**
 *
 *                  | ------ 4 bytes aligned ------ |
 *  +---------------+---------------+---------------+---------------+
 *  |0x1|0x2|0x3|0x4|0x1|0x2|0x3|0x4|0x1|0x2|0x3|0x4|0x1|0x2|0x3|   |
 *  +---------------+---------------+---------------+---------------+
 *  | ---- ACK ---  | --- length -- | ---------- payload ---------- |
 *  | --------------------------- block --------------------------- |
 *
 *
 * @author cuiyi
 */
public class Block {

    private static final int PADDING = Constant.INT_SIZE * 2;
    private static final int BYTE_ARRAY_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);

    private final int length;
    private final byte[] payload;

    public Block(byte[] payload) {
        Assert.notEmpty(payload);
        this.length = payload.length;
        this.payload = payload;
    }


    public void serialize(long capacity, long address, long offset) {
        if (offset + sizeof() <= capacity) {
            //no overflow
            serialize(address + offset);
            return;
        } else {
            long available = capacity - offset;
            if (available < Constant.INT_SIZE) {
                //whole block overflow
                serialize(address + Metadata.ORIGIN_OFFSET);
                return;
            } else if (available >= Constant.INT_SIZE && available < PADDING) {
                //put ack in, length & payload overflow
                withACK(address + offset);
                withPayload(withLength(address + Metadata.ORIGIN_OFFSET));
                ack(address + offset);
                return;
            } else {
                //payload overflow
                withLength(withACK(address + offset));
                available -= PADDING;
                if (available > 0) {
                    setBytes(payload, address + offset + PADDING, available);
                }
                setBytes(payload, available, address + Metadata.ORIGIN_OFFSET, length - available);
                ack(address + offset);
                return;
            }
        }
    }

    public synchronized static Block deserialize(long capacity, long address, long offset) {
        long available = capacity - offset;
        if (available < Constant.INT_SIZE) {
            return deserialize(address + Metadata.ORIGIN_OFFSET);
        } else {
            int length = getInt(address + offset);
            offset += Constant.INT_SIZE;
            available -= Constant.INT_SIZE;

            byte[] payload = new byte[length];

            if (available >= length) {
                getBytes(address + offset, payload, length);
                return new Block(payload);
            } else {
                if (available > 0) {
                    getBytes(address + offset, payload, available);
                }
                getBytes(address + Metadata.ORIGIN_OFFSET, payload, available, length - available);
                return new Block(payload);
            }
        }
    }

    private void serialize(long address) {
        withPayload(withLength(withACK(address)));
        ack(address);
    }

    private static Block deserialize(long address) {
        int length = getInt(address);
        address += Constant.INT_SIZE;

        byte[] payload = new byte[length];
        getBytes(address, payload, length);

        return new Block(payload);
    }

    public long sizeof() {
        return cost(length);
    }

    public byte[] getPayload() {
        return payload;
    }

    private long withACK(long address) {
        UNSAFE.putInt(address, ACK.SEGMENT);
        return address + Constant.INT_SIZE;
    }

    private long withLength(long address) {
        putInt(address, this.length);
        return address + Constant.INT_SIZE;
    }

    private long withPayload(long address) {
        setBytes(this.payload, address, this.length);
        return address + align(this.length);
    }

    private long ack(long address) {
        UNSAFE.putOrderedInt(address, ACK.DATA);
        return address + Constant.INT_SIZE;
    }


    private static long align(long length) {
        return Util.align(length, Constant.SIZE);
    }

    private static long cost(long length) {
        return align(length) + PADDING;
    }

    private static void putInt(long address, int value) {
        UNSAFE.putInt(address, value);
    }

    private static int getInt(long address) {
        return UNSAFE.getInt(address);
    }

    private static void setBytes(Object src, long address, long length) {
        UNSAFE.copyMemory(src, BYTE_ARRAY_OFFSET, null, address, length);
    }

    private static void getBytes(long address, Object dst, long length) {
        UNSAFE.copyMemory(null, address, dst, BYTE_ARRAY_OFFSET, length);
    }

    private static void setBytes(Object src, long srcOffset, long address, long length) {
        UNSAFE.copyMemory(src, BYTE_ARRAY_OFFSET + srcOffset, null, address, length);
    }

    private static void getBytes(long address, Object dst, long dstOffset, long length) {
        UNSAFE.copyMemory(null, address, dst, BYTE_ARRAY_OFFSET + dstOffset, length);
    }

}
