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

package org.forgerock.audit;

import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.RoutingMode;
import org.forgerock.audit.impl.AuditServiceImpl;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

public class AuditServiceConnectionFactoryProvider {
    private static final String INIT_PARAM_URI_TEMPLATE = "uri-template";

    private AuditServiceConnectionFactoryProvider() {

    }

    public static ConnectionFactory getConnectionFactory(final ServletConfig config)
            throws ServletException {
        final String uriTemplate = config.getInitParameter(INIT_PARAM_URI_TEMPLATE);
        if (uriTemplate == null) {
            throw new ServletException("Servlet initialization parameter '"
                    + INIT_PARAM_URI_TEMPLATE + "' not specified");
        }
        final Router router = new Router();
        router.addRoute(RoutingMode.STARTS_WITH, "/audit", new AuditServiceImpl());
        return Resources.newInternalConnectionFactory(router);
    }
}
