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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.json.resource;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.forgerock.json.fluent.JsonValue;

/**
 * This class contains methods for creating various kinds of {@code Filter} and
 * {@code FilterPredicate}s.
 */
public final class Filters {

    /**
     * A filter which invokes a sub-filter if a predicate matches the request.
     */
    private static final class PredicatedFilter implements Filter {
        private final FilterPredicate predicate;
        private final Filter subFilter;

        private PredicatedFilter(final FilterPredicate predicate, final Filter filter) {
            this.predicate = predicate;
            this.subFilter = filter;
        }

        @Override
        public void filterAction(final ServerContext context, final ActionRequest request,
                final ResultHandler<JsonValue> handler, final RequestHandler next) {
            if (predicate.matches(context, request)) {
                subFilter.filterAction(context, request, handler, next);
            } else {
                next.handleAction(context, request, handler);
            }
        }

        @Override
        public void filterCreate(final ServerContext context, final CreateRequest request,
                final ResultHandler<Resource> handler, final RequestHandler next) {
            if (predicate.matches(context, request)) {
                subFilter.filterCreate(context, request, handler, next);
            } else {
                next.handleCreate(context, request, handler);
            }
        }

        @Override
        public void filterDelete(final ServerContext context, final DeleteRequest request,
                final ResultHandler<Resource> handler, final RequestHandler next) {
            if (predicate.matches(context, request)) {
                subFilter.filterDelete(context, request, handler, next);
            } else {
                next.handleDelete(context, request, handler);
            }
        }

        @Override
        public void filterPatch(final ServerContext context, final PatchRequest request,
                final ResultHandler<Resource> handler, final RequestHandler next) {
            if (predicate.matches(context, request)) {
                subFilter.filterPatch(context, request, handler, next);
            } else {
                next.handlePatch(context, request, handler);
            }
        }

        @Override
        public void filterQuery(final ServerContext context, final QueryRequest request,
                final QueryResultHandler handler, final RequestHandler next) {
            if (predicate.matches(context, request)) {
                subFilter.filterQuery(context, request, handler, next);
            } else {
                next.handleQuery(context, request, handler);
            }
        }

        @Override
        public void filterRead(final ServerContext context, final ReadRequest request,
                final ResultHandler<Resource> handler, final RequestHandler next) {
            if (predicate.matches(context, request)) {
                subFilter.filterRead(context, request, handler, next);
            } else {
                next.handleRead(context, request, handler);
            }
        }

        @Override
        public void filterUpdate(final ServerContext context, final UpdateRequest request,
                final ResultHandler<Resource> handler, final RequestHandler next) {
            if (predicate.matches(context, request)) {
                subFilter.filterUpdate(context, request, handler, next);
            } else {
                next.handleUpdate(context, request, handler);
            }
        }
    }

    /**
     * Returns a {@code FilterPredicate} which will only match requests which
     * match all the provided predicates.
     *
     * @param predicates
     *            The predicates which requests must match.
     * @return The filter predicate.
     */
    public static FilterPredicate and(final Collection<FilterPredicate> predicates) {
        return new FilterPredicate() {
            @Override
            public boolean matches(final ServerContext context, final Request request) {
                for (final FilterPredicate predicate : predicates) {
                    if (!predicate.matches(context, request)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    /**
     * Returns a {@code FilterPredicate} which will only match requests which
     * match all the provided predicates.
     *
     * @param predicates
     *            The predicates which requests must match.
     * @return The filter predicate.
     */
    public static FilterPredicate and(final FilterPredicate... predicates) {
        return and(Arrays.asList(predicates));
    }

    /**
     * Returns a {@code FilterPredicate} which will only match requests whose
     * type is contained in {@code types}.
     *
     * @param types
     *            The request types which should be handled by the filter.
     * @return The filter predicate.
     * @see Request#getRequestType()
     */
    public static FilterPredicate matchRequestType(final RequestType... types) {
        return matchRequestType(EnumSet.copyOf(Arrays.asList(types)));
    }

    /**
     * Returns a {@code FilterPredicate} which will only match requests whose
     * type is contained in {@code types}.
     *
     * @param types
     *            The request types which should be handled by the filter.
     * @return The filter predicate.
     * @see Request#getRequestType()
     */
    public static FilterPredicate matchRequestType(final Set<RequestType> types) {
        return new FilterPredicate() {
            @Override
            public boolean matches(final ServerContext context, final Request request) {
                return types.contains(request.getRequestType());
            }
        };
    }

    /**
     * Returns a {@code FilterPredicate} which will only match requests whose
     * resource name matches the provided regular expression.
     *
     * @param regex
     *            The regular expression which must match a request's resource
     *            name.
     * @return The filter predicate.
     * @see Request#getResourceName()
     */
    public static FilterPredicate matchResourceName(final Pattern regex) {
        return new FilterPredicate() {
            @Override
            public boolean matches(final ServerContext context, final Request request) {
                return regex.matcher(request.getResourceName()).matches();
            }
        };
    }

    /**
     * Returns a {@code FilterPredicate} which will only match requests whose
     * resource name matches the provided regular expression.
     *
     * @param regex
     *            The regular expression which must match a request's resource
     *            name.
     * @return The filter predicate.
     * @see Request#getResourceName()
     */
    public static FilterPredicate matchResourceName(final String regex) {
        return matchResourceName(Pattern.compile(regex));
    }

    /**
     * Returns a {@code FilterPredicate} which will match requests which do not
     * match the provided predicate.
     *
     * @param predicate
     *            The predicate which requests must not match.
     * @return The filter predicate.
     */
    public static FilterPredicate not(final FilterPredicate predicate) {
        return new FilterPredicate() {
            @Override
            public boolean matches(final ServerContext context, final Request request) {
                return !predicate.matches(context, request);
            }
        };
    }

    /**
     * Returns a {@code FilterPredicate} which will match requests which match
     * any of the provided predicates.
     *
     * @param predicates
     *            The predicates which requests may match.
     * @return The filter predicate.
     */
    public static FilterPredicate or(final Collection<FilterPredicate> predicates) {
        return new FilterPredicate() {
            @Override
            public boolean matches(final ServerContext context, final Request request) {
                for (final FilterPredicate predicate : predicates) {
                    if (predicate.matches(context, request)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    /**
     * Returns a {@code FilterPredicate} which will match requests which match
     * any of the provided predicates.
     *
     * @param predicates
     *            The predicates which requests may match.
     * @return The filter predicate.
     */
    public static FilterPredicate or(final FilterPredicate... predicates) {
        return or(Arrays.asList(predicates));
    }

    /**
     * Returns a {@code Filter} which will only invoke {@code subFilter} when
     * the provided filter predicate matches the request being processed.
     *
     * @param predicate
     *            The filter predicate.
     * @param subFilter
     *            The sub-filter to be invoked when the predicate matches.
     * @return The wrapped filter.
     */
    public static Filter predicatedFilter(final FilterPredicate predicate, final Filter subFilter) {
        return new PredicatedFilter(predicate, subFilter);
    }

    // Prevent instantiation.
    private Filters() {
        // Nothing to do.
    }

}
