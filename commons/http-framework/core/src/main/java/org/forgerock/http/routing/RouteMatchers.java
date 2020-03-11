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

package org.forgerock.http.routing;

import java.util.List;

import org.forgerock.http.Filter;
import org.forgerock.http.header.AcceptApiVersionHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.util.Paths;
import org.forgerock.http.ApiProducer;
import org.forgerock.services.context.Context;
import org.forgerock.services.routing.RouteMatch;
import org.forgerock.services.routing.RouteMatcher;

/**
 * A utility class that contains methods for creating route matchers.
 */
public final class RouteMatchers {

    private RouteMatchers() {
        // Private utility constructor.
    }

    /**
     * Creates a {@code RouteMatcher} instance that matches
     * {@code ResourcePath}s with the provided {@literal mode} and
     * {@literal template}.
     *
     * @param mode The routing mode.
     * @param template The uri template.
     * @return A {@code RouteMatcher} instance.
     */
    public static RouteMatcher<List<String>> uriMatcher(RoutingMode mode, String template) {
        return new UriRouteMatcher(mode, template);
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
        RouteMatcher<List<String>> delegate = uriMatcher(mode, template);
        return new RequestUriRouteMatcher(delegate);
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
        return new ResourceApiVersionBehaviourManagerImpl();
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
    public static RouteMatcher<Version> resourceApiVersionMatcher(Version version) {
        return new ResourceApiVersionRouteMatcher(version);
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
     * A CHF specific {@code RouteMatcher} which extracts the routable path
     * from a {@code Request} and passes it as a {@code ResourcePath} to the
     * common {@code ResourcePath} route matcher.
     */
    private static final class RequestUriRouteMatcher extends RouteMatcher<Request> {

        private final RouteMatcher<List<String>> delegate;

        private RequestUriRouteMatcher(RouteMatcher<List<String>> delegate) {
            this.delegate = delegate;
        }

        @Override
        public RouteMatch evaluate(Context context, Request request) {
            return delegate.evaluate(context, getRemainingRequestUri(context, request));
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
        public <T> T transformApi(T descriptor, ApiProducer<T> producer) {
            return delegate.transformApi(descriptor, producer);
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }
    }

    static List<String> getRemainingRequestUri(Context context, Request request) {
        List<String> path = request.getUri().getPathElements();
        if (context.containsContext(UriRouterContext.class)) {
            List<String> matchedUri = Paths.getPathElements(context.asContext(UriRouterContext.class).getBaseUri());
            path = path.subList(matchedUri.size(), path.size());
        }
        return path;
    }

    /**
     * The default implementation of the {@code ResourceApiVersionBehaviourManager} interface.
     */
    private static final class ResourceApiVersionBehaviourManagerImpl implements ResourceApiVersionBehaviourManager {

        private boolean warningEnabled = true;
        private DefaultVersionBehaviour defaultVersionBehaviour = DefaultVersionBehaviour.LATEST;

        @Override
        public void setWarningEnabled(boolean warningEnabled) {
            this.warningEnabled = warningEnabled;
        }

        @Override
        public boolean isWarningEnabled() {
            return warningEnabled;
        }

        @Override
        public void setDefaultVersionBehaviour(DefaultVersionBehaviour behaviour) {
            this.defaultVersionBehaviour = behaviour;
        }

        @Override
        public DefaultVersionBehaviour getDefaultVersionBehaviour() {
            return defaultVersionBehaviour;
        }
    }

    /**
     * A CHF specific {@code RouteMatcher} which extracts the resource API
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
            AcceptApiVersionHeader apiVersionHeader;
            try {
                apiVersionHeader = AcceptApiVersionHeader.valueOf(request);
            } catch (IllegalArgumentException ignored) {
                // This exception is thrown when trying to parse an invalid
                // Accept-API-Version header. null is returned to signify a bad
                // request.
                return null;
            }
            return delegate.evaluate(context, apiVersionHeader.getResourceVersion());
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
        public <T> T transformApi(T descriptor, ApiProducer<T> producer) {
            return delegate.transformApi(descriptor, producer);
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }
    }
}
