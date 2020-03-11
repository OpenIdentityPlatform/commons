/**
 * Copyright 2012 Akiban Technologies, Inc.
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

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CreateAndDeleteVolumeTest extends PersistitUnitTestCase {

    /**
     * Test for https://bugs.launchpad.net/akiban-persistit/+bug/1045971
     * 
     * Dynamically loaded volumes not recovered "cleanly"
     * 
     * When a program creates new volumes using VolumeSpecifications, they
     * subsequently cause a "missing volume" warning from the journal copier and
     * prevent the removal of journal files containing the references. The
     * journal files accumulate seemingly without end, and each initialization
     * of the database seems to process more and more of the old transactions.
     * 
     * With thanks to jb5
     * 
     * @throws Exception
     */
    @Test
    @Ignore
    public void recoverDynamicVolumes() throws Exception {
        VolumeSpecification volumeSpec;
        _persistit.close();
        int remainingJournalFiles = 0;

        for (int i = 5; --i >= 0;) {
            final Persistit db = new Persistit(_config);
            try {
                volumeSpec = new VolumeSpecification(DATA_PATH + "/hwdemo" + i, null, 16384, 1, 1000, 1, true, false,
                        false);
                db.loadVolume(volumeSpec);
                final Exchange dbex = db.getExchange("hwdemo" + i, "greetings", true);
                dbex.getKey().append("Hello");
                dbex.getValue().put("World");
                dbex.store();
                dbex.getKey().to(Key.BEFORE);
                db.releaseExchange(dbex);
            } finally {
                if (i == 0) {
                    db.copyBackPages();
                }
                remainingJournalFiles = db.getJournalManager().getJournalFileCount();
                db.close();
            }
        }
        assertEquals("Should be only one remaining journal file", 1, remainingJournalFiles);
    }

    @Test
    public void useOldVSpecInducesExpectedFailure() throws Exception {
        VolumeSpecification volumeSpec;
        final Configuration configuration = _persistit.getConfiguration();
        _persistit.close();
        int remainingJournalFiles = 0;
        configuration.setUseOldVSpec(true);

        for (int i = 5; --i >= 0;) {
            final Persistit db = new Persistit(_config);
            try {
                volumeSpec = new VolumeSpecification(DATA_PATH + "/hwdemo" + i, null, 16384, 1, 1000, 1, true, false,
                        false);
                db.loadVolume(volumeSpec);
                final Exchange dbex = db.getExchange("hwdemo" + i, "greetings", true);
                dbex.getKey().append("Hello");
                dbex.getValue().put("World");
                dbex.store();
                dbex.getKey().to(Key.BEFORE);
                db.releaseExchange(dbex);
            } finally {
                if (i == 0) {
                    db.copyBackPages();
                }
                remainingJournalFiles = db.getJournalManager().getJournalFileCount();
                db.close();
            }
        }
        assertTrue("Should be only one remaining journal file", remainingJournalFiles > 1);

    }

    /**
     * Test for bug https://bugs.launchpad.net/akiban-persistit/+bug/1045983
     * 
     * Truncating a dynamically created volume results in corrupted journal If
     * you dynamically load a volume, truncate it (without adding any trees),
     * and then close it, the next time the database is initialized a fatal
     * exception is thrown:
     * 
     * <code><pre>
     * 
     * [JOURNAL_COPIER] WARNING Missing volume truncated referenced at journal address 364
     * [main] WARNING Missing volume truncated referenced at journal address 17,004 (6 similar occurrences in 0 seconds)
     * Exception in thread "main" com.persistit.exception.InvalidPageAddressException: Page 1 out of bounds [0-1]
     *  at com.persistit.VolumeStorageV2.readPage(VolumeStorageV2.java:426)
     *  at com.persistit.Buffer.load(Buffer.java:456)
     *  at com.persistit.BufferPool.get(BufferPool.java:780)
     *  at com.persistit.Tree.setRootPageAddress(Tree.java:203)
     *  at com.persistit.VolumeStructure.init(VolumeStructure.java:70)
     *  at com.persistit.VolumeStorageV2.open(VolumeStorageV2.java:217)
     *  at com.persistit.Volume.open(Volume.java:442)
     *  at com.persistit.Persistit.loadVolume(Persistit.java:1066)
     *  at Truncate.main(Truncate.java:30)
     * 
     * @throws Exception
     * 
     *             </pre></code>
     * 
     *             This test is currently disabled pending a fix.
     * 
     */
    @Test
    @Ignore
    public void truncateDynamicVolumes() throws Exception {

        VolumeSpecification volumeSpec;
        _persistit.close();

        final Persistit db = new Persistit(_config);

        for (int i = 0; i < 2; i++) {
            try {
                volumeSpec = new VolumeSpecification(DATA_PATH + "/truncated", null, 16384, 1, 1000, 1, true, false,
                        false);
                final Volume volume = db.loadVolume(volumeSpec);
                volume.truncate();
                // the following may be omitted, and the problem still exhibited
                final Exchange dbex = db.getExchange("truncated", "greetings", true);
                dbex.getKey().append("ave");
                dbex.getValue().put("mundus");
                dbex.store();
                dbex.getKey().to(Key.BEFORE);
                while (dbex.next()) {
                    System.out.println(dbex.getKey().reset().decode() + " " + dbex.getValue().get());
                }
                db.releaseExchange(dbex);
                // the preceding may be omitted, and the problem still exhibited
            } finally {
                db.close();
            }
        }

    }

}
