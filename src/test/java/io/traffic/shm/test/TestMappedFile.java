/*
 * Copyright (c) 2016 the original author or authors.
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

import io.traffic.shm.async.Block;
import io.traffic.shm.async.Queue;
import io.traffic.shm.file.MappedFile;
import io.traffic.util.Util;
import org.junit.Test;

/**
 * @author cuiyi
 */
public class TestMappedFile {

    @Test
    public void testMap() throws Exception {
        MappedFile mappedFile = MappedFile.with("/Users/peptos/shm", 200000L);
        System.out.println(mappedFile.getAddress());
    }

    @Test
    public void testQueue() throws Exception {
        Queue queue = Queue.map("/Users/peptos/shm", 2000L, 1, 0);
        queue.init();
    }

    @Test
    public void testGet() throws Exception {
        Queue queue = Queue.map("/Users/peptos/shm", 2000L, 1, 0);
        queue.init();


        Block block = queue.poll();
        if (block != null) {
            System.out.println(new String(block.getPayload(), "UTF-8"));
        }
    }

    @Test
    public void testPoll() throws Exception {
        Queue queue = Queue.map("/Users/peptos/shm", 2000L, 1, 0);
        queue.init();

        long millis = 10 * 1;
        while (true) {
            Block block = queue.poll();
            if (block != null) {
                System.out.println(new String(block.getPayload(), "UTF-8"));
            } else {
                Util.pause(millis);
            }
        }
    }


    @Test
    public void testOffer() throws Exception {
        Queue queue = Queue.map("/Users/peptos/shm", 2000L, 1, 0);

        for (int i = 0; i <= 300000; i++) {
            String str = "----";
            String string = str + i + str;
            byte[] bytes = string.getBytes("UTF-8");
            System.out.println(i + ":" + queue.offer(new Block(bytes)));
        }

        queue.close();
    }


    @Test
    public void testThread() throws Exception {
        final Queue queue = Queue.map("/Users/peptos/shm", 2000L, 1, 0);

        for (int thread = 0; thread < 100; thread++) {

            final Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 3000; i++) {
                        String str = "----";
                        String string = str + Thread.currentThread().getName() + "|" + i + str;
                        byte[] bytes = string.getBytes();
                        boolean res = queue.offer(new Block(bytes));
                        System.out.println(Thread.currentThread().getName() + "|" + i + ":" + res);
                    }
                }
            });
            t.start();
        }
    }


    @Test
    public void testReset() throws Exception {
        final Queue queue = Queue.map("/Users/peptos/shm", 2000L, 1, 0);
        queue.reset();
    }
}
