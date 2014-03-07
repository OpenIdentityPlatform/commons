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

import org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver;

/**
 * Interface through which OpenIdResolvers are obtained, and the service providing
 * them is configured.
 */
public interface OpenIdResolverService {

    /**
     * Returns the appropriate OpenId Connect resolver for the issuer. The
     * OpenId Connect JWT's "iss" field MUST be identical to the issuer param.
     *
     * @param issuer Reference to the issuer of the OpenID Connect JWT
     * @return an OpenIdResolverIF for the corresponding OpenIdResolver
     */
    public OpenIdResolver getResolverForIssuer(final String issuer);

    /**
     * Configures a new resolver implementation using the given parameters for this
     * service which is later retrievable.
     *
     * @param clientId the ID of the client for which this resolver will function
     * @param issuer issuer's name - the OpenID Connect "iss" field
     * @param keyAlias alias inside the keystore of the public key for this resolver
     * @return true if resolver configured successfully, false otherwise
     */
    public boolean configureResolverWithKey(final String clientId, final String issuer,
                                     final String keyAlias);

    /**
     * Configures a new resolver implementation using the given parameters for this
     * service which is later retrievable.
     *
     * @param clientId the ID of the client for which this resolver will function
     * @param issuer issuer's name - the OpenID Connect "iss" field
     * @param sharedSecret secret shared between client and provider
     * @return true if resolver configured successfully, false otherwise
     */
    public boolean configureResolverWithSecret(final String clientId, final String issuer,
                                            final String sharedSecret);

}
