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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.services.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.forgerock.http.ApiProducer;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.services.context.Context;
import org.forgerock.services.descriptor.Describable;
import org.forgerock.util.Pair;

/**
 * An abstract base class for implementing routers. Routers are common in applications which need to process incoming
 * requests based on varying criteria such as the target endpoint, API version expectations, client criteria (e.g.
 * source address), etc. This base class is designed to be protocol and framework independent. Frameworks should
 * sub-class an abstract router in order to provide framework specific behavior.
 * <p>
 * Generally speaking a router comprises of a series of routes, each of which is composed of a {@link RouteMatcher}
 * and a handler (H). When a request (R) is received the router invokes each {@code RouteMatcher} to see if it
 * matches and then invokes the associated handler if it is the best match.
 * <p>
 * Concrete implementations of {@code AbstractRouter} existing in both {@link org.forgerock.http.routing.Router CHF}
 * and CREST.
 *
 * @param <T> The type of the router.
 * @param <R> The type of the request.
 * @param <H> The type of the handler that will be used to handle routing requests.
 * @param <D> The type of descriptor object that the APIs supported by this router can be described using.
 */
public abstract class AbstractRouter<T extends AbstractRouter<T, R, H, D>, R, H, D>
        implements Describable<D, R>, Describable.Listener {

    private final Map<RouteMatcher<R>, H> routes = new ConcurrentHashMap<>();
    /** Matches the current route. */
    protected final RouteMatcher<R> thisRouterUriMatcher = uriMatcher(RoutingMode.EQUALS, "");
    private final List<Describable.Listener> apiListeners = new CopyOnWriteArrayList<>();
    private volatile H defaultRoute;
    private ApiProducer<D> apiProducer;
    /** Api of the current router. */
    protected D api;

    /** Creates a new abstract router with no routes defined. */
    protected AbstractRouter() {
    }

    /**
     * Creates a new router containing the same routes and default route as the
     * provided router. Changes to the returned router's routing table will not
     * impact the provided router.
     *
     * @param router The router to be copied.
     */
    @SuppressWarnings("unchecked")
    protected AbstractRouter(AbstractRouter<T, R, H, D> router) {
        this.defaultRoute = router.defaultRoute;
        addAllRoutes((T) router);
    }

    /**
     * Returns this {@code AbstractRouter} instance, typed correctly.
     *
     * @return This {@code AbstractRouter} instance.
     */
    protected abstract T getThis();

    /**
     * Gets all registered routes on this router.
     *
     * @return All registered routes.
     */
    protected final Map<RouteMatcher<R>, H> getRoutes() {
        return Collections.unmodifiableMap(routes);
    }

    /**
     * Adds all of the routes defined in the provided router to this router.
     * New routes may be added while this router is processing requests.
     *
     * @param router The router whose routes are to be copied into this router.
     * @return This router instance.
     */
    public final T addAllRoutes(T router) {
        if (this != router) {
            boolean descriptorChanged = false;
            for (Map.Entry<RouteMatcher<R>, H> route : router.getRoutes().entrySet()) {
                H handler = route.getValue();
                descriptorChanged |= updateApiDescriptor(routes.put(route.getKey(), handler), handler);
            }
            if (descriptorChanged) {
                notifyDescriptorChange();
            }
        }
        return getThis();
    }

    /**
     * Adds a new route to this router for the provided handler. New routes may
     * be added while this router is processing requests.
     *
     * <p>The provided {@literal matcher} can be used to remove this route
     * later.</p>
     *
     * @param matcher The {@code RouteMatcher} that will evaluate whether
     *                  the incoming request matches this route.
     * @param handler The handler to which matching requests will be routed.
     * @return This router instance.
     */
    public final T addRoute(RouteMatcher<R> matcher, H handler) {
        return updateApiDescriptorAndNotify(routes.put(matcher, handler), handler);
    }

    private boolean updateApiDescriptor(H oldHandler, H newHandler) {
        boolean oldHandlerDescribable = oldHandler instanceof Describable;
        boolean newHandlerDescribable = newHandler instanceof Describable;
        if (oldHandlerDescribable) {
            ((Describable) oldHandler).removeDescriptorListener(this);
        }
        if (newHandlerDescribable) {
            ((Describable) newHandler).addDescriptorListener(this);
        }
        return oldHandlerDescribable || newHandlerDescribable;
    }

    private T updateApiDescriptorAndNotify(H oldHandler, H newHandler) {
        if (updateApiDescriptor(oldHandler, newHandler)) {
            notifyDescriptorChange();
        }
        return getThis();
    }

    /**
     * Sets the handler to be used as the default route for requests which do
     * not match any of the other defined routes.
     *
     * @param handler The handler to be used as the default route.
     * @return This router instance.
     */
    public final T setDefaultRoute(H handler) {
        H oldDefault = this.defaultRoute;
        this.defaultRoute = handler;
        return updateApiDescriptorAndNotify(oldDefault, handler);
    }

    /**
     * Returns the handler to be used as the default route for requests which
     * do not match any of the other defined routes.
     *
     * @return The handler to be used as the default route.
     */
    final H getDefaultRoute() {
        return defaultRoute;
    }

    /**
     * Removes all of the routes from this router. Routes may be removed while
     * this router is processing requests.
     *
     * @return This router instance.
     */
    public final T removeAllRoutes() {
        routes.clear();
        api = null;
        return getThis();
    }

    /**
     * Removes one or more routes from this router. Routes may be removed while
     * this router is processing requests.
     *
     * @param routes The {@code RouteMatcher}s of the routes to be removed.
     * @return {@code true} if at least one of the routes was found and removed.
     */
    @SafeVarargs
    public final boolean removeRoute(RouteMatcher<R>... routes) {
        boolean isModified = false;
        boolean apiDescriptorModified = false;
        for (RouteMatcher<R> route : routes) {
            H removed = this.routes.remove(route);
            isModified |= removed != null;
            apiDescriptorModified |= updateApiDescriptor(removed, null);
        }
        if (apiDescriptorModified) {
            notifyDescriptorChange();
        }
        return isModified;
    }

    /**
     * Finds the best route that matches the given request based on the route
     * matchers of the registered routes. If no registered route matches at
     * all then the default route is chosen, if present.
     *
     * @param context The request context.
     * @param request The request to be matched against the registered routes.
     * @return A {@code Pair} containing the decorated {@code Context} and the
     * handler which is the best match for the given request or {@code null} if
     * no route was found.
     * @throws IncomparableRouteMatchException If any of the registered
     * {@code RouteMatcher}s could not be compared to one another.
     */
    protected Pair<Context, H> getBestRoute(Context context, R request) throws IncomparableRouteMatchException {
        H handler = null;
        RouteMatch bestMatch = null;
        for (Map.Entry<RouteMatcher<R>, H> route : routes.entrySet()) {
            RouteMatch result = route.getKey().evaluate(context, request);
            if (result != null) {
                if (result.isBetterMatchThan(bestMatch)) {
                    handler = route.getValue();
                    bestMatch = result;
                }
            }
        }
        if (bestMatch != null) {
            return Pair.of(bestMatch.decorateContext(context), handler);
        }

        final H dftRoute = defaultRoute;
        return dftRoute != null ? Pair.of(context, dftRoute) : null;
    }

    @Override
    public synchronized D api(ApiProducer<D> producer) {
        if (apiProducer == null) {
            this.apiProducer = producer;
            updateApi();
        }
        return this.api;
    }

    private void updateApi() {
        if (this.apiProducer != null) {
            this.api = buildApi(this.apiProducer);
        }
    }

    /**
     * Build an api with a given {@link ApiProducer}.
     *
     * @param producer The given ApiProducer to use.
     * @return an api.
     */
    @SuppressWarnings("unchecked")
    protected D buildApi(ApiProducer<D> producer) {
        List<D> descriptors = new ArrayList<>(routes.size());
        for (Map.Entry<RouteMatcher<R>, H> route : routes.entrySet()) {
            H handler = route.getValue();
            if (handler instanceof Describable) {
                RouteMatcher<R> matcher = route.getKey();
                D descriptor = ((Describable<D, R>) handler).api(producer.newChildProducer(matcher.idFragment()));
                descriptors.add(matcher.transformApi(descriptor, producer));
            }
        }
        final H dftRoute = defaultRoute;
        if (dftRoute instanceof Describable) {
            descriptors.add(((Describable<D, R>) dftRoute).api(producer));
        }
        return descriptors.isEmpty() ? null : producer.merge(descriptors);
    }

    /**
     * Create a URI matcher suitable for the request type {@code <R>}.
     * @param mode The routing mode.
     * @param pattern The pattern.
     * @return The matcher.
     */
    protected abstract RouteMatcher<R> uriMatcher(RoutingMode mode, String pattern);

    @Override
    @SuppressWarnings("unchecked")
    public D handleApiRequest(Context context, R request) {
        try {
            Pair<Context, H> bestRoute = getBestRoute(context, request);
            H handler = bestRoute != null ? bestRoute.getSecond() : null;
            if (handler instanceof Describable) {
                Context nextContext = bestRoute.getFirst();
                return ((Describable<D, R>) handler).handleApiRequest(nextContext, request);
            }
        } catch (IncomparableRouteMatchException e) {
            throw new IllegalStateException(e);
        }
        if (thisRouterUriMatcher.evaluate(context, request) != null) {
            return this.api;
        }
        throw new UnsupportedOperationException("No route matched the request " + request);
    }

    private void notifyListeners() {
        for (Describable.Listener listener : apiListeners) {
            listener.notifyDescriptorChange();
        }
    }

    @Override
    public void addDescriptorListener(Describable.Listener listener) {
        apiListeners.add(listener);
    }

    @Override
    public void removeDescriptorListener(Describable.Listener listener) {
        apiListeners.remove(listener);
    }

    @Override
    public void notifyDescriptorChange() {
        updateApi();
        notifyListeners();
    }
}
