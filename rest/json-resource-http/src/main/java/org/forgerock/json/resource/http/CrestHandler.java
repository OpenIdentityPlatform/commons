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
 * Copyright 2012-2014 ForgeRock AS.
 */

package org.forgerock.json.resource.http;

import org.forgerock.http.Context;
import org.forgerock.http.Handler;
import org.forgerock.http.Http;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.util.Reject;

/**
 * <p>A CREST HTTP utility class which creates instances of the {@link HttpAdapter}
 * to handle CREST HTTP requests.</p>
 *
 * <p>Instances must be provided with a {@code ConnectionFactory} in order to
 * operate and optionally a {@code HttpContextFactory}.</p>
 *
 * @since 3.0.0
 */
public class CrestHandler {

    private CrestHandler() {
        // Private utility constructor.
    }

    /**
     * Creates a new JSON resource HTTP Handler with the provided connection
     * factory and no context factory.
     *
     * @param connectionFactory
     *            The connection factory.
     */
    public static Handler newHandler(ConnectionFactory connectionFactory) {
        Reject.ifNull(connectionFactory);
        return Http.chainOf(new HttpAdapter(connectionFactory), new OptionsHandler());
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
     */
    public static Handler newHandler(ConnectionFactory connectionFactory, Context parentContext) {
        Reject.ifNull(connectionFactory);
        Reject.ifNull(parentContext);
        return Http.chainOf(new HttpAdapter(connectionFactory, parentContext), new OptionsHandler());
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
     */
    public static Handler newHandler(ConnectionFactory connectionFactory, HttpContextFactory contextFactory) {
        Reject.ifNull(connectionFactory);
        Reject.ifNull(contextFactory);
        return Http.chainOf(new HttpAdapter(connectionFactory, contextFactory), new OptionsHandler());
    }

//    public void destroy() { //TODO how to hook this into the shutdown of the application? Maybe its the HttpApplications job?
//        if (connectionFactory != null) {
//            connectionFactory.close();
//        }
//    }
}
