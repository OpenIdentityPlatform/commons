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
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Response Wrapper to prevent subsequent Servlets closing the response before the JASPI filter has run
 * secureResponse.
 *
 * @since 1.0.3
 */
public class JaspiHttpServletResponseWrapper extends HttpServletResponseWrapper {

    private PrintWriter writer;
    private ServletOutputStream out;

    /**
     * Constructs an instance of the JaspiHttpServletResponseWrapper.
     *
     * @param response The underlying HttpServletResponse.
     */
    public JaspiHttpServletResponseWrapper(final HttpServletResponse response) {
        super(response);
    }

    /**
     * Lazily initialises the wrapped underlying response output stream.
     *
     * @return {@inheritDoc}
     * @throws java.io.IOException {@inheritDoc}
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (out == null) {
            out = new JaspiServletOutputStream(getResponse().getOutputStream());
        }
        return out;
    }

    /**
     * Lasily initialises the wrapped underlying response writer.
     *
     * @return {@inheritDoc}
     * @throws java.io.IOException {@inheritDoc}
     */
    @Override
    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
            writer = new JaspiPrintWriter(getResponse().getWriter());
        }
        return writer;
    }
}
