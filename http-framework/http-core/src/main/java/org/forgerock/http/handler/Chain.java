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

import java.util.List;

import org.forgerock.http.Context;
import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * A chain of zero or more filters and one handler. The chain is responsible for dispatching the request to
 * each filter in the chain, and finally the handler.
 * <p>
 * When a chain dispatches a request to a filter, it creates a "subchain" (a subset of this chain, which
 * contains the remaining downstream filters and handler), and passes it as a parameter to the filter.
 * For this reason, a filter should make no assumptions or correlations using the chain
 * it is supplied with when invoked.
 * <p>
 * A filter may elect to terminate dispatching of the exchange to the rest of the chain by not calling
 * {@link #handle(Context, Request)} and generate its own response or dispatch to a completely different handler.
 */
final class Chain implements Handler {
    private final Handler handler;
    private final List<Filter> filters;
    private final int position;

    Chain(Handler handler, List<Filter> filters, int position) {
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
