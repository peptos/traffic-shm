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

package io.traffic.shm.test;

import io.traffic.shm.sync.Transport;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TestTransport {

    @Test
    public void testSubmit() throws Exception {
        int threads = 512;
        int executorThreads = 512;
        final Transport transport = Transport.map("/Users/peptos/sshm", threads, 2000);
        final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);
        final AtomicLong value = new AtomicLong();

        for (int i = 0; i < executorThreads; i++) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Create raw data.
                        Map<String, Object> data = new HashMap<String, Object>();
                        data.put("req-thread", Thread.currentThread().getName());
                        data.put("res-thread", "");
                        data.put("begin", System.nanoTime());
                        data.put("end", 0);
                        data.put("req-num", value.incrementAndGet());
                        data.put("res-num", -1);
                        data.put("duration", 0);

                        // Convert Map to byte array
                        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                        ObjectOutputStream out = new ObjectOutputStream(byteOut);
                        out.writeObject(data);

                        io.traffic.shm.sync.Block para = new io.traffic.shm.sync.Block(byteOut.toByteArray());
                        io.traffic.shm.sync.Block block = transport.submit(para, 10, TimeUnit.SECONDS);

                        // Parse byte array to Map
                        ByteArrayInputStream byteIn = new ByteArrayInputStream(block.getPayload());
                        ObjectInputStream in = new ObjectInputStream(byteIn);
                        Map<String, Object> res = (Map<String, Object>) in.readObject();
                        System.out.println(res);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        Thread.sleep(100000);
    }
}
