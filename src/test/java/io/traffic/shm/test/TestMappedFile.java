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

import io.traffic.shm.async.Queue;
import io.traffic.shm.file.MappedFile;
import io.traffic.util.CRC16;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author cuiyi
 */
public class TestMappedFile {

    @Test
    public void testMappedFile() throws Exception {
        MappedFile mappedFile = MappedFile.with("/Users/peptos/ashm", 2000L);
        System.out.println(mappedFile.getAddress());

        mappedFile.unmap();
    }

    @Test
    public void testQueue() throws Exception {
        Queue queue = Queue.map("/Users/peptos/ashm", 2000L);

        queue.close();
    }

    @Test
    public void testGet() throws Exception {
        Queue queue = Queue.map("/Users/peptos/ashm", 2000L);

        io.traffic.shm.async.Block block = queue.poll();
        if (block != null) {
            System.out.println(new String(block.getPayload(), "UTF-8"));
        }

        queue.close();
    }

    @Test
    public void testReset() throws Exception {
        final Queue queue = Queue.map("/Users/peptos/ashm", 2000L);
        queue.reset();

        queue.close();
    }

    @Test
    public void testCRC16() throws Exception {
        System.out.println(CRC16.hash("async"));
        System.out.println(CRC16.hash("sync"));
        Assert.assertEquals(22621, CRC16.hash("async"));
        Assert.assertEquals(60368, CRC16.hash("sync"));
    }

}
