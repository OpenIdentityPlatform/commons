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
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.http.spi;

import java.io.Closeable;
import java.io.IOException;

import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.ResponseException;
import org.forgerock.http.util.Options;
import org.forgerock.util.promise.Promise;

/**
 * An SPI interface for HTTP {@code Client} implementations. A
 * {@link ClientImplProvider} is loaded during construction of a new HTTP
 * {@link org.forgerock.http.Client Client}. The first available provider is
 * selected and its {@link ClientImplProvider#newClientImpl(Options)} method
 * invoked in order to construct and configure a new {@link ClientImpl}.
 */
public interface ClientImpl extends Closeable {

    /**
     * Sends an HTTP request to a remote server and returns a {@code Promise}
     * representing the asynchronous response.
     *
     * @param request
     *            The HTTP request to send.
     * @return A promise representing the pending HTTP response. The promise
     *         will yield a {@code ResponseException} when a non-2xx HTTP status
     *         code is returned.
     */
    public Promise<Response, ResponseException> sendAsync(Request request);

    /**
     * Completes all pending requests and release resources associated with
     * underlying implementation.
     */
    @Override
    public void close() throws IOException;
}
