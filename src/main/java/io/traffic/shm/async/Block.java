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
public final class Block {

    private final int length;
    private final byte[] payload;

    public Block(byte[] payload) {
        Assert.notEmpty(payload);
        this.length = payload.length;
        this.payload = payload;
    }

    public void serialize(long capacity, long address, long offset) {
        long available = capacity - offset;

        if (available >= sizeof()) {
            // no overflow
            serialize(address, offset, available, capacity);
        } else {
            if (available < Constant.INT_SIZE) {
                // whole block overflow
                serialize(address, Metadata.ORIGIN_OFFSET, available, capacity);
            } else if (available == Constant.INT_SIZE) {
                // put length in, but payload overflow
                serialize(address, offset, available, capacity);
            } else {
                // payload overflow
                withLength(address, offset);
                offset += Constant.INT_SIZE;
                available -= Constant.INT_SIZE;

                if (available > 0) {
                    UNSAFE.setBytes(payload, address + offset, available);
                }
                UNSAFE.setBytes(payload, available, address + Metadata.ORIGIN_OFFSET, length - available);
            }
        }
    }

    public void serialize(long address, long offset, long available, long capacity) {
        withLength(address, offset);
        offset += Constant.INT_SIZE;
        if (available == Constant.INT_SIZE) {
            withPayload(address, Metadata.ORIGIN_OFFSET);
        } else {
            withPayload(address, offset);
        }
    }

    private void withLength(long address, long offset) {
        UNSAFE.putIntVolatile(address + offset, this.length);
    }

    private void withPayload(long address, long offset) {
        UNSAFE.setBytes(this.payload, address + offset, this.length);
    }

    public static Block deserialize(long capacity, long address, long offset) {
        long available = capacity - offset;
        if (available < Constant.INT_SIZE) {
            return deserialize(address, Metadata.ORIGIN_OFFSET, capacity - Metadata.ORIGIN_OFFSET, capacity);
        } else {
            return deserialize(address, offset, available, capacity);
        }
    }

    private static Block deserialize(long address, long offset, long available, long capacity) {
        int length = UNSAFE.getIntVolatile(address + offset);

        if (length <= 0 || length > capacity) {
            return null;
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

    public long sizeof() {
        return cost(length);
    }

    private static long align(long length) {
        return Util.align(length, Constant.SIZE);
    }

    private static long cost(long length) {
        return Constant.INT_SIZE + align(length);
    }

    public byte[] getPayload() {
        return payload;
    }
}
