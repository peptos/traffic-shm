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


import io.traffic.util.Assert;
import io.traffic.util.Constant;
import io.traffic.util.UNSAFE;

/**
 *
 *                                  | ------ 4 bytes aligned ------ |
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

    private final int length;
    private final byte[] payload;
    private int index = -1;

    public Block(byte[] payload) {
        Assert.notEmpty(payload);
        this.length = payload.length;
        this.payload = payload;
    }

    public void serialize(long address, short ack) {
        UNSAFE.putShort(address + Constant.SHORT_SIZE, ack);
        UNSAFE.putInt(address + Constant.INT_SIZE, this.length);
        UNSAFE.setBytes(this.payload, address + Constant.INT_SIZE * 2, this.length);
        UNSAFE.putShortVolatile(address, ack);
    }

    public static Block deserialize(long address) {
        int length = UNSAFE.getInt(address);
        address += Constant.INT_SIZE;

        byte[] payload = new byte[length];
        UNSAFE.getBytes(address, payload, length);

        return new Block(payload);
    }

    public byte[] getPayload() {
        return payload;
    }

    public int getLength() {
        return length;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
