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

import io.traffic.util.UNSAFE;

/**
 * @author cuiyi
 */
public class Cursor {

    private final long address;
    private final int offset;

    public Cursor(long address, int offset) {
        this.address = address;
        this.offset = offset;
    }

    public static long rescale(long value, long min, long max) {
        // return min + ((value & Long.MAX_VALUE) - min) % (max - min);

        // The reason why the above code can not be used
        // is to reserve data in the range 0-256
        // after the cursor of Long type overflows
        return min + (value & Long.MAX_VALUE) % (max - min);
    }

    public boolean update(long expected, long value) {
        return UNSAFE.compareAndSwapLong(address + offset, expected, value);
    }

    public long offset() {
        return UNSAFE.getLongVolatile(address + offset);
    }
}
