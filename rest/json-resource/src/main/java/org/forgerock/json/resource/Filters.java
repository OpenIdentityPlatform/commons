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
 * {@code FilterCondition}s.
 */
public final class Filters {

    private static final class CrossCutFilterAdapter<C> implements Filter {
        private static abstract class Handler<C, R> implements CrossCutFilterResultHandler<C, R> {
            private final ResultHandler<R> handler;

            Handler(final ResultHandler<R> handler) {
                this.handler = handler;
            }

            @Override
            public void handleError(final ResourceException error) {
                handler.handleError(error);
            }

            @Override
            public void handleResult(final R response) {
                handler.handleResult(response);
            }
        }

        private final CrossCutFilter<C> filter;

        private CrossCutFilterAdapter(final CrossCutFilter<C> filter) {
            this.filter = filter;
        }

        @Override
        public void filterAction(final ServerContext context, final ActionRequest request,
                final ResultHandler<JsonValue> handler, final RequestHandler next) {
            filter.filterActionRequest(context, request, next, new Handler<C, JsonValue>(handler) {
                @Override
                public void handleContinue(final ServerContext newContext, final C filterContext) {
                    next.handleAction(newContext, request, wrapAction(newContext, filterContext,
                            handler));
                }
            });
        }

        @Override
        public void filterCreate(final ServerContext context, final CreateRequest request,
                final ResultHandler<Resource> handler, final RequestHandler next) {
            filter.filterGenericRequest(context, request, next, new Handler<C, Resource>(handler) {
                @Override
                public void handleContinue(final ServerContext newContext, final C filterContext) {
                    next.handleCreate(newContext, request, wrapGeneric(newContext, filterContext,
                            handler));
                }
            });
        }

        @Override
        public void filterDelete(final ServerContext context, final DeleteRequest request,
                final ResultHandler<Resource> handler, final RequestHandler next) {
            filter.filterGenericRequest(context, request, next, new Handler<C, Resource>(handler) {
                @Override
                public void handleContinue(final ServerContext newContext, final C filterContext) {
                    next.handleDelete(newContext, request, wrapGeneric(newContext, filterContext,
                            handler));
                }
            });
        }

        @Override
        public void filterPatch(final ServerContext context, final PatchRequest request,
                final ResultHandler<Resource> handler, final RequestHandler next) {
            filter.filterGenericRequest(context, request, next, new Handler<C, Resource>(handler) {
                @Override
                public void handleContinue(final ServerContext newContext, final C filterContext) {
                    next.handlePatch(newContext, request, wrapGeneric(newContext, filterContext,
                            handler));
                }
            });
        }

        @Override
        public void filterQuery(final ServerContext context, final QueryRequest request,
                final QueryResultHandler handler, final RequestHandler next) {
            filter.filterQueryRequest(context, request, next, new Handler<C, QueryResult>(handler) {
                @Override
                public void handleContinue(final ServerContext newContext, final C filterContext) {
                    next.handleQuery(newContext, request, wrapQuery(newContext, filterContext,
                            handler));
                }
            });
        }

        @Override
        public void filterRead(final ServerContext context, final ReadRequest request,
                final ResultHandler<Resource> handler, final RequestHandler next) {
            filter.filterGenericRequest(context, request, next, new Handler<C, Resource>(handler) {
                @Override
                public void handleContinue(final ServerContext newContext, final C filterContext) {
                    next.handleRead(newContext, request, wrapGeneric(newContext, filterContext,
                            handler));
                }
            });
        }

        @Override
        public void filterUpdate(final ServerContext context, final UpdateRequest request,
                final ResultHandler<Resource> handler, final RequestHandler next) {
            filter.filterGenericRequest(context, request, next, new Handler<C, Resource>(handler) {
                @Override
                public void handleContinue(final ServerContext newContext, final C filterContext) {
                    next.handleUpdate(newContext, request, wrapGeneric(newContext, filterContext,
                            handler));
                }
            });
        }

        private ResultHandler<JsonValue> wrapAction(final ServerContext context,
                final C filterContext, final ResultHandler<JsonValue> handler) {
            return new ResultHandler<JsonValue>() {
                @Override
                public void handleError(final ResourceException error) {
                    filter.filterActionError(context, filterContext, error, handler);
                }

                @Override
                public void handleResult(final JsonValue result) {
                    filter.filterActionResult(context, filterContext, result, handler);
                }
            };
        }

        private ResultHandler<Resource> wrapGeneric(final ServerContext context,
                final C filterContext, final ResultHandler<Resource> handler) {
            return new ResultHandler<Resource>() {
                @Override
                public void handleError(final ResourceException error) {
                    filter.filterGenericError(context, filterContext, error, handler);
                }

                @Override
                public void handleResult(final Resource result) {
                    filter.filterGenericResult(context, filterContext, result, handler);
                }
            };
        }

        private QueryResultHandler wrapQuery(final ServerContext context, final C filterContext,
                final QueryResultHandler handler) {
            return new QueryResultHandler() {
                @Override
                public void handleError(final ResourceException error) {
                    filter.filterQueryError(context, filterContext, error, handler);
                }

                @Override
                public boolean handleResource(final Resource resource) {
                    // The resource may be null if it was filtered out.
                    return resource == null || handler.handleResource(resource);
                }

                @Override
                public void handleResult(final QueryResult result) {
                    filter.filterQueryResult(context, filterContext, result, handler);
                }
            };
        }
    }

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
        public void filterAction(final ServerContext context, final ActionRequest request,
                final ResultHandler<JsonValue> handler, final RequestHandler next) {
            if (condition.matches(context, request)) {
                subFilter.filterAction(context, request, handler, next);
            } else {
                next.handleAction(context, request, handler);
            }
        }

        @Override
        public void filterCreate(final ServerContext context, final CreateRequest request,
                final ResultHandler<Resource> handler, final RequestHandler next) {
            if (condition.matches(context, request)) {
                subFilter.filterCreate(context, request, handler, next);
            } else {
                next.handleCreate(context, request, handler);
            }
        }

        @Override
        public void filterDelete(final ServerContext context, final DeleteRequest request,
                final ResultHandler<Resource> handler, final RequestHandler next) {
            if (condition.matches(context, request)) {
                subFilter.filterDelete(context, request, handler, next);
            } else {
                next.handleDelete(context, request, handler);
            }
        }

        @Override
        public void filterPatch(final ServerContext context, final PatchRequest request,
                final ResultHandler<Resource> handler, final RequestHandler next) {
            if (condition.matches(context, request)) {
                subFilter.filterPatch(context, request, handler, next);
            } else {
                next.handlePatch(context, request, handler);
            }
        }

        @Override
        public void filterQuery(final ServerContext context, final QueryRequest request,
                final QueryResultHandler handler, final RequestHandler next) {
            if (condition.matches(context, request)) {
                subFilter.filterQuery(context, request, handler, next);
            } else {
                next.handleQuery(context, request, handler);
            }
        }

        @Override
        public void filterRead(final ServerContext context, final ReadRequest request,
                final ResultHandler<Resource> handler, final RequestHandler next) {
            if (condition.matches(context, request)) {
                subFilter.filterRead(context, request, handler, next);
            } else {
                next.handleRead(context, request, handler);
            }
        }

        @Override
        public void filterUpdate(final ServerContext context, final UpdateRequest request,
                final ResultHandler<Resource> handler, final RequestHandler next) {
            if (condition.matches(context, request)) {
                subFilter.filterUpdate(context, request, handler, next);
            } else {
                next.handleUpdate(context, request, handler);
            }
        }
    }

    private static final class UntypedCrossCutFilterAdapter<C> implements CrossCutFilter<C> {
        private final UntypedCrossCutFilter<C> filter;

        private UntypedCrossCutFilterAdapter(final UntypedCrossCutFilter<C> filter) {
            this.filter = filter;
        }

        @Override
        public void filterActionError(final ServerContext context, final C filterContext,
                final ResourceException error, final ResultHandler<JsonValue> handler) {
            filter.filterGenericError(context, filterContext, error, checked(handler,
                    JsonValue.class));
        }

        @Override
        public void filterActionRequest(final ServerContext context, final ActionRequest request,
                final RequestHandler next, final CrossCutFilterResultHandler<C, JsonValue> handler) {
            filter.filterGenericRequest(context, request, next, checked(handler, JsonValue.class));
        }

        @Override
        public void filterActionResult(final ServerContext context, final C filterContext,
                final JsonValue result, final ResultHandler<JsonValue> handler) {
            filter.filterGenericResult(context, filterContext, result, checked(handler,
                    JsonValue.class));
        }

        @Override
        public void filterGenericError(final ServerContext context, final C filterContext,
                final ResourceException error, final ResultHandler<Resource> handler) {
            filter.filterGenericError(context, filterContext, error, checked(handler,
                    Resource.class));
        }

        @Override
        public void filterGenericRequest(final ServerContext context, final Request request,
                final RequestHandler next, final CrossCutFilterResultHandler<C, Resource> handler) {
            filter.filterGenericRequest(context, request, next, checked(handler, Resource.class));
        }

        @Override
        public void filterGenericResult(final ServerContext context, final C filterContext,
                final Resource result, final ResultHandler<Resource> handler) {
            filter.filterGenericResult(context, filterContext, result, checked(handler,
                    Resource.class));
        }

        @Override
        public void filterQueryError(final ServerContext context, final C filterContext,
                final ResourceException error, final ResultHandler<QueryResult> handler) {
            filter.filterGenericError(context, filterContext, error, checked(handler,
                    QueryResult.class));
        }

        @Override
        public void filterQueryRequest(final ServerContext context, final QueryRequest request,
                final RequestHandler next, final CrossCutFilterResultHandler<C, QueryResult> handler) {
            filter.filterGenericRequest(context, request, next, checked(handler, QueryResult.class));
        }

        @Override
        public void filterQueryResource(final ServerContext context, final C filterContext,
                final Resource resource, final ResultHandler<Resource> handler) {
            filter.filterQueryResource(context, filterContext, resource, handler);
        }

        @Override
        public void filterQueryResult(final ServerContext context, final C filterContext,
                final QueryResult result, final ResultHandler<QueryResult> handler) {
            filter.filterGenericResult(context, filterContext, result, checked(handler,
                    QueryResult.class));
        }

        private <R> CrossCutFilterResultHandler<C, Object> checked(
                final CrossCutFilterResultHandler<C, R> handler, final Class<R> clazz) {
            return new CrossCutFilterResultHandler<C, Object>() {
                @Override
                public void handleContinue(final ServerContext context, final C filterContext) {
                    handler.handleContinue(context, filterContext);
                }

                @Override
                public void handleError(final ResourceException error) {
                    handler.handleError(error);
                }

                @Override
                public void handleResult(final Object response) {
                    try {
                        handler.handleResult(clazz.cast(response));
                    } catch (final ClassCastException e) {
                        handler.handleError(new InternalServerErrorException(e));
                    }
                }
            };
        }

        private <R> ResultHandler<Object> checked(final ResultHandler<R> handler,
                final Class<R> clazz) {
            return new ResultHandler<Object>() {
                @Override
                public void handleError(final ResourceException error) {
                    handler.handleError(error);
                }

                @Override
                public void handleResult(final Object response) {
                    try {
                        handler.handleResult(clazz.cast(response));
                    } catch (final ClassCastException e) {
                        handler.handleError(new InternalServerErrorException(e));
                    }
                }
            };
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
            public boolean matches(final ServerContext context, final Request request) {
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
     * Returns a {@code Filter} adapter for the provided cross-cutting filter.
     *
     * @param crossCutFilter
     *            The cross-cutting filter to be adapted.
     * @return The adapted filter.
     */
    public static Filter asFilter(final CrossCutFilter<?> crossCutFilter) {
        return asFilter0(crossCutFilter);
    }

    /**
     * Returns a {@code Filter} adapter for the provided cross-cutting filter.
     *
     * @param crossCutFilter
     *            The cross-cutting filter to be adapted.
     * @return The adapted filter.
     */
    public static Filter asFilter(final UntypedCrossCutFilter<?> crossCutFilter) {
        return asFilter0(crossCutFilter);
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
            public boolean matches(final ServerContext context, final Request request) {
                return types.contains(request.getRequestType());
            }
        };
    }

    /**
     * Returns a {@code FilterCondition} which will only match requests whose
     * resource name matches the provided regular expression.
     *
     * @param regex
     *            The regular expression which must match a request's resource
     *            name.
     * @return The filter condition.
     * @see Request#getResourceName()
     */
    public static FilterCondition matchResourceName(final Pattern regex) {
        return new FilterCondition() {
            @Override
            public boolean matches(final ServerContext context, final Request request) {
                return regex.matcher(request.getResourceName()).matches();
            }
        };
    }

    /**
     * Returns a {@code FilterCondition} which will only match requests whose
     * resource name matches the provided regular expression.
     *
     * @param regex
     *            The regular expression which must match a request's resource
     *            name.
     * @return The filter condition.
     * @see Request#getResourceName()
     */
    public static FilterCondition matchResourceName(final String regex) {
        return matchResourceName(Pattern.compile(regex));
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
            public boolean matches(final ServerContext context, final Request request) {
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
            public boolean matches(final ServerContext context, final Request request) {
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

    private static <C> Filter asFilter0(final CrossCutFilter<C> filter) {
        return new CrossCutFilterAdapter<C>(filter);
    }

    private static <C> Filter asFilter0(final UntypedCrossCutFilter<C> filter) {
        return new CrossCutFilterAdapter<C>(new UntypedCrossCutFilterAdapter<C>(filter));
    }

    // Prevent instantiation.
    private Filters() {
        // Nothing to do.
    }

}
