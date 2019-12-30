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
import io.traffic.util.Util;
import org.junit.*;

import java.io.FileOutputStream;
import java.io.PrintStream;

public class TestReaderA {

    private static Queue queue = null;
    private static volatile boolean running = false;

    @BeforeClass
    public static void setup() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    tearDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }));
    }

    @Before
    public void setUp() throws Exception {
        queue = Queue.map("/Users/peptos/ashm", 2000L, 1, 0);
        queue.init();
        running = true;
        Trace.disable();

        PrintStream ps = new PrintStream(new FileOutputStream("r.txt"));
        System.setOut(ps);
    }

    @Test
    public void testPoll() throws Exception {
        long millis = 10 * 1;
        while (running) {
            io.traffic.shm.async.Block block = queue.poll();
            if (block != null) {
                System.out.println(new String(block.getPayload(), "UTF-8"));
            } else {
                Util.pause(millis);
            }
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        System.out.println("@After - execute");
        running = false;
        if (queue != null) {
            queue.close();
        }
    }


}
