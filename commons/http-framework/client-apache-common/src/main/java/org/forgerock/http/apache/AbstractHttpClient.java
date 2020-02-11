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

package org.forgerock.http.apache;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.InputStreamEntity;
import org.forgerock.http.header.ConnectionHeader;
import org.forgerock.http.header.ContentEncodingHeader;
import org.forgerock.http.header.ContentLengthHeader;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.io.IO;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.spi.HttpClient;
import org.forgerock.http.util.CaseInsensitiveSet;
import org.forgerock.util.Factory;

/**
 * This abstract client is used to share commonly used constants and methods
 * in both synchronous and asynchronous Apache HTTP Client libraries.
 */
public abstract class AbstractHttpClient implements HttpClient {

    /** Headers that are suppressed in request. */
    // FIXME: How should the the "Expect" header be handled?
    private static final CaseInsensitiveSet SUPPRESS_REQUEST_HEADERS = new CaseInsensitiveSet(
            Arrays.asList(
                    // populated in outgoing request by EntityRequest (HttpEntityEnclosingRequestBase):
                    "Content-Encoding", "Content-Length", "Content-Type",
                    // hop-by-hop headers, not forwarded by proxies, per RFC 2616 13.5.1:
                    "Connection", "Keep-Alive", "Proxy-Authenticate", "Proxy-Authorization", "TE",
                    "Trailers", "Transfer-Encoding", "Upgrade"));

    /** Headers that are suppressed in response. */
    private static final CaseInsensitiveSet SUPPRESS_RESPONSE_HEADERS = new CaseInsensitiveSet(
            Arrays.asList(
                    // hop-by-hop headers, not forwarded by proxies, per RFC 2616 13.5.1:
                    "Connection", "Keep-Alive", "Proxy-Authenticate", "Proxy-Authorization", "TE",
                    "Trailers", "Transfer-Encoding", "Upgrade"));

    private final Factory<Buffer> storage;

    /**
     * Base constructor for AHC {@link HttpClient} drivers.
     *
     * @param storage
     *         temporary storage area
     */
    protected AbstractHttpClient(final Factory<Buffer> storage) {
        this.storage = storage;
    }

    /** A request that encloses an entity. */
    private static class EntityRequest extends HttpEntityEnclosingRequestBase {
        private final String method;

        public EntityRequest(final Request request) {
            this.method = request.getMethod();
            final InputStreamEntity entity =
                    new InputStreamEntity(request.getEntity().getRawContentInputStream(),
                            ContentLengthHeader.valueOf(request).getLength());
            final List<String> contentType = ContentTypeHeader.valueOf(request).getValues();
            if (contentType != null && contentType.size() > 1) {
                throw new IllegalArgumentException("Content-Type configured with multiple values");
            }
            entity.setContentType(contentType == null || contentType.size() == 0 ? null : contentType.get(0));
            final List<String> encoding = ContentEncodingHeader.valueOf(request).getValues();
            if (encoding != null && encoding.size() > 1) {
                throw new IllegalArgumentException("Content-Encoding configured with multiple values");
            }
            entity.setContentEncoding(encoding == null || encoding.size() == 0 ? null : encoding.get(0));
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

    /**
     * Creates a new {@link HttpUriRequest} populated from the given {@code request}.
     * The returned message has some of its headers filtered/ignored (proxy behaviour).
     *
     * @param request OpenIG request structure
     * @return AHC request structure
     */
    protected HttpUriRequest createHttpUriRequest(final Request request) {
        // Create the Http request depending if there is an entity or not
        HttpRequestBase clientRequest = request.getEntity().isRawContentEmpty()
                ? new NonEntityRequest(request) : new EntityRequest(request);
        clientRequest.setURI(request.getUri().asURI());

        // Parse request Connection headers to be suppressed in message
        CaseInsensitiveSet removableHeaderNames = new CaseInsensitiveSet();
        removableHeaderNames.addAll(ConnectionHeader.valueOf(request).getTokens());

        // Populates request headers
        for (String name : request.getHeaders().keySet()) {
            if (!SUPPRESS_REQUEST_HEADERS.contains(name) && !removableHeaderNames.contains(name)) {
                for (final String value : request.getHeaders().get(name).getValues()) {
                    clientRequest.addHeader(name, value);
                }
            }
        }
        return clientRequest;
    }

    /**
     * Creates a new {@link Response} populated from the given AHC {@code result}.
     * The returned message has some of its headers filtered/ignored (proxy behaviour).
     *
     * @param result AHC response structure
     * @return openIG response structure
     */
    protected Response createResponse(final HttpResponse result) {
        Response response = new Response();
        // Response entity
        HttpEntity entity = result.getEntity();
        if (entity != null) {
            try {
                response.setEntity(IO.newBranchingInputStream(entity.getContent(), storage));
            } catch (IOException e) {
                response.setStatus(Status.INTERNAL_SERVER_ERROR);
                response.setCause(e);
                return response;
            }
        }

        // Response status line
        StatusLine statusLine = result.getStatusLine();
        response.setVersion(statusLine.getProtocolVersion().toString());
        response.setStatus(Status.valueOf(statusLine.getStatusCode(), statusLine.getReasonPhrase()));

        // Parse response Connection headers to be suppressed in message
        CaseInsensitiveSet removableHeaderNames = new CaseInsensitiveSet();
        removableHeaderNames.addAll(ConnectionHeader.valueOf(response).getTokens());

        // Response headers
        for (HeaderIterator i = result.headerIterator(); i.hasNext();) {
            Header header = i.nextHeader();
            String name = header.getName();
            if (!SUPPRESS_RESPONSE_HEADERS.contains(name) && !removableHeaderNames.contains(name)) {
                response.getHeaders().add(name, header.getValue());
            }
        }

        return response;
    }
}
