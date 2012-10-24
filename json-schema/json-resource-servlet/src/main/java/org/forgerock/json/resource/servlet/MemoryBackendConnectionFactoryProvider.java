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
 * Copyright 2012 ForgeRock AS.
 */
package org.forgerock.json.resource.servlet;

import static org.forgerock.json.resource.RoutingMode.EQUALS;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.InMemoryBackend;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.Router;

/**
 * Default connection factory provider.
 */
final class MemoryBackendConnectionFactoryProvider {
    private static final String INIT_PARAM_URI_TEMPLATE = "uri-template";

    // Prevent instantiation.
    private MemoryBackendConnectionFactoryProvider() {
        // Nothing to do.
    }

    static ConnectionFactory getConnectionFactory(final ServletConfig config)
            throws ServletException {
        final String uriTemplate = config.getInitParameter(INIT_PARAM_URI_TEMPLATE);
        if (uriTemplate == null) {
            throw new ServletException("Servlet initialization parameter '"
                    + INIT_PARAM_URI_TEMPLATE + "' not specified");
        }
        final Router router = new Router();
        router.addRoute(EQUALS, uriTemplate, new InMemoryBackend());
        return Resources.newInternalConnectionFactory(router);
    }
}
