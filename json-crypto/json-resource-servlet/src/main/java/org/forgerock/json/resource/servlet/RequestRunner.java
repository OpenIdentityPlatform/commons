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
 * Copyright 2012 ForgeRock AS.
 */
package org.forgerock.json.resource.servlet;

import static org.forgerock.json.resource.servlet.HttpUtils.CHARACTER_ENCODING;
import static org.forgerock.json.resource.servlet.HttpUtils.CONTENT_TYPE;
import static org.forgerock.json.resource.servlet.HttpUtils.HEADER_ETAG;
import static org.forgerock.json.resource.servlet.HttpUtils.HEADER_LOCATION;
import static org.forgerock.json.resource.servlet.HttpUtils.PARAM_PRETTY_PRINT;
import static org.forgerock.json.resource.servlet.HttpUtils.adapt;
import static org.forgerock.json.resource.servlet.HttpUtils.asBooleanValue;
import static org.forgerock.json.resource.servlet.HttpUtils.closeQuietly;
import static org.forgerock.json.resource.servlet.HttpUtils.fail;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.JsonFactory;
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
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.exception.ResourceException;

/**
 * Common request processing used by {@link RequestDispatcher}s.
 */
abstract class RequestRunner implements ResultHandler<Connection>, RequestVisitor<Void, Void> {

    // Connection set on handleResult(Connection).
    private Connection connection = null;

    private final Context context;
    private final HttpServletRequest httpRequest;
    private final HttpServletResponse httpResponse;
    private final OutputStream os;
    private final Request request;
    private final JsonGenerator writer;

    RequestRunner(final Context context, final Request request,
            final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
            final JsonFactory jsonFactory) throws Exception {
        this.context = context;
        this.request = request;
        this.httpRequest = httpRequest;
        this.httpResponse = httpResponse;

        // Configure the JSON writer.
        this.os = httpResponse.getOutputStream();
        this.writer = jsonFactory.createJsonGenerator(os);

        // Enable pretty printer if requested.
        final String[] values = httpRequest.getParameterValues(PARAM_PRETTY_PRINT);
        if (values != null) {
            if (asBooleanValue(PARAM_PRETTY_PRINT, values)) {
                writer.useDefaultPrettyPrinter();
            }
        }
        httpResponse.setContentType(CONTENT_TYPE);
        httpResponse.setCharacterEncoding(CHARACTER_ENCODING);
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
                        writer.writeObject(result.getObject());
                    } else {
                        // No content.
                        httpResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    }
                } catch (final Exception e) {
                    fail(httpResponse, e);
                } finally {
                    complete();
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
                    httpResponse.setHeader(HEADER_LOCATION, getResourceURL(request, result));
                    httpResponse.setStatus(HttpServletResponse.SC_CREATED);
                    writeResource(result);
                } catch (final Exception e) {
                    fail(httpResponse, e);
                } finally {
                    complete();
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
            private boolean needsHeader = true;

            /**
             * {@inheritDoc}
             */
            @Override
            public void handleError(final ResourceException error) {
                if (!needsHeader) {
                    // Partial results.
                    try {
                        writer.writeEndArray();
                        writer.writeObjectFieldStart("error");
                        writer.writeNumberField("code", error.getCode());
                        writer.writeStringField("message", error.getMessage());
                        writer.writeEndObject();
                        writer.writeEndObject();
                    } catch (final IOException e) {
                        // FIXME: can we do anything with this?
                    }
                }
                onError(error);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean handleResource(final Resource resource) {
                try {
                    writeHeader();
                    writer.writeObject(resource.getContent().getObject());
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
                    writer.writeStringField("paged-results-cookie", result.getPagedResultsCookie());
                    writer.writeNumberField("remaining-paged-results", result
                            .getRemainingPagedResults());
                    writer.writeEndObject();
                } catch (final Exception e) {
                    fail(httpResponse, e);
                } finally {
                    complete();
                }
            }

            private void writeHeader() throws IOException {
                if (needsHeader) {
                    writer.writeStartObject();
                    writer.writeArrayFieldStart("result");
                    needsHeader = false;
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

    /**
     * Performs post-completion processing such as completing the AsyncContext
     * (Servlet3) or a latch (Servlet2).
     */
    protected abstract void onComplete();

    private void complete() {
        try {
            closeQuietly(connection, writer, os);
        } finally {
            onComplete();
        }
    }

    private String getResourceURL(final CreateRequest request, final Resource resource) {
        final StringBuffer buffer = httpRequest.getRequestURL();

        // Strip out everything except the scheme and host.
        buffer.setLength(buffer.length() - httpRequest.getRequestURI().length());

        // Add back the context and servlet paths.
        buffer.append(httpRequest.getContextPath());
        buffer.append(httpRequest.getServletPath());

        // Add new component and resource ID.
        buffer.append(request.getComponent());
        buffer.append('/');
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
                    writeResource(result);
                } catch (final Exception e) {
                    fail(httpResponse, e);
                } finally {
                    complete();
                }
            }
        };
    }

    private void onError(final Exception e) {
        try {
            fail(httpResponse, e);
        } finally {
            complete();
        }
    }

    private void writeResource(final Resource resource) throws IOException {
        if (resource.getRevision() != null) {
            final StringBuilder builder = new StringBuilder();
            builder.append('"');
            builder.append(resource.getRevision());
            builder.append('"');
            httpResponse.setHeader(HEADER_ETAG, builder.toString());
        }
        writer.writeObject(resource.getContent().getObject());
    }
}
