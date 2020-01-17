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

package org.forgerock.jaspi.modules.openid.helpers;

import java.io.IOException;
import java.net.URL;
import java.security.Key;
import java.util.HashMap;
import java.util.Map;
import org.forgerock.jaspi.modules.openid.exceptions.FailedToLoadJWKException;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwk.JWKSet;

/**
 * Provides methods to gather a JWKSet from a URL and return
 * a map of key ids to keys as dictated by that JWKS.
 */
public class JWKSetParser {

    private SimpleHTTPClient simpleHTTPClient;
    private JWKLookup jwkLookup;

    /**
     * Constructor allowing the configuration of the read and connection timeouts used
     * by the HTTP client for this parser.
     *
     * @param readTimeout read timeout in ms
     * @param connTimeout connection timeout in ms
     */
    public JWKSetParser(final int readTimeout, final int connTimeout) {
        this(new SimpleHTTPClient(readTimeout, connTimeout));
    }

    /**
     * Alternative constructor allowing the calling class to pass in an
     * already-configured {@link SimpleHTTPClient}.
     *
     * @param simpleHTTPClient {@link SimpleHTTPClient} used to gather HTTP information
     */
    public JWKSetParser(final SimpleHTTPClient simpleHTTPClient) {
        this(simpleHTTPClient, new JWKLookup());
    }

    /**
     * Alternative constructor allowing the calling class to pass in an
     * already-configured {@link SimpleHTTPClient}.
     *
     * @param simpleHTTPClient {@link SimpleHTTPClient} used to gather HTTP information
     * @param jwkLookup to convert the jwk into a real key
     */
    public JWKSetParser(final SimpleHTTPClient simpleHTTPClient, final JWKLookup jwkLookup) {
        this.simpleHTTPClient = simpleHTTPClient;
        this.jwkLookup = jwkLookup;
    }

    /**
     * Provides a Map of KeyId:Keys as indicated by the JWKSet's URL.
     *
     * @param url The URL from which to gather the JWKSet
     * @return a map of currently valid KeyId:Keys for the provider associated with this URL
     * @throws FailedToLoadJWKException If there are problems connecting to or parsing the response
     */
    public Map<String, Key> generateMapFromJWK(URL url) throws FailedToLoadJWKException {
        //gather
        final String jwksContents = gatherHttpContents(url);

        //unmarshall
        final JWKSet jwkSet = JWKSet.parse(jwksContents);

        //return map
        return jwkSetToMap(jwkSet);
    }

    /**
     * Uses the SimpleHTTPClient to gather HTTP information.
     *
     * @param url The URL from which to read the information
     * @return a String containing the returned JSON
     * @throws FailedToLoadJWKException If there are problems connecting to the URL
     */
    private String gatherHttpContents(URL url) throws FailedToLoadJWKException {
        final String jwksContents;

        try {
            jwksContents = simpleHTTPClient.get(url);
        } catch (IOException e) {
            throw new FailedToLoadJWKException("Unable to load the JWK location over HTTP", e);
        }

        return jwksContents;
    }

    /**
     * Converts a supplied JWKSet into a map of key:values, where the keys are the keyIds and the
     * values are verification keys.
     *
     * @param jwkSet The JWKSet to convert
     * @return A map of key ids to their respective keys
     * @throws FailedToLoadJWKException If there are issues parsing the JWKSet's contents
     */
    public Map<String, Key> jwkSetToMap(JWKSet jwkSet) throws FailedToLoadJWKException {

        final Map<String, Key> keyMap = new HashMap<>();

        //store the retrieved JSON as String (kid) : Key (having converted) in this resolver
        for (JWK jwk : jwkSet.getJWKsAsList()) {
            final Key key = jwkLookup.lookup(jwk.toJsonString(), jwk.getKeyType());
            keyMap.put(jwk.getKeyId(), key);
        }

        return keyMap;

    }

}
