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
 * Copyright 2012-2016 ForgeRock AS.
 */

package org.forgerock.json.resource.http;

import static org.forgerock.json.resource.QueryResponse.FIELD_ERROR;
import static org.forgerock.json.resource.QueryResponse.FIELD_PAGED_RESULTS_COOKIE;
import static org.forgerock.json.resource.QueryResponse.FIELD_REMAINING_PAGED_RESULTS;
import static org.forgerock.json.resource.QueryResponse.FIELD_RESULT;
import static org.forgerock.json.resource.QueryResponse.FIELD_RESULT_COUNT;
import static org.forgerock.json.resource.QueryResponse.FIELD_TOTAL_PAGED_RESULTS;
import static org.forgerock.json.resource.QueryResponse.FIELD_TOTAL_PAGED_RESULTS_POLICY;
import static org.forgerock.json.resource.Requests.newUpdateRequest;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_REVISION;
import static org.forgerock.json.resource.http.HttpUtils.HEADER_ETAG;
import static org.forgerock.json.resource.http.HttpUtils.HEADER_LOCATION;
import static org.forgerock.json.resource.http.HttpUtils.JSON_MAPPER;
import static org.forgerock.json.resource.http.HttpUtils.MIME_TYPE_APPLICATION_JSON;
import static org.forgerock.json.resource.http.HttpUtils.MIME_TYPE_TEXT_PLAIN;
import static org.forgerock.json.resource.http.HttpUtils.PROTOCOL_VERSION_2;
import static org.forgerock.json.resource.http.HttpUtils.adapt;
import static org.forgerock.json.resource.http.HttpUtils.fail;
import static org.forgerock.json.resource.http.HttpUtils.getIfNoneMatch;
import static org.forgerock.json.resource.http.HttpUtils.getJsonGenerator;
import static org.forgerock.json.resource.http.HttpUtils.getRequestedProtocolVersion;
import static org.forgerock.util.Utils.closeSilently;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;

import org.forgerock.http.header.ContentApiVersionHeader;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.header.MalformedHeaderException;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.http.routing.Version;
import org.forgerock.http.util.Json;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.AdviceContext;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestVisitor;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.services.context.Context;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.encode.Base64url;
import org.forgerock.util.promise.ExceptionHandler;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.ResultHandler;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * Common request processing.
 */
final class RequestRunner implements RequestVisitor<Promise<Response, NeverThrowsException>, Void> {

    // Connection set on handleResult(Connection).
    private Connection connection = null;
    private final Context context;
    private final org.forgerock.http.protocol.Request httpRequest;
    private final Response httpResponse;
    private final Version protocolVersion;
    private final Request request;
    private final JsonGenerator jsonGenerator;

    RequestRunner(Context context, Request request, org.forgerock.http.protocol.Request httpRequest,
            Response httpResponse) throws Exception {
        this.context = context;
        this.request = request;
        this.httpRequest = httpRequest;
        this.httpResponse = httpResponse;
        // cache the request's protocol version to avoid repeated BadRequestExceptions at call-sites
        this.protocolVersion = getRequestedProtocolVersion(httpRequest);
        this.jsonGenerator = getJsonGenerator(httpRequest, httpResponse);
    }

    /**
     * Determine if upsert is supported for this request.
     *
     * @param request the CreateRequest that failed
     * @return whether we can instead try an update for the failed create
     */
    private boolean isUpsertSupported(final CreateRequest request) {
        // protocol version 2 supports upsert -- update on create-failure
        return (protocolVersion.getMajor() >= 2
                && getIfNoneMatch(httpRequest) == null
                && request.getNewResourceId() != null);
    }

    public final Promise<Response, NeverThrowsException> handleError(final ResourceException error) {
        onError(error);
        writeApiVersionHeaders(error);
        writeAdvice();
        return fail(httpRequest, httpResponse, error);
    }

    public final Promise<Response, NeverThrowsException> handleResult(final Connection result) {
        connection = result;

        // Dispatch request using visitor.
        return request.accept(this, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Promise<Response, NeverThrowsException> visitActionRequest(final Void p, final ActionRequest request) {
        return connection.actionAsync(context, request)
                .thenAsync(new AsyncFunction<ActionResponse, Response, NeverThrowsException>() {
                    @Override
                    public Promise<Response, NeverThrowsException> apply(ActionResponse result) {
                        try {
                            writeApiVersionHeaders(result);
                            writeAdvice();
                            if (result != null) {
                                Json.makeLocalizingObjectWriter(JSON_MAPPER, httpRequest)
                                        .writeValue(jsonGenerator, result.getJsonContent().getObject());
                            } else {
                                // No content.
                                httpResponse.setStatus(Status.NO_CONTENT);
                            }
                            onSuccess();
                        } catch (final Exception e) {
                            onError(e);
                        }

                        return newResultPromise(httpResponse);
                    }
                }, new AsyncFunction<ResourceException, Response, NeverThrowsException>() {
                    @Override
                    public Promise<Response, NeverThrowsException> apply(ResourceException e) {
                        return handleError(e);
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Promise<Response, NeverThrowsException> visitCreateRequest(final Void p, final CreateRequest request) {
        return connection.createAsync(context, request)
                .thenAsync(new AsyncFunction<ResourceResponse, Response, NeverThrowsException>() {
                    @Override
                    public Promise<Response, NeverThrowsException> apply(ResourceResponse result) {
                        try {
                            writeApiVersionHeaders(result);
                            writeAdvice();
                            if (result.getId() != null) {
                                httpResponse.getHeaders().put(HEADER_LOCATION, getResourceURL(result));
                            }
                            httpResponse.setStatus(Status.CREATED);
                            writeResource(result);
                            onSuccess();
                        } catch (final Exception e) {
                            onError(e);
                        }
                        return newResultPromise(httpResponse);
                    }
                }, new AsyncFunction<ResourceException, Response, NeverThrowsException>() {
                    @Override
                    public Promise<Response, NeverThrowsException> apply(ResourceException resourceException) {
                        try {
                            // treat as update to existing resource (if supported)
                            // if create failed because object already exists
                            if (resourceException instanceof PreconditionFailedException
                                    && isUpsertSupported(request)) {
                                return visitUpdateRequest(p,
                                        newUpdateRequest(
                                                request.getResourcePathObject().child(request.getNewResourceId()),
                                                request.getContent()));
                            } else {
                                return handleError(resourceException);
                            }
                        } catch (Exception e) {
                            onError(e);
                        }
                        return newResultPromise(httpResponse);
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Promise<Response, NeverThrowsException> visitDeleteRequest(final Void p, final DeleteRequest request) {
        return connection.deleteAsync(context, request)
                .thenAsync(newResourceSuccessHandler(),
                        new AsyncFunction<ResourceException, Response, NeverThrowsException>() {
                            @Override
                            public Promise<Response, NeverThrowsException> apply(ResourceException e) {
                                return handleError(e);
                            }
                        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Promise<Response, NeverThrowsException> visitPatchRequest(final Void p, final PatchRequest request) {
        return connection.patchAsync(context, request)
                .thenAsync(newResourceSuccessHandler(),
                        new AsyncFunction<ResourceException, Response, NeverThrowsException>() {
                            @Override
                            public Promise<Response, NeverThrowsException> apply(ResourceException e) {
                                return handleError(e);
                            }
                        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Promise<Response, NeverThrowsException> visitQueryRequest(final Void p, final QueryRequest request) {
        final AtomicBoolean isFirstResult = new AtomicBoolean(true);
        final AtomicInteger resultCount = new AtomicInteger(0);
        return connection.queryAsync(context, request, new QueryResourceHandler() {
            @Override
            public boolean handleResource(final ResourceResponse resource) {
                try {
                    writeHeader(resource, isFirstResult);
                    writeResourceJsonContent(resource);
                    resultCount.incrementAndGet();
                    return true;
                } catch (final Exception e) {
                    handleError(adapt(e));
                    return false;
                }
            }
        }).thenOnResult(new ResultHandler<QueryResponse>() {
            @Override
            public void handleResult(QueryResponse result) {
                try {
                    writeHeader(result, isFirstResult);
                    jsonGenerator.writeEndArray();
                    jsonGenerator.writeNumberField(FIELD_RESULT_COUNT, resultCount.get());
                    jsonGenerator.writeStringField(FIELD_PAGED_RESULTS_COOKIE, result.getPagedResultsCookie());
                    jsonGenerator.writeStringField(FIELD_TOTAL_PAGED_RESULTS_POLICY,
                            result.getTotalPagedResultsPolicy().toString());
                    jsonGenerator.writeNumberField(FIELD_TOTAL_PAGED_RESULTS, result.getTotalPagedResults());
                    // Remaining is only present for backwards compatibility with CREST2 via Accept-API-Version
                    jsonGenerator.writeNumberField(FIELD_REMAINING_PAGED_RESULTS, result.getRemainingPagedResults());
                    jsonGenerator.writeEndObject();
                    onSuccess();
                } catch (final Exception e) {
                    onError(e);
                }
            }
        }).thenOnException(new ExceptionHandler<ResourceException>() {
            @Override
            public void handleException(ResourceException error) {
                if (isFirstResult.get()) {
                    onError(error);
                } else {
                    // Partial results - it's too late to set the status.
                    try {
                        jsonGenerator.writeEndArray();
                        jsonGenerator.writeNumberField(FIELD_RESULT_COUNT, resultCount.get());
                        jsonGenerator.writeObjectField(FIELD_ERROR, error.toJsonValue().getObject());
                        jsonGenerator.writeEndObject();
                        onSuccess();
                    } catch (final Exception e) {
                        onError(e);
                    }
                }
            }
        }).thenAsync(new AsyncFunction<QueryResponse, Response, NeverThrowsException>() {
            @Override
            public Promise<Response, NeverThrowsException> apply(QueryResponse queryResponse) {
                return newResultPromise(httpResponse);
            }
        }, new AsyncFunction<ResourceException, Response, NeverThrowsException>() {
            @Override
            public Promise<Response, NeverThrowsException> apply(ResourceException e) {
                return handleError(e);
            }
        });
    }

    private void writeHeader(org.forgerock.json.resource.Response response, AtomicBoolean isFirstResult)
            throws IOException {
        if (isFirstResult.compareAndSet(true, false)) {
            writeApiVersionHeaders(response);
            writeAdvice();
            jsonGenerator.writeStartObject();
            jsonGenerator.writeArrayFieldStart(FIELD_RESULT);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Promise<Response, NeverThrowsException> visitReadRequest(final Void p, final ReadRequest request) {
        return connection.readAsync(context, request)
                .thenAsync(newResourceSuccessHandler(),
                        new AsyncFunction<ResourceException, Response, NeverThrowsException>() {
                            @Override
                            public Promise<Response, NeverThrowsException> apply(ResourceException e) {
                                return handleError(e);
                            }
                        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Promise<Response, NeverThrowsException> visitUpdateRequest(final Void p, final UpdateRequest request) {
        return connection.updateAsync(context, request)
                .thenAsync(newResourceSuccessHandler(),
                        new AsyncFunction<ResourceException, Response, NeverThrowsException>() {
                            @Override
                            public Promise<Response, NeverThrowsException> apply(ResourceException e) {
                                return handleError(e);
                            }
                        });
    }

    private void onSuccess() {
        closeSilently(connection, jsonGenerator);
    }

    private void onError(final Exception e) {
        // Don't close the JSON generator because the request will become
        // "completed" which then prevents us from sending an error.
        closeSilently(connection);
    }

    private String getResourceURL(final ResourceResponse resource) {
        // Strip out everything except the scheme and host.
        StringBuilder builder = new StringBuilder()
                .append(httpRequest.getUri().getScheme())
                .append("://")
                .append(httpRequest.getUri().getRawAuthority());

        // Add back the full context path.
        String baseUri = context.asContext(UriRouterContext.class).getBaseUri();
        if (!baseUri.isEmpty()) {
            if (!baseUri.startsWith("/")) {
                builder.append('/');
            }
            builder.append(baseUri);
        }

        // Add resource ID.
        builder.append('/');
        builder.append(resource.getId());

        return builder.toString();
    }

    private AsyncFunction<ResourceResponse, Response, NeverThrowsException> newResourceSuccessHandler() {
        return new AsyncFunction<ResourceResponse, Response, NeverThrowsException>() {
            @Override
            public Promise<Response, NeverThrowsException> apply(ResourceResponse result) {
                try {
                    writeApiVersionHeaders(result);
                    writeAdvice();
                    // Don't return the resource if this is a read request and the
                    // If-None-Match header was specified.
                    if (request instanceof ReadRequest) {
                        final String rev = getIfNoneMatch(httpRequest);
                        if (rev != null && rev.equals(result.getRevision())) {
                            // No change so 304.
                            Map<String, Object> responseBody = newResourceException(304)
                                    .setReason("Not Modified").toJsonValue().asMap();
                            return newResultPromise(new Response(Status.valueOf(304))
                                    .setEntity(responseBody));
                        }
                    }
                    writeResource(result);
                    onSuccess();
                } catch (final Exception e) {
                    onError(e);
                }
                return newResultPromise(httpResponse);
            }
        };
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

    private void writeResource(final ResourceResponse resource)
            throws IOException, ParseException, MalformedHeaderException {
        if (resource.getRevision() != null) {
            final StringBuilder builder = new StringBuilder();
            builder.append('"');
            builder.append(resource.getRevision());
            builder.append('"');
            httpResponse.getHeaders().put(HEADER_ETAG, builder.toString());
        }

        ContentType contentType = new ContentType(httpResponse.getHeaders().getFirst(ContentTypeHeader.class));

        if (contentType.match(MIME_TYPE_APPLICATION_JSON)) {
            writeResourceJsonContent(resource);
        } else if (contentType.match(MIME_TYPE_TEXT_PLAIN)) {
            writeTextValue(resource.getContent());
        } else {
            writeBinaryValue(resource.getContent());
        }
    }

    /**
     * Writes a JSON resource taking care to ensure that the _id and _rev fields are always serialized regardless of
     * the field filtering. It is essential that these fields are included so that clients can reconstruct
     * ResourceResponse object's "id" and "revision" properties. In addition, it is reasonable to assume that query
     * results should always include at least the _id field otherwise it will be difficult to perform any useful
     * client side result processing.
     */
    private void writeResourceJsonContent(final ResourceResponse resource)
            throws IOException, MalformedHeaderException {
        ObjectWriter objectWriter = Json.makeLocalizingObjectWriter(JSON_MAPPER, httpRequest);
        if (getRequestedProtocolVersion(httpRequest).getMajor() >= PROTOCOL_VERSION_2.getMajor()) {
            jsonGenerator.writeStartObject();
            final JsonValue content = resource.getContent();

            if (resource.getId() != null) {
                jsonGenerator.writeObjectField(FIELD_CONTENT_ID, resource.getId());
            } else {
                // Defensively extract an object instead of a string in case application code has stored a UUID
                // object, or some other non-JSON primitive. Also assume that a null ID means no ID.
                final Object id = content.get(FIELD_CONTENT_ID).getObject();
                if (id != null) {
                    jsonGenerator.writeObjectField(FIELD_CONTENT_ID, id.toString());
                }
            }

            if (resource.getRevision() != null) {
                jsonGenerator.writeObjectField(FIELD_CONTENT_REVISION, resource.getRevision());
            } else {
                // Defensively extract an object instead of a string in case application code has stored a Number
                // object, or some other non-JSON primitive. Also assume that a null revision means no revision.
                final Object rev = content.get(FIELD_CONTENT_REVISION).getObject();
                if (rev != null) {
                    jsonGenerator.writeObjectField(FIELD_CONTENT_REVISION, rev.toString());
                }
            }

            for (Map.Entry<String, Object> property : content.asMap().entrySet()) {
                final String key = property.getKey();
                if (!FIELD_CONTENT_ID.equals(key) && !FIELD_CONTENT_REVISION.equals(key)) {
                    jsonGenerator.writeFieldName(key);
                    objectWriter.writeValue(jsonGenerator, property.getValue());
                }
            }
            jsonGenerator.writeEndObject();
        } else {
            objectWriter.writeValue(jsonGenerator, resource.getContent().getObject());
        }
    }

    private void writeApiVersionHeaders(org.forgerock.json.resource.Response response) {
        if (response.getResourceApiVersion() != null) {
            httpResponse.getHeaders().put(
                    new ContentApiVersionHeader(protocolVersion, response.getResourceApiVersion()));
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
