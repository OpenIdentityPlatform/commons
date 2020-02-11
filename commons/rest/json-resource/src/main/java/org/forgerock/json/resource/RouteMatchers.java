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

package org.forgerock.json.resource;

import static org.forgerock.http.routing.RouteMatchers.resourceApiVersionMatcher;
import static org.forgerock.http.routing.RouteMatchers.uriMatcher;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.http.routing.ResourceApiVersionBehaviourManager;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.http.routing.Version;
import org.forgerock.http.ApiProducer;
import org.forgerock.services.context.Context;
import org.forgerock.services.routing.RouteMatch;
import org.forgerock.services.routing.RouteMatcher;

/**
 * A utility class that contains methods for creating route matchers.
 */
public final class RouteMatchers {

    private RouteMatchers() {
    }

    /**
     * Creates a {@code RouteMatcher} instance that matches {@code Request}s
     * with the provided {@literal mode} and {@literal template}.
     *
     * @param mode The routing mode.
     * @param template The uri template.
     * @return A {@code RouteMatcher} instance.
     */
    public static RouteMatcher<Request> requestUriMatcher(RoutingMode mode, String template) {
        return new RequestUriRouteMatcher(uriMatcher(mode, template));
    }

    /**
     * Creates a new {@code ResourceApiVersionBehaviourManager} which is responsibly
     * for managing whether warning headers are returned and the default
     * version behaviour when the {@literal Accept-API-Version} header is not
     * present on the request.
     *
     * @return A new {@code ResourceApiVersionBehaviourManager}.
     */
    public static ResourceApiVersionBehaviourManager newResourceApiVersionBehaviourManager() {
        return org.forgerock.http.routing.RouteMatchers.newResourceApiVersionBehaviourManager();
    }

    /**
     * Creates a {@code Filter} which MUST be placed, in the route, before any
     * API Version routing takes place.
     *
     * <p>The filter will add the required {@code Context}s, default version
     * behaviour and response headers.</p>
     *
     * @param behaviourManager A {@code ResourceApiVersionBehaviourManager} instance.
     * @return A {@code Filter} instance.
     */
    public static Filter resourceApiVersionContextFilter(ResourceApiVersionBehaviourManager behaviourManager) {
        return new ResourceApiVersionRoutingFilter(behaviourManager);
    }

    /**
     * Creates a {@code RouteMatcher} instance that matches the request
     * resource API version with the provided {@literal version}.
     *
     * @param version The API version of the resource.
     * @return A {@code RouteMatcher} instance.
     */
    public static RouteMatcher<Request> requestResourceApiVersionMatcher(Version version) {
        return new RequestApiVersionRouteMatcher(resourceApiVersionMatcher(version));
    }

    /**
     * A CREST specific {@code RouteMatcher} which extracts the requests
     * resource name from a {@code Request} and passes it as a
     * {@code ResourcePath} to the common {@code ResourcePath} route predicate.
     */
    private static final class RequestUriRouteMatcher extends RouteMatcher<Request> {

        private final RouteMatcher<List<String>> delegate;

        private RequestUriRouteMatcher(RouteMatcher<List<String>> delegate) {
            this.delegate = delegate;
        }

        @Override
        public RouteMatch evaluate(Context context, Request request) {
            final List<String> pathElements = new ArrayList<>(request.getResourcePathObject().size());
            for (String pathElement : request.getResourcePathObject()) {
                pathElements.add(pathElement);
            }
            return delegate.evaluate(context, pathElements);
        }

        @Override
        public String toString() {
            return delegate.toString();
        }

        @Override
        public String idFragment() {
            return delegate.idFragment();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof RequestUriRouteMatcher)) {
                return false;
            }
            RequestUriRouteMatcher that = (RequestUriRouteMatcher) o;
            return delegate.equals(that.delegate);
        }

        @Override
        public <T> T transformApi(T t, ApiProducer<T> apiProducer) {
            return delegate.transformApi(t, apiProducer);
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }
    }

    /**
     * A CREST specific {@code RouteMatcher} which extracts the resource API
     * version from a {@code Request} and passes it to the common
     * {@code Version} route matcher.
     */
    private static final class RequestApiVersionRouteMatcher extends RouteMatcher<Request> {

        private final RouteMatcher<Version> delegate;

        private RequestApiVersionRouteMatcher(RouteMatcher<Version> delegate) {
            this.delegate = delegate;
        }

        @Override
        public RouteMatch evaluate(Context context, Request request) {
            return delegate.evaluate(context, request.getResourceVersion());
        }

        @Override
        public String toString() {
            return delegate.toString();
        }

        @Override
        public String idFragment() {
            return delegate.idFragment();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof RequestApiVersionRouteMatcher)) {
                return false;
            }
            RequestApiVersionRouteMatcher that = (RequestApiVersionRouteMatcher) o;
            return delegate.equals(that.delegate);
        }

        @Override
        public <T> T transformApi(T t, ApiProducer<T> apiProducer) {
            return delegate.transformApi(t, apiProducer);
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }
    }
}
