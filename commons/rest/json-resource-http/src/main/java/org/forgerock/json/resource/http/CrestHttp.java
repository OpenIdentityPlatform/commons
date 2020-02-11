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
 * Copyright 2012-2016 ForgeRock AS.
 */

package org.forgerock.json.resource.http;

import static org.forgerock.json.resource.http.HttpUtils.*;

import java.net.URI;

import org.forgerock.json.resource.CrestApplication;
import org.forgerock.services.context.Context;
import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.filter.Filters;
import org.forgerock.http.handler.Handlers;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resources;
import org.forgerock.util.Reject;

/**
 * <p>A CREST HTTP utility class which creates instances of the {@link HttpAdapter}
 * to handle CREST HTTP requests.</p>
 *
 * <p>Instances must be provided with a {@code ConnectionFactory} in order to
 * operate and optionally a {@code HttpContextFactory}.</p>
 */
public final class CrestHttp {

    private CrestHttp() {
    }

    /**
     * Creates a new JSON resource HTTP Handler with the provided connection
     * factory and no context factory.
     *
     * @param connectionFactory
     *            The connection factory.
     * @return A CREST HTTP {@code Handler}.
     * @deprecated Use {@link #newHttpHandler(CrestApplication)} instead.
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public static Handler newHttpHandler(ConnectionFactory connectionFactory) {
        Reject.ifNull(connectionFactory);
        return Handlers.chainOf(new HttpAdapter(connectionFactory), newOptionsFilter());
    }

    /**
     * Creates a new JSON resource HTTP Handler with the provided connection
     * factory and a context factory which will always return the provided
     * request context.
     *
     * @param connectionFactory
     *            The connection factory.
     * @param parentContext
     *            The parent request context which should be used as the parent
     *            context of each request context.
     * @return A HTTP Handler.
     * @deprecated Use {@link #newHttpHandler(CrestApplication)} instead.
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public static Handler newHttpHandler(ConnectionFactory connectionFactory, Context parentContext) {
        Reject.ifNull(connectionFactory);
        Reject.ifNull(parentContext);
        return Handlers.chainOf(new HttpAdapter(connectionFactory, parentContext), newOptionsFilter());
    }

    /**
     * Creates a new JSON resource HTTP Handler with the provided connection
     * factory and context factory.
     *
     * @param connectionFactory
     *            The connection factory.
     * @param contextFactory
     *            The context factory which will be used to obtain the parent
     *            context of each request context.
     * @return A HTTP Handler.
     * @deprecated Use {@link #newHttpHandler(CrestApplication)} instead.
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public static Handler newHttpHandler(ConnectionFactory connectionFactory, HttpContextFactory contextFactory) {
        Reject.ifNull(connectionFactory);
        Reject.ifNull(contextFactory);
        return Handlers.chainOf(new HttpAdapter(connectionFactory, contextFactory), newOptionsFilter());
    }

    /**
     * Creates a new JSON resource HTTP handler with the provided CREST request handler.
     *
     * @param handler The {@link RequestHandler}.
     * @return A HTTP Handler.
     * @deprecated Use {@link #newHttpHandler(CrestApplication)} instead.
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public static Handler newHttpHandler(RequestHandler handler) {
        Reject.ifNull(handler);
        return Handlers.chainOf(new HttpAdapter(Resources.newInternalConnectionFactory(handler)), newOptionsFilter());
    }

    /**
     * Creates a new JSON resource HTTP handler with the provided CREST request handler.
     *
     * @param application The application.
     * @return The handler
     */
    public static Handler newHttpHandler(CrestApplication application) {
        Reject.ifNull(application);
        return Handlers.chainOf(new HttpAdapter(application, null), newOptionsFilter());
    }

    /**
     * Creates a new JSON resource HTTP handler with the provided CREST request handler.
     *
     * @param application The application.
     * @param factory A factory for creating parent HTTP Contexts.
     * @return The handler
     */
    public static Handler newHttpHandler(CrestApplication application, HttpContextFactory factory) {
        Reject.ifNull(application);
        return Handlers.chainOf(new HttpAdapter(application, factory), newOptionsFilter());
    }

    /**
     * Creates a new JSON resource HTTP handler with the provided CREST request handler.
     *
     * @param application The application.
     * @param context The parent context to use for all contexts.
     * @return The handler
     */
    public static Handler newHttpHandler(CrestApplication application, Context context) {
        Reject.ifNull(application);
        return Handlers.chainOf(new HttpAdapter(application, staticContextFactory(context)), newOptionsFilter());
    }

    /**
     * Creates a new {@link RequestHandler} that map back and forth JSON resource objects to CHF objects.
     *
     * @param handler
     *         HTTP {@link Handler} responsible for emitting the HTTP request build from JSON resource {@link
     *         org.forgerock.json.resource.Request}s.
     * @param uri
     *         base URI used to build the target URI for built HTTP message
     * @return a JSON resource {@link RequestHandler}
     */
    public static RequestHandler newRequestHandler(Handler handler, final URI uri) {
        return new CrestAdapter(handler, uri);
    }

    /**
     * Creates a new {@link ConnectionFactory} that map back and forth JSON resource objects to CHF objects.
     * <p>
     * Convenience method. Note that ConnectionFactory is going to be removed soon, so you may not need this.
     *
     * @param handler
     *         HTTP {@link Handler} responsible for emitting the HTTP request build from JSON resource {@link
     *         org.forgerock.json.resource.Request}s.
     * @param uri
     *         base URI used to build the target URI for built HTTP message
     * @return a JSON resource {@link RequestHandler}
     */
    public static ConnectionFactory newConnectionFactory(Handler handler, final URI uri) {
        return Resources.newInternalConnectionFactory(newRequestHandler(handler, uri));
    }

    private static Filter newOptionsFilter() {
        return Filters.newOptionsFilter(METHOD_DELETE,
                                        METHOD_GET,
                                        METHOD_HEAD,
                                        METHOD_PATCH,
                                        METHOD_PUT,
                                        METHOD_OPTIONS,
                                        METHOD_TRACE);
    }
}
