/**
 * Copyright 2012 Akiban Technologies, Inc.
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

import org.junit.Test;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class WarmupTest extends PersistitUnitTestCase {

  @Override
  protected Properties doGetProperties(final boolean cleanup) {
    final Properties p = super.getProperties(cleanup);
    p.setProperty("bufferinventory", "true");
    p.setProperty("bufferpreload", "true");
    return p;
  }

  @Test
  public void testWarmup() throws Exception {
    Exchange ex = _persistit.getExchange("persistit", "WarmupTest", true);
    BufferPool pool = ex.getBufferPool();
    for (int i = 1; i <= 1000; i++) {
      ex.getValue().put(RED_FOX);
      ex.clear().append(i).store();
    }

    /*
     * Warmup (bufferinventory + bufferpreload) guarantees that the pages
     * resident in the pool at shutdown are read back into the pool on restart.
     * It does not guarantee that a page returns to the same buffer slot:
     * preloadBufferInventory() sorts the recorded pages by read order and
     * reallocates buffers via the clock algorithm, so the slot index is not
     * preserved. Compare the set of resident pages, not per-slot correspondence
     * (the latter only happened to match and made the test flaky).
     */
    final Set<String> before = residentPages(pool);
    assertTrue("Test setup should leave pages resident in the pool", !before.isEmpty());

    ex = null;
    _persistit.close();

    _persistit = new Persistit(_config);
    pool = _persistit.getVolume("persistit").getStructure().getPool();

    final Set<String> after = residentPages(pool);
    final Set<String> missing = new HashSet<String>(before);
    missing.removeAll(after);
    assertTrue("Warmup should preload every previously-resident page; missing=" + missing,
        missing.isEmpty());
  }

  /**
   * Collects an identity ({@code volume:page:type:size}) for every valid,
   * recordable buffer currently resident in the pool. Mirrors the filtering in
   * {@link BufferPool#recordBufferInventory}, which skips temporary and lock
   * volumes, so the returned set is exactly what warmup is expected to reload.
   */
  private static Set<String> residentPages(final BufferPool pool) {
    final Set<String> pages = new HashSet<String>();
    for (int i = 0; i < pool.getBufferCount(); i++) {
      final Buffer b = pool.getBufferCopy(i);
      final Volume volume = b.getVolume();
      if (b.isValid() && volume != null && !volume.isTemporary() && !volume.isLockVolume()) {
        pages.add(volume.getName() + ':' + b.getPageAddress() + ':' + b.getPageType() + ':'
            + b.getBufferSize());
      }
    }
    return pages;
  }

  @Test
  public void readOrderIsSequential() throws Exception {

    Exchange ex = _persistit.getExchange("persistit", "WarmupTest", true);
    BufferPool pool = ex.getBufferPool();

    final int full = pool.getBufferCount() * (pool.getBufferSize() / RED_FOX.length());
    /*
     * Overflow the buffer pool
     */
    for (int i = 1; i <= full * 3; i++) {
      ex.getValue().put(RED_FOX);
      ex.clear().append(i).store();
    }
    /*
     * Pull some low-address pages in to scramble the pool
     */
    for (int i = full * 2; i >= 0; i -= 1000) {
      ex.clear().append(i).fetch();
    }
    /*
     * Verify that buffers in pool now have somewhat scrambled page
     * addresses
     */
    int breaks = 0;
    long previous = -1;

    for (int i = 0; i < pool.getBufferCount(); i++) {
      final Buffer b = pool.getBufferCopy(i);
      assertTrue("Every buffer should be valid at this point", b.isValid());
      if (b.getPageAddress() < previous) {
        breaks++;
      }
      previous = b.getPageAddress();
    }

    assertTrue("Buffer pool should have scrambled page address", breaks > 0);

    ex = null;
    pool = null;
    _persistit.copyBackPages();
    _persistit.close();

    _persistit = new Persistit();
    _config.setBufferInventoryEnabled(false);
    _config.setBufferPreloadEnabled(false);
    _persistit.setConfiguration(_config);
    _persistit.initialize();

    final Volume volume = _persistit.getVolume("persistit");
    final MediatedFileChannel mfc = (MediatedFileChannel) volume.getStorage().getChannel();
    final TrackingFileChannel tfc = new TrackingFileChannel();
    mfc.injectChannelForTests(tfc);
    pool = volume.getStructure().getPool();
    pool.preloadBufferInventory();
    assertTrue("Preload should have loaded pages from journal file", tfc.getReadPositionList().size() > 0);
    tfc.assertOrdered(true, true);
  }
}
