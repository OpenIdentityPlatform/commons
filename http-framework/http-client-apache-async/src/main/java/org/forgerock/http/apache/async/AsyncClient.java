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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.http.apache.async;

import static java.lang.String.format;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.forgerock.http.apache.AbstractClient;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.util.Factory;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.PromiseImpl;

/**
 * Apache HTTP Async Client based implementation.
 */
public class AsyncClient extends AbstractClient {

    private final CloseableHttpAsyncClient client;

    AsyncClient(final CloseableHttpAsyncClient client, final Factory<Buffer> storage) {
        super(storage);
        // Client should already be started
        this.client = client;
    }

    @Override
    public Promise<Response, NeverThrowsException> sendAsync(final Request request) {

        HttpUriRequest clientRequest = createHttpUriRequest(request);

        // Send request and return the configured Promise
        final PromiseImpl<Response, NeverThrowsException> promise = PromiseImpl.create();
        client.execute(clientRequest, new FutureCallback<HttpResponse>() {

            @Override
            public void completed(final HttpResponse result) {
                Response response = createResponse(result);
                promise.handleResult(response);
            }

            @Override
            public void failed(final Exception ex) {
                Response response = new Response(Status.BAD_GATEWAY);
                response.setEntity(format("Failed to obtain response for %s", request.getUri()));
                response.setCause(ex);
                promise.handleResult(response);
            }

            @Override
            public void cancelled() {
                failed(new InterruptedException("Request processing has been cancelled"));
            }
        });

        return promise;
    }

    @Override
    public void close() throws IOException {
        client.close();
    }
}
