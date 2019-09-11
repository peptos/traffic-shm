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

package io.traffic.shm.file;

import io.traffic.util.UNSAFE;
import io.traffic.util.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author cuiyi
 */
public class MappedFile {

    private final RandomAccessFile raf;
    private final FileChannel channel;
    private final long size;
    private final long address;

    private final AtomicBoolean closed = new AtomicBoolean();

    private MappedFile(RandomAccessFile raf, long size) {
        this.raf = raf;
        this.size = pageAlign(size);
        this.channel = raf.getChannel();
        this.address = map();
    }

    public static MappedFile with(String file, long size) {
        return with(new File(file), size);
    }

    public static MappedFile with(File file, long size) {
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs();
        }

        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "rw");
        } catch (FileNotFoundException ex) {
            throw new IllegalArgumentException("mapped file not found", ex);
        }

        return new MappedFile(raf, size);
    }

    public long getSize() {
        return size;
    }

    public long getAddress() {
        return address;
    }

    private long map() {
        if (closed.get()) {
            throw new IllegalArgumentException("MappedFile has been closed");
        }
        try {
            FileLock lock = channel.lock();
            try {
                raf.setLength(size);
            } finally {
                lock.release();
            }
            return map0(channel, FileChannel.MapMode.READ_WRITE, 0L, size);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void unmap() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        try {
            channel.force(true);
            unmap0(channel, address, size);

            raf.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static long map0(FileChannel fileChannel, FileChannel.MapMode mode, long position, long size)  {
        if (position < 0) {
            throw new IllegalArgumentException("Attempt to access a negative position: " + position);
        }

        if (Util.isWindows() && size > 4L << 30) {
            throw new IllegalArgumentException("Mapping more than 4096 MB is unusable on Windows, size = " + (size >> 20) + " MiB");
        }

        Method map0 = Util.getMethod(fileChannel.getClass(), "map0", int.class, long.class, long.class);
        return (Long) Util.invokeMethod(map0, fileChannel, modeFor(mode), mapAlign(position), pageAlign(size));
    }

    private static void unmap0(FileChannel fileChannel, long address, long size) {
        Method ummap0 = Util.getMethod(fileChannel.getClass(), "unmap0", long.class, long.class);
        Util.invokeMethod(ummap0, null, address, pageAlign(size));
    }

    private static int modeFor(FileChannel.MapMode mapMode) {
        int mode = -1;
        if (FileChannel.MapMode.READ_ONLY.equals(mapMode)) {
            mode = 0;
        } else if (FileChannel.MapMode.READ_WRITE.equals(mapMode)) {
            mode = 1;
        } else if (FileChannel.MapMode.PRIVATE.equals(mapMode)) {
            mode = 2;
        }
        assert (mode >= 0);
        return mode;
    }

    private static long pageAlign(long size) {
        return Util.align(size, pageSize());
    }

    private static int pageSize() {
        return UNSAFE.getPageSize();
    }

    private static long mapAlignment() {
        return Util.isWindows() ? 64 << 10 : pageSize();
    }

    private static long mapAlign(long position) {
        return Util.align(position, mapAlignment());
    }

}
