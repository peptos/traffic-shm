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

import io.traffic.shm.sync.ACK;
import io.traffic.shm.sync.Reactor;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

public class TestReactor {

    @Test
    public void testBind() throws Exception {
        Reactor reactor = Reactor.bind("/Users/peptos/sshm", 2, 2000);

        reactor.close();
    }

    @Test
    public void testReactor() throws Exception {
        Reactor acceptor = Reactor.bind("/Users/peptos/sshm", 2, 2000);
        byte[] bitmap = acceptor.select();
        System.out.println(new String(bitmap));

        acceptor.close();
    }

    @Test
    public void testAcceptAndResponse() throws Exception {
        int threads = 512;
        final Reactor reactor = Reactor.bind("/Users/peptos/sshm", threads, 2000);
        final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);

        final AtomicLong value = new AtomicLong();
        while (true) {
            byte[] bitmap = reactor.select();
            for (int i = 0; i < bitmap.length; i++) {
                if (ACK.ESTABLISHED == bitmap[i]) {
                    final int index = i;
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    io.traffic.shm.sync.Block block = reactor.get(index);
                                    if (block != null) {
                                        ByteArrayInputStream byteIn = new ByteArrayInputStream(block.getPayload());
                                        ObjectInputStream in = new ObjectInputStream(byteIn);
                                        Map<String, Object> req = (Map<String, Object>) in.readObject();
                                        System.out.println(req);

                                        //Thread.sleep(2000);

                                        long begin = (long) req.get("begin");
                                        long end = System.nanoTime();
                                        long duration = end - begin;

                                        req.put("res-thread", Thread.currentThread().getName());
                                        req.put("res-num", value.incrementAndGet());
                                        req.put("end", end);
                                        req.put("duration", duration / 1000000);

                                        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                                        ObjectOutputStream out = new ObjectOutputStream(byteOut);
                                        out.writeObject(req);

                                        io.traffic.shm.sync.Block b = new io.traffic.shm.sync.Block(byteOut.toByteArray());
                                        b.setIndex(block.getIndex());
                                        reactor.feedback(b);
                                    }

                                } catch (Throwable e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }
            }
        }
}
