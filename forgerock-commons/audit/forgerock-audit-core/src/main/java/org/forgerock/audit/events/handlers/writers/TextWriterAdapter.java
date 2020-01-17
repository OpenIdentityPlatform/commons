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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.audit.events.handlers.writers;

import java.io.IOException;
import java.io.Writer;

/**
 * Wraps a {@link TextWriter} in  a {@link Writer}.
 */
public class TextWriterAdapter extends Writer implements TextWriter {

    private final TextWriter delegate;

    /**
     * Creates the writer.
     *
     * @param delegate
     *          Delegate writer.
     */
    public TextWriterAdapter(TextWriter delegate) {
        this.delegate = delegate;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        write(new String(cbuf, off, len));
    }

    @Override
    public void write(int c) throws IOException {
        write(String.valueOf((char) c));
    }

    @Override
    public void write(char[] cbuf) throws IOException {
        write(String.valueOf(cbuf));
    }

    @Override
    public void write(String str) throws IOException {
        delegate.write(str);
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        write(str.substring(off, off + len));
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public void close() throws IOException {
        shutdown();
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public long getBytesWritten() {
        return delegate.getBytesWritten();
    }
}