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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.json.resource;

import static org.forgerock.util.Reject.checkNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.forgerock.http.ApiProducer;
import org.forgerock.services.context.Context;
import org.forgerock.services.descriptor.Describable;
import org.forgerock.util.promise.Promise;

import org.forgerock.api.models.ApiDescription;

/**
 * A chain of filters terminated by a target request handler. The filter chain
 * is thread safe and supports updates to the list of filters and the target
 * request handler while actively processing requests.
 */
public final class FilterChain implements RequestHandler, Describable<ApiDescription, Request> {
    /*
     * A request handler which represents the current position in the filter
     * chain. Maintains a reference to the filter chain which was in use at the
     * time when the cursor was created.
     */
    private final class Cursor implements RequestHandler, Describable<ApiDescription, Request> {
        private final int pos;
        private final Filter[] snapshot;

        private Cursor() {
            this(filters.toArray(new Filter[0]), 0);
        }

        private Cursor(final Filter[] snapshot, final int pos) {
            this.snapshot = snapshot;
            this.pos = pos;
        }

        @Override
        public Promise<ActionResponse, ResourceException> handleAction(final Context context,
                final ActionRequest request) {
            if (hasNext()) {
                return get().filterAction(context, request, next());
            } else {
                return target.handleAction(context, request);
            }
        }

        @Override
        public Promise<ResourceResponse, ResourceException> handleCreate(final Context context,
                final CreateRequest request) {
            if (hasNext()) {
                return get().filterCreate(context, request, next());
            } else {
                return target.handleCreate(context, request);
            }
        }

        @Override
        public Promise<ResourceResponse, ResourceException> handleDelete(final Context context,
                final DeleteRequest request) {
            if (hasNext()) {
                return get().filterDelete(context, request, next());
            } else {
                return target.handleDelete(context, request);
            }
        }

        @Override
        public Promise<ResourceResponse, ResourceException> handlePatch(final Context context,
                final PatchRequest request) {
            if (hasNext()) {
                return get().filterPatch(context, request, next());
            } else {
                return target.handlePatch(context, request);
            }
        }

        @Override
        public Promise<QueryResponse, ResourceException> handleQuery(final Context context,
                final QueryRequest request, final QueryResourceHandler handler) {
            if (hasNext()) {
                return get().filterQuery(context, request, handler, next());
            } else {
                return target.handleQuery(context, request, handler);
            }
        }

        @Override
        public Promise<ResourceResponse, ResourceException> handleRead(final Context context,
                final ReadRequest request) {
            if (hasNext()) {
                return get().filterRead(context, request, next());
            } else {
                return target.handleRead(context, request);
            }
        }

        @Override
        public Promise<ResourceResponse, ResourceException> handleUpdate(final Context context,
                final UpdateRequest request) {
            if (hasNext()) {
                return get().filterUpdate(context, request, next());
            } else {
                return target.handleUpdate(context, request);
            }
        }

        @Override
        public ApiDescription api(ApiProducer<ApiDescription> producer) {
            throw new UnsupportedOperationException("API resolution should skip filters");
        }

        @Override
        public ApiDescription handleApiRequest(Context context, Request request) {
            throw new UnsupportedOperationException("API resolution should skip filters");
        }

        @Override
        public void addDescriptorListener(Describable.Listener listener) {
            throw new UnsupportedOperationException("API resolution should skip filters");
        }

        @Override
        public void removeDescriptorListener(Describable.Listener listener) {
            throw new UnsupportedOperationException("API resolution should skip filters");
        }

        private Filter get() {
            return snapshot[pos];
        }

        private boolean hasNext() {
            return pos < snapshot.length;
        }

        private Cursor next() {
            return new Cursor(snapshot, pos + 1);
        }
    }

    private final List<Filter> filters = new CopyOnWriteArrayList<>();
    private volatile RequestHandler target;

    /**
     * Creates an empty filter chain.
     *
     * @param target
     *            The target request handler which will be invoked once
     *            processing has reached the end of the filter chain.
     */
    public FilterChain(final RequestHandler target) {
        this.target = checkNotNull(target, "Cannot create FilterChain with null target RequestHandler");
    }

    /**
     * Creates a filter chain containing the provided list of filters.
     *
     * @param target
     *            The target request handler which will be invoked once
     *            processing has reached the end of the filter chain.
     * @param filters
     *            The list of filters to be processed before invoking the
     *            target.
     */
    public FilterChain(final RequestHandler target, final Collection<Filter> filters) {
        this.target = checkNotNull(target, "Cannot create FilterChain with null target RequestHandler");
        this.filters.addAll(filters);
    }

    /**
     * Creates a filter chain containing the provided list of filters.
     *
     * @param target
     *            The target request handler which will be invoked once
     *            processing has reached the end of the filter chain.
     * @param filters
     *            The list of filters to be processed before invoking the
     *            target.
     */
    public FilterChain(final RequestHandler target, final Filter... filters) {
        this.target = checkNotNull(target, "Cannot create FilterChain with null target RequestHandler");
        this.filters.addAll(Arrays.asList(filters));
    }

    /**
     * Returns a modifiable list containing the list of filters in this filter
     * chain. Updates to the filter chain are thread safe and may be performed
     * while the processing requests.
     *
     * @return A modifiable list containing the list of filters in this filter
     *         chain.
     */
    public List<Filter> getFilters() {
        return filters;
    }

    /**
     * Returns the target request handler which will be invoked once processing
     * has reached the end of the filter chain.
     *
     * @return The target request handler which will be invoked once processing
     *         has reached the end of the filter chain.
     */
    public RequestHandler getTarget() {
        return target;
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(final Context context,
            final ActionRequest request) {
        return new Cursor().handleAction(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(final Context context,
            final CreateRequest request) {
        return new Cursor().handleCreate(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(final Context context,
            final DeleteRequest request) {
        return new Cursor().handleDelete(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(final Context context,
            final PatchRequest request) {
        return new Cursor().handlePatch(context, request);
    }

    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(final Context context,
            final QueryRequest request, final QueryResourceHandler handler) {
        return new Cursor().handleQuery(context, request, handler);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(final Context context,
            final ReadRequest request) {
        return new Cursor().handleRead(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(final Context context,
            final UpdateRequest request) {
        return new Cursor().handleUpdate(context, request);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ApiDescription api(ApiProducer<ApiDescription> producer) {
        return target instanceof Describable
                ? ((Describable<ApiDescription, Request>) target).api(producer)
                : null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ApiDescription handleApiRequest(Context context, Request request) {
        if (target instanceof Describable) {
            return ((Describable<ApiDescription, Request>) target)
                    .handleApiRequest(context, request);
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public void addDescriptorListener(Describable.Listener listener) {
        if (target instanceof Describable) {
            ((Describable) target).addDescriptorListener(listener);
        }
    }

    @Override
    public void removeDescriptorListener(Describable.Listener listener) {
        if (target instanceof Describable) {
            ((Describable) target).removeDescriptorListener(listener);
        }
    }

    /**
     * Sets the target request handler which will be invoked once processing has
     * reached the end of the filter chain. The target request handler may be
     * updated while the processing requests.
     *
     * @param target
     *            The target request handler which will be invoked once
     *            processing has reached the end of the filter chain.
     * @return This a reference to this filter chain.
     */
    public FilterChain setTarget(final RequestHandler target) {
        this.target = checkNotNull(target, "Cannot set target RequestHandler to null value");
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(filters.toString());
        builder.append(" -> ");
        builder.append(target.toString());
        return builder.toString();
    }

}
