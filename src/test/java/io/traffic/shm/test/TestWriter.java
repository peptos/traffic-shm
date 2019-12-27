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

import io.traffic.shm.async.Queue;
import io.traffic.util.Trace;
import org.junit.Test;

public class TestWriter {

    @Test
    public void testOfferA() throws Exception {
        Trace.enable();
        Queue queue = Queue.map("/Users/peptos/ashm", 2000L, 1, 0);

        for (int i = 0; i <= 90000000; i++) {
            String str = "----";
            String string = str + i + str;
            byte[] bytes = string.getBytes("UTF-8");
            boolean isOk = queue.offer(new io.traffic.shm.async.Block(bytes));
//            if (isOk) {
//                System.out.println(i);
//            }
        }

        queue.close();
    }

    @Test
    public void testOfferB() throws Exception {
        Trace.enable();
        Queue queue = Queue.map("/Users/peptos/ashm", 2000L, 1, 0);

        for (int i = 0; i <= 30000; i++) {
            String str = "****";
            String string = str + i + str;
            byte[] bytes = string.getBytes("UTF-8");
            System.out.println(i + ":" + queue.offer(new io.traffic.shm.async.Block(bytes)));
        }

        queue.close();
    }


    @Test
    public void testThread() throws Exception {
        final Queue queue = Queue.map("/Users/peptos/ashm", 2000L, 1, 0);

        for (int thread = 0; thread < 1000; thread++) {

            final Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 300000; i++) {
                        String str = "----";
                        String string = str + Thread.currentThread().getName() + "|" + i + str;
                        byte[] bytes = string.getBytes();
                        boolean res = queue.offer(new io.traffic.shm.async.Block(bytes));
                        //System.out.println(Thread.currentThread().getName() + "|" + i + ":" + res);
                    }
                }
            });
            t.start();
        }

        Thread.sleep(10000);
    }
}
