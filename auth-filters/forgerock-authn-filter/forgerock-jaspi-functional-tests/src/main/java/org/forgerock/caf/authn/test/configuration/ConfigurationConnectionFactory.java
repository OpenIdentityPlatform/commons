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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.caf.authn.test.configuration;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.UriRouter;

/**
 * CREST connection factory for all the test CREST resources.
 *
 * @since 1.5.0
 */
public final class ConfigurationConnectionFactory {

    /**
     * Private utility constructor.
     */
    private ConfigurationConnectionFactory() {
    }

    /**
     * Creates a {@code ConnectionFactory} which contains routes to each CREST resource.
     *
     * @return The {@code ConnectionFactory}.
     */
    public static ConnectionFactory getConnectionFactory() {

        UriRouter router = new UriRouter();

        router.addRoute("/configuration", InjectorHolder.getInstance(ConfigurationResource.class));
        router.addRoute("/auditrecords", InjectorHolder.getInstance(AuditResource.class));

        return Resources.newInternalConnectionFactory(router);
    }
}
