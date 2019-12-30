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
import io.traffic.util.UNSAFE;

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
    private final long mask;
    private final long address;
    private final Metadata metadata;
    private final Cursor readCursor;
    private final Cursor writeCursor;



    private Queue(MappedFile mappedFile, int id, int index) {
        this.mappedFile = mappedFile;
        this.id = id;
        this.index = index;
        this.capacity = mappedFile.getSize();
        this.mask = this.capacity - 1;
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
        metadata.initialize(this.id, this.index);
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
        Block block = Block.deserialize(this.capacity, this.address, read_abs);
        if (block != null) {
            long shift = read_abs + block.sizeof();
            long mode = read;

            if (shift > this.capacity) {
                shift = Metadata.ORIGIN_OFFSET + (shift & this.mask);
                mode = -mode;
            }
            long next = (mode < 0) ? -shift : shift;

            if (readCursor.update(read, next)) {
//                UNSAFE.storeFence();
                long prev = (capacity - read_abs) < Constant.INT_SIZE ? Metadata.ORIGIN_OFFSET : read_abs;
                UNSAFE.putIntVolatile(this.address + prev, 0);
                if (Trace.isTraceEnabled()) {
                    Trace.println(" read_shift=" + next + " FIN");
                }
                return block;
            }
            if (Trace.isTraceEnabled()) {
                Trace.println(" read_shift=" + next);
            }
        }
        return null;
    }

    private int write(long write, long read, Block block) {
        long mode = write;
        long write_abs = Math.abs(write);
        long read_abs = Math.abs(read);

        long shift = write_abs + block.sizeof();
        if (shift > this.capacity) {
            mode = -mode;
            shift = Metadata.ORIGIN_OFFSET + (shift & this.mask);;
        }

        long next = (mode < 0) ? -shift : shift;

        if (Trace.isTraceEnabled()) {
            Trace.print("read=" + read + " write=" + write + " length=" + block.sizeof() + " write_shift=" + next);
        }

        if (next * read < 0) {
            if (shift >= (read_abs - Constant.INT_SIZE)) {
                Trace.println(" OOB");
                return -1;
            }
        }

        if (writeCursor.update(write, next)) {
//            UNSAFE.storeFence();
            block.serialize(capacity, address, write_abs);
            if (Trace.isTraceEnabled()) {
                Trace.println(" FIN");
            }
            return 1;
        }
        if (Trace.isTraceEnabled()) {
            Trace.println("");
        }
        return 0;
    }

    public boolean reset() {
       return writeCursor.update(writeCursor.offset(), readCursor.offset());
    }
}
