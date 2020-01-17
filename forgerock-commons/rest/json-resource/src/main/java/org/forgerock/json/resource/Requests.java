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
 * Copyright 2012-2016 ForgeRock AS.
 */

package org.forgerock.json.resource;

import static org.forgerock.util.Utils.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.forgerock.http.routing.Version;
import org.forgerock.json.JsonException;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.util.i18n.PreferredLocales;

/**
 * A utility class containing various factory methods for creating and
 * manipulating requests.
 */
public final class Requests {
    private static abstract class AbstractRequestImpl<T extends Request> implements Request {
        private final List<JsonPointer> fields = new LinkedList<>();
        private ResourcePath resourcePath;
        private final Map<String, String> parameters = new LinkedHashMap<>(2);
        private Version resourceVersion;
        private PreferredLocales preferredLocales;

        protected AbstractRequestImpl() {
            // Default constructor.
        }

        protected AbstractRequestImpl(final Request request) {
            this.resourcePath = request.getResourcePathObject();
            this.fields.addAll(request.getFields());
            this.parameters.putAll(request.getAdditionalParameters());
            this.resourceVersion = request.getResourceVersion();
            this.preferredLocales = request.getPreferredLocales();
        }

        @Override
        public final T addField(final JsonPointer... fields) {
            for (final JsonPointer field : fields) {
                this.fields.add(notNull(field));
            }
            return getThis();
        }

        @Override
        public final T addField(final String... fields) {
            try {
                for (final String field : fields) {
                    this.fields.add(new JsonPointer(field));
                }
            } catch (final JsonException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
            return getThis();
        }

        @Override
        public final List<JsonPointer> getFields() {
            return fields;
        }

        @Override
        public final String getResourcePath() {
            return resourcePath.toString();
        }

        @Override
        public final ResourcePath getResourcePathObject() {
            return resourcePath;
        }

        @Override
        public Map<String, String> getAdditionalParameters() {
            return parameters;
        }

        @Override
        public String getAdditionalParameter(final String name) {
            return parameters.get(name);
        }

        @Override
        public Version getResourceVersion() {
            return resourceVersion;
        }

        @Override
        public final T setResourcePath(final String path) {
            resourcePath = ResourcePath.valueOf(path);
            return getThis();
        }

        @Override
        public final T setResourcePath(final ResourcePath path) {
            resourcePath = notNull(path);
            return getThis();
        }

        @Override
        public T setAdditionalParameter(final String name, final String value) throws BadRequestException {
            if (isReservedParameter(name)) {
                throw new BadRequestException("Unrecognized request parameter '" + name + "'");
            }
            parameters.put(notNull(name), value);
            return getThis();
        }

        @Override
        public T setResourceVersion(Version resourceVersion) {
            this.resourceVersion = resourceVersion;
            return getThis();
        }

        boolean isReservedParameter(String name) {
            return name.startsWith("_");
        }

        protected abstract T getThis();

        @Override
        public JsonValue toJsonValue() {
            return new JsonValue(new HashMap<>())
                    .put("method", getRequestType().name().toLowerCase())
                    .put(FIELD_RESOURCE_PATH, getResourcePath())
                    .put(FIELD_FIELDS, getFields());
        }

        @Override
        public String toString() {
            return toJsonValue().toString();
        }

        @Override
        public PreferredLocales getPreferredLocales() {
            return preferredLocales;
        }

        @Override
        public T setPreferredLocales(PreferredLocales preferredLocales) {
            this.preferredLocales = preferredLocales;
            return getThis();
        }
    }

    private static final class ActionRequestImpl extends AbstractRequestImpl<ActionRequest>
            implements ActionRequest {
        private String actionId;
        private JsonValue content;

        private ActionRequestImpl() {
            // Default constructor.
            content = new JsonValue(null);
        }

        private ActionRequestImpl(final ActionRequest request) {
            super(request);
            this.actionId = request.getAction();
            this.content = copyJsonValue(request.getContent());
        }

        @Override
        public <R, P> R accept(final RequestVisitor<R, P> v, final P p) {
            return v.visitActionRequest(p, this);
        }

        @Override
        public String getAction() {
            return actionId;
        }

        @Override
        public <T extends Enum<T>> T getActionAsEnum(final Class<T> type) {
            return asEnum(getAction(), type);
        }

        @Override
        public JsonValue getContent() {
            return content;
        }

        @Override
        public ActionRequest setAction(final String id) {
            this.actionId = notNull(id);
            return this;
        }

        @Override
        public ActionRequest setContent(final JsonValue content) {
            this.content = content != null ? content : new JsonValue(null);
            return this;
        }

        @Override
        protected ActionRequest getThis() {
            return this;
        }

        @Override
        public RequestType getRequestType() {
            return RequestType.ACTION;
        }

        @Override
        public JsonValue toJsonValue() {
            return super.toJsonValue()
                    .put(FIELD_ACTION, String.valueOf(getAction()))
                    .put(FIELD_CONTENT, getContent().getObject())
                    .put(FIELD_ADDITIONAL_PARAMETERS, getAdditionalParameters());
        }

        @Override
        boolean isReservedParameter(String name) {
            // no reserved parameters for ActionRequests in order to support current patch-by-query usage *except*
            // mimeType which only applies to true read requests.
            return name.equals("_mimeType");
        }
    }

    private static final class CreateRequestImpl extends AbstractRequestImpl<CreateRequest>
            implements CreateRequest {
        private JsonValue content;
        private String newResourceId;

        private CreateRequestImpl() {
            // Default constructor.
        }

        private CreateRequestImpl(final CreateRequest request) {
            super(request);
            this.content = copyJsonValue(request.getContent());
            this.newResourceId = request.getNewResourceId();
        }

        @Override
        public <R, P> R accept(final RequestVisitor<R, P> v, final P p) {
            return v.visitCreateRequest(p, this);
        }

        @Override
        public JsonValue getContent() {
            return content;
        }

        @Override
        public String getNewResourceId() {
            return newResourceId;
        }

        @Override
        public CreateRequest setContent(final JsonValue content) {
            this.content = notNull(content);
            return this;
        }

        @Override
        public CreateRequest setNewResourceId(final String id) {
            this.newResourceId = id;
            return this;
        }

        @Override
        protected CreateRequest getThis() {
            return this;
        }

        @Override
        public RequestType getRequestType() {
            return RequestType.CREATE;
        }

        @Override
        public JsonValue toJsonValue() {
            return super.toJsonValue()
                    .put(FIELD_NEW_RESOURCE_ID, getNewResourceId())
                    .put(FIELD_CONTENT, getContent().getObject());
        }

    }

    private static final class DeleteRequestImpl extends AbstractRequestImpl<DeleteRequest>
            implements DeleteRequest {
        private String version;

        private DeleteRequestImpl() {
            // Default constructor.
        }

        private DeleteRequestImpl(final DeleteRequest request) {
            super(request);
            this.version = request.getRevision();
        }

        @Override
        public <R, P> R accept(final RequestVisitor<R, P> v, final P p) {
            return v.visitDeleteRequest(p, this);
        }

        @Override
        public String getRevision() {
            return version;
        }

        @Override
        public DeleteRequest setRevision(final String version) {
            this.version = version;
            return this;
        }

        @Override
        protected DeleteRequest getThis() {
            return this;
        }

        @Override
        public RequestType getRequestType() {
            return RequestType.DELETE;
        }

        @Override
        public JsonValue toJsonValue() {
            return super.toJsonValue()
                    .put(FIELD_REVISION, String.valueOf(getRevision()));
        }

    }

    private static final class PatchRequestImpl extends AbstractRequestImpl<PatchRequest> implements
            PatchRequest {
        private List<PatchOperation> operations;
        private String version;

        private PatchRequestImpl() {
            operations = new LinkedList<>();
        }

        private PatchRequestImpl(final PatchRequest request) {
            super(request);
            this.operations = new LinkedList<>(request.getPatchOperations());
            this.version = request.getRevision();
        }

        @Override
        public <R, P> R accept(final RequestVisitor<R, P> v, final P p) {
            return v.visitPatchRequest(p, this);
        }

        @Override
        public String getRevision() {
            return version;
        }

        @Override
        public PatchRequest addPatchOperation(PatchOperation... operations) {
            Collections.addAll(this.operations, operations);
            return this;
        }

        @Override
        public List<PatchOperation> getPatchOperations() {
            return operations;
        }

        @Override
        public PatchRequest addPatchOperation(String operation, String field, JsonValue value) {
            operations.add(PatchOperation.operation(operation, field, value));
            return this;
        }

        @Override
        public PatchRequest setRevision(final String version) {
            this.version = version;
            return this;
        }

        @Override
        protected PatchRequest getThis() {
            return this;
        }

        @Override
        public RequestType getRequestType() {
            return RequestType.PATCH;
        }

        @Override
        public JsonValue toJsonValue() {
            final List<Object> operations = new ArrayList<>();
            for (PatchOperation operation : getPatchOperations()) {
                operations.add(operation.toJsonValue().getObject());
            }
            return super.toJsonValue()
                    .put(FIELD_REVISION, String.valueOf(getRevision()))
                    .put(FIELD_PATCH_OPERATIONS, operations);
        }
    }

    private static final class QueryRequestImpl extends AbstractRequestImpl<QueryRequest> implements
            QueryRequest {
        private org.forgerock.util.query.QueryFilter<JsonPointer> filter;
        private final List<SortKey> keys = new LinkedList<>();
        private String pagedResultsCookie;
        private CountPolicy totalPagedResultsPolicy = CountPolicy.NONE;
        private int pagedResultsOffset = 0;
        private int pageSize = 0;
        private String queryId;
        private String queryExpression;

        private QueryRequestImpl() {
            // Default constructor.
        }

        private QueryRequestImpl(final QueryRequest request) {
            super(request);
            this.filter = request.getQueryFilter();
            this.queryId = request.getQueryId();
            this.queryExpression = request.getQueryExpression();
            this.keys.addAll(request.getSortKeys());
            this.pageSize = request.getPageSize();
            this.pagedResultsCookie = request.getPagedResultsCookie();
            this.pagedResultsOffset = request.getPagedResultsOffset();
            this.totalPagedResultsPolicy = request.getTotalPagedResultsPolicy();
        }

        @Override
        public <R, P> R accept(final RequestVisitor<R, P> v, final P p) {
            return v.visitQueryRequest(p, this);
        }

        @Override
        public QueryRequest addSortKey(final SortKey... keys) {
            for (final SortKey key : keys) {
                this.keys.add(notNull(key));
            }
            return this;
        }

        @Override
        public final QueryRequest addSortKey(final String... keys) {
            for (final String key : keys) {
                this.keys.add(SortKey.valueOf(key));
            }
            return this;
        }

        @Override
        public String getPagedResultsCookie() {
            return pagedResultsCookie;
        }

        @Override
        public CountPolicy getTotalPagedResultsPolicy() {
            return totalPagedResultsPolicy;
        }

        @Override
        public int getPagedResultsOffset() {
            return pagedResultsOffset;
        }

        @Override
        public int getPageSize() {
            return pageSize;
        }

        @Override
        public org.forgerock.util.query.QueryFilter<JsonPointer> getQueryFilter() {
            return filter;
        }

        @Override
        public String getQueryId() {
            return queryId;
        }

        @Override
        public String getQueryExpression() {
            return queryExpression;
        }

        @Override
        public List<SortKey> getSortKeys() {
            return keys;
        }

        @Override
        public QueryRequest setPagedResultsCookie(final String cookie) {
            this.pagedResultsCookie = cookie;
            return this;
        }

        @Override
        public QueryRequest setTotalPagedResultsPolicy(final CountPolicy totalPagedResultsPolicy) {
            this.totalPagedResultsPolicy = notNull(totalPagedResultsPolicy);
            return this;
        }

        @Override
        public QueryRequest setPagedResultsOffset(int offset) {
            this.pagedResultsOffset = offset;
            return this;
        }

        @Override
        public QueryRequest setPageSize(final int size) {
            this.pageSize = size;
            return this;
        }

        @Override
        public QueryRequest setQueryExpression(final String expression) {
            this.queryExpression = expression;
            return this;
        }

        @Override
        public QueryRequest setQueryFilter(final org.forgerock.util.query.QueryFilter<JsonPointer> filter) {
            this.filter = filter;
            return this;
        }

        @Override
        public QueryRequest setQueryId(final String id) {
            this.queryId = id;
            return this;
        }

        @Override
        protected QueryRequest getThis() {
            return this;
        }

        @Override
        public RequestType getRequestType() {
            return RequestType.QUERY;
        }

        @Override
        public JsonValue toJsonValue() {
            final List<String> sortKeys =  new ArrayList<>();
            for (SortKey key : getSortKeys()) {
                sortKeys.add(String.valueOf(key));
            }
            return super.toJsonValue()
                    .put(FIELD_QUERY_ID, String.valueOf(getQueryId()))
                    .put(FIELD_QUERY_EXPRESSION, String.valueOf(getQueryExpression()))
                    .put(FIELD_QUERY_FILTER, String.valueOf(getQueryFilter()))
                    .put(FIELD_SORT_KEYS, sortKeys)
                    .put(FIELD_PAGE_SIZE, String.valueOf(getPageSize()))
                    .put(FIELD_PAGED_RESULTS_OFFSET, String.valueOf(getPagedResultsOffset()))
                    .put(FIELD_PAGED_RESULTS_COOKIE, String.valueOf(getPagedResultsCookie()))
                    .put(FIELD_TOTAL_PAGED_RESULTS_POLICY, String.valueOf(getTotalPagedResultsPolicy()))
                    .put(FIELD_ADDITIONAL_PARAMETERS, getAdditionalParameters());
        }
    }

    private static final class ReadRequestImpl extends AbstractRequestImpl<ReadRequest> implements
            ReadRequest {

        private ReadRequestImpl() {
            // Default constructor.
        }

        private ReadRequestImpl(final ReadRequest request) {
            super(request);
        }

        @Override
        public <R, P> R accept(final RequestVisitor<R, P> v, final P p) {
            return v.visitReadRequest(p, this);
        }

        @Override
        protected ReadRequest getThis() {
            return this;
        }

        @Override
        public RequestType getRequestType() {
            return RequestType.READ;
        }
    }

    private static final class UpdateRequestImpl extends AbstractRequestImpl<UpdateRequest>
            implements UpdateRequest {
        private JsonValue content;
        private String version;

        private UpdateRequestImpl() {
            // Default constructor.
        }

        private UpdateRequestImpl(final UpdateRequest request) {
            super(request);
            this.version = request.getRevision();
            this.content = copyJsonValue(request.getContent());
        }

        @Override
        public <R, P> R accept(final RequestVisitor<R, P> v, final P p) {
            return v.visitUpdateRequest(p, this);
        }

        @Override
        public JsonValue getContent() {
            return content;
        }

        @Override
        public String getRevision() {
            return version;
        }

        @Override
        public UpdateRequest setContent(final JsonValue content) {
            this.content = notNull(content);
            return this;
        }

        @Override
        public UpdateRequest setRevision(final String version) {
            this.version = version;
            return this;
        }

        @Override
        protected UpdateRequest getThis() {
            return this;
        }

        @Override
        public RequestType getRequestType() {
            return RequestType.UPDATE;
        }

        @Override
        public JsonValue toJsonValue() {
            return super.toJsonValue()
                    .put(FIELD_REVISION, String.valueOf(getRevision()))
                    .put(FIELD_CONTENT, getContent().getObject());
        }
    }

    private static final class ApiRequestImpl extends AbstractRequestImpl<Request> {
        private ApiRequestImpl() {
           // nothing to do
        }

        private ApiRequestImpl(ApiRequestImpl request) {
            super(request);
        }

        @Override
        protected Request getThis() {
            return this;
        }

        @Override
        public <R, P> R accept(RequestVisitor<R, P> v, P p) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RequestType getRequestType() {
            return RequestType.API;
        }
    }

    /**
     * Returns a copy of the provided action request.
     *
     * @param request
     *            The action request to be copied.
     * @return The action request copy.
     */
    public static ActionRequest copyOfActionRequest(final ActionRequest request) {
        return new ActionRequestImpl(request);
    }

    /**
     * Returns a copy of the provided create request.
     *
     * @param request
     *            The create request to be copied.
     * @return The create request copy.
     */
    public static CreateRequest copyOfCreateRequest(final CreateRequest request) {
        return new CreateRequestImpl(request);
    }

    /**
     * Returns a copy of the provided delete request.
     *
     * @param request
     *            The delete request to be copied.
     * @return The delete request copy.
     */
    public static DeleteRequest copyOfDeleteRequest(final DeleteRequest request) {
        return new DeleteRequestImpl(request);
    }

    /**
     * Returns a copy of the provided patch request.
     *
     * @param request
     *            The patch request to be copied.
     * @return The patch request copy.
     */
    public static PatchRequest copyOfPatchRequest(final PatchRequest request) {
        return new PatchRequestImpl(request);
    }

    /**
     * Returns a copy of the provided query request.
     *
     * @param request
     *            The query request to be copied.
     * @return The query request copy.
     */
    public static QueryRequest copyOfQueryRequest(final QueryRequest request) {
        return new QueryRequestImpl(request);
    }

    /**
     * Returns a copy of the provided read request.
     *
     * @param request
     *            The read request to be copied.
     * @return The read request copy.
     */
    public static ReadRequest copyOfReadRequest(final ReadRequest request) {
        return new ReadRequestImpl(request);
    }

    /**
     * Returns a copy of the provided update request.
     *
     * @param request
     *            The update request to be copied.
     * @return The update request copy.
     */
    public static UpdateRequest copyOfUpdateRequest(final UpdateRequest request) {
        return new UpdateRequestImpl(request);
    }

    /**
     * Returns a copy of the provided api request.
     *
     * @param request
     *            The api request to be copied.
     * @return The api request copy.
     */
    public static Request copyOfApiRequest(final Request request) {
        return new ApiRequestImpl((ApiRequestImpl) request);
    }

    /**
     * Returns a new action request with the provided resource path and action
     * ID. Invoking this method as follows:
     *
     * <pre>
     * newActionRequest(&quot;users/1&quot;, actionId);
     * </pre>
     *
     * Is equivalent to:
     *
     * <pre>
     * newActionRequest(&quot;users&quot;, &quot;1&quot;, actionId);
     * </pre>
     *
     * Except that the resource ID is already URL encoded in the first form.
     *
     * @param resourcePath
     *            The URL-encoded resource path.
     * @param actionId
     *            The action ID.
     * @return The new action request.
     */
    public static ActionRequest newActionRequest(final String resourcePath, final String actionId) {
        return new ActionRequestImpl().setResourcePath(resourcePath).setAction(actionId);
    }

    /**
     * Returns a new action request with the provided resource path and action
     * ID.
     *
     * @param resourcePath
     *            The parsed resource path.
     * @param actionId
     *            The action ID.
     * @return The new action request.
     */
    public static ActionRequest newActionRequest(final ResourcePath resourcePath,
            final String actionId) {
        return new ActionRequestImpl().setResourcePath(resourcePath).setAction(actionId);
    }

    /**
     * Returns a new action request with the provided resource container path,
     * resource ID, and action ID. Invoking this method as follows:
     *
     * <pre>
     * newActionRequest(&quot;users&quot;, &quot;1&quot;, &quot;someAction&quot;);
     * </pre>
     *
     * Is equivalent to:
     *
     * <pre>
     * newActionRequest(&quot;users/1&quot;, &quot;someAction&quot;);
     * </pre>
     *
     * Except that the resource ID is already URL encoded in the second form.
     *
     * @param resourceContainer
     *            The URL-encoded path of the resource container.
     * @param resourceId
     *            The URL decoded ID of the resource.
     * @param actionId
     *            The action ID.
     * @return The new action request.
     */
    public static ActionRequest newActionRequest(final String resourceContainer,
            final String resourceId, final String actionId) {
        return newActionRequest(ResourcePath.valueOf(resourceContainer), resourceId, actionId);
    }

    /**
     * Returns a new action request with the provided resource container path,
     * resource ID, and action ID.
     *
     * @param resourceContainer
     *            The parsed path of the resource container.
     * @param resourceId
     *            The URL decoded ID of the resource.
     * @param actionId
     *            The action ID.
     * @return The new action request.
     */
    public static ActionRequest newActionRequest(final ResourcePath resourceContainer,
            final String resourceId, final String actionId) {
        return newActionRequest(resourceContainer.child(resourceId), actionId);
    }

    /**
     * Returns a new create request with the provided resource path, and JSON
     * content. The create request will have a {@code null} new resource ID,
     * indicating that the server will be responsible for generating the ID of
     * the new resource. Invoking this method as follows:
     *
     * <pre>
     * newCreateRequest(&quot;users/1&quot;, content);
     * </pre>
     *
     * Is equivalent to:
     *
     * <pre>
     * newCreateRequest(&quot;users&quot;, "1", content);
     * </pre>
     *
     * Except that the resource ID is already URL encoded in the first form.
     *
     * @param resourceContainer
     *            The URL-encoded path of the resource container beneath which the new
     *            resource should be created.
     * @param content
     *            The JSON content.
     * @return The new create request.
     */
    public static CreateRequest newCreateRequest(final String resourceContainer,
            final JsonValue content) {
        return new CreateRequestImpl().setResourcePath(resourceContainer).setContent(content);
    }

    /**
     * Returns a new create request with the provided resource path, and JSON
     * content. The create request will have a {@code null} new resource ID,
     * indicating that the server will be responsible for generating the ID of
     * the new resource.
     *
     * @param resourceContainer
     *            The parsed path of the resource container beneath which the
     *            new resource should be created.
     * @param content
     *            The JSON content.
     * @return The new create request.
     */
    public static CreateRequest newCreateRequest(final ResourcePath resourceContainer,
            final JsonValue content) {
        return new CreateRequestImpl().setResourcePath(resourceContainer).setContent(content);
    }

    /**
     * Returns a new create request with the provided resource path, new
     * resource ID, and JSON content. Invoking this method as follows:
     *
     * <pre>
     * newCreateRequest(&quot;users&quot;, &quot;1&quot;, content);
     * </pre>
     *
     * Is equivalent to:
     *
     * <pre>
     * newCreateRequest(&quot;users&quot;, content).setNewResourceId(&quot;1&quot;);
     * </pre>
     *
     * Except that the resource ID is already URL encoded in the second form.
     *
     * @param resourceContainer
     *            The URL-encoded path of the resource container beneath which
     *            the new resource should be created.
     * @param newResourceId
     *            The URL decoded client provided ID of the resource to be
     *            created, or {@code null} if the server should be responsible
     *            for generating the resource ID.
     * @param content
     *            The JSON content.
     * @return The new create request.
     */
    public static CreateRequest newCreateRequest(final String resourceContainer,
            final String newResourceId, final JsonValue content) {
        return newCreateRequest(resourceContainer, content).setNewResourceId(newResourceId);
    }

    /**
     * Returns a new create request with the provided resource path, new
     * resource ID, and JSON content.
     *
     * @param resourceContainer
     *            The parsed path of the resource container beneath which the
     *            new resource should be created.
     * @param newResourceId
     *            The URL decoded client provided ID of the resource to be
     *            created, or {@code null} if the server should be responsible
     *            for generating the resource ID.
     * @param content
     *            The JSON content.
     * @return The new create request.
     */
    public static CreateRequest newCreateRequest(final ResourcePath resourceContainer,
            final String newResourceId, final JsonValue content) {
        return newCreateRequest(resourceContainer, content).setNewResourceId(newResourceId);
    }

    /**
     * Returns a new delete request with the provided resource path. Invoking
     * this method as follows:
     *
     * <pre>
     * newDeleteRequest(&quot;users/1&quot;);
     * </pre>
     *
     * Is equivalent to:
     *
     * <pre>
     * newDeleteRequest(&quot;users&quot;, &quot;1&quot;);
     * </pre>
     *
     * Except that the resource ID is already URL encoded in the first form.
     *
     * @param resourcePath
     *            The URL-encoded resource path.
     * @return The new delete request.
     */
    public static DeleteRequest newDeleteRequest(final String resourcePath) {
        return new DeleteRequestImpl().setResourcePath(resourcePath);
    }

    /**
     * Returns a new delete request with the provided resource path.
     *
     * @param resourcePath
     *            The parsed resource path.
     * @return The new delete request.
     */
    public static DeleteRequest newDeleteRequest(final ResourcePath resourcePath) {
        return new DeleteRequestImpl().setResourcePath(resourcePath);
    }

    /**
     * Returns a new delete request with the provided resource container path,
     * and resource ID. Invoking this method as follows:
     *
     * <pre>
     * newDeleteRequest(&quot;users&quot;, &quot;1&quot;);
     * </pre>
     *
     * Is equivalent to:
     *
     * <pre>
     * newDeleteRequest(&quot;users/1&quot;);
     * </pre>
     *
     * Except that the resource ID is already URL encoded in the second form.
     *
     * @param resourceContainer
     *            The URL-encoded path of the resource container.
     * @param resourceId
     *            The URL decoded ID of the resource.
     * @return The new delete request.
     */
    public static DeleteRequest newDeleteRequest(final String resourceContainer,
            final String resourceId) {
        return newDeleteRequest(ResourcePath.valueOf(resourceContainer), resourceId);
    }

    /**
     * Returns a new delete request with the provided resource container path,
     * and resource ID.
     *
     * @param resourceContainer
     *            The parsed path of the resource container.
     * @param resourceId
     *            The URL decoded ID of the resource.
     * @return The new delete request.
     */
    public static DeleteRequest newDeleteRequest(final ResourcePath resourceContainer,
            final String resourceId) {
        return newDeleteRequest(resourceContainer.child(resourceId));
    }

    /**
     * Returns a new patch request with the provided resource path and JSON
     * patch operations. Invoking this method as follows:
     *
     * <pre>
     * newPatchRequest(&quot;users/1&quot;, operations);
     * </pre>
     *
     * Is equivalent to:
     *
     * <pre>
     * newPatchRequest(&quot;users&quot;, &quot;1&quot;, operations);
     * </pre>
     *
     * Except that the resource ID is already URL encoded in the first form.
     *
     * @param resourcePath
     *            The URL-encoded resource path.
     * @param operations
     *            The JSON patch operations.
     * @return The new patch request.
     */
    public static PatchRequest newPatchRequest(final String resourcePath,
            final PatchOperation... operations) {
        return new PatchRequestImpl().setResourcePath(resourcePath).addPatchOperation(operations);
    }

    /**
     * Returns a new patch request with the provided resource path and JSON
     * patch operations.
     *
     * @param resourcePath
     *            The parsed resource path.
     * @param operations
     *            The JSON patch operations.
     * @return The new patch request.
     */
    public static PatchRequest newPatchRequest(final ResourcePath resourcePath,
            final PatchOperation... operations) {
        return new PatchRequestImpl().setResourcePath(resourcePath).addPatchOperation(operations);
    }

    /**
     * Returns a new patch request with the provided resource container path,
     * resource ID, and JSON patch operations. Invoking this method as follows:
     *
     * <pre>
     * newPatchRequest(&quot;users&quot;, &quot;1&quot;, operations);
     * </pre>
     *
     * Is equivalent to:
     *
     * <pre>
     * newPatchRequest(&quot;users/1&quot;, operations);
     * </pre>
     *
     * Except that the resource ID is already URL encoded in the second form.
     *
     * @param resourceContainer
     *            The URL-encoded path of the resource container.
     * @param resourceId
     *            The URL decoded ID of the resource.
     * @param operations
     *            The JSON patch operations.
     * @return The new patch request.
     */
    public static PatchRequest newPatchRequest(final String resourceContainer,
            final String resourceId, final PatchOperation... operations) {
        return newPatchRequest(ResourcePath.valueOf(resourceContainer), resourceId, operations);
    }

    /**
     * Returns a new patch request with the provided resource container path,
     * resource ID, and JSON patch operations.
     *
     * @param resourceContainer
     *            The parsed path of the resource container.
     * @param resourceId
     *            The URL decoded ID of the resource.
     * @param operations
     *            The JSON patch operations.
     * @return The new patch request.
     */
    public static PatchRequest newPatchRequest(final ResourcePath resourceContainer,
            final String resourceId, final PatchOperation... operations) {
        return newPatchRequest(resourceContainer.child(resourceId), operations);
    }

    /**
     * Returns a new query request with the provided resource container path.
     * Example:
     *
     * <pre>
     * newQueryRequest(&quot;users&quot;);
     * </pre>
     *
     * @param resourceContainer
     *            The URL-encoded path of the resource container.
     * @return The new query request.
     */
    public static QueryRequest newQueryRequest(final String resourceContainer) {
        return new QueryRequestImpl().setResourcePath(resourceContainer);
    }

    /**
     * Returns a new query request with the provided resource container path.
     * Example:
     *
     * <pre>
     * newQueryRequest(ResourcePath.valueOf(&quot;users&quot;));
     * </pre>
     *
     * @param resourceContainer
     *            The parsed path of the resource container.
     * @return The new query request.
     */
    public static QueryRequest newQueryRequest(final ResourcePath resourceContainer) {
        return new QueryRequestImpl().setResourcePath(resourceContainer);
    }

    /**
     * Returns a new read request with the provided resource path. Invoking this
     * method as follows:
     *
     * <pre>
     * newReadRequest(&quot;users/1&quot;);
     * </pre>
     *
     * Is equivalent to:
     *
     * <pre>
     * newReadRequest(&quot;users&quot;, &quot;1&quot;);
     * </pre>
     *
     * Except that the resource ID is already URL encoded in the first form.
     *
     * @param resourcePath
     *            The URL-encoded resource path.
     * @return The new read request.
     */
    public static ReadRequest newReadRequest(final String resourcePath) {
        return new ReadRequestImpl().setResourcePath(resourcePath);
    }

    /**
     * Returns a new read request with the provided resource path.
     *
     * @param resourcePath
     *            The parsed resource path.
     * @return The new read request.
     */
    public static ReadRequest newReadRequest(final ResourcePath resourcePath) {
        return new ReadRequestImpl().setResourcePath(resourcePath);
    }

    /**
     * Returns a new read request with the provided resource container path, and
     * resource ID. Invoking this method as follows:
     *
     * <pre>
     * newReadRequest(&quot;users&quot;, &quot;1&quot;);
     * </pre>
     *
     * Is equivalent to:
     *
     * <pre>
     * newReadRequest(&quot;users/1&quot;);
     * </pre>
     *
     * Except that the resource ID is already URL encoded in the second form.
     *
     * @param resourceContainer
     *            The URL-encoded path of the resource container.
     * @param resourceId
     *            The URL decoded ID of the resource.
     * @return The new read request.
     */
    public static ReadRequest newReadRequest(final String resourceContainer, final String resourceId) {
        return newReadRequest(ResourcePath.valueOf(resourceContainer), resourceId);
    }

    /**
     * Returns a new read request with the provided resource container path, and
     * resource ID.
     *
     * @param resourceContainer
     *            The parsed path of the resource container.
     * @param resourceId
     *            The URL decoded ID of the resource.
     * @return The new read request.
     */
    public static ReadRequest newReadRequest(final ResourcePath resourceContainer,
            final String resourceId) {
        return newReadRequest(resourceContainer.child(resourceId));
    }

    /**
     * Returns a new update request with the provided resource path and new JSON
     * content. Invoking this method as follows:
     *
     * <pre>
     * newUpdateRequest(&quot;users/1&quot;, newContent);
     * </pre>
     *
     * Is equivalent to:
     *
     * <pre>
     * newUpdateRequest(&quot;users&quot;, &quot;1&quot;, newContent);
     * </pre>
     *
     * Except that the resource ID is already URL encoded in the first form.
     *
     * @param resourcePath
     *            The URL-encoded resource path.
     * @param newContent
     *            The new JSON content.
     * @return The new update request.
     */
    public static UpdateRequest newUpdateRequest(final String resourcePath,
            final JsonValue newContent) {
        return new UpdateRequestImpl().setResourcePath(resourcePath).setContent(newContent);
    }

    /**
     * Returns a new update request with the provided resource path and new JSON
     * content.
     *
     * @param resourcePath
     *            The parsed resource path.
     * @param newContent
     *            The new JSON content.
     * @return The new update request.
     */
    public static UpdateRequest newUpdateRequest(final ResourcePath resourcePath,
            final JsonValue newContent) {
        return new UpdateRequestImpl().setResourcePath(resourcePath).setContent(newContent);
    }

    /**
     * Returns a new update request with the provided resource container path,
     * resource ID, and new JSON content. Invoking this method as follows:
     *
     * <pre>
     * newUpdateRequest(&quot;users&quot;, &quot;1&quot;, newContent);
     * </pre>
     *
     * Is equivalent to:
     *
     * <pre>
     * newUpdateRequest(&quot;users/1&quot;, newContent);
     * </pre>
     *
     * Except that the resource ID is already URL encoded in the second form.
     *
     * @param resourceContainer
     *            The URL-encoded path of the resource container.
     * @param resourceId
     *            The URL decoded ID of the resource.
     * @param newContent
     *            The new JSON content.
     * @return The new update request.
     */
    public static UpdateRequest newUpdateRequest(final String resourceContainer,
            final String resourceId, final JsonValue newContent) {
        return newUpdateRequest(ResourcePath.valueOf(resourceContainer), resourceId, newContent);
    }

    /**
     * Returns a new update request with the provided resource container path,
     * resource ID, and new JSON content.
     *
     * @param resourceContainer
     *            The parsed path of the resource container.
     * @param resourceId
     *            The URL decoded ID of the resource.
     * @param newContent
     *            The new JSON content.
     * @return The new update request.
     */
    public static UpdateRequest newUpdateRequest(final ResourcePath resourceContainer,
            final String resourceId, final JsonValue newContent) {
        return newUpdateRequest(resourceContainer.child(resourceId), newContent);
    }

    /**
     * Returns a new API request with the provided path.
     * @param path The path.
     * @return The request.
     */
    public static Request newApiRequest(final ResourcePath path) {
        return new ApiRequestImpl().setResourcePath(path);
    }

    private static JsonValue copyJsonValue(final JsonValue value) {
        return value != null ? value.copy() : null;
    }

    private static <T> T notNull(final T object) {
        if (object != null) {
            return object;
        } else {
            throw new NullPointerException();
        }
    }

    private Requests() {
        // Prevent instantiation.
    }

    // TODO: unmodifiable
}
