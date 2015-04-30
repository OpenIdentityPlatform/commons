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
 * Copyright 2009 Sun Microsystems Inc.
 * Portions Copyright 2010–2011 ApexIdentity Inc.
 * Portions Copyright 2011-2015 ForgeRock AS.
 */

package org.forgerock.http.apache.httpclient;

import java.io.IOException;
import java.util.Arrays;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.forgerock.http.header.ConnectionHeader;
import org.forgerock.http.header.ContentEncodingHeader;
import org.forgerock.http.header.ContentLengthHeader;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.io.IO;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.ResponseException;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.spi.ClientImpl;
import org.forgerock.http.util.CaseInsensitiveSet;
import org.forgerock.util.Factory;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * Apache HTTP Client implementation.
 */
final class ApacheHttpClientImpl implements ClientImpl {

    /** A request that encloses an entity. */
    private static class EntityRequest extends HttpEntityEnclosingRequestBase {
        private final String method;

        public EntityRequest(final Request request) {
            this.method = request.getMethod();
            final InputStreamEntity entity =
                    new InputStreamEntity(request.getEntity().getRawContentInputStream(),
                            ContentLengthHeader.valueOf(request).getLength());
            entity.setContentType(ContentTypeHeader.valueOf(request).toString());
            entity.setContentEncoding(ContentEncodingHeader.valueOf(request).toString());
            setEntity(entity);
        }

        @Override
        public String getMethod() {
            return method;
        }
    }

    /** A request that does not enclose an entity. */
    private static class NonEntityRequest extends HttpRequestBase {
        private final String method;

        public NonEntityRequest(final Request request) {
            this.method = request.getMethod();
            final Header[] contentLengthHeader = getHeaders(ContentLengthHeader.NAME);
            if ((contentLengthHeader == null || contentLengthHeader.length == 0)
                    && ("PUT".equals(method) || "POST".equals(method) || "PROPFIND".equals(method))) {
                setHeader(ContentLengthHeader.NAME, "0");
            }
        }

        @Override
        public String getMethod() {
            return method;
        }
    }

    /** Headers that are suppressed in request. */
    // FIXME: How should the the "Expect" header be handled?
    private static final CaseInsensitiveSet SUPPRESS_REQUEST_HEADERS = new CaseInsensitiveSet(
            Arrays.asList(
                    // populated in outgoing request by EntityRequest (HttpEntityEnclosingRequestBase):
                    "Content-Encoding", "Content-Length", "Content-Type",
                    // hop-by-hop headers, not forwarded by proxies, per RFC 2616 §13.5.1:
                    "Connection", "Keep-Alive", "Proxy-Authenticate", "Proxy-Authorization", "TE",
                    "Trailers", "Transfer-Encoding", "Upgrade"));

    /** Headers that are suppressed in response. */
    private static final CaseInsensitiveSet SUPPRESS_RESPONSE_HEADERS = new CaseInsensitiveSet(
            Arrays.asList(
                    // hop-by-hop headers, not forwarded by proxies, per RFC 2616 §13.5.1:
                    "Connection", "Keep-Alive", "Proxy-Authenticate", "Proxy-Authorization", "TE",
                    "Trailers", "Transfer-Encoding", "Upgrade"));

    /** The Apache HTTP client to transmit requests through. */
    private final CloseableHttpClient httpClient;

    /**
     * Allocates temporary buffers for caching streamed content during request
     * processing.
     */
    private final Factory<Buffer> storage;

    ApacheHttpClientImpl(final CloseableHttpClient httpClient, final Factory<Buffer> storage) {
        this.httpClient = httpClient;
        this.storage = storage;
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }

    @Override
    public Promise<Response, ResponseException> sendAsync(final Request request) {
        final HttpRequestBase clientRequest =
                request.getEntity().mayContainData() ? new EntityRequest(request)
                        : new NonEntityRequest(request);
        clientRequest.setURI(request.getUri().asURI());
        // connection headers to suppress
        final CaseInsensitiveSet suppressConnection = new CaseInsensitiveSet();
        // parse request connection headers to be suppressed in request
        suppressConnection.addAll(ConnectionHeader.valueOf(request).getTokens());
        // request headers
        for (final String name : request.getHeaders().keySet()) {
            if (!SUPPRESS_REQUEST_HEADERS.contains(name) && !suppressConnection.contains(name)) {
                for (final String value : request.getHeaders().get(name)) {
                    clientRequest.addHeader(name, value);
                }
            }
        }
        // send request
        final Response response = new Response();
        final HttpResponse clientResponse;
        try {
            clientResponse = httpClient.execute(clientRequest);
            // response entity
            final HttpEntity clientResponseEntity = clientResponse.getEntity();
            if (clientResponseEntity != null) {
                response.setEntity(IO.newBranchingInputStream(clientResponseEntity.getContent(),
                        storage));
            }
        } catch (final IOException e) {
            response.setStatus(Status.INTERNAL_SERVER_ERROR);
            final ResponseException re = new ResponseException(response, "Cannot obtain a Response from server", e);
            return Promises.newExceptionPromise(re);
        }

        // response status line
        final StatusLine statusLine = clientResponse.getStatusLine();
        response.setVersion(statusLine.getProtocolVersion().toString());
        response.setStatus(Status.valueOf(statusLine.getStatusCode()));
        // parse response connection headers to be suppressed in response
        suppressConnection.clear();
        suppressConnection.addAll(ConnectionHeader.valueOf(response).getTokens());
        // response headers
        for (final HeaderIterator i = clientResponse.headerIterator(); i.hasNext();) {
            final Header header = i.nextHeader();
            final String name = header.getName();
            if (!SUPPRESS_RESPONSE_HEADERS.contains(name) && !suppressConnection.contains(name)) {
                response.getHeaders().add(name, header.getValue());
            }
        }
        // TODO: decide if need to try-finally to call httpRequest.abort?
        if (response.getStatus().isSuccessful()) {
            return Promises.newResultPromise(response);
        } else {
            return Promises.newExceptionPromise(new ResponseException(response));
        }
    }

}
