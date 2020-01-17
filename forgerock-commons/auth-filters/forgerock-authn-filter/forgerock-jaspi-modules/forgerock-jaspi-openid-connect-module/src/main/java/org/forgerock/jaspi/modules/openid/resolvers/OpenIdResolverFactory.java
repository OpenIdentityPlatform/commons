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
package org.forgerock.jaspi.modules.openid.resolvers;

import java.net.URL;
import java.security.PublicKey;
import org.forgerock.jaspi.modules.openid.exceptions.FailedToLoadJWKException;

/**
 * For producing OpenId Resolvers.
 */
public class OpenIdResolverFactory {

    private WellKnownOpenIdConfigurationFactory openIdConfigurationFactory;

    /**
     * For tests.
     *
     * @param openIdConfigurationFactory used to produce JWKResolvers from supplied well-known open ID config URLs
     */
    OpenIdResolverFactory(final WellKnownOpenIdConfigurationFactory openIdConfigurationFactory) {
        this.openIdConfigurationFactory = openIdConfigurationFactory;
    }

    /**
     * For generating an OpenIDResolverFactory with the supplied timeouts which will
     * be used for all HTTP communication originating form this factory.
     *
     * @param readTimeout HTTP read timeout for produced resolvers
     * @param connTimeout HTTP connection timeout for produced resolvers
     */
    public OpenIdResolverFactory(final int readTimeout, final int connTimeout) {
        openIdConfigurationFactory = new WellKnownOpenIdConfigurationFactory(readTimeout, connTimeout);
    }

    /**
     * Creates a public key resolver for the supplied issuer.
     *
     * @param issuer The issuer's reference name
     * @param key Key to use for this issuer
     * @return a configured and usable PublicKeyOpenIdResolverImpl
     */
    public OpenIdResolver createPublicKeyResolver(String issuer, PublicKey key) {
        return new PublicKeyOpenIdResolverImpl(issuer, key);
    }

    /**
     * Creates a shared secret (HMAC) key resolver for the supplied issuer.
     *
     * @param issuer The issuer's reference name
     * @param sharedSecret SharedSecret for which to use with HMAC
     * @return a configured and usable SharedSecretOpenIdResolverImpl
     */
    public OpenIdResolver createSharedSecretResolver(String issuer, String sharedSecret) {
        return new SharedSecretOpenIdResolverImpl(issuer, sharedSecret);
    }

    /**
     * Creates a public key resolver for the supplied issuer using
     * keys supplied at the JWK Set URL.
     *
     * @param issuer The issuer's reference name
     * @param jwkUrl From which to read the JWK Set
     * @param readTimeout read timeout setting for HTTP connections
     * @param connTimeout connection timeout setting for HTTP connections
     * @return a configured and usable JWKOpenIdResolverImpl
     * @throws FailedToLoadJWKException If there were problems reading or configuring data from the URL
     */
    public OpenIdResolver createJWKResolver(String issuer, URL jwkUrl, int readTimeout, int connTimeout)
            throws FailedToLoadJWKException {
        return new JWKOpenIdResolverImpl(issuer, jwkUrl, readTimeout, connTimeout);
    }

    /**
     * Creates a public key resolver for the supplied issuer using keys supplied
     * at the .well-known open ID configuration URL.
     *
     * @param configUrl Location of the .well-known Open ID Connect config
     * @return a configured and usable JWKOpenIdResolverImpl
     * @throws FailedToLoadJWKException If there were problems reading or configuring data from the URL
     */
    public OpenIdResolver createFromOpenIDConfigUrl(URL configUrl) throws FailedToLoadJWKException {
        return openIdConfigurationFactory.build(configUrl);
    }
}
