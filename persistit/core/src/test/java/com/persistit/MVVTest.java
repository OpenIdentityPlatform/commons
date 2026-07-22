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
 * Portions Copyrighted 2026 3A Systems, LLC
 */

package com.persistit;

import com.persistit.exception.CorruptValueException;
import com.persistit.exception.PersistitException;
import com.persistit.util.Util;
import junit.framework.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import static com.persistit.MVV.STORE_EXISTED_MASK;
import static com.persistit.MVV.STORE_LENGTH_MASK;
import static com.persistit.MVV.TYPE_MVV;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MVVTest {
    @Test
    public void requireBigEndian() {
        // Tests have many expected arrays explicitly typed out, sanity check
        // setting
        assertEquals(true, Persistit.BIG_ENDIAN);
    }

    @Test
    public void lengthEstimate() {
        byte[] source = {};
        assertTrue(MVV.estimateRequiredLength(source, -1, 5) >= 5);
        assertTrue(MVV.estimateRequiredLength(source, source.length, 5) >= 5);

        source = newArray(0xA, 0xB, 0xC, 0xD, 0xE, 0xF);
        assertTrue(MVV.estimateRequiredLength(source, 2, 74) >= (2 + 74));
        assertTrue(MVV.estimateRequiredLength(source, source.length, 74) >= (source.length + 74));

        source = newArray(TYPE_MVV, 0, 0, 0, 0, 0, 0, 0, 1, 0, 2, 0x1, 0x2, 0, 0, 0, 0, 0, 0, 0, 5, 0, 1, 0xA);
        assertTrue(MVV.estimateRequiredLength(source, source.length, 1) >= (source.length + 1));
    }

    @Test
    public void lengthExactly() {
        byte[] source = {};
        assertEquals(MVV.exactRequiredLength(source, 0, -1, 1, 5), MVV.overheadLength(1) + 5);
        assertEquals(MVV.exactRequiredLength(source, 0, source.length, 1, 5), MVV.overheadLength(2) + 5);

        source = newArray(0xA, 0xB, 0xC, 0xD, 0xE, 0xF);
        assertEquals(MVV.exactRequiredLength(source, 0, 2, 1, 74), MVV.overheadLength(2) + 2 + 74);
        assertEquals(MVV.exactRequiredLength(source, 0, source.length, 1, 74), MVV.overheadLength(2) + source.length
                + 74);

        source = newArray(TYPE_MVV, 0, 0, 0, 0, 0, 0, 0, 1, 0, 2, 0x1, 0x2, 0, 0, 0, 0, 0, 0, 0, 2, 0, 1, 0xA, /* extra */
                0, 0, 0, 0, 0);
        final int usedLength = source.length - 5;
        // new version (-1 = MVV type ID)
        assertEquals(MVV.exactRequiredLength(source, 0, usedLength, 3, 3), usedLength + MVV.overheadLength(1) + 3 - 1);
        // replace version, shorter
        assertEquals(MVV.exactRequiredLength(source, 0, usedLength, 1, 1), usedLength - 1);
        // replace version, longer
        assertEquals(MVV.exactRequiredLength(source, 0, usedLength, 1, 3), usedLength + 1);
    }

    @Test
    public void isMVVAllInputs() {
        final byte[] empty = {};
        assertEquals("empty and unused", false, MVV.isArrayMVV(empty, 0, -1));
        assertEquals("empty and undefined", false, MVV.isArrayMVV(empty, 0, 0));

        final byte[] primordial = newArray(0xA, 0xB, 0xC);
        assertEquals("primordial and unused", false, MVV.isArrayMVV(primordial, 0, -1));
        assertEquals("primordial and used", false, MVV.isArrayMVV(primordial, 0, primordial.length));

        final byte[] mvvEmptyVersion = newArray(TYPE_MVV, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0);
        assertEquals("mvv empty value", true, MVV.isArrayMVV(mvvEmptyVersion, 0, mvvEmptyVersion.length));
        assertEquals("mvv array but unused", false, MVV.isArrayMVV(mvvEmptyVersion, 0, -1));

        final byte[] mvvTwoVersions = newArray(TYPE_MVV, 0, 0, 0, 0, 0, 0, 0, 1, 0, 2, 0xA, 0xB, 0, 0, 0, 0, 0, 0, 0,
                9, 0, 1, 0xC);
        assertEquals("mvv two versions", true, MVV.isArrayMVV(mvvTwoVersions, 0, mvvTwoVersions.length));
    }

    @Test
    public void storeToUnused() {
        final int vh = 200;
        final byte[] source = { 0xA, 0xB, 0xC };

        final byte[] target = new byte[100];
        final int storedLength = storeVersion(target, -1, vh, source, source.length);

        assertEquals(source.length + MVV.overheadLength(1), storedLength);
        assertArrayEqualsLen(newArray(TYPE_MVV, 0, 0, 0, 0, 0, 0, 0, vh, 0, 3, 0xA, 0xB, 0xC), target, storedLength);
    }

    @Test
    public void storeToUndefined() {
        final int vh = 200;
        final byte[] source = { 0xA, 0xB, 0xC };

        final byte[] target = new byte[100];
        final int storedLength = storeVersion(target, 0, vh, source, source.length);

        assertEquals(source.length + MVV.overheadLength(2), storedLength);
        assertArrayEqualsLen(
                newArray(TYPE_MVV, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, vh, 0, 3, 0xA, 0xB, 0xC), target,
                storedLength);
    }

    @Test
    public void storeToPrimordial() {
        final int vh = 200;
        final byte[] source = { 0xD, 0xE, 0xF };
        final byte[] target = new byte[100];

        final int targetLength = writeArray(target, 0xA, 0xB, 0xC);
        final int storedLength = storeVersion(target, targetLength, vh, source, source.length);

        assertEquals(targetLength + source.length + MVV.overheadLength(2), storedLength);
        assertArrayEqualsLen(
                newArray(TYPE_MVV, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0xA, 0xB, 0xC, 0, 0, 0, 0, 0, 0, 0, vh, 0, 3, 0xD,
                        0xE, 0xF), target, storedLength);
    }

    @Test
    public void storeToExisting() {
        final int vh1 = 10, vh2 = 200;

        final byte[] target = new byte[100];
        final int targetContentsLength = 4;
        final int targetLength = writeArray(target, TYPE_MVV, 0, 0, 0, 0, 0, 0, 0, vh1, 0, 4, 0xA, 0xB, 0xC, 0xD);
        final byte[] source = { 0xE, 0xF };
        final int storedLength = storeVersion(target, targetLength, vh2, source, source.length);

        assertEquals(targetContentsLength + source.length + MVV.overheadLength(2), storedLength);
        assertArrayEqualsLen(
                newArray(TYPE_MVV, 0, 0, 0, 0, 0, 0, 0, vh1, 0, 4, 0xA, 0xB, 0xC, 0xD, 0, 0, 0, 0, 0, 0, 0, vh2, 0, 2,
                        0xE, 0xF), target, storedLength);
    }

    @Test
    public void storeToExistingVersionEqualLength() {
        final int vh1 = 199, vh2 = 200, vh3 = 201;
        final byte[] target = new byte[100];
        final int targetLength = writeArray(target, TYPE_MVV, 0, 0, 0, 0, 0, 0, 0, vh1, 0, 2, 0x4, 0x5, 0, 0, 0, 0, 0,
                0, 0, vh2, 0, 3, 0xA, 0xB, 0xC, 0, 0, 0, 0, 0, 0, 0, vh3, 0, 4, 0x6, 0x7, 0x8, 0x9);
        final byte[] source = { 0xD, 0xE, 0xF };
        final int storedLength = storeVersion(target, targetLength, vh2, source, source.length);
        assertTrue("version existed", (storedLength & STORE_EXISTED_MASK) != 0);
        assertArrayEqualsLen(
                newArray(TYPE_MVV, 0, 0, 0, 0, 0, 0, 0, vh1, 0, 2, 0x4, 0x5, 0, 0, 0, 0, 0, 0, 0, vh2, 0, 3, 0xD, 0xE,
                        0xF, 0, 0, 0, 0, 0, 0, 0, vh3, 0, 4, 0x6, 0x7, 0x8, 0x9), target, storedLength);
    }

    @Test
    public void storeToExistingVersionShorterLength() {
        final int vh1 = 199, vh2 = 200, vh3 = 201;
        final byte[] target = new byte[100];
        final int targetLength = writeArray(target, TYPE_MVV, 0, 0, 0, 0, 0, 0, 0, vh1, 0, 2, 0x4, 0x5, 0, 0, 0, 0, 0,
                0, 0, vh2, 0, 3, 0xA, 0xB, 0xC, 0, 0, 0, 0, 0, 0, 0, vh3, 0, 4, 0x6, 0x7, 0x8, 0x9);
        final byte[] source = { 0xD, 0xE };
        int storedLength = storeVersion(target, targetLength, vh2, source, source.length);
        assertTrue("version existed", (storedLength & STORE_EXISTED_MASK) != 0);

        storedLength &= STORE_LENGTH_MASK;
        assertEquals(targetLength - 1, storedLength);
        assertArrayEqualsLen(
                newArray(TYPE_MVV, 0, 0, 0, 0, 0, 0, 0, vh1, 0, 2, 0x4, 0x5, 0, 0, 0, 0, 0, 0, 0, vh2, 0, 2, 0xD, 0xE,
                        0, 0, 0, 0, 0, 0, 0, vh3, 0, 4, 0x6, 0x7, 0x8, 0x9), target, storedLength);
    }

    @Test
    public void storeToExistingVersionLongerLength() {
        final int vh1 = 199, vh2 = 200, vh3 = 201;
        final byte[] target = new byte[100];
        final int targetLength = writeArray(target, TYPE_MVV, 0, 0, 0, 0, 0, 0, 0, vh1, 0, 2, 0x4, 0x5, 0, 0, 0, 0, 0,
                0, 0, vh2, 0, 3, 0xA, 0xB, 0xC, 0, 0, 0, 0, 0, 0, 0, vh3, 0, 4, 0x6, 0x7, 0x8, 0x9);
        final byte[] source = { 0xC, 0xD, 0xE, 0xF };
        int storedLength = storeVersion(target, targetLength, vh2, source, source.length);
        assertTrue("version existed", (storedLength & STORE_EXISTED_MASK) != 0);

        storedLength &= STORE_LENGTH_MASK;

        assertEquals(targetLength + 1, storedLength);
        assertArrayEqualsLen(
                newArray(TYPE_MVV, 0, 0, 0, 0, 0, 0, 0, vh1, 0, 2, 0x4, 0x5, 0, 0, 0, 0, 0, 0, 0, vh2, 0, 4, 0xC, 0xD,
                        0xE, 0xF, 0, 0, 0, 0, 0, 0, 0, vh3, 0, 4, 0x6, 0x7, 0x8, 0x9), target, storedLength);
    }

    @Test
    public void storeToExistingVersionIfComparedAsInt() {
        final long vh1 = 0x0000000000AABBCCL;
        final byte[] source1 = { 0xA };
        final long vh2 = 0x00FFFFFF00AABBCCL;
        final byte[] source2 = { 0xB };

        int targetLength = 0;
        final byte[] target = new byte[100];
        targetLength = storeVersion(target, targetLength, vh1, source1, source1.length);
        targetLength = storeVersion(target, targetLength, vh2, source2, source2.length);

        assertArrayEqualsLen(
                newArray(TYPE_MVV, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0xAA, 0xBB, 0xCC, 0, 1, 0xA, 0, 0xFF,
                        0xFF, 0xFF, 0, 0xAA, 0xBB, 0xCC, 0, 1, 0xB), target, targetLength);
    }

    @Test
    public void storeBigVersions() {
        final long versions[] = { 10, Short.MAX_VALUE, Integer.MAX_VALUE, 1844674407370955161L, 8301034833169298227L,
                Long.MAX_VALUE };
        final byte contents[][] = { newArray(0xA0), newArray(0xB0, 0xB1), newArray(0xC0, 0xC1, 0xC2),
                newArray(0xD0, 0xD1, 0xD2, 0xD3), newArray(0xE0, 0xE1, 0xE2, 0xE3, 0xE4),
                newArray(0xF0, 0xF1, 0xF2, 0xF3, 0xF4, 0xF5) };

        assertEquals(versions.length, contents.length);

        // Build expected
        final byte[] expectedArray = new byte[1000];
        expectedArray[0] = (byte) TYPE_MVV;
        Util.putLong(expectedArray, 1, 0);
        Util.putShort(expectedArray, 9, 0);
        int off = 11;

        for (int i = 0; i < versions.length; ++i) {
            Util.putLong(expectedArray, off, versions[i]);
            Util.putShort(expectedArray, off + 8, contents[i].length);
            System.arraycopy(contents[i], 0, expectedArray, off + 10, contents[i].length);
            off += 10 + contents[i].length;
        }

        // Build actual
        int targetLength = 0;
        final byte[] target = new byte[1000];
        for (int i = 0; i < versions.length; ++i) {
            targetLength = storeVersion(target, targetLength, versions[i], contents[i], contents[i].length);
        }

        assertArrayEqualsLen(expectedArray, target, targetLength);
    }

    @Test(expected = IllegalArgumentException.class)
    public void storeToUndefinedOverCapacity() {
        final long vh = 10;
        final byte[] source = { 0xA, 0xB, 0xC };
        final int neededLength = MVV.overheadLength(2) + source.length;
        final byte[] target = new byte[neededLength - 1];

        storeVersion(target, 0, vh, source, source.length);
    }

    @Test(expected = IllegalArgumentException.class)
    public void storeToPrimordialOverCapacity() {
        final long vh = 10;
        final byte[] source = { 0xA, 0xB, 0xC };
        final int neededLength = MVV.overheadLength(2) + source.length + 3;

        final byte[] target = new byte[neededLength - 1];
        final int targetLength = writeArray(target, 0xD, 0xE, 0xF);

        storeVersion(target, targetLength, vh, source, source.length);
    }

    @Test(expected = IllegalArgumentException.class)
    public void storeToExistingOverCapacity() {
        final long vh1 = 10, vh2 = 11;
        final byte[] source1 = { 0xA, 0xB, 0xC };
        final byte[] source2 = { 0xD, 0xE, 0xF };
        final int neededLength = MVV.overheadLength(3) + source1.length + source2.length;
        final byte[] target = new byte[neededLength - 1];

        int targetLength = 0;
        try {
            targetLength = storeVersion(target, targetLength, vh1, source1, source1.length);
        } catch (final IllegalArgumentException e) {
            Assert.fail("Expected success on first store");
        }

        storeVersion(target, targetLength, vh2, source2, source2.length);
    }

    @Test(expected = IllegalArgumentException.class)
    public void storeToExistingVersionLongerLengthOverCapacity() {
        final long vh = 10;
        final byte[] source = { 0xA, 0xB, 0xC, 0xD };
        final int neededLength = MVV.overheadLength(2) + source.length;
        final byte[] target = new byte[neededLength - 1];

        int targetLength = 0;
        try {
            targetLength = storeVersion(target, targetLength, vh, source, source.length - 1);
        } catch (final IllegalArgumentException e) {
            Assert.fail("Expected success on first store");
        }

        storeVersion(target, targetLength, vh, source, source.length);
    }

    @Test
    public void storeToExistingVersionAtCapacityShorterLength() {
        final long vh = 10;
        final byte[] source = { 0xA, 0xB, 0xC, 0xD };
        final int neededLength = MVV.overheadLength(2) + source.length;
        final byte[] target = new byte[neededLength];

        int targetLength = 0;
        targetLength = storeVersion(target, targetLength, vh, source, source.length);
        storeVersion(target, targetLength, vh, source, source.length - 1);
    }

    @Test
    public void fetchVersionFromUnused() throws PersistitException {
        final long vh = 10;
        final byte[] source = {};
        final byte[] target = {};
        assertEquals(MVV.VERSION_NOT_FOUND, MVV.fetchVersion(source, -1, vh, target));
    }

    @Test
    public void fetchVersionFromUndefined() throws PersistitException {
        final long vh = 10;
        final byte[] source = {};
        final byte[] target = {};
        assertEquals(MVV.VERSION_NOT_FOUND, MVV.fetchVersion(source, source.length, vh, target));
    }

    @Test
    public void fetchVersionFromPrimordial() throws PersistitException {
        final long vh = 10;
        final byte[] source = { 0xA, 0xB, 0xC };
        final byte[] target = {};
        assertEquals(MVV.VERSION_NOT_FOUND, MVV.fetchVersion(source, source.length, vh, target));
    }

    @Test
    public void fetchVersionFromExistingNoFound() throws PersistitException {
        final long vh = 10;
        final byte[] source = { (byte) TYPE_MVV, 0, 0, 0, 0, 0, 0, 0, 1, 0, 3, 0xA, 0xB, 0xC, 0, 0, 0, 0, 0, 0, 0, 2,
                0, 2, 0xD, 0xE };
        final byte[] target = {};
        assertEquals(MVV.VERSION_NOT_FOUND, MVV.fetchVersion(source, source.length, vh, target));
    }

    @Test
    public void fetchVersionFromExisting() throws PersistitException {
        final long vh = 10;
        final byte[] source = { (byte) TYPE_MVV, 0, 0, 0, 0, 0, 0, 0, 10, 0, 2, 0xA, 0xB, 0, 0, 0, 0, 0, 0, 0, 11, 0,
                3, 0xB, 0xC };
        final byte[] expected = { 0xA, 0xB };
        final byte[] target = new byte[20];
        final int fetchedLen = MVV.fetchVersion(source, source.length, vh, target);
        assertEquals(expected.length, fetchedLen);
        assertArrayEqualsLen(expected, target, expected.length);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fetchVersionFromExistingOverCapacity() throws PersistitException {
        final long vh = 10;
        final byte[] source = { (byte) TYPE_MVV, 0, 0, 0, 0, 0, 0, 0, 11, 0, 5, 0x1, 0x2, 0x3, 0x4, 0x5, 0, 0, 0, 0, 0,
                0, 0, 9, 0, 1, 0xA, 0, 0, 0, 0, 0, 0, 0, 10, 0, 3, 0xB, 0xC, 0xD };
        final byte[] expected = { 0xB, 0xC, 0xD };
        final byte[] target = new byte[expected.length - 1];
        MVV.fetchVersion(source, source.length, vh, target);
    }

    @Test
    public void visitUnused() throws PersistitException {
        final byte[] source = {};
        final TestVisitor visitor = new TestVisitor();
        MVV.visitAllVersions(visitor, source, 0, -1);
        assertTrue(visitor.initCalled);
        assertEquals(newVisitorMap(), visitor.versions);
    }

    @Test
    public void visitUndefined() throws PersistitException {
        final byte[] source = {};
        final TestVisitor visitor = new TestVisitor();
        MVV.visitAllVersions(visitor, source, 0, source.length);
        assertTrue(visitor.initCalled);
        assertEquals(newVisitorMap(0, 0, 0), visitor.versions);
    }

    @Test
    public void visitAndFetchByOffsetPrimordial() throws PersistitException {
        final byte[] source = { 0xA, 0xB, 0xC };
        final TestVisitor visitor = new TestVisitor();
        MVV.visitAllVersions(visitor, source, 0, source.length);
        assertTrue(visitor.initCalled);
        assertEquals(newVisitorMap(0, 3, 0), visitor.versions);

        final byte[] target = new byte[3];
        MVV.fetchVersionByOffset(source, source.length, 0, target);
        assertArrayEquals(source, target);
    }

    @Test
    public void visitAndFetchByOffsetMVV() throws PersistitException {
        final byte[] source = { (byte) TYPE_MVV, 0, 0, 0, 0, 0, 0, 0, 1, 0, 3, 0xA, 0xB, 0xC, 0, 0, 0, 0, 0, 0, 0, 2,
                0, 2, 0xD, 0xE, 0, 0, 0, 0, 0, 0, 0, 11, 0, 5, 0x1, 0x2, 0x3, 0x4, 0x5, 0, 0, 0, 0, 0, 0, 0, 127, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 9, 0, 1, 0xA, 0, 1, 2, 3, 4, 5, 6, 7, 0, 3, 0xB, 0xC, 0xD, 0, 0, 0, 0, 0, 0, 0, 3,
                0, 0 };
        final TestVisitor visitor = new TestVisitor();
        MVV.visitAllVersions(visitor, source, 0, source.length);
        assertTrue(visitor.initCalled);
        assertEquals(
                newVisitorMap(1, 3, 11, 2, 2, 24, 11, 5, 36, 127, 0, 51, 9, 1, 61, 283686952306183L, 3, 72, 3, 0, 85),
                visitor.versions);

        for (final Map.Entry<Long, LengthAndOffset> entry : visitor.versions.entrySet()) {
            final int length = (int) entry.getValue().length;
            final int offset = (int) entry.getValue().offset;
            final byte[] target = new byte[length];
            MVV.fetchVersionByOffset(source, source.length, offset, target);
            assertArrayEqualsLen(source, offset, target, length);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void fetchByOffsetNegative() throws PersistitException {
        final byte[] source = {};
        final byte[] target = new byte[10];
        MVV.fetchVersionByOffset(source, source.length, -1, target);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fetchByOffsetTooLarge() throws PersistitException {
        final byte[] source = newArray(TYPE_MVV, 0, 0, 0, 0, 0, 0, 0, 1, 0, 3, 0xA, 0xB, 0xC);
        final byte[] target = new byte[10];
        MVV.fetchVersionByOffset(source, source.length, source.length + 1, target);
    }

    @Test
    public void storeAndFetchVersionMany() throws PersistitException {
        final int VERSION_COUNT = 10;
        final int versions[] = new int[VERSION_COUNT];
        final byte sources[][] = new byte[VERSION_COUNT][];

        for (int i = 0; i < VERSION_COUNT; ++i) {
            versions[i] = (i + 1) * 5;
            final int length = (versions[i] % 4 + 1) * 5;
            sources[i] = new byte[length];
            for (int j = 0; j < length; ++j) {
                sources[i][j] = (byte) (2 * j);
            }
        }

        int targetLength = 0;
        final byte target[] = new byte[MVV.overheadLength(VERSION_COUNT) + VERSION_COUNT * 20];
        for (int i = 0; i < VERSION_COUNT; ++i) {
            targetLength = storeVersion(target, targetLength, versions[i], sources[i], sources[i].length);
        }

        final byte fetchtarget[] = new byte[50];
        for (int i = 0; i < VERSION_COUNT; ++i) {
            final int fetchedLen = MVV.fetchVersion(target, targetLength, versions[i], fetchtarget);
            assertEquals(sources[i].length, fetchedLen);
            assertArrayEqualsLen(sources[i], fetchtarget, sources[i].length);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void tryValueTooLong() {
        final long VERSION = 10;
        final int LENGTH = MVV.MAX_LENGTH_MASK + 1;
        final byte[] target = new byte[LENGTH + 100];
        final byte[] source = new byte[LENGTH];
        MVV.storeVersion(target, 0, 0, target.length, VERSION, source, 0, source.length);
    }

    //
    // Issue #286: a version left in the marked state (e.g. by a prune that was
    // interrupted mid-way) must still be readable. The length accessors used
    // by the fetch paths formerly read the length field signed and with the
    // mark bit included, driving the scan offset negative and throwing
    // ArrayIndexOutOfBoundsException.
    //

    @Test
    public void visitMarkedVersion() throws PersistitException {
        final byte[] source = newArray(TYPE_MVV, 0, 0, 0, 0, 0, 0, 0, 1, 0, 3, 0xA, 0xB, 0xC, 0, 0, 0, 0, 0, 0, 0, 2,
                0, 2, 0xD, 0xE);
        MVV.mark(source, 1);
        final TestVisitor visitor = new TestVisitor();
        MVV.visitAllVersions(visitor, source, 0, source.length);
        assertTrue(visitor.initCalled);
        assertEquals(newVisitorMap(1, 3, 11, 2, 2, 24), visitor.versions);
    }

    @Test
    public void fetchMarkedVersion() throws PersistitException {
        final byte[] source = newArray(TYPE_MVV, 0, 0, 0, 0, 0, 0, 0, 10, 0, 2, 0xA, 0xB, 0, 0, 0, 0, 0, 0, 0, 11, 0,
                3, 0xC, 0xD, 0xE);
        MVV.mark(source, 1);
        MVV.mark(source, 13);
        final byte[] target = new byte[20];
        assertEquals(2, MVV.fetchVersion(source, source.length, 10, target));
        assertArrayEqualsLen(newArray(0xA, 0xB), target, 2);
        assertEquals(3, MVV.fetchVersion(source, source.length, 11, target));
        assertArrayEqualsLen(newArray(0xC, 0xD, 0xE), target, 3);
    }

    @Test
    public void fetchByOffsetMarkedVersion() throws PersistitException {
        final byte[] source = newArray(TYPE_MVV, 0, 0, 0, 0, 0, 0, 0, 10, 0, 2, 0xA, 0xB, 0, 0, 0, 0, 0, 0, 0, 11, 0,
                3, 0xC, 0xD, 0xE);
        MVV.mark(source, 1);
        final byte[] target = new byte[20];
        assertEquals(2, MVV.fetchVersionByOffset(source, source.length, 11, target));
        assertArrayEqualsLen(newArray(0xA, 0xB), target, 2);
    }

    @Test(expected = CorruptValueException.class)
    public void visitTruncatedHeaderThrows() throws PersistitException {
        final byte[] source = newArray(TYPE_MVV, 0, 0, 0, 0, 0);
        MVV.visitAllVersions(new TestVisitor(), source, 0, source.length);
    }

    /**
     * The old code also threw CorruptValueException here (from the trailing
     * length check), but only after handing the visitor an out-of-bounds
     * version — which Exchange.MvvVisitor would record and fetchVersionByOffset
     * would then copy. The fix must throw before the visitor sees it.
     */
    @Test
    public void visitOverrunningLengthThrows() throws PersistitException {
        final byte[] source = newArray(TYPE_MVV, 0, 0, 0, 0, 0, 0, 0, 1, 0, 50, 0xA, 0xB, 0xC);
        final TestVisitor visitor = new TestVisitor();
        try {
            MVV.visitAllVersions(visitor, source, 0, source.length);
            fail("expected CorruptValueException");
        } catch (final CorruptValueException expected) {
        }
        assertTrue("visitor must not see an out-of-bounds version", visitor.versions.isEmpty());
    }

    /**
     * Issue #286: when prune exits via an exception after its first pass has
     * marked versions (e.g. interrupted while resolving a commit status, or a
     * CorruptValueException), the finally-block safety net must remove the
     * marks. Its loop bound was wrong for a non-zero offset — exactly the
     * in-page pruning case — leaving mark bits behind in the live buffer page.
     */
    @Test
    public void pruneExceptionUnmarksVersionsAtNonZeroOffset() throws Exception {
        final TimestampAllocator tsa = new TimestampAllocator();
        final TransactionIndex ti = new TransactionIndex(tsa, 1);
        final TransactionStatus status1 = ti.registerTransaction();
        final TransactionStatus status2 = ti.registerTransaction();

        /*
         * Two uncommitted versions from different transactions make prune's
         * first pass throw "Multiple uncommitted versions" after it has marked
         * the first version. The non-zero offset emulates in-page pruning.
         */
        final int offset = 117;
        final byte[] bytes = new byte[offset + 100];
        final byte[] v1 = { 0xA, 0xB, 0xC };
        final byte[] v2 = { 0xD, 0xE };
        int length = MVV.storeVersion(bytes, offset, -1, bytes.length, TransactionIndex.ts2vh(status1.getTs()), v1, 0,
                v1.length) & STORE_LENGTH_MASK;
        length = MVV.storeVersion(bytes, offset, length, bytes.length, TransactionIndex.ts2vh(status2.getTs()), v2, 0,
                v2.length) & STORE_LENGTH_MASK;

        final byte[] before = bytes.clone();
        try {
            MVV.prune(bytes, offset, length, ti, true, new ArrayList<MVV.PrunedVersion>());
            fail("expected CorruptValueException");
        } catch (final CorruptValueException expected) {
        }
        assertArrayEquals("prune must leave an MVV without stale marks unchanged when it throws", before, bytes);
    }

    /**
     * Issue #286: on a misaligned (corrupt) MVV the tightened first-pass guard
     * throws before the traversal can mark outside the MVV region, so the
     * finally-block safety net must stop at the same point. A net that keeps
     * iterating while inside the region would unmark() — write — past the
     * region end, into the next record of a live buffer page.
     */
    @Test
    public void pruneExceptionMustNotUnmarkPastRegionEnd() throws Exception {
        final TimestampAllocator tsa = new TimestampAllocator();
        final TransactionIndex ti = new TransactionIndex(tsa, 1);
        final TransactionStatus status = ti.registerTransaction();

        /*
         * One uncommitted version, so the first pass marks it, followed by a
         * claimed region length that leaves the next version header straddling
         * the region end: the guard throws with the mark still set. The 0xFF
         * fill makes any out-of-region unmark() visible (bit 15 cleared).
         */
        final int offset = 117;
        final byte[] bytes = new byte[offset + 100];
        Arrays.fill(bytes, (byte) 0xFF);
        final byte[] v1 = { 0xA, 0xB, 0xC };
        final int stored = MVV.storeVersion(bytes, offset, -1, bytes.length, TransactionIndex.ts2vh(status.getTs()),
                v1, 0, v1.length) & STORE_LENGTH_MASK;
        final int length = stored + 5;

        final byte[] before = bytes.clone();
        try {
            MVV.prune(bytes, offset, length, ti, true, new ArrayList<MVV.PrunedVersion>());
            fail("expected CorruptValueException");
        } catch (final CorruptValueException expected) {
        }
        assertArrayEquals("prune must not write outside the MVV region when it throws", before, bytes);
    }

    //
    // Issue #292: prune uses the mark bit as private transient state and
    // assumes no version is marked on entry. A stale mark left on disk by a
    // prune interrupted before the issue #286 fix violated that assumption:
    // prune could promote the wrong version and skip the PrunedVersion
    // accounting. Prune now clears all mark bits up front.
    //

    /**
     * A stale mark on an obsolete committed version used to win the
     * primordial-conversion scan: the obsolete value was resurrected while the
     * most recent committed version was silently dropped — and neither showed
     * up in the PrunedVersion list, leaking the MVV count (and the page chain,
     * had the dropped version been a long record).
     */
    @Test
    public void pruneStaleMarkedVersionPromotesMostRecentCommitted() throws Exception {
        final TimestampAllocator tsa = new TimestampAllocator();
        final TransactionIndex ti = new TransactionIndex(tsa, 1);

        final int offset = 7;
        final byte[] bytes = new byte[offset + 100];
        final byte[] oldValue = { 0xA, 0xB };
        final byte[] newValue = { 0xC, 0xD, 0xE };
        int length = storeCommittedVersion(tsa, ti, bytes, offset, -1, oldValue);
        final long oldVersionHandle = MVV.getVersion(bytes, offset + 1);
        length = storeCommittedVersion(tsa, ti, bytes, offset, length, newValue);
        ti.updateActiveTransactionCache();

        /* The stale mark an interrupted pre-#286 prune leaves behind. */
        MVV.mark(bytes, offset + 1);

        final ArrayList<MVV.PrunedVersion> pruned = new ArrayList<MVV.PrunedVersion>();
        final int newLength = MVV.prune(bytes, offset, length, ti, true, pruned);

        assertEquals(newValue.length, newLength);
        assertArrayEquals(newValue, Arrays.copyOfRange(bytes, offset, offset + newLength));
        assertEquals("obsolete version must be accounted for", 1, pruned.size());
        assertEquals(oldVersionHandle, pruned.get(0).getVersionHandle());
    }

    /**
     * The same scenario with the stale mark on the zero-length initial version
     * of a value that was undefined before the MVV was created: prune used to
     * resurrect "undefined", discarding the committed value entirely.
     */
    @Test
    public void pruneStaleMarkedUndefinedVersionNotPromoted() throws Exception {
        final TimestampAllocator tsa = new TimestampAllocator();
        final TransactionIndex ti = new TransactionIndex(tsa, 1);

        final int offset = 7;
        final byte[] bytes = new byte[offset + 100];
        final byte[] value = { 0xC, 0xD, 0xE };
        final int length = storeCommittedVersion(tsa, ti, bytes, offset, 0, value);
        ti.updateActiveTransactionCache();

        /* The stale mark sits on the zero-length undefined initial version. */
        MVV.mark(bytes, offset + 1);

        final ArrayList<MVV.PrunedVersion> pruned = new ArrayList<MVV.PrunedVersion>();
        final int newLength = MVV.prune(bytes, offset, length, ti, true, pruned);

        assertEquals(value.length, newLength);
        assertArrayEquals(value, Arrays.copyOfRange(bytes, offset, offset + newLength));
        assertEquals("the primordial version carries no accounting", 0, pruned.size());
    }

    /**
     * Milder variant in the multi-version path: a stale-marked dead version
     * was treated as a keeper — it survived the prune and was left out of the
     * PrunedVersion list, deferring its removal and accounting to the next
     * prune while this one reported a clean result.
     */
    @Test
    public void pruneStaleMarkedVersionPrunedInMultiVersionPath() throws Exception {
        final TimestampAllocator tsa = new TimestampAllocator();
        final TransactionIndex ti = new TransactionIndex(tsa, 1);

        final int offset = 7;
        final byte[] bytes = new byte[offset + 100];
        final byte[] v1 = { 0xA };
        final byte[] v2 = { 0xB, 0xC };
        final byte[] v3 = { 0xD, 0xE, 0xF };
        int length = storeCommittedVersion(tsa, ti, bytes, offset, -1, v1);
        final long vh1 = MVV.getVersion(bytes, offset + 1);
        length = storeCommittedVersion(tsa, ti, bytes, offset, length, v2);
        final int v2At = offset + 1 + MVV.LENGTH_PER_VERSION + v1.length;
        final long vh2 = MVV.getVersion(bytes, v2At);
        length = storeCommittedVersion(tsa, ti, bytes, offset, length, v3);
        final int v3At = v2At + MVV.LENGTH_PER_VERSION + v2.length;
        final long vh3 = MVV.getVersion(bytes, v3At);
        ti.updateActiveTransactionCache();

        MVV.mark(bytes, offset + 1);

        final ArrayList<MVV.PrunedVersion> pruned = new ArrayList<MVV.PrunedVersion>();
        final int newLength = MVV.prune(bytes, offset, length, ti, false, pruned);

        assertEquals("both dead versions must be pruned in one round", MVV.overheadLength(1) + v3.length, newLength);
        assertEquals(vh3, MVV.getVersion(bytes, offset + 1));
        assertEquals(v3.length, MVV.getLength(bytes, offset + 1));
        assertArrayEquals(v3,
                Arrays.copyOfRange(bytes, offset + 1 + MVV.LENGTH_PER_VERSION, offset + 1 + MVV.LENGTH_PER_VERSION
                        + v3.length));
        assertEquals("both dead versions must be accounted for", 2, pruned.size());
        assertEquals(vh1, pruned.get(0).getVersionHandle());
        assertEquals(vh2, pruned.get(1).getVersionHandle());
        int from = offset + 1;
        while (from + MVV.LENGTH_PER_VERSION <= offset + newLength) {
            assertFalse("no version may remain marked", MVV.isMarked(bytes, from));
            from += MVV.getLength(bytes, from) + MVV.LENGTH_PER_VERSION;
        }
    }

    /**
     * The up-front sweep must not follow a corrupted length field: a
     * misaligned traversal lands inside a value and unmark() clears bit 15 of
     * a payload byte — inside the MVV region, past what
     * {@link #pruneExceptionMustNotUnmarkPastRegionEnd} guards. Prune must
     * reject the malformed region without writing anything.
     */
    @Test
    public void pruneMustNotSweepMisalignedMvv() throws Exception {
        final TimestampAllocator tsa = new TimestampAllocator();
        final TransactionIndex ti = new TransactionIndex(tsa, 1);

        /*
         * Two aborted versions, so no pass marks anything and any byte that
         * differs after prune was written by the sweep. The 0xFF fill of the
         * second value makes a misaligned unmark() visible (bit 15 cleared).
         */
        final int offset = 7;
        final byte[] bytes = new byte[offset + 100];
        final byte[] v1 = { 0x1, 0x2, 0x3, 0x4 };
        final byte[] v2 = new byte[20];
        Arrays.fill(v2, (byte) 0xFF);
        int length = storeAbortedVersion(tsa, ti, bytes, offset, -1, v1);
        length = storeAbortedVersion(tsa, ti, bytes, offset, length, v2);
        ti.updateActiveTransactionCache();

        /* Corrupt the first version's length field: it claims 9 bytes, not 4. */
        MVV.putLength(bytes, offset + 1, 9);

        final byte[] before = bytes.clone();
        try {
            MVV.prune(bytes, offset, length, ti, true, new ArrayList<MVV.PrunedVersion>());
            fail("expected CorruptValueException");
        } catch (final CorruptValueException expected) {
        }
        assertArrayEquals("prune must not write into a misaligned MVV region", before, bytes);
    }

    //
    // Test helper methods
    //

    /**
     * Store <code>value</code> as a new version created by a registered
     * transaction and commit it, so that {@link MVV#prune} sees a committed,
     * non-concurrent version.
     */
    private static int storeCommittedVersion(final TimestampAllocator tsa, final TransactionIndex ti,
            final byte[] bytes, final int offset, final int length, final byte[] value) throws Exception {
        final TransactionStatus status = ti.registerTransaction();
        final int newLength = MVV.storeVersion(bytes, offset, length, bytes.length,
                TransactionIndex.ts2vh(status.getTs()), value, 0, value.length) & STORE_LENGTH_MASK;
        final long tc = tsa.updateTimestamp();
        status.commit(tc);
        ti.notifyCompleted(status, tc);
        return newLength;
    }

    /**
     * Store <code>value</code> as a new version created by a registered
     * transaction and abort it, so that {@link MVV#prune} sees an aborted
     * version and marks nothing.
     */
    private static int storeAbortedVersion(final TimestampAllocator tsa, final TransactionIndex ti,
            final byte[] bytes, final int offset, final int length, final byte[] value) throws Exception {
        final TransactionStatus status = ti.registerTransaction();
        final int newLength = MVV.storeVersion(bytes, offset, length, bytes.length,
                TransactionIndex.ts2vh(status.getTs()), value, 0, value.length) & STORE_LENGTH_MASK;
        status.incrementMvvCount();
        status.abort();
        ti.notifyCompleted(status, tsa.updateTimestamp());
        return newLength;
    }

    private static int writeArray(final byte[] array, final int... contents) {
        assert contents.length <= array.length : "Too many values for array";
        for (int i = 0; i < contents.length; ++i) {
            final int value = contents[i];
            assert value >= 0 && value <= 255 : "Value " + value + " out of byte range at index " + i;
            array[i] = (byte) value;
        }
        return contents.length;
    }

    private static byte[] newArray(final int... contents) {
        final byte[] array = new byte[contents.length];
        writeArray(array, contents);
        return array;
    }

    private static void assertArrayEqualsLen(final byte[] expected, final byte[] actual, final int length) {
        assertArrayEqualsLen(expected, 0, actual, length);
    }

    private static void assertArrayEqualsLen(final byte[] expected, final int offset, final byte[] actual,
            final int length) {
        if (expected.length < length) {
            throw new AssertionError(String.format("Expected array is too short: %d vs %d", actual.length, length));
        }
        if (actual.length < length) {
            throw new AssertionError(String.format("Actual array is too short: %d vs %d", actual.length, length));
        }
        for (int i = 0; i < length; ++i) {
            final byte bE = expected[offset + i];
            final byte bA = actual[i];
            if (bE != bA) {
                throw new AssertionError(String.format("Arrays differed at element [%d]: expected <%d> but was <%d>",
                        i, bE, bA));
            }
        }
    }

    private static class LengthAndOffset {
        long length;
        long offset;

        public LengthAndOffset(final long length, final long offset) {
            this.length = length;
            this.offset = offset;
        }

        @Override
        public String toString() {
            return String.format("(%d,%d)", length, offset);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o)
                return true;
            if (!(o instanceof LengthAndOffset))
                return false;
            final LengthAndOffset that = (LengthAndOffset) o;
            return length == that.length && offset == that.offset;
        }
    }

    private static class TestVisitor implements MVV.VersionVisitor {
        boolean initCalled = false;
        Map<Long, LengthAndOffset> versions = new TreeMap<Long, LengthAndOffset>();

        @Override
        public void init() {
            initCalled = true;
            versions.clear();
        }

        @Override
        public void sawVersion(final long version, final int offset, final int valueLength) {
            versions.put(version, new LengthAndOffset(valueLength, offset));
        }
    }

    private static Map<Long, LengthAndOffset> newVisitorMap(final long... vals) {
        assertTrue("must be (version,length,offset) triplets", (vals.length % 3) == 0);
        final Map<Long, LengthAndOffset> outMap = new TreeMap<Long, LengthAndOffset>();
        for (int i = 0; i < vals.length; i += 3) {
            outMap.put(vals[i], new LengthAndOffset(vals[i + 1], vals[i + 2]));
        }
        return outMap;
    }

    /**
     * Helper method to emulate the original MVV.storeVersion. Now the caller is
     * obligated to called and check the exact required length before calling
     * storeVersion and is likely to get an ArrayIndexOutOfBounds or other
     * undefined behavior if it fails to do so.
     */
    static int storeVersion(final byte[] target, final int targetLength, final long versionHandle, final byte[] source,
            final int sourceLength) {
        return MVV.storeVersion(target, 0, targetLength, target.length, versionHandle, source, 0, sourceLength);
    }

}
