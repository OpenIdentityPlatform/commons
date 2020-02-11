/**
 * Copyright 2011-2012 Akiban Technologies, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.persistit;

import com.persistit.Exchange.Sequence;
import com.persistit.policy.SplitPolicy;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SplitPolicyTest extends PersistitUnitTestCase {

    @Test
    public void testLeftBias() {
        final Buffer nullBuffer = null;
        final int mockLeftSize = 20;
        int capacity = 0;
        final SplitPolicy leftBias = SplitPolicy.LEFT_BIAS;
        assertEquals("LEFT", leftBias.toString());
        int measure = leftBias.splitFit(nullBuffer, 0, 0, false, mockLeftSize, 0, 0, 0, capacity, 0, Sequence.NONE);
        /* splitFit should return 0 since leftSize is larger than capcity */
        assertEquals(0, measure);

        capacity = 21;
        measure = leftBias.splitFit(nullBuffer, 0, 0, false, mockLeftSize, 0, 0, 0, capacity, 0, Sequence.NONE);
        /* splitFit just returns the given leftSize for LEFT_BIAS policy */
        assertEquals(mockLeftSize, measure);
    }

    @Test
    public void testRightBias() {
        final Buffer nullBuffer = null;
        final int mockRightSize = 20;
        int capacity = 0;
        final SplitPolicy rightBias = SplitPolicy.RIGHT_BIAS;
        assertEquals("RIGHT", rightBias.toString());
        int measure = rightBias.splitFit(nullBuffer, 0, 0, false, 0, mockRightSize, 0, 0, capacity, 0, Sequence.NONE);
        /* splitFit should return 0 since rightSize is larger than capacity */
        assertEquals(0, measure);

        capacity = 21;
        measure = rightBias.splitFit(nullBuffer, 0, 0, false, 0, mockRightSize, 0, 0, capacity, 0, Sequence.NONE);
        /* splitFit just returns the given rightSize for RIGHT_BIAS policy */
        assertEquals(mockRightSize, measure);
    }

    @Test
    public void testEvenBias() {
        final Buffer nullBuffer = null;
        int mockRightSize = 20;
        int mockLeftSize = 20;
        int capacity = 0;
        final SplitPolicy evenBias = SplitPolicy.EVEN_BIAS;
        assertEquals("EVEN", evenBias.toString());
        int measure = evenBias.splitFit(nullBuffer, 0, 0, false, mockLeftSize, mockRightSize, 0, 0, capacity, 0,
                Sequence.NONE);
        /*
         * splitFit should return 0 since rightSize & leftSize are larger than
         * capacity
         */
        assertEquals(0, measure);

        capacity = 21;
        measure = evenBias.splitFit(nullBuffer, 0, 0, false, mockLeftSize, mockRightSize, 0, 0, capacity, 0,
                Sequence.NONE);
        /*
         * splitFit returns (capacity - abs(rightSize - leftSize)) for EVEN_BIAS
         * policy
         */
        assertEquals(capacity, measure);

        capacity = 21;
        mockLeftSize = 5;
        mockRightSize = 15;
        measure = evenBias.splitFit(nullBuffer, 0, 0, false, mockLeftSize, mockRightSize, 0, 0, capacity, 0,
                Sequence.NONE);
        /*
         * splitFit returns (capacity - abs(rightSize - leftSize)) for EVEN_BIAS
         * policy
         */
        assertEquals(11, measure);
    }

    @Test
    public void testNiceBias() {
        final Buffer nullBuffer = null;
        int mockRightSize = 20;
        int mockLeftSize = 20;
        int capacity = 0;
        final SplitPolicy niceBias = SplitPolicy.NICE_BIAS;
        assertEquals("NICE", niceBias.toString());
        int measure = niceBias.splitFit(nullBuffer, 0, 0, false, mockLeftSize, mockRightSize, 0, 0, capacity, 0,
                Sequence.NONE);
        /*
         * splitFit should return 0 since rightSize & leftSize are larger than
         * capacity
         */
        assertEquals(0, measure);

        capacity = 21;
        measure = niceBias.splitFit(nullBuffer, 0, 0, false, mockLeftSize, mockRightSize, 0, 0, capacity, 0,
                Sequence.NONE);
        /*
         * splitFit returns ((capacity * 2) - abs((2 * rightSize) - leftSize))
         * for EVEN_BIAS policy
         */
        assertEquals(22, measure);

        capacity = 21;
        mockLeftSize = 5;
        mockRightSize = 15;
        measure = niceBias.splitFit(nullBuffer, 0, 0, false, mockLeftSize, mockRightSize, 0, 0, capacity, 0,
                Sequence.NONE);
        /*
         * splitFit returns ((capacity * 2) - abs((2 * rightSize) - leftSize))
         * for EVEN_BIAS policy
         */
        assertEquals(17, measure);
    }

    @Test
    public void testPackBias() throws Exception {
        final Exchange ex = _persistit.getExchange("persistit", "SplitPolicyTest", true);
        ex.getValue().put("aaabbbcccdddeee");
        ex.to(1);
        final long page = ex.fetchBufferCopy(0).getPageAddress();
        final Buffer buffer = ex.getBufferPool().get(ex.getVolume(), page, false, true);
        buffer.releaseTouched();
        for (int i = 0; buffer.getAvailableSize() > 100; i++) {
            ex.to(i).store();
        }

        int mockRightSize = 20;
        int mockLeftSize = 20;
        int capacity = 0;
        final SplitPolicy packBias = SplitPolicy.PACK_BIAS;
        assertEquals("PACK", packBias.toString());
        //
        // For non-sequential inserts, works the same as NICE_BIAS.
        //
        int measure = packBias.splitFit(buffer, 0, 0, false, mockLeftSize, mockRightSize, 0, 0, capacity, 0,
                Sequence.NONE);
        /*
         * splitFit should return 0 since rightSize & leftSize are larger than
         * capacity
         */
        assertEquals(0, measure);

        capacity = 21;
        measure = packBias.splitFit(buffer, 0, 0, false, mockLeftSize, mockRightSize, 0, 0, capacity, 0, Sequence.NONE);
        /*
         * splitFit returns ((capacity * 2) - abs((2 * rightSize) - leftSize))
         * for EVEN_BIAS policy
         */
        assertEquals(22, measure);

        capacity = 21;
        mockLeftSize = 5;
        mockRightSize = 15;
        measure = packBias.splitFit(buffer, 0, 0, false, mockLeftSize, mockRightSize, 0, 0, capacity, 0, Sequence.NONE);
        /*
         * splitFit returns ((capacity * 2) - abs((2 * rightSize) - leftSize))
         * for EVEN_BIAS policy
         */
        assertEquals(17, measure);

        //
        // Sequential insert cases
        //
        for (int p = buffer.getKeyBlockStart(); p < buffer.getKeyBlockEnd(); p += Buffer.KEYBLOCK_LENGTH) {
            final int splitBest = split(packBias, buffer, p, Sequence.FORWARD);
            if (p > buffer.getKeyBlockStart() + 256 && p < buffer.getKeyBlockEnd() - 256) {
                assertEquals(splitBest, p);
            }
        }
        for (int p = buffer.getKeyBlockStart(); p < buffer.getKeyBlockEnd(); p += Buffer.KEYBLOCK_LENGTH) {
            final int splitBest = split(packBias, buffer, p, Sequence.REVERSE);
            if (p > buffer.getKeyBlockStart() + 260 && p < buffer.getKeyBlockEnd() - 260) {
                assertEquals(splitBest, p + Buffer.KEYBLOCK_LENGTH);
            }
        }
    }

    private int split(final SplitPolicy policy, final Buffer buffer, final int foundAt, final Sequence sequence) {
        int best = -1;
        int bestMeasure = -1;
        int leftSize = 0;
        int rightSize = buffer.getBufferSize() + 10;
        final int perKeySize = rightSize / buffer.getKeyCount();
        for (int p = buffer.getKeyBlockStart(); p < buffer.getKeyBlockEnd(); p += Buffer.KEYBLOCK_LENGTH) {

            final int measure = policy.splitFit(buffer, p, foundAt, false, leftSize, rightSize,
                    buffer.getBufferSize() - 100, leftSize + rightSize, buffer.getBufferSize(), best, sequence);
            if (measure > bestMeasure) {
                best = p;
                bestMeasure = measure;
            }
            leftSize += perKeySize;
            rightSize -= perKeySize;
        }
        return best;
    }

    @Test
    public void testPackBiasPacking() throws Exception {
        final Exchange ex = _persistit.getExchange("persistit", "SplitPolicyTest", true);
        final Random random = new Random(1);
        ex.setSplitPolicy(SplitPolicy.PACK_BIAS);
        ex.getValue().put("aaabbbcccdddeee");

        ex.removeAll();
        for (int i = 0; ex.getVolume().getStorage().getNextAvailablePage() < 20; i++) {
            ex.to(i).store();
        }
        final float ratioFowardSequential = inuseRatio(ex);
        assertTrue(ratioFowardSequential > .85);

        ex.removeAll();
        for (int i = 1000000; ex.getVolume().getStorage().getNextAvailablePage() < 21; i--) {
            ex.to(i).store();
        }
        final float ratioReverseSequential = inuseRatio(ex);
        assertTrue(ratioReverseSequential > .85);

        ex.removeAll();
        for (; ex.getVolume().getStorage().getNextAvailablePage() < 22;) {
            ex.to(random.nextInt()).store();
        }
        final float ratioRandom = inuseRatio(ex);
        assertTrue(ratioRandom > .5 && ratioRandom < .75);
    }

    private float inuseRatio(final Exchange ex) throws Exception {
        float total = 0;
        float used = 0;
        //
        // forward sequential
        //
        for (long page = ex.clear().append(0).fetchBufferCopy(0).getPageAddress(); page < 20; page++) {
            final Buffer buffer = ex.getBufferPool().get(ex.getVolume(), page, false, true);
            if (buffer.isDataPage()) {
                final int available = buffer.getAvailableSize();
                System.out.println(buffer + " avail=" + available);
                total = total + buffer.getBufferSize();
                used = used + buffer.getBufferSize() - buffer.getAvailableSize();
            }
            buffer.releaseTouched();
        }
        return used / total;
    }

}
