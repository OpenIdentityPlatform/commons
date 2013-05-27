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
 * Copyright 2012-2013 ForgeRock AS.
 */
package org.forgerock.json.resource.servlet;

import static org.forgerock.json.resource.QueryResult.FIELD_ERROR;
import static org.forgerock.json.resource.QueryResult.FIELD_PAGED_RESULTS_COOKIE;
import static org.forgerock.json.resource.QueryResult.FIELD_REMAINING_PAGED_RESULTS;
import static org.forgerock.json.resource.QueryResult.FIELD_RESULT;
import static org.forgerock.json.resource.QueryResult.FIELD_RESULT_COUNT;
import static org.forgerock.json.resource.servlet.HttpUtils.HEADER_ETAG;
import static org.forgerock.json.resource.servlet.HttpUtils.HEADER_LOCATION;
import static org.forgerock.json.resource.servlet.HttpUtils.adapt;
import static org.forgerock.json.resource.servlet.HttpUtils.closeQuietly;
import static org.forgerock.json.resource.servlet.HttpUtils.getIfNoneMatch;
import static org.forgerock.json.resource.servlet.HttpUtils.getJsonGenerator;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.JsonGenerator;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.Context;
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
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.UpdateRequest;

/**
 * Common request processing.
 */
final class RequestRunner implements ResultHandler<Connection>, RequestVisitor<Void, Void> {

    // Connection set on handleResult(Connection).
    private Connection connection = null;
    private final Context context;
    private final HttpServletRequest httpRequest;
    private final HttpServletResponse httpResponse;
    private final Request request;
    private final JsonGenerator writer;
    private final ServletSynchronizer sync;

    RequestRunner(final Context context, final Request request,
            final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
            final ServletSynchronizer sync) throws Exception {
        this.context = context;
        this.request = request;
        this.httpRequest = httpRequest;
        this.httpResponse = httpResponse;
        this.writer = getJsonGenerator(httpRequest, httpResponse);
        this.sync = sync;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void handleError(final ResourceException error) {
        onError(error);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void handleResult(final Connection result) {
        connection = result;

        // Dispatch request using visitor.
        request.accept(this, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Void visitActionRequest(final Void p, final ActionRequest request) {
        connection.actionAsync(context, request, new ResultHandler<JsonValue>() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void handleError(final ResourceException error) {
                onError(error);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void handleResult(final JsonValue result) {
                try {
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
            }
        });
        return null; // return Void.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Void visitCreateRequest(final Void p, final CreateRequest request) {
        connection.createAsync(context, request, new ResultHandler<Resource>() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void handleError(final ResourceException error) {
                onError(error);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void handleResult(final Resource result) {
                try {
                    if (result.getId() != null) {
                        httpResponse.setHeader(HEADER_LOCATION, getResourceURL(request, result));
                    }
                    httpResponse.setStatus(HttpServletResponse.SC_CREATED);
                    writeResource(result);
                    onSuccess();
                } catch (final Exception e) {
                    onError(e);
                }
            }
        });
        return null; // return Void.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Void visitDeleteRequest(final Void p, final DeleteRequest request) {
        connection.deleteAsync(context, request, newResourceResultHandler());
        return null; // return Void.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Void visitPatchRequest(final Void p, final PatchRequest request) {
        connection.patchAsync(context, request, newResourceResultHandler());
        return null; // return Void.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Void visitQueryRequest(final Void p, final QueryRequest request) {
        connection.queryAsync(context, request, new QueryResultHandler() {
            private boolean isFirstResult = true;
            private int resultCount = 0;

            /**
             * {@inheritDoc}
             */
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

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean handleResource(final Resource resource) {
                try {
                    writeHeader();
                    writeJsonValue(resource.getContent());
                    resultCount++;
                    return true;
                } catch (final Exception e) {
                    handleError(adapt(e));
                    return false;
                }
            }

            /**
             * {@inheritDoc}
             */
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
        });
        return null; // return Void.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Void visitReadRequest(final Void p, final ReadRequest request) {
        connection.readAsync(context, request, newResourceResultHandler());
        return null; // return Void.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Void visitUpdateRequest(final Void p, final UpdateRequest request) {
        connection.updateAsync(context, request, newResourceResultHandler());
        return null; // return Void.
    }

    private void onSuccess() {
        try {
            closeQuietly(connection, writer);
        } finally {
            sync.signalAndComplete();
        }
    }

    private void onError(final Exception e) {
        try {
            // Don't close the JSON writer because the request will become
            // "completed" which then prevents us from sending an error.
            closeQuietly(connection);
        } finally {
            sync.signalAndComplete(e);
        }
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
        buffer.append(request.getResourceName());
        if (!request.getResourceName().endsWith("/")) {
            buffer.append('/');
        }
        buffer.append(resource.getId());

        return buffer.toString();
    }

    private ResultHandler<Resource> newResourceResultHandler() {
        return new ResultHandler<Resource>() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void handleError(final ResourceException error) {
                onError(error);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void handleResult(final Resource result) {
                try {
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
            }
        };
    }

    private void writeJsonValue(final JsonValue json) throws IOException {
        writer.writeObject(json.getObject());
    }

    private void writeResource(final Resource resource) throws IOException {
        if (resource.getRevision() != null) {
            final StringBuilder builder = new StringBuilder();
            builder.append('"');
            builder.append(resource.getRevision());
            builder.append('"');
            httpResponse.setHeader(HEADER_ETAG, builder.toString());
        }
        writeJsonValue(resource.getContent());
    }
}
