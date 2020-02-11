/**
 * Copyright 2011-2012 Akiban Technologies, Inc.
 * Copyright 2015 ForgeRock AS
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

import static com.persistit.TransactionStatus.ABORTED;
import static com.persistit.util.SequencerConstants.PAGE_MAP_READ_INVALIDATE_A;
import static com.persistit.util.SequencerConstants.RECOVERY_PRUNING_B;
import static com.persistit.util.ThreadSequencer.sequence;
import static com.persistit.util.Util.NS_PER_MS;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.persistit.AlertMonitor.AlertLevel;
import com.persistit.AlertMonitor.Event;
import com.persistit.CheckpointManager.Checkpoint;
import com.persistit.JournalRecord.CP;
import com.persistit.JournalRecord.IT;
import com.persistit.JournalRecord.IV;
import com.persistit.JournalRecord.JE;
import com.persistit.JournalRecord.JH;
import com.persistit.JournalRecord.PA;
import com.persistit.JournalRecord.PM;
import com.persistit.JournalRecord.TM;
import com.persistit.JournalRecord.TX;
import com.persistit.Persistit.FatalErrorException;
import com.persistit.TransactionPlayer.TransactionPlayerListener;
import com.persistit.exception.CorruptJournalException;
import com.persistit.exception.PersistitException;
import com.persistit.exception.PersistitIOException;
import com.persistit.exception.PersistitInterruptedException;
import com.persistit.exception.RebalanceException;
import com.persistit.exception.VolumeNotFoundException;
import com.persistit.mxbeans.JournalManagerMXBean;
import com.persistit.util.Debug;
import com.persistit.util.Util;

/**
 * Manages the disk-based I/O journal. The journal contains both committed
 * transactions and images of updated pages.
 *
 * @author peter
 *
 */
class JournalManager implements JournalManagerMXBean, VolumeHandleLookup {

    /** Copy with no delay as soon as the number of journal files reach this threshold. */
    final static int PAGE_COPIER_URGENT = 5;
    final static int URGENT = 10;
    final static int ALMOST_URGENT = 8;
    final static int HALF_URGENT = 5;
    final static int URGENT_COMMIT_DELAY_MILLIS = 50;
    final static int GENTLE_COMMIT_DELAY_MILLIS = 12;
    private final static int IO_MEASUREMENT_CYCLES = 8;
    private final static int TOO_MANY_WARN_THRESHOLD = 5;
    private final static int TOO_MANY_ERROR_THRESHOLD = 10;
    private final static long KILO = 1024;

    /**
     * REGEX expression that recognizes the name of a journal file.
     */
    final static Pattern PATH_PATTERN = Pattern.compile("(.+)\\.(\\d{12})");

    private long _journalCreatedTime;

    private final Map<PageNode, PageNode> _pageMap = new HashMap<PageNode, PageNode>();

    private final RangeRemovingArrayList<PageNode> _pageList = new RangeRemovingArrayList<PageNode>();

    private final Map<PageNode, PageNode> _branchMap = new HashMap<PageNode, PageNode>();

    private final Map<Volume, Integer> _volumeToHandleMap = new HashMap<Volume, Integer>();

    private final Map<Integer, Volume> _handleToVolumeMap = new HashMap<Integer, Volume>();

    private final Map<TreeDescriptor, Integer> _treeToHandleMap = new HashMap<TreeDescriptor, Integer>();

    private final Map<Integer, TreeDescriptor> _handleToTreeMap = new HashMap<Integer, TreeDescriptor>();
    
    /** Maintain list of running transactions keyed by their start time-stamp. */  
    private final Map<Long, TransactionMapItem> _liveTransactionMap = new HashMap<Long, TransactionMapItem>();

    private final Persistit _persistit;

    private long _blockSize;

    private volatile int _writeBufferSize = DEFAULT_BUFFER_SIZE;

    private ByteBuffer _writeBuffer;

    private long _writeBufferAddress = Long.MAX_VALUE;

    private JournalFlusher _flusher;

    private JournalCopier _copier;

    private final AtomicBoolean _closed = new AtomicBoolean();

    private final AtomicBoolean _copying = new AtomicBoolean();

    private final AtomicBoolean _copyFast = new AtomicBoolean();

    private final AtomicBoolean _flushing = new AtomicBoolean();

    private final AtomicBoolean _appendOnly = new AtomicBoolean();

    private final AtomicBoolean _ignoreMissingVolume = new AtomicBoolean();

    private String _journalFilePath;

    /**
     * Address of first available byte in the journal. This is usually the
     * address of the next record to be written, but if that next record
     * requires more space than is available in the current journal file, it
     * will advance to the start of the next journal file.
     */
    private volatile long _currentAddress;

    /**
     * Smallest journal address at which a record still needed is located.
     * Initially zero, increases as journal files are consumed and deleted.
     */
    private volatile long _baseAddress;

    private final Map<Long, FileChannel> _journalFileChannels = new HashMap<Long, FileChannel>();

    /**
     * Counter used to assign internal handle values to Volume and Tree records.
     */
    private int _handleCounter = 0;

    private Checkpoint _lastValidCheckpoint = new Checkpoint(0, 0);

    private long _lastValidCheckpointJournalAddress = 0;

    private long _lastValidCheckpointBaseAddress = 0;

    private long _deleteBoundaryAddress = 0;

    private int _lastReportedJournalFileCount = 0;

    private boolean _isNewEpoch = true;

    private volatile long _writePageCount = 0;

    private volatile long _readPageCount = 0;

    private volatile long _copiedPageCount = 0;

    private volatile long _droppedPageCount = 0;

    private final AtomicLong _totalCommits = new AtomicLong();

    private final AtomicLong _totalCommitWaitTime = new AtomicLong();

    private final AtomicLong _totalFlushCycles = new AtomicLong();

    private final AtomicLong _totalFlushIoTime = new AtomicLong();

    private volatile long _flushInterval = DEFAULT_FLUSH_INTERVAL_MS;

    private volatile long _slowIoAlertThreshold = DEFAULT_SLOW_IO_ALERT_THRESHOLD_MS;

    private final TransactionPlayer _player = new TransactionPlayer(new JournalTransactionPlayerSupport());

    private final TransactionPlayerListener _listener = new ProactiveRollbackListener();

    private final PruneTransactionPlayer _pruneCommited = new PruneTransactionPlayer();

    private final AtomicBoolean _writePagePruning = new AtomicBoolean(true);

    private final AtomicBoolean _rollbackPruning = new AtomicBoolean(true);

    /*
     * Tunable parameters that determine how vigorously the copyBack thread
     * performs I/O. Hopefully we can set good defaults and not expose these as
     * knobs.
     */
    private volatile long _copierInterval = DEFAULT_COPIER_INTERVAL_MS;

    private volatile int _copiesPerCycle = DEFAULT_COPIES_PER_CYCLE;

    private volatile long _copierTimestampLimit = Long.MAX_VALUE;

    private volatile long _earliestCommittedTimestamp = Long.MAX_VALUE;

    private volatile long _earliestAbortedTimestamp = Long.MAX_VALUE;

    private boolean _allowHandlesForTempVolumesAndTrees;

    private volatile int _urgentFileCountThreshold = DEFAULT_URGENT_FILE_COUNT_THRESHOLD;

    private volatile long _throttleSleepInterval;

    /**
     * <p>
     * Initialize the new journal. This method takes its information from the
     * supplied RecoveryManager if supplied and valid. Otherwise it starts a new
     * journal at address 0.
     * </p>
     * <p>
     * If a RecoveryManager is supplied and has a valid keystone address, then
     * this method continues the existing journal. A new journal file will be
     * created with a generation number one larger than that of the keystone
     * file, and the new file is given the same journal create date as the
     * recovered journal. New journal files are also required to have the same
     * maximumSize and path name (not including generation suffix) as the
     * existing journal, so in the event <code>rman</code> is non-null and
     * contains a valid keystone, the <code>path</code> and
     * <code>maximumSize</code> parameters are ignored.
     * </p>
     * <p>
     * Otherwise, this method creates a new journal starting at journal address
     * 0 with the specified path and maximum file size. Journal file names are
     * created by appending a period followed by a generation number suffix to
     * the supplied path name. For example if the supplied path is
     * "/xxx/yyy/zzz" then journal file names will be
     * "/xxx/yyy/zzz.000000000000", "/xxx/yyy/zzz.000000000001", and so on. (The
     * suffix contains twelve digits.)
     * </p>
     *
     * @param rman
     * @param path
     * @param maximumSize
     * @throws PersistitException
     */
    public synchronized void init(final RecoveryManager rman, final String path, final long maximumSize)
            throws PersistitException {
        _writeBuffer = ByteBuffer.allocate(_writeBufferSize);
        if (rman != null && rman.getKeystoneAddress() != -1) {
            _journalFilePath = rman.getJournalFilePath();
            _blockSize = rman.getBlockSize();
            _currentAddress = rman.getKeystoneAddress() + _blockSize;
            _baseAddress = rman.getBaseAddress();
            _journalCreatedTime = rman.getJournalCreatedTime();
            _lastValidCheckpoint = rman.getLastValidCheckpoint();
            rman.collectRecoveredPages(_pageMap, _branchMap);
            rman.collectRecoveredVolumeMaps(_handleToVolumeMap, _volumeToHandleMap);
            rman.collectRecoveredTreeMaps(_handleToTreeMap, _treeToHandleMap);
            rman.collectRecoveredTransactionMap(_liveTransactionMap);
            /*
             * Set _handleCount so that newly created handles do not conflict
             * with existing resources.
             */
            for (final Integer handle : _handleToTreeMap.keySet()) {
                _handleCounter = Math.max(_handleCounter, handle + 1);
            }
            for (final Integer handle : _handleToVolumeMap.keySet()) {
                _handleCounter = Math.max(_handleCounter, handle + 1);
            }
            /*
             * Populate page list in journal address order.
             */
            for (final PageNode root : _pageMap.values()) {
                for (PageNode pn = root; pn != null; pn = pn.getPrevious()) {
                    _pageList.add(pn);
                }
            }
            Collections.sort(_pageList, PageNode.READ_COMPARATOR);

        } else {
            _journalFilePath = journalPath(path).getAbsoluteFile().toString();
            _blockSize = maximumSize;
            _currentAddress = 0;
            _journalCreatedTime = System.currentTimeMillis();
        }
        _closed.set(false);
    }

    public void startJournal() throws PersistitException {
        synchronized (this) {
            prepareWriteBuffer(JH.OVERHEAD);
        }
        _flusher = new JournalFlusher();
        _copier = new JournalCopier();

        _copier.start();
        _flusher.start();
    }

    /**
     * Copy dynamic variables into a {@link Management.JournalInfo} structure.
     *
     * @param info
     */
    public synchronized void populateJournalInfo(final Management.JournalInfo info) {
        info.closed = _closed.get();
        if (_blockSize == 0) {
            return;
        }
        info.copiedPageCount = _copiedPageCount;
        info.droppedPageCount = _droppedPageCount;
        info.copying = _copying.get();
        info.currentGeneration = _currentAddress;
        info.currentJournalAddress = _writeBuffer == null ? 0 : _writeBufferAddress + _writeBuffer.position();
        info.currentJournalFile = addressToFile(_currentAddress).getPath();
        info.flushing = _flushing.get();
        info.journaledPageCount = _writePageCount;
        info.readPageCount = _readPageCount;
        if (_lastValidCheckpointJournalAddress != 0) {
            info.lastValidCheckpointSystemTime = _lastValidCheckpoint.getSystemTimeMillis();
            info.lastValidCheckpointTimestamp = _lastValidCheckpoint.getTimestamp();
            info.lastValidCheckpointJournalFile = addressToFile(_lastValidCheckpointJournalAddress).getPath();
            info.lastValidCheckpointJournalAddress = _lastValidCheckpointJournalAddress;
        } else {
            info.lastValidCheckpointSystemTime = 0;
            info.lastValidCheckpointTimestamp = 0;
            info.lastValidCheckpointJournalFile = null;
            info.lastValidCheckpointJournalAddress = 0;
        }
        info.blockSize = _blockSize;
        info.pageMapSize = _pageMap.size();
        info.baseAddress = _baseAddress;
        info.appendOnly = _appendOnly.get();
        info.fastCopying = _copyFast.get();
    }

    @Override
    public synchronized int getLiveTransactionMapSize() {
        return _liveTransactionMap.size();
    }

    @Override
    public synchronized int getPageMapSize() {
        return _pageMap.size();
    }

    @Override
    public synchronized int getPageListSize() {
        return _pageList.size();
    }

    @Override
    public synchronized long getBaseAddress() {
        return _baseAddress;
    }

    @Override
    public synchronized long getCurrentAddress() {
        return _currentAddress;
    }

    @Override
    public long getBlockSize() {
        return _blockSize;
    }

    @Override
    public boolean isAppendOnly() {
        return _appendOnly.get();
    }

    @Override
    public boolean isIgnoreMissingVolumes() {
        return _ignoreMissingVolume.get();
    }

    @Override
    public boolean isCopyingFast() {
        return _copyFast.get();
    }

    @Override
    public void setAppendOnly(final boolean appendOnly) {
        _appendOnly.set(appendOnly);
    }

    @Override
    public void setIgnoreMissingVolumes(final boolean ignore) {
        _ignoreMissingVolume.set(ignore);
    }

    @Override
    public void setCopyingFast(final boolean fast) {
        _copyFast.set(fast);
    }

    @Override
    public long getFlushInterval() {
        return _flusher.getPollInterval();
    }

    @Override
    public void setFlushInterval(final long flushInterval) {
        _flusher.setPollInterval(flushInterval);
    }

    @Override
    public long getCopierInterval() {
        return _copier.getPollInterval();
    }

    @Override
    public void setCopierInterval(final long copierInterval) {
        _copier.setPollInterval(copierInterval);
    }

    @Override
    public void setRollbackPruningEnabled(final boolean rollbackPruning) {
        _rollbackPruning.set(rollbackPruning);
    }

    @Override
    public void setWritePagePruningEnabled(final boolean writePruning) {
        _writePagePruning.set(writePruning);
    }

    public JournalManager(final Persistit persistit) {
        _persistit = persistit;
    }

    @Override
    public boolean isClosed() {
        return _closed.get();
    }

    @Override
    public boolean isCopying() {
        return _copying.get();
    }

    @Override
    public boolean isRollbackPruningEnabled() {
        return _rollbackPruning.get();
    }

    @Override
    public boolean isWritePagePruningEnabled() {
        return _writePagePruning.get();
    }

    @Override
    public String getJournalFilePath() {
        return _journalFilePath;
    }

    @Override
    public long getJournaledPageCount() {
        return _writePageCount;
    }

    @Override
    public long getReadPageCount() {
        return _readPageCount;
    }

    @Override
    public long getCopiedPageCount() {
        return _copiedPageCount;
    }

    @Override
    public long getDroppedPageCount() {
        return _droppedPageCount;
    }

    public long getEarliestCommittedTransactionTimestamp() {
        return _earliestCommittedTimestamp;
    }

    public long getEarliestAbortedTransactionTimestamp() {
        return _earliestAbortedTimestamp;
    }

    @Override
    public long getJournalCreatedTime() {
        return _journalCreatedTime;
    }

    public Checkpoint getLastValidCheckpoint() {
        return _lastValidCheckpoint;
    }

    @Override
    public long getLastValidCheckpointTimestamp() {
        return _lastValidCheckpoint.getTimestamp();
    }

    @Override
    public String getLastCopierException() {
        return Util.toString(_copier.getLastException());
    }

    @Override
    public String getLastFlusherException() {
        return Util.toString(_flusher.getLastException());
    }

    @Override
    public long getLastValidCheckpointTimeMillis() {
        return _lastValidCheckpoint.getSystemTimeMillis();
    }

    @Override
    public long getSlowIoAlertThreshold() {
        return _slowIoAlertThreshold;
    }

    @Override
    public long getTotalCompletedCommits() {
        return _totalCommits.get();
    }

    @Override
    public long getCommitCompletionWaitTime() {
        return _totalCommitWaitTime.get() / NS_PER_MS;
    }

    @Override
    public long getCurrentTimestamp() {
        return _persistit.getCurrentTimestamp();
    }

    @Override
    public void setSlowIoAlertThreshold(final long slowIoAlertThreshold) {
        Util.rangeCheck(slowIoAlertThreshold, MINIMUM_SLOW_ALERT_THRESHOLD_MS, MAXIMUM_SLOW_ALERT_THRESHOLD_MS);
        _slowIoAlertThreshold = slowIoAlertThreshold;
    }

    @Override
    public int getUrgentFileCountThreshold() {
        return _urgentFileCountThreshold;
    }

    @Override
    public void setUrgentFileCountThreshold(final int threshold) {
        Util.rangeCheck(threshold, MINIMUM_URGENT_FILE_COUNT_THRESHOLD, MAXIMUM_URGENT_FILE_COUNT_THRESHOLD);
        _urgentFileCountThreshold = threshold;

    }

    /**
     * Compute an "urgency" factor that determines how vigorously the
     * JOURNAL_COPIER thread should perform I/O. This number is computed on a
     * scale of 0 to 10; larger values are intended make the thread work harder.
     * A value of 10 suggests the copier should run flat-out.
     *
     * @return the JOURNAL_COPIER urgency on a scale of 0 to 10
     */
    @Override
    public int urgency() {
        if (_copyFast.get()) {
            return URGENT;
        }
        final int remainingFiles = _urgentFileCountThreshold - getJournalFileCount();
        return Math.max(0, Math.min(URGENT - remainingFiles, URGENT));
    }

    /**
     * Introduce delay into an application thread when JOURNAL_COPIER thread is
     * behind. The amount of delay depends on the value returned by
     * {@link #urgency()}. When that value is {@value #URGENT} then the delay is
     * {@value #URGENT_COMMIT_DELAY_MILLIS} milliseconds.
     *
     * @throws PersistitInterruptedException
     */
    public void throttle() throws PersistitInterruptedException {
        final long interval = _throttleSleepInterval;
        if (interval > 0) {
            Util.sleep(interval);
        }
    }

    int handleForVolume(final Volume volume) throws PersistitException {
        if (volume.getHandle() != 0) {
            return volume.getHandle();
        }
        if (!_allowHandlesForTempVolumesAndTrees && volume.isTemporary()) {
            throw new IllegalStateException("Creating handle for temporary volume " + volume);
        }
        if (volume.getHandle() != 0) {
            return volume.getHandle();
        }
        synchronized (this) {
            if (volume.getHandle() != 0) {
                return volume.getHandle();
            }
            Integer handle = _volumeToHandleMap.get(volume);
            if (handle == null) {
                handle = Integer.valueOf(++_handleCounter);
                Debug.$assert0.t(!_handleToVolumeMap.containsKey(handle));
                writeVolumeHandleToJournal(volume, handle.intValue());
                _volumeToHandleMap.put(volume, handle);
                _handleToVolumeMap.put(handle, volume);
            }
            return volume.setHandle(handle.intValue());
        }
    }

    synchronized int handleForTree(final TreeDescriptor td, final boolean create) throws PersistitException {
        if (td.getVolumeHandle() == -1) {
            // Tree in transient volume -- don't journal updates to it
            return -1;
        }
        Integer handle = _treeToHandleMap.get(td);
        if (handle == null) {
            if (!create) {
                return -1;
            }
            handle = Integer.valueOf(++_handleCounter);
            Debug.$assert0.t(!_handleToTreeMap.containsKey(handle));
            if (td.getVolumeHandle() != Volume.LOCK_VOLUME_HANDLE) {
                writeTreeHandleToJournal(td, handle.intValue());
            }
            _treeToHandleMap.put(td, handle);
            _handleToTreeMap.put(handle, td);
        }
        return handle.intValue();
    }

    int handleForTree(final Tree tree) throws PersistitException {
        if (!_allowHandlesForTempVolumesAndTrees && tree.getVolume().isTemporary() && !tree.getVolume().isLockVolume()) {
            throw new IllegalStateException("Creating handle for temporary tree " + tree);
        }
        if (tree.getHandle() != 0) {
            return tree.getHandle();
        }
        synchronized (this) {
            if (tree.getHandle() != 0) {
                return tree.getHandle();
            }
            final TreeDescriptor td = new TreeDescriptor(handleForVolume(tree.getVolume()), tree.getName());
            return tree.setHandle(handleForTree(td, true));
        }
    }

    Tree treeForHandle(final int handle) throws PersistitException {
        final TreeDescriptor td = lookupTreeHandle(handle);
        if (td == null) {
            return null;
        }
        final Volume volume = volumeForHandle(td.getVolumeHandle());
        if (volume == null) {
            return null;
        }
        return volume.getStructure().getTreeInternal(td.getTreeName());
    }

    Volume volumeForHandle(final int handle) throws PersistitException {
        final Volume volume = lookupVolumeHandle(handle);
        if (volume == null) {
            if (handle == Volume.LOCK_VOLUME_HANDLE) {
                return _persistit.getLockVolume();
            } else {
                return null;
            }
        }
        if (!volume.isOpened()) {
            volume.open(_persistit);
        }
        return volume;
    }

    synchronized Volume getVolumeByName(final String volumeName) {
        for (final Volume v : _handleToVolumeMap.values()) {
            if (volumeName.equals(v.getName())) {
                return v;
            }
        }
        return null;
    }

    @Override
    public synchronized Volume lookupVolumeHandle(final int handle) {
        return _handleToVolumeMap.get(Integer.valueOf(handle));
    }

    public synchronized TreeDescriptor lookupTreeHandle(final int handle) {
        return _handleToTreeMap.get(Integer.valueOf(handle));
    }

    private void readFully(final ByteBuffer bb, final long address) throws PersistitIOException,
            CorruptJournalException {
        //
        // If necessary read the bytes out of the _writeBuffer
        // before they have been written out to the file. This code
        // requires the _writeBuffer to be a HeapByteBuffer.
        //
        final int position = bb.position();
        final int length = bb.remaining();
        synchronized (this) {
            if (address >= _writeBufferAddress && address + length <= _currentAddress) {
                assert _writeBufferAddress + _writeBuffer.position() == _currentAddress : String.format(
                        "writeBufferAddress=%,d position=%,d currentAddress=%,d", _writeBufferAddress,
                        _writeBuffer.position(), _currentAddress);
                final int wbPosition = _writeBuffer.position();
                final int wbLimit = _writeBuffer.limit();
                _writeBuffer.position((int) (address - _writeBufferAddress));
                _writeBuffer.limit((int) (address - _writeBufferAddress) + length);
                bb.put(_writeBuffer);
                _writeBuffer.limit(wbLimit);
                _writeBuffer.position(wbPosition);
                bb.position(position);
                return;
            }
        }

        final FileChannel fc = getFileChannel(address);

        long fileAddr = addressToOffset(address);
        while (bb.remaining() > 0) {
            int count;
            try {
                count = fc.read(bb, fileAddr);
            } catch (final IOException ioe) {
                throw new PersistitIOException(ioe);
            }
            if (count < 0) {
                final File file = addressToFile(address);
                throw new CorruptJournalException(String.format("End of file at %s:%d(%,d)", file, fileAddr, address));
            }
            fileAddr += count;
        }
        bb.limit(bb.position());
        bb.position(position);
    }

    boolean readPageFromJournal(final Buffer buffer) throws PersistitIOException {
        final int bufferSize = buffer.getBufferSize();
        final long pageAddress = buffer.getPageAddress();
        final ByteBuffer bb = buffer.getByteBuffer();

        final Volume volume = buffer.getVolume();
        final PageNode pn = lookupUpPageNode(pageAddress, volume);
        if (pn == null) {
            return false;
        }
        bb.position(0);
        final long recordPageAddress = readPageBufferFromJournal(pn, bb);
        _persistit.getIOMeter().chargeReadPageFromJournal(volume, pageAddress, bufferSize, pn.getJournalAddress(),
                buffer.getIndex());

        if (pageAddress != recordPageAddress) {
            throw new CorruptJournalException("Record at " + pn + " is not volume/page " + buffer.toString());
        }

        if (bb.limit() != bufferSize) {
            throw new CorruptJournalException("Record at " + pn + " is wrong size: expected/actual=" + bufferSize + "/"
                    + bb.limit());
        }
        _readPageCount++;
        buffer.getVolume().getStatistics().bumpReadCounter();
        return true;
    }

    PageNode lookupUpPageNode(final long pageAddress, final Volume volume) {
        PageNode pnLookup = null;
        synchronized (this) {
            final Integer volumeHandle = _volumeToHandleMap.get(volume);
            if (volumeHandle != null) {
                pnLookup = _pageMap.get(new PageNode(volumeHandle, pageAddress, -1, -1));
            }
        }

        if (pnLookup == null) {
            return null;
        }

        final PageNode pn = new PageNode(pnLookup.getVolumeHandle(), pnLookup.getPageAddress(),
                pnLookup.getJournalAddress(), pnLookup.getTimestamp());
        sequence(PAGE_MAP_READ_INVALIDATE_A);

        /*
         * If the page is still valid, use the values saved in pn so we don't
         * lose them mid-processing. We can use it because it was in the map
         * when we first looked and that means it is is still in the journal.
         * The journal won't go away because of the claim on buffer preventing
         * new checkpoints and that keeps the copier from deleting it.
         */
        if (pnLookup.isInvalid()) {
            return null;
        }
        return pn;
    }

    private long readPageBufferFromJournal(final PageNode pn, final ByteBuffer bb) throws PersistitIOException,
            CorruptJournalException {
        final int at = bb.position();
        bb.limit(at + PA.OVERHEAD);
        readFully(bb, pn.getJournalAddress());
        if (bb.remaining() < PA.OVERHEAD) {
            throw new CorruptJournalException("Record at " + pn.toStringJournalAddress(this) + " is incomplete");
        }
        final int type = JournalRecord.getType(bb);
        final int payloadSize = JournalRecord.getLength(bb) - PA.OVERHEAD;
        final int leftSize = PA.getLeftSize(bb);
        final int bufferSize = PA.getBufferSize(bb);
        final long pageAddress = PA.getPageAddress(bb);

        if (type != PA.TYPE) {
            throw new CorruptJournalException("Record at " + pn.toStringJournalAddress(this) + " is not a PAGE record");
        }

        if (leftSize < 0 || payloadSize < leftSize || payloadSize > bufferSize) {
            throw new CorruptJournalException("Record at " + pn.toStringJournalAddress(this)
                    + " invalid sizes: recordSize= " + payloadSize + " leftSize=" + leftSize + " bufferSize="
                    + bufferSize);
        }

        if (pageAddress != pn.getPageAddress() && pn.getPageAddress() != -1) {
            throw new CorruptJournalException("Record at " + pn.toStringJournalAddress(this)
                    + " mismatched page address: expected/actual=" + pn.getPageAddress() + "/" + pageAddress);
        }

        bb.limit(at + payloadSize).position(at);
        readFully(bb, pn.getJournalAddress() + PA.OVERHEAD);

        final int rightSize = payloadSize - leftSize;
        System.arraycopy(bb.array(), leftSize + at, bb.array(), bufferSize - rightSize + at, rightSize);
        Arrays.fill(bb.array(), leftSize + at, bufferSize - rightSize + at, (byte) 0);
        bb.limit(bb.capacity()).position(at).limit(at + bufferSize);
        return pageAddress;
    }

    /**
     * Method used by diagnostic tools to attempt to read a page from journal
     *
     * @param address
     *            journal address
     * @param _bb
     *            ByteBuffer in which to return the result
     * @return pageAddress of the page at the specified location, or -1 if the
     *         address does not reference a valid page
     * @throws PersistitException
     */
    Buffer readPageBuffer(final long address) throws PersistitException {
        ByteBuffer bb = ByteBuffer.allocate(PA.OVERHEAD);
        readFully(bb, address);
        if (bb.remaining() < PA.OVERHEAD) {
            return null;
        }
        final int type = JournalRecord.getType(bb);
        final int payloadSize = JournalRecord.getLength(bb) - PA.OVERHEAD;
        final int leftSize = PA.getLeftSize(bb);
        final int bufferSize = PA.getBufferSize(bb);
        final long pageAddress = PA.getPageAddress(bb);
        final int volumeHandle = PA.getVolumeHandle(bb);

        if (type != PA.TYPE || leftSize < 0 || payloadSize < leftSize || payloadSize > bufferSize) {
            return null;
        }

        final BufferPool pool = _persistit.getBufferPool(bufferSize);
        final Buffer buffer = new Buffer(bufferSize, -1, pool, _persistit);
        buffer.setPageAddressAndVolume(pageAddress, volumeForHandle(volumeHandle));
        bb = buffer.getByteBuffer();
        bb.limit(payloadSize).position(0);
        readFully(bb, address + PA.OVERHEAD);

        if (leftSize > 0) {
            final int rightSize = payloadSize - leftSize;
            System.arraycopy(bb.array(), leftSize, bb.array(), bufferSize - rightSize, rightSize);
            Arrays.fill(bb.array(), leftSize, bufferSize - rightSize, (byte) 0);
        }
        bb.limit(bufferSize).position(0);
        final boolean acquired = buffer.claim(true, 0);
        assert acquired : "buffer in use";
        buffer.load();
        buffer.release();
        return buffer;
    }

    private void advance(final int recordSize) {
        Debug.$assert1.t(recordSize > 0 && recordSize + _writeBuffer.position() <= _writeBuffer.capacity());
        _currentAddress += recordSize;
        _writeBuffer.position(_writeBuffer.position() + recordSize);
    }

    /**
     * Write a JH (journal header) record. This record must be written to the
     * beginning of the journal file. Note that this method does not call
     * {@link #prepareWriteBuffer(int)} - the write buffer needs to be ready to
     * receive the JH record.
     *
     * @throws PersistitException
     */
    synchronized void writeJournalHeader() throws PersistitException {
        JH.putType(_writeBuffer);
        JournalRecord.putTimestamp(_writeBuffer, epochalTimestamp());
        JH.putVersion(_writeBuffer, VERSION);
        JH.putBlockSize(_writeBuffer, _blockSize);
        JH.putBaseJournalAddress(_writeBuffer, _baseAddress);
        JH.putCurrentJournalAddress(_writeBuffer, _currentAddress);
        JH.putJournalCreatedTime(_writeBuffer, _journalCreatedTime);
        JH.putFileCreatedTime(_writeBuffer, System.currentTimeMillis());
        JH.putPath(_writeBuffer, addressToFile(_currentAddress).getPath());
        final int recordSize = JournalRecord.getLength(_writeBuffer);
        _persistit.getIOMeter().chargeWriteOtherToJournal(recordSize, _currentAddress);
        advance(recordSize);
    }

    /**
     * Write the JE (journal end) record. This record must be written to the end
     * of each complete journal file. Note that this method does not call
     * {@link #prepareWriteBuffer(int)} - the write buffer needs to be ready to
     * receive the JE record.
     *
     * @throws PersistitException
     */
    synchronized void writeJournalEnd() throws PersistitException {
        if (_writeBufferAddress != Long.MAX_VALUE) {
            //
            // prepareWriteBuffer contract guarantees there's always room in
            // the write buffer for this record.
            //
            JE.putType(_writeBuffer);
            JournalRecord.putTimestamp(_writeBuffer, epochalTimestamp());
            JournalRecord.putLength(_writeBuffer, JE.OVERHEAD);
            JE.putCurrentJournalAddress(_writeBuffer, _currentAddress);
            JE.putBaseAddress(_writeBuffer, _baseAddress);
            JE.putJournalCreatedTime(_writeBuffer, _journalCreatedTime);
            _persistit.getIOMeter().chargeWriteOtherToJournal(JE.OVERHEAD, _currentAddress);
            advance(JE.OVERHEAD);
        }
    }

    synchronized void writePageMap() throws PersistitException {
        int count = 0;
        for (final PageNode lastPageNode : _pageMap.values()) {
            PageNode pageNode = lastPageNode;
            while (pageNode != null) {
                count++;
                pageNode = pageNode.getPrevious();
            }
        }
        for (final PageNode lastPageNode : _branchMap.values()) {
            PageNode pageNode = lastPageNode;
            while (pageNode != null) {
                count++;
                pageNode = pageNode.getPrevious();
            }
        }

        final int recordSize = PM.OVERHEAD + PM.ENTRY_SIZE * count;
        prepareWriteBuffer(recordSize);
        PM.putType(_writeBuffer);
        JournalRecord.putLength(_writeBuffer, recordSize);
        JournalRecord.putTimestamp(_writeBuffer, epochalTimestamp());
        advance(PM.OVERHEAD);
        int offset = 0;
        for (final PageNode lastPageNode : _pageMap.values()) {
            PageNode pageNode = lastPageNode;
            while (pageNode != null) {
                PM.putEntry(_writeBuffer, offset / PM.ENTRY_SIZE, pageNode.getTimestamp(),
                        pageNode.getJournalAddress(), pageNode.getVolumeHandle(), pageNode.getPageAddress());

                offset += PM.ENTRY_SIZE;
                count--;
                if (count == 0 || offset + PM.ENTRY_SIZE >= _writeBuffer.remaining()) {
                    advance(offset);
                    offset = 0;
                }
                if (PM.ENTRY_SIZE >= _writeBuffer.remaining()) {
                    flush();
                }
                pageNode = pageNode.getPrevious();
            }
        }
        for (final PageNode lastPageNode : _branchMap.values()) {
            PageNode pageNode = lastPageNode;
            while (pageNode != null) {
                PM.putEntry(_writeBuffer, offset / PM.ENTRY_SIZE, pageNode.getTimestamp(),
                        pageNode.getJournalAddress(), pageNode.getVolumeHandle(), pageNode.getPageAddress());

                offset += PM.ENTRY_SIZE;
                count--;
                if (count == 0 || offset + PM.ENTRY_SIZE >= _writeBuffer.remaining()) {
                    advance(offset);
                    offset = 0;
                }
                if (PM.ENTRY_SIZE >= _writeBuffer.remaining()) {
                    flush();
                }
                pageNode = pageNode.getPrevious();
            }
        }
        Debug.$assert0.t(count == 0);
        _persistit.getIOMeter().chargeWriteOtherToJournal(recordSize, _currentAddress - recordSize);
    }

    synchronized void writeTransactionMap() throws PersistitException {
        int count = _liveTransactionMap.size();
        final int recordSize = TM.OVERHEAD + TM.ENTRY_SIZE * count;
        prepareWriteBuffer(recordSize);
        TM.putType(_writeBuffer);
        JournalRecord.putLength(_writeBuffer, recordSize);
        JournalRecord.putTimestamp(_writeBuffer, epochalTimestamp());
        advance(TM.OVERHEAD);
        int offset = 0;
        for (final TransactionMapItem ts : _liveTransactionMap.values()) {
            TM.putEntry(_writeBuffer, offset / TM.ENTRY_SIZE, ts.getStartTimestamp(), ts.getCommitTimestamp(),
                    ts.getStartAddress(), ts.getLastRecordAddress());
            offset += TM.ENTRY_SIZE;
            count--;
            if (count == 0 || offset + TM.ENTRY_SIZE >= _writeBuffer.remaining()) {
                advance(offset);
                offset = 0;
            }
            if (TM.ENTRY_SIZE >= _writeBuffer.remaining()) {
                flush();
            }
        }

        Debug.$assert0.t(count == 0);
        _persistit.getIOMeter().chargeWriteOtherToJournal(recordSize, _currentAddress - recordSize);
    }

    synchronized void writeCheckpointToJournal(final Checkpoint checkpoint) throws PersistitException {
        //
        // Make sure all prior journal entries are committed to disk before
        // writing this record.
        //
        force();
        //
        // Prepare room for CP.OVERHEAD bytes in the journal. If doing so
        // started a new journal file then there's no need to write another
        // CP record.
        //
        if (!prepareWriteBuffer(CP.OVERHEAD)) {
            final long address = _currentAddress;
            JournalRecord.putLength(_writeBuffer, CP.OVERHEAD);
            CP.putType(_writeBuffer);
            JournalRecord.putTimestamp(_writeBuffer, checkpoint.getTimestamp());
            CP.putSystemTimeMillis(_writeBuffer, checkpoint.getSystemTimeMillis());
            CP.putBaseAddress(_writeBuffer, _baseAddress);
            _persistit.getIOMeter().chargeWriteOtherToJournal(CP.OVERHEAD, _currentAddress);
            advance(CP.OVERHEAD);
            force();

            checkpointWritten(checkpoint);

            _persistit.getLogBase().checkpointWritten.log(checkpoint, address);
            _persistit.getIOMeter().chargeWriteOtherToJournal(CP.OVERHEAD, address);
        }

        _lastValidCheckpoint = checkpoint;
        _lastValidCheckpointJournalAddress = _currentAddress - CP.OVERHEAD;
        _lastValidCheckpointBaseAddress = _baseAddress;
    }

    void writePageToJournal(final Buffer buffer) throws PersistitException {

        final Volume volume;
        final int recordSize;

        synchronized (this) {

            if (!buffer.isTemporary() && buffer.getTimestamp() < _lastValidCheckpoint.getTimestamp()) {
                _persistit.getLogBase().lateWrite.log(_lastValidCheckpoint, buffer);
            }

            volume = buffer.getVolume();
            final int handle = handleForVolume(volume);
            int leftSize;
            int rightSize;
            if (buffer.isDataPage() || buffer.isIndexPage() || buffer.isGarbagePage()) {
                leftSize = buffer.getKeyBlockEnd();
                rightSize = buffer.getBufferSize() - buffer.getAlloc();
            } else {
                leftSize = 0;
                rightSize = buffer.getBufferSize();
            }

            recordSize = PA.OVERHEAD + leftSize + rightSize;

            prepareWriteBuffer(recordSize);
            Debug.$assert1.t(_writeBuffer.remaining() >= recordSize);

            final long address = _currentAddress;
            final int position = _writeBuffer.position();

            JournalRecord.putLength(_writeBuffer, recordSize);
            PA.putVolumeHandle(_writeBuffer, handle);
            PA.putType(_writeBuffer);
            JournalRecord.putTimestamp(_writeBuffer, buffer.isTemporary() ? -1 : buffer.getTimestamp());
            PA.putLeftSize(_writeBuffer, leftSize);
            PA.putBufferSize(_writeBuffer, buffer.getBufferSize());
            PA.putPageAddress(_writeBuffer, buffer.getPageAddress());
            advance(PA.OVERHEAD);

            if (leftSize > 0) {
                _writeBuffer.put(buffer.getBytes(), 0, leftSize);
                _writeBuffer.put(buffer.getBytes(), buffer.getBufferSize() - rightSize, rightSize);
            } else {
                _writeBuffer.put(buffer.getBytes());
            }
            Debug.$assert0.t(_writeBuffer.position() - position == recordSize);
            _currentAddress += recordSize - PA.OVERHEAD;

            final PageNode pageNode = new PageNode(handle, buffer.getPageAddress(), address, buffer.getTimestamp());
            _pageList.add(pageNode);
            PageNode oldPageNode = _pageMap.put(pageNode, pageNode);

            if (oldPageNode != null) {
                assert oldPageNode.getTimestamp() <= pageNode.getTimestamp();
            }
            final long checkpointTimestamp = _persistit.getTimestampAllocator().getProposedCheckpointTimestamp();
            if (oldPageNode != null && oldPageNode.getTimestamp() > checkpointTimestamp
                    && buffer.getTimestamp() > checkpointTimestamp) {
                oldPageNode.invalidate();
                oldPageNode = oldPageNode.getPrevious();
            }
            pageNode.setPrevious(oldPageNode);
            _writePageCount++;
        }
        _persistit.getIOMeter().chargeWritePageToJournal(volume, buffer.getPageAddress(), buffer.getBufferSize(),
                _currentAddress - recordSize, urgency(), buffer.getIndex());
    }

    /**
     * package-private for unit tests only.
     *
     * @param volume
     * @param handle
     * @throws PersistitException
     */
    synchronized void writeVolumeHandleToJournal(final Volume volume, final int handle) throws PersistitException {
        prepareWriteBuffer(IV.MAX_LENGTH);
        IV.putType(_writeBuffer);
        IV.putHandle(_writeBuffer, handle);
        IV.putVolumeId(_writeBuffer, volume.getId());
        JournalRecord.putTimestamp(_writeBuffer, epochalTimestamp());
        if (_persistit.getConfiguration().isUseOldVSpec()) {
            IV.putVolumeSpecification(_writeBuffer, volume.getName());
        } else {
            IV.putVolumeSpecification(_writeBuffer, volume.getSpecification().toString());
        }
        final int recordSize = JournalRecord.getLength(_writeBuffer);
        _persistit.getIOMeter().chargeWriteOtherToJournal(recordSize, _currentAddress);
        advance(recordSize);
    }

    synchronized void writeTreeHandleToJournal(final TreeDescriptor td, final int handle) throws PersistitException {
        prepareWriteBuffer(IT.MAX_LENGTH);
        IT.putType(_writeBuffer);
        IT.putHandle(_writeBuffer, handle);
        IT.putVolumeHandle(_writeBuffer, td.getVolumeHandle());
        JournalRecord.putTimestamp(_writeBuffer, epochalTimestamp());
        IT.putTreeName(_writeBuffer, td.getTreeName());
        final int recordSize = JournalRecord.getLength(_writeBuffer);
        _persistit.getIOMeter().chargeWriteOtherToJournal(recordSize, _currentAddress);
        advance(recordSize);
    }

    /**
     * <p>
     * Write a transaction or partial transaction to the journal as a TX record
     * containing a variable number of variable-length update records. The
     * supplied <code>buffer</code> contains the update records.
     * </p>
     * <p>
     * TX records typically represent a complete transaction, but in the case of
     * transactions with a large number of updates, there may be multiple TX
     * records. In that case each TX record but the last one written specifies a
     * commit timestamp value of zero indicating that the transaction has not
     * committed yet, and each TX record but the first one written specifies the
     * journal address of the previous one. These pointers allow the recovery
     * process find efficiently all the updates of a transaction that needs to
     * be rolled back.
     * </p>
     *
     * @param buffer
     *            The buffer containing the update records
     * @param startTimestamp
     *            Transaction start timestamp
     * @param commitTimestamp
     *            Transaction commit timestamp, or 0 if the transaction has not
     *            committed yet
     * @param backchainAddress
     *            Journal address of previous TX record written by this
     *            transaction, or 0 if there is to previous record
     *
     * @return
     * @throws PersistitException
     */
    synchronized long writeTransactionToJournal(final ByteBuffer buffer, final long startTimestamp,
            final long commitTimestamp, final long backchainAddress) throws PersistitException {
        final int recordSize = TX.OVERHEAD + buffer.position();
        prepareWriteBuffer(recordSize);
        final long address = _currentAddress;
        TX.putLength(_writeBuffer, recordSize);
        TX.putType(_writeBuffer);
        TX.putTimestamp(_writeBuffer, startTimestamp);
        TX.putCommitTimestamp(_writeBuffer, commitTimestamp);
        TX.putBackchainAddress(_writeBuffer, backchainAddress);
        _persistit.getIOMeter().chargeWriteTXtoJournal(recordSize, _currentAddress);
        advance(TX.OVERHEAD);
        try {
            buffer.flip();
            _writeBuffer.put(buffer);
        } finally {
            buffer.clear();
        }
        _currentAddress += recordSize - TX.OVERHEAD;
        if (commitTimestamp != ABORTED) {
            final long key = Long.valueOf(startTimestamp);
            TransactionMapItem item = _liveTransactionMap.get(key);
            if (item == null) {
                if (backchainAddress != 0) {
                    throw new IllegalStateException("Missing back-chained transaction for start timestamp "
                            + startTimestamp);
                }
                item = new TransactionMapItem(startTimestamp, address);
                _liveTransactionMap.put(startTimestamp, item);
            } else {
                if (backchainAddress == 0) {
                    throw new IllegalStateException("Duplicate transaction " + item);
                }
                if (item.isCommitted()) {
                    throw new IllegalStateException("Transaction already committed " + item);
                }
                item.setLastRecordAddress(address);
            }
            item.setCommitTimestamp(commitTimestamp);
        }
        return address;
    }

    static File journalPath(final String path) {
        final File file = new File(path);
        if (file.isDirectory()) {
            return new File(file, DEFAULT_JOURNAL_FILE_NAME);
        } else {
            return file;
        }
    }

    static long fileToGeneration(final File file) {
        final Matcher matcher = PATH_PATTERN.matcher(file.getName());
        if (matcher.matches()) {
            return Long.parseLong(matcher.group(2));
        } else {
            return -1;
        }
    }

    static String fileToPath(final File file) {
        final Matcher matcher = PATH_PATTERN.matcher(file.getPath());
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    static File generationToFile(final String path, final long generation) {
        return new File(String.format(PATH_FORMAT, path, generation));
    }

    File addressToFile(final long address) {
        return generationToFile(_journalFilePath, address / _blockSize);
    }

    long addressToOffset(final long address) {
        return address % _blockSize;
    }

    void setWriteBufferSize(final int size) {
        if (size < MINIMUM_BUFFER_SIZE || size > MAXIMUM_BUFFER_SIZE) {
            throw new IllegalArgumentException("Invalid write buffer size: " + size);
        }
        _writeBufferSize = size;
    }

    public void close() throws PersistitException {
        _closed.set(true);
        rollover();

        final JournalCopier copier = _copier;
        _copier = null;
        if (copier != null) {
            _persistit.waitForIOTaskStop(copier);
        }

        final JournalFlusher flusher = _flusher;
        _flusher = null;
        if (flusher != null) {
            _persistit.waitForIOTaskStop(flusher);
        }

        synchronized (this) {
            try {
                closeAllChannels();
            } catch (final IOException ioe) {
                throw new PersistitIOException(ioe);
            } finally {
                _handleToTreeMap.clear();
                _handleToVolumeMap.clear();
                _volumeToHandleMap.clear();
                _treeToHandleMap.clear();
                _pageMap.clear();
                _pageList.clear();
                _writeBuffer = null;
            }
        }
    }

    private void closeAllChannels() throws IOException {
        synchronized (this) {
            try {
                for (final FileChannel channel : _journalFileChannels.values()) {
                    if (channel != null) {
                        channel.close();
                    }
                }

            } finally {
                _journalFileChannels.clear();
            }
        }
    }

    /**
     * Abruptly stop (using {@link Thread#stop()}) the copier and flusher
     * threads. This method should be used only by tests.
     */
    void crash() throws IOException {
        IOTaskRunnable.crash(_flusher);
        IOTaskRunnable.crash(_copier);
        //
        // Even when simulating a crash do this to release
        // channels and therefore allow disk space to be returned to
        // the OS when the files are deleted.
        //
        closeAllChannels();
    }

    /**
     * Flushes the write buffer
     *
     * @throws PersistitException
     */
    synchronized long flush() throws PersistitException {
        _persistit.checkFatal();
        final long address = _writeBufferAddress;
        if (address != Long.MAX_VALUE && _writeBuffer != null) {

            assert _writeBufferAddress + _writeBuffer.position() == _currentAddress : String.format(
                    "writeBufferAddress=%,d position=%,d currentAddress=%,d", _writeBufferAddress,
                    _writeBuffer.position(), _currentAddress);

            try {
                if (_writeBuffer.position() > 0) {
                    final FileChannel channel = getFileChannel(address);
                    final long size = channel.size();
                    if (size < addressToOffset(address)) {
                        throw new CorruptJournalException(String.format(
                                "Journal file %s size %,d does not match current address %,d", addressToFile(address),
                                size, address));
                    }

                    _writeBuffer.flip();
                    boolean writeComplete = false;
                    final int written;
                    try {
                        /*
                         * Note: contract for FileChannel requires write to
                         * return normally only when all bytes have been
                         * written. (See java.nio.channels.WritableByteChannel
                         * #write(ByteBuffer), statement
                         * "Unless otherwise specified...")
                         */
                        channel.write(_writeBuffer, _writeBufferAddress % _blockSize);
                        /*
                         * Surprise: FileChannel#write does not throw an
                         * Exception if it successfully writes some bytes and
                         * then encounters a disk full condition. (Found this
                         * out empirically.)
                         */
                        writeComplete = _writeBuffer.remaining() == 0;
                    } finally {
                        written = _writeBuffer.position();
                        _writeBufferAddress += written;
                        if (writeComplete) {
                            if (_writeBuffer.capacity() != _writeBufferSize) {
                                _writeBuffer = ByteBuffer.allocate(_writeBufferSize);
                            } else {
                                _writeBuffer.clear();
                            }
                        } else {
                            /*
                             * If the buffer didn't get written, perhaps due to
                             * an interrupt or disk-full condition, then compact
                             * to remove any bytes from the buffer that actually
                             * did get written and reset other measurements.
                             */
                            _writeBuffer.compact();
                        }
                        final long remaining = _blockSize - (_writeBufferAddress % _blockSize);
                        if (remaining < (_writeBuffer.limit())) {
                            _writeBuffer.limit((int) remaining);
                        }
                    }

                    assert _writeBufferAddress + _writeBuffer.position() == _currentAddress : String.format(
                            "writeBufferAddress=%,d position=%,d currentAddress=%,d", _writeBufferAddress,
                            _writeBuffer.position(), _currentAddress);

                    _persistit.getIOMeter().chargeFlushJournal(written, address);
                    return _writeBufferAddress;
                }
            } catch (final IOException e) {
                throw new PersistitIOException("Writing to file " + addressToFile(address), e);
            }
        }
        return Long.MAX_VALUE;
    }

    /**
     * Force all data written to the journal file to disk.
     */
    @Override
    public void force() throws PersistitException {
        long address = Long.MAX_VALUE;
        try {
            address = flush();
            if (address != Long.MAX_VALUE) {
                final FileChannel channel = getFileChannel(address);
                channel.force(false);
            }
        } catch (final IOException e) {
            throw new PersistitIOException("Writing to file " + addressToFile(address), e);
        }
    }

    /**
     * Map a ByteBuffer to a file address, as needed to ensure client methods
     * can write their records. This method modifies the values of _writeBuffer,
     * _writeBufferAddress, and in case a new journal file is prepared (a
     * "roll-over" event), it also modifies _currentAddress to reflect the
     * current address in the new file.
     *
     * @param size
     *            Size of record to be written
     * @return <code>true</code> if and only if a new journal file was started
     * @throws PersistitException
     */
    private boolean prepareWriteBuffer(final int size) throws PersistitException {
        _persistit.checkFatal();
        boolean newJournalFile = false;
        if (getCurrentJournalSize() == 0) {
            flush();
            _writeBufferAddress = _currentAddress;
            startJournalFile();
            newJournalFile = true;
        }

        assert _writeBufferAddress + _writeBuffer.position() == _currentAddress : String.format(
                "writeBufferAddress=%,d position=%,d currentAddress=%,d", _writeBufferAddress, _writeBuffer.position(),
                _currentAddress);
        //
        // If the current journal file has room for the record, then return.
        //
        if (_writeBuffer.remaining() > size + JE.OVERHEAD) {
            return newJournalFile;
        }
        //
        // Otherwise, flush the write buffer and try again
        flush();

        if (_writeBuffer.remaining() > size + JE.OVERHEAD) {
            return newJournalFile;
        }
        //
        // In the special case of a record which may be longer than
        // the capacity of the buffer (e.g., the PageMap), then check whether
        // there is enough room in the file to hold the entire map. In that case
        // then the buffer is prepared because the PM and TM writers know how to
        // fill the buffer multiple times.
        //
        if (_writeBuffer.remaining() == _writeBuffer.capacity()) {
            final long remaining = _blockSize - getCurrentJournalSize();
            if (remaining > size + JE.OVERHEAD) {
                return newJournalFile;
            }
        }
        //
        // Finally if there's still not enough room we're committed to
        // rolling the journal.
        //
        rolloverWithNewFile();
        return true;
    }

    void rollover() throws PersistitException {
        rollover(false, false);
    }

    void rolloverWithNewFile() throws PersistitException {
        rollover(false, true);
    }

    void rolloverWithNewBaseAndFile() throws PersistitException {
        rollover(true, true);
    }

    private synchronized void rollover(final boolean setBaseAddress, final boolean startNewFile)
            throws PersistitException {
        if (_writeBufferAddress != Long.MAX_VALUE) {
            writeJournalEnd();
            flush();

            try {
                final long length = getCurrentJournalSize();
                final boolean matches = length == (_writeBuffer.position() + _writeBufferAddress) % _blockSize;
                final FileChannel channel = getFileChannel(_currentAddress);
                Debug.$assert1.t(matches);
                if (matches) {
                    channel.truncate(length);
                }
                channel.force(true);
            } catch (final IOException ioe) {
                throw new PersistitIOException(ioe);
            }
            _currentAddress = ((_currentAddress / _blockSize) + 1) * _blockSize;
            _writeBuffer.clear();
            _writeBufferAddress = _currentAddress;
            _isNewEpoch = false;

            if (setBaseAddress) {
                _baseAddress = _currentAddress;
            }
            if (startNewFile) {
                prepareWriteBuffer(JH.OVERHEAD);
            }
        }
    }

    /**
     * Timestamp marking the Page Map, Transaction Map and other records in the
     * journal header. This timestamp is used to discriminate between pages in a
     * "branch" history and the live history. See comments in
     * {@link RecoveryManager#scanLoadPageMap(long, long, int)} for details.
     *
     * @return either the current timestamp or the timestamp of the last valid
     *         checkpoint, depending on whether this journal file starts a new
     *         epoch.
     */
    private long epochalTimestamp() {
        return _isNewEpoch ? getLastValidCheckpointTimestamp() : _persistit.getCurrentTimestamp();
    }

    private void startJournalFile() throws PersistitException {
        //
        // Write the beginning of a new journal file.
        //
        // The information written here is designed to accelerate recovery.
        // The recovery process can simply read the JournalHeader and
        // subsequent records from the last journal file to load the page
        // map and live transaction map. The journal file is valid for
        // recovery only if the CP (checkpoint) record is present in the
        // recovered file.
        //
        writeJournalHeader();
        //
        // Write IV (identify volume) records for each volume in the handle
        // map
        //
        for (final Map.Entry<Integer, Volume> entry : _handleToVolumeMap.entrySet()) {
            writeVolumeHandleToJournal(entry.getValue(), entry.getKey().intValue());
        }
        //
        // Write IT (identify tree) records for each tree in the handle
        // map
        //
        for (final Map.Entry<Integer, TreeDescriptor> entry : _handleToTreeMap.entrySet()) {
            if (entry.getValue().getVolumeHandle() != Volume.LOCK_VOLUME_HANDLE) {
                writeTreeHandleToJournal(entry.getValue(), entry.getKey().intValue());
            }
        }
        //
        // Write the PM (Page Map) record
        //
        writePageMap();
        //
        // Write the TM (Transaction Map) record
        //
        writeTransactionMap();
        //
        // Finally, write the current CP (checkpoint) record.
        //
        writeCheckpointToJournal(_lastValidCheckpoint);
    }

    /**
     * Return the <code>FileChannel</code> for the journal file containing the
     * supplied <code>address</code>. If necessary, create a new
     * {@link MediatedFileChannel}.
     *
     * @param address
     *            the journal address of a record in the journal for which the
     *            corresponding channel will be returned
     * @throws PersistitException
     *             if the <code>MediatedFileChannel</code> cannot be created
     */
    synchronized FileChannel getFileChannel(final long address) throws PersistitIOException {
        if (address < _deleteBoundaryAddress || address > _currentAddress + _blockSize) {
            throw new IllegalArgumentException("Invalid journal address " + address + " outside of range ("
                    + _deleteBoundaryAddress + ":" + (_currentAddress + _blockSize) + ")");
        }
        final long generation = address / _blockSize;
        FileChannel channel = _journalFileChannels.get(generation);
        if (channel == null) {
            try {
                channel = new MediatedFileChannel(addressToFile(address), "rw");
                _journalFileChannels.put(generation, channel);
            } catch (final IOException ioe) {
                throw new PersistitIOException(ioe);
            }
        }
        return channel;
    }

    /**
     * Set the copyFast flag and then wait until all checkpointed pages have
     * been copied to their respective volumes, allowing the journal files to be
     * deleted. Pages modified after the last valid checkpoint cannot be copied.
     * <p>
     * Does nothing of the <code>appendOnly</code> is set.
     *
     * @throws PersistitException
     */
    @Override
    public void copyBack() throws Exception {
        if (!_appendOnly.get()) {
            _copyFast.set(true);
            final int exceptionCount = _copier.getExceptionCount();
            while (_copyFast.get()) {
                _copier.kick();
                Util.sleep(Persistit.SHORT_DELAY);
                if (_copier.getExceptionCount() != exceptionCount) {
                    throw _copier.getLastException();
                }
            }
        }
    }

    /**
     * Remove transactions and PageNode entries when possible due to completion
     * of a new checkpoint.
     *
     * @param checkpoint
     */
    private void checkpointWritten(final Checkpoint checkpoint) {

        //
        // Will become the earliest timestamp of any record needed to
        // be retained for recovery. For transactions containing LONG_RECORD
        // pages, those pages may be written to the journal with timestamps
        // earlier than the commitTimestamp of the transaction but they are
        // guaranteed to be written with timestamp values later than the
        // transaction's startTimestamp. Therefore we can't cull PageMap entries
        // later than this recoveryTimestamp because the pages they refer to may
        // be needed for recovery.
        //
        long recoveryTimestamp = checkpoint.getTimestamp();
        recoveryTimestamp = Math.min(Math.min(recoveryTimestamp, _earliestCommittedTimestamp),
                _earliestAbortedTimestamp);
        //
        // Remove all but the most recent PageNode version before the
        // checkpoint.
        //
        for (final PageNode pageNode : _pageMap.values()) {
            for (PageNode pn = pageNode; pn != null; pn = pn.getPrevious()) {
                if (pn.getTimestamp() < recoveryTimestamp) {
                    pn.removeHistory();
                    break;
                }
            }
        }
        //
        // Remove any PageNode from the branchMap having a timestamp less
        // than the checkpoint. Generally all such entries are removed after
        // the first checkpoint that has been established after recovery.
        //
        for (final Iterator<PageNode> iterator = _branchMap.values().iterator(); iterator.hasNext();) {
            final PageNode pageNode = iterator.next();
            if (pageNode.getTimestamp() < recoveryTimestamp) {
                iterator.remove();
            }
        }

        checkpoint.completed();
    }

    /**
     * Remove obsolete TransactionMapItem instances from the live transaction
     * map. An instance is obsolete if it refers to a transaction that committed
     * earlier than that last valid checkpoint (because all of the effects of
     * that transaction are now check-pointed into the B-Trees themselves) or if
     * it is from an aborted transaction that has no remaining MVV values.
     */
    void pruneObsoleteTransactions() {
        pruneObsoleteTransactions(isRollbackPruningEnabled());
    }

    void pruneObsoleteTransactions(final boolean rollbackPruningEnabled) {
        final long timestamp = _lastValidCheckpoint.getTimestamp();
        long earliestCommitted = Long.MAX_VALUE;
        long earliestAborted = Long.MAX_VALUE;
        final List<TransactionMapItem> toPrune = new ArrayList<TransactionMapItem>();
        final List<TransactionMapItem> toPruneCommited = new ArrayList<>();

        /*
         * Remove any committed transactions that committed before the
         * checkpoint. No need to keep a record of such a transaction since its
         * updates are now fully written to the journal in modified page images.
         */
        synchronized (this) {
            for (final Iterator<TransactionMapItem> iterator = _liveTransactionMap.values().iterator(); iterator
                    .hasNext();) {
                final TransactionMapItem item = iterator.next();
                if (item.isCommitted()) {
                    if (item.getCommitTimestamp() < timestamp) {
                        toPruneCommited.add(item);
                    } else if (item.getStartTimestamp() < earliestCommitted) {
                        earliestCommitted = item.getStartTimestamp();
                    }
                } else {
                    final TransactionStatus status;
                    status = _persistit.getTransactionIndex().getStatus(item.getStartTimestamp());
                    if (status == null || status.getTs() != item.getStartTimestamp()) {
                        iterator.remove();
                    } else if (status.getTc() == ABORTED && status.isNotified()) {
                        if (status.getMvvCount() == 0) {
                            iterator.remove();
                            sequence(RECOVERY_PRUNING_B);
                        } else {
                            if (item.getStartTimestamp() < earliestAborted) {
                                earliestAborted = item.getStartTimestamp();
                            }
                            if (rollbackPruningEnabled) {
                                toPrune.add(item);
                            }
                        }
                    }
                }
            }
            _earliestCommittedTimestamp = earliestCommitted;
            _earliestAbortedTimestamp = earliestAborted;
        }
        Collections.sort(toPruneCommited, TransactionMapItem.TRANSACTION_MAP_ITEM_COMPARATOR);
        final List<Long> startTimestamps = new ArrayList<>(toPruneCommited.size());
        for (final TransactionMapItem item : toPruneCommited) {
            try {
                synchronized (_player) {
                  _player.applyTransaction(item, _pruneCommited);
                }
                startTimestamps.add(item.getStartTimestamp());
            } catch (final PersistitException e) {
                _persistit.getLogBase().pruneException.log(e, item);
            }
        }
        synchronized (this)
        {
          _liveTransactionMap.keySet().removeAll(startTimestamps);
        }

        /*
         * Sort the toPrune list - since all members are aborted, the comparison
         * will be by startTimeStamp which is a good approximation of journal
         * address order.
         */
        Collections.sort(toPrune, TransactionMapItem.TRANSACTION_MAP_ITEM_COMPARATOR);
        for (final TransactionMapItem item : toPrune) {
            try {
                synchronized (_player) {
                    final TransactionStatus status;
                    status = _persistit.getTransactionIndex().getStatus(item.getStartTimestamp());
                    if (status != null && status.getTs() == item.getStartTimestamp() && status.getTc() == ABORTED
                            && status.isNotified() && status.getMvvCount() > 0) {
                        _player.applyTransaction(item, _listener);
                    }
                }
            } catch (final PersistitException e) {
                _persistit.getLogBase().pruneException.log(e, item);
            }
        }
    }

    /**
     * General method used to wait for durability. This method is used by all
     * three commit modes: SOFT, HARD and GROUP. The two parameters represent
     * time intervals in milliseconds.
     *
     * @param flushedTimestamp
     *            a timestamp taken after the transaction buffer belonging to
     *            the current transaction has been flushed.
     * @param leadTime
     *            time interval in milliseconds by which to anticipate I/O
     *            completion; the method will return as soon as the I/O
     *            operation that will flush the current generation of data is
     *            expected to complete within that time interval
     * @param stallTime
     *            time interval in milliseconds that this thread is willing to
     *            wait for I/O completion. If if the JOURNAL_FLUSHER is
     *            currently pausing, the pause time may be shortened to try to
     *            complete the I/O when requested. In particular, a value of
     *            zero indicates the I/O should start immediately.
     * @throws PersistitInterruptedException
     */

    void waitForDurability(final long flushedTimestamp, final long leadTime, final long stallTime)
            throws PersistitException {
        final JournalFlusher flusher = _flusher;
        if (flusher != null) {
            flusher.waitForDurability(flushedTimestamp, leadTime, stallTime);
        } else {
            throw new IllegalStateException("JOURNAL_FLUSHER is not running");
        }
    }

    public static class TreeDescriptor {

        final int _volumeHandle;

        final String _treeName;

        TreeDescriptor(final int volumeHandle, final String treeName) {
            _volumeHandle = volumeHandle;
            _treeName = treeName;
        }

        public int getVolumeHandle() {
            return _volumeHandle;
        }

        public String getTreeName() {
            return _treeName;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null || !(obj instanceof TreeDescriptor)) {
                return false;
            }
            final TreeDescriptor td = (TreeDescriptor) obj;
            return td._treeName.equals(_treeName) && td._volumeHandle == _volumeHandle;
        }

        @Override
        public int hashCode() {
            return _treeName.hashCode() ^ _volumeHandle;
        }

        @Override
        public String toString() {
            return "{" + _volumeHandle + "}" + _treeName;
        }
    }

    /**
     * A PageNode represents the existence of a copy of a page in the journal.
     * It links to previously created PageNode objects which refer to earlier
     * versions of the same page. These earlier instances are truncated whenever
     * a later version of the same page has been checkpointed.
     *
     * PageNode instances are designed to serve as both Key and Value fields of
     * the _pageNodeMap. The general rubric when adding a page to the journal is
     * to construct a PageNode representing the page image, and then use it to
     * perform a lookup in the _pageNodeMap. If there is no matching PageNode
     * already in the map then simply add the new one. If there is a matching
     * PageNode, link it to the new one then replace the entry in the map.
     *
     * This class implement Comparable on the page address. This is used in
     * forming a sorted set of PageNodes so that we can copy pages in roughly
     * sequential order to each Volume file.
     */
    public static class PageNode {

        final int _volumeHandle;

        final long _pageAddress;

        final long _timestamp;

        long _journalAddress;

        int _offset;

        PageNode _previous;

        PageNode(final int volumeHandle, final long pageAddress) {
            this(volumeHandle, pageAddress, Long.MIN_VALUE, -1);
        }

        PageNode(final int volumeHandle, final long pageAddress, final long journalAddress, final long timestamp) {
            this._volumeHandle = volumeHandle;
            this._pageAddress = pageAddress;
            this._journalAddress = journalAddress;
            this._timestamp = timestamp;
        }

        /**
         * Construct a copy, also copying members of the linked list. Used by
         * #queryPageMap.
         */
        PageNode(final PageNode pageNode) {
            _volumeHandle = pageNode._volumeHandle;
            _pageAddress = pageNode._pageAddress;
            _journalAddress = pageNode._journalAddress;
            _timestamp = pageNode._timestamp;
            _offset = pageNode._offset;
            final PageNode previous = pageNode._previous;
            if (previous != null) {
                _previous = new PageNode(previous);
            }
        }

        /**
         * @return the previous
         */
        public PageNode getPrevious() {
            return _previous;
        }

        /**
         * @param previous
         *            the previous to set
         */
        public void setPrevious(final PageNode previous) {
            if (previous != null) {
                assert _timestamp >= previous._timestamp;
            }
            this._previous = previous;
        }

        /**
         * @return the volumeHandle
         */
        public int getVolumeHandle() {
            return _volumeHandle;
        }

        /**
         * @return the pageAddress
         */
        public long getPageAddress() {
            return _pageAddress;
        }

        /**
         * @return the journalAddress
         */
        public long getJournalAddress() {
            return _journalAddress;
        }

        /**
         * @return the timestamp
         */
        public long getTimestamp() {
            return _timestamp;
        }

        public void setOffset(final int offset) {
            _offset = offset;
        }

        public int getOffset() {
            return _offset;
        }

        @Override
        public int hashCode() {
            return _volumeHandle ^ (int) _pageAddress ^ (int) (_pageAddress >>> 32);
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null || !(obj instanceof PageNode)) {
                return false;
            }
            final PageNode pn = (PageNode) obj;
            return _pageAddress == pn._pageAddress && _volumeHandle == pn._volumeHandle;
        }

        @Override
        public String toString() {
            return String.format("[%d]%d@%d{%d}%s", _volumeHandle, _pageAddress, _journalAddress, _timestamp,
                    _previous == null ? "" : "+");
        }

        public String toString(final JournalManager jman) {
            final Volume volume = jman._handleToVolumeMap.get(_volumeHandle);
            if (volume == null) {
                return toString();
            }
            return String.format("%s:%d@%d{%d}%s", volume, _pageAddress, _journalAddress, _timestamp,
                    _previous == null ? "" : "+");
        }

        public String toStringPageAddress(final VolumeHandleLookup lvh) {
            final Volume volume = lvh.lookupVolumeHandle(_volumeHandle);
            return String.format("%s:%d", volume == null ? String.valueOf(_volumeHandle) : volume.toString(),
                    _pageAddress);
        }

        public String toStringJournalAddress(final VolumeHandleLookup lvn) {
            return String.format("%d{%d}%s", _journalAddress, _timestamp, _previous == null ? "" : "+");

        }

        final static Comparator<PageNode> READ_COMPARATOR = new Comparator<PageNode>() {

            @Override
            public int compare(final PageNode a, final PageNode b) {
                if (!a.isInvalid() && !b.isInvalid()) {
                    return a.getJournalAddress() > b.getJournalAddress() ? 1 : a.getJournalAddress() < b
                            .getJournalAddress() ? -1 : 0;
                }
                if (a.isInvalid() && !b.isInvalid()) {
                    return -1;
                }
                if (!a.isInvalid() && b.isInvalid()) {
                    return 1;
                }
                if (a._volumeHandle != b._volumeHandle) {
                    return a._volumeHandle - b._volumeHandle;
                }
                return a._pageAddress > b._pageAddress ? 1 : a._pageAddress < b._pageAddress ? -1 : 0;
            }
        };

        final static Comparator<PageNode> WRITE_COMPARATOR = new Comparator<PageNode>() {

            @Override
            public int compare(final PageNode a, final PageNode b) {
                if (a.getVolumeHandle() != b.getVolumeHandle()) {
                    return a.getVolumeHandle() < b._volumeHandle ? -1 : 1;
                }
                return a.getPageAddress() < b.getPageAddress() ? -1 : a.getPageAddress() > b.getPageAddress() ? 1 : 0;
            }
        };

        boolean isInvalid() {
            return _journalAddress == Long.MIN_VALUE;
        }

        void invalidate() {
            _journalAddress = Long.MIN_VALUE;
        }

        void removeHistory() {
            PageNode pn = getPrevious();
            setPrevious(null);
            while (pn != null) {
                final PageNode previous = pn.getPrevious();
                pn.invalidate();
                pn.setPrevious(null);
                pn = previous;
            }
        }
    }

    public static class TransactionMapItem implements Comparable<TransactionMapItem> {

        private final long _startAddress;

        private final long _startTimestamp;

        private long _commitTimestamp;

        private long _lastRecordAddress;

        TransactionMapItem(final long startTimestamp, final long address) {
            _startTimestamp = startTimestamp;
            _commitTimestamp = 0;
            _startAddress = address;
            _lastRecordAddress = address;
        }

        TransactionMapItem(final TransactionMapItem item) {
            _startAddress = item._startAddress;
            _startTimestamp = item._startTimestamp;
            _commitTimestamp = item._commitTimestamp;
            _lastRecordAddress = item._lastRecordAddress;
        }

        public long getStartAddress() {
            return _startAddress;
        }

        public long getStartTimestamp() {
            return _startTimestamp;
        }

        public long getCommitTimestamp() {
            return _commitTimestamp;
        }

        public long getLastRecordAddress() {
            return _lastRecordAddress;
        }

        void setCommitTimestamp(final long commitTimestamp) {
            _commitTimestamp = commitTimestamp;
        }

        void setLastRecordAddress(final long address) {
            _lastRecordAddress = address;
        }

        public boolean isCommitted() {
            return _commitTimestamp > 0;
        }

        public boolean isAborted() {
            return _commitTimestamp == ABORTED;
        }

        @Override
        public String toString() {
            return String.format("TStatus %,d{%,d}%s", _startAddress, _commitTimestamp, isCommitted() ? "c" : "u");
        }

        @Override
        public int compareTo(final TransactionMapItem ts) {
            if (isCommitted()) {
                return ts.getCommitTimestamp() < _commitTimestamp ? 1 : ts.getCommitTimestamp() > _commitTimestamp ? -1
                        : 0;
            } else {
                return ts.isCommitted() ? -1 : ts.getStartTimestamp() < _startTimestamp ? 1
                        : ts.getStartTimestamp() > _startTimestamp ? -1 : 0;
            }
        }

        final static Comparator<TransactionMapItem> TRANSACTION_MAP_ITEM_COMPARATOR = new Comparator<TransactionMapItem>() {

            @Override
            public int compare(final TransactionMapItem a, final TransactionMapItem b) {
                return a.getLastRecordAddress() > b.getLastRecordAddress() ? 1 : a.getLastRecordAddress() < b
                        .getLastRecordAddress() ? -1 : 0;
            }
        };

    }

    private class JournalCopier extends IOTaskRunnable {

        private volatile boolean _shouldStop = false;
        private final ByteBuffer _bb = ByteBuffer.allocate(DEFAULT_COPY_BUFFER_SIZE);
        private final List<PageNode> _copyList = new ArrayList<PageNode>(_copiesPerCycle);
        int _lastCyclePagesWritten;

        JournalCopier() {
            super(JournalManager.this._persistit);
        }

        void start() {
            start("JOURNAL_COPIER", _copierInterval);
        }

        @Override
        public void runTask() throws Exception {

            _copying.set(true);
            try {
                _copyList.clear();
                if (!_appendOnly.get()) {
                    selectForCopy(_copyList);
                    if (!_copyList.isEmpty()) {
                        readForCopy(_copyList, _bb);
                    }
                    if (!_copyList.isEmpty()) {
                        writeForCopy(_copyList, _bb);
                    }
                }
                cleanupForCopy(_copyList);
                _lastCyclePagesWritten = _copyList.size();
                if (_copyList.isEmpty()) {
                    _copyFast.set(false);
                }
            } finally {
                _copying.set(false);
            }

            long throttleInterval = 0;
            if (!_appendOnly.get()) {
                final int urgency = urgency();
                if (urgency == URGENT) {
                    throttleInterval = URGENT_COMMIT_DELAY_MILLIS;
                } else if (urgency > ALMOST_URGENT) {
                    throttleInterval = GENTLE_COMMIT_DELAY_MILLIS;
                }
            }
            if (throttleInterval != _throttleSleepInterval) {
                _throttleSleepInterval = throttleInterval;
            }

        }

        @Override
        protected boolean shouldStop() {
            return _closed.get() || _shouldStop;
        }

        /**
         * Return a nice interval, in milliseconds, to wait between copierCycle
         * invocations. When number of journal reach the PAGE_COPIER_URGENT threshold,
         * the page copier run flat-out.
         */
        @Override
        public long pollInterval() {
            final long pollInterval = super.getPollInterval();
            if (_lastCyclePagesWritten == 0 || getJournalFileCount() < PAGE_COPIER_URGENT) {
                return pollInterval;
            }
            return 0;
        }
    }

    private class JournalFlusher extends IOTaskRunnable {

        volatile long _lastExceptionTimestamp = 0;
        volatile Exception _lastException = null;

        long[] _ioTimes = new long[IO_MEASUREMENT_CYCLES];
        int _ioCycle;
        volatile long _expectedIoTime;
        volatile long _startTime;
        volatile long _endTime;
        volatile long _startTimestamp;
        volatile long _endTimestamp;

        JournalFlusher() {
            super(JournalManager.this._persistit);
        }

        void start() {
            start("JOURNAL_FLUSHER", _flushInterval);
        }

        /**
         * General method used to wait for durability. {@See
         * JournalManager#waitForDurability(long, long, long)}.
         *
         * @throws PersistitInterruptedException
         */
        private void waitForDurability(final long flushedTimestamp, final long leadTime, final long stallTime)
                throws PersistitException {
            /*
             * Commit is known durable once the JOURNAL_FLUSHER thread has
             * posted an _endTimestamp larger than flushedTimestamp.
             */
            final long now = System.nanoTime();
            long remainingStallTime = stallTime;

            while (true) {
                /*
                 * Detect whether an I/O cycle is in progress; if so estimate
                 * how much more time (in nanoseconds) it will require to
                 * complete.
                 */
                long estimatedRemainingIoNanos = -1;
                long startTime;
                long endTime;
                long startTimestamp;
                long endTimestamp;

                /*
                 * Spin until values are stable
                 */
                while (true) {
                    startTimestamp = _startTimestamp;
                    endTimestamp = _endTimestamp;
                    startTime = _startTime;
                    endTime = _endTime;
                    if (startTimestamp == _startTimestamp && endTimestamp == _endTimestamp) {
                        if (flushedTimestamp > startTimestamp && startTimestamp > endTimestamp) {
                            estimatedRemainingIoNanos = Math.max(startTime + _expectedIoTime - now, 0);
                        }
                        break;
                    }
                    Util.spinSleep();
                }

                if (endTimestamp > flushedTimestamp && startTimestamp > flushedTimestamp) {
                    /*
                     * Done - commit is durable
                     */
                    break;
                }

                long remainingSleepNanos;
                if (estimatedRemainingIoNanos == -1) {
                    remainingSleepNanos = Math.max(0, _flushInterval - (now - endTime));
                } else {
                    remainingSleepNanos = _flushInterval;
                }

                long estimatedNanosToFinish;
                if (startTimestamp < flushedTimestamp) {
                    estimatedNanosToFinish = remainingSleepNanos + _expectedIoTime;
                } else {
                    estimatedNanosToFinish = estimatedRemainingIoNanos;
                }

                if (leadTime > 0 && leadTime * NS_PER_MS >= estimatedNanosToFinish) {
                    /*
                     * If the caller specified an leadTime interval larger than
                     * the estimated time remaining in the cycle, then return
                     * immediately. This handles the "soft" commit case.
                     */
                    break;
                } else if (estimatedRemainingIoNanos == -1) {
                    /*
                     * If there is no I/O in progress, then wait as long as
                     * possible (determined by stallTime) before kicking the
                     * JOURNAL_FLUSHER to write the caller's transaction.
                     */
                    if (remainingStallTime > 0) {
                        Util.sleep(remainingStallTime);
                        remainingStallTime = 0;
                    } else {
                        kick();
                        Util.spinSleep();
                    }
                } else {
                    /*
                     * Otherwise wait for concurrent I/O operation to finish. Do
                     * this by polling because our experiments with using locks
                     * here showed significant excess CPU consumption.
                     */
                    Util.spinSleep();
                }
            }
            if (_lastExceptionTimestamp > flushedTimestamp) {
                final Exception e = _lastException;
                if (e instanceof PersistitException) {
                    throw (PersistitException) e;
                } else {
                    throw new PersistitException(e);
                }
            }
            _totalCommits.incrementAndGet();
            _totalCommitWaitTime.addAndGet(System.nanoTime() - now);
        }

        @Override
        protected void runTask() {
            _flushing.set(true);
            try {
                try {
                    /*
                     * This lock is intended only to help other threads in
                     * waitForDurability to know when the I/O operation has
                     * finished.
                     */
                    try {
                        _startTimestamp = _persistit.getTimestampAllocator().updateTimestamp();
                        _startTime = System.nanoTime();
                        /*
                         * Flush the write buffer and call FileChannel.force().
                         */
                        force();

                    } finally {
                        _endTime = System.nanoTime();
                        _endTimestamp = _persistit.getTimestampAllocator().updateTimestamp();
                    }

                    final long elapsed = _endTime - _startTime;
                    _totalFlushCycles.incrementAndGet();
                    _totalFlushIoTime.addAndGet(elapsed);
                    _ioTimes[_ioCycle] = elapsed;
                    _ioCycle = (_ioCycle + 1) % IO_MEASUREMENT_CYCLES;

                    long avg = 0;
                    for (int index = 0; index < IO_MEASUREMENT_CYCLES; index++) {
                        avg += _ioTimes[index];
                    }
                    avg /= IO_MEASUREMENT_CYCLES;

                    _expectedIoTime = avg;
                    if (elapsed > _slowIoAlertThreshold * NS_PER_MS) {
                        _persistit.getLogBase().longJournalIO.log(elapsed / NS_PER_MS, IO_MEASUREMENT_CYCLES, avg
                                / NS_PER_MS);
                    }

                } catch (final Exception e) {
                    if (e instanceof InterruptedException || e instanceof FatalErrorException) {
                        _closed.set(true);
                    } else if (e instanceof PersistitException) {
                        _persistit.getAlertMonitor().post(
                                new Event(AlertLevel.ERROR, _persistit.getLogBase().journalWriteError, e,
                                        addressToFile(_writeBufferAddress), addressToOffset(_writeBufferAddress)),
                                AlertMonitor.JOURNAL_CATEGORY);
                    } else {
                        _persistit.getLogBase().journalWriteError.log(e, addressToFile(_writeBufferAddress),
                                addressToOffset(_writeBufferAddress));
                    }
                }
            } finally {
                _flushing.set(false);
            }

        }

        @Override
        protected boolean shouldStop() {
            return _closed.get();
        }
    }

    synchronized void selectForCopy(final List<PageNode> list) {
        list.clear();
        if (!_appendOnly.get()) {
            final long timeStampUpperBound = Math.min(getLastValidCheckpointTimestamp(), _copierTimestampLimit);
            for (final Iterator<PageNode> iterator = _pageList.iterator(); iterator.hasNext();) {
                final PageNode pageNode = iterator.next();
                for (PageNode pn = pageNode; pn != null && !pn.isInvalid(); pn = pn.getPrevious()) {
                    if (pn.getTimestamp() < timeStampUpperBound) {
                        list.add(pn);
                        break;
                    }
                }
                if (list.size() >= _copiesPerCycle) {
                    break;
                }
            }
        }
    }

    void readForCopy(final List<PageNode> list, final ByteBuffer bb) throws PersistitException {
        Collections.sort(list, PageNode.READ_COMPARATOR);
        bb.clear();

        Volume volume = null;
        int handle = -1;

        for (final Iterator<PageNode> iterator = list.iterator(); iterator.hasNext();) {

            final PageNode pageNode = iterator.next();
            if (pageNode.isInvalid()) {
                iterator.remove();
                continue;
            }
            pageNode.setOffset(-1);
            if (pageNode.getVolumeHandle() != handle) {
                handle = -1;
                try {
                    volume = volumeForHandle(pageNode.getVolumeHandle());
                    handle = volume.getHandle();
                } catch (final VolumeNotFoundException vnfe) {
                    // Deal with this in writeForCopy
                    continue;
                }
            }
            if (volume == null) {
                // Deal with this in writeForCopy
                continue;
            }

            final int at = bb.position();
            final long pageAddress;
            try {
                final PageNode stablePageNode = new PageNode(pageNode);
                if (pageNode.isInvalid()) {
                    iterator.remove();
                    continue;
                }
                pageAddress = readPageBufferFromJournal(stablePageNode, bb);
                _persistit.getIOMeter().chargeCopyPageFromJournal(volume, pageAddress, volume.getPageSize(),
                        stablePageNode.getJournalAddress(), urgency());
            } catch (final PersistitException ioe) {
                _persistit
                        .getAlertMonitor()
                        .post(new Event(AlertLevel.ERROR, _persistit.getLogBase().copyException, ioe, volume,
                                pageNode.getPageAddress(), pageNode.getJournalAddress()), AlertMonitor.JOURNAL_CATEGORY);
                throw ioe;
            }

            Debug.$assert0.t(pageAddress == pageNode.getPageAddress());
            pageNode.setOffset(at);

            if (bb.limit() - at != volume.getStructure().getPageSize()) {
                throw new CorruptJournalException(pageNode.toStringPageAddress(this) + " bufferSize " + bb.limit()
                        + " does not match " + volume + " bufferSize " + volume.getPageSize() + " at "
                        + pageNode.toStringJournalAddress(this));
            }

            bb.position(bb.limit());
        }
    }

    void writeForCopy(final List<PageNode> list, final ByteBuffer bb) throws PersistitException {
        Collections.sort(list, PageNode.WRITE_COMPARATOR);
        Volume volume = null;
        int handle = -1;
        final Set<Volume> volumes = new HashSet<Volume>();

        for (final Iterator<PageNode> iterator = list.iterator(); iterator.hasNext();) {
            final PageNode pageNode = iterator.next();

            if (pageNode.getVolumeHandle() != handle) {
                handle = -1;
                volume = null;
                Volume candidate = null;
                try {
                    candidate = lookupVolumeHandle(pageNode.getVolumeHandle());
                    if (candidate != null) {
                        if (!candidate.isOpened()) {
                            candidate.open(_persistit);
                        }
                        handle = pageNode.getVolumeHandle();
                        volume = candidate;
                    }
                } catch (final VolumeNotFoundException vnfe) {
                    _persistit.getAlertMonitor().post(
                            new Event(AlertLevel.WARN, _persistit.getLogBase().missingVolume, candidate,
                                    pageNode.getJournalAddress()), AlertMonitor.MISSING_VOLUME_CATEGORY);
                    if (_ignoreMissingVolume.get()) {
                        _persistit.getLogBase().lostPageFromMissingVolume.log(pageNode.getPageAddress(), candidate,
                                pageNode.getJournalAddress());
                        // Not removing the page from the List here will cause
                        // cleanupForCopy to remove it from
                        // the page map.
                        continue;
                    }
                }
            }
            if (volume == null || volume.isClosed()) {
                // Remove from the List so that below we won't remove it from
                // from the pageMap.
                iterator.remove();
                continue;
            }

            final long pageAddress = pageNode.getPageAddress();
            volume.getStorage().extend(pageAddress);
            final int pageSize = volume.getPageSize();
            final int at = pageNode.getOffset();
            bb.limit(bb.capacity()).position(at).limit(at + pageSize);

            try {
                volume.getStorage().writePage(bb, pageAddress);
                volumes.add(volume);
            } catch (final PersistitException ioe) {
                _persistit.getLogBase().copyException.log(ioe, volume, pageNode.getPageAddress(),
                        pageNode.getJournalAddress());
                throw ioe;
            }

            _copiedPageCount++;
            _persistit.getIOMeter().chargeCopyPageToVolume(volume, pageAddress, volume.getPageSize(),
                    pageNode.getJournalAddress(), urgency());
        }

        for (final Volume vol : volumes) {
            vol.getStorage().force();
        }

    }

    private void cleanupForCopy(final List<PageNode> list) throws PersistitException {
        //
        // Files and FileChannels no longer needed for recovery.
        //
        final List<FileChannel> obsoleteFileChannels = new ArrayList<FileChannel>();
        final List<File> obsoleteFiles = new ArrayList<File>();

        // Address of the first file needed for recovery
        long deleteBoundary = 0;

        synchronized (this) {
            for (final PageNode copiedPageNode : list) {
                PageNode pageNode = _pageMap.get(copiedPageNode);
                if (pageNode.getJournalAddress() == copiedPageNode.getJournalAddress()) {
                    pageNode.removeHistory();
                    pageNode.invalidate();
                    final PageNode pn = _pageMap.remove(pageNode);
                    assert pn == copiedPageNode;
                } else {
                    PageNode previous = pageNode.getPrevious();
                    while (previous != null) {
                        if (previous.getJournalAddress() == copiedPageNode.getJournalAddress()) {
                            // No need to keep the previous entry, or any of
                            // its predecessors
                            pageNode.removeHistory();
                            break;
                        } else {
                            pageNode = previous;
                            previous = pageNode.getPrevious();
                        }
                    }
                }
            }
            _droppedPageCount += cleanupPageList() - list.size();
            //
            // Will hold the address of the first record containing information
            // not yet copied back into a Volume, and therefore required for
            // recovery.
            //
            long recoveryBoundary = _currentAddress;
            //
            // Detect first journal address holding a mapped page
            // required for recovery
            //

            for (final PageNode pageNode : _pageMap.values()) {
                //
                // If there are multiple versions, we need to keep
                // the most recent one that has been checkpointed.
                //
                for (PageNode pn = pageNode; pn != null; pn = pn.getPrevious()) {
                    if (!pn.isInvalid() && pn.getJournalAddress() < recoveryBoundary) {
                        recoveryBoundary = pn.getJournalAddress();
                    }
                }
            }
            //
            // Detect first journal address still holding an uncheckpointed
            // Transaction required for recovery.
            //
            for (final Iterator<TransactionMapItem> iterator = _liveTransactionMap.values().iterator(); iterator
                    .hasNext();) {
                final TransactionMapItem item = iterator.next();
                if (item.getStartAddress() < recoveryBoundary) {
                    recoveryBoundary = item.getStartAddress();
                }
            }

            if (recoveryBoundary < _baseAddress) {
                throw new IllegalStateException(String.format("Retrograde base address %,d is less than current %,d",
                        recoveryBoundary, _baseAddress));
            }

            _baseAddress = recoveryBoundary;
            for (deleteBoundary = _deleteBoundaryAddress; deleteBoundary + _blockSize <= _lastValidCheckpointBaseAddress; deleteBoundary += _blockSize) {
                final long generation = deleteBoundary / _blockSize;
                final FileChannel channel = _journalFileChannels.remove(generation);
                if (channel != null) {
                    obsoleteFileChannels.add(channel);
                }
                obsoleteFiles.add(addressToFile(deleteBoundary));
            }
            //
            // Conditions mean that there is no active content in the
            // journal and the current journal file has more than RT bytes
            // in it where RT is the "rolloverThreshold". When these
            // conditions are met then we force a rollover and cause the
            // current journal file to be deleted. This behavior keeps
            // the journal small when there are no un-checkpointed pages
            // or transactions.
            //
            if (_baseAddress == _currentAddress && _lastValidCheckpointBaseAddress >= _currentAddress - CP.OVERHEAD
                    && (getCurrentJournalSize() > rolloverThreshold())) {
                final FileChannel channel = _journalFileChannels.remove(_currentAddress / _blockSize);
                if (channel != null) {
                    obsoleteFileChannels.add(channel);
                }
                obsoleteFiles.add(addressToFile(_currentAddress));
                rolloverWithNewBaseAndFile();
            }
        }

        for (final FileChannel channel : obsoleteFileChannels) {
            if (channel != null) {
                try {
                    channel.close();
                } catch (final IOException e) {
                    // TODO - log this?
                    // Ignored for now - this simply means we can't close
                    // a file we don't need any more.
                }
            }
        }

        boolean deleted = true;
        for (final File file : obsoleteFiles) {
            if (!file.delete()) {
                deleted = false;
                // TODO - log this.
                // Ignored for now - this simply means we can't delete
                // a file we don't need any more.
            }
        }
        if (deleted) {
            _deleteBoundaryAddress = deleteBoundary;
        }
        reportJournalFileCount();
    }

    /**
     * Remove obsolete PageNodes from the page list.
     *
     * @return Count of removed PageNode instances.
     */
    int cleanupPageList() {
        final int size = _pageList.size();
        int from;
        for (from = 0; from < size && !_pageList.get(from).isInvalid(); from++)
            ;
        int to = from;
        for (from = from + 1; from < size; from++) {
            final PageNode pn = _pageList.get(from);
            if (!pn.isInvalid()) {
                _pageList.set(to++, pn);
            }
        }
        if (size > to) {
            _pageList.removeRange(to, size);
        }
        return size - to;
    }

    synchronized void truncate(final Volume volume, final long timestamp) {
        for (final PageNode lastPageNode : _pageMap.values()) {
            PageNode pageNode = lastPageNode;
            while (pageNode != null) {
                if (volume.getHandle() == pageNode.getVolumeHandle() && pageNode.getTimestamp() < timestamp) {
                    pageNode.invalidate();
                }
                pageNode = pageNode.getPrevious();
            }
        }
    }

    private void reportJournalFileCount() {
        /*
         * Does not need synchronization since only the JOURNAL_COPIER thread
         * calls this
         */
        final int journalFileCount = getJournalFileCount();
        if (journalFileCount != _lastReportedJournalFileCount) {
            if (journalFileCount > TOO_MANY_ERROR_THRESHOLD + _urgentFileCountThreshold) {
                _persistit.getAlertMonitor()
                        .post(new Event(AlertLevel.ERROR, _persistit.getLogBase().tooManyJournalFilesError,
                                journalFileCount), AlertMonitor.MANY_JOURNAL_FILES);
            } else if (journalFileCount > TOO_MANY_WARN_THRESHOLD + _urgentFileCountThreshold) {
                _persistit.getAlertMonitor()
                        .post(new Event(AlertLevel.WARN, _persistit.getLogBase().tooManyJournalFilesWarning,
                                journalFileCount), AlertMonitor.MANY_JOURNAL_FILES);
            } else {
                _persistit.getAlertMonitor().post(
                        new Event(AlertLevel.NORMAL, _persistit.getLogBase().normalJournalFileCount, journalFileCount),
                        AlertMonitor.MANY_JOURNAL_FILES);
            }
            _lastReportedJournalFileCount = journalFileCount;
        }
    }

    private class JournalTransactionPlayerSupport implements TransactionPlayerSupport {

        final ByteBuffer _readBuffer = ByteBuffer.allocate(Transaction.TRANSACTION_BUFFER_SIZE
                + JournalRecord.TX.OVERHEAD);

        @Override
        public void read(final long address, final int size) throws PersistitIOException {
            _readBuffer.clear().limit(size);
            readFully(_readBuffer, address);
        }

        @Override
        public ByteBuffer getReadBuffer() {
            return _readBuffer;
        }

        @Override
        public void convertToLongRecord(final Value value, final int treeHandle, final long address,
                final long commitTimestamp) throws PersistitException {
            // Do nothing - long record value does not need to be recovered for
            // pruning
        }

        @Override
        public Persistit getPersistit() {
            return _persistit;
        }
    }

    class PruneTransactionPlayer implements TransactionPlayerListener
    {

      @Override
      public void startRecovery(long address, long timestamp) throws PersistitException
      {
      }

      @Override
      public void startTransaction(long address, long timestamp, long commitTimestamp) throws PersistitException
      {
      }

      @Override
      public void store(long address, long timestamp, Exchange exchange) throws PersistitException
      {
      }

      @Override
      public void removeKeyRange(long address, long startTimestamp, Exchange exchange, Key from, Key to)
          throws PersistitException
      {
        try {
          exchange.prune(from, to);
      } catch (final RebalanceException e) {
          // ignore
      }
      }

      @Override
      public void removeTree(long address, long timestamp, Exchange exchange) throws PersistitException
      {
      }

      @Override
      public void delta(long address, long timestamp, Tree tree, int index, int accumulatorType, long value)
          throws PersistitException
      {
      }

      @Override
      public void endTransaction(long address, long timestamp) throws PersistitException
      {
      }

      @Override
      public void endRecovery(long address, long timestamp) throws PersistitException
      {
      }

      @Override
      public boolean requiresLongRecordConversion()
      {
        return false;
      }

      @Override
      public boolean createTree(long timestamp) throws PersistitException
      {
        return false;
      }

    }

    class ProactiveRollbackListener implements TransactionPlayerListener {

        TransactionStatus status;

        @Override
        public void store(final long address, final long timestamp, final Exchange exchange) throws PersistitException {
            exchange.prune();
        }

        @Override
        public void removeKeyRange(final long address, final long timestamp, final Exchange exchange, final Key from,
                final Key to) throws PersistitException {
            try {
                exchange.prune(from, to);
            } catch (final RebalanceException e) {
                // ignore
            }
        }

        @Override
        public void removeTree(final long address, final long timestamp, final Exchange exchange)
                throws PersistitException {
            // TODO
        }

        @Override
        public void delta(final long address, final long timestamp, final Tree tree, final int index,
                final int accumulatorType, final long value) throws PersistitException {
            // Nothing to to undo.
        }

        @Override
        public void startRecovery(final long address, final long timestamp) throws PersistitException {
            // Default: do nothing
        }

        @Override
        public void startTransaction(final long address, final long startTimestamp, final long commitTimestamp)
                throws PersistitException {
            // Default: do nothing
            status = _persistit.getTransactionIndex().getStatus(startTimestamp);
        }

        @Override
        public void endTransaction(final long address, final long timestamp) throws PersistitException {
            final TransactionStatus ts = _persistit.getTransactionIndex().getStatus(timestamp);
            /*
             * Can be null because the MVV count became zero and
             * TransactionIndex already removed it.
             */
            if (ts != null) {
                if (ts.getMvvCount() > 0 && _persistit.isInitialized()) {
                    _persistit.getLogBase().pruningIncomplete.log(ts,
                            TransactionPlayer.addressToString(address, timestamp));
                }
            }
        }

        @Override
        public void endRecovery(final long address, final long timestamp) throws PersistitException {
            // Default: do nothing
        }

        @Override
        public boolean requiresLongRecordConversion() {
            return false;
        }

        @Override
        public boolean createTree(final long timestamp) throws PersistitException {
            return false;
        }

    }

    /**
     * Extend ArrayList to export the removeRange method.
     */
    @SuppressWarnings("serial")
    static class RangeRemovingArrayList<T> extends ArrayList<T> {
        @Override
        public void removeRange(final int fromIndex, final int toIndex) {
            super.removeRange(fromIndex, toIndex);
        }
    }

    private long rolloverThreshold() {
        return _closed.get() ? 0 : ROLLOVER_THRESHOLD;
    }

    /**
     * @return number of internal handle values that have been assigned so far
     */
    public int getHandleCount() {
        return _handleCounter;
    }

    long getLastValidCheckpointBaseAddress() {
        return _lastValidCheckpointBaseAddress;
    }

    /**
     * For use only by unit tests that test page maps, etc.
     *
     * @param handleToVolumeMap
     */
    synchronized void unitTestInjectVolumes(final Map<Integer, Volume> handleToVolumeMap) {
        _handleToVolumeMap.putAll(handleToVolumeMap);
    }

    /**
     * For use only by unit tests that test page maps, etc.
     *
     * @param handleToVolumeMap
     */
    void unitTestInjectPageMap(final Map<PageNode, PageNode> pageMap) {
        _pageMap.putAll(pageMap);
    }

    void unitTestInjectTransactionMap(final Map<Long, TransactionMapItem> transactionMap) {
        _liveTransactionMap.putAll(transactionMap);
    }

    void unitTestClearTransactionMap() {
        _liveTransactionMap.clear();
    }

    long getCurrentJournalSize() {
        return _currentAddress % _blockSize;
    }

    long getWriteBufferAddress() {
        return _writeBufferAddress;
    }

    int getJournalFileCount() {
        return (int) (_currentAddress / _blockSize - _baseAddress / _blockSize) + 1;
    }

    synchronized boolean unitTestTxnExistsInLiveMap(final Long startTimestamp) {
        return _liveTransactionMap.containsKey(startTimestamp);
    }

    void unitTestInjectPageList(final List<PageNode> list) {
        _pageList.addAll(list);
    }

    boolean unitTestPageListEquals(final List<PageNode> list) {
        return list.equals(_pageList);
    }

    synchronized List<File> unitTestGetAllJournalFiles() {
        final List<File> files = new ArrayList<File>();
        for (final Long address : _journalFileChannels.keySet()) {
            files.add(addressToFile(address));
        }
        return files;
    }

    void unitTestAllowHandlesForTemporaryVolumesAndTrees() {
        _allowHandlesForTempVolumesAndTrees = true;
    }

    public PageNode queryPageNode(final int volumeHandle, final long pageAddress) {
        final PageNode pn = _pageMap.get(new PageNode(volumeHandle, pageAddress, -1, -1));
        if (pn != null) {
            return new PageNode(pn);
        } else {
            return null;
        }
    }

    public PageNode queryBranchNode(final int volumeHandle, final long pageAddress) {
        final PageNode pn = _branchMap.get(new PageNode(volumeHandle, pageAddress, -1, -1));
        if (pn != null) {
            return new PageNode(pn);
        } else {
            return null;
        }
    }

    public TransactionMapItem queryTransactionMap(final long timestamp) {
        final TransactionMapItem item = _liveTransactionMap.get(timestamp);
        if (item != null) {
            return new TransactionMapItem(item);
        } else {
            return null;
        }
    }

    public SortedMap<Integer, Volume> queryVolumeMap() {
        return new TreeMap<Integer, Volume>(_handleToVolumeMap);
    }

    public SortedMap<Integer, TreeDescriptor> queryTreeMap() {
        return new TreeMap<Integer, TreeDescriptor>(_handleToTreeMap);
    }
}
