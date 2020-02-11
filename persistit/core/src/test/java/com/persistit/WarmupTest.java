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

import java.util.Properties;

import static org.junit.Assert.assertEquals;
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

    final Buffer[] buff = new Buffer[100];
    for (int i = 0; i < pool.getBufferCount(); ++i) {
      buff[i] = pool.getBufferCopy(i);
    }

    ex = null;
    _persistit.close();

    _persistit = new Persistit(_config);
    ex = _persistit.getExchange("persistit", "WarmupTest", false);
    pool = ex.getBufferPool();

    for (int i = 0; i < pool.getBufferCount(); ++i) {
      final Buffer bufferCopy = pool.getBufferCopy(i);
      assertEquals(bufferCopy.getPageAddress(), buff[i].getPageAddress());
      assertEquals(bufferCopy.getPageType(), buff[i].getPageType());
      assertEquals(bufferCopy.getBufferSize(), buff[i].getBufferSize());
    }
  }

  @Test
  @Ignore
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
