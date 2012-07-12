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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2012 ForgeRock AS. All rights reserved.
 */

package org.forgerock.json.resource;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;

/**
 * A utility class containing various factory methods for creating and
 * manipulating requests.
 */
public final class Requests {
    private static abstract class AbstractRequestImpl<T extends Request> implements Request {
        private String component;
        private final List<JsonPointer> fields = new LinkedList<JsonPointer>();
        private String id;

        protected AbstractRequestImpl() {
            // Default constructor.
        }

        protected AbstractRequestImpl(final Request request) {
            this.component = request.getComponent();
            this.id = request.getResourceId();
            this.fields.addAll(request.getFieldFilters());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public final T addFieldFilter(final JsonPointer... fields) {
            for (final JsonPointer field : fields) {
                this.fields.add(notNull(field));
            }
            return getThis();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public final T addFieldFilter(final String... fields) {
            for (final String field : fields) {
                this.fields.add(new JsonPointer(field));
            }
            return getThis();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public final String getComponent() {
            return component;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public final List<JsonPointer> getFieldFilters() {
            return fields;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public final String getResourceId() {
            return id;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public final T setComponent(final String name) {
            this.component = notNull(name);
            return getThis();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public final T setResourceId(final String id) {
            this.id = id;
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
            this.actionId = request.getActionId();
            this.content = copyJsonValue(request.getContent());
            this.parameters.putAll(request.getAdditionalActionParameters());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getActionId() {
            return actionId;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Map<String, String> getAdditionalActionParameters() {
            return parameters;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public JsonValue getContent() {
            return content;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ActionRequest setActionId(final String id) {
            this.actionId = notNull(id);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ActionRequest setAdditionalActionParameter(final String name, final String value) {
            parameters.put(notNull(name), notNull(value));
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ActionRequest setContent(final JsonValue content) {
            this.content = content;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected ActionRequest getThis() {
            return this;
        }

    }

    private static final class CreateRequestImpl extends AbstractRequestImpl<CreateRequest>
            implements CreateRequest {
        private JsonValue content;

        private CreateRequestImpl() {
            // Default constructor.
        }

        private CreateRequestImpl(final CreateRequest request) {
            super(request);
            this.content = copyJsonValue(request.getContent());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public JsonValue getContent() {
            return content;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public CreateRequest setContent(final JsonValue content) {
            this.content = notNull(content);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected CreateRequest getThis() {
            return this;
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

        /**
         * {@inheritDoc}
         */
        @Override
        public String getRevision() {
            return version;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DeleteRequest setRevision(final String version) {
            this.version = version;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DeleteRequest getThis() {
            return this;
        }

    }

    private static final class PatchRequestImpl extends AbstractRequestImpl<PatchRequest> implements
            PatchRequest {
        private Patch patch;
        private String version;

        private PatchRequestImpl() {
            // Default constructor.
        }

        private PatchRequestImpl(final PatchRequest request) {
            super(request);
            this.patch = request.getPatch(); // FIXME: is Patch immutable?
            this.version = request.getRevision();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Patch getPatch() {
            return patch;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getRevision() {
            return version;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public PatchRequest setPatch(final Patch changes) {
            this.patch = notNull(changes);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public PatchRequest setRevision(final String version) {
            this.version = version;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected PatchRequest getThis() {
            return this;
        }

    }

    private static final class QueryRequestImpl extends AbstractRequestImpl<QueryRequest> implements
            QueryRequest {
        private QueryFilter filter;
        private final List<SortKey> keys = new LinkedList<SortKey>();
        private String pagedResultsCookie;
        private int pageSize = 0;
        private final Map<String, String> parameters = new LinkedHashMap<String, String>(2);
        private String queryId;

        private QueryRequestImpl() {
            // Default constructor.
        }

        private QueryRequestImpl(final QueryRequest request) {
            super(request);
            this.filter = request.getQueryFilter();
            this.queryId = request.getQueryId();
            this.keys.addAll(request.getSortKeys());
            this.parameters.putAll(request.getQueryAdditionalParameters());
            this.pageSize = request.getPageSize();
            this.pagedResultsCookie = request.getPagedResultsCookie();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public QueryRequest addSortKey(final SortKey... keys) {
            for (final SortKey key : keys) {
                this.keys.add(notNull(key));
            }
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getPagedResultsCookie() {
            return pagedResultsCookie;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getPageSize() {
            return pageSize;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Map<String, String> getQueryAdditionalParameters() {
            return parameters;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public QueryFilter getQueryFilter() {
            return filter;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getQueryId() {
            return queryId;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<SortKey> getSortKeys() {
            return keys;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public QueryRequest setAdditionalQueryParameter(final String name, final String value) {
            parameters.put(notNull(name), notNull(value));
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public QueryRequest setPagedResultsCookie(final String cookie) {
            this.pagedResultsCookie = cookie;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public QueryRequest setPageSize(final int size) {
            this.pageSize = size;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public QueryRequest setQueryFilter(final QueryFilter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public QueryRequest setQueryId(final String id) {
            this.queryId = id;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected QueryRequest getThis() {
            return this;
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

        /**
         * {@inheritDoc}
         */
        @Override
        protected ReadRequest getThis() {
            return this;
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

        /**
         * {@inheritDoc}
         */
        @Override
        public JsonValue getNewContent() {
            return content;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getRevision() {
            return version;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public UpdateRequest setNewContent(final JsonValue content) {
            this.content = notNull(content);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public UpdateRequest setRevision(final String version) {
            this.version = version;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected UpdateRequest getThis() {
            return this;
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
     * Returns a new action request with the provided component name and action
     * ID and {@code null} resource ID.
     *
     * @param component
     *            The component name.
     * @param actionId
     *            The action ID.
     * @return The new action request.
     */
    public static ActionRequest newActionRequest(final String component, final String actionId) {
        return new ActionRequestImpl().setComponent(component).setActionId(actionId);
    }

    /**
     * Returns a new action request with the provided component name, resource
     * ID, and action ID.
     *
     * @param component
     *            The component name.
     * @param resourceId
     *            The resource ID.
     * @param actionId
     *            The action ID.
     * @return The new action request.
     */
    public static ActionRequest newActionRequest(final String component, final String resourceId,
            final String actionId) {
        return new ActionRequestImpl().setComponent(component).setResourceId(resourceId)
                .setActionId(actionId);
    }

    /**
     * Returns a new create request with the provided component name and JSON
     * content. The resource ID will initially be {@code null} indicating that a
     * server-provided ID should be assigned.
     *
     * @param component
     *            The component name.
     * @param content
     *            The JSON content.
     * @return The new create request.
     */
    public static CreateRequest newCreateRequest(final String component, final JsonValue content) {
        return new CreateRequestImpl().setComponent(component).setContent(content);
    }

    /**
     * Returns a new create request with the provided component name, resource
     * ID, and JSON content.
     *
     * @param component
     *            The component name.
     * @param resourceId
     *            The resource ID or {@code null} if a server-provided ID should
     *            be assigned.
     * @param content
     *            The JSON content.
     * @return The new create request.
     */
    public static CreateRequest newCreateRequest(final String component, final String resourceId,
            final JsonValue content) {
        return new CreateRequestImpl().setComponent(component).setResourceId(resourceId)
                .setContent(content);
    }

    /**
     * Returns a new delete request with the provided component name and
     * resource ID.
     *
     * @param component
     *            The component name.
     * @param resourceId
     *            The resource ID.
     * @return The new delete request.
     */
    public static DeleteRequest newDeleteRequest(final String component, final String resourceId) {
        return new DeleteRequestImpl().setComponent(component).setResourceId(resourceId);
    }

    /**
     * Returns a new patch request with the provided component name and JSON
     * patch.
     *
     * @param component
     *            The component name.
     * @param changes
     *            The JSON patch.
     * @return The new patch request.
     */
    public static PatchRequest newPatchRequest(final String component, final Patch changes) {
        return new PatchRequestImpl().setComponent(component).setPatch(changes);
    }

    /**
     * Returns a new patch request with the provided component name, resource
     * ID, and JSON patch.
     *
     * @param component
     *            The component name.
     * @param resourceId
     *            The resource ID.
     * @param changes
     *            The JSON patch.
     * @return The new patch request.
     */
    public static PatchRequest newPatchRequest(final String component, final String resourceId,
            final Patch changes) {
        return new PatchRequestImpl().setComponent(component).setResourceId(resourceId).setPatch(
                changes);
    }

    /**
     * Returns a new query request with the provided component name.
     *
     * @param component
     *            The component name.
     * @return The new query request.
     */
    public static QueryRequest newQueryRequest(final String component) {
        return new QueryRequestImpl().setComponent(component);
    }

    /**
     * Returns a new read request with the provided component name.
     *
     * @param component
     *            The component name.
     * @return The new read request.
     */
    public static ReadRequest newReadRequest(final String component) {
        return new ReadRequestImpl().setComponent(component);
    }

    /**
     * Returns a new read request with the provided component name and resource
     * ID.
     *
     * @param component
     *            The component name.
     * @param resourceId
     *            The resource ID.
     * @return The new read request.
     */
    public static ReadRequest newReadRequest(final String component, final String resourceId) {
        return new ReadRequestImpl().setComponent(component).setResourceId(resourceId);
    }

    /**
     * Returns a new update request with the provided component name and new
     * JSON content.
     *
     * @param component
     *            The component name.
     * @param newContent
     *            The new JSON content.
     * @return The new update request.
     */
    public static UpdateRequest newUpdateRequest(final String component, final JsonValue newContent) {
        return new UpdateRequestImpl().setComponent(component).setNewContent(newContent);
    }

    /**
     * Returns a new update request with the provided component name, resource
     * ID, and new JSON content.
     *
     * @param component
     *            The component name.
     * @param resourceId
     *            The resource ID.
     * @param newContent
     *            The new JSON content.
     * @return The new update request.
     */
    public static UpdateRequest newUpdateRequest(final String component, final String resourceId,
            final JsonValue newContent) {
        return new UpdateRequestImpl().setComponent(component).setResourceId(resourceId)
                .setNewContent(newContent);
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
