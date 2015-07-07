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

package org.forgerock.http.handler;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.forgerock.http.Filter;
import org.forgerock.http.Handler;

/**
 * Utility methods for creating common types of handlers.
 */
public final class Handlers {

    private Handlers() {
        // Prevent instantiation.
    }

    /**
     * Creates a {@link Handler} which wraps the provided {@literal filters}
     * around the provided target {@literal handler}.
     *
     * @param handler The target handler which will be invoked once
     *                processing has reached the end of the filter chain.
     * @param filters The list of filters to be processed before invoking the
     *                target.
     * @return A {@code Handler}.
     * @see #chainOf(Handler, List)
     */
    public static Handler chainOf(final Handler handler, final Filter... filters) {
        return chainOf(handler, Arrays.asList(filters));
    }

    /**
     * Creates a {@link Handler} which wraps the provided {@literal filters}
     * around the provided target {@literal handler}.
     *
     * @param handler The target handler which will be invoked once
     *                processing has reached the end of the filter chain.
     * @param filters The list of filters to be processed before invoking the
     *                target.
     * @return A {@code Handler}.
     * @see #chainOf(Handler, Filter...)
     */
    public static Handler chainOf(final Handler handler, final List<Filter> filters) {
        return new Chain(handler, filters, 0);
    }

    /**
     * Creates a {@link Filter} which encapsulates the provided {@literal filters}
     * into a single {@code Filter}.
     *
     * @param filters The list of filters to be invoked, in order.
     * @return A {@code Filter}.
     * @see #chainOf(Collection)
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
    public static Filter chainOf(final Collection<Filter> filters) {
        // TODO: return a subsequence of filters.
        return null;
    }

}
