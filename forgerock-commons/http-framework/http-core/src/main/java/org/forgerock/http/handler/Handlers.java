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

package org.forgerock.http.handler;

import static org.forgerock.http.protocol.Response.newResponsePromise;
import static org.forgerock.http.protocol.Responses.newInternalServerError;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import org.forgerock.http.ApiProducer;
import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.services.context.Context;
import org.forgerock.services.descriptor.Describable;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

import io.swagger.models.Swagger;

/**
 * Utility methods for creating common types of handlers.
 */
public final class Handlers {

    private Handlers() {
        // Prevent instantiation.
    }

    /**
     * Creates a "filtered handler" instance.
     *
     * <p>It will invoke the {@code first} filter, giving it the {@code handler} handler as {@code next}.
     *
     * @param handler The filtered instance
     * @param filter the filter to apply
     * @return a new {@link Handler} instance that filters the given {@code handler}.
     */
    public static DescribableHandler filtered(final Handler handler, final Filter filter) {
        final Describable<Swagger, Request> describable = asDescribableHandler(handler);
        return new DescribableHandler() {
            @Override
            public Promise<Response, NeverThrowsException> handle(final Context context, final Request request) {
                return filter.filter(context, request, handler);
            }

            @Override
            public Swagger api(ApiProducer<Swagger> producer) {
                return describable.api(producer);
            }

            @Override
            public Swagger handleApiRequest(Context context, Request request) {
                return describable.handleApiRequest(context, request);
            }

            @Override
            public void addDescriptorListener(Listener listener) {
                describable.addDescriptorListener(listener);
            }

            @Override
            public void removeDescriptorListener(Listener listener) {
                describable.removeDescriptorListener(listener);
            }
        };
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
    public static DescribableHandler chainOf(final Handler handler, final Filter... filters) {
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
    public static DescribableHandler chainOf(final Handler handler, final List<Filter> filters) {
        // Create a cons-list structure:
        //   Given [A, B, C, D] filters and a H handler
        //   Build a (A . (B . (C . (D . H)))) handler chain
        DescribableHandler result = asDescribableHandler(handler);
        if (filters != null) {
            ListIterator<Filter> i = filters.listIterator(filters.size());
            while (i.hasPrevious()) {
                result = filtered(result, i.previous());
            }
        }
        return result;
    }

    /**
     * Adapts a {@link Handler} to a {@link DescribableHandler} without adding support for API Descriptions if it is
     * not already implemented.
     * @param handler The handler.
     * @return The describable handler.
     */
    @SuppressWarnings("unchecked")
    public static DescribableHandler asDescribableHandler(final Handler handler) {
        if (handler instanceof DescribableHandler) {
            return (DescribableHandler) handler;
        }
        if (handler instanceof Describable) {
            return new HandlerDescribableAsDescribableHandler(handler, (Describable<Swagger, Request>) handler);
        }
        return new UndescribedAsDescribableHandler(handler);
    }

    private static class HandlerDescribableAsDescribableHandler implements DescribableHandler {
        private final Handler handler;
        private final Describable<Swagger, Request> describable;

        public HandlerDescribableAsDescribableHandler(Handler handler, Describable<Swagger, Request> describable) {
            this.handler = handler;
            this.describable = describable;
        }

        @Override
        public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
            return handler.handle(context, request);
        }

        @Override
        public Swagger api(ApiProducer<Swagger> producer) {
            return describable.api(producer);
        }

        @Override
        public Swagger handleApiRequest(Context context, Request request) {
            return describable.handleApiRequest(context, request);
        }

        @Override
        public void addDescriptorListener(Listener listener) {
            describable.addDescriptorListener(listener);
        }

        @Override
        public void removeDescriptorListener(Listener listener) {
            describable.removeDescriptorListener(listener);
        }
    }

    private static class UndescribedAsDescribableHandler implements DescribableHandler {
        private final Handler handler;

        public UndescribedAsDescribableHandler(Handler handler) {
            this.handler = handler;
        }

        @Override
        public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
            return handler.handle(context, request);
        }

        @Override
        public Swagger api(ApiProducer<Swagger> producer) {
            return null;
        }

        @Override
        public Swagger handleApiRequest(Context context, Request request) {
            return null;
        }

        @Override
        public void addDescriptorListener(Listener listener) {

        }

        @Override
        public void removeDescriptorListener(Listener listener) {

        }
    }

    /**
     * A common HTTP Framework {@link Handler} responding 500 Internal Server Error.
     * @param cause The cause of the internal server error.
     * @return The handler.
     */
    public static Handler internalServerErrorHandler(final Exception cause) {
        return new Handler() {
            @Override
            public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
                return newResponsePromise(newInternalServerError(cause));
            }
        };
    }

}
