# **traffic-shm**  [![logo](https://github.com/peptos/traffic-shm/raw/master/logo.png "logo")](https://github.com/peptos/traffic-shm "logo")


## Overview
traffic-shm (Shared Memory) is a Java based message library, which is designed for interprocess communication (IPC) on the same server.


## Features
### 1. Pure Java
Shared memory is an efficient mechanism for interprocess communication. Memory mapped files offer a dynamical memory management
feature that allows applications to access files on disk in the same way they attach the shared segment of physical memory to their virtual address spaces.

**traffic-shm** provides a pure java implementation using *sun.misc.Unsafe* and *FileChannel*, JDK 1.6+ is required.

### 2. Cross-Platform

*alignment:* 4-bytes aligned
*byteorder:* big-endian

Most of the major operating systems like Linux, macOS, Windows, AIX and HP-UX are supported.

Note:
- set **-Xmpas:on** on HP-UX
- set **--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED** option to support JDK 9+

### 3. Async Mode:
#### 3.1 Lock-Free
With non-blocking algorithm, implementation of a multi-producer/single-consumer concurrent queue, **traffic-shm** could be use to build a real-time system with high throughput and low latency.

#### 3.2 Message Ordering

**traffic-shm** provides a FIFO queue which is ONCE-AND-ONLY-ONCE guaranteed.
cursor is forward only, once a message is delivered successfully, message is AUTOMATIC ACKNOWLEDGEMENT which means that a message is acknowledged as soon as the receiver gets it.

### 4. Sync Mode:
#### 4.1 Segmental Lock
offer a multi-producer/single-consumer concurrent data structure

## Data Structure Layout
**Async Mode:**
![Async](https://github.com/peptos/traffic-shm/raw/master/async.png)

**Sync Mode:**
![Sync](https://github.com/peptos/traffic-shm/raw/master/async.png)


## Getting Started

***Async:***

*Reader:*

	Queue queue = Queue.map("/Users/peptos/ashm", 2000L, 1, 0);
	queue.init();

	while (true) {
		Block block = queue.poll();
		if (block != null) {
			System.out.println(new String(block.getPayload(), "UTF-8"));
		} else {
			Util.pause(10);
		}
	}

*Writer:*

	Queue queue = Queue.map("/Users/peptos/ashm", 2000L, 1, 0);

	String string = "hello, world";
	byte[] bytes = string.getBytes("UTF-8");
	System.out.println(queue.offer(new Block(bytes)));

	queue.close();

***Sync:***

*Reactor:*

    Reactor reactor = Reactor.bind("/Users/peptos/sshm", 5, 2000);
    while (true) {
        byte[] bitmap = reactor.select();
        for (int i = 0; i < bitmap.length; i++) {
            if (ACK.ESTABLISHED == bitmap[i]) {
                Block block = reactor.get(i);

                try {
                    Thread.sleep(2000); // MOCK do the business
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Block response = new Block(String.valueOf(Math.random()).getBytes());
                response.setIndex(block.getIndex());
                reactor.feedback(response);
            }
        }
    }
	

*Transport:*

	Transport transport = Transport.map("/Users/peptos/sshm", 5, 2000);
	try {
      Block para = new Block(String.valueOf(Math.random()).getBytes());
      Block block = transport.submit(para, 10, TimeUnit.SECONDS);
      System.out.println(new String(block.getPayload()));
	} catch (InterruptedException e) {
      e.printStackTrace();
	} catch (TimeoutException e) {
      e.printStackTrace();
	}
	
	transport.close();
