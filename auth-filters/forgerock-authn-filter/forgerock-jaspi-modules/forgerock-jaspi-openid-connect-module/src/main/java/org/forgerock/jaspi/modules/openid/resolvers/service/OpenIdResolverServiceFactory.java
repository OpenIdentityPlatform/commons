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

/**
 * Produces OpenIdResolverService implementations for the Configurator
 */
public class OpenIdResolverServiceFactory {

    /**
     * Creates an OpenIdResolverService using the given keystore information.
     *
     * @param keystoreType The type of Java KeyStore.
     * @param keystoreLocation The file path to the KeyStore.
     * @param keystorePassword The password for the KeyStore.
     * @return a new {@link OpenIdResolverService} implementation
     */
    public OpenIdResolverService createOpenIdResolverService(final String keystoreType,
                                                             final String keystoreLocation,
                                                             final String keystorePassword) {
            return new OpenIdResolverServiceImpl(keystoreType, keystoreLocation, keystorePassword);
    }

}
