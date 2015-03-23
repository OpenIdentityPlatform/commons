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

package org.forgerock.jaspi.modules.openid.resolvers.service;

import static org.forgerock.caf.authentication.framework.JaspiRuntime.LOG;

import java.net.URL;
import java.security.PublicKey;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.forgerock.jaspi.modules.openid.exceptions.FailedToLoadJWKException;
import org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver;
import org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolverFactory;
import org.forgerock.json.jose.utils.KeystoreManager;
import org.forgerock.json.jose.utils.KeystoreManagerException;

/**
 * Holds a copy of the current OpenID Resolvers.
 *
 * As new resolvers are configured, this class loads up the appropriate verification key and
 * stores it along with the other information necessary for it to perform its task.
 *
 * This service stores {@link org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver}s against their issuer key,
 * so the appropriate {@link org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver} can be looked up.
 */
public class OpenIdResolverServiceImpl implements OpenIdResolverService {

    private final ConcurrentMap<String, OpenIdResolver> openIdResolvers =
            new ConcurrentHashMap<String, OpenIdResolver>();

    private final int readTimeout;
    private final int connTimeout;

    private final OpenIdResolverFactory openIdResolverFactory;

    /**
     * Constructor for the OpenIdResolverServiceImpl which will use the supplied
     * read and connection timeouts when communicating over HTTP.
     *
     * @param readTimeout HTTP read timeout for resolvers
     * @param connTimeout HTTP connection timeout for resolvers
     */
    public OpenIdResolverServiceImpl(final int readTimeout, final int connTimeout) {
        this.readTimeout = readTimeout;
        this.connTimeout = connTimeout;
        this.openIdResolverFactory = new OpenIdResolverFactory(readTimeout, connTimeout);
    }

    /**
     * For tests.
     *
     * @param openIdResolverFactory Factory to provide resolvers
     * @param readTimeout HTTP read timeout for resolvers
     * @param connTimeout HTTP connection timeout for resolvers
     */
    OpenIdResolverServiceImpl(OpenIdResolverFactory openIdResolverFactory, final int readTimeout,
                              final int connTimeout) {
        this.readTimeout = readTimeout;
        this.connTimeout = connTimeout;
        this.openIdResolverFactory = openIdResolverFactory;
    }

    /**
     * Returns the appropriate resolver for the given issuer - if it exists. Otherwise null.
     *
     * @param issuer The name of the issuer of the Open Id Connect token to check
     * @return A resolver which can handle verification of the Open Id Connect token
     */
    public OpenIdResolver getResolverForIssuer(final String issuer) {
        return openIdResolvers.get(issuer);
    }

    /**
     * Configures a new Resolver by finding the appropriate public key in the supplied keystore,
     * and adds it to the Map of current resolvers.
     *
     * @param issuer The issuer which provides the Open ID Connect auth token
     * @param keyAlias The alias under which the public key is stored
     * @param keystoreLocation location of the keystore file
     * @param keystoreType type of the keystore file
     * @param keystorePassword password to enter the keystore
     * @return true if the resolver was configured successfully, false otherwise
     */
    public boolean configureResolverWithKey(final String issuer,
                                            final String keyAlias, final String keystoreLocation,
                                            final String keystoreType, final String keystorePassword) {

        try {
            // Do not need the private key password as we are only ever getting the public key
            final KeystoreManager keystoreManager = new KeystoreManager(null, keystoreType, keystoreLocation,
                    keystorePassword);
            final PublicKey key = keystoreManager.getPublicKey(keyAlias);

            final OpenIdResolver impl = openIdResolverFactory.createPublicKeyResolver(issuer, key);
            openIdResolvers.put(issuer, impl);
        } catch (KeystoreManagerException kme) {
            LOG.debug("Error accessing the KeystoreManager", kme);
            return false;
        } catch (NullPointerException npe) {
            LOG.debug("No key found in keystore with appropriate alias", npe);
            return false;
        }

        return true;
    }

    /**
     * Configures a new Resolver by finding the appropriate public key in the supplied keystore,
     * and adds it to the Map of current resolvers.
     *
     * @param issuer The issuer which provides the Open ID Connect auth token
     * @param sharedSecret The known-to-both-parties secret String
     * @return true if the resolver was configured successfully, false otherwise
     */
    public boolean configureResolverWithSecret(final String issuer, final String sharedSecret) {

        try {
            final OpenIdResolver impl = openIdResolverFactory.createSharedSecretResolver(issuer, sharedSecret);
            openIdResolvers.put(issuer, impl);
        } catch (IllegalArgumentException iae) {
            LOG.debug("Shared secret must not be null", iae);
            return false;
        }

        return true;
    }

    /**
     * Configures a new Resolver by setting it up to download public keys from the supplied url.
     *
     * @param issuer The issuer which provides the Open ID Connect auth token
     * @param jwkUrl location from which to determine which public key to use
     * @return true if the resolver was configured successfully, false otherwise
     */
    public boolean configureResolverWithJWK(final String issuer,
                                            final URL jwkUrl) {

        try {
            final OpenIdResolver impl = openIdResolverFactory.createJWKResolver(issuer, jwkUrl,
                    readTimeout, connTimeout);
            openIdResolvers.put(issuer, impl);
        } catch (FailedToLoadJWKException e) {
            LOG.debug("Unable to load JSON Web Keys", e);
            return false;
        }

        return true;
    }

    /**
     * Configures a new Resolver by setting it up to download public keys from the supplied
     * well-known Open Id Connect URL.
     *
     * @param configUrl location from which to determine which public key to use
     * @return true if the resolver was configured successfully, false otherwise
     */
    public boolean configureResolverWithWellKnownOpenIdConfiguration(final URL configUrl) {

        try {
            final OpenIdResolver impl = openIdResolverFactory.createFromOpenIDConfigUrl(configUrl);
            openIdResolvers.put(impl.getIssuer(), impl);
        } catch (FailedToLoadJWKException e) {
            LOG.debug("Unable to load JSON Web Keys", e);
            return false;
        }

        return true;
    }

}
