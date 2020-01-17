/**
 * Copyright 2005-2012 Akiban Technologies, Inc.
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
import com.persistit.util.ThreadSequencer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Properties;

public abstract class PersistitUnitTestCase {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder(new File(System.getProperty("buildDirectory")));

  private final static long TEN_SECONDS = 10L * 1000L * 1000L * 1000L;
  protected String DATA_PATH;
  public final static String VOLUME_NAME = "persistit";
  protected final static String RED_FOX = "The quick red fox jumped over the lazy brown dog.";

  protected static String createString(final int exactLength) {
    final StringBuilder sb = new StringBuilder(exactLength);
    // Simple 0..9a..z string
    for (int i = 0; i < 36; ++i) {
      sb.append(Character.forDigit(i, 36));
    }
    final String numAndLetters = sb.toString();
    while (sb.length() < exactLength) {
      sb.append(numAndLetters);
    }
    return sb.toString().substring(0, exactLength);
  }

  protected Persistit _persistit = new Persistit();

  protected Configuration _config;

  /**
   * Returns a Properties object with settings appropriate for unit tests -
   * i.e., very small allocations.
   * 
   * @return Initialization properties for unit tests.
   */
  protected final Properties getProperties(final boolean cleanup) {
    if (cleanup) {
      cleanUpDirectory(new File(DATA_PATH));
    }
    final Properties p = new Properties();
    p.setProperty("datapath", DATA_PATH);
    p.setProperty("buffer.count.16384", "20");
    p.setProperty("volume.1", "${datapath}/" + VOLUME_NAME + ",create,"
      + "pageSize:16384,initialPages:100,extensionPages:100," + "maximumPages:25000");
    p.setProperty("journalpath", "${datapath}/persistit_journal");
    p.setProperty("logfile", "${datapath}/persistit_${timestamp}.log");
    p.setProperty("tmpvoldir", "${datapath}");
    p.setProperty("jmx", "true");
    return p;
  }

  protected final Properties getBiggerProperties(final boolean cleanup) {
    if (cleanup) {
      cleanUpDirectory(new File(DATA_PATH));
    }
    final Properties p = new Properties();
    p.setProperty("datapath", DATA_PATH);
    p.setProperty("buffer.count.16384", "2000");
    p.setProperty("volume.1", "${datapath}/" + VOLUME_NAME + ",create,"
      + "pageSize:16384,initialPages:100,extensionPages:100," + "maximumPages:100000,alias:persistit");
    p.setProperty("volume.2", "${datapath}/persistit_system,create,"
      + "pageSize:16384,initialPages:100,extensionPages:100," + "maximumPages:100000,alias:_system");
    p.setProperty("volume.3", "${datapath}/persistit_txn,create,"
      + "pageSize:16384,initialPages:100,extensionPages:100," + "maximumPages:100000,alias:_txn");
    p.setProperty("journalpath", "${datapath}/persistit_journal");
    p.setProperty("logfile", "${datapath}/persistit_${timestamp}.log");
    p.setProperty("tmpvoldir", "${datapath}");
    return p;
  }

  protected final Properties getAlternateProperties(final boolean cleanup) {
    if (cleanup) {
      cleanUpDirectory(new File(DATA_PATH));
    }
    final Properties p = new Properties();
    p.setProperty("datapath", DATA_PATH);
    p.setProperty("buffer.count.16384", "40");
    p.setProperty("volume.1", "${datapath}/temp,create," + "pageSize:16384,initialPages:4,extensionPages:1,"
      + "maximumPages:100000,alias:persistit");
    p.setProperty("journalpath", "${datapath}/persistit_alt_journal");
    p.setProperty("logfile", "${datapath}/persistit_${timestamp}.log");
    p.setProperty("tmpvoldir", "${datapath}");
    return p;
  }

  protected final Properties getPropertiesByMemory(final boolean cleanup, final String memSpecification) {
    if (cleanup) {
      cleanUpDirectory(new File(DATA_PATH));
    }
    final Properties p = new Properties();
    p.setProperty("datapath", DATA_PATH);
    p.setProperty("buffer.memory.16384", memSpecification);
    p.setProperty("volume.1", "${datapath}/" + VOLUME_NAME + ",create,"
      + "pageSize:16384,initialPages:100,extensionPages:100," + "maximumPages:25000");
    p.setProperty("journalpath", "${datapath}/persistit_journal");
    p.setProperty("logfile", "${datapath}/persistit_${timestamp}.log");
    p.setProperty("tmpvoldir", "${datapath}");
    return p;
  }

  protected void cleanUpDirectory(final File file) {
    if (!file.exists()) {
      file.mkdirs();
      return;
    } else if (file.isFile()) {
      throw new IllegalStateException(file + " must be a directory");
    } else {
      final File[] files = file.listFiles();
      cleanUpFiles(files);
    }
  }

  protected void cleanUpFiles(final File[] files) {
    for (final File file : files) {
      if (file.isDirectory()) {
        cleanUpDirectory(file);
      }
      file.delete();
    }
  }

  protected Properties doGetProperties(final boolean cleanup) {
    return getProperties(cleanup);
  }

  @Before
  public final void prepare() throws Exception {
    DATA_PATH = temp.newFolder("persistit").getAbsolutePath().replaceAll("\\\\", "/");
    setUp();
  }

  public void setUp() throws Exception {
    checkNoPersistitThreads();
    _persistit.setProperties(doGetProperties(true));
    _persistit.initialize();
    _config = _persistit.getConfiguration();
  }

  @After
  public void tearDown() throws Exception {
    // Ensure sequencer is disabled after each test.
    ThreadSequencer.disableSequencer();
    final WeakReference<Persistit> ref = new WeakReference<Persistit>(_persistit);
    _persistit.close(false);
    _persistit = null;

    if (!doesRefBecomeNull(ref)) {
      System.out.println("Persistit has a leftover strong reference");
    }
    checkNoPersistitThreads();
  }

  public void runAllTests() throws Exception {

  }

  public void setPersistit(final Persistit persistit) {
    _persistit = persistit;
  }

  protected void initAndRunTest() throws Exception {
    setUp();
    try {
      runAllTests();
    } catch (final Throwable t) {
      t.printStackTrace();
    } finally {
      tearDown();
    }
  }

  private final static String[] PERSISTIT_THREAD_NAMES = {"CHECKPOINT_WRITER", "JOURNAL_COPIER", "JOURNAL_FLUSHER",
    "PAGE_WRITER", "TXN_UPDATE"};

  protected boolean checkNoPersistitThreads() {
    boolean alive = false;
    final Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces();
    for (final Thread t : map.keySet()) {
      final String name = t.getName();
      for (final String p : PERSISTIT_THREAD_NAMES) {
        if (name.contains(p)) {
          alive = true;
          System.err.println("Thread " + t + " is still alive");
        }
      }
    }
    return alive;
  }

  protected void safeCrashAndRestoreProperties() throws PersistitException {
    _persistit.flush();
    _persistit.crash();
    _persistit = new Persistit(_config);
  }

  protected void crashWithoutFlushAndRestoreProperties() throws PersistitException {
    _persistit.crash();
    _persistit = new Persistit(_config);
  }

  public static boolean doesRefBecomeNull(final WeakReference<?> ref) throws InterruptedException {
    final long expires = System.nanoTime() + TEN_SECONDS;
    while (ref.get() != null && System.nanoTime() < expires) {
      System.gc();
      Thread.sleep(10);
    }
    return ref.get() == null;
  }

  protected void disableBackgroundCleanup() {
    _persistit.getCleanupManager().setPollInterval(-1);
    _persistit.getJournalManager().setWritePagePruningEnabled(false);
  }

  protected void drainJournal() throws Exception {
    _persistit.flush();
    /*
     * Causes all TreeStatistics to be marked clean so that the subsequent
     * checkpoint will not add another dirty page.
     */
    _persistit.flushStatistics();
    _persistit.checkpoint();
    _persistit.getJournalManager().copyBack();
  }
}
