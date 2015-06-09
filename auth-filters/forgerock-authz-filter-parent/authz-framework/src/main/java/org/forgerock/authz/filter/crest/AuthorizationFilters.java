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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.authz.filter.crest;

import static org.forgerock.util.promise.Promises.newExceptionPromise;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.authz.filter.crest.api.CrestAuthorizationModule;
import org.forgerock.http.ServerContext;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.FilterChain;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;

/**
 * This class contains methods for creating {@link FilterChain}s to protect resources by performing authorization on
 * each incoming request.
 *
 * @since 1.5.0
 */
public final class AuthorizationFilters {

    /**
     * Private utility constructor.
     */
    private AuthorizationFilters() {
        throw new UnsupportedOperationException("AuthorizationFilters cannot be instantiated.");
    }

    /**
     * Returns a new {@link FilterChain} which will perform authorization for each request before allowing access to the
     * provided collection resource provider.
     *
     * @param target The collection resource provider.
     * @param modules The {@code CrestAuthorizationModule}s that will perform authorization for each request.
     * @return A new {@code FilterChain} which will filter requests before allowing access to the provided collection
     * resource provider.
     * @throws java.lang.NullPointerException If either the specified {@code target} or {@code modules} parameters are
     * {@code null}.
     */
    public static FilterChain createFilter(CollectionResourceProvider target, CrestAuthorizationModule... modules) {
        Reject.ifNull(target, "Target cannot be null.");
        Reject.ifNull(modules, "Authorization module cannot be null.");
        Reject.ifTrue(modules.length == 0, "Authorization filters cannot be empty.");

        final List<Filter> filters = new ArrayList<Filter>();
        for (final CrestAuthorizationModule module : modules) {
            filters.add(new AuthorizationFilter(module));
        }

        return new FilterChain(Resources.newCollection(target), filters);
    }

    /**
     * Returns a new {@link FilterChain} which will perform authorization for each request before allowing access to the
     * provided singleton resource provider.
     *
     * @param target The singleton resource provider.
     * @param modules The {@code CrestAuthorizationModule}s that will perform authorization for each request.
     * @return A new {@code FilterChain} which will filter requests before allowing access to the provided singleton
     * resource provider.
     * @throws java.lang.NullPointerException If either the specified {@code target} or {@code modules} parameters are
     * {@code null}.
     */
    public static FilterChain createFilter(SingletonResourceProvider target, CrestAuthorizationModule... modules) {
        Reject.ifNull(target, "Target cannot be null.");
        Reject.ifNull(modules, "Authorization module cannot be null.");
        Reject.ifTrue(modules.length == 0, "Authorization filters cannot be empty.");

        final List<Filter> filters = new ArrayList<Filter>();
        for (final CrestAuthorizationModule module : modules) {
            filters.add(new AuthorizationFilter(module));
        }

        return new FilterChain(Resources.newSingleton(target), filters);
    }

    /**
     * Returns a new {@link FilterChain} which will perform authorization for each request before allowing access to the
     * provided RequestHandler.
     *
     * @param target The RequestHandler.
     * @param modules The {@code CrestAuthorizationModule}s that will perform authorization for each request.
     * @return A new {@code FilterChain} which will filter requests before allowing access to the provided RequestHandler.
     * @throws java.lang.NullPointerException If either the specified {@code target} or {@code modules} parameters are
     * {@code null}.
     */
    public static FilterChain createFilter(RequestHandler target, CrestAuthorizationModule... modules) {
        Reject.ifNull(target, "Target cannot be null.");
        Reject.ifNull(modules, "Authorization module cannot be null.");
        Reject.ifTrue(modules.length == 0, "Authorization filters cannot be empty.");

        final List<Filter> filters = new ArrayList<Filter>();
        for (final CrestAuthorizationModule module : modules) {
            filters.add(new AuthorizationFilter(module));
        }

        return new FilterChain(target, filters);
    }

    /**
     * <p>A {@code AuthorizationFilter} will filter requests based on the result of the authorization performed by the
     * specified {@link CrestAuthorizationModule}.</p>
     *
     * @since 1.5.0
     */
    private static final class AuthorizationFilter implements Filter {

        private final CrestAuthorizationModule module;

        /**
         * Creates a new {@code AuthorizationFilter} instance.
         *
         * @param module The {@code CrestAuthorizationModule} that will perform the authorization of requests.
         */
        private AuthorizationFilter(CrestAuthorizationModule module) {
            this.module = module;
        }

        /**
         * Creates a new unauthorized {@code ResourceException}.
         *
         * @param result An {@code AuthorizationResult} instance for a unauthorized request.
         * @return A {@code ResourceException} representing a {@code Unauthorized} request.
         */
        private ResourceException newUnauthorizedException(AuthorizationResult result) {
            final ResourceException e = ResourceException.getException(ResourceException.FORBIDDEN, result.getReason());
            e.setDetail(result.getDetail());
            return e;
        }

        /**
         * <p>Filters an action request based on the result of the authorization from the configured
         * {@link CrestAuthorizationModule}.</p>
         *
         * <p>If the request is authorized, the next {@code RequestHandler} in the chain is called. Otherwise a
         * unauthorized {@code ResourceException} is given to the {@code ResultHandler}.</p>
         *
         * @param context {@inheritDoc}
         * @param request {@inheritDoc}
         * @param next {@inheritDoc}
         * @return {@inheritDoc}
         */
        @Override
        public Promise<JsonValue, ResourceException> filterAction(final ServerContext context,
                final ActionRequest request, final RequestHandler next) {
            return module.authorizeAction(context, request)
                    .thenAsync(new AsyncFunction<AuthorizationResult, JsonValue, ResourceException>() {
                        @Override
                        public Promise<JsonValue, ResourceException> apply(AuthorizationResult result) {
                            if (result.isAuthorized()) {
                                return next.handleAction(context, request);
                            } else {
                                return newExceptionPromise(newUnauthorizedException(result));
                            }
                        }
                    });
        }

        /**
         * <p>Filters an create request based on the result of the authorization from the configured
         * {@link CrestAuthorizationModule}.</p>
         *
         * <p>If the request is authorized, the next {@code RequestHandler} in the chain is called. Otherwise a
         * unauthorized {@code ResourceException} is given to the {@code ResultHandler}.</p>
         *
         * @param context {@inheritDoc}
         * @param request {@inheritDoc}
         * @param next {@inheritDoc}
         * @return {@inheritDoc}
         */
        @Override
        public Promise<Resource, ResourceException> filterCreate(final ServerContext context,
                final CreateRequest request, final RequestHandler next) {
            return module.authorizeCreate(context, request)
                    .thenAsync(new AsyncFunction<AuthorizationResult, Resource, ResourceException>() {
                        @Override
                        public Promise<Resource, ResourceException> apply(AuthorizationResult result) {
                            if (result.isAuthorized()) {
                                return next.handleCreate(context, request);
                            } else {
                                return newExceptionPromise(newUnauthorizedException(result));
                            }
                        }
                    });
        }

        /**
         * <p>Filters an delete request based on the result of the authorization from the configured
         * {@link CrestAuthorizationModule}.</p>
         *
         * <p>If the request is authorized, the next {@code RequestHandler} in the chain is called. Otherwise a
         * unauthorized {@code ResourceException} is given to the {@code ResultHandler}.</p>
         *
         * @param context {@inheritDoc}
         * @param request {@inheritDoc}
         * @param next {@inheritDoc}
         * @return {@inheritDoc}
         */
        @Override
        public Promise<Resource, ResourceException> filterDelete(final ServerContext context,
                final DeleteRequest request, final RequestHandler next) {
            return module.authorizeDelete(context, request)
                    .thenAsync(new AsyncFunction<AuthorizationResult, Resource, ResourceException>() {
                        @Override
                        public Promise<Resource, ResourceException> apply(AuthorizationResult result) {
                            if (result.isAuthorized()) {
                                return next.handleDelete(context, request);
                            } else {
                                return newExceptionPromise(newUnauthorizedException(result));
                            }
                        }
                    });
        }

        /**
         * <p>Filters an patch request based on the result of the authorization from the configured
         * {@link CrestAuthorizationModule}.</p>
         *
         * <p>If the request is authorized, the next {@code RequestHandler} in the chain is called. Otherwise a
         * unauthorized {@code ResourceException} is given to the {@code ResultHandler}.</p>
         *
         * @param context {@inheritDoc}
         * @param request {@inheritDoc}
         * @param next {@inheritDoc}
         * @return {@inheritDoc}
         */
        @Override
        public Promise<Resource, ResourceException> filterPatch(final ServerContext context,
                final PatchRequest request, final RequestHandler next) {
            return module.authorizePatch(context, request)
                    .thenAsync(new AsyncFunction<AuthorizationResult, Resource, ResourceException>() {
                        @Override
                        public Promise<Resource, ResourceException> apply(AuthorizationResult result) {
                            if (result.isAuthorized()) {
                                return next.handlePatch(context, request);
                            } else {
                                return newExceptionPromise(newUnauthorizedException(result));
                            }
                        }
                    });
        }

        /**
         * <p>Filters an query request based on the result of the authorization from the configured
         * {@link CrestAuthorizationModule}.</p>
         *
         * <p>If the request is authorized, the next {@code RequestHandler} in the chain is called. Otherwise a
         * unauthorized {@code ResourceException} is given to the {@code ResultHandler}.</p>
         *
         * @param context {@inheritDoc}
         * @param request {@inheritDoc}
         * @param handler {@inheritDoc}
         * @param next {@inheritDoc}
         * @return {@inheritDoc}
         */
        @Override
        public Promise<QueryResult, ResourceException> filterQuery(final ServerContext context,
                final QueryRequest request, final QueryResourceHandler handler, final RequestHandler next) {
            return module.authorizeQuery(context, request)
                    .thenAsync(new AsyncFunction<AuthorizationResult, QueryResult, ResourceException>() {
                        @Override
                        public Promise<QueryResult, ResourceException> apply(AuthorizationResult result) {
                            if (result.isAuthorized()) {
                                return next.handleQuery(context, request, handler);
                            } else {
                                return newExceptionPromise(newUnauthorizedException(result));
                            }
                        }
                    });
        }

        /**
         * <p>Filters an read request based on the result of the authorization from the configured
         * {@link CrestAuthorizationModule}.</p>
         *
         * <p>If the request is authorized, the next {@code RequestHandler} in the chain is called. Otherwise a
         * unauthorized {@code ResourceException} is given to the {@code ResultHandler}.</p>
         *
         * @param context {@inheritDoc}
         * @param request {@inheritDoc}
         * @param next {@inheritDoc}
         * @return {@inheritDoc}
         */
        @Override
        public Promise<Resource, ResourceException> filterRead(final ServerContext context, final ReadRequest request,
                final RequestHandler next) {
            return module.authorizeRead(context, request)
                    .thenAsync(new AsyncFunction<AuthorizationResult, Resource, ResourceException>() {
                        @Override
                        public Promise<Resource, ResourceException> apply(AuthorizationResult result) {
                            if (result.isAuthorized()) {
                                return next.handleRead(context, request);
                            } else {
                                return newExceptionPromise(newUnauthorizedException(result));
                            }
                        }
                    });
        }

        /**
         * <p>Filters an update request based on the result of the authorization from the configured
         * {@link CrestAuthorizationModule}.</p>
         *
         * <p>If the request is authorized, the next {@code RequestHandler} in the chain is called. Otherwise a
         * unauthorized {@code ResourceException} is given to the {@code ResultHandler}.</p>
         *
         * @param context {@inheritDoc}
         * @param request {@inheritDoc}
         * @param next {@inheritDoc}
         * @return {@inheritDoc}
         */
        @Override
        public Promise<Resource, ResourceException> filterUpdate(final ServerContext context,
                final UpdateRequest request, final RequestHandler next) {
            return module.authorizeUpdate(context, request)
                    .thenAsync(new AsyncFunction<AuthorizationResult, Resource, ResourceException>() {
                        @Override
                        public Promise<Resource, ResourceException> apply(AuthorizationResult result) {
                            if (result.isAuthorized()) {
                                return next.handleUpdate(context, request);
                            } else {
                                return newExceptionPromise(newUnauthorizedException(result));
                            }
                        }
                    });
        }
    }
}
