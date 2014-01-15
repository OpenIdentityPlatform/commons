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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.jaspi.runtime.response;

import javax.servlet.ServletOutputStream;
import java.io.IOException;

/**
 * Wrapper around a HttpServletResponse's output stream to prevent subsequent Servlets closing the stream before the
 * JASPI filter has run secureResponse.
 *
 * @since 1.0.3
 */
public class JaspiServletOutputStream extends ServletOutputStream {

    private final ServletOutputStream servletOutputStream;

    /**
     * Constructs an instance of the JaspiServletOutputStream.
     *
     * @param servletOutputStream The underlying ServletOutputStream.
     */
    public JaspiServletOutputStream(final ServletOutputStream servletOutputStream) {
        this.servletOutputStream = servletOutputStream;
    }

    /**
     * Calls write(int) on the underlying output stream.
     *
     * @param i {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    @Override
    public void write(final int i) throws IOException {
        servletOutputStream.write(i);
    }

    /**
     * Calls write(byte[]) on the underlying output stream.
     *
     * @param b {@inheritDoc}
     * @throws IOException {@inheritDoc}
     * @since 1.3.0
     */
    @Override
    public void write(byte[] b) throws IOException {
        servletOutputStream.write(b);
    }

    /**
     * Calls write(byte[], int, int) on the underlying output stream.
     *
     * @param b {@inheritDoc}
     * @param off {@inheritDoc}
     * @param len {@inheritDoc}
     * @throws IOException {@inheritDoc}
     * @since 1.3.0
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        servletOutputStream.write(b, off, len);
    }

    /**
     * Calls close() on the underlying output stream.
     *
     * @throws IOException {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        servletOutputStream.close();
    }
}
