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
import org.forgerock.json.resource.ConnectionFactory;
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
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.exception.ResourceException;

/**
 * Servlet 2.x request dispatcher which processes requests synchronously using
 * blocking IO.
 */
final class Servlet2RequestDispatcher implements RequestDispatcher {

    private static final class QueryResultHandlerImpl implements QueryResultHandler {
        private boolean needsHeader = true;
        private ResourceException resourceException = null;

        private final JsonGenerator writer;

        private QueryResultHandlerImpl(final JsonGenerator writer) {
            this.writer = writer;
        }

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
            } else {
                // No results received.
                resourceException = error;
            }
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
            } catch (final IOException e) {
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
            } catch (final IOException e) {
                handleError(adapt(e));
            }
        }

        private ResourceException getResourceException() {
            return resourceException;
        }

        private void writeHeader() throws IOException {
            if (needsHeader) {
                writer.writeStartObject();
                writer.writeArrayFieldStart("results");
                needsHeader = false;
            }
        }
    }

    private static final class Runner implements RequestVisitor<ResourceException, Void> {
        private final Connection connection;
        private final Context context;
        private final HttpServletRequest httpRequest;
        private final HttpServletResponse httpResponse;
        private final JsonGenerator writer;

        private Runner(final Connection connection, final Context context,
                final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
                final JsonGenerator writer) {
            this.connection = connection;
            this.httpRequest = httpRequest;
            this.httpResponse = httpResponse;
            this.context = context;
            this.writer = writer;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ResourceException visitActionRequest(final Void p, final ActionRequest request) {
            try {
                final JsonValue result = connection.action(context, request);
                if (result != null) {
                    writer.writeObject(result.getObject());
                } else {
                    // No content.
                    httpResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
                }
                return null;
            } catch (final IOException e) {
                return adapt(e);
            } catch (final ResourceException e) {
                return e;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ResourceException visitCreateRequest(final Void p, final CreateRequest request) {
            try {
                final Resource resource = connection.create(context, request);
                httpResponse.setHeader(HEADER_LOCATION, getResourceURL(request, resource));
                httpResponse.setStatus(HttpServletResponse.SC_CREATED);
                return writeResource(resource);
            } catch (final ResourceException e) {
                return e;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ResourceException visitDeleteRequest(final Void p, final DeleteRequest request) {
            try {
                return writeResource(connection.delete(context, request));
            } catch (final ResourceException e) {
                return e;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ResourceException visitPatchRequest(final Void p, final PatchRequest request) {
            try {
                return writeResource(connection.patch(context, request));
            } catch (final ResourceException e) {
                return e;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ResourceException visitQueryRequest(final Void p, final QueryRequest request) {
            try {
                final QueryResultHandlerImpl handler = new QueryResultHandlerImpl(writer);
                connection.query(context, request, handler);
                return handler.getResourceException();
            } catch (final ResourceException e) {
                return e;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ResourceException visitReadRequest(final Void p, final ReadRequest request) {
            try {
                return writeResource(connection.read(context, request));
            } catch (final ResourceException e) {
                return e;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ResourceException visitUpdateRequest(final Void p, final UpdateRequest request) {
            try {
                return writeResource(connection.update(context, request));
            } catch (final ResourceException e) {
                return e;
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

        private ResourceException writeResource(final Resource resource) {
            if (resource.getRevision() != null) {
                final StringBuilder builder = new StringBuilder();
                builder.append('"');
                builder.append(resource.getRevision());
                builder.append('"');
                httpResponse.setHeader(HEADER_ETAG, builder.toString());
            }
            try {
                writer.writeObject(resource.getContent().getObject());
                return null;
            } catch (final IOException e) {
                return adapt(e);
            }
        }

    }

    private final ConnectionFactory connectionFactory;
    private final JsonFactory jsonFactory;

    /**
     * Creates a new Servlet 2.x request dispatcher.
     *
     * @param connectionFactory
     *            The underlying connection factory to which requests should be
     *            dispatched.
     * @param jsonFactory
     *            The JSON factory which should be used for writing and reading
     *            JSON.
     */
    Servlet2RequestDispatcher(final ConnectionFactory connectionFactory,
            final JsonFactory jsonFactory) {
        this.connectionFactory = connectionFactory;
        this.jsonFactory = jsonFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispatchRequest(final Context context, final Request request,
            final HttpServletRequest httpRequest, final HttpServletResponse httpResponse) {
        Connection connection = null;
        OutputStream os = null;
        JsonGenerator writer = null;
        try {
            connection = connectionFactory.getConnection();
            os = httpResponse.getOutputStream();
            writer = jsonFactory.createJsonGenerator(os);

            // Enable pretty printer if requested.
            final String[] values = httpRequest.getParameterValues(PARAM_PRETTY_PRINT);
            if (values != null) {
                if (asBooleanValue(PARAM_PRETTY_PRINT, values)) {
                    writer.useDefaultPrettyPrinter();
                }
            }

            httpResponse.setContentType(CONTENT_TYPE);
            httpResponse.setCharacterEncoding(CHARACTER_ENCODING);

            final Runner runner =
                    new Runner(connection, context, httpRequest, httpResponse, writer);
            final ResourceException e = request.accept(runner, null);
            if (e != null) {
                throw e;
            }
        } catch (final ResourceException e) {
            fail(httpResponse, e);
        } catch (final IOException e) {
            fail(httpResponse, e);
        } finally {
            closeQuietly(connection, writer, os);
        }
    }

}
