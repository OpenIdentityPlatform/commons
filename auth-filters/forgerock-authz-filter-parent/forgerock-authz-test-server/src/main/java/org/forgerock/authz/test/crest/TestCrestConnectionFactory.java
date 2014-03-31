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
* Copyright 2014 ForgeRock AS.
*/
package org.forgerock.authz.test.crest;

import javax.servlet.ServletException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.Router;

/**
 * ConnectionFactory for the CREST / CAF Interop test.
 */
public class TestCrestConnectionFactory {

    private static final String ALL_ROUTES = "";

    /**
     * Creates a ConnectionFactory which maps everything that comes through it
     * to our TestCrestResource.
     *
     * @return ConnectionFactory which will create connections to our Test Resource
     * @throws ServletException If there's problems generating the factory
     */
    public static ConnectionFactory getConnectionFactory() throws ServletException {
        try {
            final Router router = new Router();
            router.addRoute(ALL_ROUTES, new TestCrestResource());
            return Resources.newInternalConnectionFactory(router);
        } catch (final Exception e) {
            throw new ServletException(e);
        }
    }

}
