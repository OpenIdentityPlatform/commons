/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.http.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * A {@link BranchingInputStream} for reading from files. Uses a {@code FileInputStream} to read from the file, but
 * tracks the position read to so that branches can skip to the same location.
 * <p>
 *     This stream would be most suited for when a large file is expected to piped straight to output, without
 *     modification and minimal branching. If the stream is not a large file, is going to be modified by filters, or is
 *     going to have substantial branching, a wrapper {@code BranchingInputStream} could be considered instead.
 * </p>
 * @see IO#newBranchingInputStream(java.io.InputStream, org.forgerock.util.Factory)
 */
public final class FileBranchingStream extends BranchingInputStream {

    private final InputStream input;
    private final File file;
    private long position = 0;

    /**
     * Creates a new stream for the specified file.
     * @param file The file to read from.
     * @throws FileNotFoundException If the file does not exist.
     */
    public FileBranchingStream(File file) throws FileNotFoundException {
        super(null);
        this.file = file;
        input = new BufferedInputStream(new FileInputStream(file));
    }

    private FileBranchingStream(File file, long position, BranchingInputStream parent) throws IOException {
        super(parent);
        this.file = file;
        input = new FileInputStream(file);
        long skipBytes = position;
        while (this.position < position) {
            long skipped = input.skip(skipBytes);
            this.position += skipped;
            skipBytes -= skipped;
        }
    }

    @Override
    public BranchingInputStream branch() throws IOException {
        return new FileBranchingStream(file, position, this);
    }

    @Override
    public BranchingInputStream copy() throws IOException {
        return new FileBranchingStream(file, position, parent());
    }

    @Override
    public int read() throws IOException {
        position++;
        return input.read();
    }

    @Override
    public void close() throws IOException {
        input.close();
    }

}
