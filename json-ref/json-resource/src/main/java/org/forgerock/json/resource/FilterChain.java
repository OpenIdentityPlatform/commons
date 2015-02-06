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
 * Copyright 2013 ForgeRock AS.
 */
package org.forgerock.json.resource;

import static org.forgerock.util.Reject.checkNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.forgerock.http.ServerContext;
import org.forgerock.json.fluent.JsonValue;

/**
 * A chain of filters terminated by a target request handler. The filter chain
 * is thread safe and supports updates to the list of filters and the target
 * request handler while actively processing requests.
 */
public final class FilterChain implements RequestHandler {
    /*
     * A request handler which represents the current position in the filter
     * chain. Maintains a reference to the filter chain which was in use at the
     * time when the cursor was created.
     */
    private final class Cursor implements RequestHandler {
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
        public void handleAction(final ServerContext context, final ActionRequest request,
                final ResultHandler<JsonValue> handler) {
            if (hasNext()) {
                get().filterAction(context, request, handler, next());
            } else {
                target.handleAction(context, request, handler);
            }
        }

        @Override
        public void handleCreate(final ServerContext context, final CreateRequest request,
                final ResultHandler<Resource> handler) {
            if (hasNext()) {
                get().filterCreate(context, request, handler, next());
            } else {
                target.handleCreate(context, request, handler);
            }
        }

        @Override
        public void handleDelete(final ServerContext context, final DeleteRequest request,
                final ResultHandler<Resource> handler) {
            if (hasNext()) {
                get().filterDelete(context, request, handler, next());
            } else {
                target.handleDelete(context, request, handler);
            }
        }

        @Override
        public void handlePatch(final ServerContext context, final PatchRequest request,
                final ResultHandler<Resource> handler) {
            if (hasNext()) {
                get().filterPatch(context, request, handler, next());
            } else {
                target.handlePatch(context, request, handler);
            }
        }

        @Override
        public void handleQuery(final ServerContext context, final QueryRequest request,
                final QueryResultHandler handler) {
            if (hasNext()) {
                get().filterQuery(context, request, handler, next());
            } else {
                target.handleQuery(context, request, handler);
            }
        }

        @Override
        public void handleRead(final ServerContext context, final ReadRequest request,
                final ResultHandler<Resource> handler) {
            if (hasNext()) {
                get().filterRead(context, request, handler, next());
            } else {
                target.handleRead(context, request, handler);
            }
        }

        @Override
        public void handleUpdate(final ServerContext context, final UpdateRequest request,
                final ResultHandler<Resource> handler) {
            if (hasNext()) {
                get().filterUpdate(context, request, handler, next());
            } else {
                target.handleUpdate(context, request, handler);
            }
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

    private final List<Filter> filters = new CopyOnWriteArrayList<Filter>();
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
    public void handleAction(final ServerContext context, final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        new Cursor().handleAction(context, request, handler);
    }

    @Override
    public void handleCreate(final ServerContext context, final CreateRequest request,
            final ResultHandler<Resource> handler) {
        new Cursor().handleCreate(context, request, handler);
    }

    @Override
    public void handleDelete(final ServerContext context, final DeleteRequest request,
            final ResultHandler<Resource> handler) {
        new Cursor().handleDelete(context, request, handler);
    }

    @Override
    public void handlePatch(final ServerContext context, final PatchRequest request,
            final ResultHandler<Resource> handler) {
        new Cursor().handlePatch(context, request, handler);
    }

    @Override
    public void handleQuery(final ServerContext context, final QueryRequest request,
            final QueryResultHandler handler) {
        new Cursor().handleQuery(context, request, handler);
    }

    @Override
    public void handleRead(final ServerContext context, final ReadRequest request,
            final ResultHandler<Resource> handler) {
        new Cursor().handleRead(context, request, handler);
    }

    @Override
    public void handleUpdate(final ServerContext context, final UpdateRequest request,
            final ResultHandler<Resource> handler) {
        new Cursor().handleUpdate(context, request, handler);
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
