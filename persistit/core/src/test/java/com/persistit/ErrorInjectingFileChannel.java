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

import com.persistit.MediatedFileChannel.TestChannelInjector;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * <p>
 * A {@link FileChannel} implementation that simulates IOExceptions under
 * control of a unit test program. This class implements only those methods used
 * by Persistit; many methods of FileChannel throw
 * {@link UnsupportedOperationException}.
 * </p>
 * 
 * @author peter
 * 
 */
class ErrorInjectingFileChannel extends FileChannel implements TestChannelInjector {

    volatile FileChannel _channel;

    volatile IOException _injectedIOException;
    volatile String _injectedIOExceptionFlags;
    volatile long _injectedDiskFullLimit = Long.MAX_VALUE;

    @Override
    public void setChannel(final FileChannel channel) {
        _channel = channel;
    }

    private void injectFailure(final char type) throws IOException {
        final IOException e = _injectedIOException;
        if (e != null && _injectedIOExceptionFlags.indexOf(type) >= 0) {
            throw e;
        }
    }

    /**
     * Set an IOException to be thrown on subsequent I/O operations. This method
     * is intended for use only for unit tests. The <code>flags</code> parameter
     * determines which I/O operations throw exceptions:
     * <ul>
     * <li>o - open</li>
     * <li>c - close</li>
     * <li>r - read</li>
     * <li>w - write</li>
     * <li>f - force</li>
     * <li>t - truncate</li>
     * <li>l - lock</li>
     * <li>e - extending Volume file</li>
     * </ul>
     * For example, if flags is "wt" then write and truncate operations with
     * throw the injected IOException.
     * 
     * @param exception
     *            The IOException to throw
     * @param flags
     *            Selected operations
     */
    void injectTestIOException(final IOException exception, final String flags) {
        _injectedIOException = exception;
        _injectedIOExceptionFlags = flags;
    }

    /**
     * Sets a file position at which writes will simulate a disk-full condition
     * by throwing an IOException.
     * 
     * @param limit
     */
    void injectDiskFullLimit(final long limit) {
        _injectedDiskFullLimit = limit;
    }

    @Override
    protected void implCloseChannel() throws IOException {
        _channel.close();
        injectFailure('c');
    }

    /*
     * --------------------------------
     * 
     * Implementations of these FileChannel methods simply delegate to the inner
     * FileChannel. But they retry upon receiving a ClosedChannelException
     * caused by an I/O operation on a different thread having been interrupted.
     * 
     * --------------------------------
     */
    @Override
    public void force(final boolean metaData) throws IOException {
        injectFailure('f');
        _channel.force(metaData);
    }

    @Override
    public int read(final ByteBuffer byteBuffer, final long position) throws IOException {
        injectFailure('r');
        return _channel.read(byteBuffer, position);
    }

    @Override
    public long size() throws IOException {
        injectFailure('s');
        return _channel.size();
    }

    @Override
    public FileChannel truncate(final long size) throws IOException {
        injectFailure('t');
        return _channel.truncate(size);
    }

    @Override
    public synchronized FileLock tryLock(final long position, final long size, final boolean shared) throws IOException {
        injectFailure('l');
        return _channel.tryLock(position, size, shared);
    }

    @Override
    public int write(final ByteBuffer byteBuffer, final long position) throws IOException {
        injectFailure('w');
        if (byteBuffer.remaining() == 1) {
            injectFailure('e');
        }
        final long capacity = Math.max(0L, _injectedDiskFullLimit - position);
        final int delta = (int) Math.max(0L, byteBuffer.remaining() - capacity);
        byteBuffer.limit(byteBuffer.limit() - delta);
        final int written = _channel.write(byteBuffer, position);
        byteBuffer.limit(byteBuffer.limit() + delta);
        /*
         * Modified to mimic behavior seen on an actual full disk: A Disk Full
         * condition is thrown only if no bytes can be written.
         */
        if (delta > 0 && written == 0) {
            throw new IOException("Disk Full");
        } else {
            return written;
        }
    }

    /*
     * --------------------------------
     * 
     * Persistit does not use these methods and so they are Unsupported. Note
     * that it would be difficult to support the relative read/write methods
     * because the channel size is unavailable after it is closed. Therefore a
     * client of this class must maintain its own position counter and cannot
     * use the relative-addressing calls.
     * 
     * --------------------------------
     */
    @Override
    public FileLock lock(final long position, final long size, final boolean shared) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public MappedByteBuffer map(final MapMode arg0, final long arg1, final long arg2) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long position() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileChannel position(final long arg0) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int read(final ByteBuffer byteBuffer) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long read(final ByteBuffer[] arg0, final int arg1, final int arg2) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long transferFrom(final ReadableByteChannel arg0, final long arg1, final long arg2) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long transferTo(final long arg0, final long arg1, final WritableByteChannel arg2) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int write(final ByteBuffer byteBuffer) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long write(final ByteBuffer[] arg0, final int arg1, final int arg2) throws IOException {
        throw new UnsupportedOperationException();
    }

}
