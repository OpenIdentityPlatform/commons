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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.http.routing;

import org.forgerock.http.Context;
import org.forgerock.http.ResourcePath;
import org.forgerock.http.RoutingMode;
import org.forgerock.http.protocol.Request;

/**
 * A utility class that contains methods for creating route matchers.
 *
 * @since 1.0.0
 */
public final class RouteMatchers {

    /**
     * Creates a {@code RouteMatcher} instance that matches
     * {@code ResourcePath}s with the provided {@literal mode} and
     * {@literal template}.
     *
     * @param mode The routing mode.
     * @param template The uri template.
     * @return A {@code RouteMatcher} instance.
     */
    public static RouteMatcher<ResourcePath> uriMatcher(RoutingMode mode, String template) {
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
        RouteMatcher<ResourcePath> delegate = uriMatcher(mode, template);
        return new RequestUriRouteMatcher(delegate);
    }

    /**
     * A CHF specific {@code RouteMatcher} which extracts the routable path
     * from a {@code Request} and passes it as a {@code ResourcePath} to the
     * common {@code ResourcePath} route matcher.
     *
     * @since 1.0.0
     */
    private static final class RequestUriRouteMatcher extends RouteMatcher<Request> {

        private final RouteMatcher<ResourcePath> delegate;

        private RequestUriRouteMatcher(RouteMatcher<ResourcePath> delegate) {
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
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RequestUriRouteMatcher)) return false;
            RequestUriRouteMatcher that = (RequestUriRouteMatcher) o;
            return delegate.equals(that.delegate);
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }
    }

    static ResourcePath getRemainingRequestUri(Context context, Request request) {
        ResourcePath path = request.getUri().getResourcePath();
        if (context.containsContext(RouterContext.class)) {
            ResourcePath matchedUri = ResourcePath.valueOf(context.asContext(RouterContext.class).getBaseUri());
            path = path.tail(matchedUri.size());
        }
        return path;
    }
}
