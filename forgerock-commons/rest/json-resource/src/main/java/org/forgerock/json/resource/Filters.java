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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.json.resource;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * This class contains methods for creating various kinds of {@code Filter} and
 * {@code FilterCondition}s.
 */
public final class Filters {

    /**
     * A filter which invokes a sub-filter if a condition matches the request.
     */
    private static final class ConditionalFilter implements Filter {
        private final FilterCondition condition;
        private final Filter subFilter;

        private ConditionalFilter(final FilterCondition condition, final Filter filter) {
            this.condition = condition;
            this.subFilter = filter;
        }

        @Override
        public Promise<ActionResponse, ResourceException> filterAction(final Context context,
                final ActionRequest request, final RequestHandler next) {
            if (condition.matches(context, request)) {
                return subFilter.filterAction(context, request, next);
            } else {
                return next.handleAction(context, request);
            }
        }

        @Override
        public Promise<ResourceResponse, ResourceException> filterCreate(final Context context,
                final CreateRequest request, final RequestHandler next) {
            if (condition.matches(context, request)) {
                return subFilter.filterCreate(context, request, next);
            } else {
                return next.handleCreate(context, request);
            }
        }

        @Override
        public Promise<ResourceResponse, ResourceException> filterDelete(final Context context,
                final DeleteRequest request, final RequestHandler next) {
            if (condition.matches(context, request)) {
                return subFilter.filterDelete(context, request, next);
            } else {
                return next.handleDelete(context, request);
            }
        }

        @Override
        public Promise<ResourceResponse, ResourceException> filterPatch(final Context context,
                final PatchRequest request, final RequestHandler next) {
            if (condition.matches(context, request)) {
                return subFilter.filterPatch(context, request, next);
            } else {
                return next.handlePatch(context, request);
            }
        }

        @Override
        public Promise<QueryResponse, ResourceException> filterQuery(final Context context,
                final QueryRequest request, final QueryResourceHandler handler, final RequestHandler next) {
            if (condition.matches(context, request)) {
                return subFilter.filterQuery(context, request, handler, next);
            } else {
                return next.handleQuery(context, request, handler);
            }
        }

        @Override
        public Promise<ResourceResponse, ResourceException> filterRead(final Context context, final ReadRequest request,
                final RequestHandler next) {
            if (condition.matches(context, request)) {
                return subFilter.filterRead(context, request, next);
            } else {
                return next.handleRead(context, request);
            }
        }

        @Override
        public Promise<ResourceResponse, ResourceException> filterUpdate(final Context context,
                final UpdateRequest request, final RequestHandler next) {
            if (condition.matches(context, request)) {
                return subFilter.filterUpdate(context, request, next);
            } else {
                return next.handleUpdate(context, request);
            }
        }
    }

    /**
     * Returns a {@code FilterCondition} which will only match requests which
     * match all the provided conditions.
     *
     * @param conditions
     *            The conditions which requests must match.
     * @return The filter condition.
     */
    public static FilterCondition and(final Collection<FilterCondition> conditions) {
        return new FilterCondition() {
            @Override
            public boolean matches(final Context context, final Request request) {
                for (final FilterCondition condition : conditions) {
                    if (!condition.matches(context, request)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    /**
     * Returns a {@code FilterCondition} which will only match requests which
     * match all the provided conditions.
     *
     * @param conditions
     *            The conditions which requests must match.
     * @return The filter condition.
     */
    public static FilterCondition and(final FilterCondition... conditions) {
        return and(Arrays.asList(conditions));
    }

    /**
     * Returns a {@code Filter} which will only invoke {@code subFilter} when
     * the provided filter condition matches the request being processed.
     *
     * @param condition
     *            The filter condition.
     * @param subFilter
     *            The sub-filter to be invoked when the condition matches.
     * @return The wrapped filter.
     */
    public static Filter conditionalFilter(final FilterCondition condition, final Filter subFilter) {
        return new ConditionalFilter(condition, subFilter);
    }

    /**
     * Returns a {@code FilterCondition} which will only match requests whose
     * type is contained in {@code types}.
     *
     * @param types
     *            The request types which should be handled by the filter.
     * @return The filter condition.
     * @see Request#getRequestType()
     */
    public static FilterCondition matchRequestType(final RequestType... types) {
        return matchRequestType(EnumSet.copyOf(Arrays.asList(types)));
    }

    /**
     * Returns a {@code FilterCondition} which will only match requests whose
     * type is contained in {@code types}.
     *
     * @param types
     *            The request types which should be handled by the filter.
     * @return The filter condition.
     * @see Request#getRequestType()
     */
    public static FilterCondition matchRequestType(final Set<RequestType> types) {
        return new FilterCondition() {
            @Override
            public boolean matches(final Context context, final Request request) {
                return types.contains(request.getRequestType());
            }
        };
    }

    /**
     * Returns a {@code FilterCondition} which will only match requests whose
     * resource path matches the provided regular expression.
     *
     * @param regex
     *            The regular expression which must match a request's resource
     *            path.
     * @return The filter condition.
     * @see Request#getResourcePath()
     */
    public static FilterCondition matchResourcePath(final Pattern regex) {
        return new FilterCondition() {
            @Override
            public boolean matches(final Context context, final Request request) {
                return regex.matcher(request.getResourcePath()).matches();
            }
        };
    }

    /**
     * Returns a {@code FilterCondition} which will only match requests whose
     * resource path matches the provided regular expression.
     *
     * @param regex
     *            The regular expression which must match a request's resource
     *            path.
     * @return The filter condition.
     * @see Request#getResourcePath()
     */
    public static FilterCondition matchResourcePath(final String regex) {
        return matchResourcePath(Pattern.compile(regex));
    }

    /**
     * Returns a {@code FilterCondition} which will match requests which do not
     * match the provided condition.
     *
     * @param condition
     *            The condition which requests must not match.
     * @return The filter condition.
     */
    public static FilterCondition not(final FilterCondition condition) {
        return new FilterCondition() {
            @Override
            public boolean matches(final Context context, final Request request) {
                return !condition.matches(context, request);
            }
        };
    }

    /**
     * Returns a {@code FilterCondition} which will match requests which match
     * any of the provided conditions.
     *
     * @param conditions
     *            The conditions which requests may match.
     * @return The filter condition.
     */
    public static FilterCondition or(final Collection<FilterCondition> conditions) {
        return new FilterCondition() {
            @Override
            public boolean matches(final Context context, final Request request) {
                for (final FilterCondition condition : conditions) {
                    if (condition.matches(context, request)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    /**
     * Returns a {@code FilterCondition} which will match requests which match
     * any of the provided conditions.
     *
     * @param conditions
     *            The conditions which requests may match.
     * @return The filter condition.
     */
    public static FilterCondition or(final FilterCondition... conditions) {
        return or(Arrays.asList(conditions));
    }

    // Prevent instantiation.
    private Filters() {
        // Nothing to do.
    }

}
