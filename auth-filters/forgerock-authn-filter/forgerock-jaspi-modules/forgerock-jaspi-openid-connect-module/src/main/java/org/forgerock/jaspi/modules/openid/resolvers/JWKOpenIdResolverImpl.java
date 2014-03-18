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
import java.security.Key;
import java.util.HashMap;
import java.util.Map;
import org.forgerock.auth.common.DebugLogger;
import org.forgerock.jaspi.logging.LogFactory;
import org.forgerock.jaspi.modules.openid.exceptions.FailedToLoadJWKException;
import org.forgerock.jaspi.modules.openid.exceptions.InvalidSignatureException;
import org.forgerock.jaspi.modules.openid.exceptions.OpenIdConnectVerificationException;
import org.forgerock.jaspi.modules.openid.helpers.JWKSetParser;
import org.forgerock.jaspi.modules.openid.helpers.SimpleHTTPClient;
import org.forgerock.json.jose.jws.SignedJwt;

/**
 * This class exists to allow Open Id Providers to supply or promote a JWK exposure point for
 * their public keys. We convert the exposed keys they provide according to the algorithm
 * defined by their JWK and offer their keys in a map key'd on their keyId.
 *
 * The map of keys is loaded on construction, and reloaded each time an Open Id token is
 * passed in to this resolver whose keyId does not exist within the list that we currently have.
 *
 * This means that we will cache the keys for as long as they are valid, and as soon as we
 * receive a request to verify using a key which we don't have we discard our current keys and
 * re-fill our map.
 */
public class JWKOpenIdResolverImpl extends BaseOpenIdResolver {

    private final URL jwkUrl;

    private final Map<String, Key> keyMap = new HashMap<String, Key>();

    private final JWKSetParser jwkParser;

    private static final DebugLogger DEBUG = LogFactory.getDebug();

    /**
     * Constructor using provided timeout values to generate the
     * {@link SimpleHTTPClient} used for communicating over HTTP.
     *
     * @param issuer The issuer (provider) of the Open Id Connect id token
     * @param jwkUrl the URL from which we will attempt to read and parse our JWKSet
     * @param readTimeout the read timeout associated with HTTP requests
     * @param connTimeout the connection timeout associated with HTTP requests
     * @throws FailedToLoadJWKException if there were issues resolving or parsing the JWK
     */
    public JWKOpenIdResolverImpl(final String issuer, final URL jwkUrl, final int readTimeout,
                                 final int connTimeout) throws FailedToLoadJWKException {
        super(issuer);

        jwkParser = new JWKSetParser(readTimeout, connTimeout);
        this.jwkUrl = jwkUrl;

        try {
            reloadKeys();
        } catch (FailedToLoadJWKException e) {
            DEBUG.debug("Unable to load keys from the JWK over HTTP");
            throw new FailedToLoadJWKException("Unable to load keys from the JWK over HTTP", e);
        }
    }

    /**
     * Constructor using an already-created {@link SimpleHTTPClient}.
     *
     * @param issuer The issuer (provider) of the Open Id Connect id token
     * @param jwkUrl The URL from which we will attempt to read and parse our JWKSet
     * @param httpClient The http client through which we will attempt to read the jwkUrl
     * @throws FailedToLoadJWKException if there were issues resolving or parsing the JWK.
     */
    public JWKOpenIdResolverImpl(final String issuer, final URL jwkUrl, final SimpleHTTPClient httpClient)
            throws FailedToLoadJWKException {
        super(issuer);

        jwkParser = new JWKSetParser(httpClient);
        this.jwkUrl = jwkUrl;

        try {
            reloadKeys();
        } catch (FailedToLoadJWKException e) {
            DEBUG.debug("Unable to load keys from the JWK over HTTP");
            throw new FailedToLoadJWKException("Unable to load keys from the JWK over HTTP", e);
        }
    }


    /**
     * Test constructor using an already-created JwkParser.
     *
     * @param issuer The issuer (provider) of the Open Id Connect id token
     * @param jwkUrl The URL from which we will attempt to read and parse our JWKSet
     */
    JWKOpenIdResolverImpl(final String issuer, final URL jwkUrl, final JWKSetParser jwkParser)
            throws FailedToLoadJWKException {
        super(issuer);

        this.jwkParser = jwkParser;
        this.jwkUrl = jwkUrl;

        try {
            reloadKeys();
        } catch (FailedToLoadJWKException e) {
            DEBUG.debug("Unable to load keys from the JWK over HTTP");
            throw new FailedToLoadJWKException("Unable to load keys from the JWK over HTTP", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateIdentity(final SignedJwt idClaim) throws OpenIdConnectVerificationException {
        super.validateIdentity(idClaim);
        verifySignature(idClaim);
    }

    /**
     * Verifies that the JWS was signed by the supplied key. Throws an exception otherwise.
     *
     * @param idClaim The JWS to verify
     * @throws InvalidSignatureException If the JWS supplied does not match the key for this resolver
     * @throws FailedToLoadJWKException If the JWK supplied cannot be loaded from its remote location
     */
    public void verifySignature(final SignedJwt idClaim) throws InvalidSignatureException,
            FailedToLoadJWKException {

        final Key key;

        synchronized (keyMap) {
            if (!keyMap.containsKey(idClaim.getHeader().getKeyId())) {
                reloadKeys();
            }
        }

        key = keyMap.get(idClaim.getHeader().getKeyId());

        if (key == null || !idClaim.verify(key)) {
            DEBUG.debug("JWS unable to be verified");
            throw new InvalidSignatureException("JWS unable to be verified");
        }
    }

    /**
     * Communicates with the configured server, attempting to download the latest keyset
     * for use.
     *
     * @throws FailedToLoadJWKException if there were issues parsing the supplied URL
     */
    private void reloadKeys() throws FailedToLoadJWKException {
        synchronized (keyMap) {
            keyMap.clear();
            keyMap.putAll(jwkParser.generateMapFromJWK(jwkUrl));
        }
    }

}
