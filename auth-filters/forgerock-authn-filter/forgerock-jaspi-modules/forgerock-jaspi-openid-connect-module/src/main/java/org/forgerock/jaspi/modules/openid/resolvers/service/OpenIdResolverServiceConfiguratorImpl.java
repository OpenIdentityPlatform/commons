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
package org.forgerock.jaspi.modules.openid.resolvers.service;

import java.util.List;
import java.util.Map;
import org.forgerock.auth.common.DebugLogger;
import org.forgerock.jaspi.logging.LogFactory;
import org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver;

/**
 * Implementation of the {@link OpenIdResolverServiceConfigurator} interface.
 */
public class OpenIdResolverServiceConfiguratorImpl implements OpenIdResolverServiceConfigurator {

    private static final DebugLogger DEBUG = LogFactory.getDebug();
    private final OpenIdResolverServiceFactory factory;

    /**
     * Default constructor
     */
    public OpenIdResolverServiceConfiguratorImpl() {
        this.factory = new OpenIdResolverServiceFactory();
    }

    /**
     * Used for tests
     *
     * @param factory to use to generate ResolverServiceImpl
     */
    OpenIdResolverServiceConfiguratorImpl(final OpenIdResolverServiceFactory factory) {
        this.factory = factory;
    }

    /**
     * {@inheritDoc}
     */
    public boolean configureService(final OpenIdResolverService service, final List<Map<String, String>> resolvers) {

        if (resolvers == null || resolvers.size() < 1) {
            return false;
        }

        boolean atLeastOne = false;

        for (Map<String, String> resolverConfig : resolvers) {

            final String clientId = resolverConfig.get(OpenIdResolver.CLIENT_ID_KEY);
            final String issuer = resolverConfig.get(OpenIdResolver.ISSUER_KEY);
            final String keyAlias = resolverConfig.get(OpenIdResolver.KEY_ALIAS_KEY);
            final String clientSecret = resolverConfig.get(OpenIdResolver.CLIENT_SECRET_KEY);

            if ((keyAlias == null && clientSecret == null) || (keyAlias != null && clientSecret != null)) {
                DEBUG.debug("Resolver configuration must include only one of keyAlias or clientSecret");
                continue;
            }

            if(keyAlias != null) {
                if (!service.configureResolverWithKey(clientId, issuer, keyAlias)) {
                    DEBUG.debug("Unable to configure resolver using keyAlias for " + issuer);
                } else {
                    atLeastOne = true;
                }
            } else {
                if (!service.configureResolverWithSecret(clientId, issuer, clientSecret)) {
                    DEBUG.debug("Unable to configure resolver using sharedSecret for " + issuer);
                } else {
                    atLeastOne = true;
                }
            }
        }

        return atLeastOne;

    }

    /**
     * {@inheritDoc}
     */
    public OpenIdResolverService setupService(final String keystoreType, final String keystoreLocation,
                                              final String keystorePassword) {
        return factory.createOpenIdResolverService(keystoreType, keystoreLocation, keystorePassword);

    }
}
