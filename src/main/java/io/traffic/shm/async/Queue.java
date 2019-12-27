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
import io.traffic.util.Trace;

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
        this.metadata = new Metadata(this.address);
        this.readCursor = this.metadata.readCursor();
        this.writeCursor = this.metadata.writeCursor();
    }


    public static Queue map(String file, long size, int id, int index) {
        MappedFile mappedFile = MappedFile.with(file, size);
        return new Queue(mappedFile, id, index);
    }

    public void init() {
        metadata.initialize(id, index);
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
        if (Trace.isTraceEnabled()) {
            Trace.print("read=" + read + " write=" + write);
        }
        long read_abs = Math.abs(read);
        Block block = Block.deserialize(capacity, address, read_abs);

        long shift = read_abs + block.sizeof();
        long mode = read;

        if (shift >= capacity) {
            shift = Metadata.ORIGIN_OFFSET + shift % capacity;
            mode = -mode;
        }
        long next = (mode < 0) ? -shift : shift;
        if (readCursor.update(read, next)) {
            if (Trace.isTraceEnabled()) {
                Trace.println(" read shift=" + next + " FIN");
            }
            return block;
        }
        if (Trace.isTraceEnabled()) {
            Trace.println(" read shift=" + next);
        }
        return null;
    }

    private int write(long write, long read, Block block) {
        long mode = write;
        long write_abs = Math.abs(write);
        long read_abs = Math.abs(read);

        long available = capacity - Metadata.ORIGIN_OFFSET - Math.abs(write_abs - read_abs);
        if (write * read < 0) {
            available = Math.abs(write_abs - read_abs);
        }

        if ((available - Constant.INT_SIZE) < block.sizeof()) {
            return -1;
        }

        if (Trace.isTraceEnabled()) {
            Trace.print("read=" + read + " write=" + write + " available=" + (available - Constant.INT_SIZE) + " length=" + block.sizeof());
        }

        long shift = write_abs + block.sizeof();
        if (shift >= capacity) {
            mode = -mode;
            shift = Metadata.ORIGIN_OFFSET + shift % capacity;
        }

        long next = (mode < 0) ? -shift : shift;
        if (writeCursor.update(write, next)) {
            block.serialize(capacity, address, write_abs);
            if (Trace.isTraceEnabled()) {
                Trace.println(" write shift=" + next + " FIN");
            }
            return 1;
        }
        if (Trace.isTraceEnabled()) {
            Trace.println(" write shift=" + next);
        }
        return 0;
    }

    public boolean reset() {
       return writeCursor.update(writeCursor.offset(), readCursor.offset());
    }
}
