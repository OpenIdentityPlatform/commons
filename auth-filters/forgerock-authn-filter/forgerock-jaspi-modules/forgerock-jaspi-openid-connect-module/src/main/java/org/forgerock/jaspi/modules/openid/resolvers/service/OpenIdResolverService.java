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

import java.net.URL;
import org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver;

/**
 * Interface through which OpenIdResolvers are obtained, and the service providing
 * them is configured.
 *
 * A resolver can be configured through a number of configurations, each of which results
 * in the generation of a key which can be used to perform cryptographic verification
 * of the JWS which will be provided to the resolver once it is configured inside the service.
 *
 * The service will then provide access to the specific resolver needed at the point of
 * verification by keying on its
 * {@link org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver#getIssuer()} value.
 *
 * Resolvers can be configured by supplying one of the following configurations:
 *
 * - The issuer's name, along with the specific location of the public key to use
 * when performing verification as drawn from a standard trust store.
 * - The issuer's name, along with a shared secret which can be used to create an HMAC
 * which will verify the signature in the provided JWS.
 * - The issuer's name, along with the URL of a JWK set, which provides keys through
 * a public exposure point.
 * - A .well-known configuration URL, which provides both the issuer name and location
 * of the corresponding JWK set which it should use to configure the resolver.
 */
public interface OpenIdResolverService {

    /**
     * Returns the appropriate OpenId Connect resolver for the issuer. The
     * OpenId Connect JWT's "iss" field MUST be identical to the issuer param.
     *
     * @param issuer Reference to the issuer of the OpenID Connect JWT
     * @return an OpenIdResolver for the corresponding provider
     */
    public OpenIdResolver getResolverForIssuer(final String issuer);

    /**
     * Configures a new resolver implementation using the given parameters for this
     * service which is later retrievable.
     *
     * @param issuer issuer's name - the OpenID Connect "iss" field
     * @param keyAlias alias inside the keystore of the public key for this resolver
     * @param keystoreLocation location of the keystore from which to retrieve the key
     * @param keystoreType the type of keystore to connect to
     * @param keystorePassword password for connecting to the keystore
     * @return true if resolver configured successfully, false otherwise
     */
    public boolean configureResolverWithKey(final String issuer,
                                     final String keyAlias, final String keystoreLocation,
                                     final String keystoreType, final String keystorePassword);

    /**
     * Configures a new resolver implementation using the given parameters for this
     * service which is later retrievable.
     *
     * @param issuer issuer's name - the OpenID Connect "iss" field
     * @param sharedSecret secret shared between client and provider
     * @return true if resolver configured successfully, false otherwise
     */
    public boolean configureResolverWithSecret(final String issuer,
                                            final String sharedSecret);


    /**
     * Configures a new resolver implementation using the given parameters for this
     * service which is later retrievable.
     *
     * @param issuer issuer's name - the OpenID Connect "iss" field
     * @param jwkUrl location from which to determine which public key to use
     * @return true if resolver configured successfully, false otherwise
     */
    public boolean configureResolverWithJWK(final String issuer, final URL jwkUrl);

    /**
     * Configures a new resolver implementation using the given configUrl as the
     * location from which to draw all necessary information pertaining to the resolver.
     * Specifically and minimally this means the issuer value and the location of the
     * JWK url
     *
     * @param configUrl The well-known Open Id Connect configuration url
     * @return true if resolver configured successfully, false otherwise
     */
    public boolean configureResolverWithWellKnownOpenIdConfiguration(final URL configUrl);

}
