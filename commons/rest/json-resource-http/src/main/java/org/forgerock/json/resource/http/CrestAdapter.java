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

package org.forgerock.json.resource.http;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.forgerock.http.protocol.Status.CREATED;
import static org.forgerock.http.protocol.Status.NO_CONTENT;
import static org.forgerock.http.protocol.Status.OK;
import static org.forgerock.json.JsonValueFunctions.enumConstant;
import static org.forgerock.json.resource.QueryResponse.FIELD_ERROR;
import static org.forgerock.json.resource.QueryResponse.FIELD_PAGED_RESULTS_COOKIE;
import static org.forgerock.json.resource.QueryResponse.FIELD_RESULT;
import static org.forgerock.json.resource.QueryResponse.FIELD_TOTAL_PAGED_RESULTS;
import static org.forgerock.json.resource.QueryResponse.FIELD_TOTAL_PAGED_RESULTS_POLICY;
import static org.forgerock.json.resource.QueryResponse.NO_COUNT;
import static org.forgerock.json.resource.ResourceException.FIELD_CODE;
import static org.forgerock.json.resource.ResourceException.FIELD_DETAIL;
import static org.forgerock.json.resource.ResourceException.FIELD_MESSAGE;
import static org.forgerock.json.resource.ResourceException.FIELD_REASON;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_REVISION;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.json.resource.http.HttpUtils.DEFAULT_PROTOCOL_VERSION;
import static org.forgerock.json.resource.http.HttpUtils.ETAG_ANY;
import static org.forgerock.json.resource.http.HttpUtils.FIELDS_DELIMITER;
import static org.forgerock.json.resource.http.HttpUtils.HEADER_IF_MATCH;
import static org.forgerock.json.resource.http.HttpUtils.HEADER_IF_NONE_MATCH;
import static org.forgerock.json.resource.http.HttpUtils.METHOD_DELETE;
import static org.forgerock.json.resource.http.HttpUtils.METHOD_GET;
import static org.forgerock.json.resource.http.HttpUtils.METHOD_PATCH;
import static org.forgerock.json.resource.http.HttpUtils.METHOD_POST;
import static org.forgerock.json.resource.http.HttpUtils.METHOD_PUT;
import static org.forgerock.json.resource.http.HttpUtils.MIME_TYPE_APPLICATION_JSON;
import static org.forgerock.json.resource.http.HttpUtils.PARAM_ACTION;
import static org.forgerock.json.resource.http.HttpUtils.PARAM_FIELDS;
import static org.forgerock.json.resource.http.HttpUtils.PARAM_PAGED_RESULTS_COOKIE;
import static org.forgerock.json.resource.http.HttpUtils.PARAM_PAGED_RESULTS_OFFSET;
import static org.forgerock.json.resource.http.HttpUtils.PARAM_PAGE_SIZE;
import static org.forgerock.json.resource.http.HttpUtils.PARAM_QUERY_EXPRESSION;
import static org.forgerock.json.resource.http.HttpUtils.PARAM_QUERY_FILTER;
import static org.forgerock.json.resource.http.HttpUtils.PARAM_QUERY_ID;
import static org.forgerock.json.resource.http.HttpUtils.PARAM_SORT_KEYS;
import static org.forgerock.json.resource.http.HttpUtils.PARAM_TOTAL_PAGED_RESULTS_POLICY;
import static org.forgerock.json.resource.http.HttpUtils.SORT_KEYS_DELIMITER;
import static org.forgerock.util.CloseSilentlyFunction.closeSilently;
import static org.forgerock.util.Reject.checkNotNull;
import static org.forgerock.util.Utils.joinAsString;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.forgerock.http.Handler;
import org.forgerock.http.MutableUri;
import org.forgerock.http.protocol.Responses;
import org.forgerock.http.header.AcceptApiVersionHeader;
import org.forgerock.http.header.ContentApiVersionHeader;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.protocol.Form;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.Version;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CountPolicy;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.SortKey;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.services.context.Context;
import org.forgerock.util.Function;
import org.forgerock.util.promise.Promise;

/**
 * This class is a bridge between CREST and CHF (the counter-part of {@link HttpAdapter}): it is used to transform
 * CREST {@link org.forgerock.json.resource.Request} into CHF {@link Request} and CHF {@link Response} back in
 * CREST {@link org.forgerock.json.resource.Response}.
 *
 * Example:
 * <pre>
 *     {@code
 *     RequestHandler client = CrestHttp.newRequestHandler(new HttpClientHandler(),
 *                                                         new URI("http://www.example.com/api/"));
 *     }
 * </pre>
 *
 * You can even wrap it into a {@link org.forgerock.json.resource.Connection} for the fluent API:
 * <pre>
 *     {@code
 *     Connection connection = Resources.newInternalConnection(client);
 *     ResourceResponse response = connection.create(context,
 *                                                   newCreateRequest("/users",
 *                                                                    "bjensen",
 *                                                                    json(object(field("login", "bjensen")))));
 *     }
 * </pre>
 *
 * <p><strong>Implementation note:</strong> We do not want to resurrect
 * {@link org.forgerock.json.resource.AdviceContext AdviceContext} from returned HTTP headers.
 * Contexts should not be used for communicating protocol content.
 * Anything that is to be communicated between peers should be exposed as part of the Request/Response interfaces,
 * such as preferred languages, resource API version, etc. If we have a specific use case for Advices then they should
 * be exposed as properties of the Response.
 */
final class CrestAdapter implements RequestHandler {

    private static final Status NOT_MODIFIED = Status.valueOf(304, "Not Modified");

    private final Handler handler;
    private final URI baseUri;

    /**
     * Constructs a new {@link CrestAdapter} wrapping the given HTTP {@code handler}.
     *
     * @param handler
     *         HTTP handler that will handle translated requests
     * @param uri
     *         base URI (need to end with a {@code /}) for HTTP requests
     */
    public CrestAdapter(Handler handler, URI uri) {
        this.handler = checkNotNull(handler);
        this.baseUri = checkNotNull(uri);
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {

        Request httpRequest = new Request();
        prepareHttpRequest(request, httpRequest);
        httpRequest.setMethod(METHOD_POST);
        Form form = new Form();
        form.putSingle(PARAM_ACTION, request.getAction());
        form.appendRequestQuery(httpRequest);
        if (request.getContent() != null) {
            httpRequest.getEntity().setJson(request.getContent().getObject());
        }

        // Expect OK or NO_CONTENT
        return handler.handle(context, httpRequest)
                      .then(closeSilently(new Function<Response, ActionResponse, ResourceException>() {
                          @Override
                          public ActionResponse apply(Response response) throws ResourceException {
                              // Transform HTTP response to CREST ActionResponse

                              // CREST always output message with either application/json, text/plain,
                              // everything else is considered as a binary content

                              // We'll never output a request with 'mimeType'
                              // so the output content is always application/json
                              JsonValue content = loadJsonValueContent(response);

                              if (OK.equals(response.getStatus()) || NO_CONTENT.equals(response.getStatus())) {
                                  return setResourceVersion(response, newActionResponse(content));
                              } else {
                                  throw createResourceException(response, content);
                              }
                          }
                      }), Responses.<ActionResponse, ResourceException>noopExceptionFunction());
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
        final Request httpRequest = new Request();
        prepareHttpRequest(request, httpRequest);

        String resourceId = request.getNewResourceId();
        if (resourceId == null) {
            // container generated ID => POST
            httpRequest.setMethod(METHOD_POST);
        } else {
            // caller provided ID => PUT + If-None-Match: *
            httpRequest.setMethod(METHOD_PUT);
            MutableUri uri = httpRequest.getUri();
            try {
                // path and new resource id are not URL encoded (uri will take of that automatically)
                uri.setPath(uri.getPath() + "/" + request.getNewResourceId());
            } catch (URISyntaxException e) {
                return new InternalServerErrorException("Cannot rebuild resource path", e).asPromise();
            }
            setIfNoneMatchToAny(httpRequest);
        }

        if (request.getContent() != null) {
            httpRequest.getEntity().setJson(request.getContent().getObject());
        }

        // Expect CREATED
        return handler.handle(context, httpRequest)
                      .then(closeSilently(new Function<Response, ResourceResponse, ResourceException>() {
                          @Override
                          public ResourceResponse apply(Response response) throws ResourceException {
                              // Transform HTTP response to CREST ResourceResponse

                              // CREST always output message with either application/json, text/plain,
                              // everything else is considered as a binary content

                              // We'll never output a request with 'mimeType'
                              // so the output content is always application/json
                              JsonValue content = loadJsonValueContent(response);

                              if (CREATED.equals(response.getStatus())) {
                                  return setResourceVersion(response, createResourceResponse(content));
                              } else {
                                  throw createResourceException(response, content);
                              }
                          }
                      }), Responses.<ResourceResponse, ResourceException>noopExceptionFunction());
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {

        Request httpRequest = new Request();
        httpRequest.setMethod(METHOD_DELETE);
        prepareHttpRequest(request, httpRequest);
        setIfMatch(httpRequest, request.getRevision());

        // Expect OK
        return handler.handle(context, httpRequest)
                      .then(buildCrestResponse(asList(Status.OK)),
                            Responses.<ResourceResponse, ResourceException>noopExceptionFunction());
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
        Request httpRequest = new Request();
        httpRequest.setMethod(METHOD_PATCH);
        prepareHttpRequest(request, httpRequest);
        setIfMatch(httpRequest, request.getRevision());

        if (!request.getPatchOperations().isEmpty()) {
            JsonValue content = new JsonValue(new LinkedList<>());
            for (PatchOperation operation : request.getPatchOperations()) {
                content.add(operation.toJsonValue().getObject());
            }
            httpRequest.getEntity().setJson(content.getObject());
        }

        // Expect OK
        return handler.handle(context, httpRequest)
                      .then(buildCrestResponse(asList(Status.OK)),
                            Responses.<ResourceResponse, ResourceException>noopExceptionFunction());
    }

    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(Context context,
                                                                 QueryRequest request,
                                                                 final QueryResourceHandler queryHandler) {
        Request httpRequest = new Request();
        prepareHttpRequest(request, httpRequest);
        httpRequest.setMethod(METHOD_GET);
        Form form = new Form();
        putIfNotNull(form, PARAM_QUERY_ID, request.getQueryId());
        putIfNotNull(form, PARAM_QUERY_EXPRESSION, request.getQueryExpression());
        putIfNotNull(form, PARAM_QUERY_FILTER, request.getQueryFilter());
        putIfNotNull(form, PARAM_TOTAL_PAGED_RESULTS_POLICY, request.getTotalPagedResultsPolicy());
        putIfNotNull(form, PARAM_PAGED_RESULTS_COOKIE, request.getPagedResultsCookie());
        List<SortKey> sortKeys = request.getSortKeys();
        if (sortKeys != null && !sortKeys.isEmpty()) {
            form.putSingle(PARAM_SORT_KEYS, joinAsString(SORT_KEYS_DELIMITER));
        }
        if (request.getPageSize() > 0) {
            form.putSingle(PARAM_PAGE_SIZE, String.valueOf(request.getPageSize()));
        }
        if (request.getPagedResultsOffset() >= 1) {
            form.putSingle(PARAM_PAGED_RESULTS_OFFSET, String.valueOf(request.getPagedResultsOffset()));
        }
        if (!form.isEmpty()) {
            form.appendRequestQuery(httpRequest);
        }

        // Expect OK
        return handler.handle(context, httpRequest)
                      .then(closeSilently(new Function<Response, QueryResponse, ResourceException>() {
                          @Override
                          public QueryResponse apply(Response response) throws ResourceException {
                              // Transform HTTP response to CREST ActionResponse

                              // CREST always output message with either application/json, text/plain,
                              // everything else is considered as a binary content

                              // We'll never output a request with 'mimeType'
                              // so the output content is always application/json
                              JsonValue content = loadJsonValueContent(response);

                              if (OK.equals(response.getStatus()) && !content.isDefined(FIELD_ERROR)) {

                                  String pagedResultsCookie = content.get(FIELD_PAGED_RESULTS_COOKIE).asString();
                                  CountPolicy countPolicy = content.get(FIELD_TOTAL_PAGED_RESULTS_POLICY)
                                                                   .as(enumConstant(CountPolicy.class));
                                  Integer totalPagedResults = content.get(FIELD_TOTAL_PAGED_RESULTS)
                                                                     .defaultTo(NO_COUNT)
                                                                     .asInteger();
                                  QueryResponse queryResponse = newQueryResponse(pagedResultsCookie,
                                                                                 countPolicy,
                                                                                 totalPagedResults);
                                  for (JsonValue value : content.get(FIELD_RESULT)) {
                                      queryHandler.handleResource(createResourceResponse(value));
                                  }

                                  return setResourceVersion(response, queryResponse);
                              } else {
                                  throw createResourceException(response, content);
                              }
                          }
                      }), Responses.<QueryResponse, ResourceException>noopExceptionFunction());
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {

        Request httpRequest = new Request();
        httpRequest.setMethod(METHOD_GET);
        prepareHttpRequest(request, httpRequest);

        // Expect OK or NOT_MODIFIED(304)
        return handler.handle(context, httpRequest)
                      .then(buildCrestResponse(asList(Status.OK, NOT_MODIFIED)),
                            Responses.<ResourceResponse, ResourceException>noopExceptionFunction());
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {

        Request httpRequest = new Request();
        httpRequest.setMethod(METHOD_PUT);
        prepareHttpRequest(request, httpRequest);
        setIfMatch(httpRequest, request.getRevision());
        if (request.getContent() != null) {
            httpRequest.getEntity().setJson(request.getContent().getObject());
        }

        // Only expect OK
        return handler.handle(context, httpRequest)
                      .then(buildCrestResponse(asList(Status.OK)),
                            Responses.<ResourceResponse, ResourceException>noopExceptionFunction());
    }

    private static Function<Response, ResourceResponse, ResourceException> buildCrestResponse(
            final List<Status> accepted) {
        return closeSilently(new Function<Response, ResourceResponse, ResourceException>() {
            @Override
            public ResourceResponse apply(Response response) throws ResourceException {
                // Transform HTTP response to CREST ResourceResponse

                // CREST always output message with either application/json, text/plain,
                // everything else is considered as a binary content

                // We'll never output a request with 'mimeType'
                // so the output content is always application/json
                JsonValue content = loadJsonValueContent(response);

                if (accepted.contains(response.getStatus())) {
                    return setResourceVersion(response, createResourceResponse(content));
                } else {
                    throw createResourceException(response, content);
                }
            }
        });
    }

    private static void putIfNotNull(Form form, String name, Object value) {
        if (value != null) {
            form.putSingle(name, value.toString());
        }
    }

    private static JsonValue loadJsonValueContent(final Response response) throws ResourceException {
        if (MIME_TYPE_APPLICATION_JSON.equals(ContentTypeHeader.valueOf(response).getType())) {
            try {
                return new JsonValue(response.getEntity().getJson());
            } catch (IOException e) {
                throw new InternalServerErrorException("Cannot parse HTTP response content as JSON", e);
            }
        }
        throw new InternalServerErrorException("Response is not application/json");
    }

    private static ResourceResponse createResourceResponse(final JsonValue content) {
        return newResourceResponse(content.get(FIELD_CONTENT_ID).asString(),
                                   content.get(FIELD_CONTENT_REVISION).asString(),
                                   content);
    }

    private static <T extends org.forgerock.json.resource.Response> T setResourceVersion(final Response httpResponse,
                                                                                         final T result) {
        if (httpResponse.getHeaders().containsKey(ContentApiVersionHeader.NAME)) {
            Version resourceVersion = ContentApiVersionHeader.valueOf(httpResponse).getResourceVersion();
            result.setResourceApiVersion(resourceVersion);
        }
        return result;
    }

    private static void setRequestedResourceVersion(Request request, Version resourceVersion) {
        // Force protocol version to 2.0 (current) at least
        request.getHeaders().put(new AcceptApiVersionHeader(DEFAULT_PROTOCOL_VERSION,
                                                                  resourceVersion));
    }

    private static ResourceException createResourceException(final Response response, final JsonValue content) {
        ResourceException exception = newResourceException(content.get(FIELD_CODE)
                                                                  .defaultTo(response.getStatus().getCode())
                                                                  .asInteger(),
                                                           content.get(FIELD_MESSAGE).asString());

        if (content.isDefined(FIELD_DETAIL)) {
            exception.setDetail(content.get(FIELD_DETAIL));
        }
        if (content.isDefined(FIELD_REASON)) {
            exception.setReason(content.get(FIELD_REASON).asString());
        }
        // TODO Add other fields (cause) ?
        return setResourceVersion(response, exception);
    }

    private static void setIfMatch(final Request request, final String revision) {
        String value = "*";
        if (revision != null) {
            value = format("\"%s\"", revision);
        }
        request.getHeaders().put(HEADER_IF_MATCH, value);
    }

    private static void setIfNoneMatchToAny(final Request request) {
        request.getHeaders().put(HEADER_IF_NONE_MATCH, ETAG_ANY);
    }

    private void prepareHttpRequest(final org.forgerock.json.resource.Request request, final Request httpRequest) {
        setRequestedResourceVersion(httpRequest, request.getResourceVersion());

        httpRequest.setUri(baseUri.resolve(request.getResourcePath()));

        final Form form = new Form();
        if (!request.getFields().isEmpty()) {
            form.putSingle(PARAM_FIELDS, joinAsString(FIELDS_DELIMITER, request.getFields().toArray()));
        }
        for (Map.Entry<String, String> entry : request.getAdditionalParameters().entrySet()) {
            form.putSingle(entry.getKey(), entry.getValue());
        }
        if (!form.isEmpty()) {
            form.toRequestQuery(httpRequest);
        }
    }

}
