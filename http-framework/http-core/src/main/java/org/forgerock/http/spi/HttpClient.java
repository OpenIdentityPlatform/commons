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
 * Copyright 2014-2016 ForgeRock AS.
 */
package org.forgerock.http.spi;

import java.io.Closeable;
import java.io.IOException;

import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * An SPI interface for HTTP {@code Client} implementations. A
 * {@link HttpClientProvider} is loaded during construction of a new HTTP
 * {@link org.forgerock.http.Client Client}. The first available provider is
 * selected and its {@link HttpClientProvider#newHttpClient(org.forgerock.util.Options)} method
 * invoked in order to construct and configure a new {@link HttpClient}.
 *
 * <p>It is the responsibility of the caller to {@link org.forgerock.http.protocol.Message#close() close}
 * the request and response messages.
 *
 * <pre>
 *     {@code
 *     HttpClient client = ...
 *     try (Response response = client.sendAsync(...).getOrThrowUninterruptibly()) {
 *       // consumes the response completely
 *     }
 *     }
 * </pre>
 *
 * <p><b>Note:</b> Callers should not use try-with-resources pattern if they forward the
 * response asynchronously (using a {@link Promise} for instance): the message
 * would be emptied before the callbacks are applied.
 */
public interface HttpClient extends Closeable {

    /**
     * Returns a {@link Promise} representing the asynchronous {@link Response} of the given {@code request}.
     * If any (asynchronous) processing goes wrong, the promise still contains a {@link Response} (probably from the
     * {@literal 4xx} or {@literal 5xx} status code family).
     * <p>
     * The returned {@link Promise} contains the response returned from the server as-is.
     * This is responsibility of the client to produce the appropriate error response ({@literal 404},
     * {@literal 500}, ...) in case of processing or transport errors.
     *
     * @param request
     *            The HTTP request to send.
     * @return A promise representing the pending HTTP response.
     */
    Promise<Response, NeverThrowsException> sendAsync(Request request);

    /**
     * Completes all pending requests and release resources associated with
     * underlying implementation.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    void close() throws IOException;
}
