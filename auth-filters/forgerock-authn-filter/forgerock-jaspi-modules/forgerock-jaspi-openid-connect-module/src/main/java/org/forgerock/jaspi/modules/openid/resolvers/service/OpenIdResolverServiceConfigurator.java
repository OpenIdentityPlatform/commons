/** The contents of this file are subject to the terms of the Common Development and
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

/**
 * Interface directing how to generate (
 * {@link OpenIdResolverServiceConfigurator#setupService(String, String, String)}) and
 * then configure ({@link OpenIdResolverServiceConfigurator#configureService(OpenIdResolverService, java.util.List)})
 * an {@link OpenIdResolverService}.
 */
public interface OpenIdResolverServiceConfigurator {

    /**
     * Configures a provided {@link OpenIdResolverService} using the resolver information held
     * in a {@link List} of {@link Map}
     *
     * @param service to configure
     * @param resolvers the configuration
     * @return false if any resolver configuration fails true otherwise
     */
    public boolean configureService(final OpenIdResolverService service, final List<Map<String, String>> resolvers);

    /**
     * Provides an OpenIdResolverService, using the provided keystore details.
     *
     * @param keystoreType type of keystore
     * @param keystoreLocation location of the keystore
     * @param keystorePassword password to access the keystore
     * @return a usable OpenIdResolverService, null otherwise
     */
    public OpenIdResolverService setupService(final String keystoreType, final String keystoreLocation,
                                              final String keystorePassword);

}
