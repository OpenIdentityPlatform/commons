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

package org.forgerock.http.filter;

import java.util.Arrays;
import java.util.List;

import org.forgerock.http.Context;
import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.SessionManager;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * Utility methods for creating common types of filters.
 */
public final class Filters {

    private Filters() {
        // Prevent instantiation.
    }

    /**
     * Creates a {@link Filter} which handles HTTP OPTIONS method requests.
     *
     * @param allowedMethods The allowed HTTP methods of the endpoint.
     * @return A {@code Filter}.
     */
    public static Filter newOptionsFilter(String... allowedMethods) {
        return new OptionsFilter(allowedMethods);
    }

    /**
     * Creates a session {@link Filter} that will use the provided
     * {@link SessionManager} to manage the users session.
     *
     * @param sessionManager The {@code SessionManager}.
     * @return A session {@code Filter}.
     */
    public static Filter newSessionFilter(SessionManager sessionManager) {
        return new SessionFilter(sessionManager);
    }

    /**
     * Creates a {@link Filter} which encapsulates the provided {@literal filters}
     * into a single {@code Filter}.
     *
     * @param filters The list of filters to be invoked, in order.
     * @return A {@code Filter}.
     * @see #chainOf(List)
     */
    public static Filter chainOf(final Filter... filters) {
        return chainOf(Arrays.asList(filters));
    }

    /**
     * Creates a {@link Filter} which encapsulates the provided {@literal filters}
     * into a single {@code Filter}.
     *
     * @param filters The list of filters to be invoked, in order.
     * @return A {@code Filter}.
     * @see #chainOf(Filter...)
     */
    public static Filter chainOf(final List<Filter> filters) {
        return new Filter() {
            @Override
            public Promise<Response, NeverThrowsException> filter(Context context, Request request, Handler next) {
                return new Chain(next, filters, 0).handle(context, request);
            }
        };
    }

    private static final class Chain implements Handler {
        private final Handler handler;
        private final List<Filter> filters;
        private final int position;

        private Chain(Handler handler, List<Filter> filters, int position) {
            this.handler = handler;
            this.filters = filters;
            this.position = position;
        }

        @Override
        public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
            if (position < filters.size()) {
                return filters.get(position).filter(context, request, next());
            } else {
                return handler.handle(context, request);
            }
        }

        private Handler next() {
            return new Chain(handler, filters, position + 1);
        }

        @Override
        public String toString() {
            return filters.toString() + " -> " + handler.toString();
        }
    }
}
