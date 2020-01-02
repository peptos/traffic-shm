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

package io.traffic.shm.sync;

public abstract class ACK {

    public static final short REQ_SYN = -1; //0xFFFF
    public static final int REQ_FIN = 0xFFFFFFFF;
    public static final int RST = 0xFF0000FF;
    public static final short RES_SYN = 0x1111;
    public static final int RES_FIN = 0x11111111;
    public static final byte NULL = 0x0;
    public static final byte ESTABLISHED = 0x1;

}
