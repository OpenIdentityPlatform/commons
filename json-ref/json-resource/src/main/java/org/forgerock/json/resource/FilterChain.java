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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.forgerock.json.fluent.JsonValue;

/**
 * A chain of filters terminated by a target request handler.
 */
public final class FilterChain implements RequestHandler {
    private final class Cursor implements RequestHandler {
        private final int pos;

        private Cursor() {
            this(0);
        }

        private Cursor(final int pos) {
            this.pos = pos;
        }

        @Override
        public void handleAction(final ServerContext context, final ActionRequest request,
                final ResultHandler<JsonValue> handler) {
            if (pos < filters.size()) {
                filters.get(pos).filterAction(context, request, handler, new Cursor(pos + 1));
            } else {
                target.handleAction(context, request, handler);
            }
        }

        @Override
        public void handleCreate(final ServerContext context, final CreateRequest request,
                final ResultHandler<Resource> handler) {
            if (pos < filters.size()) {
                filters.get(pos).filterCreate(context, request, handler, new Cursor(pos + 1));
            } else {
                target.handleCreate(context, request, handler);
            }
        }

        @Override
        public void handleDelete(final ServerContext context, final DeleteRequest request,
                final ResultHandler<Resource> handler) {
            if (pos < filters.size()) {
                filters.get(pos).filterDelete(context, request, handler, new Cursor(pos + 1));
            } else {
                target.handleDelete(context, request, handler);
            }
        }

        @Override
        public void handlePatch(final ServerContext context, final PatchRequest request,
                final ResultHandler<Resource> handler) {
            if (pos < filters.size()) {
                filters.get(pos).filterPatch(context, request, handler, new Cursor(pos + 1));
            } else {
                target.handlePatch(context, request, handler);
            }
        }

        @Override
        public void handleQuery(final ServerContext context, final QueryRequest request,
                final QueryResultHandler handler) {
            if (pos < filters.size()) {
                filters.get(pos).filterQuery(context, request, handler, new Cursor(pos + 1));
            } else {
                target.handleQuery(context, request, handler);
            }
        }

        @Override
        public void handleRead(final ServerContext context, final ReadRequest request,
                final ResultHandler<Resource> handler) {
            if (pos < filters.size()) {
                filters.get(pos).filterRead(context, request, handler, new Cursor(pos + 1));
            } else {
                target.handleRead(context, request, handler);
            }
        }

        @Override
        public void handleUpdate(final ServerContext context, final UpdateRequest request,
                final ResultHandler<Resource> handler) {
            if (pos < filters.size()) {
                filters.get(pos).filterUpdate(context, request, handler, new Cursor(pos + 1));
            } else {
                target.handleUpdate(context, request, handler);
            }
        }

        @Override
        public String toString() {
            return String.valueOf(pos);
        }

    }

    private final List<Filter> filters;
    private final RequestHandler target;

    /**
     * Creates an empty filter chain.
     *
     * @param target
     *            The target request handler to be invoked once processing has
     *            reached the end of the chain.
     */
    public FilterChain(final RequestHandler target) {
        this.target = target;
        this.filters = Collections.emptyList();
    }

    /**
     * Creates a filter chain containing the provided list of filters.
     *
     * @param target
     *            The target request handler to be invoked once processing has
     *            reached the end of the chain.
     * @param filters
     *            The list of filters to be processed before invoking the
     *            target.
     */
    public FilterChain(final RequestHandler target, final Collection<Filter> filters) {
        this.target = target;
        this.filters = new ArrayList<Filter>(filters);
    }

    /**
     * Creates a filter chain containing the provided list of filters.
     *
     * @param target
     *            The target request handler to be invoked once processing has
     *            reached the end of the chain.
     * @param filters
     *            The list of filters to be processed before invoking the
     *            target.
     */
    public FilterChain(final RequestHandler target, final Filter... filters) {
        this.target = target;
        this.filters = Arrays.asList(filters);
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

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(filters.toString());
        builder.append(" -> ");
        builder.append(target.toString());
        return builder.toString();
    }

}
