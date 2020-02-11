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

import com.persistit.exception.PersistitException;
import org.junit.Test;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MVCCBasicTest extends MVCCTestBase {
    private static final String KEY1 = "k1";
    private static final String KEY2 = "k2";
    private static final long VALUE1 = 12345L;
    private static final long VALUE2 = 67890L;

    @Test
    public void testTwoTrxDifferentTimestamps() throws PersistitException {
        trx1.begin();
        trx2.begin();
        try {
            assertFalse("differing start timestamps", trx1.getStartTimestamp() == trx2.getStartTimestamp());
            trx1.commit();
            trx2.commit();
        } finally {
            trx1.end();
            trx2.end();
        }
    }

    @Test
    public void testSingleTrxWriteAndRead() throws Exception {
        trx1.begin();
        try {
            store(ex1, KEY1, VALUE1);
            assertEquals("fetch before commit", VALUE1, fetch(ex1, KEY1));
            trx1.commit();
        } finally {
            trx1.end();
        }

        trx1.begin();
        try {
            assertEquals("fetch after commit", VALUE1, fetch(ex1, KEY1));
            trx1.commit();
        } finally {
            trx1.end();
        }
    }

    @Test
    public void testTwoTrxDistinctWritesOverlappedReads() throws Exception {
        trx1.begin();
        trx2.begin();
        try {
            store(ex1, KEY1, VALUE1);
            store(ex2, KEY2, VALUE2);

            fetch(ex2, KEY1, false);
            assertFalse("trx2 sees uncommitted trx1 value", ex2.getValue().isDefined());

            fetch(ex1, KEY2, false);
            assertFalse("trx1 sees uncommitted trx2 value", ex1.getValue().isDefined());

            trx1.commit();

            fetch(ex2, KEY1, false);
            assertFalse("trx2 sees committed trx1 from future", ex2.getValue().isDefined());

            trx2.commit();
        } finally {
            trx1.end();
            trx2.end();
        }

        // Both should see both now
        trx1.begin();
        trx2.begin();
        try {
            assertEquals("original trx1 value from new trx1", VALUE1, fetch(ex1, KEY1));
            assertEquals("original trx2 value from new trx1", VALUE2, fetch(ex1, KEY2));
            trx1.commit();

            assertEquals("original trx1 value from new trx2", VALUE1, fetch(ex2, KEY1));
            assertEquals("original trx2 value from new trx2", VALUE2, fetch(ex2, KEY2));
            trx2.commit();
        } finally {
            trx1.end();
            trx2.end();
        }
    }

    @Test
    public void testSingleTrxManyInserts() throws Exception {
        // Enough for a new index level and many splits
        final int INSERT_COUNT = 5000;

        for (int i = 0; i < INSERT_COUNT; ++i) {
            trx1.begin();
            try {
                store(ex1, i, i * 2);
                trx1.commit();
            } finally {
                trx1.end();
            }
        }

        trx1.begin();
        try {
            for (int i = 0; i < INSERT_COUNT; ++i) {
                assertEquals(i * 2, fetch(ex1, i));
            }
            trx1.commit();
        } finally {
            trx1.end();
        }
    }

    @Test
    public void testSingleTrxMultipleLongRecordVersions() throws Exception {
        final int VERSIONS_TO_STORE = 5;
        final String longStr = createString(ex1.getVolume().getPageSize());

        for (int curVer = 0; curVer < VERSIONS_TO_STORE; ++curVer) {
            trx1.begin();
            try {
                store(ex1, curVer, longStr);
                ex1.getValue().clear();
                ex1.fetch();
                assertEquals("key after fetch pre-commit", curVer, ex1.getKey().decodeInt());
                assertEquals("value after fetch pre-commit", longStr, ex1.getValue().getString());
                trx1.commit();
            } finally {
                trx1.end();
            }
        }

        for (int curVer = 0; curVer < VERSIONS_TO_STORE; ++curVer) {
            trx1.begin();
            try {
                fetch(ex1, curVer, false);
                assertEquals("fetched key post-commit", curVer, ex1.getKey().decodeInt());
                assertEquals("fetched value post-commit", longStr, ex1.getValue().getString());
                trx1.commit();
            } finally {
                trx1.end();
            }
        }
    }

    /*
     * Store dozens of small, unique versions of a single key to result in
     * resulting in a LONG MVV value. Check etch pre and post commit.
     */
    @Test
    public void testLongMVVFromManySmall() throws Exception {
        final int PER_LENGTH = 250;
        final String smallStr = createString(PER_LENGTH);
        final int versionCount = (int) ((ex1.getVolume().getPageSize() / PER_LENGTH) * 1.1);

        for (int i = 1; i <= versionCount; ++i) {
            trx1.begin();
            try {
                final String value = smallStr + i;
                store(ex1, KEY1, value);
                assertEquals("value pre-commit version " + i, value, fetch(ex1, KEY1));
                trx1.commit();
                trx1.end();

                trx1.begin();
                assertEquals("value post-commit version " + i, value, fetch(ex1, KEY1));
                trx1.commit();
            } finally {
                trx1.end();
            }
        }
    }

    /*
     * Store multiple unique versions of a single key, with individual versions
     * are both short and long records, resulting in a LONG MVV value. Check
     * fetch pre and post commit.
     */
    @Test
    public void testLongMVVFromManySmallAndLong() throws Exception {
        final int pageSize = ex1.getVolume().getPageSize();
        final String longStr = createString(pageSize);
        final double[] valueLengths = { pageSize * 0.05, 10, pageSize * 0.80, 0, pageSize * 0.20, 25, pageSize * 0.40,
                10, pageSize * 0.10, 45, };

        for (int i = 0; i < valueLengths.length; ++i) {
            trx1.begin();
            try {
                final int length = (int) valueLengths[i];
                final String value = longStr.substring(0, length);
                store(ex1, KEY1, value);
                assertEquals("value pre-commit version " + i, value, fetch(ex1, KEY1));
                trx1.commit();
                trx1.end();

                trx1.begin();
                assertEquals("value post-commit version " + i, value, fetch(ex1, KEY1));
                trx1.commit();
            } finally {
                trx1.end();
            }
        }
    }

    @Test
    public void testIsValuedDefinedTwoTrx() throws Exception {
        trx1.begin();
        trx2.begin();
        try {
            store(ex1, "trx1", 1);
            store(ex2, "trx2", 2);

            assertFalse("trx1 sees uncommitted trx2 key", ex1.clear().append("trx2").isValueDefined());
            assertFalse("trx2 sees uncommitted trx2 key", ex2.clear().append("trx1").isValueDefined());

            trx1.commit();
            trx2.commit();
        } finally {
            trx1.end();
        }

        trx1.begin();
        try {
            assertTrue("committed trx1 key", ex1.clear().append("trx1").isValueDefined());
            assertTrue("committed trx2 key", ex1.clear().append("trx2").isValueDefined());
            trx1.commit();
        } finally {
            trx1.end();
        }
    }

    @Test
    public void testTraverseShallowTwoTrx() throws Exception {
        final List<KVPair> baseList = kvList("a", "A", "z", "Z");
        trx1.begin();
        try {
            storeAll(ex1, baseList);
            trx1.commit();
        } finally {
            trx1.end();
        }

        List<KVPair> trx1List = kvList("d", "D", "trx1", 111, "x", "X");
        List<KVPair> trx2List = kvList("b", "B", "c", "C", "trx2", 222);

        trx1.begin();
        trx2.begin();
        try {
            storeAll(ex1, trx1List);
            storeAll(ex2, trx2List);
            storeAll(ex1, kvList(arr("e", "trx1"), 1, arr("h", "trx1"), 11));
            storeAll(ex2, kvList(arr("f", "trx2"), 2, arr("g", "trx2"), 22));

            trx1List.addAll(kvList("e", "UD", "h", "UD"));
            trx2List.addAll(kvList("f", "UD", "g", "UD"));

            trx1List = combine(trx1List, baseList);
            trx2List = combine(trx2List, baseList);

            assertEquals("trx1 forward,shallow traversal", trx1List, traverseAllFoward(ex1, false));
            assertEquals("trx2 forward,shallow traversal", trx2List, traverseAllFoward(ex2, false));

            Collections.reverse(trx1List);
            Collections.reverse(trx2List);

            assertEquals("trx1 reverse,shallow traversal", trx1List, traverseAllReverse(ex1, false));
            assertEquals("trx2 reverse,shallow traversal", trx2List, traverseAllReverse(ex2, false));

            trx1.commit();
            trx2.commit();
        } finally {
            trx1.end();
            trx2.end();
        }

        trx1.begin();
        try {
            final List<KVPair> fList = combine(trx1List, trx2List);
            assertEquals("final forward,shallow traversal", fList, traverseAllFoward(ex1, false));
            Collections.reverse(fList);
            assertEquals("final reverse,shallow traversal", fList, traverseAllReverse(ex1, false));

            trx1.commit();
        } finally {
            trx1.end();
        }
    }

    @Test
    public void testTraverseDeepTwoTrx() throws Exception {
        final List<KVPair> baseList = kvList("a", "A", "z", "Z");

        trx1.begin();
        try {
            storeAll(ex1, baseList);
            trx1.commit();
        } finally {
            trx1.end();
        }

        List<KVPair> trx1List = kvList(arr("b", "trx1"), 1, arr("d", "trx1"), 11, "trx1", 111);
        List<KVPair> trx2List = kvList(arr("b", "trx2"), 2, arr("c", "trx2"), 22, "trx2", 222);

        trx1.begin();
        trx2.begin();
        try {
            storeAll(ex1, trx1List);
            storeAll(ex2, trx2List);

            trx1List = combine(trx1List, baseList);
            trx2List = combine(trx2List, baseList);

            assertEquals("trx1 forward,deep traversal", trx1List, traverseAllFoward(ex1, true));
            assertEquals("trx2 forward,deep traversal", trx2List, traverseAllFoward(ex2, true));

            Collections.reverse(trx1List);
            Collections.reverse(trx2List);

            assertEquals("trx1 reverse,deep traversal", trx1List, traverseAllReverse(ex1, true));
            assertEquals("trx2 reverse,deep traversal", trx2List, traverseAllReverse(ex2, true));

            trx1.commit();
            trx2.commit();
        } finally {
            trx1.end();
            trx2.end();
        }

        trx1.begin();
        try {
            final List<KVPair> fList = combine(trx1List, trx2List);

            assertEquals("final forward,deep traversal", fList, traverseAllFoward(ex1, true));
            Collections.reverse(fList);
            assertEquals("final reverse,deep traversal", fList, traverseAllReverse(ex1, true));

            trx1.commit();
        } finally {
            trx1.end();
        }
    }

    @Test
    public void testTwoTrxManyTraverseManyKeys() throws Exception {
        final int MIN_PAGES = 6;
        final int MAX_KV_PER_PAGE = ex1.getVolume().getPageSize() / (8 + 14); // ##,trxX
                                                                              // =>
                                                                              // MVV,VER,LEN,##
        final int KVS_PER_TRX = (MIN_PAGES * MAX_KV_PER_PAGE) / 2;
        final int TOTAL_KVS = KVS_PER_TRX * 2;

        trx1.begin();
        trx2.begin();
        try {
            for (int i = 0; i < TOTAL_KVS; ++i) {
                if (i % 2 == 0) {
                    store(ex1, i, "trx1", i);
                } else {
                    store(ex2, i, "trx2", i);
                }
            }

            final Exchange[] exchanges = { ex1, ex1 };
            final Key.Direction[] directions = { Key.GT, Key.LT };
            final boolean[] deepFlags = { true, false };

            for (final Exchange ex : exchanges) {
                final String expectedSeg2 = (ex == ex1) ? "trx1" : "trx2";
                final Key key = ex.getKey();
                final Value value = ex.getValue();

                for (final Key.Direction dir : directions) {
                    final Key.EdgeValue startEdge = (dir == Key.GT) ? Key.BEFORE : Key.AFTER;

                    for (final boolean deep : deepFlags) {
                        final String desc = expectedSeg2 + " " + dir + " " + (deep ? "deep" : "shallow") + ", ";

                        int traverseCount = 0;
                        ex.clear().append(startEdge);
                        while (ex.traverse(dir, deep)) {
                            ++traverseCount;
                            if (deep) {
                                assertEquals(desc + "key depth", 2, key.getDepth());
                                final int keySeg1 = key.indexTo(0).decodeInt();
                                final String keySeg2 = key.indexTo(1).decodeString();
                                final int val = value.getInt();
                                assertEquals(desc + "key seg1 equals value", keySeg1, val);
                                assertEquals(desc + "key seg2", expectedSeg2, keySeg2);
                            } else {
                                assertEquals(desc + "key depth", 1, key.getDepth());
                                assertEquals(desc + "value defined", false, value.isDefined());
                            }
                        }

                        assertEquals(desc + "traverse count", KVS_PER_TRX, traverseCount);
                    }
                }
            }

            trx1.commit();
            trx2.commit();
        } finally {
            trx1.end();
            trx2.end();
        }
    }

    /*
     * Simple sanity check as KeyFilter inspects the keys but doesn't care,
     * directly, about MVCC
     */
    @Test
    public void testKeyFilterTraverseTwoTrx() throws Exception {
        trx1.begin();
        trx2.begin();
        try {
            final List<KVPair> trx1List = kvList("a", "A", "c", "C", "e", "E", "f", "f", "i", "I");
            final List<KVPair> trx2List = kvList("b", "B", "d", "D", "g", "G", "h", "H", "j", "J");

            storeAll(ex1, trx1List);
            storeAll(ex2, trx2List);

            final KeyFilter filter = new KeyFilter(new KeyFilter.Term[] { KeyFilter.rangeTerm("b", "i") });
            trx1List.remove(0);
            trx2List.remove(trx2List.size() - 1);

            assertEquals("trx1 forward filter traversal", trx1List, doTraverse(Key.BEFORE, ex1, Key.GT, filter));
            assertEquals("trx2 forward filter traversal", trx2List, doTraverse(Key.BEFORE, ex2, Key.GT, filter));

            Collections.reverse(trx1List);
            Collections.reverse(trx2List);

            assertEquals("trx1 reverse filter traversal", trx1List, doTraverse(Key.AFTER, ex1, Key.LT, filter));
            assertEquals("trx2 reverse filter traversal", trx2List, doTraverse(Key.AFTER, ex2, Key.LT, filter));

            trx1.commit();
            trx2.commit();
        } finally {
            trx1.end();
        }
    }

    /*
     * Bug found independently of MVCC but fixed due to traverse() changes
     */
    @Test
    public void testShallowTraverseWrongParentValueBug() throws Exception {
        trx1.begin();
        try {
            final List<KVPair> kvList = kvList("a", "A", "b", "B", "z", "Z");
            storeAll(ex1, kvList);
            store(ex1, "a", "a", "AA");

            assertEquals("forward traversal", kvList, traverseAllFoward(ex1, false));
            Collections.reverse(kvList);
            assertEquals("reverse traversal", kvList, traverseAllReverse(ex1, false));

            trx1.commit();
        } finally {
            trx1.end();
        }
    }

    @Test
    public void testSingleTrxStoreRemoveFetch() throws Exception {
        trx1.begin();
        try {
            store(ex1, KEY1, VALUE1);
            assertEquals("fetched value pre-remove pre-commit", VALUE1, fetch(ex1, KEY1));

            assertTrue("key existed pre-remove", remove(ex1, KEY1));

            fetch(ex1, KEY1, false);
            assertFalse("fetched value defined post-remove pre-commit", ex1.getValue().isDefined());

            ex1.clear().append(KEY1);
            assertFalse("key defined post-remove pre-commit", ex1.isValueDefined());

            trx1.commit();
        } finally {
            trx1.end();
        }

        trx1.begin();
        try {
            fetch(ex1, KEY1, false);
            assertFalse("fetched value defined post-remove pre-commit", ex1.getValue().isDefined());

            ex1.clear().append(KEY1);
            assertFalse("key defined post-remove pre-commit", ex1.isValueDefined());

            trx1.commit();
        } finally {
            trx1.end();
        }
    }

    @Test
    public void testRemovedKeysHaveChildrenBug() throws Exception {
        final List<KVPair> keepList = kvList(arr("a", 1), "A1", arr("c", 2), "C2");
        final List<KVPair> removeList = kvList(arr("b", 1), "B1", arr("b", 2), "B2", arr("b", 3), "B3", arr("c", 1),
                "C1");

        trx1.begin();
        try {
            storeAll(ex1, keepList);
            storeAll(ex1, removeList);
            trx1.commit();
        } finally {
            trx1.end();
        }

        // concurrent transaction to prevent pruning of originals
        trx1.begin();
        try {
            trx2.begin();
            try {
                final Key key = ex2.getKey();

                key.clear().append("b").append(1);
                assertEquals(key + " initially exists", true, ex2.isValueDefined());

                key.clear().append("b");
                assertEquals(key + " initially has children", true, ex2.hasChildren());

                removeAll(ex2, removeList);

                key.clear().append("b").append(1);
                assertEquals(key + " exists after removal", false, ex2.isValueDefined());

                key.clear().append("b");
                assertEquals(key + " has children after removal", false, ex2.hasChildren());

                trx2.commit();
            } finally {
                trx2.end();
            }
            trx1.commit();
        } finally {
            trx1.end();
        }
    }

    @Test
    public void testTwoTrxRemoveRanges() throws Exception {
        final List<KVPair> bothList = kvList("a", "A", "m", "M", "z", "Z");
        trx1.begin();
        try {
            storeAll(ex1, bothList);
            trx1.commit();
        } finally {
            trx1.end();
        }

        final Key ka = new Key(_persistit);
        final Key kb = new Key(_persistit);

        trx1.begin();
        trx2.begin();
        try {
            final List<KVPair> trx1List1 = kvList("b", "B", "e", "e", "f", "f", "x", "X");
            storeAll(ex1, trx1List1);

            final List<KVPair> trx2List = kvList("d", "D", "n", "N", "v", "V", "y", "Y");
            storeAll(ex2, trx2List);

            // Explicitly testing overlapping ranges, as the overlaps should
            // not be visible to each other

            ka.clear().append("b");
            kb.clear().append("v");
            assertTrue("trx1 keys removed", ex1.removeKeyRange(ka, kb));

            final List<KVPair> trx1List2 = kvList("a", "A", "x", "X", "z", "Z");
            assertEquals("trx1 traverse post removeKeyRange", trx1List2, traverseAllFoward(ex1, true));
            assertEquals("trx2 traverse post trx1 removeKeyRange", combine(bothList, trx2List),
                    traverseAllFoward(ex2, true));

            ka.clear().append("n");
            kb.clear().append(Key.AFTER);
            assertTrue("trx2 keys removed", ex2.removeKeyRange(ka, kb));
            assertEquals("trx2 traverse post removeAll", kvList("a", "A", "d", "D", "m", "M"),
                    traverseAllFoward(ex2, true));
            assertEquals("trx1 traverse post trx2 removeAll", trx1List2, traverseAllFoward(ex1, true));

            trx1.commit();
            trx2.commit();
        } finally {
            trx1.end();
            trx2.end();
        }

        trx1.begin();
        try {
            assertEquals("traverse post-commit", kvList("a", "A", "d", "D", "x", "X"), traverseAllFoward(ex1, true));
            trx1.commit();
        } finally {
            trx1.end();
        }
    }

    @Test
    public void testRemoveWithSplitsSmall() throws Exception {
        final int keyCount = _persistit.getBufferPool(ex1.getVolume().getPageSize()).getMaxKeys();
        insertRemoveAllAndVerify(keyCount);
    }

    @Test
    public void testRemoveWithSplitsMedium() throws Exception {
        final int keyCount = _persistit.getBufferPool(ex1.getVolume().getPageSize()).getMaxKeys() * 5;
        insertRemoveAllAndVerify(keyCount);
    }

    @Test
    public void testRemoveWithSplitsLarge() throws Exception {
        final int keyCount = _persistit.getBufferPool(ex1.getVolume().getPageSize()).getMaxKeys() * 10;
        insertRemoveAllAndVerify(keyCount);
    }

    private void insertRemoveAllAndVerify(final int keyCount) throws Exception {
        trx1.begin();
        try {
            for (int i = 0; i < keyCount; ++i) {
                ex1.getValue().clear();
                ex1.clear().append(String.format("%05d", i)).store();
            }
            trx1.commit();
        } finally {
            trx1.end();
        }

        trx1.begin();
        try {
            assertEquals("traversed count initial", keyCount, traverseAllFoward(ex1, true).size());
            ex1.removeAll();
            assertEquals("traversed count post-remove pre-commit", 0, traverseAllFoward(ex1, true).size());
            trx1.commit();
        } finally {
            trx1.end();
        }

        trx1.begin();
        try {
            assertEquals("traverse post-remove post-commit", 0, traverseAllFoward(ex1, true).size());
            trx1.commit();
        } finally {
            trx1.end();
        }
    }

    @Test
    public void testKeysVisitedDuringTraverse() throws PersistitException {
        final int TOTAL_DEPTH_1 = 10;
        final int TOTAL_DEPTH_2 = 5;

        trx1.begin();
        try {
            int curKey = 0;
            for (int d1 = 0; d1 < TOTAL_DEPTH_1; ++d1) {
                final String s = String.valueOf((char) ('a' + d1));
                ex1.clear().append(s);
                ex1.getValue().clear().put(s.toUpperCase());
                ex1.store();
                for (int d2 = 1; d2 <= TOTAL_DEPTH_2; ++d2) {
                    ex1.setDepth(1);
                    ex1.append(d2);
                    ex1.getValue().clear().put(++curKey);
                    ex1.store();
                }
            }
            trx1.commit();
        } finally {
            trx1.end();
        }

        trx1.begin();
        try {
            ex1.clear().append("a");
            assertEquals("'a' hasChildren", true, ex1.hasChildren());
            assertEquals("keys traversed for 'a' hasChildren", 1, ex1.getKeysVisitedDuringTraverse());

            ex1.clear().append("a").append(TOTAL_DEPTH_2);
            assertEquals("'a,1' hasChildren", false, ex1.hasChildren());
            assertEquals("keys traversed for 'a' hasChildren", 1, ex1.getKeysVisitedDuringTraverse());

            // Remove everything between A and J
            final Key removeBegin = new Key(_persistit);
            removeBegin.append("a").nudgeDeeper();
            final Key removeEnd = new Key(_persistit);
            removeEnd.append("j");

            ex1.removeKeyRange(removeBegin, removeEnd);

            // Can stop when we hit first sibling (depth < traverse minDepth)
            ex1.clear().append("a");
            assertEquals("'a' hasChildren after remove", false, ex1.hasChildren());
            assertEquals("keys traversed for 'a' hasChildren post-remove", TOTAL_DEPTH_2 + 1,
                    ex1.getKeysVisitedDuringTraverse());

            // Should be able to stop when first (depth < traverse minDepth)
            ex1.clear().append("a").append(TOTAL_DEPTH_2);
            assertEquals("'a,1' hasChildren after remove", false, ex1.hasChildren());
            assertEquals("keys traversed for 'a' hasChildren post-remove", 1, ex1.getKeysVisitedDuringTraverse());

            // Same optimization test, by way of specially known KeyFilter
            ex1.clear().append("a");
            final KeyFilter filter1 = new KeyFilter(ex1.getKey(), ex1.getKey().getDepth() + 1, Integer.MAX_VALUE);
            assertEquals("traverse w/filter1 found key post-remove", false,
                    ex1.traverse(Key.GT, filter1, Integer.MAX_VALUE));
            assertEquals("keys traversed with filter1 post-remove", TOTAL_DEPTH_2 + 1,
                    ex1.getKeysVisitedDuringTraverse());

            // If not using the 'special' KeyFilter, we can't exit traverse
            // early
            final KeyFilter filter2 = new KeyFilter(new KeyFilter.Term[] { KeyFilter.simpleTerm("a") }, 2,
                    Integer.MAX_VALUE);
            assertEquals("traverse w/filter2 found key post-remove", false,
                    ex1.traverse(Key.GT, filter2, Integer.MAX_VALUE));
            // All depth1 and depth2 in range ('a','j')
            final int expectedKeys = (TOTAL_DEPTH_1 - 1) * TOTAL_DEPTH_2 + TOTAL_DEPTH_1 - 1;
            assertEquals("keys traversed with filter2 post-remove", expectedKeys, ex1.getKeysVisitedDuringTraverse());

            trx1.commit();
        } finally {
            trx1.end();
        }
    }

    /*
     * Make sure traverse() exits as soon as possible even when the keys are not
     * parent/child segments but the search key is just truncated.
     */
    @Test
    public void testKeysVisitedDuringTraverseUniformDepth() throws PersistitException {
        final int KEY_COUNT = 50;

        trx1.begin();
        try {
            for (int i = 1; i <= KEY_COUNT; ++i) {
                ex1.clear().append(i).append(i);
                ex1.store();
            }
            trx1.commit();
        } finally {
            trx1.end();
        }

        trx1.begin();
        try {
            ex1.clear().append(10);
            assertEquals("'10' hasChildren", true, ex1.hasChildren());
            assertEquals("keys traversed for '10' hasChildren", 1, ex1.getKeysVisitedDuringTraverse());

            ex1.clear().append(10).append(10);
            assertEquals("'10,10' hasChildren", false, ex1.hasChildren());
            assertEquals("keys traversed for '10,10' hasChildren", 1, ex1.getKeysVisitedDuringTraverse());

            ex1.clear().append(10);
            final KeyFilter filter1 = new KeyFilter(ex1.getKey(), ex1.getKey().getDepth() + 1, Integer.MAX_VALUE);
            assertEquals("traverse w/filter found key", true, ex1.traverse(Key.GT, filter1, Integer.MAX_VALUE));
            assertEquals("keys traversed w/filter", 1, ex1.getKeysVisitedDuringTraverse());

            // Remove everything between 10,10 and 40,40
            final Key removeBegin = new Key(_persistit);
            removeBegin.append(10).append(10);
            final Key removeEnd = new Key(_persistit);
            removeEnd.append(40).append(40);

            ex1.removeKeyRange(removeBegin, removeEnd);

            ex1.clear().append(10);
            assertEquals("'10' hasChildren post-remove", false, ex1.hasChildren());
            assertEquals("keys traversed for '10' hasChildren post-remove", 2, ex1.getKeysVisitedDuringTraverse());

            ex1.clear().append(10).append(10);
            assertEquals("'10,10' hasChildren", false, ex1.hasChildren());
            assertEquals("keys traversed for '10,10' hasChildren post-remove", 1, ex1.getKeysVisitedDuringTraverse());

            ex1.clear().append(10);
            final KeyFilter filter2 = new KeyFilter(ex1.getKey(), ex1.getKey().getDepth() + 1, Integer.MAX_VALUE);
            assertEquals("traverse filter found key post-remove", false,
                    ex1.traverse(Key.GT, filter2, Integer.MAX_VALUE));
            assertEquals("keys traversed w/filter post-remove", 2, ex1.getKeysVisitedDuringTraverse());

            trx1.commit();
        } finally {
            trx1.end();
        }
    }

    @Test
    public void testRedundantRemoveReturnValue() throws PersistitException {
        trx1.begin();
        try {
            store(ex1, KEY1, VALUE1);
            assertEquals("fetch after store", VALUE1, fetch(ex1, KEY1));
            trx1.commit();
        } finally {
            trx1.end();
        }

        trx1.begin();
        try {
            assertEquals("fetch from new trx after commit", VALUE1, fetch(ex1, KEY1));
            assertEquals("key was removed first time", true, remove(ex1, KEY1));

            assertEquals("key is defined after remove", false, ex1.clear().append(KEY1).isValueDefined());
            ex1.clear().append(KEY1).fetch();
            assertEquals("value is defined after remove", false, ex1.getValue().isDefined());

            assertEquals("key was removed second time", false, remove(ex1, KEY1));

            trx1.commit();
        } finally {
            trx1.end();
        }

        trx1.begin();
        try {
            assertEquals("key is defined from new trx after remove", false, ex1.clear().append(KEY1).isValueDefined());
            assertEquals("value is defined from new trx after remove", false, ex1.getValue().isDefined());

            assertEquals("key was removed from new trx", false, remove(ex1, KEY1));

            trx1.commit();
        } finally {
            trx1.end();
        }
    }

    @Test
    public void testSimpleTransactionStepUsage() throws Exception {
        final int KEY_COUNT = 20;
        final List<KVPair> baseList = kvList();
        final List<KVPair> secondList = kvList();

        for (int i = 0; i < KEY_COUNT; ++i) {
            final int value = i * 10;
            baseList.add(new KVPair(i, null, value));
            secondList.add(new KVPair(i + KEY_COUNT, null, value + 1));
        }

        // Store originals
        trx1.begin();
        try {
            storeAll(ex1, baseList);
            trx1.commit();
        } finally {
            trx1.end();
        }

        // Traverse and update at the same time, toggling step back and forth
        // This emulates the server usage by the operators
        final List<KVPair> traversedList = kvList();
        trx1.begin();
        Exchange storeEx = null;
        try {
            final Iterator<KVPair> insertIt = secondList.iterator();
            _persistit.setSessionId(trx1.getSessionId());
            storeEx = _persistit.getExchange(TEST_VOLUME_NAME, TEST_TREE_NAME, false);

            ex1.clear().append(Key.BEFORE);
            while (ex1.next()) {
                traversedList.add(new KVPair(ex1.getKey().decodeInt(), null, ex1.getValue().getInt()));

                if (insertIt.hasNext()) {
                    final int prevStep = trx1.incrementStep();
                    final KVPair pair = insertIt.next();
                    store(storeEx, pair.k1, pair.v);
                    trx1.setStep(prevStep);
                }
            }

            assertEquals("only traversed original keys", baseList, traversedList);

            trx1.commit();
        } finally {
            if (storeEx != null) {
                _persistit.releaseExchange(storeEx);
            }
            trx1.end();
        }

        trx1.begin();
        try {
            final List<KVPair> combined = combine(baseList, secondList);
            final List<KVPair> traversed = traverseAllFoward(ex1, true);
            assertEquals("traversed all after commit", combined, traversed);
            trx1.commit();
        } finally {
            trx1.end();
        }
    }

    @Test
    public void testTransactionStepTraverseBeforeCommit() throws Exception {
        final int KEY_COUNT = 10;
        final List<KVPair> originalList = kvList();
        final List<KVPair> updatedList = kvList();

        for (int i = 0; i < KEY_COUNT; ++i) {
            originalList.add(new KVPair(i, null, i * 10));
            updatedList.add(new KVPair(i + KEY_COUNT, null, i * 10 + 1));
        }

        Exchange storeEx = null;
        trx1.begin();
        try {
            storeEx = createExchange(trx1);

            trx1.setStep(0);
            storeAll(ex1, originalList);

            final List<KVPair> traversedStep0 = kvList();
            final Iterator<KVPair> updatedIt = updatedList.iterator();

            ex1.clear().append(Key.BEFORE);
            while (ex1.next()) {
                traversedStep0.add(new KVPair(ex1.getKey().decodeInt(), null, ex1.getValue().getInt()));

                if (updatedIt.hasNext()) {
                    final int prevStep = trx1.incrementStep();
                    ex1.remove();
                    final KVPair pair = updatedIt.next();
                    store(storeEx, pair.k1, pair.v);
                    trx1.setStep(prevStep);
                }
            }

            trx1.setStep(1);
            final List<KVPair> traversedStep1 = traverseAllFoward(ex1, true);

            trx1.setStep(2);
            final List<KVPair> traversedStep2 = traverseAllFoward(ex1, true);

            assertEquals("traversed only originals from step 0", originalList, traversedStep0);
            assertEquals("traversed only originals from step 1", updatedList, traversedStep1);
            assertEquals("traversed only updated from step 2", updatedList, traversedStep2);

            trx1.commit();
        } finally {
            releaseExchange(storeEx);
            trx1.end();
        }

        trx1.begin();
        try {
            final List<KVPair> traversed = traverseAllFoward(ex1, true);
            assertEquals("traversed only updated after commit", updatedList, traversed);
            trx1.commit();
        } finally {
            trx1.end();
        }
    }

    @Test
    public void testUnorderedStepStoreAndFetch() throws PersistitException {
        final int STEP_COUNT = 10;
        final int stepOrder[] = { 8, 7, 9, 4, 6, 2, 0, 1, 3, 5 };
        assertEquals("step order array size", STEP_COUNT, stepOrder.length);

        trx1.begin();
        try {
            for (final int step : stepOrder) {
                trx1.setStep(step);
                store(ex1, KEY1, step);
            }

            for (int i = 0; i < STEP_COUNT; ++i) {
                trx1.setStep(i);
                fetch(ex1, KEY1, false);
                assertEquals("fetched value from step " + i, i, ex1.getValue().getInt());
            }

            trx1.commit();
        } finally {
            trx1.end();
        }
    }

    @Test
    public void testHigherVersionWithLowerStep() throws PersistitException {
        trx1.begin();
        try {
            trx1.setStep(5);
            store(ex1, KEY1, VALUE1);
            assertEquals("fetch after store from trx1, step 5", VALUE1, fetch(ex1, KEY1));
            trx1.commit();
        } finally {
            trx1.end();
        }

        // Concurrent txn to prevent prune
        trx1.begin();
        try {
            trx2.begin();
            try {
                trx2.setStep(0);
                store(ex2, KEY1, VALUE2);
                assertEquals("fetch after store from tx2, step 0", VALUE2, fetch(ex2, KEY1));
                trx2.commit();
            } finally {
                trx2.end();
            }
            trx1.commit();
        } finally {
            trx1.end();
        }
    }

    @Test
    public void testStoreReallyLongRecord() throws PersistitException {
        // Enough that if the length portion of the MVV is signed we fail
        final String LONG_STR = createString(Short.MAX_VALUE + 2);

        trx1.begin();
        try {
            store(ex1, KEY1, LONG_STR);
            assertEquals("fetched value", LONG_STR, fetch(ex1, KEY1));
            trx1.commit();
        } finally {
            trx1.end();
        }

        trx1.begin();
        try {
            assertEquals("fetched committed value", LONG_STR, fetch(ex1, KEY1));
            trx1.commit();
        } finally {
            trx1.end();
        }
    }

    //
    // Test Helpers
    //

    private Exchange createExchange(final Transaction txn) throws PersistitException {
        _persistit.setSessionId(txn.getSessionId());
        return _persistit.getExchange(TEST_VOLUME_NAME, TEST_TREE_NAME, true);
    }

    private void releaseExchange(final Exchange ex) {
        if (ex != null) {
            _persistit.releaseExchange(ex);
        }
    }
}
