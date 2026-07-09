/**
 * Copyright 2011-2012 Akiban Technologies, Inc.
 *
 * Portions Copyrighted 2026 3A Systems, LLC.
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

import com.persistit.Transaction.CommitPolicy;
import com.persistit.util.Util;
import org.junit.Test;

import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ClassIndexTest extends PersistitUnitTestCase {

    /**
     * Arbitrary constant chosen to be found exactly once in the
     * MockSerailizableObject class file.
     */
    final static long SUID_CONSTANT = 0xF3E1D4C1B5A99286L;
    private final static String MOCK_SERIALIZABLE_CLASS_NAME = "MockSerializableObject";

    private int _maxHandle = 0;

    final Map<Integer, ClassInfo> map = new ConcurrentHashMap<Integer, ClassInfo>();

    @SuppressWarnings("serial")
    public static class A implements Serializable {

    }

    @SuppressWarnings("serial")
    public static class B implements Serializable {

    }

    public static class C {

    }

    @Override
    public void tearDown() throws Exception {
        map.clear();
        super.tearDown();
    }

    @Test
    public void oneClassInfo() throws Exception {
        final ClassIndex cx = _persistit.getClassIndex();
        cx.registerClass(this.getClass());
        final ClassInfo ci = cx.lookupByClass(this.getClass());
        assertEquals(this.getClass().getName(), ci.getName());
        assertTrue(ci.getHandle() > 0);
    }

    @Test
    public void manyClassInfo() throws Exception {
        _maxHandle = 0;
        final ClassIndex cx = _persistit.getClassIndex();
        final Class<?> clazz = Persistit.class;
        test2a(cx, clazz);

        assertEquals("Cache misses should match map size", map.size(),
                cx.getCacheMisses() - cx.getDiscardedDuplicates());

        for (int handle = 0; handle < _maxHandle + 10; handle++) {
            assertTrue(equals(map.get(handle), cx.lookupByHandle(handle)));
        }

        final ClassIndex cy = new ClassIndex(_persistit);
        for (int handle = 0; handle < _maxHandle + 10; handle++) {
            assertTrue(equals(map.get(handle), cy.lookupByHandle(handle)));
        }
        for (int handle = 0; handle < _maxHandle + 10; handle++) {
            final ClassInfo ci = map.get(handle);
            if (ci != null) {
                assertEquals(ci, cy.lookupByClass(ci.getDescribedClass()));
            }
        }

        final ClassIndex cz = new ClassIndex(_persistit);
        for (int handle = 0; handle < _maxHandle + 10; handle++) {
            final ClassInfo ci = map.get(handle);
            if (ci != null) {
                assertEquals(ci, cz.lookupByClass(ci.getDescribedClass()));
            }
        }
        System.out.println(cx.size() + " classes");
    }

    @Test
    public void multiThreaded() throws Exception {
        final int threadCount = 50;

        final Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    final Transaction transaction = _persistit.getTransaction();
                    for (int k = 0; k < 10; k++) {
                        try {
                            transaction.begin();
                            try {
                                stress(_persistit.getClassIndex(), Persistit.class, new HashSet<Class<?>>());
                                if ((index % 3) == 0) {
                                    transaction.rollback();
                                } else {
                                    transaction.commit();
                                }
                            } finally {
                                transaction.end();
                            }
                        } catch (final Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
        for (int i = 0; i < threadCount; i++) {
            threads[i].start();
        }
        for (int i = 0; i < threadCount; i++) {
            threads[i].join();
        }

        /*
         * After the concurrent registration above, verify that the ClassIndex is
         * internally consistent: every class reachable from Persistit.class
         * resolves to a handle that resolves back to the same ClassInfo. This runs
         * single-threaded on the test thread, so a genuine inconsistency is
         * reported reliably. (The previous check mutated shared test state from 50
         * threads and asserted from those threads, making it racy and its failures
         * invisible to JUnit.)
         */
        final ClassIndex cx = _persistit.getClassIndex();
        verifyConsistent(cx, Persistit.class, new HashSet<Class<?>>());
    }

    /**
     * Concurrent stress helper: recursively look up (and thereby register) every
     * class reachable from {@code clazz}. It touches no shared test state, so it
     * is safe to run from many threads at once; a per-invocation visited set
     * bounds the recursion over the class graph.
     */
    private void stress(final ClassIndex cx, final Class<?> clazz, final Set<Class<?>> visited) throws Exception {
        if (clazz.isPrimitive() || !visited.add(clazz)) {
            return;
        }
        cx.lookupByClass(clazz);
        if (cx.size() < 1000) {
            for (final Field field : clazz.getDeclaredFields()) {
                stress(cx, field.getType(), visited);
            }
        }
    }

    /**
     * Single-threaded consistency check over the class graph: for every reachable
     * class, lookupByClass and lookupByHandle must agree.
     */
    private void verifyConsistent(final ClassIndex cx, final Class<?> clazz, final Set<Class<?>> visited)
            throws Exception {
        if (clazz.isPrimitive() || !visited.add(clazz)) {
            return;
        }
        final ClassInfo byClass = cx.lookupByClass(clazz);
        assertNotNull(byClass);
        assertEquals(byClass, cx.lookupByHandle(byClass.getHandle()));
        if (cx.size() < 1000) {
            for (final Field field : clazz.getDeclaredFields()) {
                verifyConsistent(cx, field.getType(), visited);
            }
        }
    }

    @Test
    public void rollback() throws Exception {
        Exchange ex = _persistit.getExchange("persistit", "ClassIndexTest", true);
        final Transaction txn = ex.getTransaction();
        txn.begin();
        try {

            ex.getValue().put(new A());
            ex.to("A").store();
            ex.getValue().put(new B());
            ex.to("B").store();
            txn.rollback();
        } finally {
            txn.end();
        }

        txn.begin();
        try {
            ex.getValue().put(new B());
            ex.to("B").store();
            ex.getValue().put(new A());
            ex.to("A").store();
            txn.commit(CommitPolicy.HARD);
        } finally {
            txn.end();
        }

        _persistit.crash();
        _persistit = new Persistit(_config);
        ex = _persistit.getExchange("persistit", "ClassIndexTest", false);
        final Object b = ex.to("B").fetch().getValue().get();
        final Object a = ex.to("A").fetch().getValue().get();
        assertTrue("Incorrect class", a instanceof A);
        assertTrue("Incorrect class", b instanceof B);
    }

    @Test
    public void knownNull() throws Exception {
        final ClassIndex cx = _persistit.getClassIndex();
        final ClassInfo ci = cx.lookupByHandle(12345);
        assertEquals("Should return cached known null", ci, cx.lookupByHandle(12345));
    }

    @Test
    public void multipleVersions() throws Exception {
        final ClassIndex cx = _persistit.getClassIndex();
        cx.clearAllEntries();
        final int count = 10;
        final Class<?>[] classes = new Class<?>[count];
        for (int i = 0; i < count; i++) {
            classes[i] = makeAClass(i);
            assertNotNull("this test requires tools.jar on the classpath", classes[i]);
            cx.lookupByClass(classes[i]);
        }
        assertEquals("Each version should be unique", count, cx.getCacheMisses());
        final Set<Integer> handles = new HashSet<Integer>();
        for (int i = 0; i < count; i++) {
            final ClassInfo ci = cx.lookupByClass(classes[i]);
            final int handle = ci.getHandle();
            assertTrue("Handles must be unique", handles.add(handle));
        }
        assertEquals("Each version should be in cache", count, cx.getCacheMisses());
        for (final Integer handle : handles) {
            final ClassInfo ci = cx.lookupByHandle(handle);
            assertEquals("Lookup by handle and class must match", ci, cx.lookupByClass(ci.getDescribedClass()));
        }
    }

    private boolean equals(final ClassInfo a, final ClassInfo b) {
        if (a == b) {
            return true;
        }
        if (a == null) {
            return b.getDescribedClass() == null;
        }
        return a.equals(b);
    }

    private void test2a(final ClassIndex cx, final Class<?> clazz) throws Exception {
        if (clazz.isPrimitive()) {
            return;
        }
        final ClassInfo ci = cx.lookupByClass(clazz);
        final ClassInfo copy = map.get(ci.getHandle());
        if (ci.getHandle() > _maxHandle) {
            assertNull(copy);
            map.put(ci.getHandle(), ci);
            _maxHandle = ci.getHandle();
            final Field[] fields = clazz.getDeclaredFields();
            for (final Field field : fields) {
                if (cx.size() < 1000) {
                    test2a(cx, field.getType());
                }
            }
        } else {
            assert (copy.equals(ci));
        }
    }

    private static Class<?> makeAClass(final long suid) throws Exception {
        final InputStream is = ClassIndexTest.class.getClassLoader().getResourceAsStream(
                "com/persistit/" + MOCK_SERIALIZABLE_CLASS_NAME + "_classBytes");
        final byte[] bytes = new byte[10000];
        final int length = is.read(bytes);
        replaceSuid(bytes, suid);
        final ClassLoader cl = new ClassLoader(ClassIndexTest.class.getClassLoader()) {
            @Override
            public Class<?> loadClass(final String name) {
                if (name.contains(MOCK_SERIALIZABLE_CLASS_NAME)) {
                    return defineClass(name, bytes, 0, length);
                } else {
                    try {
                        return ClassIndexTest.class.getClassLoader().loadClass(name);
                    } catch (final Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
        return cl.loadClass("com.persistit." + MOCK_SERIALIZABLE_CLASS_NAME);
    }

    private final static void replaceSuid(final byte[] bytes, final long suid) {
        boolean replaced = false;
        for (int i = 0; i < bytes.length; i++) {
            boolean found = true;
            for (int j = 0; found && j < 8; j++) {
                found &= ((bytes[i + j] & 0xFF) == ((SUID_CONSTANT >>> (56 - j * 8)) & 0xFF));
            }
            if (found) {
                assertTrue("Found multiple instances of SUID_CONSTANT", !replaced);
                Util.putLong(bytes, i, suid);
                replaced = true;
            }
        }
        assertTrue("Did not find SUID_CONSTANT", replaced);
    }

}
