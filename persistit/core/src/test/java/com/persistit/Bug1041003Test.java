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

import com.persistit.JournalRecord.JE;
import com.persistit.Transaction.CommitPolicy;
import com.persistit.exception.PersistitIOException;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Properties;

import static org.junit.Assert.fail;

public class Bug1041003Test extends PersistitUnitTestCase {

  final static int BLOCKSIZE = 10000000;

  /*
   * This class needs to be in com.persistit rather than com.persistit.unit
   * because it uses some package- private methods in Persistit.
   */

  private final String _volumeName = "persistit";

  @Override
  protected Properties doGetProperties(final boolean cleanup) {
    final Properties p = getProperties(cleanup);
    p.setProperty("journalsize", Integer.toString(BLOCKSIZE));
    return p;
  }

  private ErrorInjectingFileChannel errorInjectingChannel(final FileChannel channel) {
    final ErrorInjectingFileChannel eimfc = new ErrorInjectingFileChannel();
    ((MediatedFileChannel) channel).injectChannelForTests(eimfc);
    return eimfc;
  }

  /**
   * Simulate IOException on attempt to append to the journal. This simulates
   * bug #878346. Sets an injected IOException on journal file .000000000001
   * then stores a bunch of data until a failure occurs. Clears the injected
   * error, runs one more transaction and then checks the resulting database
   * state for correctness.
   * 
   * @throws Exception
   */
  @Test
  public void leaveTransactionBufferFlipped() throws Exception {
    /*
     * Need first journal file almost full so that an attempt to write a
     * transaction will force a rollover.
     */
    final Exchange exchange = _persistit.getExchange(_volumeName, "Bug1041003Test", true);
    _persistit.flush();
    final JournalManager jman = _persistit.getJournalManager();
    final ByteBuffer bb = ByteBuffer.allocate(BLOCKSIZE);
    final long size = BLOCKSIZE - JE.OVERHEAD - jman.getCurrentAddress() - 1;
    bb.position((int) (size - JournalRecord.TX.OVERHEAD));
    jman.writeTransactionToJournal(bb, 1, 2, 0);
    final Transaction txn = _persistit.getTransaction();
    final ErrorInjectingFileChannel eifc = errorInjectingChannel(_persistit.getJournalManager().getFileChannel(
      BLOCKSIZE));
    /*
     * Will cause any attempt to write into the second journal file to fail.
     */
    eifc.injectTestIOException(new IOException("injected"), "w");
    try {
      txn.begin();
      try {
        exchange.getValue().put(RED_FOX);
        exchange.to(1).store();
        txn.commit(CommitPolicy.HARD);
      } finally {
        txn.end();
      }
    } catch (final PersistitIOException e) {
      if (e.getMessage().contains("injected")) {
        System.out.println("Expected: " + e);
      } else {
        throw e;
      }
    }
    /*
     * Now remove the disk full condition. Transaction should now succeed.
     */
    eifc.injectTestIOException(null, "w");
    txn.begin();
    try {
      exchange.getValue().put(RED_FOX + RED_FOX);
      /*
       * Bug 1041003 causes the following line to throw an
       * IllegalStateException.
       */
      exchange.to(1).store();
      txn.commit(CommitPolicy.HARD);
    } catch (final IllegalStateException e) {
      fail("Bug 1041003 strikes: " + e);
    } finally {
      txn.end();
    }
  }

}
