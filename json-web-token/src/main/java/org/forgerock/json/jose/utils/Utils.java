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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.json.jose.utils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.forgerock.json.jose.exceptions.InvalidJwtException;
import org.forgerock.util.encode.Base64url;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * This class provides utility methods to share common behaviour.
 *
 * @since 2.0.0
 */
public final class Utils {

    /** Cached JSON object mapper for parsing tokens. */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.STRICT_DUPLICATE_DETECTION, true)
            .configure(SerializationFeature.INDENT_OUTPUT, false);

    /** UTF-8 Charset. */
    public static final Charset CHARSET = StandardCharsets.UTF_8;

    /** Private constructor. */
    private Utils() {
        // Utility class
    }

    /**
     * Base64url encodes the given String, converting the String to UTF-8 bytes.
     *
     * @param s The String to encoded.
     * @return A Base64url encoded UTF-8 String.
     */
    public static String base64urlEncode(String s) {
        return Base64url.encode(s.getBytes(CHARSET));
    }

    /**
     * Base64url decodes the given String and converts the decoded bytes into a UTF-8 String.
     *
     * @param s The Base64url encoded String to decode.
     * @return The UTF-8 decoded String.
     */
    public static String base64urlDecode(String s) {
        return new String(Base64url.decode(s), CHARSET);
    }

    /**
     * Compares two byte arrays for equality, in a constant time.
     * <p>
     * If the two byte arrays don't match the method will not return until the whole byte array has been checked.
     * This prevents timing attacks.
     * Unless the two arrays are not off equal length, and in this case the method will return immediately.
     *
     * @param a One of the byte arrays to compare.
     * @param b The other byte array to compare.
     *
     * @return <code>true</code> if the arrays are equal, <code>false</code> otherwise.
     */
    public static boolean constantEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }

        boolean result = true;
        for (int i = 0; i < a.length; i++) {
            result &= a[i] == b[i];
        }
        return result;
    }

    /**
     * Parses the given JSON string into a NoDuplicatesMap.
     * <p>
     * The JWT specification details that any JWT with duplicate header parameters or claims MUST be rejected so
     * a Map implementation is used to parse the JSON which will throw an exception if an entry with the same key
     * is added to the map more than once.
     *
     * @param json The JSON string to parse.
     * @return A Map of the JSON properties.
     * @throws InvalidJwtException if the json value is not well formed or contains duplicate keys.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseJson(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, LinkedHashMap.class);
        } catch (IOException e) {
            throw new InvalidJwtException("Failed to parse json: " + e.getMessage(), e);
        }
    }

    /**
     * Writes the given map as a string in JSON object format.
     *
     * @param object the object to write as JSON.
     * @return the JSON serialisation of the given object.
     * @throws InvalidJwtException if the object cannot be converted to JSON for any reason.
     */
    public static String writeJsonObject(Map<String, Object> object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new InvalidJwtException("Failed to write json: " + e, e);
        }
    }
}
