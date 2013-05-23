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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.json.jwt;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonValue;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class to create an initial JWT or reconstruct a JWT object from a string.
 */
public class JwtBuilder {

    /**
     * Creates a new PlaintextJWT.
     *
     * @return A PlaintextJWT.
     */
    public PlaintextJwt jwt() {
        return new PlaintextJwt();
    }

    /**
     * Given a JWT as a String, this method will determine the type of JWT the String represents and creates the
     * appropriate JWT object for it.
     *
     * @param jwt The JWT String.
     * @return The reconstructed JWT object.
     */
    public Jwt recontructJwt(String jwt) {

        try {
            JwtString jwtString = new JwtString(jwt);

            JsonValue headerJson = new JsonValue(toJsonValue(jwtString.getHeader()));

            JwtType jwtType = JwtType.valueOf(headerJson.get("typ").asString());
            switch (jwtType) {
                case JWT: {

                    PlaintextJwt plaintextJwt = new PlaintextJwt(reconstructJwtHeaders(jwtString.getHeader()),
                            reconstructJwtContent(jwtString.getContent()));

                    String algorithm = plaintextJwt.getHeader("alg");
                    if (algorithm != null && !"none".equals(algorithm)) {
                        return new SignedJwt(plaintextJwt, jwtString.getThirdPart());
                    } else {
                        return plaintextJwt;
                    }
                }
    //        case JWS: {
    //
    //        }
    //        case JWE: {
    //
    //        }
                default: {
                    throw new JWTBuilderException("Unable to reconstruct JWT");
                }
            }
        } catch (JsonException e) {
            throw new JWTBuilderException("Unable to reconstruct JWT", e);
        }
    }

    /**
     * Reconstructs a JWT's headers from a String.
     *
     * @param header The JWT's header as a String.
     * @return A Map of the JWT's headers.
     * @throws JsonException If there is a problem parsing the headers String.
     */
    private Map<String, String> reconstructJwtHeaders(String header) throws JsonException {

        Map<String, String> jwtHeader = new HashMap<String, String>();

        JsonValue headerJson = new JsonValue(toJsonValue(header));

        for (String key : headerJson.keys()) {
            jwtHeader.put(key, headerJson.get(key).asString());
        }

        return jwtHeader;
    }

    /**
     * Reconstructs a JWT's payload content from a String.
     *
     * @param content The JWT's content as a String.
     * @return A Map of the JWT's content.
     * @throws JsonException If there is a problem parsing the content String.
     */
    private Map<String, Object> reconstructJwtContent(String content) throws JsonException {

        Map<String, Object> jwtContent = new LinkedHashMap<String, Object>();

        JsonValue contentJson = new JsonValue(toJsonValue(content));

        for (String key : contentJson.keys()) {
            Object value = null;
            JsonValue jsonValue = contentJson.get(key);
            if (jsonValue.isBoolean()) {
                value = jsonValue.asBoolean();
            } else if (jsonValue.isNumber()) {
                value = jsonValue.asNumber();
            } else {
                value = jsonValue.asString();
            }
            jwtContent.put(key, value);
        }

        return jwtContent;
    }

    /**
     * Converts a String into a JsonValue.
     *
     * @param json The json String.
     * @return A JsonValue object.
     * @throws JsonException If there is a problem parsing the json String.
     */
    private JsonValue toJsonValue(String json) throws JsonException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return new JsonValue(mapper.readValue(json, Map.class));
        } catch (IOException e) {
            throw new JsonException("Failed to parse json", e);
        }
    }
}
