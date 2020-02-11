/**
 * Copyright 2005-2012 Akiban Technologies, Inc.
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

import static com.persistit.VolumeHeader.getDirectoryRoot;
import static com.persistit.VolumeHeader.getExtendedPageCount;
import static com.persistit.VolumeHeader.getGarbageRoot;
import static com.persistit.VolumeHeader.getId;
import static com.persistit.VolumeHeader.getNextAvailablePage;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.persistit.CleanupManager.CleanupAction;
import com.persistit.CleanupManager.CleanupAntiValue;
import com.persistit.Exchange.Sequence;
import com.persistit.JournalRecord.IV;
import com.persistit.JournalRecord.PA;
import com.persistit.MVV.PrunedVersion;
import com.persistit.Management.RecordInfo;
import com.persistit.exception.InUseException;
import com.persistit.exception.InvalidPageAddressException;
import com.persistit.exception.InvalidPageStructureException;
import com.persistit.exception.InvalidPageTypeException;
import com.persistit.exception.PersistitException;
import com.persistit.exception.PersistitIOException;
import com.persistit.exception.PersistitInterruptedException;
import com.persistit.exception.RebalanceException;
import com.persistit.exception.VolumeClosedException;
import com.persistit.policy.JoinPolicy;
import com.persistit.policy.SplitPolicy;
import com.persistit.util.Debug;
import com.persistit.util.Util;

/**
 * <p>
 * Memory structure that holds and manipulates the state of a fixed-length page
 * of a {@link Volume}. Persistit manipulates the content of a page by copying
 * it into a <code>Buffer</code>, reading and/or modifying modifying the
 * <code>Buffer</code>, and then writing the <code>Buffer</code>'s content back
 * into the page. There are several types of pages within the BTree structure -
 * e.g., index pages and data pages. A <code>Buffer</code> can hold and
 * manipulate any kind of page.
 * </p>
 * <p>
 * Multiple <code>Buffer</code>s are managed by a {@link BufferPool} that
 * provides methods to locate, load and store the contents of the
 * <code>Buffer</code>.
 * </p>
 * <p>
 * <code>Buffer</code> provides the low-level methods that organize keys and
 * values into byte arrays to be stored in database files.
 * </p>
 *
 * @version 1.0
 */

public class Buffer extends SharedResource {

    /**
     * Architectural lower bound on buffer size
     */
    public final static int MIN_BUFFER_SIZE = 1024;
    /**
     * Architectural upper bound on buffer size
     */
    public final static int MAX_BUFFER_SIZE = 16384;
    /**
     * Page type code for an unallocated page.
     */
    public final static int PAGE_TYPE_UNALLOCATED = 0;
    /**
     * Page type code for a data page. Data pages are the leaf pages of the
     * BTree.
     */
    public final static int PAGE_TYPE_DATA = 1;
    /**
     * Minimum page type code for an index page. Index pages are the non-leaf
     * pages of the BTree.
     */
    public final static int PAGE_TYPE_INDEX_MIN = 2;

    /**
     * Maximum page type code for an index page. Index pages are the non-leaf
     * pages of the BTree.
     */
    public final static int PAGE_TYPE_INDEX_MAX = 21;
    /**
     * Page type code for a page that has been converted to hold a list of
     * garbage chains. Pages deallocated as a result of (@link Exchange#remove)
     * operations are added organized into Garbage pages.
     */
    public final static int PAGE_TYPE_GARBAGE = 30;
    /**
     * Page type code for pages used to store continuation bytes for long
     * records
     */
    public final static int PAGE_TYPE_LONG_RECORD = 31;
    /**
     * Largest value page type code.
     */
    public final static int PAGE_TYPE_MAX = 31;

    public final static int PAGE_TYPE_HEAD = 32;

    public final static String[] TYPE_NAMES = { "Unused", // 0
            "Data", // 1
            "Index1", // 2
            "Index2", // 3
            "Index3", // 4
            "Index4", // 5
            "Index5", // 6
            "Index6", // 7
            "Index7", // 8
            "Index8", // 9
            "Index9", // 10
            "Index10", // 11
            "Index11", // 12
            "Index12", // 13
            "Index13", // 14
            "Index14", // 15
            "Index15", // 16
            "Index16", // 17
            "Index17", // 18
            "Index18", // 19
            "Index19", // 20
            "Index20", // 21
            "Invalid", // 22
            "Invalid", // 23
            "Invalid", // 24
            "Invalid", // 25
            "Invalid", // 26
            "Invalid", // 27
            "Invalid", // 28
            "Invalid", // 29
            "Garbage", // 30
            "LongRec", // 31
            "Head", // 32
    };

    /**
     * Size of standard header portion of each page type
     */
    public final static int HEADER_SIZE = 32;

    final static long MAX_VALID_PAGE_ADDR = Integer.MAX_VALUE - 1;
    /**
     * A <code>Buffer</code> contains header information, a series of contiguous
     * key blocks, each of which contains an Elided Byte Count (EBC), one byte
     * of the key sequence, called the discriminator byte (DB), and an offset to
     * another section of the buffer that contains the bytes of the key value
     * and the payload (TAIL). These three values are coded into an int (4
     * bytes) so that every key block is int-aligned. The TAIL value is the
     * offset within this buffer of the tail block. It is always a multiple of
     * 4, making the tail block int-aligned too.
     *
     * Offsets to fields in header:
     */
    final static int TYPE_OFFSET = 0;
    final static int BUFFER_LENGTH_OFFSET = 1;
    final static int KEY_BLOCK_END_OFFSET = 2;
    final static int FREE_OFFSET = 4;
    final static int SLACK_OFFSET = 6;
    final static int PAGE_ADDRESS_OFFSET = 8;
    final static int RIGHT_SIBLING_OFFSET = 16;
    final static int TIMESTAMP_OFFSET = 24;

    // Offset within page of first KeyBlock
    final static int KEY_BLOCK_START = HEADER_SIZE;

    // (Constant) length of the key block
    final static int KEYBLOCK_LENGTH = 4;

    // Mask to form pointer to a key block
    final static int KEYBLOCK_MASK = 0xFFFFFFFC;

    // Length of tail block
    final static int TAILBLOCK_HDR_SIZE_DATA = 4;
    final static int TAILBLOCK_HDR_SIZE_INDEX = 8;
    final static int TAILBLOCK_POINTER = 4;
    final static int TAILBLOCK_FACTOR = 4;
    final static int TAILBLOCK_MASK = 0xFFFFFFFC;

    final static int TAILBLOCK_SIZE_MASK = 0x0000FFFF;
    final static int TAILBLOCK_KLENGTH_MASK = 0x0FFF0000;
    final static int TAILBLOCK_INUSE_MASK = 0x10000000;
    // final static int TAILBLOCK_SIZE_SHIFT = 0;
    final static int TAILBLOCK_KLENGTH_SHIFT = 16;

    // Flag indicates exact match
    final static int EXACT_MASK = 1;

    final static int P_MASK = 0x0000FFFC;

    final static int DEPTH_MASK = 0x0FFF0000;
    final static int DEPTH_SHIFT = 16;
    final static int FIXUP_MASK = 0x40000000;

    // Mask for the discriminator byte field within a keyblock
    private final static int DB_MASK = 0x000000FF;

    // Mask for the key common count field within a keyblock
    private final static int EBC_MASK = 0x000FFF00;

    // Mask for the tail pointer within a keyblock
    private final static int TAIL_MASK = 0xFFF00000;

    // Shift for discriminator byte field
    // private final static int DB_SHIFT = 0;

    // Shift for key common count field
    private final static int EBC_SHIFT = 8;

    // Shift for tail pointer
    private final static int TAIL_SHIFT = 20 - 2;

    final static int GARBAGE_BLOCK_SIZE = 32;

    private final static int GARBAGE_BLOCK_STATUS = 4;
    private final static int GARBAGE_BLOCK_LEFT_PAGE = 8;
    private final static int GARBAGE_BLOCK_RIGHT_PAGE = 16;
    private final static int GARBAGE_BLOCK_EXPECTED_COUNT = 24;

    /**
     *
     */
    final static int LONGREC_TYPE = 255;
    final static int LONGREC_PREFIX_SIZE_OFFSET = 2;
    final static int LONGREC_SIZE_OFFSET = 4;
    final static int LONGREC_PAGE_OFFSET = 12;
    final static int LONGREC_PREFIX_SIZE = 100;
    final static int LONGREC_PREFIX_OFFSET = 20;
    final static int LONGREC_SIZE = LONGREC_PREFIX_OFFSET + LONGREC_PREFIX_SIZE;

    final static int ANTIVALUE_TYPE = Value.CLASS_ANTIVALUE;
    /**
     * Implicit overhead size
     */
    final static int INDEX_PAGE_OVERHEAD = KEY_BLOCK_START + 2 * KEYBLOCK_LENGTH + 2 * TAILBLOCK_HDR_SIZE_INDEX;

    final static int DATA_PAGE_OVERHEAD = KEY_BLOCK_START + 2 * KEYBLOCK_LENGTH + 2 * TAILBLOCK_HDR_SIZE_DATA;

    /**
     * Upper bound on long record chains.
     */
    final static int MAX_LONG_RECORD_CHAIN = 5000;

    private final static int ESTIMATED_FIXED_BUFFER_OVERHEAD = 200;

    public final static int MAX_KEY_RATIO = 16;

    private final static int BINARY_SEARCH_THRESHOLD = 6;

    private final static int PRUNE_MVV_HELPER_CHANGED = 1;
    private final static int PRUNE_MVV_HELPER_HAS_LONG = 2;

    abstract static class VerifyVisitor {

        protected void visitPage(final long timestamp, final Volume volume, final long page, final int type,
                final int bufferSize, final int keyBlockStart, final int keyBlockEnd, final int alloc,
                final int available, final long rightSibling) throws PersistitException {
        }

        protected void visitIndexRecord(final Key key, final int foundAt, final int tail, final int kLength,
                final long pointer) throws PersistitException {

        }

        protected void visitDataRecord(final Key key, final int foundAt, final int tail, final int klength,
                final int offset, final int length, final byte[] bytes) throws PersistitException {
        }
    }

    // For debugging - set true in debugger to create verbose toString() output.
    //
    boolean _toStringDebug = false;

    /**
     * The BufferPool in which this buffer is allocated.
     */
    final private BufferPool _pool;

    /**
     * Index within the buffer pool
     */
    final private int _poolIndex;

    /**
     * The page address of the page currently loaded in this buffer.
     */
    private volatile long _page;

    /**
     * The Volume from which the page was loaded.
     */
    private volatile Volume _vol;

    /**
     * Timestamp of last Transaction to modify this resource
     */
    private volatile long _timestamp;

    /**
     * A ByteBuffer facade for this Buffer used in NIO operations
     */
    private final ByteBuffer _byteBuffer;

    /**
     * The size of this buffer
     */
    private final int _bufferSize;

    /**
     * The bytes in this buffer. Note these bytes are also the backing store of
     * _byteBuffer.
     */
    private final byte[] _bytes;

    /**
     * FastIndex structure used for rapid page searching
     */
    private final FastIndex _fastIndex;

    /**
     * The right sibling page address
     */
    private volatile long _rightSibling;

    /**
     * Type code for this page.
     */
    private volatile int _type;

    /**
     * Tailblock header size
     */
    private volatile int _tailHeaderSize = TAILBLOCK_HDR_SIZE_DATA;

    /**
     * Offset within the buffer to the first byte past the last keyblock
     */
    private volatile int _keyBlockEnd;

    /**
     * Offset within the buffer to the lowest tailblock. To allocate a new
     * tailblock, consume the space below this point.
     */
    private volatile int _alloc;

    /**
     * Count of unused bytes above _alloc.
     */
    private volatile int _slack;

    /**
     * Count of MVV values
     */
    private volatile int _mvvCount;

    /**
     * Singly-linked list of Buffers current having the same hash code.
     * (Maintained by BufferPool.)
     */
    private Buffer _next = null;

    private volatile long _lastPrunedTime;

    private volatile boolean _enqueuedForAntiValuePruning;

    /**
     * Construct a new buffer.
     *
     * @param size
     *            The buffer size, in bytes.
     */
    Buffer(int size, final int index, final BufferPool pool, final Persistit persistit) {
        super(persistit);
        boolean ok = false;
        for (int s = MIN_BUFFER_SIZE; !ok && s <= MAX_BUFFER_SIZE; s *= 2) {
            if (s == size) {
                ok = true;
            }
        }
        if (!ok) {
            throw new IllegalArgumentException("Invalid buffer size: " + size);
        }
        size &= KEYBLOCK_MASK;
        _pool = pool;
        _poolIndex = index;
        _byteBuffer = ByteBuffer.allocate(size);
        _bytes = _byteBuffer.array();
        _bufferSize = size;
        _fastIndex = new FastIndex(this, 1 + (size - HEADER_SIZE) / MAX_KEY_RATIO);
    }

    Buffer(final Buffer original) {
        this(original._bufferSize, original._poolIndex, original._pool, original._persistit);
        setStatus(original);
        _type = original._type;
        _timestamp = original._timestamp;
        _page = original._page;
        _vol = original._vol;
        _rightSibling = original._rightSibling;
        _alloc = original._alloc;
        _slack = original._slack;
        _mvvCount = original._mvvCount;
        setKeyBlockEnd(original._keyBlockEnd);
        _tailHeaderSize = original._tailHeaderSize;
        System.arraycopy(original._bytes, 0, _bytes, 0, _bytes.length);
    }

    /**
     * Initializes the buffer so that it contains no keys or data.
     */
    void init(final int type) {
        assert isOwnedAsWriterByMe();
        _type = type;
        setKeyBlockEnd(KEY_BLOCK_START);
        _tailHeaderSize = isIndexPage() ? TAILBLOCK_HDR_SIZE_INDEX : TAILBLOCK_HDR_SIZE_DATA;
        _rightSibling = 0;
        _alloc = _bufferSize;
        _slack = 0;
        _mvvCount = 0;
        clearEnqueuedForPruning();
        bumpGeneration();
    }

    void clearEnqueuedForPruning() {
        _enqueuedForAntiValuePruning = false;
        _lastPrunedTime = 0;
    }

    /**
     *
     * Extract fields from the buffer.
     *
     * @throws PersistitIOException
     * @throws InvalidPageAddressException
     * @throws InvalidPageStructureException
     * @throws VolumeClosedException
     * @throws InUseException
     * @throws PersistitInterruptedException
     */
    void load(final Volume vol, final long page) throws PersistitIOException, InvalidPageAddressException,
            InvalidPageStructureException, VolumeClosedException, InUseException, PersistitInterruptedException {
        _vol = vol;
        _page = page;
        vol.getStorage().readPage(this);
        load();
    }

    void load() throws InvalidPageStructureException {
        Debug.$assert0.t(isOwnedAsWriterByMe());

        _timestamp = getLong(TIMESTAMP_OFFSET);

        if (_page != 0) {
            final int type = getByte(TYPE_OFFSET);
            if (type > PAGE_TYPE_MAX) {
                throw new InvalidPageStructureException("Invalid type " + type);
            }
            _type = type;
            setKeyBlockEnd(getChar(KEY_BLOCK_END_OFFSET));

            if (type == PAGE_TYPE_UNALLOCATED) {
                _rightSibling = 0;
                _alloc = _bufferSize;
                _slack = 0;
                _rightSibling = 0;
            } else {
                Debug.$assert0.t(getByte(BUFFER_LENGTH_OFFSET) * 256 == _bufferSize);
                Debug.$assert0.t(getLong(PAGE_ADDRESS_OFFSET) == _page);
                _alloc = getChar(FREE_OFFSET);
                _slack = getChar(SLACK_OFFSET);
                _rightSibling = getLong(RIGHT_SIBLING_OFFSET);

                if (isDataPage()) {
                    _tailHeaderSize = TAILBLOCK_HDR_SIZE_DATA;
                    _mvvCount = Integer.MAX_VALUE;
                    clearEnqueuedForPruning();
                } else if (isIndexPage()) {
                    _tailHeaderSize = TAILBLOCK_HDR_SIZE_INDEX;
                }
            }
        } else {
            _type = PAGE_TYPE_HEAD;
        }
        invalidateFastIndex();
        bumpGeneration();
    }

    void writePageOnCheckpoint(final long timestamp) throws PersistitException {
        Debug.$assert0.t(isOwnedAsWriterByMe());
        final long checkpointTimestamp = _persistit.getTimestampAllocator().getProposedCheckpointTimestamp();
        if (isDirty() && !isTemporary() && getTimestamp() < checkpointTimestamp && timestamp > checkpointTimestamp) {
            writePage();
            _pool.bumpForcedCheckpointWrites();
        }
    }

    void writePage() throws PersistitException {
        assert isOwnedAsWriterByMe();
        _persistit.checkFatal();
        final Volume volume = getVolume();
        if (volume != null) {
            clearSlack();
            save();
            _vol.getStorage().writePage(this);
            clearDirty();
            volume.getStatistics().bumpWriteCounter();
            _pool.bumpWriteCounter();
        }
    }

    @Override
    boolean clearDirty() {
        if (super.clearDirty()) {
            _pool.decrementDirtyPageCount();
            return true;
        }
        return false;
    }

    @Override
    boolean setDirty() {
        throw new UnsupportedOperationException();
    }

    void setDirtyAtTimestamp(final long timestamp) {
        if (!isOwnedAsWriterByMe()) {
            throw new IllegalStateException("Exclusive claim required " + this);
        }
        if (super.setDirty()) {
            _pool.incrementDirtyPageCount();
        }
        _timestamp = timestamp;
        bumpGeneration();
    }

    @Override
    boolean claim(final boolean writer) throws PersistitInterruptedException {
        return claim(writer, DEFAULT_MAX_WAIT_TIME);
    }

    @Override
    boolean claim(final boolean writer, final long timeout) throws PersistitInterruptedException {
        if (super.claim(writer, timeout)) {
            if (!isDirty()) {
                _timestamp = _persistit.getCurrentTimestamp();
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    void release() {
        if (Debug.ENABLED && isDirty() && (isDataPage() || isIndexPage())) {
            assertVerify();
        }
        super.release();
    }

    void releaseTouched() {
        setTouched();
        release();
    }

    /**
     * Zero out all bytes in this buffer.
     */
    void clear() {
        Util.clearBytes(_bytes, 0, _bufferSize);
    }

    void clearBytes(final int from, final int to) {
        Util.clearBytes(_bytes, from, to);
    }

    /**
     * Clears all tailblock bytes that are no longer in use. This makes sure we
     * don't retain data that has been deleted or moved in this page.
     *
     * @throws InvalidPageStructureException
     */
    void clearSlack() throws InvalidPageStructureException {
        if (isGarbagePage()) {
            Util.clearBytes(_bytes, _keyBlockEnd, _alloc);
        } else if (isDataPage() || isIndexPage()) {
            repack();
            clearBytes(_keyBlockEnd, _alloc);
        }
    }

    /**
     * Post fields back into the buffer in preparation for writing it to disk.
     */
    void save() {
        putLong(TIMESTAMP_OFFSET, _timestamp);
        if (_page != 0) {
            putByte(TYPE_OFFSET, _type);
            putByte(BUFFER_LENGTH_OFFSET, _bufferSize / 256);
            putChar(KEY_BLOCK_END_OFFSET, _keyBlockEnd);
            putChar(FREE_OFFSET, _alloc);
            putChar(SLACK_OFFSET, _slack);
            putLong(PAGE_ADDRESS_OFFSET, _page);
            putLong(RIGHT_SIBLING_OFFSET, _rightSibling);
        }
    }

    /**
     * @return The {@link Volume} to which the page currently occupying this
     *         <code>Buffer</code> belongs
     */
    public Volume getVolume() {
        return _vol;
    }

    /**
     * @return The ByteBuffer facade for this Buffer.
     */
    public ByteBuffer getByteBuffer() {
        return _byteBuffer;
    }

    /**
     * @return The page address of the current page.
     */
    public long getPageAddress() {
        return _page;
    }

    /**
     * @return The size of the buffer, in bytes
     */
    public int getBufferSize() {
        return _bufferSize;
    }

    /**
     * @return Timestamp at which page held by this buffer was last changed
     */
    public long getTimestamp() {
        return _timestamp;
    }

    /**
     * @return Number of remaining bytes available for allocation
     */
    public int getAvailableSize() {
        if (_type == Buffer.PAGE_TYPE_DATA || _type >= Buffer.PAGE_TYPE_INDEX_MIN
                && _type <= Buffer.PAGE_TYPE_INDEX_MAX) {
            return _alloc - _keyBlockEnd + _slack;
        } else {
            return 0;
        }
    }

    /**
     * @return Number of key-value pairs currently stored in this page.
     */
    public int getKeyCount() {
        return (_keyBlockEnd - KEY_BLOCK_START) / KEYBLOCK_LENGTH;
    }

    /**
     * @return Offset to next available allocation block within the page.
     */
    public int getAlloc() {
        return _alloc;
    }

    /**
     * Gets the page type
     *
     * @return the type as an integer
     */
    public int getPageType() {
        return _type;
    }

    /**
     * @return A displayable String version of the page type.
     */
    public String getPageTypeName() {
        return getPageTypeName(_page, _type);
    }

    public static String getPageTypeName(final long page, final int type) {
        if (page == 0) {
            return TYPE_NAMES[PAGE_TYPE_HEAD];
        }
        if (type == Buffer.PAGE_TYPE_UNALLOCATED || type == Buffer.PAGE_TYPE_DATA || type >= Buffer.PAGE_TYPE_INDEX_MIN
                && type <= Buffer.PAGE_TYPE_INDEX_MAX || type == Buffer.PAGE_TYPE_GARBAGE
                || type == Buffer.PAGE_TYPE_LONG_RECORD) {
            return TYPE_NAMES[type];
        } else
            return "Invalid" + type;
    }

    /**
     * Gets the index of this Buffer within the buffer pool
     *
     * @return The index
     */
    public int getIndex() {
        return _poolIndex;
    }

    /**
     * Get the page address of the right sibling page
     *
     * @return the sibling's address
     */
    public long getRightSibling() {
        return _rightSibling;
    }

    public void setPageAddressAndVolume(final long pageAddress, final Volume volume) {
        _page = pageAddress;
        _vol = volume;
    }

    /**
     * Set the right sibling's page address
     *
     * @param pageAddress
     *            the sibling's address
     */
    void setRightSibling(final long pageAddress) {
        Debug.$assert0.t(isOwnedAsWriterByMe());
        _rightSibling = pageAddress;
    }

    long getVolumeId() {
        final Volume volume = _vol;
        return volume == null ? 0 : volume.getId();
    }

    int getKeyBlockStart() {
        return KEY_BLOCK_START;
    }

    int getKeyBlockEnd() {
        return _keyBlockEnd;
    }

    int getMvvCount() {
        return _mvvCount;
    }

    void setKeyBlockEnd(final int index) {
        Debug.$assert0.t(index >= KEY_BLOCK_START && index <= (_pool.getMaxKeys() * KEYBLOCK_LENGTH) + KEY_BLOCK_START
                || (!isDataPage() && !isIndexPage() || !isValid()));
        _keyBlockEnd = index;
    }

    void setAlloc(final int alloc) {
        Debug.$assert0.t(alloc >= 0 && alloc <= _bufferSize);
        _alloc = alloc;
    }

    void setNext(final Buffer buffer) {
        Debug.$assert0.t(buffer != this);
        _next = buffer;
    }

    Buffer getNext() {
        return _next;
    }

    /**
     * Finds the keyblock in this page that exactly matches or immediately
     * follows the supplied key.
     * <p>
     * The result is encoded as
     * <p>
     * <br>
     * (fixupRequired ? FIXUP_MASK : 0) | <br>
     * (exact ? EXACT_MASK : 0) | <br>
     * (depth &lt;&lt; DEPTH_SHIFT) | <br>
     * offset
     * <p>
     * where: <br>
     * fixupRequired is true if the successor keyblock needs to be adjusted when
     * inserting the supplied key <br>
     * exact is true if the key matches exactly <br>
     * depth is the count of bytes in the matching left subkey of the
     * predecessor key block <br>
     * offset is the offset of the keyblock at or after the supplied key.
     * <p>
     *
     * @param key
     *            The key to seek
     * @return An encoded result (see above). Returns 0 if the supplied key
     *         precedes the first key in the page. Returns Integer.MAX_VALUE if
     *         it follows the last key in the page.
     * @throws PersistitInterruptedException
     */
    int findKey(final Key key) throws PersistitInterruptedException {
        final FastIndex fastIndex = getFastIndex();
        final byte[] kbytes = key.getEncodedBytes();
        final int klength = key.getEncodedSize();
        int depth = 0;
        int left = KEY_BLOCK_START;
        int right = _keyBlockEnd;
        final int start = left;
        final int tailHeaderSize = _tailHeaderSize;

        for (int p = start; p < right;) {
            //
            // Here we MUST land on a KeyBlock at an ebc value change point.
            //
            int kbData = getInt(p);
            int index = (p - start) >> 2;
            int runCount = fastIndex.getRunCount(index);
            final int ebc = decodeKeyBlockEbc(kbData);

            if (depth < ebc) {
                // We know that depth < ebc for a bunch of KeyBlocks - we
                // can skip them. We can skip all KeyBlocks with ebc values
                // equal to or greater than this one, so whether there's a
                // runCount (positive) or crossCount (negative), we skip
                // this KeyBlock and all its successors.
                if (runCount < 0) {
                    runCount = -runCount;
                }
                p += KEYBLOCK_LENGTH * (runCount + 1);
                continue;
            }

            else if (depth > ebc) {
                final int result = p | (depth << DEPTH_SHIFT);
                return result;
            }

            else // if (depth == ebc)
            {
                // Now we are looking for the keyblock with the matching db
                int db = decodeKeyBlockDb(kbData);
                int kb = kbytes[depth] & 0xFF;

                if (kb < db) {
                    final int result = p | (depth << DEPTH_SHIFT);
                    return result;
                }
                if (kb > db) {
                    if (runCount > 0) {
                        //
                        // There is a run of runCount keys after p that all have
                        // the same ebc. Depending on how big the run is, we
                        // either do a linear search or perform a binary search
                        // within the run.
                        //
                        final int p2 = p + (runCount * KEYBLOCK_LENGTH);
                        //
                        // p2 now points to the last key block with the same
                        // ebc in this run.
                        //
                        final int kbData2 = getInt(p2);
                        final int db2 = decodeKeyBlockDb(kbData2);
                        //
                        // For the common case that runCount == 1, we avoid
                        // setting up the binary search loop. Instead, the
                        // result is completely determined here by the
                        // value of db2.
                        //
                        if (runCount == 1) {
                            if (db2 > kb) {
                                //
                                // This is right because we already know
                                // that kb > db.
                                //
                                final int result = p2 | (depth << DEPTH_SHIFT);
                                return result;
                            } else if (db2 < kb) {
                                //
                                // The key at the end of the run is still less
                                // than kb so we just skip the entire run and
                                // increment p. One possibility is that the
                                // run is interrupted by a series of deeper keys
                                // -
                                // in that case we use the cross count to skip
                                // all of them.
                                final int runCount2 = fastIndex.getRunCount(index + runCount);
                                assert runCount2 <= 0;
                                p = p2 + KEYBLOCK_LENGTH * (-runCount + 1);
                                continue;
                            } else {
                                //
                                // Found it right here. We'll fall through to
                                // the equality check below.
                                //
                                p = p2;
                                db = db2;
                            }
                        }
                        //
                        // If runCount > 1 then we have more work to do.
                        //
                        else if (db2 > kb) {
                            //
                            // The key block we want is between [p, p2).
                            // Time to do a binary search.
                            //
                            left = p;
                            right = p2;
                            //
                            // Adjust left and right bounds of the range
                            // using db - kb difference; every db must differ
                            // from its predecessor because the ebcs are all
                            // the same. Therefore the we can define upper
                            // bounds on the maximum distance from the left
                            // and right ends of the range to the keyblock
                            // we are seeking.
                            //
                            if (runCount > BINARY_SEARCH_THRESHOLD) {
                                final int distance = (right - left) >> 2;
                                final int oldRight = right;
                                if (distance > kb - db + 1) {
                                    right = left + ((kb - db + 1) << 2);
                                }
                                if (distance > db2 - kb + 1) {
                                    left = oldRight - ((db2 - kb + 1) << 2);
                                }
                            }
                            //
                            // Perform binary search
                            //
                            while (right > left) {
                                p = ((left + right) >> 1) & P_MASK;
                                if (p == left) {
                                    //
                                    // This is true because if p == left then
                                    // right must be left + 1. We already know
                                    // that kb > db and less than db2, so the
                                    // final answer is know to be in right.
                                    //
                                    final int result = right | (depth << DEPTH_SHIFT);
                                    return result;
                                }
                                //
                                // Otherwise perform a probe and the current
                                // mid-point and
                                // adjust the ends depending on the comparison.
                                //
                                final int db1 = getDb(p);

                                if (db1 == kb) {
                                    db = db1;
                                    break;
                                } else if (db1 > kb) {
                                    right = p;
                                } else {
                                    left = p;
                                    db = db1;
                                }
                            }
                        } else if (db2 < kb) {
                            //
                            // We just want to move forward from kb2, skipping
                            // the crossCount if non-zero.
                            //
                            index = (p2 - start) >> 2;
                            runCount = fastIndex.getRunCount(index);
                            assert runCount <= 0;
                            p = p2 + KEYBLOCK_LENGTH * (-runCount + 1);
                            continue;
                        } else {
                            // found it right here. We'll fall through to the
                            // equality check below.
                            p = p2;
                            db = db2;
                        }
                    } else {
                        //
                        // Skipping all the keys in the crossCount. These keys
                        // are all deeper and therefore all fall before the db
                        // are looking for. This case also handles
                        // runCount == 0 where we simply advance to the next key
                        // block.
                        //
                        p += KEYBLOCK_LENGTH * (-runCount + 1);
                        continue;
                    }
                }

                assert db == kb;
                //
                // kb == db so we now try to go deeper into the key. On
                // an exact match we will perform this block of code once
                // for each byte in the key.
                //
                kbData = getInt(p);
                final int tail = decodeKeyBlockTail(kbData);
                final int tbData = getInt(tail);
                final int tlength = decodeTailBlockKLength(tbData) + depth + 1;
                final int qlength = tlength < klength ? tlength : klength;
                //
                // Walk down the key, increasing depth
                //
                boolean matched = true;
                if (++depth < qlength) {
                    int q = tail + tailHeaderSize;
                    kb = kbytes[depth];
                    db = _bytes[q++];

                    while (kb == db && ++depth < qlength) {
                        kb = kbytes[depth];
                        db = _bytes[q++];
                    }

                    if (kb != db) {
                        kb = kb & 0xFF;
                        db = db & 0xFF;

                        if (kb < db) {
                            //
                            // Key is less than tail, so we return
                            // this keyblock
                            //
                            final int result = p | (depth << DEPTH_SHIFT) | FIXUP_MASK;
                            return result;
                        }
                        matched = false;
                    }
                }

                if (matched && depth == qlength) {
                    // We matched all the way to the end of either key or
                    // tail.
                    //
                    if (qlength == tlength) {
                        //
                        // We matched all the way to the end of the tail
                        //
                        if (qlength == klength) {
                            //
                            // And the key lengths are equal so this is an
                            // exact match.
                            //
                            final int result = p | (depth << DEPTH_SHIFT) | EXACT_MASK;
                            return result;
                        }
                    } else if (tlength > qlength) {
                        //
                        // Tail is longer, so the key
                        // key is less than tail, so we return the
                        // this keyblock since it is greater than the key
                        //
                        final int result = p | (depth << DEPTH_SHIFT) | FIXUP_MASK;
                        return result;
                    }
                    // Otherwise, the key is longer, so we move to the next
                    // key block at the bottom of this loop.
                }
                // Advance to the next keyblock
                p += KEYBLOCK_LENGTH;
            }

        }

        final int result = right | (depth << DEPTH_SHIFT);
        return result;
    }

    boolean isPrimordialAntiValue(final int foundAt) {
        if (isDataPage()) {
            final int p = foundAt & P_MASK;
            if (p >= KEY_BLOCK_START && p < _keyBlockEnd) {
                final int kbData = getInt(p);
                final int tail = decodeKeyBlockTail(kbData);
                final int tbData = getInt(tail);
                final int klength = decodeTailBlockKLength(tbData);
                final int size = decodeTailBlockSize(tbData);
                final int offset = tail + _tailHeaderSize + klength;
                final int valueSize = size - klength - _tailHeaderSize;
                return valueSize == 1 && _bytes[offset] == MVV.TYPE_ANTIVALUE;
            }
        }
        return false;

    }

    /**
     * Given a foundAt position, return a long value that encodes the offset and
     * size of the associated value field
     *
     * @param foundAt
     * @return (offset << 32) | size;
     */
    long at(final int foundAt) {
        if (isDataPage() || isIndexPage()) {
            final int p = foundAt & P_MASK;
            if (p >= KEY_BLOCK_START && p < _keyBlockEnd) {
                final int kbData = getInt(p);
                final int tail = decodeKeyBlockTail(kbData);
                final int tbData = getInt(tail);
                final int klength = decodeTailBlockKLength(tbData);
                final int size = decodeTailBlockSize(tbData);
                final int offset = tail + _tailHeaderSize + klength;
                final int valueSize = size - klength - _tailHeaderSize;
                return ((long) offset) << 32 | valueSize;
            }
        }
        return -1;
    }

    void keyAt(final int foundAt, final Key key) {
        Debug.$assert0.t(foundAt > 0 && foundAt < _keyBlockEnd);
        if (isDataPage() || isIndexPage()) {
            for (int p = KEY_BLOCK_START; p <= foundAt; p += KEYBLOCK_LENGTH) {
                final int kbData = getInt(p);
                final int tail = decodeKeyBlockTail(kbData);
                final int ebc = decodeKeyBlockEbc(kbData);
                final int db = decodeKeyBlockDb(kbData);
                final int tbData = getInt(tail);
                final int klength = decodeTailBlockKLength(tbData);
                final byte[] keyBytes = key.getEncodedBytes();
                keyBytes[ebc] = (byte) db;
                System.arraycopy(_bytes, tail + _tailHeaderSize, keyBytes, ebc + 1, klength);
                key.setEncodedSize(ebc + klength + 1);
            }
        }
    }

    Value fetch(final int foundAt, final Value value) {
        if ((foundAt & EXACT_MASK) == 0) {
            value.clear();
        } else {
            Debug.$assert0.t(foundAt > 0 && (foundAt & P_MASK) < _keyBlockEnd);
            final int kbData = getInt(foundAt & P_MASK);
            final int tail = decodeKeyBlockTail(kbData);
            final int tbData = getInt(tail);
            final int klength = decodeTailBlockKLength(tbData);
            final int size = decodeTailBlockSize(tbData);
            final int valueSize = size - klength - _tailHeaderSize;
            value.putEncodedBytes(_bytes, tail + _tailHeaderSize + klength, valueSize);
        }
        return value;
    }

    long fetchLongRecordPointer(final int foundAt) {
        if (!isDataPage()) {
            return 0;
        }
        final int kbData = getInt(foundAt & P_MASK);
        final int tail = decodeKeyBlockTail(kbData);
        final int tbData = getInt(tail);
        final int klength = decodeTailBlockKLength(tbData);
        final int size = decodeTailBlockSize(tbData);
        final int valueSize = size - klength - _tailHeaderSize;
        if (valueSize != LONGREC_SIZE) {
            return 0;
        }
        if ((_bytes[tail + _tailHeaderSize + klength] & 0xFF) != LONGREC_TYPE) {
            return 0;
        }

        final long pointer = getLong(tail + _tailHeaderSize + klength + LONGREC_PAGE_OFFSET);
        return pointer;
    }

    void neuterLongRecord(final int foundAt) {
        assert isDataPage() : "Invalid page type for long records: " + this;
        final int kbData = getInt(foundAt & P_MASK);
        final int tail = decodeKeyBlockTail(kbData);
        final int tbData = getInt(tail);
        final int klength = decodeTailBlockKLength(tbData);
        final int size = decodeTailBlockSize(tbData);
        final int valueSize = size - klength - _tailHeaderSize;
        if (valueSize != LONGREC_SIZE) {
            return;
        }
        if ((_bytes[tail + _tailHeaderSize + klength] & 0xFF) != LONGREC_TYPE) {
            return;
        }
        /*
         * Value will now be undefined rather than a long record.
         */
        putByte(tail + _tailHeaderSize + klength, ANTIVALUE_TYPE);
    }

    long getPointer(final int foundAt) throws PersistitException {
        if (!isIndexPage()) {
            throw new InvalidPageTypeException("type=" + _type);
        }
        final int kbData = getInt(foundAt & P_MASK);
        final int tail = decodeKeyBlockTail(kbData);
        return getInt(tail + 4);
    }

    /**
     * Internal implementation of getKey using a previously computed result from
     * the findKey() method.
     *
     * @param key
     * @param mode
     * @param foundAt
     * @return foundAt value
     */
    int traverse(final Key key, final Key.Direction mode, final int foundAt) {
        final boolean exactMatch = (foundAt & EXACT_MASK) > 0;
        if (mode == Key.EQ || exactMatch && (mode == Key.LTEQ || mode == Key.GTEQ)) {
            return foundAt;
        }
        if (mode == Key.LT || mode == Key.LTEQ) {
            return previousKey(key, foundAt);
        } else if (mode == Key.GT || mode == Key.GTEQ) {
            return nextKey(key, foundAt);
        } else
            throw new IllegalArgumentException("Invalid mode: " + mode);
    }

    /**
     * Helper method to compute the actual bytes in the previous key.
     *
     * @param key
     * @param foundAt
     * @return foundAt value
     */
    int previousKey(final Key key, final int foundAt) {
        int p = (foundAt & P_MASK) - KEYBLOCK_LENGTH;
        final int depth = (foundAt & DEPTH_MASK) >>> DEPTH_SHIFT;

        if (p < KEY_BLOCK_START)
            return foundAt;

        final byte[] kbytes = key.getEncodedBytes();

        // Compute the count of prefix bytes in the supplied key that
        // are known to match. The leftmost knownGood bytes do not need
        // to be recovered by traversing keys in the page.
        //
        final int kbData2 = getInt(p + KEYBLOCK_LENGTH);
        final int ebc2 = decodeKeyBlockEbc(kbData2);
        int kbData = getInt(p);
        int ebc = decodeKeyBlockEbc(kbData);
        int knownGood = ebc;
        if (ebc2 < ebc) {
            knownGood = ebc2;
        }
        if (depth < knownGood) {
            knownGood = depth;
        }

        int tail = decodeKeyBlockTail(kbData);
        //
        // This is the length of the key value we need to reconstruct.
        // We walk backward through the keys on this page until we find
        // a key with an ebc value that is less than or equal to knownGood.
        // For each key we copy the bytes from its tail to fill in the gap
        // between ebc and unknown. This reduces the value of unknown. Once
        // the unknown count becomes less than or equal to the knownGood count,
        // we are done.
        //
        // (+1 is for the discriminator byte)
        int unknown = decodeTailBlockKLength(getInt(tail)) + ebc + 1;
        key.setEncodedSize(unknown);

        final int result = p | (unknown << DEPTH_SHIFT) | EXACT_MASK;
        //
        // Reconstruct the previous key.
        //
        for (;;) {
            if (ebc < unknown) {
                // move bytes from this keyblock into the result key.
                kbytes[ebc] = (byte) decodeKeyBlockDb(kbData);
                final int more = unknown - ebc - 1;
                if (more > 0) {
                    System.arraycopy(_bytes, tail + _tailHeaderSize, kbytes, ebc + 1, more);
                }
                unknown = ebc;
            }
            if (unknown <= knownGood)
                break;

            p -= KEYBLOCK_LENGTH;
            Debug.$assert1.t(p >= KEY_BLOCK_START);

            kbData = getInt(p);
            ebc = decodeKeyBlockEbc(kbData);
            tail = decodeKeyBlockTail(kbData);
        }
        return result;
    }

    /**
     * Helper method to compute the actual bytes in the successor key.
     *
     * @param key
     * @param foundAt
     * @return foundAt value
     */
    int nextKey(final Key key, final int foundAt) {
        int p = foundAt & P_MASK;
        if ((foundAt & EXACT_MASK) != 0)
            p += KEYBLOCK_LENGTH;

        if (p >= _keyBlockEnd)
            return foundAt;

        final byte[] kbytes = key.getEncodedBytes();
        final int kbData = getInt(p);
        final int ebc = decodeKeyBlockEbc(kbData);
        final int tail = decodeKeyBlockTail(kbData);
        final int tbData = getInt(tail);
        final int klength = decodeTailBlockKLength(tbData);
        final int keyLength = klength + ebc + 1;
        key.setEncodedSize(keyLength);

        final int result = p | (keyLength << DEPTH_SHIFT) | EXACT_MASK;
        //
        // Note: the findKey method is guaranteed to return the offset of a
        // keyblock whose first ebc bytes match the supplied key.
        //
        kbytes[ebc] = (byte) decodeKeyBlockDb(kbData);
        System.arraycopy(_bytes, tail + _tailHeaderSize, kbytes, ebc + 1, klength);

        return result;
    }

    /**
     * Helper method for IntegrityCheck to find next long record key block
     *
     * @param value
     * @param foundAt
     * @return foundAt value
     */
    int nextLongRecord(final Value value, final int foundAt) {
        Debug.$assert0.t(isDataPage());
        for (int p = foundAt & P_MASK; p < _keyBlockEnd; p += KEYBLOCK_LENGTH) {
            final int kbData = getInt(p);
            final int tail = decodeKeyBlockTail(kbData);
            final int tbData = getInt(tail);
            final int klength = decodeTailBlockKLength(tbData);
            final int size = decodeTailBlockSize(tbData);
            final int valueSize = size - klength - _tailHeaderSize;
            if ((valueSize > 0) && ((_bytes[tail + _tailHeaderSize + klength] & 0xFF) == LONGREC_TYPE)) {
                value.putEncodedBytes(_bytes, tail + _tailHeaderSize + klength, valueSize);
                return p;
            }
        }
        return -1;
    }

    int previousKeyBlock(final int foundAt) {
        final int p = (foundAt & P_MASK) - KEYBLOCK_LENGTH;
        if (p < KEY_BLOCK_START || p > _keyBlockEnd)
            return -1;
        return p;
    }

    int nextKeyBlock(final int foundAt) {
        final int p = (foundAt & P_MASK) + KEYBLOCK_LENGTH;
        if (p >= _keyBlockEnd || p < KEY_BLOCK_START)
            return -1;
        return p;
    }

    int toKeyBlock(int foundAt) {
        foundAt &= P_MASK;
        if (KEY_BLOCK_START >= _keyBlockEnd)
            return -1;
        if (foundAt < KEY_BLOCK_START)
            return KEY_BLOCK_START;
        if (foundAt >= _keyBlockEnd)
            return _keyBlockEnd - KEYBLOCK_LENGTH;
        return foundAt;
    }

    /**
     * Insert or replace a value for a key. If there already is a value with the
     * same key, then replace it. Otherwise insert a new key and value into the
     * buffer.
     *
     * @param key
     *            The key on under which the value will be stored
     * @param value
     *            The value, converted to a byte array
     * @throws PersistitInterruptedException
     */
    int putValue(final Key key, final ValueHelper valueHelper) throws PersistitInterruptedException {
        final int p = findKey(key);
        return putValue(key, valueHelper, p, false);
    }

    /**
     * Insert or replace a value for a key. If there already is a value with the
     * same key, then replace it. Otherwise insert a new key and value into the
     * buffer.
     *
     * @param key
     *            The key under which the value will be stored
     * @param value
     *            The value to be stored
     * @param foundAt
     *            The keyblock before which this record will be inserted
     */
    int putValue(final Key key, final ValueHelper valueHelper, final int foundAt, final boolean postSplit) {
        if (Debug.ENABLED) {
            assertVerify();
        }
        final boolean exactMatch = (foundAt & EXACT_MASK) > 0;
        final int p = foundAt & P_MASK;

        if (exactMatch) {
            return replaceValue(key, valueHelper, p);
        } else {
            int length;
            if (isIndexPage()) {
                length = 0;
            } else {
                length = valueHelper.requiredLength(_bytes, 0, -1);
            }

            final int depth = (foundAt & DEPTH_MASK) >>> DEPTH_SHIFT;
            final boolean fixupSuccessor = (foundAt & FIXUP_MASK) > 0;
            final byte[] kbytes = key.getEncodedBytes();

            int ebcNew;
            int ebcSuccessor;
            int kbSuccessor = 0;
            int delta = 0;
            int successorTail = 0;
            int successorTailBlock = 0;
            int successorTailSize = 0;
            int free1 = 0;
            int free2 = 0;
            if (fixupSuccessor) {
                kbSuccessor = getInt(p);
                ebcNew = decodeKeyBlockEbc(kbSuccessor);
                ebcSuccessor = depth;
                delta = ebcSuccessor - ebcNew;
                successorTail = decodeKeyBlockTail(kbSuccessor);
                successorTailBlock = getInt(successorTail);
                successorTailSize = decodeTailBlockSize(successorTailBlock);
                free1 = (successorTailSize + ~TAILBLOCK_MASK) & TAILBLOCK_MASK;
                free2 = (successorTailSize - delta + ~TAILBLOCK_MASK) & TAILBLOCK_MASK;

            } else {
                ebcSuccessor = 0;
                ebcNew = depth;
            }
            final int klength = key.getEncodedSize() - ebcNew - 1;
            final int newTailSize = klength + length + _tailHeaderSize;

            if (getKeyCount() >= _pool.getMaxKeys() || !willFit(newTailSize + KEYBLOCK_LENGTH - (free1 - free2))) {
                Debug.$assert0.t(!postSplit);
                return -1;
            }

            //
            // Possibly increase the ebc value of the successor
            // (Needs to be done first so that computation of new run value
            // is correct.
            //
            if (fixupSuccessor && ebcNew != ebcSuccessor) {
                final int successorKeyLength = decodeTailBlockKLength(successorTailBlock);
                final int successorDb = getByte(successorTail + _tailHeaderSize + delta - 1);

                // Write updated successor tail block
                putInt(successorTail, encodeTailBlock(successorTailSize - delta, successorKeyLength - delta));

                System.arraycopy(_bytes, successorTail + _tailHeaderSize + delta, _bytes, successorTail
                        + _tailHeaderSize, successorTailSize - _tailHeaderSize - delta);

                if (free2 < free1) {
                    deallocTail(successorTail + free2, free1 - free2);
                }

                kbSuccessor = encodeKeyBlock(ebcSuccessor, successorDb, successorTail);

                putInt(p, kbSuccessor);
            }
            final int dbNew = kbytes[ebcNew] & 0xFF;
            //
            // Allocate space for the new tail block
            //
            setKeyBlockEnd(getKeyBlockEnd() + KEYBLOCK_LENGTH);

            int newTail = allocTail(newTailSize);
            if (newTail == -1) {
                _keyBlockEnd -= KEYBLOCK_LENGTH;
                repack();
                setKeyBlockEnd(getKeyBlockEnd() + KEYBLOCK_LENGTH);
                newTail = allocTail(newTailSize);
                if (newTail == -1) {
                    _persistit.fatal("Insufficient space to insert record in " + this + " at =" + p, null);
                }
            }

            // Shift the subsequent key blocks
            System.arraycopy(_bytes, p, _bytes, p + KEYBLOCK_LENGTH, _keyBlockEnd - p - KEYBLOCK_LENGTH);

            // Write new key block
            final int newKeyBlock = encodeKeyBlock(ebcNew, dbNew, newTail);
            putInt(p, newKeyBlock);

            // Write new tail block
            putInt(newTail, encodeTailBlock(newTailSize, klength));

            Debug.$assert0.t(klength >= 0 && ebcNew + 1 >= 0 && ebcNew + 1 + klength <= kbytes.length
                    && newTail + _tailHeaderSize >= 0 && newTail + _tailHeaderSize + klength <= _bytes.length);

            System.arraycopy(kbytes, ebcNew + 1, _bytes, newTail + _tailHeaderSize, klength);

            if (isIndexPage()) {
                final int pointer = (int) valueHelper.getPointerValue();

                Debug.$assert0.t(p + KEYBLOCK_LENGTH < _keyBlockEnd ? pointer > 0 : true);
                putInt(newTail + TAILBLOCK_POINTER, pointer);
            } else {
                final int offset = newTail + _tailHeaderSize + klength;
                final int storedLength = valueHelper.storeVersion(_bytes, offset, -1, _bytes.length); // TODO
                // limit
                incCountIfMvv(_bytes, offset, storedLength & MVV.STORE_LENGTH_MASK);

                Debug.$assert0.t(MVV.verify(_persistit.getTransactionIndex(), _bytes, offset, storedLength
                        & MVV.STORE_LENGTH_MASK));
            }
            //
            // Correct not to call getFastIndex()
            //
            _fastIndex.insertKeyBlock(p, ebcSuccessor, fixupSuccessor);
            bumpGeneration();

            if (p > KEY_BLOCK_START) {
                Debug.$assert0.t(adjacentKeyCheck(p - KEYBLOCK_LENGTH));
            }
            if (p + KEYBLOCK_LENGTH < _keyBlockEnd) {
                Debug.$assert0.t(adjacentKeyCheck(p));
            }

            if (Debug.ENABLED) {
                assertVerify();
            }

            return p | (key.getEncodedSize() << DEPTH_SHIFT) | EXACT_MASK;
        }
    }

    /**
     * This method is for debugging only. It should be asserted after a key has
     * been inserted or removed.
     *
     * @param p
     * @return when key is adjacent
     */
    private boolean adjacentKeyCheck(int p) {
        p &= P_MASK;
        final int kbData1 = getInt(p);
        final int kbData2 = getInt(p + KEYBLOCK_LENGTH);
        final int db1 = decodeKeyBlockDb(kbData1);
        final int ebc1 = decodeKeyBlockEbc(kbData1);
        final int db2 = decodeKeyBlockDb(kbData2);
        final int ebc2 = decodeKeyBlockEbc(kbData2);

        if (db1 == 0 && p > KEY_BLOCK_START) {
            return false; // Can set breakpoint here
        }
        if (db2 == 0) {
            return false; // Can set breakpoint here
        }

        if (ebc1 == ebc2 && db1 < db2)
            return true;
        if (ebc2 < ebc1)
            return true;
        if (ebc2 > ebc1) {
            final int tail1 = decodeKeyBlockTail(kbData1);
            final int tbData1 = getInt(tail1);
            final int klength1 = decodeTailBlockKLength(tbData1);
            int db = -1;
            if (klength1 >= ebc2 - ebc1) {
                db = _bytes[tail1 + _tailHeaderSize + ebc2 - ebc1 - 1] & DB_MASK;
                if (db2 > db)
                    return true;
                return false; // Can set breakpoint here
            } else if (klength1 + 1 == ebc2 - ebc1) {
                // All bytes of key1 are elided by key2
                return true;
            }
            return false; // Can set breakpoint here
        }
        return false; // Can set breakpoint here
    }

    private int replaceValue(final Key key, final ValueHelper valueHelper, final int p) {
        final int kbData = getInt(p);
        final int tail = decodeKeyBlockTail(kbData);
        final int tbData = getInt(tail);
        final int klength = decodeTailBlockKLength(tbData);
        final int oldTailSize = decodeTailBlockSize(tbData);
        boolean wasMVV = false;
        boolean isMVV = false;

        int length;
        if (isIndexPage()) {
            length = 0;
        } else {
            length = valueHelper.requiredLength(_bytes, tail + _tailHeaderSize + klength, oldTailSize - _tailHeaderSize
                    - klength);
            wasMVV = isValueMVV(_bytes, tail + _tailHeaderSize + klength, oldTailSize - _tailHeaderSize - klength);
        }

        final int newTailSize = klength + length + _tailHeaderSize;
        final int oldNext = (tail + oldTailSize + ~TAILBLOCK_MASK) & TAILBLOCK_MASK;
        final int newNext = (tail + newTailSize + ~TAILBLOCK_MASK) & TAILBLOCK_MASK;
        int newTail = tail;
        if (newNext < oldNext) {
            // Free the remainder of the old tail block
            deallocTail(newNext, oldNext - newNext);
        } else if (newNext > oldNext) {
            if (!willFit(newNext - oldNext)) {
                return -1; // need to split the page
            }
            //
            // Free the old tail block entirely
            //
            deallocTail(tail, oldTailSize);
            newTail = allocTail(newTailSize);
            if (newTail == -1) {
                repack();
                //
                // Guaranteed to succeed because of the test on willFit() above
                //
                newTail = allocTail(newTailSize);
                if (newTail == -1) {
                    _persistit.fatal("Insufficient space to replace records in " + this + " at =" + p, null);
                }
            }
            putInt(p, encodeKeyBlockTail(kbData, newTail));
        }
        putInt(newTail, encodeTailBlock(newTailSize, klength));
        if (newTail != tail) {
            System.arraycopy(key.getEncodedBytes(), key.getEncodedSize() - klength, _bytes, newTail + _tailHeaderSize,
                    klength);
        }

        if (isIndexPage()) {
            final long pointer = valueHelper.getPointerValue();
            Debug.$assert0.t(p + KEYBLOCK_LENGTH < _keyBlockEnd ? pointer > 0 : pointer == -1);
            putInt(newTail + TAILBLOCK_POINTER, (int) pointer);
        } else {
            final int offset = newTail + _tailHeaderSize + klength;
            final int storedLength = valueHelper.storeVersion(_bytes, offset, oldTailSize - _tailHeaderSize - klength,
                    _bytes.length); // TODO - limit
            isMVV = isValueMVV(_bytes, offset, storedLength & MVV.STORE_LENGTH_MASK);

            Debug.$assert0.t(MVV.verify(_persistit.getTransactionIndex(), _bytes, offset, storedLength
                    & MVV.STORE_LENGTH_MASK));

        }
        if (!wasMVV && isMVV) {
            _mvvCount++;
        } else if (wasMVV && !isMVV) {
            _mvvCount--;
        }
        if (Debug.ENABLED) {
            assertVerify();
        }
        return p | (key.getEncodedSize() << DEPTH_SHIFT) | EXACT_MASK;
    }

    /**
     * Removes the keys and associated values from the key blocks indicated by
     * the two argument. The removal range starts at foundAt1 inclusive and ends
     * at foundAt2 exclusive.
     *
     * @param foundAt1
     *            Encoded location of the first key block to remove
     * @param foundAt2
     *            Encoded location of the first key block beyond the removal
     *            range.
     * @param spareKey
     *            A key used in accumulating bytes for the successor key to the
     *            removal range. As a side effect, this key contains the last
     *            key to be removed by this method.
     * @return <i>true</i> if the key was found and the value removed, else
     *         <i>false</i>
     */
    boolean removeKeys(final int foundAt1, final int foundAt2, final Key spareKey) {
        if (Debug.ENABLED) {
            assertVerify();
        }
        final int p1 = foundAt1 & P_MASK;
        int p2 = foundAt2 & P_MASK;
        if ((foundAt2 & EXACT_MASK) != 0)
            p2 += KEYBLOCK_LENGTH;

        if (p1 < KEY_BLOCK_START || p1 > _keyBlockEnd || p2 < KEY_BLOCK_START || p2 > _keyBlockEnd) {
            throw new IllegalArgumentException("p1=" + p1 + " p2=" + p2 + " in " + summarize());
        }
        if (p2 <= p1)
            return false;

        int ebc = Integer.MAX_VALUE;

        final byte[] spareBytes = spareKey.getEncodedBytes();
        int keySize = 0;
        for (int p = p1; p < p2; p += KEYBLOCK_LENGTH) {
            final int kbData = getInt(p);
            final int ebcCandidate = decodeKeyBlockEbc(kbData);
            if (ebcCandidate < ebc) {
                ebc = ebcCandidate;
            }
            final int db = decodeKeyBlockDb(kbData);
            final int tail = decodeKeyBlockTail(kbData);
            final int tbData = getInt(tail);
            final int klength = decodeTailBlockKLength(tbData);
            spareBytes[ebcCandidate] = (byte) db;
            if (klength > 0) {
                System.arraycopy(_bytes, tail + _tailHeaderSize, spareBytes, ebcCandidate + 1, klength);
            }
            keySize = klength + ebcCandidate + 1;
            final int size = (decodeTailBlockSize(tbData) + ~TAILBLOCK_MASK) & TAILBLOCK_MASK;
            deallocTail(tail, size);
        }
        spareKey.setEncodedSize(keySize);
        //
        // Remove the deleted key blocks
        //
        System.arraycopy(_bytes, p2, _bytes, p1, _keyBlockEnd - p2);

        _keyBlockEnd -= (p2 - p1);

        //
        // All the tailblocks that need to be deleted have been marked free.
        // Now we need to reallocate the tail block for the successor key
        // if due to a loss of ebc count we need to represent more bytes
        // of its key. Note: p1 now points to that successor key block.
        //
        if (p1 < _keyBlockEnd) {
            int kbNext = getInt(p1);
            final int ebcNext = decodeKeyBlockEbc(kbNext);

            //
            // If ebcNext > ebc then the successor key will need to expand.
            //
            if (ebcNext > ebc) {
                int tailNext = decodeKeyBlockTail(kbNext);
                final int dbNext = decodeKeyBlockDb(kbNext);
                final int tbNext = getInt(tailNext);
                final int nextTailSize = decodeTailBlockSize(tbNext);

                final int nextTailBlockSize = (nextTailSize + ~TAILBLOCK_MASK) & TAILBLOCK_MASK;

                final int newNextTailBlockSize = (nextTailSize + ebcNext - ebc + ~TAILBLOCK_MASK) & TAILBLOCK_MASK;

                final int delta = newNextTailBlockSize - nextTailBlockSize;
                boolean freeNextTailBlock = false;
                int newNextTail = tailNext;
                if (delta > 0) {
                    // Now attempt to allocate a new tail block for the
                    // successor key.
                    //
                    newNextTail = allocTail(newNextTailBlockSize);
                    if (newNextTail == -1) {
                        // If allocTail failed to return a new block into which
                        // we can copy the successor block, then we'll try to
                        // wedge an allocation in right before it.
                        //
                        newNextTail = wedgeTail(tailNext, delta);
                        //
                        // If that fails, then we repack and try again.
                        //
                        if (newNextTail == -1) {
                            repack();
                            //
                            // Repacking will move the tail blocks so we need
                            // to refetch pointers to the ones we care about.
                            //
                            kbNext = getInt(p1);
                            tailNext = decodeKeyBlockTail(kbNext);
                            newNextTail = wedgeTail(tailNext, delta);
                        }
                    } else {
                        freeNextTailBlock = true;
                    }
                    if (newNextTail == -1) {
                        _persistit.fatal("Can't wedge enough space in " + this + " foundAt1=" + foundAt1 + " foundAt2="
                                + foundAt2 + " spareKey=" + spareKey + " nextTailBlockSize=" + nextTailBlockSize
                                + " newNextTailBlockSize=" + newNextTailBlockSize + " ebc=" + ebc + " ebcNext="
                                + ebcNext, null);
                    }
                }
                //
                // At this point newNextTail points to a tail block with
                // sufficient space to hold the revised successor key.
                // It may or may not be at a new location. We start by copying
                // the payload associated with the old tail block to its new
                // location.
                //
                if (isIndexPage()) {
                    putInt(newNextTail + TAILBLOCK_POINTER, getInt(tailNext + TAILBLOCK_POINTER));
                }
                System.arraycopy(_bytes, tailNext + _tailHeaderSize, _bytes, newNextTail + _tailHeaderSize + ebcNext
                        - ebc, nextTailSize - _tailHeaderSize);

                _bytes[newNextTail + _tailHeaderSize + ebcNext - ebc - 1] = (byte) dbNext;

                //
                // Next copy copy the additional bytes of key that are to be
                // removed.
                //
                System.arraycopy(spareBytes, ebc + 1, _bytes, newNextTail + _tailHeaderSize, ebcNext - ebc - 1);
                //
                // Now construct the new tail block
                //
                final int newNextKLength = decodeTailBlockKLength(tbNext) + ebcNext - ebc;
                final int newNextTailSize = nextTailSize + ebcNext - ebc;
                putInt(newNextTail, encodeTailBlock(newNextTailSize, newNextKLength));
                if (freeNextTailBlock) {
                    final int toFree = (nextTailSize + ~TAILBLOCK_MASK) & TAILBLOCK_MASK;
                    deallocTail(tailNext, toFree);
                }
                //
                // Fix up the successor key block
                //
                final int kbNewNext = encodeKeyBlock(ebc, spareBytes[ebc], newNextTail);
                putInt(p1, kbNewNext);
            }
        }
        invalidateFastIndex();
        bumpGeneration();
        if (Debug.ENABLED) {
            assertVerify();
        }
        return true;
    }

    /**
     * Moves the tail blocks below the specified tail offset downward in order
     * to free delta bytes of space before the specified tail block. This is
     * sometimes done in the removeKey() method to allow the key that follows
     * the one being removed to have its ebc value increased.
     *
     * @param tail
     *            Offset of tail block to be expanded downward
     * @param delta
     *            The amount by which that block is to be expanded.
     * @return int Offset of the expanded tail block, or -1 if it does not fit.
     */
    private int wedgeTail(final int tail, int delta) {
        delta = (delta + ~TAILBLOCK_MASK) & TAILBLOCK_MASK;
        if (delta == 0) {
            return tail;
        }
        if (delta < 0) {
            throw new IllegalArgumentException("wedgeTail delta must be positive: " + delta + " is invalid");
        }
        if (_alloc - _keyBlockEnd < delta) {
            return -1;
        }

        System.arraycopy(_bytes, _alloc, _bytes, _alloc - delta, tail - _alloc);
        _alloc -= delta;

        for (int p = KEY_BLOCK_START; p < _keyBlockEnd; p += KEYBLOCK_LENGTH) {
            final int kbData = getInt(p);
            final int oldTail = decodeKeyBlockTail(kbData);
            if (oldTail < tail) {
                putInt(p, encodeKeyBlockTail(kbData, oldTail - delta));
            }
        }
        return tail - delta;
    }

    /**
     * Copies keys and values to a newly allocated buffer in order to make room
     * for an insertion, and then performs the insertion.
     *
     * @param rightSibling
     *            The buffer containing the new right sibling page
     * @param key
     *            The key being inserted or replaced
     * @param value
     *            The new value
     * @param foundAt
     *            Offset to keyblock where insertion will occur
     * @param indexKey
     *            A Key into which this method writes the rightmost key in the
     *            left sibling page.
     * @param sequence
     *            The insert sequence (FORWARD, REVERSE or NONE)
     * @param policy
     *            The SplitPolicy for this insertion
     * @return offset of the inserted key block. If positive, this value denotes
     *         a location in this Buffer. If negative, it denotes a location in
     *         the right sibling Buffer.
     * @throws PersistitException
     */
    final int split(final Buffer rightSibling, final Key key, final ValueHelper valueHelper, int foundAt,
            final Key indexKey, final Sequence sequence, final SplitPolicy policy) throws PersistitException {
        // Make sure the right sibling page is empty.

        Debug.$assert0.t(rightSibling._keyBlockEnd == KEY_BLOCK_START);
        Debug.$assert0.t(rightSibling._alloc == rightSibling._bufferSize);
        if (Debug.ENABLED) {
            assertVerify();
        }

        if (_mvvCount > 0) {
            rightSibling._mvvCount = Integer.MAX_VALUE;
        }

        // First we calculate how large the virtual page containing the
        // modified or inserted key/value pair would be.
        //
        int currentSize = _bufferSize - _alloc - _slack + _keyBlockEnd - KEY_BLOCK_START;

        final int foundAtPosition = foundAt & P_MASK;
        final boolean exact = (foundAt & EXACT_MASK) != 0;
        int depth = (foundAt & DEPTH_MASK) >>> DEPTH_SHIFT;
        final boolean fixupSuccessor = (foundAt & FIXUP_MASK) > 0;

        int ebcNew;
        int ebcSuccessor;
        int deltaSuccessorTailSize = 0;
        int deltaSuccessorEbc = 0;

        if (fixupSuccessor) {
            final int kbSuccessor = getInt(foundAtPosition);
            final int tbSuccessor = getInt(decodeKeyBlockTail(kbSuccessor));
            ebcNew = decodeKeyBlockEbc(kbSuccessor);
            ebcSuccessor = depth;
            final int tbSize = decodeTailBlockSize(tbSuccessor);

            // This is the number of bytes by which the successor key
            // can have its elided byte count increased.
            deltaSuccessorEbc = ebcSuccessor - ebcNew;

            final int oldSize = (tbSize + ~TAILBLOCK_MASK) & TAILBLOCK_MASK;
            final int newSize = (tbSize - deltaSuccessorEbc + ~TAILBLOCK_MASK) & TAILBLOCK_MASK;

            // This is the number of bytes by which the successor tail block
            // can be reduced (because its elision count has increased.)
            deltaSuccessorTailSize = oldSize - newSize;
        } else {
            ebcSuccessor = 0;
            ebcNew = depth;
        }
        int keyBlockSizeDelta = KEYBLOCK_LENGTH;
        int oldTailBlockSize = 0;
        int newTailBlockSize;
        int newValueSize;
        if (exact) {
            final int kbData = getInt(foundAtPosition);
            final int tail = decodeKeyBlockTail(kbData);
            final int tbData = getInt(tail);
            final int tbSize = decodeTailBlockSize(tbData);
            final int klength = decodeTailBlockKLength(tbData);
            oldTailBlockSize = (tbSize + ~TAILBLOCK_MASK) & TAILBLOCK_MASK;
            keyBlockSizeDelta = 0;
            ebcNew = decodeKeyBlockEbc(kbData);
            newValueSize = valueHelper.requiredLength(_bytes, tail + _tailHeaderSize + klength, tbSize
                    - _tailHeaderSize - klength);
        } else {
            newValueSize = valueHelper.requiredLength(_bytes, 0, -1);
        }
        newTailBlockSize = ((isIndexPage() ? 0 : newValueSize) + _tailHeaderSize + key.getEncodedSize() - ebcNew - 1 + ~TAILBLOCK_MASK)
                & TAILBLOCK_MASK;

        final int virtualSize = currentSize + newTailBlockSize - oldTailBlockSize + keyBlockSizeDelta
                - deltaSuccessorTailSize;

        int splitBest = 0; // Maximal fitness measure
        int splitAt = 0; // Offset to first keyblock in right sibling

        int leftSize = 0; // Bytes in left sibling page

        boolean armed = true;
        int whereInserted = -1;

        final int rightKeyBlock = _keyBlockEnd - KEYBLOCK_LENGTH;
        for (int p = KEY_BLOCK_START; p < rightKeyBlock;) {
            int splitCandidate = 0;
            if (p == foundAtPosition && armed) {

                // Here we are adding the newly inserted (or replacing) record
                // to the left side. The key block pointed to by p is our
                // candidate for the first key in the right sibling page.

                leftSize += newTailBlockSize + KEYBLOCK_LENGTH;
                if (exact) {
                    p += KEYBLOCK_LENGTH;
                }

                // Compute the number of bytes by which the successor tailblock
                // will grow due to its elision count becoming zero.
                //
                final int kbData = getInt(p);
                final int tbData = getInt(decodeKeyBlockTail(kbData));
                final int ebc = decodeKeyBlockEbc(kbData);
                final int tbSize = decodeTailBlockSize(tbData);
                final int tbSizeDelta = ((tbSize + ebc + ~TAILBLOCK_MASK) & TAILBLOCK_MASK)
                        - ((tbSize + ~TAILBLOCK_MASK) & TAILBLOCK_MASK);

                final int edgeTailBlockSize = (decodeTailBlockKLength(tbData) - deltaSuccessorEbc + _tailHeaderSize + ~TAILBLOCK_MASK)
                        & TAILBLOCK_MASK;

                if (p < rightKeyBlock) {
                    final int rightSize = virtualSize - leftSize + tbSizeDelta;

                    splitCandidate = policy.splitFit(this, p, foundAtPosition, exact, leftSize + KEYBLOCK_LENGTH
                            + edgeTailBlockSize, rightSize, currentSize, virtualSize, _bufferSize - KEY_BLOCK_START,
                            splitBest, sequence);
                    if (splitCandidate > splitBest) {
                        splitBest = splitCandidate;
                        splitAt = p | EXACT_MASK;
                    }
                }
                armed = false;
            } else {
                // Here we are proposing the key block pointed to by p as the
                // last key of the left page.
                int kbData = getInt(p);
                int tbData = getInt(decodeKeyBlockTail(kbData));
                int tbSizeDelta;
                final int tailBlockSize = (decodeTailBlockSize(tbData) + ~TAILBLOCK_MASK) & TAILBLOCK_MASK;
                leftSize += tailBlockSize + KEYBLOCK_LENGTH;

                p += KEYBLOCK_LENGTH;
                int edgeTailBlockSize;

                if (p == foundAtPosition && armed) {
                    tbSizeDelta = (((isIndexPage() ? 0 : newValueSize) + _tailHeaderSize + key.getEncodedSize() + ~TAILBLOCK_MASK) & TAILBLOCK_MASK)
                            - newTailBlockSize;

                    edgeTailBlockSize = (key.getEncodedSize() - depth + _tailHeaderSize + ~TAILBLOCK_MASK)
                            & TAILBLOCK_MASK;
                } else {
                    kbData = getInt(p);
                    tbData = getInt(decodeKeyBlockTail(kbData));
                    final int ebc = decodeKeyBlockEbc(kbData);
                    final int tbSize = decodeTailBlockSize(tbData);

                    tbSizeDelta = ((tbSize + ebc + ~TAILBLOCK_MASK) & TAILBLOCK_MASK)
                            - ((tbSize + ~TAILBLOCK_MASK) & TAILBLOCK_MASK);

                    edgeTailBlockSize = (decodeTailBlockKLength(tbData) + _tailHeaderSize + ~TAILBLOCK_MASK)
                            & TAILBLOCK_MASK;
                }

                if (p < rightKeyBlock) {
                    final int rightSize = virtualSize - leftSize + tbSizeDelta;

                    splitCandidate = policy.splitFit(this, p, foundAtPosition, exact, leftSize + KEYBLOCK_LENGTH
                            + edgeTailBlockSize, rightSize, currentSize, virtualSize, _bufferSize - KEY_BLOCK_START,
                            splitBest, sequence);
                    if (splitCandidate > splitBest) {
                        splitBest = splitCandidate;
                        splitAt = p;
                    }
                }
            }
            // Following is true when we have gone past the best split
            // point.
            if (splitCandidate == 0 && splitBest != 0) {
                break;
            }
        }

        if (splitBest == 0) {
            throw new InvalidPageStructureException("Can't split page " + this + " exact=" + exact + " insertAt="
                    + foundAtPosition + " currentSize=" + currentSize + " virtualSize=" + virtualSize + " leftSize="
                    + leftSize);
        }
        //
        // Now move the keys and records.
        //
        final byte[] indexKeyBytes = indexKey.getEncodedBytes();
        final int splitAtPosition = splitAt & P_MASK;

        final boolean lastLeft = (splitAt & EXACT_MASK) != 0;
        final boolean firstRight = !lastLeft && splitAtPosition == foundAtPosition;
        int indexKeyDepth = 0;
        //
        // First we need to compute the full key in the right sibling page.
        //
        int scanStart = KEY_BLOCK_START;
        if (foundAtPosition <= splitAt) {
            // We know that the value of key is at or before foundAt, so we
            // can just copy the bytes from key and avoid scanning the records
            // to the left of foundAt.
            //
            scanStart = foundAtPosition;
            indexKeyDepth = key.getEncodedSize();
            System.arraycopy(key.getEncodedBytes(), 0, indexKeyBytes, 0, indexKeyDepth);
        }
        //
        // If we are inserting the new key as the first key of the right
        // page, then do not scan because the insert key is the same as the
        // split key.
        //
        if (!firstRight) {
            for (int p = scanStart; p <= splitAtPosition; p += KEYBLOCK_LENGTH) {
                final int kbData = getInt(p);
                final int ebc = decodeKeyBlockEbc(kbData);
                final int db = decodeKeyBlockDb(kbData);
                final int tail = decodeKeyBlockTail(kbData);
                if (ebc > indexKeyDepth) {
                    throw new InvalidPageStructureException("ebc at " + p + " ebc=" + ebc + " > indexKeyDepth="
                            + indexKeyDepth);
                }
                indexKeyDepth = ebc;
                indexKeyBytes[indexKeyDepth++] = (byte) db;
                final int tbData = getInt(tail);
                final int klength = decodeTailBlockKLength(tbData);

                System.arraycopy(_bytes, tail + _tailHeaderSize, indexKeyBytes, indexKeyDepth, klength);
                indexKeyDepth += klength;
            }
        }
        indexKey.setEncodedSize(indexKeyDepth);
        //
        // Set the keyblock end for the right sibling so that alloc() knows
        // its bounds.
        //
        rightSibling.setKeyBlockEnd(_keyBlockEnd - (splitAtPosition - KEY_BLOCK_START));
        int rightP = KEY_BLOCK_START;
        //
        // Now move all the records from the split point forward to the
        // right page and deallocate their space in the left page.
        //

        for (int p = splitAtPosition; p < _keyBlockEnd; p += KEYBLOCK_LENGTH) {

            final int kbData = getInt(p);
            final int db = decodeKeyBlockDb(kbData);
            final int ebc = decodeKeyBlockEbc(kbData);
            final int tail = decodeKeyBlockTail(kbData);

            final int tbData = getInt(tail);
            final int klength = decodeTailBlockKLength(tbData);
            final int tailBlockSize = decodeTailBlockSize(tbData);
            final int dataSize = tailBlockSize - _tailHeaderSize - klength;
            final int newKeyLength;
            final int newDb;
            final int newEbc;
            //
            // Adjust for first key in right page
            //
            if (p == splitAtPosition) {
                newKeyLength = klength + ebc;
                newDb = ebc > 0 ? indexKeyBytes[0] : db;
                newEbc = 0;
            } else {
                newKeyLength = klength;
                newDb = db;
                newEbc = ebc;
            }
            //
            // Adjust for replacement case.
            //
            int newDataSize;
            if (exact && isDataPage() && foundAtPosition == p) {
                newDataSize = newValueSize;
                Debug.$assert0.t(newDataSize > dataSize);
            } else {
                newDataSize = dataSize;
            }
            newTailBlockSize = newKeyLength + newDataSize + _tailHeaderSize;
            //
            // Allocate the new tail block.
            //
            final int newTailBlock = rightSibling.allocTail(newTailBlockSize);
            Debug.$assert0.t(newTailBlock >= 0 && newTailBlock < rightSibling._bufferSize);
            Debug.$assert0.t(newTailBlock != -1);

            rightSibling.putInt(newTailBlock, encodeTailBlock(newTailBlockSize, newKeyLength));

            if (p == splitAtPosition && ebc > 0) {
                System.arraycopy(indexKeyBytes, 1, // Note: byte 0 is the
                                                   // discriminator byte
                        rightSibling._bytes, newTailBlock + _tailHeaderSize, ebc - 1);

                rightSibling.putByte(newTailBlock + _tailHeaderSize + ebc - 1, db);

                System.arraycopy(_bytes, tail + _tailHeaderSize, rightSibling._bytes, newTailBlock + _tailHeaderSize
                        + ebc, klength);
            } else {
                System.arraycopy(_bytes, tail + _tailHeaderSize, rightSibling._bytes, newTailBlock + _tailHeaderSize,
                        klength);
            }

            if (isDataPage()) {
                System.arraycopy(_bytes, tail + _tailHeaderSize + klength, rightSibling._bytes, newTailBlock
                        + _tailHeaderSize + newKeyLength, dataSize);
            } else {
                rightSibling.putInt(newTailBlock + TAILBLOCK_POINTER, getInt(tail + TAILBLOCK_POINTER));
            }

            //
            // Put the key block into the right page.
            //
            rightSibling.putInt(rightP, encodeKeyBlock(newEbc, newDb, newTailBlock));
            rightP += KEYBLOCK_LENGTH;
            //
            // Deallocate the tailblock from the left page. If this is the
            // split key, then we deallocate the whole tail only if the
            // insert key is going in as the first key in the right sibling.
            // Otherwise, we simply deallocate the data.
            //
            //
            if (p != splitAtPosition || (firstRight && !exact)) {
                // deallocate the whole tail block. If this is the split
                // position, then we are going to add a new, different edge
                // key below.
                //
                deallocTail(tail, tailBlockSize);
            } else {
                // This is the split position, and the insert key is either
                // an exact match for the first key in the right sibling page,
                // or it is not being inserted there. In this case, the key
                // we are examining in the left sibling page will become the
                // new edge key. All we need to do is deallocate any extra
                // space needed by this key's data.
                //
                // Later, we will fix up the edge key in the case where the
                // insert key will be the first key of the right sibling.
                //
                if (isDataPage()) {
                    currentSize = (tailBlockSize + ~TAILBLOCK_MASK) & TAILBLOCK_MASK;

                    final int newSize = (tailBlockSize - dataSize + ~TAILBLOCK_MASK) & TAILBLOCK_MASK;
                    if (newSize != currentSize) {
                        deallocTail(tail + newSize, currentSize - newSize);
                    }
                    putInt(tail, encodeTailBlock(_tailHeaderSize + klength, klength));
                } else {
                    // edge key for index block. We destroy the pointer value
                    // so that we don't get confused later.
                    putInt(tail + TAILBLOCK_POINTER, -1);
                }
            }
        }
        //
        // Fix up the right edge key in the left page.
        //
        final int kbData = getInt(splitAtPosition);
        int edgeTail = decodeKeyBlockTail(kbData);
        final int ebc = decodeKeyBlockEbc(kbData);
        depth = (foundAt & DEPTH_MASK) >>> DEPTH_SHIFT;

        if (firstRight && !exact) {
            //
            // Handle the special case where the inserted key is the first
            // key of the right page.
            //
            if (fixupSuccessor) {
                depth = ebc;
            }
            final int db = indexKeyBytes[depth];

            final int edgeKeyLength = indexKey.getEncodedSize() - depth - 1;
            final int edgeTailBlockSize = edgeKeyLength + _tailHeaderSize;
            edgeTail = allocTail(edgeTailBlockSize);
            if (edgeTail == -1) {
                setKeyBlockEnd(splitAtPosition);
                repack();
                edgeTail = allocTail(edgeTailBlockSize);
            }

            if (edgeTail == -1) {
                _persistit
                        .fatal("Insufficient space for edgeTail records in " + this + " at =" + splitAtPosition, null);
            }
            putInt(edgeTail, encodeTailBlock(edgeTailBlockSize, edgeKeyLength));

            System.arraycopy(indexKeyBytes, depth + 1, _bytes, edgeTail + _tailHeaderSize, edgeKeyLength);

            putInt(splitAtPosition, encodeKeyBlock(depth, db, edgeTail));
        }
        //
        // In any case, the new key block end is at the split position.
        //
        setKeyBlockEnd(splitAtPosition + KEYBLOCK_LENGTH);
        //
        // If this is in index level, then set the pointer value to -1 so that
        // it does not later get confused and interpreted as a valid pointer.
        //
        if (isIndexPage()) {
            putInt(edgeTail + TAILBLOCK_POINTER, -1);
        }

        invalidateFastIndex();
        rightSibling.invalidateFastIndex();

        Debug.$assert0.t(rightSibling._keyBlockEnd > KEY_BLOCK_START + KEYBLOCK_LENGTH ? rightSibling
                .adjacentKeyCheck(KEY_BLOCK_START) : true);

        if (!exact) {
            if (foundAtPosition >= splitAtPosition && (!lastLeft || foundAtPosition > splitAtPosition)) {
                foundAt -= (splitAtPosition - KEY_BLOCK_START);
                if (firstRight && !fixupSuccessor) {
                    foundAt = (foundAt & P_MASK) | (ebc > 0 ? FIXUP_MASK : 0) | (ebc << DEPTH_SHIFT);
                }
                final int t = rightSibling.putValue(key, valueHelper, foundAt, true);
                whereInserted = -foundAt;
                Debug.$assert0.t(t != -1);
            } else {
                final int t = putValue(key, valueHelper, foundAt, true);
                whereInserted = foundAt;
                Debug.$assert0.t(t != -1);
            }
        } else {
            if (foundAtPosition < splitAtPosition) {
                whereInserted = replaceValue(key, valueHelper, foundAtPosition);
            } else {
                whereInserted = rightSibling.replaceValue(key, valueHelper, foundAtPosition - splitAtPosition
                        + KEY_BLOCK_START);
            }
            //
            // It is really bad if whereInserted is less than 0. Means that we
            // failed to replace the value.
            //
            Debug.$assert0.t(whereInserted > 0);
            if (whereInserted <= 0) {
                throw new IllegalStateException("p = " + whereInserted + " foundAtPosition=" + foundAtPosition
                        + " splitAtPosition=" + splitAtPosition);
            }
        }

        Debug.$assert0.t(KEY_BLOCK_START + KEYBLOCK_LENGTH < _keyBlockEnd);
        Debug.$assert0.t(KEY_BLOCK_START + KEYBLOCK_LENGTH < rightSibling._keyBlockEnd);

        // Indicate that both buffers have changed.
        bumpGeneration();
        rightSibling.bumpGeneration();

        if (Debug.ENABLED) {
            assertVerify();
            rightSibling.assertVerify();
        }

        return whereInserted;
    }

    /**
     * <p>
     * Join or rebalance two pages as part of a deletion operation. This buffer
     * contains the left edge of the deletion. All the keys at or above foundAt1
     * are to be removed. The supplied <code>Buffer</code> contains the key at
     * the right edge of the deletion. All keys up to, but not including
     * foundAt2 are to be removed from it. Comments and variable names use the
     * words "left" and "right" to refer to this Buffer and the supplied Buffer,
     * respectively.
     * </p>
     * <p>
     * This method attempts to combine all the remaining keys and data into one
     * page. If they will not fit, then it rebalances the keys and values across
     * the two pages. As a side effect, it copies the first key of the
     * rebalanced right page into the supplied <code>indexKey</code>. The caller
     * will then reinsert that key value into index pages above this one.
     * </p>
     * <p>
     * In an extremely rare case the deletion of a key from a pair of pages
     * results in a state where the two pages cannot be rebalanced at all. For
     * this to happen the following must hold:
     * <ul>
     * <li>Let K1 and K2 be the initial key of the right page and the key at
     * foundAt2, respectively.</li>
     * <li>K2 is longer than K1.</li>
     * <li>There is no feasible way to arrange records on the two pages without
     * placing K2 at the left edge of the rebalanced right page.</li>
     * <li>The left page has insufficient free space after removing the former
     * right-edge key K1 to hold K2.</li>
     * </ul>
     * In this case this method leaves both buffers unchanged and throws a
     * <code>RebalanceException</code>. Calling code should catch and handle
     * this exception by, for example, splitting one of the pages.
     * </p>
     *
     * @param buffer
     *            The buffer containing the right edge key
     * @param foundAt1
     *            Offset of the first key block to remove
     * @param foundAt2
     *            Offset of the first key block in buffer to keep
     * @param spareKey
     *            A spare Key used internally for intermediate results
     * @param policy
     *            The JoinPolicy that allocates records between the two pages.
     * @return <i>true</i> if the result is a rebalanced pair of pages or
     *         <i>false</i> if the two pages were joined into a single page.
     * @throws RebalanceException
     *             in the rare case where no rearrangement of the records is
     *             possible.
     */
    final boolean join(final Buffer buffer, int foundAt1, int foundAt2, final Key indexKey, final Key spareKey,
            final JoinPolicy policy) throws RebalanceException {
        foundAt1 &= P_MASK;
        foundAt2 &= P_MASK;

        if (buffer == this || foundAt1 <= KEY_BLOCK_START || foundAt1 >= _keyBlockEnd || foundAt2 <= KEY_BLOCK_START
                || foundAt2 >= buffer._keyBlockEnd) {
            Debug.$assert0.t(false);
            throw new IllegalArgumentException("foundAt1=" + foundAt1 + " foundAt2=" + foundAt2 + " _keyBlockEnd="
                    + _keyBlockEnd + " buffer._keyBlockEnd=" + buffer._keyBlockEnd);
        }

        if (Debug.ENABLED) {
            spareKey.clear();
            assertVerify();
            buffer.assertVerify();
        }

        final boolean hasMVV = (_mvvCount > 0) || (buffer.getMvvCount() > 0);
        //
        // Working variables
        //
        int kbData; // content of a key block
        int tbData; // content of a tail block header
        int tail; // offset within page of a tail block

        final byte[] spareKeyBytes = spareKey.getEncodedBytes();
        final byte[] indexKeyBytes = indexKey.getEncodedBytes();
        //
        // Initialize spareKey to contain the first key of the right
        // page.
        //
        buffer.keyAt(foundAt2, spareKey);

        final long measureLeft = joinMeasure(foundAt1, _keyBlockEnd);
        final long measureRight = buffer.joinMeasure(KEY_BLOCK_START, foundAt2);
        kbData = buffer.getInt(foundAt2);
        final int oldEbc = decodeKeyBlockEbc(kbData);

        final int newEbc;
        if (_rightSibling == buffer._page) {
            /*
             * Pages are are contiguous so first key of right sibling is
             * identical to max key of the current page.
             */
            newEbc = Math.min(oldEbc, Math.min((int) (measureLeft >>> 32), (int) (measureRight >>> 32)));
        } else {
            /*
             * Since pages are not contiguous, we need to extract the max key
             * from the current page and compute the ebc from it.
             */
            keyAt(foundAt1, indexKey);
            final int firstUnique = indexKey.firstUniqueByteIndex(spareKey);
            newEbc = Math.min(Math.min(oldEbc, firstUnique),
                    Math.min((int) (measureLeft >>> 32), (int) (measureRight >>> 32)));
        }

        tail = decodeKeyBlockTail(kbData);
        tbData = buffer.getInt(tail);

        final int oldSize = decodeTailBlockSize(tbData);
        final int newSize = oldSize + (oldEbc - newEbc);
        final int adjustmentForNewEbc = ((newSize + ~TAILBLOCK_MASK) & TAILBLOCK_MASK)
                - ((oldSize + ~TAILBLOCK_MASK) & TAILBLOCK_MASK);

        /*
         * Size and number of keys in the virtual page that would result from
         * joining all non-removed records.
         */
        final int virtualSize = inUseSize() + buffer.inUseSize() - (int) measureLeft - (int) measureRight
                + adjustmentForNewEbc + KEY_BLOCK_START;

        final int virtualKeyCount = ((foundAt1 - KEY_BLOCK_START) + (buffer.getKeyBlockEnd() - foundAt2))
                / KEYBLOCK_LENGTH;

        final boolean okayToRejoin = virtualKeyCount < _pool.getMaxKeys() && policy.acceptJoin(this, virtualSize);

        boolean result;

        if (okayToRejoin) {
            /*
             * REJOIN CASE
             *
             * -----------
             *
             * This is the case where according to the JoinPolicy, the records
             * from the right page can be merged into the left page. The caller
             * will then deallocate the right page.
             */
            Debug.$assert0.t(virtualSize <= _bufferSize);
            /*
             * Deallocate the records being removed from the left page
             */
            joinDeallocateTails(foundAt1, _keyBlockEnd);

            if (newEbc < oldEbc) {
                /*
                 * Deallocate the records being removed from the right page and
                 * then reduce the ebc of the first remaining key as needed. The
                 * only reason for deallocating tail blocks in the right page is
                 * that reduceEbc may need some addition space. Later the right
                 * page will be cleared.
                 */
                buffer.joinDeallocateTails(KEY_BLOCK_START, foundAt2);
                buffer.reduceEbc(foundAt2, newEbc, spareKeyBytes);
            }

            setKeyBlockEnd(foundAt1);
            /*
             * Move non-removed records from the right page to the left page.
             */
            moveRecords(buffer, foundAt2, buffer._keyBlockEnd, foundAt1, false);

            /*
             * Now set the right page to have no key blocks; this allows all the
             * remaining tail block space to be deallocated if the page is
             * subsequently written to disk.
             */
            buffer.setKeyBlockEnd(KEY_BLOCK_START);
            buffer.clearBytes(KEY_BLOCK_START, _bufferSize);
            buffer.setAlloc(_bufferSize);
            /*
             * unsplice the right buffer from the right sibling chain.
             */
            final long rightSibling = buffer.getRightSibling();
            setRightSibling(rightSibling);
            if (hasMVV) {
                _mvvCount = Integer.MAX_VALUE;
            }
            invalidateFastIndex();
            result = false;
        }

        else {
            /*
             * REBALANCE CASE
             *
             * --------------
             *
             * The remaining records still require two pages. Some key must be
             * chosen to serve as the edge between the pages. Use the JoinPolicy
             * to find the optimal location.
             */
            int joinOffset = joinMeasureRebalanceOffset(buffer, virtualSize, foundAt1, foundAt2, adjustmentForNewEbc,
                    policy);

            if (joinOffset == 0) {
                /*
                 * Rebalancing is infeasible
                 */
                throw new RebalanceException();
            } else if (joinOffset < 0) {
                /*
                 * Move records from the right page to the left page.
                 */
                joinOffset = -joinOffset;
                buffer.keyAt(joinOffset, indexKey);
                /*
                 * Remove records from the left page
                 */
                joinDeallocateTails(foundAt1, _keyBlockEnd);
                clearBytes(foundAt1, _keyBlockEnd);
                setKeyBlockEnd(foundAt1);

                /*
                 * Deallocate the records being removed from the right page and
                 * then reduce the ebc of the first remaining key as needed.
                 */
                buffer.joinDeallocateTails(KEY_BLOCK_START, foundAt2);
                buffer.clearBytes(KEY_BLOCK_START, foundAt2);
                buffer.reduceEbc(foundAt2, newEbc, spareKeyBytes);
                final int rightSize = buffer._keyBlockEnd - joinOffset;

                moveRecords(buffer, foundAt2, joinOffset, _keyBlockEnd, true);

                System.arraycopy(buffer._bytes, joinOffset, buffer._bytes, KEY_BLOCK_START, rightSize);
                buffer.clearBytes(KEY_BLOCK_START + rightSize, buffer._keyBlockEnd);
                buffer.setKeyBlockEnd(KEY_BLOCK_START + rightSize);
                buffer.reduceEbc(KEY_BLOCK_START, 0, indexKeyBytes);

            } else {
                /*
                 * We will move records from the left page to the right page.
                 */
                keyAt(joinOffset, indexKey);
                joinDeallocateTails(foundAt1, _keyBlockEnd);
                clearBytes(foundAt1, _keyBlockEnd);
                setKeyBlockEnd(foundAt1);

                buffer.joinDeallocateTails(KEY_BLOCK_START, foundAt2);

                final int rightSize = buffer._keyBlockEnd - foundAt2;
                System.arraycopy(buffer._bytes, foundAt2, buffer._bytes, KEY_BLOCK_START, rightSize);
                buffer.clearBytes(KEY_BLOCK_START + rightSize, buffer._keyBlockEnd);
                buffer.setKeyBlockEnd(KEY_BLOCK_START + rightSize);
                buffer.reduceEbc(KEY_BLOCK_START, newEbc, spareKeyBytes);

                if (joinOffset != foundAt1) {
                    buffer.moveRecords(this, joinOffset, foundAt1, KEY_BLOCK_START, false);
                    setKeyBlockEnd(joinOffset);
                }
                /*
                 * Set up the edge key in the left page.
                 */
                moveRecords(buffer, KEY_BLOCK_START, KEY_BLOCK_START, joinOffset, true);
                /*
                 * Fix the left key of the right page so that its ebc is zero.
                 */
                buffer.reduceEbc(KEY_BLOCK_START, 0, indexKeyBytes);
            }

            setRightSibling(buffer.getPageAddress());

            invalidateFastIndex();
            buffer.invalidateFastIndex();

            if (hasMVV) {
                _mvvCount = Integer.MAX_VALUE;
                buffer._mvvCount = Integer.MAX_VALUE;
            }
            result = true;
        }

        Debug.$assert0.t(KEY_BLOCK_START + KEYBLOCK_LENGTH < _keyBlockEnd);

        if (result) {
            Debug.$assert0.t(KEY_BLOCK_START + KEYBLOCK_LENGTH < buffer._keyBlockEnd);
        }
        /*
         * Indicate that both buffers have changed
         */
        bumpGeneration();
        buffer.bumpGeneration();

        if (Debug.ENABLED) {
            assertVerify();
            if (result) {
                buffer.assertVerify();
            }
        }

        return result;
    }

    /**
     * Compute total size used by key blocks and tail blocks within a page. Does
     * not include the size of the page header.
     *
     * @return computed size
     */
    int inUseSize() {
        return (_keyBlockEnd - KEY_BLOCK_START) + (_bufferSize - _alloc) - _slack;
    }

    /**
     * <p>
     * Measure the number of bytes in the buffer that will be freed by removing
     * a range of key blocks and their associated tail blocks. Also determine
     * the minimum ebc value among of all the key blocks being deleted. For
     * example, if <code>from</code> and <code>to</code> delimit exactly one key
     * block, then the value will reflect the size of that key block
     * (KEYBLOCK_LENGTH) plus the size of its associated tail block. The minimum
     * ebc value will the ebc encoded in that key block.
     * </p>
     * <p>
     * This method returns a long which encoded these two values as <code><pre>
     *     (minimumEbc << 32) | totalDeallocatedSize
     * </pre></code>
     * </p>
     * <p>
     * DOES NOT MODIFY BUFFER
     * </p>
     *
     * @param from
     *            offset of first key block being deleted
     * @param to
     *            offset of the next key block not being deleted
     * @return long encoding the size being deleted and the minimum ebc
     *
     */
    long joinMeasure(final int from, final int to) {
        int minimumEbc = Integer.MAX_VALUE;
        int totalDeallocatedSize = 0;
        for (int index = from; index < to; index += KEYBLOCK_LENGTH) {
            final int kbData = getInt(index);
            final int ebc = decodeKeyBlockEbc(kbData);
            if (index != KEY_BLOCK_START && ebc < minimumEbc) {
                minimumEbc = ebc;
            }
            final int tail = decodeKeyBlockTail(kbData);
            final int tbData = getInt(tail);
            final int size = decodeTailBlockSize(tbData);
            totalDeallocatedSize += ((size + ~TAILBLOCK_MASK) & TAILBLOCK_MASK) + KEYBLOCK_LENGTH;
        }
        return (((long) minimumEbc) << 32) | totalDeallocatedSize;
    }

    /**
     * <p>
     * Deallocate tail blocks associated with all key blocks delimited by key
     * block offsets <code>from</code> and <code>to</code>.
     * </p>
     * <p>
     * MODIFIES THIS BUFFER
     * </p>
     *
     * @param from
     *            offset of first key block being deleted
     * @param to
     *            offset of the next key block not being deleted
     */
    void joinDeallocateTails(final int from, final int to) {
        for (int index = from; index < to; index += KEYBLOCK_LENGTH) {
            final int kbData = getInt(index);
            final int tail = decodeKeyBlockTail(kbData);
            final int tbData = getInt(tail);
            final int size = (decodeTailBlockSize(tbData) + ~TAILBLOCK_MASK) & TAILBLOCK_MASK;
            deallocTail(tail, size);
        }
    }

    /**
     * <p>
     * Compute the offset of key block at which two pages from which keys are
     * being removed will be rebalanced. Uses the supplied
     * <code>JoinPolicy</code> to compute a cost value for each possible offset
     * and returns the optimal one.
     * </p>
     * <p>
     * Let p be the result. If p is positive, the records starting at p in the
     * current page should be moved into <code>buffer</code>. If the p is
     * negative, records starting at offset -p in <code>buffer</code> should be
     * moved into this buffer. If p is zero, there is no offset at which
     * rebalancing is possible.
     * </p>
     * <p>
     * The last case is possible (albeit extremely rare) because removing a key
     * that falls at the beginning of the right page may cause a condition in
     * which the both pages remain extremely full and any key elected to become
     * the new right edge key of the left page is unable to fit in the left
     * page. This happens if the key being removed is shorter than the one
     * following it and both pages are nearly full.
     * </p>
     * <p>
     * DOES NOT MODIFY EITHER BUFFER
     * </p>
     *
     * @param buffer
     *            The buffer containing the right sibling page
     * @param virtualSize
     *            Size of a virtual page constructed by merging all records not
     *            being deleted
     * @param foundAt1
     *            Offset in the current buffer of the first key block being
     *            removed
     * @param foundAt2
     *            Offset in <code>buffer</code> of the next key block not being
     *            removed
     * @param adjustmentForNewEbc
     *            amount by which the tail block of the record at foundAt2 must
     *            increase to allow for a reduced ebc value
     * @param indexKey
     *            Key in which the newly elected edge key will be returned
     * @param spareKey
     *            A spare Key in which intermediate key values can be
     *            accumulated
     * @param policy
     *            JoinPolicy used to choose optimal rebalance point
     * @return the join offset as described above
     */

    int joinMeasureRebalanceOffset(final Buffer buffer, final int virtualSize, final int foundAt1, final int foundAt2,
            final int adjustmentForNewEbc, final JoinPolicy policy) {

        int joinBest = 0;
        int joinOffset = 0;
        int leftSize = 0;

        //
        // Working variables
        //
        int kbData; // content of a key block
        int tbData; // content of a tail block header
        int tail; // offset within page of a tail block
        int klength; // length of the key portion of a tail block
        //
        // Search the left page for good rebalance point.
        //
        for (int p = KEY_BLOCK_START; p < foundAt1; p += KEYBLOCK_LENGTH) {
            kbData = getInt(p);
            final int ebc = decodeKeyBlockEbc(kbData);
            tail = decodeKeyBlockTail(kbData);
            tbData = getInt(tail);
            final int size = decodeTailBlockSize(tbData);
            klength = decodeTailBlockKLength(tbData);

            final int delta = ((size + ebc + ~TAILBLOCK_MASK) & TAILBLOCK_MASK)
                    - ((size + ~TAILBLOCK_MASK) & TAILBLOCK_MASK);

            final int candidateRightSize = virtualSize - leftSize + delta;

            final int candidateLeftSize = leftSize + KEYBLOCK_LENGTH + ((klength + ~TAILBLOCK_MASK) & TAILBLOCK_MASK)
                    + _tailHeaderSize;

            final int rightKeyCount = ((buffer.getKeyBlockEnd() - foundAt2) + (foundAt1 - p)) / KEYBLOCK_LENGTH;

            final int joinFit = policy.rebalanceFit(this, buffer, p, foundAt1, foundAt2, virtualSize,
                    candidateLeftSize, candidateRightSize, _bufferSize - KEY_BLOCK_START);

            if (joinFit > joinBest && rightKeyCount < _pool.getMaxKeys()) {
                joinBest = joinFit;
                joinOffset = p;
            }

            leftSize += KEYBLOCK_LENGTH + ((size + ~TAILBLOCK_MASK) & TAILBLOCK_MASK);
        }

        /*
         * Search the right page for good rebalance point.
         */
        for (int p = foundAt2; p < buffer._keyBlockEnd; p += KEYBLOCK_LENGTH) {
            kbData = buffer.getInt(p);
            final int ebc = decodeKeyBlockEbc(kbData);
            tail = decodeKeyBlockTail(kbData);
            tbData = buffer.getInt(tail);
            final int size = decodeTailBlockSize(tbData);
            klength = decodeTailBlockKLength(tbData);

            /*
             * This is the amount by which the tail block for the candidate
             * rebalance key would have to grow if it became the first key on
             * the right page and its ebc became zero.
             */
            final int delta = ((size + ebc + ~TAILBLOCK_MASK) & TAILBLOCK_MASK)
                    - ((size + ~TAILBLOCK_MASK) & TAILBLOCK_MASK);

            /*
             * Amount by which the current tail block needs to grow to
             * accommodate reduced ebc.
             */
            final int adjustment = (p == foundAt2) ? adjustmentForNewEbc : 0;

            final int candidateRightSize = virtualSize - leftSize + delta;

            final int candidateLeftSize = leftSize + ((klength + ~TAILBLOCK_MASK) & TAILBLOCK_MASK) + adjustment
                    + _tailHeaderSize + KEYBLOCK_LENGTH;

            final int leftKeyCount = ((foundAt1 - KEY_BLOCK_START) + (p - foundAt2)) / KEYBLOCK_LENGTH;

            final int joinFit = policy.rebalanceFit(this, buffer, p, foundAt1, foundAt2, virtualSize,
                    candidateLeftSize, candidateRightSize, _bufferSize - KEY_BLOCK_START);

            if (joinFit > joinBest && leftKeyCount < _pool.getMaxKeys()) {
                joinBest = joinFit;
                joinOffset = -p;
            }

            leftSize += KEYBLOCK_LENGTH + ((size + ~TAILBLOCK_MASK) & TAILBLOCK_MASK) + adjustment;
        }
        return joinOffset;

    }

    synchronized void invalidateFastIndex() {
        _fastIndex.invalidate();
    }

    synchronized FastIndex getFastIndex() {
        if (!_fastIndex.isValid()) {
            _fastIndex.recompute();
        }
        return _fastIndex;
    }

    private void reduceEbc(final int p, final int newEbc, final byte[] indexKeyBytes) {
        int kbData = getInt(p);
        final int oldDb = decodeKeyBlockDb(kbData);
        final int oldEbc = decodeKeyBlockEbc(kbData);
        int tail = decodeKeyBlockTail(kbData);
        final int tbData = getInt(tail);
        int size = decodeTailBlockSize(tbData);
        int klength = decodeTailBlockKLength(tbData);

        if (newEbc == oldEbc)
            return;
        if (newEbc > oldEbc) {
            throw new IllegalArgumentException("newEbc=" + newEbc + " must be less than oldEbc=" + oldEbc);
        }

        final int delta = ((size + oldEbc - newEbc + ~TAILBLOCK_MASK) & TAILBLOCK_MASK)
                - ((size + ~TAILBLOCK_MASK) & TAILBLOCK_MASK);
        int newTail;
        boolean wedged = false;

        if (delta == 0) {
            newTail = tail;
        } else {
            newTail = allocTail(size + delta);
            if (newTail == -1) {
                newTail = wedgeTail(tail, delta);
                if (newTail == -1) {
                    repack();
                    //
                    // Repacking will move the tail blocks so we need
                    // to refetch pointers to the ones we care about.
                    //
                    kbData = getInt(p);
                    tail = decodeKeyBlockTail(kbData);
                    newTail = wedgeTail(tail, delta);

                    if (newTail == -1) {
                        _persistit.fatal("Insufficient space for reduceEbc records in " + this + " at =" + p, null);
                    }
                }
                wedged = true;
            }
        }

        if (newTail != tail && isIndexPage()) {
            putInt(newTail + TAILBLOCK_POINTER, getInt(tail + TAILBLOCK_POINTER));
        }
        System.arraycopy(_bytes, tail + _tailHeaderSize, _bytes, newTail + _tailHeaderSize + oldEbc - newEbc, size
                - _tailHeaderSize);

        _bytes[newTail + _tailHeaderSize + oldEbc - newEbc - 1] = (byte) oldDb;

        System.arraycopy(indexKeyBytes, newEbc + 1, _bytes, newTail + _tailHeaderSize, oldEbc - newEbc - 1);

        if (newTail != tail && !wedged) {
            deallocTail(tail, size);
        }

        size += oldEbc - newEbc;
        klength += oldEbc - newEbc;
        final int newDb = indexKeyBytes[newEbc] & 0xFF;

        putInt(newTail, encodeTailBlock(size, klength));

        putInt(p, encodeKeyBlock(newEbc, newDb, newTail));
    }

    /**
     * Move records from buffer2 to this buffer. The
     *
     * @param buffer
     * @param p1
     * @param p2
     * @param insertAt
     * @param includesRightEdge
     */
    void moveRecords(final Buffer buffer, final int p1, final int p2, int insertAt, final boolean includesRightEdge) {
        if (p2 - p1 + _keyBlockEnd > _alloc) {
            repack();
        }
        if (insertAt < _keyBlockEnd) {
            System.arraycopy(_bytes, insertAt, _bytes, insertAt + p2 - p1, _keyBlockEnd - insertAt);

        }
        clearBytes(insertAt, insertAt + p2 - p1);

        setKeyBlockEnd(getKeyBlockEnd() + p2 - p1);
        if (includesRightEdge)
            setKeyBlockEnd(getKeyBlockEnd() + KEYBLOCK_LENGTH);

        for (int p = p1; p < p2 || includesRightEdge && p == p2; p += KEYBLOCK_LENGTH) {
            final int kbData = buffer.getInt(p);
            final int ebc = decodeKeyBlockEbc(kbData);
            final int db = decodeKeyBlockDb(kbData);
            final int tail = decodeKeyBlockTail(kbData);
            final int tbData = buffer.getInt(tail);
            final int size = decodeTailBlockSize(tbData);
            final int klength = decodeTailBlockKLength(tbData);
            int newSize = size;
            final boolean edgeCase = includesRightEdge && p == p2;
            if (edgeCase) {
                // this is just for the right edge key of the left page
                newSize = _tailHeaderSize + klength;
            }

            int newTail = allocTail(newSize);
            if (newTail == -1) {
                repack();
                newTail = allocTail(newSize);
            }

            if (newTail == -1) {
                _persistit
                        .fatal("Insufficient space to move records in " + this + "from " + buffer + " at =" + p, null);
            }

            System.arraycopy(buffer._bytes, tail + 4, _bytes, newTail + 4, newSize - 4);

            putInt(newTail, encodeTailBlock(newSize, klength));

            if (edgeCase && isIndexPage()) {
                putInt(newTail + TAILBLOCK_POINTER, -1);
            }

            putInt(insertAt, encodeKeyBlock(ebc, db, newTail));
            insertAt += KEYBLOCK_LENGTH;

            if (!edgeCase) {
                //
                // Remove the record from the source page.
                //
                buffer.deallocTail(tail, size);
                buffer.putInt(p, 0);
            }
        }
    }

    /**
     * Allocates a tail block of the specified size and returns its offset. Note
     * that if this method cannot allocate sufficient space without repacking,
     * it returns -1. The caller can call repack() and then call allocTail
     * again. The caller is responsible for repacking because it may need to
     * perform various other actions if tail blocks move.
     *
     * @param size
     *            Size of the required tail block
     * @return Offset to the allocated tail block, or -1 if there is not
     *         sufficient free space.
     */
    private int allocTail(int size) {
        size = (size + ~TAILBLOCK_MASK) & TAILBLOCK_MASK;
        final int alloc = _alloc - size;
        if (alloc >= _keyBlockEnd) {
            _alloc = alloc;
            return alloc;
        } else {
            return -1;
        }
    }

    private void deallocTail(final int tail, int size) {
        size = (size + ~TAILBLOCK_MASK) & TAILBLOCK_MASK;

        Debug.$assert0.t((size > 0 && size <= _bufferSize - _alloc) && (tail >= _alloc && tail < _bufferSize)
                && (tail + size <= _bufferSize));

        if (tail == _alloc) {
            //
            // If we are deallocating the lower tail block, then aggregate
            // any free space above this block.
            //
            while (tail + size < _bufferSize) {
                final int kbNext = getInt(tail + size);
                if ((kbNext & TAILBLOCK_INUSE_MASK) == 0) {
                    final int sizeNext = decodeTailBlockSize(kbNext);
                    Debug.$assert0.t((sizeNext & ~TAILBLOCK_MASK) == 0 && sizeNext != 0);
                    _slack -= sizeNext;
                    putInt(tail + size, 0);
                    size += sizeNext;
                } else
                    break;
            }
            _alloc += size;
        }

        else {
            putInt(tail, encodeFreeBlock(size));
            _slack += size;
        }
    }

    /**
     * Repacks the tail blocks so that they are contiguous.
     */
    private void repack() {
        Debug.$assert0.t(isOwnedAsWriterByMe());

        final int[] plan = getRepackPlanBuffer();
        //
        // Phase 1:
        // For each allocated tail block, post the offset of its
        // left sibling and the count of free bytes between the left
        // sibling and this block.
        //
        int free = 0;
        int back = 0;
        for (int tail = _alloc; tail < _bufferSize;) {
            final int tbData = getInt(tail);

            final int size = (decodeTailBlockSize(tbData) + ~TAILBLOCK_MASK) & TAILBLOCK_MASK;
            if (size <= 0) {
                _persistit.fatal("Buffer has invalid tailblock length " + size + " at " + tail + " in " + this, null);
            }

            if ((tbData & TAILBLOCK_INUSE_MASK) != 0) {
                plan[tail / TAILBLOCK_FACTOR] = (back << 16) | free;
                free = 0;
                back = tail;
            } else {
                free += size;
            }
            tail += size;
        }
        // Phase 2:
        // Move each tailblock rightward by the accumulated free space
        // that has been removed. Update the plan to represent the
        // shift distance for each allocated block.
        //
        int alloc = _bufferSize;
        int moveFrom = 0;
        int moveSize = 0;

        for (int tail = back; tail != 0;) {
            moveFrom = tail;
            if (free > 0)
                moveSize += alloc - tail - free;
            alloc = tail + free;
            final int planData = plan[tail / TAILBLOCK_FACTOR];
            plan[tail / TAILBLOCK_FACTOR] = free + tail;
            final int deltaFree = planData & 0xFFFF;
            if (deltaFree > 0 && moveSize > 0 && free > 0) {
                System.arraycopy(_bytes, moveFrom, _bytes, moveFrom + free, moveSize);
                moveSize = 0;
            }

            free += deltaFree;
            tail = planData >>> 16;
        }
        if (moveSize > 0 && free > 0) {
            System.arraycopy(_bytes, moveFrom, _bytes, moveFrom + free, moveSize);
        }
        _alloc = alloc;
        _slack = 0;
        //
        // Phase 3:
        // Fix up the key blocks.
        //
        if (free > 0) {
            for (int p = KEY_BLOCK_START; p < _keyBlockEnd; p += KEYBLOCK_LENGTH) {
                final int kbData = getInt(p);
                //
                // For certain remove operations, we may have invalid keyblocks
                // in range. We can safely ignore them here.
                //
                if (kbData != 0) {
                    final int tail = decodeKeyBlockTail(kbData);
                    final int newTail = plan[tail / KEYBLOCK_LENGTH];

                    if (newTail != tail) {
                        putInt(p, encodeKeyBlockTail(kbData, newTail));
                    }
                }
            }
        }
    }

    /**
     * Determines whether it is possible to allocate a new tailblock with a
     * specified size.
     *
     * @param needed
     *            The size required by the proposed new tailblock.
     * @return boolean <i>true</i> if it will fit, else <i>false</i>.
     */
    private boolean willFit(final int needed) {
        return needed <= (_alloc - _keyBlockEnd + _slack);
    }

    /**
     * Determines whether supplied index points to the right of the last key in
     * the page.
     *
     * @param foundAt
     *            The keyblock index
     */
    boolean isAfterRightEdge(final int foundAt) {
        final int p = foundAt & P_MASK;
        return (p >= _keyBlockEnd) || ((p == _keyBlockEnd - KEYBLOCK_LENGTH && (foundAt & EXACT_MASK) != 0));
    }

    /**
     * Determines whether supplied index points to the left of the first key in
     * the page.
     *
     * @param foundAt
     */
    boolean isBeforeLeftEdge(final int foundAt) {
        return (((foundAt & EXACT_MASK) == 0 && (foundAt & P_MASK) <= KEY_BLOCK_START) || (foundAt & P_MASK) < KEY_BLOCK_START);
    }

    // ------------------------------------------------------------------------
    byte[] getBytes() {
        return _bytes;
    }

    int getByte(final int index) {
        return (_bytes[index] & 0xFF);
    }

    int getChar(final int index) {
        if (Persistit.BIG_ENDIAN) {
            return (_bytes[index + 1] & 0xFF) | (_bytes[index] & 0xFF) << 8;
        } else {
            return (_bytes[index] & 0xFF) | (_bytes[index + 1] & 0xFF) << 8;
        }
    }

    int getInt(final int index) {
        if (Persistit.BIG_ENDIAN) {
            return (_bytes[index + 3] & 0xFF) | (_bytes[index + 2] & 0xFF) << 8 | (_bytes[index + 1] & 0xFF) << 16
                    | (_bytes[index] & 0xFF) << 24;
        } else {
            return (_bytes[index] & 0xFF) | (_bytes[index + 1] & 0xFF) << 8 | (_bytes[index + 2] & 0xFF) << 16
                    | (_bytes[index + 3] & 0xFF) << 24;
        }
    }

    long getLong(final int index) {
        if (Persistit.BIG_ENDIAN) {
            return _bytes[index + 7] & 0xFF | (long) (_bytes[index + 6] & 0xFF) << 8
                    | (long) (_bytes[index + 5] & 0xFF) << 16 | (long) (_bytes[index + 4] & 0xFF) << 24
                    | (long) (_bytes[index + 3] & 0xFF) << 32 | (long) (_bytes[index + 2] & 0xFF) << 40
                    | (long) (_bytes[index + 1] & 0xFF) << 48 | (long) (_bytes[index] & 0xFF) << 56;
        } else {
            return _bytes[index] & 0xFF | (long) (_bytes[index + 1] & 0xFF) << 8
                    | (long) (_bytes[index + 2] & 0xFF) << 16 | (long) (_bytes[index + 3] & 0xFF) << 24
                    | (long) (_bytes[index + 4] & 0xFF) << 32 | (long) (_bytes[index + 5] & 0xFF) << 40
                    | (long) (_bytes[index + 6] & 0xFF) << 48 | (long) (_bytes[index + 7] & 0xFF) << 56;
        }
    }

    int getDb(final int index) {
        if (Persistit.BIG_ENDIAN) {
            return _bytes[index + 3] & 0xFF;
        } else {
            return _bytes[index] & 0xFF;
        }
    }

    void putByte(final int index, final int value) {
        Debug.$assert0.t(index >= 0 && index + 1 <= _bytes.length);
        _bytes[index] = (byte) (value);
    }

    void putChar(final int index, final int value) {
        Debug.$assert0.t(index >= 0 && index + 2 <= _bytes.length);
        if (Persistit.BIG_ENDIAN) {
            _bytes[index + 1] = (byte) (value);
            _bytes[index] = (byte) (value >>> 8);
        } else {
            _bytes[index] = (byte) (value);
            _bytes[index + 1] = (byte) (value >>> 8);
        }
    }

    void putInt(final int index, final int value) {
        Debug.$assert0.t(index >= 0 && index + 4 <= _bytes.length);
        if (Persistit.BIG_ENDIAN) {
            _bytes[index + 3] = (byte) (value);
            _bytes[index + 2] = (byte) (value >>> 8);
            _bytes[index + 1] = (byte) (value >>> 16);
            _bytes[index] = (byte) (value >>> 24);
        } else {
            _bytes[index] = (byte) (value);
            _bytes[index + 1] = (byte) (value >>> 8);
            _bytes[index + 2] = (byte) (value >>> 16);
            _bytes[index + 3] = (byte) (value >>> 24);
        }
    }

    void putLong(final int index, final long value) {
        Debug.$assert0.t(index >= 0 && index + 8 <= _bytes.length);

        if (Persistit.BIG_ENDIAN) {
            _bytes[index + 7] = (byte) (value);
            _bytes[index + 6] = (byte) (value >>> 8);
            _bytes[index + 5] = (byte) (value >>> 16);
            _bytes[index + 4] = (byte) (value >>> 24);
            _bytes[index + 3] = (byte) (value >>> 32);
            _bytes[index + 2] = (byte) (value >>> 40);
            _bytes[index + 1] = (byte) (value >>> 48);
            _bytes[index] = (byte) (value >>> 56);
        } else {
            _bytes[index] = (byte) (value);
            _bytes[index + 1] = (byte) (value >>> 8);
            _bytes[index + 2] = (byte) (value >>> 16);
            _bytes[index + 3] = (byte) (value >>> 24);
            _bytes[index + 4] = (byte) (value >>> 32);
            _bytes[index + 5] = (byte) (value >>> 40);
            _bytes[index + 6] = (byte) (value >>> 48);
            _bytes[index + 7] = (byte) (value >>> 56);
        }
    }

    static void writeLongRecordDescriptor(final byte[] bytes, final int size, final long pageAddr) {
        if (bytes.length != LONGREC_SIZE) {
            throw new IllegalArgumentException("Bad LONG_RECORD descriptor size: " + size);
        }
        bytes[0] = (byte) LONGREC_TYPE;
        bytes[1] = (byte) 0;
        Util.putChar(bytes, LONGREC_PREFIX_SIZE_OFFSET, LONGREC_PREFIX_SIZE);
        Util.putLong(bytes, LONGREC_SIZE_OFFSET, size);
        Util.putLong(bytes, LONGREC_PAGE_OFFSET, pageAddr);
    }

    static int decodeLongRecordDescriptorSize(final byte[] bytes, final int offset) {
        int type;
        if ((type = (bytes[offset] & 0xFF)) != LONGREC_TYPE) {
            throw new IllegalArgumentException("Bad LONG_RECORD descriptor type: " + type);
        }
        return (int) Util.getLong(bytes, offset + LONGREC_SIZE_OFFSET);
    }

    static long decodeLongRecordDescriptorPointer(final byte[] bytes, final int offset) {
        int type;
        if ((type = (bytes[offset] & 0xFF)) != LONGREC_TYPE) {
            throw new IllegalArgumentException("Bad LONG_RECORD descriptor type: " + type);
        }
        return Util.getLong(bytes, offset + LONGREC_PAGE_OFFSET);
    }

    static int bufferSizeWithOverhead(final int bufferSize) {
        final int fastIndexSize = ((bufferSize - HEADER_SIZE) / MAX_KEY_RATIO) * FastIndex.BYTES_PER_ENTRY;
        return bufferSize + fastIndexSize + ESTIMATED_FIXED_BUFFER_OVERHEAD;
    }

    /**
     * @return <i>true</i> if the current page is unallocated
     */
    public boolean isUnallocatedPage() {
        return _type == PAGE_TYPE_UNALLOCATED;
    }

    /**
     * @return <i>true</i> if the current page is a data page
     */
    public boolean isDataPage() {
        return _type == PAGE_TYPE_DATA;
    }

    /**
     * @return <i>true</i> if the current page is an index page
     */
    public boolean isIndexPage() {
        return _type >= PAGE_TYPE_INDEX_MIN && _type <= PAGE_TYPE_INDEX_MAX;
    }

    /**
     * @return <i>true</i> if the current page is a garbage page
     */
    public boolean isGarbagePage() {
        return _type == PAGE_TYPE_GARBAGE;
    }

    /**
     * @return <i>true</i> if the current page is the head page of a
     *         {@link Volume}
     */
    public boolean isHeadPage() {
        return _type == PAGE_TYPE_HEAD;
    }

    /**
     * @return <i>true</i> if the current page is a long record page
     */
    public boolean isLongRecordPage() {
        return _type == PAGE_TYPE_LONG_RECORD;
    }

    static int tailBlockSize(final int size) {
        return (size + ~TAILBLOCK_MASK) & TAILBLOCK_MASK;
    }

    static int encodeKeyBlock(final int ebc, final int db, final int tail) {
        return ((ebc << EBC_SHIFT) & EBC_MASK) | ((db /* << DB_SHIFT */) & DB_MASK)
                | ((tail << TAIL_SHIFT) & TAIL_MASK);
    }

    static int encodeKeyBlockTail(final int kbData, final int tail) {
        return (kbData & ~TAIL_MASK) | ((tail << TAIL_SHIFT) & TAIL_MASK);
    }

    static int encodeTailBlock(final int size, final int klength) {
        return ((klength << TAILBLOCK_KLENGTH_SHIFT) & TAILBLOCK_KLENGTH_MASK) | ((size /*
                                                                                         * <<
                                                                                         * TAILBLOCK_SIZE_SHIFT
                                                                                         */) & TAILBLOCK_SIZE_MASK)
                | TAILBLOCK_INUSE_MASK;
    }

    static int encodeFreeBlock(final int size) {
        return ((size /* << TAILBLOCK_SIZE_SHIFT */) & TAILBLOCK_SIZE_MASK);
    }

    static int decodeKeyBlockEbc(final int kbData) {
        return (kbData & EBC_MASK) >>> EBC_SHIFT;
    }

    static int decodeKeyBlockDb(final int kbData) {
        return (kbData & DB_MASK) /* >>> DB_SHIFT */;
    }

    static int decodeKeyBlockTail(final int kbData) {
        return (kbData & TAIL_MASK) >>> TAIL_SHIFT;
    }

    static int decodeTailBlockSize(final int tbData) {
        return (tbData & TAILBLOCK_SIZE_MASK) /* >>> TAILBLOCK_SIZE_SHIFT */;
    }

    static int decodeTailBlockKLength(final int tbData) {
        return (tbData & TAILBLOCK_KLENGTH_MASK) >>> TAILBLOCK_KLENGTH_SHIFT;
    }

    static boolean decodeTailBlockInUse(final int tbData) {
        return (tbData & TAILBLOCK_INUSE_MASK) != 0;
    }

    static int decodeDepth(final int foundAt) {
        return (foundAt & DEPTH_MASK) >>> DEPTH_SHIFT;
    }

    final int[] getRepackPlanBuffer() {
        return _persistit.getThreadLocalIntArray(MAX_BUFFER_SIZE / TAILBLOCK_FACTOR);
    }

    PersistitException verify(Key key, final VerifyVisitor visitor) {
        try {
            if (_page == 0) {
                return new InvalidPageStructureException("head page is neither a data page nor an index page");
            }
            if (!isIndexPage() && !isDataPage()) {
                return new InvalidPageStructureException("page type " + _type
                        + " is neither data page nor an index page");
            }
            if (key == null) {
                key = new Key(_persistit);
            }

            final byte[] kb = key.getEncodedBytes();
            final int[] plan = getRepackPlanBuffer();
            for (int index = 0; index < plan.length; index++) {
                plan[index] = 0;
            }

            if (visitor != null) {
                visitor.visitPage(getTimestamp(), getVolume(), getPageAddress(), getPageType(), getBufferSize(),
                        getKeyBlockStart(), getKeyBlockEnd(), getAlloc(), getAvailableSize(), getRightSibling());
            }

            for (int p = KEY_BLOCK_START; p < _keyBlockEnd; p += KEYBLOCK_LENGTH) {
                final int kbData = getInt(p);
                final int db = decodeKeyBlockDb(kbData);
                final int ebc = decodeKeyBlockEbc(kbData);
                final int tail = decodeKeyBlockTail(kbData);

                if (p == KEY_BLOCK_START && ebc != 0) {
                    return new InvalidPageStructureException("invalid initial ebc " + ebc + " for keyblock at " + p
                            + " --[" + summarize() + "]");
                }

                if (tail < _keyBlockEnd || tail < _alloc || tail > _bufferSize - _tailHeaderSize
                        || (tail & ~TAILBLOCK_MASK) != 0) {
                    return new InvalidPageStructureException("invalid tail block offset " + tail + " for keyblock at "
                            + p + " --[" + summarize() + "]");
                }
                final int tbData = getInt(tail);
                final int klength = decodeTailBlockKLength(tbData);
                if ((tbData & TAILBLOCK_INUSE_MASK) == 0) {
                    return new InvalidPageStructureException("not in-use tail block offset " + tail
                            + " for keyblock at " + p + " --[" + summarize() + "]");
                }
                // Verify that first key in this pages matches the final key
                // of the preceding page.
                if (p == KEY_BLOCK_START && key.getEncodedSize() != 0) {
                    int index = 0;
                    int compare = 0;
                    int size = key.getEncodedSize();
                    if (klength < size)
                        size = klength + 1;
                    compare = (kb[0] & 0xFF) - db;
                    while (compare == 0 && ++index < size) {
                        compare = (kb[index] & 0xFF) - (_bytes[tail + _tailHeaderSize + index - 1] & 0xFF);
                    }
                    if (compare != 0) {
                        final String s = compare < 0 ? "too big" : "too small";
                        return new InvalidPageStructureException("initial key " + s + " at offset " + index
                                + " for keyblock at " + p + " --[" + summarize() + "]");
                    }
                }
                // Verify that successor keys follow in sequence.
                if (p > KEY_BLOCK_START && ebc < key.getEncodedSize()) {
                    final int dbPrev = kb[ebc] & 0xFF;
                    if (db < dbPrev) {
                        return new InvalidPageStructureException("db not greater: db=" + db + " dbPrev=" + dbPrev
                                + " for keyblock at " + p + " --[" + summarize() + "]");
                    }
                }
                //
                // If this is an index page, make sure the pointer isn't
                // redundant
                //
                if (isIndexPage()) {
                    final int pointer = getInt(tail + 4);
                    if (visitor != null) {
                        visitor.visitIndexRecord(key, p, tail, klength, pointer);
                    }
                    if (pointer == -1) {
                        if (p + KEYBLOCK_LENGTH != _keyBlockEnd) {
                            return new InvalidPageStructureException("index pointer has pointer to -1 "
                                    + " for keyblock at " + p + " --[" + summarize() + "]");
                        }
                    }
                } else if (isDataPage()) {
                    final int size = decodeTailBlockSize(tbData);
                    final int offset = tail + _tailHeaderSize + klength;
                    final int length = size - klength - _tailHeaderSize;
                    if (visitor != null) {
                        visitor.visitDataRecord(key, p, tail, klength, offset, length, getBytes());
                    }

                    if (!MVV.verify(_bytes, offset, length)) {
                        throw new InvalidPageStructureException("invalid MVV record at offset/length=" + offset + "/"
                                + length);
                    }

                }

                if (_pool != null && getKeyCount() > _pool.getMaxKeys()) {
                    return new InvalidPageStructureException("page has too many keys: has " + getKeyCount()
                            + " but max is " + _pool.getMaxKeys());
                }

                kb[ebc] = (byte) db;
                System.arraycopy(_bytes, tail + _tailHeaderSize, kb, ebc + 1, klength);
                key.setEncodedSize(ebc + klength + 1);
                plan[tail / TAILBLOCK_FACTOR] = p;
            }

            // Now check the free blocks

            final int formerBlock = _alloc;
            for (int tail = _alloc; tail < _bufferSize;) {
                if ((tail & ~TAILBLOCK_MASK) != 0 || tail < 0 || tail > _bufferSize) {
                    return new InvalidPageStructureException("Tail block at " + formerBlock + " is invalid");
                }
                final int tbData = getInt(tail);
                final int size = decodeTailBlockSize(tbData);
                if (size <= ~TAILBLOCK_MASK || size >= _bufferSize - _keyBlockEnd) {
                    return new InvalidPageStructureException("Tailblock at " + tail + " has invalid size=" + size);
                }
                if ((tbData & TAILBLOCK_INUSE_MASK) != 0) {
                    if (plan[tail / TAILBLOCK_FACTOR] == 0) {
                        return new InvalidPageStructureException("Tailblock at " + tail + " is in use, but no key "
                                + " block points to it.");
                    }
                    final int klength = decodeTailBlockKLength(tbData);
                    {
                        if (klength + _tailHeaderSize > size) {
                            return new InvalidPageStructureException("Tailblock at " + tail + " has klength=" + klength
                                    + " longer than size=" + size + " - headerSize=" + _tailHeaderSize);
                        }
                    }
                } else {
                    if (plan[tail / TAILBLOCK_FACTOR] != 0) {
                        return new InvalidPageStructureException("Tailblock at " + tail + " is marked free, but the "
                                + " key block at " + plan[tail / TAILBLOCK_FACTOR] + " points to it.");
                    }
                }
                tail += ((size + ~TAILBLOCK_MASK) & TAILBLOCK_MASK);
            }
            return null;
        } catch (final PersistitException pe) {
            return pe;
        }

    }

    /**
     * For each MVV record in a data page, attempt to prune it to remove
     * obsolete versions. Note that also this process modifies the content of
     * the buffer, the buffer remains logically identical. Therefore it is not
     * necessary if the Buffer is already dirty to call
     * {@link #writePageOnCheckpoint(long)}; in other words, the results of
     * pruning the page can be saved with the preceding checkpoint even though a
     * new checkpoint has been proposed.
     *
     * @param tree
     * @param spareKey
     * @return <code>true</code> if this method changed the contents of the
     *         buffer
     * @throws PersistitException
     */
    boolean pruneMvvValues(final Tree tree, final boolean pruneLongMVVs, final List<CleanupAction> cleanupActions)
            throws PersistitException {
        boolean changed = false;
        try {
            boolean hasLongMvvRecords = false;

            if (!isOwnedAsWriterByMe()) {
                throw new IllegalStateException("Exclusive claim required " + this);
            }
            if (isDataPage() && _mvvCount != 0) {
                final long timestamp = _persistit.getTimestampAllocator().updateTimestamp();
                _mvvCount = 0;
                writePageOnCheckpoint(timestamp);
                final int flags = pruneMvvValuesHelper(tree, cleanupActions);
                changed = (flags & PRUNE_MVV_HELPER_CHANGED) != 0;
                hasLongMvvRecords = (flags & PRUNE_MVV_HELPER_HAS_LONG) != 0;

                if (changed) {
                    setDirtyAtTimestamp(timestamp);
                }

                final List<PrunedVersion> prunedVersions = new ArrayList<PrunedVersion>();

                if (pruneLongMVVs && hasLongMvvRecords) {
                    final List<PersistitException> deferredExceptions = new ArrayList<PersistitException>();
                    final List<Long> oldChainsToDeallocate = new ArrayList<Long>();

                    final Buffer copy = new Buffer(this);
                    final boolean copyChanged = copy.pruneLongMvvValues(tree, prunedVersions, deferredExceptions,
                            oldChainsToDeallocate, cleanupActions);
                    if (copyChanged) {
                        changed = true;
                        final long copyTimestamp = _persistit.getTimestampAllocator().updateTimestamp();
                        writePageOnCheckpoint(copyTimestamp);
                        System.arraycopy(copy._bytes, 0, _bytes, 0, _bufferSize);
                        _alloc = copy._alloc;
                        _slack = copy._slack;
                        _mvvCount = copy._mvvCount;
                        if (_keyBlockEnd != copy._keyBlockEnd) {
                            _keyBlockEnd = copy._keyBlockEnd;
                            invalidateFastIndex();
                        }
                        setDirtyAtTimestamp(copyTimestamp);
                        deallocatePrunedVersions(_persistit, _vol, prunedVersions);
                        for (final Long oldLongRecordChain : oldChainsToDeallocate) {
                            _vol.getStructure().deallocateGarbageChain(oldLongRecordChain, 0);
                        }
                    }
                    if (!deferredExceptions.isEmpty()) {
                        _mvvCount++;
                        throw deferredExceptions.get(0);
                    }
                }
            }

        } finally {
            if (Debug.ENABLED && changed) {
                assertVerify();
            }
        }

        return changed;
    }

    private int pruneMvvValuesHelper(final Tree tree, final List<CleanupAction> cleanupActions)
            throws PersistitException {
        boolean changed = false;
        boolean hasLongMvvRecords = false;
        final List<PrunedVersion> prunedVersions = new ArrayList<PrunedVersion>();

        for (int p = KEY_BLOCK_START; p < _keyBlockEnd; p += KEYBLOCK_LENGTH) {
            final int kbData = getInt(p);
            final int tail = decodeKeyBlockTail(kbData);
            final int tbData = getInt(tail);
            final int klength = decodeTailBlockKLength(tbData);
            final int oldTailSize = decodeTailBlockSize(tbData);
            final int offset = tail + _tailHeaderSize + klength;
            final int oldSize = oldTailSize - klength - _tailHeaderSize;

            if (oldSize > 0) {
                int valueByte = _bytes[offset] & 0xFF;
                if (isLongMVV(_bytes, offset, oldSize)) {
                    /*
                     * Can't prune in this pass because of long record timestamp
                     * management. Simply remember that there are long records
                     * to prune, then prune them in a copy of the buffer.
                     */
                    hasLongMvvRecords = true;
                }
                if (valueByte == MVV.TYPE_MVV) {
                    final int newSize = MVV.prune(_bytes, offset, oldSize, _persistit.getTransactionIndex(), true,
                            prunedVersions);
                    if (newSize != oldSize) {
                        changed = true;
                        final int newTailSize = klength + newSize + _tailHeaderSize;
                        final int oldNext = (tail + oldTailSize + ~TAILBLOCK_MASK) & TAILBLOCK_MASK;
                        final int newNext = (tail + newTailSize + ~TAILBLOCK_MASK) & TAILBLOCK_MASK;
                        if (newNext < oldNext) {
                            // Free the remainder of the old tail block
                            deallocTail(newNext, oldNext - newNext);
                        } else {
                            Debug.$assert0.t(newNext == oldNext);
                        }
                        // Rewrite the tail block header
                        putInt(tail, encodeTailBlock(newTailSize, klength));
                        Debug.$assert0.t(MVV.verify(_bytes, offset, newSize));
                    }
                    valueByte = newSize > 0 ? _bytes[offset] & 0xFF : -1;
                    incCountIfMvv(_bytes, offset, newSize);
                }

                if (pruneAntiValue(valueByte, p, tree, cleanupActions)) {
                    changed = true;
                    p -= KEYBLOCK_LENGTH;
                }
            }
        }
        deallocatePrunedVersions(_persistit, _vol, prunedVersions);
        prunedVersions.clear();
        return (changed ? PRUNE_MVV_HELPER_CHANGED : 0) | (hasLongMvvRecords ? PRUNE_MVV_HELPER_HAS_LONG : 0);
    }

    /**
     * This method attempts to prune each LongRecord MVVs found in the page.
     * This method is performed on a _copy_ of the original Buffer being pruned;
     * the bytes of the copy are then copied back to the original buffer upon
     * successful completion of this process.
     */
    private boolean pruneLongMvvValues(final Tree tree, final List<PrunedVersion> prunedVersions,
            final List<PersistitException> deferredExceptions, final List<Long> toDeallocate,
            final List<CleanupAction> cleanupActions) {

        boolean changed = false;
        for (int p = KEY_BLOCK_START; p < _keyBlockEnd; p += KEYBLOCK_LENGTH) {
            final int kbData = getInt(p);
            final int tail = decodeKeyBlockTail(kbData);
            final int tbData = getInt(tail);
            final int klength = decodeTailBlockKLength(tbData);
            final int oldTailSize = decodeTailBlockSize(tbData);
            final int offset = tail + _tailHeaderSize + klength;
            final int oldSize = oldTailSize - klength - _tailHeaderSize;

            if (oldSize > 0) {
                int valueByte = _bytes[offset] & 0xFF;
                if (isLongMVV(_bytes, offset, oldSize)) {
                    final Value value = _persistit.getThreadLocalValue();
                    boolean pruned = false;
                    try {
                        pruned = pruneLongMvv(_bytes, offset, oldSize, value, prunedVersions, toDeallocate);
                    } catch (final PersistitException pe) {
                        deferredExceptions.add(pe);
                    }
                    if (pruned) {
                        changed = true;
                        final int newSize = value.getEncodedSize();
                        assert newSize <= oldSize : "Pruned long value overflow";
                        System.arraycopy(value.getEncodedBytes(), 0, _bytes, offset, newSize);
                        final int newTailSize = klength + newSize + _tailHeaderSize;
                        final int oldNext = (tail + oldTailSize + ~TAILBLOCK_MASK) & TAILBLOCK_MASK;
                        final int newNext = (tail + newTailSize + ~TAILBLOCK_MASK) & TAILBLOCK_MASK;
                        if (newNext < oldNext) {
                            // Free the remainder of the old tail block
                            deallocTail(newNext, oldNext - newNext);
                        } else {
                            Debug.$assert0.t(newNext == oldNext);
                        }
                        // Rewrite the tail block header
                        putInt(tail, encodeTailBlock(newTailSize, klength));
                        valueByte = newSize > 0 ? _bytes[offset] & 0xFF : -1;
                        value.changeLongRecordMode(false);
                    }
                }
                if (pruneAntiValue(valueByte, p, tree, cleanupActions)) {
                    changed = true;
                    p -= KEYBLOCK_LENGTH;
                }
            }
        }

        if (Debug.ENABLED && changed) {
            assertVerify();
        }
        return changed;
    }

    private boolean pruneAntiValue(final int valueByte, final int p, final Tree tree,
            final List<CleanupAction> cleanupActions) {
        if (valueByte == MVV.TYPE_ANTIVALUE) {
            if (p == KEY_BLOCK_START) {
                if (tree != null) {
                    final int treeHandle = tree.getHandle();
                    assert treeHandle != 0 : "MVV found in a temporary tree " + tree;
                    if (cleanupActions != null) {
                        cleanupActions.add(new CleanupAntiValue(treeHandle, getPageAddress()));
                    } else if (!_enqueuedForAntiValuePruning) {
                        if (_persistit.getCleanupManager().offer(new CleanupAntiValue(treeHandle, getPageAddress()))) {
                            _enqueuedForAntiValuePruning = true;
                        }
                    }
                } else {
                    _mvvCount++;
                }
            } else if (p == _keyBlockEnd - KEYBLOCK_LENGTH) {
                Debug.$assert1.t(false);
            } else {
                final boolean removed = removeKeys(p | EXACT_MASK, p | EXACT_MASK, _persistit.getThreadLocalKey());
                Debug.$assert0.t(removed);
                return true;
            }
        }
        return false;
    }

    private boolean pruneLongMvv(final byte[] bytes, final int offset, final int oldSize, final Value value,
            final List<PrunedVersion> prunedVersions, final List<Long> toDeallocate) throws PersistitException {
        assert isLongMVV(bytes, offset, oldSize) : "Not a long MVV";
        final long oldLongRecordChain = decodeLongRecordDescriptorPointer(bytes, offset);
        value.changeLongRecordMode(false);
        value.ensureFit(oldSize);
        System.arraycopy(bytes, offset, value.getEncodedBytes(), 0, oldSize);
        value.setEncodedSize(oldSize);
        final LongRecordHelper helper = new LongRecordHelper(_persistit, _vol);
        helper.fetchLongRecord(value, Integer.MAX_VALUE, SharedResource.DEFAULT_MAX_WAIT_TIME);
        final byte[] rawBytes = value.getEncodedBytes();
        final int oldLongSize = value.getEncodedSize();
        // TODO - perhaps remove. Done as a precaution for now.
        Debug.$assert0.t(MVV.verify(rawBytes, 0, oldLongSize));
        final List<PrunedVersion> provisionalPrunedVersions = new ArrayList<PrunedVersion>();
        final int newLongSize = MVV.prune(rawBytes, 0, oldLongSize, _persistit.getTransactionIndex(), true,
                provisionalPrunedVersions);
        if (newLongSize == oldLongSize) {
            // No pruning done.
            return false;
        }
        value.setEncodedSize(newLongSize);
        if (newLongSize > oldSize) {
            helper.storeLongRecord(value, false);
        }
        if (oldLongRecordChain != 0) {
            toDeallocate.add(oldLongRecordChain);
        }
        prunedVersions.addAll(provisionalPrunedVersions);
        return true;
    }

    /**
     * @return A displayable summary of information about the page contained in
     *         this <code>Buffer</code>.
     */
    public String summarize() {
        return String.format("Page=%,d type=%s rightSibling=%,d status=%s start=%d end=%d size=%d alloc=%d "
                + "slack=%d index=%d timestamp=%,d generation=%,d", _page, getPageTypeName(), _rightSibling,
                getStatusDisplayString(), KEY_BLOCK_START, _keyBlockEnd, _bufferSize, _alloc, _slack, getIndex(),
                getTimestamp(), getGeneration());
    }

    @Override
    public String toString() {
        if (_toStringDebug) {
            return toStringDetail();
        }
        return String.format("Page %,d in volume %s at index %,d timestamp=%,d status=%s type=%s", _page, _vol,
                _poolIndex, _timestamp, getStatusDisplayString(), getPageTypeName());
    }

    /**
     * @return a human-readable inventory of the contents of this buffer
     */
    public String toStringDetail() {
        return toStringDetail(-1, 42, 82, 0, true);
    }

    /**
     * @param findPointer
     * @return a human-readable representation of a page; if it is an index page
     *         and findPointer >= 0, then only show the records that surround
     *         the one that points to findPointer. This provides a way to
     *         quickly find pointer paths in pages.
     */
    String toStringDetail(final long findPointer, final int maxKeyDisplayLength, final int maxValueDisplayLength,
            final int contextLines, final boolean all) {
        final StringBuilder sb = new StringBuilder(String.format(
                "Page %,d in volume %s at index @%,d status %s type %s", _page, _vol, _poolIndex,
                getStatusDisplayString(), getPageTypeName()));
        if (!isValid()) {
            sb.append(" - invalid");
        } else if (isDataPage() || isIndexPage()) {
            sb.append(String.format("\n  type=%,d  alloc=%,d  slack=%,d  " + "keyBlockStart=%,d  keyBlockEnd=%,d "
                    + "timestamp=%,d generation=%,d right=%,d hash=%,d", _type, _alloc, _slack, KEY_BLOCK_START,
                    _keyBlockEnd, getTimestamp(), getGeneration(), getRightSibling(), _pool.hashIndex(_vol, _page)));
            try {
                final Key key = new Key(_persistit);
                final Value value = new Value(_persistit);
                final RecordInfo[] records = getRecords();
                int foundPointerRecord = -1;
                if (isIndexPage() && findPointer >= 0) {
                    for (int index = 0; index < records.length; index++) {
                        if (records[index].getPointerValue() == findPointer) {
                            foundPointerRecord = index;
                        }
                    }
                }
                boolean elision = false;
                for (int index = 0; index < records.length; index++) {
                    final RecordInfo r = records[index];

                    String mark = " ";
                    boolean selected = all | index < contextLines || index >= records.length - contextLines;
                    if (foundPointerRecord >= 0) {
                        selected |= (index >= foundPointerRecord - contextLines)
                                && (index <= foundPointerRecord + contextLines);
                        if (index == foundPointerRecord) {
                            mark = "*";
                        }
                    }

                    if (selected) {
                        final String keyString;
                        final String valueString;

                        if (r.getKeyState() != null) {
                            r.getKeyState().copyTo(key);
                            keyString = Util.abridge(key.toString(), maxKeyDisplayLength);
                        } else {
                            keyString = "*error*";
                        }

                        if (isDataPage() && r.getValueState() != null) {
                            r.getValueState().copyTo(value);
                            valueString = Util.abridge(value.toString(), maxValueDisplayLength);
                        } else {
                            valueString = "*error*";
                        }

                        if (elision) {
                            sb.append(String.format("\n     ..."));
                            elision = false;
                        }

                        if (isDataPage()) {
                            r.getValueState().copyTo(value);
                            sb.append(String.format("\n%s   %5d: db=%3d ebc=%3d tb=%,5d [%,d]%s=[%,d]%s", mark,
                                    r.getKbOffset(), r.getDb(), r.getEbc(), r.getTbOffset(), r.getKLength(), keyString,
                                    r.getValueState().getEncodedBytes().length, valueString));
                        } else {
                            sb.append(String.format("\n%s  %5d: db=%3d ebc=%3d tb=%,5d [%,d]%s->%,d", mark,
                                    r.getKbOffset(), r.getDb(), r.getEbc(), r.getTbOffset(), r.getKLength(), keyString,
                                    r.getPointerValue()));
                        }
                        if (r.getError() != null) {
                            sb.append(String.format(" !! %s", r.getError()));
                        }
                    } else {
                        elision = true;
                    }

                }
            } catch (final Exception e) {
                sb.append(" - " + e);
            }
        } else if (isHeadPage()) {
            sb.append(String.format("\n  type=%,d  " + "timestamp=%,d generation=%,d right=%,d hash=%,d", _type,
                    getTimestamp(), getGeneration(), getRightSibling(), _pool.hashIndex(_vol, _page)));
            sb.append(String.format("\n  nextAvailablePage=%,d extendedPageCount=%,d "
                    + " directoryRootPage=%,d garbageRootPage=%,d id=%,d ", getNextAvailablePage(_bytes),
                    getExtendedPageCount(_bytes), getDirectoryRoot(_bytes), getGarbageRoot(_bytes), getId(_bytes)));
        } else if (isGarbagePage()) {
            sb.append(String.format("\n  type=%,d  " + "timestamp=%,d generation=%,d right=%,d hash=%,d", _type,
                    getTimestamp(), getGeneration(), getRightSibling(), _pool.hashIndex(_vol, _page)));
            for (int p = _alloc; p < _bufferSize; p += GARBAGE_BLOCK_SIZE) {
                sb.append(String.format("\n  garbage chain @%,6d : %,d -> %,d", p,
                        getLong(p + GARBAGE_BLOCK_LEFT_PAGE), getLong(p + GARBAGE_BLOCK_RIGHT_PAGE)));
            }
        } else {
            sb.append(String.format("\n  type=%,d  " + "timestamp=%,d generation=%,d right=%,d hash=%,d\n", _type,
                    getTimestamp(), getGeneration(), getRightSibling(), _pool.hashIndex(_vol, _page)));
        }
        return sb.toString();
    }

    String foundAtString(int p) {
        final StringBuilder sb = new StringBuilder("<");
        sb.append(p & P_MASK);
        if ((p & EXACT_MASK) != 0)
            sb.append(":exact");
        if ((p & FIXUP_MASK) > 0)
            sb.append(":fixup");
        sb.append(":depth=");
        sb.append(decodeDepth(p));
        p = p & P_MASK;
        if (p < KEY_BLOCK_START)
            sb.append(":before");
        else if (p >= _keyBlockEnd)
            sb.append(":after");
        else if (p + KEYBLOCK_LENGTH == _keyBlockEnd)
            sb.append(":end");
        else {
            final int kbData = getInt(p);
            sb.append(":ebc=" + decodeKeyBlockEbc(kbData));
            sb.append(":db=" + decodeKeyBlockDb(kbData));
            sb.append(":tail=" + decodeKeyBlockTail(kbData));
        }
        sb.append(">");
        return sb.toString();
    }

    /**
     * @return an array of <code>Record</code>s extracted from this buffer.
     */
    public ManagementImpl.RecordInfo[] getRecords() {
        ManagementImpl.RecordInfo[] result = null;

        if (isIndexPage() || isDataPage()) {
            final Key key = new Key(_persistit);
            final Value value = new Value(_persistit);

            final int count = (_keyBlockEnd - KEY_BLOCK_START) / KEYBLOCK_LENGTH;
            result = new ManagementImpl.RecordInfo[count];
            int n = 0;
            for (int p = KEY_BLOCK_START; p < _keyBlockEnd; p += KEYBLOCK_LENGTH) {
                final ManagementImpl.RecordInfo rec = new ManagementImpl.RecordInfo();
                try {
                    rec._kbOffset = p;

                    final int kbData = getInt(p);
                    final int db = decodeKeyBlockDb(kbData);
                    final int ebc = decodeKeyBlockEbc(kbData);
                    final int tail = decodeKeyBlockTail(kbData);
                    rec._tbOffset = tail;
                    rec._ebc = ebc;
                    rec._db = db;

                    final int tbData = tail != 0 ? getInt(tail) : 0;
                    final int size = decodeTailBlockSize(tbData);
                    final int klength = decodeTailBlockKLength(tbData);
                    final boolean inUse = decodeTailBlockInUse(tbData);
                    rec._klength = klength;
                    rec._size = size;
                    rec._inUse = inUse;

                    final byte[] kbytes = key.getEncodedBytes();
                    kbytes[ebc] = (byte) db;
                    System.arraycopy(_bytes, tail + _tailHeaderSize, kbytes, ebc + 1, klength);
                    key.setEncodedSize(ebc + 1 + klength);
                    rec._key = new KeyState(key);

                    if (isIndexPage()) {
                        rec._pointerValue = getInt(tail + 4);
                    } else {
                        int vsize = size - _tailHeaderSize - klength;
                        if (vsize < 0)
                            vsize = 0;
                        value.putEncodedBytes(_bytes, tail + _tailHeaderSize + klength, vsize);
                        if (value.isDefined() && (value.getEncodedBytes()[0] & 0xFF) == LONGREC_TYPE) {
                            value.setLongRecordMode(true);
                        } else {
                            value.setLongRecordMode(false);
                        }
                        rec._value = new ValueState(value);
                    }
                } catch (final Exception e) {
                    rec._error = e.toString();
                    if (rec._key == null) {
                        rec._key = new KeyState(new Key(_persistit));
                    }
                    if (rec._value == null) {
                        rec._value = new ValueState(new Value(_persistit));
                    }
                }
                result[n++] = rec;
            }
        } else if (isGarbagePage()) {
            final int count = (_bufferSize - _alloc) / GARBAGE_BLOCK_SIZE;
            result = new ManagementImpl.RecordInfo[count];
            int n = 0;
            for (int p = _alloc; p < _bufferSize; p += GARBAGE_BLOCK_SIZE) {
                final ManagementImpl.RecordInfo rec = new ManagementImpl.RecordInfo();
                rec._tbOffset = p;
                rec._garbageStatus = getInt(p + GARBAGE_BLOCK_STATUS);
                rec._garbageLeftPage = getLong(p + GARBAGE_BLOCK_LEFT_PAGE);
                rec._garbageRightPage = getLong(p + GARBAGE_BLOCK_RIGHT_PAGE);
                result[n++] = rec;
            }
        }
        return result;
    }

    void assertVerify() {
        if (Debug.VERIFY_PAGES) {
            final Exception verifyException = verify(null, null);
            Debug.$assert1.t(verifyException == null);
        }
    }

    boolean addGarbageChain(final long left, final long right, final long expectedCount) {
        Debug.$assert0.t(left > 0 && left <= MAX_VALID_PAGE_ADDR && left != _page && right != _page && isGarbagePage());

        if (_alloc - GARBAGE_BLOCK_SIZE < _keyBlockEnd) {
            return false;
        } else {
            assert !chainIsRedundant(left) : "Attempting to add a redundate garbage chain " + left + "->" + right
                    + " to " + this;
            _alloc -= GARBAGE_BLOCK_SIZE;
            putInt(_alloc + GARBAGE_BLOCK_STATUS, 0);
            putLong(_alloc + GARBAGE_BLOCK_LEFT_PAGE, left);
            putLong(_alloc + GARBAGE_BLOCK_RIGHT_PAGE, right);
            putLong(_alloc + GARBAGE_BLOCK_EXPECTED_COUNT, expectedCount);
            bumpGeneration();
            return true;
        }
    }

    private boolean chainIsRedundant(final long left) {
        for (int p = _alloc; p < _bufferSize; p += GARBAGE_BLOCK_SIZE) {
            final long oldLeft = getGarbageChainLeftPage(p);
            if (oldLeft == left) {
                return true;
            }
        }
        return false;
    }

    int getGarbageChainStatus() {
        Debug.$assert0.t(isGarbagePage());
        if (_alloc + GARBAGE_BLOCK_SIZE > _bufferSize)
            return -1;
        else
            return getInt(_alloc + GARBAGE_BLOCK_STATUS);
    }

    long getGarbageChainLeftPage() {
        Debug.$assert0.t(isGarbagePage());
        if (_alloc + GARBAGE_BLOCK_SIZE > _bufferSize)
            return -1;
        final long page = getLong(_alloc + GARBAGE_BLOCK_LEFT_PAGE);
        Debug.$assert0.t(page > 0 && page <= MAX_VALID_PAGE_ADDR && page != _page);
        return page;
    }

    long getGarbageChainRightPage() {
        Debug.$assert1.t(isGarbagePage());
        if (_alloc + GARBAGE_BLOCK_SIZE > _bufferSize)
            return -1;
        else
            return getLong(_alloc + GARBAGE_BLOCK_RIGHT_PAGE);
    }

    long getGarbageChainLeftPage(final int p) {
        final long page = getLong(p + GARBAGE_BLOCK_LEFT_PAGE);
        Debug.$assert1.t(page > 0 && page <= MAX_VALID_PAGE_ADDR && page != _page);
        return page;
    }

    long getGarbageChainRightPage(final int p) {
        return getLong(p + GARBAGE_BLOCK_RIGHT_PAGE);
    }

    void removeGarbageChain() {
        Debug.$assert1.t(isGarbagePage() && _alloc + GARBAGE_BLOCK_SIZE <= _bufferSize);
        clearBytes(_alloc, _alloc + GARBAGE_BLOCK_SIZE);
        _alloc += GARBAGE_BLOCK_SIZE;
        bumpGeneration();
    }

    void setGarbageLeftPage(final long left) {
        Debug.$assert1.t(isOwnedAsWriterByMe() && isGarbagePage() && left > 0 && left <= MAX_VALID_PAGE_ADDR
                && left != _page && _alloc + GARBAGE_BLOCK_SIZE <= _bufferSize && _alloc >= _keyBlockEnd);
        putLong(_alloc + GARBAGE_BLOCK_LEFT_PAGE, left);
        bumpGeneration();
    }

    void populateInfo(final ManagementImpl.BufferInfo info) {
        info.poolIndex = _poolIndex;
        info.pageAddress = _page;
        info.rightSiblingAddress = _rightSibling;
        final Volume vol = _vol;
        if (vol != null) {
            info.volumeName = vol.getPath();
        } else {
            info.volumeName = null;
        }
        info.type = _type;
        info.typeName = getPageTypeName();
        info.bufferSize = _bufferSize;
        info.keyBlockStart = KEY_BLOCK_START;
        info.keyBlockEnd = _keyBlockEnd;
        info.availableBytes = getAvailableSize();
        info.alloc = _alloc;
        info.slack = _slack;
        info.timestamp = _timestamp;
        info.status = getStatus();
        info.statusName = getStatusCode();
        final Thread writerThread = getWriterThread();
        if (writerThread != null) {
            info.writerThreadName = writerThread.getName();
        } else {
            info.writerThreadName = null;
        }
        info.updateAcquisitonTime();
    }

    void enqueuePruningAction(final int treeHandle) {
        if (_mvvCount > 0) {
            final long delay = _persistit.getCleanupManager().getMinimumPruningDelay();
            if (delay > 0) {
                final long last = _lastPrunedTime;
                final long now = System.currentTimeMillis();
                if (now - last > delay) {
                    _lastPrunedTime = now;
                    _persistit.getCleanupManager().offer(
                            new CleanupManager.CleanupPruneAction(treeHandle, getPageAddress()));
                }
            }
        }
    }

    /**
     * Dump a copy of this <code>Buffer</code> to the supplied ByteBuffer. The
     * format is identical to the journal: an optional IV record to identify the
     * volume followed by a PA record.
     *
     * @param bb
     *            ByteBuffer to write to
     * @param secure
     *            If <code>true</code> obscure the data values
     * @param verbose
     *            If <code>true</code> display the buffer summary on System.out.
     * @param identifiedVolumes
     *            A set of Volumes for which IV records have already been
     *            written. This method adds a volume to this set whenever it
     *            writes an IV record.
     * @throws Exception
     */
    void dump(final ByteBuffer bb, final boolean secure, final boolean verbose, final Set<Volume> identifiedVolumes)
            throws Exception {
        final byte[] bytes = new byte[_bufferSize];
        int type;
        int keyBlockEnd;
        int alloc;
        int slack;
        long page;
        Volume volume;
        long rightSibling;
        long timestamp;
        int bufferSize;
        /*
         * Copy all the information needed quickly and then release the buffer.
         */
        final boolean claimed = claim(false, Persistit.SHORT_DELAY);
        try {
            bufferSize = _bufferSize;
            type = _type;
            keyBlockEnd = _keyBlockEnd;
            alloc = _alloc;
            slack = _slack;
            page = _page;
            volume = _vol;
            rightSibling = _rightSibling;
            timestamp = _timestamp;
            System.arraycopy(_bytes, 0, bytes, 0, bufferSize);
        } finally {
            if (claimed) {
                release();
            }
        }

        final String toString = toString();
        if (verbose) {
            System.out.println(toString);
        }

        final int volumeHandle = volume == null ? 0 : volume.getHandle();
        if (volume != null && !identifiedVolumes.contains(volume)) {
            IV.putType(bb);
            IV.putHandle(bb, volumeHandle);
            IV.putVolumeId(bb, volume.getId());
            IV.putTimestamp(bb, 0);
            if (_persistit.getConfiguration().isUseOldVSpec()) {
                IV.putVolumeSpecification(bb, volume.getName());
            } else {
                IV.putVolumeSpecification(bb, volume.getSpecification().toString());
            }
            bb.position(bb.position() + IV.getLength(bb));
            identifiedVolumes.add(volume);
        }

        final boolean isDataPage = type == PAGE_TYPE_DATA;
        final boolean isIndexPage = type >= PAGE_TYPE_INDEX_MIN && type <= PAGE_TYPE_INDEX_MAX;
        final boolean isLongRecordPage = type == PAGE_TYPE_LONG_RECORD;

        /*
         * Following is equivalent to the save method, except written to the
         * byte array copy.
         */
        Util.putLong(bytes, TIMESTAMP_OFFSET, timestamp);
        if (page != 0) {
            Util.putByte(bytes, TYPE_OFFSET, type);
            Util.putByte(bytes, BUFFER_LENGTH_OFFSET, bufferSize / 256);
            Util.putChar(bytes, KEY_BLOCK_END_OFFSET, keyBlockEnd);
            Util.putChar(bytes, FREE_OFFSET, alloc);
            Util.putChar(bytes, SLACK_OFFSET, slack);
            Util.putLong(bytes, PAGE_ADDRESS_OFFSET, page);
            Util.putLong(bytes, RIGHT_SIBLING_OFFSET, rightSibling);
        }

        if (isDataPage && secure) {
            dumpSecureOverwriteValues(bytes);
        }

        int left = bufferSize;
        int right = 0;
        if (isDataPage || isIndexPage) {
            if (KEY_BLOCK_START <= keyBlockEnd && keyBlockEnd <= alloc && alloc < left) {
                right = left - alloc;
                left = keyBlockEnd;
            }
        } else if (secure && isLongRecordPage) {
            left = KEY_BLOCK_START;
        }
        final int recordSize = PA.OVERHEAD + left + right;
        PA.putLength(bb, recordSize);
        PA.putType(bb);
        PA.putVolumeHandle(bb, volumeHandle);
        PA.putTimestamp(bb, timestamp);
        PA.putLeftSize(bb, left);
        PA.putBufferSize(bb, bufferSize);
        PA.putPageAddress(bb, page);
        bb.position(bb.position() + PA.OVERHEAD);
        bb.put(bytes, 0, left);
        bb.put(bytes, bufferSize - right, right);
    }

    /**
     * Overwrite the value payload bytes in the supplied buffer image to appear
     * as strings of 'x's.
     *
     * @param bytes
     *            buffer image
     */
    private void dumpSecureOverwriteValues(final byte[] bytes) {
        if (bytes[0] != PAGE_TYPE_DATA) {
            return;
        }
        /*
         * Figure out if this is part of any system tree. If so then don't
         * overwrite values.
         */
        for (int p = KEY_BLOCK_START; p < Util.getInt(bytes, KEY_BLOCK_END_OFFSET); p += KEYBLOCK_LENGTH) {
            final int kbData = Util.getInt(bytes, p);
            final int db = decodeKeyBlockDb(kbData);
            if (db == 0 && p == KEY_BLOCK_START) {
                continue;
            } else if (db == Key.TYPE_STRING) {
                final int tail = decodeKeyBlockTail(kbData);
                if (bytes[tail + TAILBLOCK_HDR_SIZE_DATA] == '_') {
                    // Probably a system key - don't overwrite values
                    return;
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        int tail = Util.getChar(bytes, FREE_OFFSET);
        for (; tail < bytes.length;) {
            final int tbData = Util.getInt(bytes, tail);
            final int tbSize = (decodeTailBlockSize(tbData) + ~TAILBLOCK_MASK) & TAILBLOCK_MASK;
            final int tbKLength = decodeTailBlockKLength(tbData);
            //
            // If the tbSize field is corrupt then just dump the
            // remainder of the buffer
            // since we need that to figure out the problem.
            //
            if (tbSize < TAILBLOCK_HDR_SIZE_DATA || tbSize + tail > bytes.length) {
                break;
            }
            //
            // Otherwise, dump just the portion of the tailblock we
            // need for analysis and fill the rest with 'x's.
            //
            final boolean tbInUse = decodeTailBlockInUse(tbData);
            // Number of bytes we need to dump
            int keep;
            if (tbInUse) {
                keep = TAILBLOCK_HDR_SIZE_DATA + tbKLength;
                if (tbSize - keep >= LONGREC_PREFIX_SIZE && Util.getByte(bytes, tail + keep) == LONGREC_TYPE) {
                    keep += LONGREC_PREFIX_OFFSET;
                }
                if (keep < TAILBLOCK_HDR_SIZE_DATA && keep > tbSize) {
                    keep = tbSize;
                }
            } else {
                keep = TAILBLOCK_HDR_SIZE_DATA;
            }

            for (int fill = keep; fill < tbSize; fill++) {
                bytes[tail + fill] = (byte) (fill == keep ? ' ' : 'x');
            }
            tail += tbSize;
        }
    }

    static void deallocatePrunedVersions(final Persistit persistit, final Volume volume,
            final List<PrunedVersion> prunedVersions) {
        for (final PrunedVersion pv : prunedVersions) {
            final TransactionStatus ts = persistit.getTransactionIndex().getStatus(pv.getTs());
            if (ts != null && ts.getTc() == TransactionStatus.ABORTED) {
                ts.decrementMvvCount();
            }
            if (pv.getLongRecordPage() != 0) {
                try {
                    volume.getStructure().deallocateGarbageChain(pv.getLongRecordPage(), 0);
                } catch (final PersistitException e) {
                    persistit.getLogBase().pruneException.log(e, ts);
                }
            }
        }
        prunedVersions.clear();
    }

    static boolean isLongRecord(final byte[] bytes, final int offset, final int length) {
        return (length > 0) && ((bytes[offset] & 0xFF) == LONGREC_TYPE);
    }

    static boolean isLongMVV(final byte[] bytes, final int offset, final int length) {
        return isLongRecord(bytes, offset, length) && (length > LONGREC_PREFIX_OFFSET)
                && MVV.isArrayMVV(bytes, offset + LONGREC_PREFIX_OFFSET, length - LONGREC_PREFIX_OFFSET);
    }

    static boolean isValueMVV(final byte[] bytes, final int offset, final int length) {
        return MVV.isArrayMVV(bytes, offset, length) || isLongMVV(bytes, offset, length);
    }

    private void incCountIfMvv(final byte[] bytes, final int offset, final int length) {
        if (isValueMVV(bytes, offset, length)) {
            ++_mvvCount;
        }
    }

}
