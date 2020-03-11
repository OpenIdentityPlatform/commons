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
package org.forgerock.jaspi.modules.openid.helpers;

import java.security.Key;
import javax.crypto.spec.SecretKeySpec;
import org.forgerock.jaspi.modules.openid.exceptions.FailedToLoadJWKException;
import org.forgerock.json.JsonException;
import org.forgerock.json.jose.jwk.EcJWK;
import org.forgerock.json.jose.jwk.KeyType;
import org.forgerock.json.jose.jwk.OctJWK;
import org.forgerock.json.jose.jwk.RsaJWK;
import org.forgerock.json.jose.jws.JwsAlgorithm;

/**
 * Helper class to look up and return the keys from specific JWK implementation
 * algorithm types.
 */
public class JWKLookup {

    /**
     * Lookup returns the key from the given json, under the assumption it's of the correct
     * keyType.
     *
     * @param json JSON from which to attempt to generate a key
     * @param keyType The type of key we expect to be generated from the JSON
     * @return a valid key for verifying a JWT
     * @throws FailedToLoadJWKException If there's an issue handling the loading of the JWK
     */
    public Key lookup(String json, KeyType keyType) throws FailedToLoadJWKException {
        try {
            switch (keyType) {
            case RSA:
                final RsaJWK rsaJWK = RsaJWK.parse(json);
                return rsaJWK.toRSAPublicKey();
            case EC:
                final EcJWK ecJWK = EcJWK.parse(json);
                return ecJWK.toECPublicKey();
            case OCT:
                final OctJWK octJWK = OctJWK.parse(json);
                final String jwkKey = octJWK.getKey();

                final Key key = new SecretKeySpec(jwkKey.getBytes(),
                        JwsAlgorithm.getJwsAlgorithm(octJWK.getAlgorithm()).getMdAlgorithm());

                return key;
            default:
                throw new FailedToLoadJWKException("Unable to find handler for Key Type");
            }
        } catch (JsonException je) {
            throw new FailedToLoadJWKException("Unable to generate Key from provided JSON", je);
        }
    }

}
