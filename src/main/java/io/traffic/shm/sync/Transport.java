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
import io.traffic.util.CRC16;
import io.traffic.util.Constant;
import io.traffic.util.UNSAFE;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.traffic.util.Util.pageAlign;


public class Transport implements Closeable {

    private final MappedFile mappedFile;
    private final int threads;
    private final int stackSize;
    private final long capacity;
    private final long address;
    private final int baseIndex;
    private final Metadata metadata;

    private final ConcurrentMap<Integer, Object> locks = new ConcurrentHashMap<Integer, Object>();


    private Transport(MappedFile mappedFile, int threads, int stackSize, int baseIndex) {
        this.mappedFile = mappedFile;
        this.threads = threads;
        this.stackSize = pageAlign(stackSize);
        this.capacity = mappedFile.getSize();
        this.address = mappedFile.getAddress();
        this.baseIndex = baseIndex;
        this.metadata = new Metadata(this.address, this.threads, this.stackSize);
        for (int i = 0; i < threads; i++) {
            locks.put(i, new byte[0]);
        }
        this.metadata.map();
    }

    public static Transport map(String file, int threads, int stackSize) {
        int baseIndex = (int) Math.ceil((double) threads / (Constant.PAGE_SIZE - Metadata.ORIGIN_OFFSET));
        MappedFile mappedFile = MappedFile.with(file, (threads + baseIndex) * pageAlign(stackSize));
        return new Transport(mappedFile, threads, stackSize, baseIndex);
    }

    @Override
    public void close() throws IOException {
        if (mappedFile != null) {
            this.mappedFile.unmap();
        }
    }

    public Block submit(Block block, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        int index = CRC16.hash(Long.toString(Thread.currentThread().getId())) % threads;
        long offset = (baseIndex + index) * stackSize;

        synchronized (locks.get(index)) {
            block.serialize(address + offset, ACK.REQ_SYN);
            this.metadata.mark(index, ACK.ESTABLISHED);

            long nanos = unit.toNanos(timeout);
            if (nanos > 0L) {
                long dl = System.nanoTime() + nanos;
                long deadline = (dl == 0L) ? 1L : dl; // avoid 0

                while (nanos > 0L) {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    if (ACK.RES_FIN == UNSAFE.getIntVolatile(address + offset)) {
                        return Block.deserialize(address + offset + Constant.INT_SIZE);
                    }
                    nanos = deadline - System.nanoTime();
                }
            }
            throw new TimeoutException();
        }
    }
}
