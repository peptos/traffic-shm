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

import io.traffic.util.*;


/**
 *
 *                  | ------ 4 bytes aligned ------ |
 *  +---------------+---------------+---------------+
 *  |0x1|0x2|0x3|0x4|0x1|0x2|0x3|0x4|0x1|0x2|0x3|   |
 *  +---------------+---------------+---------------+
 *  | --- length -- | ---------- payload ---------- |
 *  | ------------------- block ------------------- |
 *
 *
 * @author cuiyi
 */
public class Block {

    private final int length;
    private final byte[] payload;

    public Block(byte[] payload) {
        Assert.notEmpty(payload);
        this.length = payload.length;
        this.payload = payload;
    }

    public void serialize(long capacity, long address, long offset) {
        if (offset + sizeof() <= capacity) {
            // no overflow
            serialize(address + offset);
        } else {
            long available = capacity - offset;
            if (available < Constant.INT_SIZE) {
                // whole block overflow
                serialize(address + Metadata.ORIGIN_OFFSET);
            } else if (available == Constant.INT_SIZE) {
                // put length in, but payload overflow
                withLength(address + offset);
                withPayload(address + Metadata.ORIGIN_OFFSET);
            } else {
                // payload overflow
                withLength((address + offset));
                available -= Constant.INT_SIZE;;
                if (available > 0) {
                    UNSAFE.setBytes(payload, address + offset + Constant.INT_SIZE, available);
                }
                UNSAFE.setBytes(payload, available, address + Metadata.ORIGIN_OFFSET, length - available);
            }
        }
    }

    public static Block deserialize(long capacity, long address, long offset) {
        long available = capacity - offset;
        if (Trace.isTraceEnabled()) {
            Trace.print(" nowrap available=" + available);
        }

        if (available < Constant.INT_SIZE) {
            return deserialize(capacity, address + Metadata.ORIGIN_OFFSET);
        } else {
            int length = UNSAFE.getInt(address + offset);
            if (length > capacity) {
                throw new IndexOutOfBoundsException("payload length out of bounds: " + length);
            }
            if (Trace.isTraceEnabled()) {
                Trace.print(" length=" + length);
            }
            offset += Constant.INT_SIZE;
            available -= Constant.INT_SIZE;

            byte[] payload = new byte[length];

            if (available >= length) {
                UNSAFE.getBytes(address + offset, payload, length);
            } else {
                if (available > 0) {
                    UNSAFE.getBytes(address + offset, payload, available);
                }
                UNSAFE.getBytes(address + Metadata.ORIGIN_OFFSET, payload, available, length - available);
            }
            return new Block(payload);
        }
    }

    private void serialize(long address) {
        withPayload(withLength(address));
    }

    private static Block deserialize(long capacity, long address) {
        int length = UNSAFE.getInt(address);
        address += Constant.INT_SIZE;

        if (length > capacity) {
            throw new IndexOutOfBoundsException("payload length out of bounds: " + length);
        }
        if (Trace.isTraceEnabled()) {
            Trace.print(" length=" + length);
        }

        byte[] payload = new byte[length];
        UNSAFE.getBytes(address, payload, length);

        return new Block(payload);
    }

    public long sizeof() {
        return cost(length);
    }

    public byte[] getPayload() {
        return payload;
    }

    private long withLength(long address) {
        UNSAFE.putOrderedInt(address, this.length);
        return address + Constant.INT_SIZE;
    }

    private void withPayload(long address) {
        UNSAFE.setBytes(this.payload, address, this.length);
    }

    private static long align(long length) {
        return Util.align(length, Constant.SIZE);
    }

    private static long cost(long length) {
        return Constant.INT_SIZE + align(length);
    }

}
