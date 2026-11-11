/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.remoting.etcd.option;

import io.etcd.jetcd.ByteSequence;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Test the key range helpers of {@link OptionUtil}.
 */
class OptionUtilTest {

    @Test
    void testIncrementsLastByte() {
        byte[] end = OptionUtil.prefixEndOf(ByteSequence.from(new byte[] {0x61})).getBytes();
        assertArrayEquals(new byte[] {0x62}, end);
    }

    @Test
    void testIncrementsOnlyLastByteOfMultiByteKey() {
        byte[] end = OptionUtil.prefixEndOf(ByteSequence.from(new byte[] {0x61, 0x62})).getBytes();
        assertArrayEquals(new byte[] {0x61, 0x63}, end);
    }

    @Test
    void testTrailingMaxByteCarriesToPreviousByte() {
        // A trailing 0xFF must carry to the preceding byte and be dropped, not wrap to 0x00.
        byte[] end = OptionUtil.prefixEndOf(ByteSequence.from(new byte[] {0x61, (byte) 0xff})).getBytes();
        assertArrayEquals(new byte[] {0x62}, end);
    }

    @Test
    void testMultipleTrailingMaxBytesAreDropped() {
        byte[] end =
                OptionUtil.prefixEndOf(ByteSequence.from(new byte[] {0x61, (byte) 0xff, (byte) 0xff})).getBytes();
        assertArrayEquals(new byte[] {0x62}, end);
    }

    @Test
    void testAllMaxBytesReturnNoPrefixEnd() {
        byte[] end = OptionUtil.prefixEndOf(ByteSequence.from(new byte[] {(byte) 0xff, (byte) 0xff})).getBytes();
        assertArrayEquals(OptionUtil.NO_PREFIX_END, end);
    }

    @Test
    void testEmptyPrefixReturnsNoPrefixEnd() {
        byte[] end = OptionUtil.prefixEndOf(ByteSequence.from(new byte[0])).getBytes();
        assertArrayEquals(OptionUtil.NO_PREFIX_END, end);
    }

    @Test
    void testInputIsNotMutated() {
        byte[] original = new byte[] {0x61, (byte) 0xff};
        ByteSequence prefix = ByteSequence.from(original);
        OptionUtil.prefixEndOf(prefix);
        assertArrayEquals(new byte[] {0x61, (byte) 0xff}, prefix.getBytes());
    }
}
