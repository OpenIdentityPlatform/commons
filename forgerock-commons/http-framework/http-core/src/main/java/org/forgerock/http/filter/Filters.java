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

package org.forgerock.http.filter;

import java.util.Arrays;
import java.util.List;

import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.handler.Handlers;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.session.SessionManager;
import org.forgerock.services.context.Context;
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
     * Shared, stateless and immutable empty filter singleton (no-op).
     * It just forwards the request and return the next's response.
     */
    private static final Filter EMPTY_FILTER = new Filter() {
        @Override
        public Promise<Response, NeverThrowsException> filter(Context context, Request request, Handler next) {
            return next.handle(context, request);
        }
    };

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

        // Create a cons-list structure recursively:
        //   Given [A, B, C, D] filters
        //   Build a (A . (B . (C . D))) filter chain

        if (filters.isEmpty()) {
            return EMPTY_FILTER;
        }

        if (filters.size() == 1) {
            return filters.get(0);
        }

        return combine(filters.get(0),
                       chainOf(filters.subList(1, filters.size())));
    }

    /**
     * Combine 2 filters together in a composite filter ({@literal filter#1 + filter#2 -> filter(filter#1 >
     * filter#2)}).
     *
     * <p>When the composite filter is invoked, it first invokes the {@code first} filter, providing it with a dedicated
     * "next handler" instance.
     *
     * <p>This "next handler" instance, when called, invokes the {@code second} filter and provides it with the
     * "original" {@code next} handler.
     */
    private static Filter combine(final Filter first, final Filter second) {
        return new Filter() {
            @Override
            public Promise<Response, NeverThrowsException> filter(Context context, Request request, Handler next) {
                // Execute first filter, that hand-off to the second filter
                // that in turns is provided with the original next handler
                return first.filter(context, request, Handlers.filtered(next, second));
            }
        };
    }
}
