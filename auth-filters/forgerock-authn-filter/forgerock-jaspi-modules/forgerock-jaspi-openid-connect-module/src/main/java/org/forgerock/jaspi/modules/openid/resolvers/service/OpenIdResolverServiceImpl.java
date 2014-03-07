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

import java.security.PublicKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.forgerock.auth.common.DebugLogger;
import org.forgerock.jaspi.logging.LogFactory;
import org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver;
import org.forgerock.jaspi.modules.openid.resolvers.PublicKeyOpenIdResolverImpl;
import org.forgerock.jaspi.modules.openid.resolvers.SharedSecretOpenIdResolverImpl;
import org.forgerock.json.jose.utils.KeystoreManager;
import org.forgerock.json.jose.utils.KeystoreManagerException;

/**
 * Holds a copy of the current OpenID Resolvers.
 *
 * Held in a ConcurrentHashMap for multi-threaded access, this class holds a KeystoreManager
 * instance which is instantiated on construction from the passed in settings.
 *
 * As new resolvers are configured, this class loads up the appropriate public key and
 * stores it along with the other information necessary for it to perform its task.
 *
 * This service stores {@link org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver}s against their issuer key,
 * so the appropriate {@link org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver} can be looked up.
 */
public class OpenIdResolverServiceImpl implements OpenIdResolverService {

    private static final DebugLogger DEBUG = LogFactory.getDebug();

    private final Map<String, OpenIdResolver> openIdResolvers = new ConcurrentHashMap<String, OpenIdResolver>();

    private final KeystoreManager keystoreManager;

    /**
     * Constructor for the OpenIdResolverServiceImpl
     *
     * @param keystoreType The type of Java KeyStore.
     * @param keystoreLocation The file path to the KeyStore.
     * @param keystorePassword The password for the KeyStore.
     */
    public OpenIdResolverServiceImpl(final String keystoreType, final String keystoreLocation,
                                     final String keystorePassword) {

        keystoreManager = new KeystoreManager(keystoreType, keystoreLocation, keystorePassword);
    }

    /**
     * Returns the appropriate resolver for the given issuer - if it exists. Otherwise null.
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
     * @param clientId The clientId for which this resolver is valid
     * @param issuer The issuer which provides the Open ID Connect auth token
     * @param keyAlias The alias under which the public key is stored
     * @return true if the resolver was configured successfully, false otherwise
     */
    public boolean configureResolverWithKey(final String clientId, final String issuer, final String keyAlias) {

        try {
            final PublicKey key = lookupKey(keyAlias);

            final OpenIdResolver impl = new PublicKeyOpenIdResolverImpl(issuer, clientId, key);
            openIdResolvers.put(issuer, impl);
        } catch (KeystoreManagerException kme) {
            DEBUG.debug("Error accessing the KeystoreManager", kme);
            return false;
        } catch (NullPointerException npe) {
            DEBUG.debug("No key found in keystore with appropriate alias", npe);
            return false;
        }

        return true;
    }

    /**
     * Configures a new Resolver by finding the appropriate public key in the supplied keystore,
     * and adds it to the Map of current resolvers.
     *
     * @param clientId The clientId for which this resolver is valid
     * @param issuer The issuer which provides the Open ID Connect auth token
     * @param sharedSecret The known-to-bogth-parties secret String
     * @return true if the resolver was configured successfully, false otherwise
     */
    public boolean configureResolverWithSecret(final String clientId, final String issuer, final String sharedSecret) {

        try {
            final OpenIdResolver impl = new SharedSecretOpenIdResolverImpl(issuer, clientId, sharedSecret);
            openIdResolvers.put(issuer, impl);
        } catch (IllegalArgumentException iae) {
            DEBUG.debug("Shared secret must not be null", iae);
            return false;
        }

        return true;
    }

    /**
     * Retrieves a public key from the keystoreManager, based on the supplied alias
     *
     * @param keyAlias The name of the key to look up from the keystore
     * @return The public key associated with the supplied keyAlias from the keystore
     */
    private PublicKey lookupKey(final String keyAlias) {
        return keystoreManager.getPublicKey(keyAlias);
    }

}
