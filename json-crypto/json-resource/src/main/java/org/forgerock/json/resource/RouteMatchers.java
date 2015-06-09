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

package org.forgerock.json.resource;

import static org.forgerock.http.routing.RouteMatchers.uriMatcher;

import org.forgerock.http.Context;
import org.forgerock.http.ResourcePath;
import org.forgerock.http.RoutingMode;
import org.forgerock.http.routing.RouteMatch;
import org.forgerock.http.routing.RouteMatcher;

/**
 * A utility class that contains methods for creating route matchers.
 *
 * @since 3.0.0
 */
public final class RouteMatchers {

    /**
     * Creates a {@code RouteMatcher} instance that matches {@code Request}s
     * with the provided {@literal mode} and {@literal template}.
     *
     * @param mode The routing mode.
     * @param template The uri template.
     * @return A {@code RouteMatcher} instance.
     */
    public static RouteMatcher<Request> requestUriMatcher(RoutingMode mode, String template) {
        return new RequestUriRoutePredicate(uriMatcher(mode, template));
    }

    /**
     * A CREST specific {@code RouteMatcher} which extracts the requests
     * resource name from a {@code Request} and passes it as a
     * {@code ResourcePath} to the common {@code ResourcePath} route predicate.
     *
     * @since 3.0.0
     */
    private static final class RequestUriRoutePredicate extends RouteMatcher<Request> {

        private final RouteMatcher<ResourcePath> delegate;

        private RequestUriRoutePredicate(RouteMatcher<ResourcePath> delegate) {
            this.delegate = delegate;
        }

        @Override
        public RouteMatch evaluate(Context context, Request request) {
            return delegate.evaluate(context, request.getResourcePathObject());
        }

        @Override
        public String toString() {
            return delegate.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RequestUriRoutePredicate)) return false;
            RequestUriRoutePredicate that = (RequestUriRoutePredicate) o;
            return delegate.equals(that.delegate);
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }
    }
}
