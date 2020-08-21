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

import io.traffic.shm.file.MappedFile;
import io.traffic.util.Assert;
import io.traffic.util.Constant;
import io.traffic.util.Tracer;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author cuiyi
 */
public class Queue implements Closeable {

    private final MappedFile mappedFile;
    private final int id;
    private final int index;
    private final long capacity;
    private final long address;
    private final Metadata metadata;
    private final Cursor readCursor;
    private final Cursor writeCursor;



    private Queue(MappedFile mappedFile, int id, int index) {
        this.mappedFile = mappedFile;
        this.id = id;
        this.index = index;
        this.capacity = mappedFile.getSize();
        this.address = mappedFile.getAddress();
        this.metadata = new Metadata(this.capacity, this.address);
        this.readCursor = this.metadata.readCursor();
        this.writeCursor = this.metadata.writeCursor();
    }

    public static Queue map(String file, long size) {
        return map(file, size, 0, 0);
    }

    public static Queue map(String file, long size, int id, int index) {
        Queue queue = new Queue(MappedFile.with(file, size), id, index);
        queue.init();
        return queue;
    }

    private void init() {
        metadata.initialize(this.id, this.index);
    }

    public static Queue attach(String file) {
        return attach(file, 0, 0);
    }

    public static Queue attach(String file, int id, int index) {
        Queue queue = new Queue(MappedFile.as(file), id, index);
        return queue;
    }

    @Override
    public void close() throws IOException {
        if (mappedFile != null) {
            this.mappedFile.unmap();
        }
    }

    public Block poll() {
        return read(readCursor.offset(), writeCursor.offset());
    }

    public boolean add(Block block) {
        if (offer(block)) {
            return true;
        } else {
            throw new IllegalStateException();
        }
    }

    public boolean offer(Block block) {
        Assert.notNull(block);
        Assert.notNull(block.getPayload());

        return 1 == write(writeCursor.offset(), readCursor.offset(), block);
    }

    private Block read(long read, long write) {
        if (read == write) {
            return null;
        }

        long offset = rescale(read);
        Block block = Block.deserialize(this.capacity, this.address, offset);
        if (block == null) {
            return null;
        }

        long shift = read + block.sizeof();
        if (readCursor.update(read, shift)) {
            if (Tracer.isTraceEnabled()) {
                Tracer.println("R=" + read + " W=" + write + " r=" + rescale(read) + " w=" + rescale(write)
                        + " l=" + block.getPayload().length + " RS=" + shift + " rs=" + rescale(shift) + " FIN");
            }
            return block;
        }
        return null;
    }

    private int write(long write, long read, Block block) {
        long available = this.capacity - Metadata.ORIGIN_OFFSET - write + read;

        if (block.sizeof() > available - Constant.INT_SIZE) {
            return -1;
        }

        long shift = write + block.sizeof();
        if (writeCursor.update(write, shift)) {
            long offset = rescale(write);
            block.serialize(capacity, address, offset);
            if (Tracer.isTraceEnabled()) {
                Tracer.println("W=" + write + " R=" + read + " w=" + rescale(write) + " r=" + rescale(read)
                        + " l=" + block.sizeof() + " WS=" + shift + " ws=" + rescale(shift) + " FIN");
            }
            return 1;
        }
        return 0;
    }

    public boolean reset() {
       return writeCursor.update(writeCursor.offset(), readCursor.offset());
    }


    private long rescale(long value) {
        return Cursor.rescale(value, Metadata.ORIGIN_OFFSET, this.capacity);
    }
}
