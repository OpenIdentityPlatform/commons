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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.json.jose.jwk;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.forgerock.json.JsonException;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jwt.JWObject;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Holds a Set of JWKs.
 */
public class JWKSet extends JWObject {

    /**
     * Constructs an empty JWKSet.
     */
    public JWKSet() {

    }

    /**
     * Construct a JWKSet from a single JWK.
     * @param jwk the jwk to construct the set from
     */
    public JWKSet(JWK jwk) {
        if (jwk == null) {
            throw new JsonException("JWK must not be null");
        }
        put("keys", jwk);
    }

    /**
     * Construct a JWKSet from a single JWK.
     * @param jwk contains a list of json web keys
     */
    public JWKSet(JsonValue jwk) {
        if (jwk == null) {
            throw new JsonException("JWK must not be null");
        }
        put("keys", jwk);
    }

    /**
     * Construct a JWKSet from a List of JWKs.
     * @param jwkList a list of jwks
     */
    public JWKSet(List<JWK> jwkList) {
        if (jwkList == null) {
            throw new JsonException("The list cant be null");
        }
        put("keys", jwkList);
    }

    /**
     * Get the JWKs in the set.
     * @return a list of JWKs
     */
    public List<JWK> getJWKsAsList() {
        List<JWK> listOfJWKs = new LinkedList<>();
        JsonValue jwks = get("keys");
        Iterator<JsonValue> i = jwks.iterator();
        while (i.hasNext()) {
            listOfJWKs.add(JWK.parse(i.next()));
        }
        return listOfJWKs;
    }

    /**
     * Get the JWKs in the set.
     * @return a list of JWKs as JsonValues
     */
    public JsonValue getJWKsAsJsonValue() {
        return get("keys");
    }

    /**
     * Converts a json string to a jsonValue.
     * @param json a json jwk set object string
     * @return a json value of the son string
     * @throws JsonException if unable to parse
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
     * Parses a JWKSet object from a string json object.
     * @param json string json object
     * @return a JWKSet
     */
    public static JWKSet parse(String json) {
        JsonValue jwkSet = new JsonValue(toJsonValue(json));
        return parse(jwkSet);
    }

    /**
     * Parses a JWKSet object from a jsonValue object.
     * @param json an JsonValue object
     * @return a JWKSet
     */
    public static JWKSet parse(JsonValue json) {
        if (json == null) {
            throw new JsonException("Cant parse JWKSet. No json data.");
        }
        return new JWKSet(json.get("keys"));
    }

    /**
     * Prints the JWK Set as a json string.
     * @return A String representing JWK
     */
    public String toJsonString() {
        return super.toString();
    }

}
