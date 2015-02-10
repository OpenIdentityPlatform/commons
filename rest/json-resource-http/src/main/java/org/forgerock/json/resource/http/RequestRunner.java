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

package org.forgerock.json.resource.http;

import static org.forgerock.json.resource.QueryResult.*;
import static org.forgerock.json.resource.http.HttpUtils.*;
import static org.forgerock.util.Utils.closeSilently;

import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;

import org.forgerock.http.Context;
import org.forgerock.http.RouterContext;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.ResponseException;
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
import org.forgerock.util.encode.Base64url;
import org.forgerock.util.promise.AsyncFunction;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * Common request processing.
 */
final class RequestRunner implements RequestVisitor<Promise<Response, ResponseException>, Void> {

    // Connection set on handleResult(Connection).
    private Connection connection = null;
    private final Context context;
    private final org.forgerock.http.protocol.Request httpRequest;
    private final Response httpResponse;
    private final Request request;
    private final JsonGenerator writer;

    RequestRunner(Context context, Request request, org.forgerock.http.protocol.Request httpRequest, Response httpResponse)
            throws Exception {
        this.context = context;
        this.request = request;
        this.httpRequest = httpRequest;
        this.httpResponse = httpResponse;
        this.writer = getJsonGenerator(httpRequest, httpResponse);
    }

    public final Promise<Response, ResponseException> handleError(final ResourceException error) {
        onError(error);
        return fail(httpRequest, error);
    }

    public final Promise<Response, ResponseException> handleResult(final Connection result) {
        connection = result;

        // Dispatch request using visitor.
        return request.accept(this, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Promise<Response, ResponseException> visitActionRequest(final Void p, final ActionRequest request) {
        return connection.actionAsync(context, request)
                .thenAsync(new AsyncFunction<JsonValue, Response, ResponseException>() {
                    @Override
                    public Promise<Response, ResponseException> apply(JsonValue result) throws ResponseException {
                        try {
                            writeAdvice();
                            if (result != null) {
                                writeJsonValue(result);
                            } else {
                                // No content.
                                httpResponse.setStatus(204);
                            }
                            onSuccess();
                        } catch (final Exception e) {
                            onError(e);
                        }

                        return Promises.newSuccessfulPromise(httpResponse);
                    }
                }, new AsyncFunction<ResourceException, Response, ResponseException>() {
                    @Override
                    public Promise<Response, ResponseException> apply(ResourceException e) throws ResponseException {
                        return handleError(e);
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Promise<Response, ResponseException> visitCreateRequest(final Void p, final CreateRequest request) {
        return connection.createAsync(context, request)
                .thenAsync(new AsyncFunction<Resource, Response, ResponseException>() {
                    @Override
                    public Promise<Response, ResponseException> apply(Resource result) throws ResponseException {
                        try {
                            writeAdvice();
                            if (result.getId() != null) {
                                httpResponse.getHeaders().putSingle(HEADER_LOCATION, getResourceURL(request,
                                        result));
                            }
                            httpResponse.setStatus(201);
                            writeResource(result);
                            onSuccess();
                        } catch (final Exception e) {
                            onError(e);
                        }
                        return Promises.newSuccessfulPromise(httpResponse);
                    }
                }, new AsyncFunction<ResourceException, Response, ResponseException>() {
                    @Override
                    public Promise<Response, ResponseException> apply(ResourceException e) throws ResponseException {
                        return handleError(e);
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Promise<Response, ResponseException> visitDeleteRequest(final Void p, final DeleteRequest request) {
        return connection.deleteAsync(context, request)
                .thenAsync(newResourceSuccessHandler(),
                        new AsyncFunction<ResourceException, Response, ResponseException>() {
                            @Override
                            public Promise<Response, ResponseException> apply(ResourceException e) throws ResponseException {
                                return handleError(e);
                            }
                        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Promise<Response, ResponseException> visitPatchRequest(final Void p, final PatchRequest request) {
        return connection.patchAsync(context, request)
                .thenAsync(newResourceSuccessHandler(),
                        new AsyncFunction<ResourceException, Response, ResponseException>() {
                            @Override
                            public Promise<Response, ResponseException> apply(ResourceException e) throws ResponseException {
                                return handleError(e);
                            }
                        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Promise<Response, ResponseException> visitQueryRequest(final Void p, final QueryRequest request) {
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
        }).thenAsync(new AsyncFunction<QueryResult, Response, ResponseException>() {
            @Override
            public Promise<Response, ResponseException> apply(QueryResult queryResult) throws ResponseException {
                return Promises.newSuccessfulPromise(httpResponse);
            }
        }, new AsyncFunction<ResourceException, Response, ResponseException>() {
            @Override
            public Promise<Response, ResponseException> apply(ResourceException e) throws ResponseException {
                return handleError(e);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Promise<Response, ResponseException> visitReadRequest(final Void p, final ReadRequest request) {
        return connection.readAsync(context, request)
                .thenAsync(newResourceSuccessHandler(),
                        new AsyncFunction<ResourceException, Response, ResponseException>() {
                            @Override
                            public Promise<Response, ResponseException> apply(ResourceException e) throws ResponseException {
                                return handleError(e);
                            }
                        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Promise<Response, ResponseException> visitUpdateRequest(final Void p, final UpdateRequest request) {
        return connection.updateAsync(context, request)
                .thenAsync(newResourceSuccessHandler(),
                        new AsyncFunction<ResourceException, Response, ResponseException>() {
                            @Override
                            public Promise<Response, ResponseException> apply(ResourceException e) throws ResponseException {
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

    private String getResourceURL(final CreateRequest request, final Resource resource) {
        // Strip out everything except the scheme and host.
        StringBuilder builder = new StringBuilder()
                .append(httpRequest.getUri().getScheme())
                .append("://")
                .append(httpRequest.getUri().getRawAuthority());

        // Add back the context path.
        builder.append(context.asContext(RouterContext.class).getMatchedUri());

        // Add new resource name and resource ID.
        final ResourceName resourceName = request.getResourceNameObject();
        if (!resourceName.isEmpty()) {
            builder.append('/');
            builder.append(resourceName);
        }
        builder.append('/');
        builder.append(resource.getId());

        return builder.toString();
    }

    private AsyncFunction<Resource, Response, ResponseException> newResourceSuccessHandler() {
        return new AsyncFunction<Resource, Response, ResponseException>() {
            @Override
            public Promise<Response, ResponseException> apply(Resource result) {
                try {
                    writeAdvice();
                    // Don't return the resource if this is a read request and the
                    // If-None-Match header was specified.
                    if (request instanceof ReadRequest) {
                        final String rev = getIfNoneMatch(httpRequest);
                        if (rev != null && rev.equals(result.getRevision())) {
                            // No change so 304.
                            Map<String, Object> responseBody = ResourceException.getException(304)
                                    .setReason("Not Modified").toJsonValue().asMap();
                            return Promises.newSuccessfulPromise(new Response().setStatusAndReason(304)
                                    .setEntity(responseBody));
                        }
                    }
                    writeResource(result);
                    onSuccess();
                } catch (final Exception e) {
                    onError(e);
                }
                return Promises.newSuccessfulPromise(httpResponse);
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
        httpResponse.setEntity(data);
    }

    private void writeResource(final Resource resource) throws IOException, ParseException {
        if (resource.getRevision() != null) {
            final StringBuilder builder = new StringBuilder();
            builder.append('"');
            builder.append(resource.getRevision());
            builder.append('"');
            httpResponse.getHeaders().putSingle(HEADER_ETAG, builder.toString());
        }

        ContentType contentType = new ContentType(ContentTypeHeader.valueOf(httpResponse).toString());

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
                httpResponse.getHeaders().put(entry.getKey(), entry.getValue());
            }
        }
    }
}
