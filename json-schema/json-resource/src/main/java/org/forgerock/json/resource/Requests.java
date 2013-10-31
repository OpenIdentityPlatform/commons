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
 * Copyright 2012-2013 ForgeRock AS.
 */

package org.forgerock.json.resource;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;

/**
 * A utility class containing various factory methods for creating and
 * manipulating requests.
 */
public final class Requests {
    private static abstract class AbstractRequestImpl<T extends Request> implements Request {
        private final List<JsonPointer> fields = new LinkedList<JsonPointer>();
        private ResourceName resourceName;

        protected AbstractRequestImpl() {
            // Default constructor.
        }

        protected AbstractRequestImpl(final Request request) {
            this.resourceName = request.getResourceNameObject();
            this.fields.addAll(request.getFields());
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
        public final String getResourceName() {
            return resourceName.toString();
        }

        @Override
        public final ResourceName getResourceNameObject() {
            return resourceName;
        }

        @Override
        public final T setResourceName(final String name) {
            resourceName = ResourceName.valueOf(name);
            return getThis();
        }

        @Override
        public final T setResourceName(final ResourceName name) {
            resourceName = notNull(name);
            return getThis();
        }

        protected abstract T getThis();

    }

    private static final class ActionRequestImpl extends AbstractRequestImpl<ActionRequest>
            implements ActionRequest {
        private String actionId;
        private JsonValue content;
        private final Map<String, String> parameters = new LinkedHashMap<String, String>(2);

        private ActionRequestImpl() {
            // Default constructor.
        }

        private ActionRequestImpl(final ActionRequest request) {
            super(request);
            this.actionId = request.getAction();
            this.content = copyJsonValue(request.getContent());
            this.parameters.putAll(request.getAdditionalActionParameters());
        }

        @Override
        public <R, P> R accept(final RequestVisitor<R, P> v, final P p) {
            return v.visitActionRequest(p, this);
        };

        @Override
        public String getAction() {
            return actionId;
        }

        @Override
        public Map<String, String> getAdditionalActionParameters() {
            return parameters;
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
        public ActionRequest setAdditionalActionParameter(final String name, final String value) {
            parameters.put(notNull(name), notNull(value));
            return this;
        }

        @Override
        public ActionRequest setContent(final JsonValue content) {
            this.content = content;
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
        };

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
        };

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

    }

    private static final class PatchRequestImpl extends AbstractRequestImpl<PatchRequest> implements
            PatchRequest {
        private List<PatchOperation> operations;
        private String version;

        private PatchRequestImpl() {
            operations = new LinkedList<PatchOperation>();
        }

        private PatchRequestImpl(final PatchRequest request) {
            super(request);
            this.operations = new LinkedList<PatchOperation>(request.getPatchOperations());
            this.version = request.getRevision();
        }

        @Override
        public <R, P> R accept(final RequestVisitor<R, P> v, final P p) {
            return v.visitPatchRequest(p, this);
        };

        @Override
        public String getRevision() {
            return version;
        }

        @Override
        public PatchRequest addPatchOperation(PatchOperation... operations) {
            for (PatchOperation operation : operations) {
                this.operations.add(operation);
            }
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
    }

    private static final class QueryRequestImpl extends AbstractRequestImpl<QueryRequest> implements
            QueryRequest {
        private QueryFilter filter;
        private final List<SortKey> keys = new LinkedList<SortKey>();
        private String pagedResultsCookie;
        private int pagedResultsOffset = 0;
        private int pageSize = 0;
        private final Map<String, String> parameters = new LinkedHashMap<String, String>(2);
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
            this.parameters.putAll(request.getAdditionalQueryParameters());
            this.pageSize = request.getPageSize();
            this.pagedResultsCookie = request.getPagedResultsCookie();
            this.pagedResultsOffset = request.getPagedResultsOffset();
        }

        @Override
        public <R, P> R accept(final RequestVisitor<R, P> v, final P p) {
            return v.visitQueryRequest(p, this);
        };

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
        public Map<String, String> getAdditionalQueryParameters() {
            return parameters;
        }

        @Override
        public String getPagedResultsCookie() {
            return pagedResultsCookie;
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
        public QueryFilter getQueryFilter() {
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
        public QueryRequest setAdditionalQueryParameter(final String name, final String value) {
            parameters.put(notNull(name), notNull(value));
            return this;
        }

        @Override
        public QueryRequest setPagedResultsCookie(final String cookie) {
            this.pagedResultsCookie = cookie;
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
        public QueryRequest setQueryFilter(final QueryFilter filter) {
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
        };

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
            this.content = copyJsonValue(request.getNewContent());
        }

        @Override
        public <R, P> R accept(final RequestVisitor<R, P> v, final P p) {
            return v.visitUpdateRequest(p, this);
        };

        @Override
        public JsonValue getNewContent() {
            return content;
        }

        @Override
        public String getRevision() {
            return version;
        }

        @Override
        public UpdateRequest setNewContent(final JsonValue content) {
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
     * Returns a new action request with the provided resource name and action
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
     * @param resourceName
     *            The resource name.
     * @param actionId
     *            The action ID.
     * @return The new action request.
     */
    public static ActionRequest newActionRequest(final String resourceName, final String actionId) {
        return new ActionRequestImpl().setResourceName(resourceName).setAction(actionId);
    }

    /**
     * Returns a new action request with the provided resource container name,
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
     *            The name of the resource container.
     * @param resourceId
     *            The URL decoded ID of the resource.
     * @param actionId
     *            The action ID.
     * @return The new action request.
     */
    public static ActionRequest newActionRequest(final String resourceContainer,
            final String resourceId, final String actionId) {
        return newActionRequest(concat(resourceContainer, resourceId), actionId);
    }

    /**
     * Returns a new create request with the provided resource name, and JSON
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
     *            The name of the resource container beneath which the new
     *            resource should be created.
     * @param content
     *            The JSON content.
     * @return The new create request.
     */
    public static CreateRequest newCreateRequest(final String resourceContainer,
            final JsonValue content) {
        return new CreateRequestImpl().setResourceName(resourceContainer).setContent(content);
    }

    /**
     * Returns a new create request with the provided resource name, new
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
     *            The name of the resource container beneath which the new
     *            resource should be created.
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
     * Returns a new delete request with the provided resource name. Invoking
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
     * @param resourceName
     *            The resource name.
     * @return The new delete request.
     */
    public static DeleteRequest newDeleteRequest(final String resourceName) {
        return new DeleteRequestImpl().setResourceName(resourceName);
    }

    /**
     * Returns a new delete request with the provided resource container name,
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
     *            The name of the resource container.
     * @param resourceId
     *            The URL decoded ID of the resource.
     * @return The new delete request.
     */
    public static DeleteRequest newDeleteRequest(final String resourceContainer,
            final String resourceId) {
        return newDeleteRequest(concat(resourceContainer, resourceId));
    }

    /**
     * Returns a new patch request with the provided resource name and JSON
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
     * @param resourceName
     *            The resource name.
     * @param operations
     *            The JSON patch operations.
     * @return The new patch request.
     */
    public static PatchRequest newPatchRequest(final String resourceName,
            final PatchOperation... operations) {
        return new PatchRequestImpl().setResourceName(resourceName).addPatchOperation(operations);
    }

    /**
     * Returns a new patch request with the provided resource container name,
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
     *            The name of the resource container.
     * @param resourceId
     *            The URL decoded ID of the resource.
     * @param operations
     *            The JSON patch operations.
     * @return The new patch request.
     */
    public static PatchRequest newPatchRequest(final String resourceContainer,
            final String resourceId, final PatchOperation... operations) {
        return newPatchRequest(concat(resourceContainer, resourceId), operations);
    }

    /**
     * Returns a new query request with the provided resource container name.
     * Example:
     *
     * <pre>
     * newQueryRequest(&quot;users&quot;);
     * </pre>
     *
     * @param resourceContainer
     *            The name of the resource container.
     * @return The new query request.
     */
    public static QueryRequest newQueryRequest(final String resourceContainer) {
        return new QueryRequestImpl().setResourceName(resourceContainer);
    }

    /**
     * Returns a new read request with the provided resource name. Invoking this
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
     * @param resourceName
     *            The resource name.
     * @return The new read request.
     */
    public static ReadRequest newReadRequest(final String resourceName) {
        return new ReadRequestImpl().setResourceName(resourceName);
    }

    /**
     * Returns a new read request with the provided resource container name, and
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
     *            The name of the resource container.
     * @param resourceId
     *            The URL decoded ID of the resource.
     * @return The new read request.
     */
    public static ReadRequest newReadRequest(final String resourceContainer, final String resourceId) {
        return newReadRequest(concat(resourceContainer, resourceId));
    }

    /**
     * Returns a new update request with the provided resource name and new JSON
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
     * @param resourceName
     *            The resource name.
     * @param newContent
     *            The new JSON content.
     * @return The new update request.
     */
    public static UpdateRequest newUpdateRequest(final String resourceName,
            final JsonValue newContent) {
        return new UpdateRequestImpl().setResourceName(resourceName).setNewContent(newContent);
    }

    /**
     * Returns a new update request with the provided resource container name,
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
     *            The name of the resource container.
     * @param resourceId
     *            The URL decoded ID of the resource.
     * @param newContent
     *            The new JSON content.
     * @return The new update request.
     */
    public static UpdateRequest newUpdateRequest(final String resourceContainer,
            final String resourceId, final JsonValue newContent) {
        return newUpdateRequest(concat(resourceContainer, resourceId), newContent);
    }

    private static String concat(final String resourceContainer, final String resourceId) {
        final String urlEncodedResourceId = ResourceName.urlEncode(resourceId);
        final StringBuilder builder =
                new StringBuilder(resourceContainer.length() + 1 + urlEncodedResourceId.length());
        builder.append(resourceContainer);
        if (!resourceContainer.endsWith("/") && resourceContainer.length() > 1) {
            builder.append('/');
        }
        builder.append(urlEncodedResourceId);
        return builder.toString();
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
