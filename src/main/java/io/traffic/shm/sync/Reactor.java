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

import io.traffic.shm.file.MappedFile;
import io.traffic.util.*;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.traffic.util.Util.pageAlign;

public class Reactor implements Closeable {

    private final MappedFile mappedFile;
    private final int threads;
    private final int stackSize;
    private final long capacity;
    private final long address;
    private final int baseIndex;
    private final Metadata metadata;
    private final byte[] bitmap;
    private final AtomicBoolean running = new AtomicBoolean(false);


    private Reactor(MappedFile mappedFile, int threads, int stackSize, int baseIndex) {
        this.mappedFile = mappedFile;
        this.threads = threads;
        this.stackSize = pageAlign(stackSize);
        this.capacity = mappedFile.getSize();
        this.address = mappedFile.getAddress();
        this.baseIndex = baseIndex;
        this.bitmap = new byte[threads];
        this.metadata = new Metadata(this.address, this.threads, this.stackSize);
        this.metadata.bind();
        if (!this.running.compareAndSet(false, true)) {
            throw new IllegalStateException("Acceptor is already running");
        }
    }

    public static Reactor bind(String file, int threads, int stackSize) {
        int baseIndex = (int) Math.ceil((double) threads / (Constant.PAGE_SIZE - Metadata.ORIGIN_OFFSET));
        MappedFile mappedFile = MappedFile.with(file, true, (threads + baseIndex) * pageAlign(stackSize));
        return new Reactor(mappedFile, threads, stackSize, baseIndex);
    }

    public byte[] select() {
        UNSAFE.getBytes(address + Metadata.ORIGIN_OFFSET, bitmap, this.threads);
        return this.bitmap;
    }

    public Block get(int index) {
        this.metadata.mark(index, ACK.NULL);

        long offset = (baseIndex + index) * stackSize;

        if (UNSAFE.compareAndSwapInt(address + offset, ACK.REQ_FIN, ACK.RST)) {
            Block block = Block.deserialize(address + offset + Constant.INT_SIZE);
            block.setIndex(baseIndex + index);
            return block;
        }
        return null;
    }

    public void feedback(Block block) {
        Assert.notNull(block);
        if (block.getLength() > (stackSize - Constant.INT_SIZE * 2)) {
            throw new IndexOutOfBoundsException("payload length out of bounds: "+ block.getLength());
        }
        int index = CRC16.hash(Long.toString(Thread.currentThread().getId())) % threads;
        long offset = (baseIndex + index) * stackSize;

        if (block.getIndex() != -1) {
            index = block.getIndex();
            offset = index * stackSize;
        }

        block.serialize(address + offset, ACK.RES_SYN);
    }

    public boolean isRunning() {
        return this.running.get();
    }

    @Override
    public void close() throws IOException {
        this.running.set(false);

        if (mappedFile != null) {
            this.mappedFile.unmap();
        }
    }

}
