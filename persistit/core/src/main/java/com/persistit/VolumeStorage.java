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
 * Portions Copyrighted 2026 3A Systems, LLC
 */

package com.persistit;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Random;

import com.persistit.exception.InUseException;
import com.persistit.exception.InvalidPageAddressException;
import com.persistit.exception.PersistitException;
import com.persistit.exception.PersistitIOException;
import com.persistit.exception.PersistitInterruptedException;
import com.persistit.exception.VolumeClosedException;

/**
 * Manage all details of file I/O for a <code>Volume</code> backing file.
 * 
 * @author peter
 */
abstract class VolumeStorage extends SharedResource {

    private final static Random ID_GENERATOR = new Random();
    protected Volume _volume;

    /**
     * Generate a random positive (non-zero) long value to be used as a
     * validation of a Volume's identity.
     *
     * @return a random positive (non-zero) long identity value
     */
    protected static long generateId() {
        return (ID_GENERATOR.nextLong() & 0x0FFFFFFFFFFl) + 1;
    }

    VolumeStorage(final Persistit persistit, final Volume volume) {
        super(persistit);
        _volume = volume;
    }

    /**
     * Returns the path name by which this volume was opened.
     * 
     * @return The path name
     */
    String getPath() {
        return _volume.getSpecification().getPath();
    }

    /**
     * Indicate whether this <code>Volume</code> prohibits updates.
     * 
     * @return <code>true</code> if this Volume prohibits updates.
     */
    boolean isReadOnly() {
        return _volume.getSpecification().isReadOnly();
    }

    /**
     * Indicate whether this is a temporary volume
     * 
     * @return <code>true</code> if this volume is temporary
     */
    abstract boolean isTemp();

    /**
     * @return the channel used to read and write pages of this volume.
     * @throws PersistitIOException if an I/O error occurs
     */
    abstract FileChannel getChannel() throws PersistitIOException;

    /**
     * Create a new <code>Volume</code> backing file according to the
     * {@link Volume}'s volume specification.
     * 
     * @throws PersistitException if a persistence error occurs
     */
    abstract void create() throws PersistitException;

    /**
     * Open an existing <code>Volume</code> backing file.
     * 
     * @throws PersistitException if a persistence error occurs
     */
    abstract void open() throws PersistitException;

    /**
     * @return <code>true</code> if a backing file exists on the specified path.
     * @throws PersistitException if a persistence error occurs
     */
    abstract boolean exists() throws PersistitException;

    /**
     * Delete the backing file for this Volume if it exists.
     * 
     * @return <code>true</code> if there was a file and it was successfully
     *         deleted
     * @throws PersistitException if a persistence error occurs
     */
    abstract boolean delete() throws PersistitException;

    /**
     * Force all file system buffers to disk.
     * 
     * @throws PersistitIOException if an I/O error occurs
     */
    abstract void force() throws PersistitIOException;

    /**
     * Close the file resources held by this <code>Volume</code>. After this
     * method is called no further file I/O is possible.
     * 
     * @throws PersistitException if a persistence error occurs
     */
    abstract void close() throws PersistitException;

    /**
     * Flush metadata to the volume backing store.
     * 
     * @throws PersistitException if a persistence error occurs
     */
    abstract void flush() throws PersistitException;

    abstract void truncate() throws PersistitException;

    abstract boolean isOpened();

    abstract boolean isClosed();

    abstract long getExtentedPageCount();

    abstract long getNextAvailablePage();

    abstract void claimHeadBuffer() throws PersistitException;

    abstract void releaseHeadBuffer();

    abstract void readPage(Buffer buffer) throws PersistitIOException, InvalidPageAddressException,
            VolumeClosedException, InUseException, PersistitInterruptedException;

    abstract void writePage(final Buffer buffer) throws PersistitException;

    abstract void writePage(final ByteBuffer bb, final long page) throws PersistitException;

    abstract long allocNewPage() throws PersistitException;

    abstract void extend(final long pageAddr) throws PersistitException;

    abstract void flushMetaData() throws PersistitException;

    abstract boolean updateMetaData(final byte[] bytes);

}
