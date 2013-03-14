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
            filter.handleActionRequest(context, request, next, new Handler<C, JsonValue>(handler) {
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
            filter.handleGenericRequest(context, request, next, new Handler<C, Resource>(handler) {
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
            filter.handleGenericRequest(context, request, next, new Handler<C, Resource>(handler) {
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
            filter.handleGenericRequest(context, request, next, new Handler<C, Resource>(handler) {
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
            filter.handleQueryRequest(context, request, next, new Handler<C, QueryResult>(handler) {
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
            filter.handleGenericRequest(context, request, next, new Handler<C, Resource>(handler) {
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
            filter.handleGenericRequest(context, request, next, new Handler<C, Resource>(handler) {
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
                    filter.handleActionError(context, filterContext, error, handler);
                }

                @Override
                public void handleResult(final JsonValue result) {
                    filter.handleActionResult(context, filterContext, result, handler);
                }
            };
        }

        private ResultHandler<Resource> wrapGeneric(final ServerContext context,
                final C filterContext, final ResultHandler<Resource> handler) {
            return new ResultHandler<Resource>() {
                @Override
                public void handleError(final ResourceException error) {
                    filter.handleGenericError(context, filterContext, error, handler);
                }

                @Override
                public void handleResult(final Resource result) {
                    filter.handleGenericResult(context, filterContext, result, handler);
                }
            };
        }

        private QueryResultHandler wrapQuery(final ServerContext context, final C filterContext,
                final QueryResultHandler handler) {
            return new QueryResultHandler() {
                @Override
                public void handleError(final ResourceException error) {
                    filter.handleQueryError(context, filterContext, error, handler);
                }

                @Override
                public boolean handleResource(final Resource resource) {
                    return handler.handleResource(resource);
                }

                @Override
                public void handleResult(final QueryResult result) {
                    filter.handleQueryResult(context, filterContext, result, handler);
                }
            };
        }
    }

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

    private static final class UntypedCrossCutFilterAdapter<C> implements CrossCutFilter<C> {
        private final UntypedCrossCutFilter<C> filter;

        private UntypedCrossCutFilterAdapter(final UntypedCrossCutFilter<C> filter) {
            this.filter = filter;
        }

        @Override
        public void handleActionError(final ServerContext context, final C filterContext,
                final ResourceException error, final ResultHandler<JsonValue> handler) {
            filter.handleGenericError(context, filterContext, error, checked(handler,
                    JsonValue.class));
        }

        @Override
        public void handleActionRequest(final ServerContext context, final ActionRequest request,
                final RequestHandler next, final CrossCutFilterResultHandler<C, JsonValue> handler) {
            filter.handleGenericRequest(context, request, next, checked(handler, JsonValue.class));
        }

        @Override
        public void handleActionResult(final ServerContext context, final C filterContext,
                final JsonValue result, final ResultHandler<JsonValue> handler) {
            filter.handleGenericResult(context, filterContext, result, checked(handler,
                    JsonValue.class));
        }

        @Override
        public void handleGenericError(final ServerContext context, final C filterContext,
                final ResourceException error, final ResultHandler<Resource> handler) {
            filter.handleGenericError(context, filterContext, error, checked(handler,
                    Resource.class));
        }

        @Override
        public void handleGenericRequest(final ServerContext context, final Request request,
                final RequestHandler next, final CrossCutFilterResultHandler<C, Resource> handler) {
            filter.handleGenericRequest(context, request, next, checked(handler, Resource.class));
        }

        @Override
        public void handleGenericResult(final ServerContext context, final C filterContext,
                final Resource result, final ResultHandler<Resource> handler) {
            filter.handleGenericResult(context, filterContext, result, checked(handler,
                    Resource.class));
        }

        @Override
        public void handleQueryError(final ServerContext context, final C filterContext,
                final ResourceException error, final ResultHandler<QueryResult> handler) {
            filter.handleGenericError(context, filterContext, error, checked(handler,
                    QueryResult.class));
        }

        @Override
        public void handleQueryRequest(final ServerContext context, final QueryRequest request,
                final RequestHandler next, final CrossCutFilterResultHandler<C, QueryResult> handler) {
            filter.handleGenericRequest(context, request, next, checked(handler, QueryResult.class));
        }

        @Override
        public void handleQueryResource(final ServerContext context, final C filterContext,
                final Resource resource, final ResultHandler<Resource> handler) {
            filter.handleQueryResource(context, filterContext, resource, handler);
        }

        @Override
        public void handleQueryResult(final ServerContext context, final C filterContext,
                final QueryResult result, final ResultHandler<QueryResult> handler) {
            filter.handleGenericResult(context, filterContext, result, checked(handler,
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
