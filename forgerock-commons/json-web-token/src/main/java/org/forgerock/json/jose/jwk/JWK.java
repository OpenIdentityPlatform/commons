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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.json.jose.jwk;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.forgerock.json.JsonException;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jwt.JWObject;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The abstract base class for the 3 implementations of JWK.
 */
public abstract class JWK extends JWObject {
    /**
     * The KeyType key.
     */
    protected static final String KTY = "kty";

    /**
     * The KeyUse key.
     */
    protected static final String USE = "use";

    /**
     * The Algorithm key.
     */
    protected static final String ALG = "alg";

    /**
     * The KeyID key.
     */
    protected static final String KID = "kid";

    /**
     * The X509 URL key.
     */
    protected static final String X5U = "x5u";

    /**
     * The X509 thumbnail key.
     */
    protected static final String X5T = "x5t";

    /**
     * The X509 chain key.
     */
    protected static final String X5C = "x5c";

    /**
     * Creates a JWK given the basic parameters.
     * @param kty the JWK key type
     * @param use the JWK use
     * @param alg the JWK algorithm
     * @param kid the JWK key id
     */
    protected JWK(KeyType kty, KeyUse use, String alg, String kid) {
        this(kty, use, alg, kid, null, null, null);
    }

    /**
     * Creates a JWK given the basic parameters.
     * @param kty the JWK key type
     * @param use the JWK use
     * @param alg the JWK algorithm
     * @param kid the JWK key id
     * @param x5u the x509 url for the key
     * @param x5t the x509 thumbnail for the key
     * @param x5c the x509 chain as a list of Base64 encoded strings
     */
    protected JWK(KeyType kty, KeyUse use, String alg, String kid, String x5u, String x5t, List<String> x5c) {
        super();
        if (kty == null) {
            new JsonException("kty is a required field");
        }
        put(KTY, kty.toString());
        if (kid == null || kid.isEmpty()) {
            new JsonException("kid is a required field");
        }
        put(KID, kid);
        if (use != null) {
            put(USE, use.toString());
        }
        if (alg != null && !alg.isEmpty()) {
            put(ALG, alg);
        }
        if (x5c != null && !x5c.isEmpty()) {
            put(X5C, x5c);
        }
        if (x5t != null && !x5t.isEmpty()) {
            put(X5T, x5t);
        }
        if (x5u != null && !x5u.isEmpty()) {
            put(X5U, x5u);
        }
    }

    /**
     * Gets the kty parameter of the JWK.
     * @return A KeyType for the JWK
     */
    public KeyType getKeyType() {
        return KeyType.getKeyType(get(KTY).asString());
    }

    /**
     * Gets the use parameter of the JWK.
     * @return A String representing the use parameter
     */
    public KeyUse getUse() {
        return KeyUse.getKeyUse(get(USE).asString());
    }

    /**
     * Gets the alg parameter of the JWK.
     * @return A String representing the alg parameter
     */
    public String getAlgorithm() {
        return get(ALG).asString();
    }

    /**
     * Gets the kid parameter of the JWK.
     * @return A String representing the kid parameter
     */
    public String getKeyId() {
        return get(KID).asString();
    }

    /**
     * Prints the JWK Object as a json string.
     * @return A String representing JWK
     */
    public String toJsonString() {
        return toString();
    }

    /**
     * Parses a String into the proper JWK type.
     *
     * @param json The json String.
     * @return A JWK object
     * @throws org.forgerock.json.JsonException If there is a problem parsing the json String.
     */
    public static JWK parse(String json) {
        JsonValue jwk = new JsonValue(toJsonValue(json));
        return parse(jwk);
    }

    /**
     * Parses a JsonValue into the proper JWK type.
     *
     * @param jwk The JsonValue Object.
     * @return A JWK object
     * @throws org.forgerock.json.JsonException If there is a problem parsing the json String.
     */
    public static JWK parse(JsonValue jwk) {
        KeyType kty = KeyType.getKeyType(jwk.get(KTY).asString());

        if (kty.equals(KeyType.RSA)) {
            return RsaJWK.parse(jwk);
        } else if (kty.equals(KeyType.OCT)) {
            return OctJWK.parse(jwk);
        } else if (kty.equals(KeyType.EC)) {
            return EcJWK.parse(jwk);
        } else {
            throw new JsonException("Failed to parse json invalid kty parameter");
        }
    }

    /**
     * Converts a String into a JsonValue.
     *
     * @param json The json String.
     * @return A JsonValue object.
     * @throws org.forgerock.json.JsonException If there is a problem parsing the json String.
     */
    protected static JsonValue toJsonValue(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return new JsonValue(mapper.readValue(json, Map.class));
        } catch (IOException e) {
            throw new JsonException("Failed to parse json", e);
        }
    }

    /**
     * Gets the X509 URL.
     * @return the url of the 509 cert header or null
     */
    public String getX509URL() {
        return get(X5U).asString();
    }

    /**
     * Gets the X509 thumbnail.
     * @return Base64url of the X509 thumbnail
     */
    public String getX509Thumbnail() {
        return get(X5T).asString();
    }

    /**
     * Gets a List of X509 chain certs.
     * @return X509 Cert Chain as list of encoded strings or null if none are available.
     */
    public List<String> getX509Chain() {
        return get(X5C).asList(String.class);
    }
}
