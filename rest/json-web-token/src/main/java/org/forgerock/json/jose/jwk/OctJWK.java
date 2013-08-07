/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions copyright [year] [name of copyright owner]"
 */
package org.forgerock.json.jose.jwk;

import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.util.encode.Base64;

import java.util.List;

/**
 * Creates an Octet JWK.
 */
public class OctJWK extends JWK {
    /**
     * The Secret Key key value.
     */
    private final static String K = "k";

    /**
     * Constructs a OctJWK.
     * @param use the JWK use
     * @param alg the JWK algorithm
     * @param kid the JWK key id
     * @param key the symmetric key
     * @param x5u the x509 url for the key
     * @param x5t the x509 thumbnail for the key
     * @param x5c the x509 chain
     */
    public OctJWK(KeyUse use, String alg, String kid, String key, String x5u, String x5t, List<Base64> x5c) {
        super(KeyType.OCT, use, alg, kid, x5u, x5t, x5c);
        if (key == null || key.isEmpty()) {
            throw new JsonException("key is a required field for an OctJWK");
        }
        put(K, key);
    }

    /**
     * Gets the symmetric key.
     * @return the symmetric key that is Base64url encoded
     */
    public String getKey() {
        return get(K).asString();
    }

    /**
     * Parses a OctJWK object from a string json object.
     * @param json string json object
     * @return a OctJWK
     */
    public static OctJWK parse(String json) {
        JsonValue jwk = new JsonValue(toJsonValue(json));
        return parse(jwk);
    }

    /**
     * Parses a OctJWK object from a jsonValue object.
     * @param json an JsonValue object
     * @return a OctJWK
     */
    public static OctJWK parse(JsonValue json) {
        if (json == null) {
            throw new JsonException("Cant parse OctJWK. No json data.");
        }

        KeyType kty = null;
        KeyUse use = null;

        String k = null, alg = null, kid = null;
        String x5u = null, x5t = null;
        List x5c = null;

        k = json.get(K).asString();

        kty = KeyType.getKeyType(json.get(KTY).asString());
        if (!kty.equals(KeyType.OCT)) {
            throw new JsonException("Invalid key type. Not an Oct JWK");
        }

        use = KeyUse.getKeyUse(json.get(USE).asString());
        alg = json.get(ALG).asString();
        kid = json.get(KID).asString();

        x5u = json.get(X5U).asString();
        x5t = json.get(X5T).asString();
        x5c = json.get(X5C).asList();

        return new OctJWK(use, alg, kid, k, x5u, x5t, x5c);
    }

    /**
     * Prints the JWK as a json string.
     * @return json string
     */
    public String toJsonString() {
        return super.toString();
    }
}
