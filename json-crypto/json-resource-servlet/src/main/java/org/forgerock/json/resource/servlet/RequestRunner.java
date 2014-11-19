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
 * Copyright 2012-2014 ForgeRock AS.
 */

package org.forgerock.json.resource.servlet;

import static org.forgerock.json.resource.QueryResult.*;
import static org.forgerock.json.resource.servlet.HttpUtils.*;
import static org.forgerock.util.Utils.closeSilently;

import com.fasterxml.jackson.core.JsonGenerator;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.AdviceContext;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestVisitor;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceName;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.resource.core.Context;
import org.forgerock.util.encode.Base64url;
import org.forgerock.util.promise.AsyncFunction;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Common request processing.
 */
final class RequestRunner implements RequestVisitor<Promise<Void, NeverThrowsException>, Void> {

    // Connection set on handleResult(Connection).
    private Connection connection = null;
    private final Context context;
    private final HttpServletRequest httpRequest;
    private final HttpServletResponse httpResponse;
    private final Request request;
    private final JsonGenerator writer;

    RequestRunner(final Context context, final Request request,
            final HttpServletRequest httpRequest, final HttpServletResponse httpResponse) throws Exception {
        this.context = context;
        this.request = request;
        this.httpRequest = httpRequest;
        this.httpResponse = httpResponse;
        this.writer = getJsonGenerator(httpRequest, httpResponse);
    }

    public final Promise<Void, NeverThrowsException> handleError(final ResourceException error) {
        onError(error);
        return Promises.newSuccessfulPromise(null);
    }

    public final Promise<Void, NeverThrowsException> handleResult(final Connection result) {
        connection = result;

        // Dispatch request using visitor.
        return request.accept(this, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Promise<Void, NeverThrowsException> visitActionRequest(final Void p, final ActionRequest request) {
        return connection.actionAsync(context, request)
                .thenAsync(new AsyncFunction<JsonValue, Void, NeverThrowsException>() {
                    @Override
                    public Promise<Void, NeverThrowsException> apply(JsonValue result) throws NeverThrowsException {
                        try {
                            writeAdvice();
                            if (result != null) {
                                writeJsonValue(result);
                            } else {
                                // No content.
                                httpResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
                            }
                            onSuccess();
                        } catch (final Exception e) {
                            onError(e);
                        }

                        return Promises.newSuccessfulPromise(null);
                    }
                }, new AsyncFunction<ResourceException, Void, NeverThrowsException>() {
                    @Override
                    public Promise<Void, NeverThrowsException> apply(ResourceException e) throws NeverThrowsException {
                        return handleError(e);
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Promise<Void, NeverThrowsException> visitCreateRequest(final Void p, final CreateRequest request) {
        return connection.createAsync(context, request)
                .thenAsync(new AsyncFunction<Resource, Void, NeverThrowsException>() {
                    @Override
                    public Promise<Void, NeverThrowsException> apply(Resource result) throws NeverThrowsException {
                        try {
                            writeAdvice();
                            if (result.getId() != null) {
                                httpResponse.setHeader(HEADER_LOCATION, getResourceURL(request,
                                        result));
                            }
                            httpResponse.setStatus(HttpServletResponse.SC_CREATED);
                            writeResource(result);
                            onSuccess();
                        } catch (final Exception e) {
                            onError(e);
                        }
                        return Promises.newSuccessfulPromise(null);
                    }
                }, new AsyncFunction<ResourceException, Void, NeverThrowsException>() {
                    @Override
                    public Promise<Void, NeverThrowsException> apply(ResourceException e) throws NeverThrowsException {
                        return handleError(e);
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Promise<Void, NeverThrowsException> visitDeleteRequest(final Void p, final DeleteRequest request) {
        return connection.deleteAsync(context, request)
                .thenAsync(newResourceSuccessHandler(),
                        new AsyncFunction<ResourceException, Void, NeverThrowsException>() {
                            @Override
                            public Promise<Void, NeverThrowsException> apply(ResourceException e) throws NeverThrowsException {
                                return handleError(e);
                            }
                        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Promise<Void, NeverThrowsException> visitPatchRequest(final Void p, final PatchRequest request) {
        return connection.patchAsync(context, request)
                .thenAsync(newResourceSuccessHandler(),
                        new AsyncFunction<ResourceException, Void, NeverThrowsException>() {
                            @Override
                            public Promise<Void, NeverThrowsException> apply(ResourceException e) throws NeverThrowsException {
                                return handleError(e);
                            }
                        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Promise<Void, NeverThrowsException> visitQueryRequest(final Void p, final QueryRequest request) {
        return connection.queryAsync(context, request, new QueryResultHandler() {
            private boolean isFirstResult = true;
            private int resultCount = 0;

            @Override
            public void handleError(final ResourceException error) {
                if (isFirstResult) {
                    onError(error);
                } else {
                    // Partial results - it's too late to set the status.
                    try {
                        writer.writeEndArray();
                        writer.writeNumberField(FIELD_RESULT_COUNT, resultCount);
                        writer.writeObjectField(FIELD_ERROR, error.toJsonValue().getObject());
                        writer.writeEndObject();
                        onSuccess();
                    } catch (final Exception e) {
                        onError(e);
                    }
                }
            }

            @Override
            public boolean handleResource(final Resource resource) {
                try {
                    writeAdvice();
                    writeHeader();
                    writeJsonValue(resource.getContent());
                    resultCount++;
                    return true;
                } catch (final Exception e) {
                    handleError(adapt(e));
                    return false;
                }
            }

            @Override
            public void handleResult(final QueryResult result) {
                try {
                    writeHeader();
                    writer.writeEndArray();
                    writer.writeNumberField(FIELD_RESULT_COUNT, resultCount);
                    writer.writeStringField(FIELD_PAGED_RESULTS_COOKIE, result
                            .getPagedResultsCookie());
                    writer.writeNumberField(FIELD_REMAINING_PAGED_RESULTS, result
                            .getRemainingPagedResults());
                    writer.writeEndObject();
                    onSuccess();
                } catch (final Exception e) {
                    onError(e);
                }
            }

            private void writeHeader() throws IOException {
                if (isFirstResult) {
                    writer.writeStartObject();
                    writer.writeArrayFieldStart(FIELD_RESULT);
                    isFirstResult = false;
                }
            }
        })
                .thenAsync(new AsyncFunction<QueryResult, Void, NeverThrowsException>() {
                    @Override
                    public Promise<Void, NeverThrowsException> apply(QueryResult queryResult) throws NeverThrowsException {
                        return Promises.newSuccessfulPromise(null);
                    }
                }, new AsyncFunction<ResourceException, Void, NeverThrowsException>() {
                    @Override
                    public Promise<Void, NeverThrowsException> apply(ResourceException e) throws NeverThrowsException {
                        return Promises.newSuccessfulPromise(null);
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Promise<Void, NeverThrowsException> visitReadRequest(final Void p, final ReadRequest request) {
        return connection.readAsync(context, request)
                .thenAsync(newResourceSuccessHandler(),
                        new AsyncFunction<ResourceException, Void, NeverThrowsException>() {
                            @Override
                            public Promise<Void, NeverThrowsException> apply(ResourceException e) throws NeverThrowsException {
                                return handleError(e);
                            }
                        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Promise<Void, NeverThrowsException> visitUpdateRequest(final Void p, final UpdateRequest request) {
        return connection.updateAsync(context, request)
                .thenAsync(newResourceSuccessHandler(),
                        new AsyncFunction<ResourceException, Void, NeverThrowsException>() {
                            @Override
                            public Promise<Void, NeverThrowsException> apply(ResourceException e) throws NeverThrowsException {
                                return handleError(e);
                            }
                        });
    }

    private void onSuccess() {
        closeSilently(connection, writer);
    }

    private void onError(final Exception e) {
        // Don't close the JSON writer because the request will become
        // "completed" which then prevents us from sending an error.
        closeSilently(connection);
    }

    private String forceEmptyIfNull(final String s) {
        return s != null ? s : "";
    }

    private String getResourceURL(final CreateRequest request, final Resource resource) {
        final StringBuffer buffer = httpRequest.getRequestURL();

        // Strip out everything except the scheme and host.
        buffer.setLength(buffer.length() - httpRequest.getRequestURI().length());

        /*
         * Add back the context and servlet paths (these should never be null
         * but in some cases they are, e.g. when running Jetty from Maven).
         */
        buffer.append(forceEmptyIfNull(httpRequest.getContextPath()));
        buffer.append(forceEmptyIfNull(httpRequest.getServletPath()));

        // Add new resource name and resource ID.
        final ResourceName resourceName = request.getResourceNameObject();
        if (!resourceName.isEmpty()) {
            buffer.append('/');
            buffer.append(resourceName);
        }
        buffer.append('/');
        buffer.append(resource.getId());

        return buffer.toString();
    }

    private AsyncFunction<Resource, Void, NeverThrowsException> newResourceSuccessHandler() {
        return new AsyncFunction<Resource, Void, NeverThrowsException>() {
            @Override
            public Promise<Void, NeverThrowsException> apply(Resource result) throws NeverThrowsException {
                try {
                    writeAdvice();
                    // Don't return the resource if this is a read request and the
                    // If-None-Match header was specified.
                    if (request instanceof ReadRequest) {
                        final String rev = getIfNoneMatch(httpRequest);
                        if (rev != null && rev.equals(result.getRevision())) {
                            // No change so 304.
                            throw ResourceException.getException(304).setReason("Not Modified");
                        }
                    }

                    writeResource(result);
                    onSuccess();
                } catch (final Exception e) {
                    onError(e);
                }
                return Promises.newSuccessfulPromise(null);
            }
        };
    }

    private void writeJsonValue(final JsonValue json) throws IOException {
        writer.writeObject(json.getObject());
    }

    private void writeTextValue(final JsonValue json) throws IOException {
        if (json.isMap() && !json.asMap().isEmpty()) {
            writeToResponse(json.asMap().entrySet().iterator().next().getValue().toString().getBytes());
        } else if (json.isList() && !json.asList().isEmpty()) {
            writeToResponse(json.asList(String.class).iterator().next().getBytes());
        } else if (json.isString()) {
            writeToResponse(json.asString().getBytes());
        } else if (json.isBoolean()) {
            writeToResponse(json.asBoolean().toString().getBytes());
        } else if (json.isNumber()) {
            writeToResponse(json.asNumber().toString().getBytes());
        } else {
            throw new IOException("Content is unknown type or is empty");
        }
    }

    private void writeBinaryValue(final JsonValue json) throws IOException {
        if (json.isMap() && !json.asMap().isEmpty()) {
            writeToResponse(Base64url.decode(json.asMap().entrySet().iterator().next().getValue().toString()));
        } else if (json.isList() && !json.asList().isEmpty()) {
            writeToResponse(Base64url.decode(json.asList(String.class).iterator().next()));
        } else if (json.isString()) {
            writeToResponse(Base64url.decode(json.asString()));
        } else {
            throw new IOException("Content is not an accepted type or is empty");
        }
    }

    private void writeToResponse(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            throw new IOException("Content is empty or corrupt");
        }
        httpResponse.setContentLength(data.length);
        httpResponse.getOutputStream().write(data);
    }

    private void writeResource(final Resource resource) throws IOException, ParseException {
        if (resource.getRevision() != null) {
            final StringBuilder builder = new StringBuilder();
            builder.append('"');
            builder.append(resource.getRevision());
            builder.append('"');
            httpResponse.setHeader(HEADER_ETAG, builder.toString());
        }

        ContentType contentType = new ContentType(httpResponse.getContentType());

        if (contentType.match(MIME_TYPE_APPLICATION_JSON)) {
            writeJsonValue(resource.getContent());
        } else if (contentType.match(MIME_TYPE_TEXT_PLAIN)) {
            writeTextValue(resource.getContent());
        } else {
            writeBinaryValue(resource.getContent());
        }
    }

    private void writeAdvice() {
        if (context.containsContext(AdviceContext.class)) {
            AdviceContext adviceContext = context.asContext(AdviceContext.class);
            for (Map.Entry<String, List<String>> entry : adviceContext.getAdvices().entrySet()) {
                for (String value : entry.getValue()) {
                    httpResponse.setHeader(entry.getKey(), value);
                }
            }
        }
    }
}
