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

package org.forgerock.http.apache.async;

import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.forgerock.http.apache.AbstractHttpClient;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.util.Factory;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.PromiseImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Apache HTTP Async Client based implementation.
 */
public class AsyncHttpClient extends AbstractHttpClient {

    private static final Logger logger = LoggerFactory.getLogger(AsyncHttpClient.class);

    private final CloseableHttpAsyncClient client;

    AsyncHttpClient(final CloseableHttpAsyncClient client, final Factory<Buffer> storage) {
        super(storage);
        // Client should already be started
        this.client = client;
    }

    @Override
    public Promise<Response, NeverThrowsException> sendAsync(final Request request) {

        HttpUriRequest clientRequest = createHttpUriRequest(request);

        // Send request and return the configured Promise
        final PromiseImpl<Response, NeverThrowsException> promise = PromiseImpl.create();
        FutureCallback<HttpResponse> callback = new FutureCallback<HttpResponse>() {

            @Override
            public void completed(final HttpResponse result) {
                Response response = createResponse(result);
                promise.handleResult(response);
            }

            @Override
            public void failed(final Exception ex) {
                logger.error("Failed to obtain response for {}", request.getUri());
                Response response = new Response(Status.BAD_GATEWAY);
                response.setCause(ex);
                promise.handleResult(response);
            }

            @Override
            public void cancelled() {
                failed(new InterruptedException("Request processing has been cancelled"));
            }
        };

        // Copy the MDC before submitting the job
        Map<String, String> mdc = MDC.getCopyOfContextMap();
        if (mdc != null) {
            callback = new MdcFutureCallback(callback, mdc);
        }

        // Execute
        client.execute(clientRequest, callback);

        return promise;
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

    /**
     * This callback setup the MDC when the async HTTP client hand-off response processing back to the caller.
     * In other words, all log statements appearing before this callback is invoked will not have updated
     * contextual information.
     */
    private static final class MdcFutureCallback implements FutureCallback<HttpResponse> {

        private final FutureCallback<HttpResponse> delegate;
        private final Map<String, String> mdc;

        private MdcFutureCallback(final FutureCallback<HttpResponse> delegate, final Map<String, String> mdc) {
            this.delegate = delegate;
            this.mdc = mdc;
        }

        @Override
        public void completed(final HttpResponse result) {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            try {
                MDC.setContextMap(mdc);
                delegate.completed(result);
            } finally {
                restoreMdc(previous);
            }
        }

        @Override
        public void failed(final Exception ex) {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            try {
                MDC.setContextMap(mdc);
                delegate.failed(ex);
            } finally {
                restoreMdc(previous);
            }
        }

        @Override
        public void cancelled() {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            try {
                MDC.setContextMap(mdc);
                delegate.cancelled();
            } finally {
                restoreMdc(previous);
            }
        }

        private void restoreMdc(Map<String, String> previous) {
            if (previous != null) {
                MDC.setContextMap(previous);
            } else {
                MDC.clear();
            }
        }
    }
}
