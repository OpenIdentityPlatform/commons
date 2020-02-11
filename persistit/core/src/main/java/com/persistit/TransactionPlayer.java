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

import static com.persistit.TransactionIndex.ts2vh;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.persistit.AlertMonitor.AlertLevel;
import com.persistit.AlertMonitor.Event;
import com.persistit.JournalManager.TransactionMapItem;
import com.persistit.JournalManager.TreeDescriptor;
import com.persistit.JournalRecord.D0;
import com.persistit.JournalRecord.D1;
import com.persistit.JournalRecord.DR;
import com.persistit.JournalRecord.DT;
import com.persistit.JournalRecord.SR;
import com.persistit.JournalRecord.TX;
import com.persistit.exception.CorruptJournalException;
import com.persistit.exception.PersistitException;
import com.persistit.exception.VolumeNotFoundException;

/**
 * Read and apply transaction from the journal to the live database. To apply a
 * transaction, this class calls methods of a TransactionPlayerListener.
 */

class TransactionPlayer {

    private final AtomicLong appliedUpdates = new AtomicLong();
    private final AtomicLong ignoredUpdates = new AtomicLong();
    private final AtomicLong failedUpdates = new AtomicLong();

    interface TransactionPlayerListener {

        void startRecovery(long address, long timestamp) throws PersistitException;

        void startTransaction(long address, long timestamp, long commitTimestamp) throws PersistitException;

        void store(long address, long timestamp, Exchange exchange) throws PersistitException;

        void removeKeyRange(long address, long startTimestamp, Exchange exchange, Key from, Key to)
                throws PersistitException;

        void removeTree(long address, long timestamp, Exchange exchange) throws PersistitException;

        void delta(long address, long timestamp, Tree tree, int index, int accumulatorType, long value)
                throws PersistitException;

        void endTransaction(long address, long timestamp) throws PersistitException;

        void endRecovery(long address, long timestamp) throws PersistitException;

        boolean requiresLongRecordConversion();

        boolean createTree(long timestamp) throws PersistitException;
    }

    final TransactionPlayerSupport _support;

    TransactionPlayer(final TransactionPlayerSupport support) {
        _support = support;
    }

    void applyTransaction(final TransactionMapItem item, final TransactionPlayerListener listener)
            throws PersistitException {

        final List<Long> chainedAddress = new ArrayList<Long>();
        long address = item.getLastRecordAddress();

        int recordSize;
        int type;
        long startTimestamp;
        long commitTimestamp;
        long backchainAddress;

        for (;;) {
            _support.read(address, TX.OVERHEAD);
            recordSize = TX.getLength(_support.getReadBuffer());
            _support.read(address, recordSize);
            type = TX.getType(_support.getReadBuffer());
            startTimestamp = TX.getTimestamp(_support.getReadBuffer());
            commitTimestamp = TX.getCommitTimestamp(_support.getReadBuffer());
            backchainAddress = TX.getBackchainAddress(_support.getReadBuffer());
            if (recordSize < TX.OVERHEAD || recordSize > Transaction.TRANSACTION_BUFFER_SIZE + TX.OVERHEAD
                    || type != TX.TYPE) {
                throw new CorruptJournalException("Transaction record at " + addressToString(address)
                        + " has invalid length " + recordSize + " or type " + type);
            }
            if (startTimestamp != item.getStartTimestamp()) {
                throw new CorruptJournalException("Transaction record at " + addressToString(address)
                        + " has an invalid start timestamp: " + startTimestamp);
            }
            if (backchainAddress == 0) {
                if (address != item.getStartAddress()) {
                    throw new CorruptJournalException("Transaction record at " + addressToString(address)
                            + " has an invalid start " + addressToString(item.getStartAddress()));
                }
                break;
            }
            chainedAddress.add(0, address);
            address = backchainAddress;
        }

        listener.startTransaction(address, startTimestamp, commitTimestamp);
        applyTransactionUpdates(_support.getReadBuffer(), address, recordSize, startTimestamp, commitTimestamp,
                listener);

        for (final Long continuation : chainedAddress) {
            address = continuation.longValue();
            _support.read(address, TX.OVERHEAD);
            recordSize = TX.getLength(_support.getReadBuffer());
            if (recordSize < TX.OVERHEAD || recordSize > Transaction.TRANSACTION_BUFFER_SIZE + TX.OVERHEAD
                    || type != TX.TYPE) {
                throw new CorruptJournalException("Transaction record at " + addressToString(address)
                        + " has invalid length " + recordSize + " or type " + type);
            }
            _support.read(address, recordSize);
            applyTransactionUpdates(_support.getReadBuffer(), address, recordSize, startTimestamp, commitTimestamp,
                    listener);
        }
        listener.endTransaction(address, startTimestamp);

    }

    void applyTransactionUpdates(final ByteBuffer byteBuffer, final long address, final int recordSize,
            final long startTimestamp, final long commitTimestamp, final TransactionPlayerListener listener)
            throws PersistitException {
        ByteBuffer bb = byteBuffer;
        final int start = bb.position();
        int end = start + recordSize;
        int position = start + TX.OVERHEAD;
        
        final Tree directoryTree = _support.getPersistit().getSystemVolume().getDirectoryTree();

        while (position < end) {
            bb.position(position);
            final int innerSize = JournalRecord.getLength(bb);
            final int type = JournalRecord.getType(bb);
            try {
                switch (type) {
                case SR.TYPE: {
                    final int keySize = SR.getKeySize(bb);
                    final int treeHandle = SR.getTreeHandle(bb);

                    final Exchange exchange = getExchange(treeHandle, address, startTimestamp, listener);
                    if (exchange != null) {
                        exchange.ignoreTransactions();
                        final Key key = exchange.getKey();
                        final Value value = exchange.getValue();
                        System.arraycopy(bb.array(), bb.position() + SR.OVERHEAD, key.getEncodedBytes(), 0, keySize);
                        key.setEncodedSize(keySize);
                        final int valueSize = innerSize - SR.OVERHEAD - keySize;
                        value.ensureFit(valueSize);
                        System.arraycopy(bb.array(), bb.position() + SR.OVERHEAD + keySize, value.getEncodedBytes(), 0,
                                valueSize);
                        value.setEncodedSize(valueSize);

                        if (value.getEncodedSize() >= Buffer.LONGREC_SIZE
                                && (value.getEncodedBytes()[0] & 0xFF) == Buffer.LONGREC_TYPE) {
                            /*
                             * convertToLongRecord will pollute the
                             * getReadBuffer(). Therefore before calling it we
                             * need to copy the TX record to a fresh ByteBuffer.
                             */
                            if (bb == _support.getReadBuffer()) {
                                end = recordSize - (position - start);
                                bb = ByteBuffer.allocate(end);
                                bb.put(_support.getReadBuffer().array(), position, end);
                                bb.flip();
                                position = 0;
                            }
                            if (listener.requiresLongRecordConversion()) {
                                _support.convertToLongRecord(value, treeHandle, address, commitTimestamp);
                            }
                        }

                        listener.store(address, startTimestamp, exchange);
                        // Don't keep exchanges with enlarged value - let them be GC'd
                        if (exchange.getValue().getEncodedBytes().length < Value.DEFAULT_MAXIMUM_SIZE) {
                            releaseExchange(exchange, directoryTree);
                        }
                    }
                    appliedUpdates.incrementAndGet();
                    break;
                }

                case DR.TYPE: {
                    final int key1Size = DR.getKey1Size(bb);
                    final int elisionCount = DR.getKey2Elision(bb);
                    final Exchange exchange = getExchange(DR.getTreeHandle(bb), address, startTimestamp, listener);
                    if (exchange != null) {
                        exchange.ignoreTransactions();
                        final Key key1 = exchange.getAuxiliaryKey3();
                        final Key key2 = exchange.getAuxiliaryKey4();
                        System.arraycopy(bb.array(), bb.position() + DR.OVERHEAD, key1.getEncodedBytes(), 0, key1Size);
                        key1.setEncodedSize(key1Size);
                        final int key2Size = innerSize - DR.OVERHEAD - key1Size;
                        System.arraycopy(key1.getEncodedBytes(), 0, key2.getEncodedBytes(), 0, elisionCount);
                        System.arraycopy(bb.array(), bb.position() + DR.OVERHEAD + key1Size, key2.getEncodedBytes(),
                                elisionCount, key2Size);
                        key2.setEncodedSize(key2Size + elisionCount);
                        listener.removeKeyRange(address, startTimestamp, exchange, key1, key2);
                        releaseExchange(exchange, directoryTree);
                    }
                    appliedUpdates.incrementAndGet();
                    break;
                }

                case DT.TYPE: {
                    final Exchange exchange = getExchange(DT.getTreeHandle(bb), address, startTimestamp, listener);
                    if (exchange != null) {
                        listener.removeTree(address, startTimestamp, exchange);
                        releaseExchange(exchange, directoryTree);
                    }
                    appliedUpdates.incrementAndGet();
                    break;
                }

                case D0.TYPE: {
                    final Exchange exchange = getExchange(D0.getTreeHandle(bb), address, startTimestamp, listener);
                    if (exchange != null) {
                        /*
                         * Note that the commitTimestamp, not startTimestamp is
                         * passed to the delta method. The
                         * Accumulator#updateBaseValue method needs the
                         * commitTimestamp.
                         */
                        listener.delta(address, commitTimestamp, exchange.getTree(), D0.getIndex(bb),
                                D0.getAccumulatorTypeOrdinal(bb), 1);
                        appliedUpdates.incrementAndGet();
                    }
                    break;
                }

                case D1.TYPE: {
                    final Exchange exchange = getExchange(D1.getTreeHandle(bb), address, startTimestamp, listener);
                    if (exchange != null) {
                        listener.delta(address, startTimestamp, exchange.getTree(), D1.getIndex(bb),
                                D1.getAccumulatorTypeOrdinal(bb), D1.getValue(bb));
                    }
                    appliedUpdates.incrementAndGet();
                    break;
                }

                default: {
                    throw new CorruptJournalException("Invalid record type " + type + " at journal address "
                            + addressToString(address + position - start) + " index of transaction record at "
                            + addressToString(address));
                }
                }
            } catch (final VolumeNotFoundException vnfe) {
                final Persistit db = _support.getPersistit();
                if (db.getJournalManager().isIgnoreMissingVolumes()) {
                    /*
                     * If ignoreMissingVolumes is enabled, then issue a warning
                     * Alert, but allow recovery or rollback to continue.
                     */
                    db.getAlertMonitor().post(
                            new Event(AlertLevel.WARN, db.getLogBase().missingVolume, vnfe.getMessage(), address
                                    + position - start), AlertMonitor.MISSING_VOLUME_CATEGORY);
                    ignoredUpdates.incrementAndGet();
                } else {
                    failedUpdates.incrementAndGet();
                    throw vnfe;
                }
            } catch (final PersistitException e) {
                failedUpdates.incrementAndGet();
                throw e;
            }
            position += innerSize;
        }
    }

    public static String addressToString(final long address) {
        return String.format("JournalAddress %,d", address);
    }

    public static String addressToString(final long address, final long timestamp) {
        return String.format("JournalAddress %,d{%,d}", address, timestamp);
    }

    /**
     * Returns an Exchange on which an operation can be applied or rolled back.
     * For a {@link TransactionPlayerListener} that performs roll backs, it is
     * important not to create a new tree when none exists. Therefore this
     * method may return <code>null</code> to indicate that no tree exists and
     * therefore the requested operation should be ignored. Whether to create a
     * new tree is determined by the
     * {@link TransactionPlayerListener#createTree(long)} method.
     * 
     * @param treeHandle
     * @param from
     * @param timestamp
     * @param listener
     * @return the <code>Exchange</code> on which a recovery operation should be
     *         applied, or <code>null</code> if there is no backing
     *         <code>Tree</code>.
     * @throws PersistitException
     */
    private Exchange getExchange(final int treeHandle, final long from, final long timestamp,
            final TransactionPlayerListener listener) throws PersistitException {
        final TreeDescriptor td = _support.getPersistit().getJournalManager().lookupTreeHandle(treeHandle);
        if (td == null) {
            throw new CorruptJournalException("Tree handle " + treeHandle + " is undefined at "
                    + addressToString(from, timestamp));
        }
        final Volume volume = _support.getPersistit().getJournalManager().volumeForHandle(td.getVolumeHandle());
        if (volume == null) {
            throw new CorruptJournalException("Volume handle " + td.getVolumeHandle() + " is undefined at "
                    + addressToString(from, timestamp));
        }
        if (VolumeStructure.DIRECTORY_TREE_NAME.equals(td.getTreeName())) {
            return volume.getStructure().directoryExchange();
        } else {
            final Tree tree = volume.getStructure().getTreeInternal(td.getTreeName());
            if (!listener.createTree(timestamp) && (tree == null || !tree.hasVersion(ts2vh(timestamp)))) {
                return null;
            }
            final Exchange exchange = _support.getPersistit().getExchange(volume, td.getTreeName(), true);
            exchange.ignoreTransactions();
            return exchange;
        }
    }

    private void releaseExchange(final Exchange exchange, final Tree directoryTree) {
        if (!exchange.getTree().equals(directoryTree)) {
            _support.getPersistit().releaseExchange(exchange);
        }
    }

    long getAppliedUpdates() {
        return appliedUpdates.get();
    }

    long getIgnoredUpdates() {
        return ignoredUpdates.get();
    }

    long getFailedUpdates() {
        return failedUpdates.get();
    }

}
