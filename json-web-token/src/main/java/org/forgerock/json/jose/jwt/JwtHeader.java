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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.json.jose.jwt;

import static org.forgerock.json.jose.jwt.JwtHeaderKey.ALG;
import static org.forgerock.json.jose.jwt.JwtHeaderKey.TYP;
import static org.forgerock.json.jose.jwt.JwtHeaderKey.getHeaderKey;

import java.util.Map;

/**
 * A base implementation class for JWT Headers.
 * <p>
 * Provides methods to set header parameters for all types of JWT Headers.
 *
 * @see org.forgerock.json.jose.jws.JwsHeader
 * @see org.forgerock.json.jose.jwe.JweHeader
 *
 * @since 2.0.0
 */
public abstract class JwtHeader extends JWObject {

    /**
     * Constructs a new JwtHeader, with the "typ" parameter set to "JWT".
     */
    public JwtHeader() {
        put(TYP.value(), JwtType.JWT.toString());
    }

    /**
     * Constructs a new JwtHeader, with its parameters set to the contents of the given Map.
     *
     * @param headers A Map containing the parameters to be set in the header.
     */
    public JwtHeader(Map<String, Object> headers) {
        this();
        setParameters(headers);
    }

    /**
     * Sets the type of JWT this header represents.
     * <p>
     * For non-nested JWTs then the "JWT" type is RECOMMENDED to be used but it is OPTIONAL to set the "typ" property.
     * For nested signed or encrypted JWTs the JWT type MUST be "JWS" and "JWE" respectively and the "typ" property
     * MUST be set.
     *
     * @see JwtType
     *
     * @param jwtType The JwtType.
     */
    public void setType(JwtType jwtType) {
        put(TYP.value(), jwtType.toString());
    }

    /**
     * Gets the type of JWT this header represents.
     *
     * @return The JwtType.
     */
    public JwtType getType() {
        return JwtType.valueOf(get(TYP.value()).asString().toUpperCase());
    }

    /**
     * Sets the algorithm used to perform cryptographic signing and/or encryption on the JWT.
     *
     * @param algorithm The Algorithm.
     */
    public void setAlgorithm(Algorithm algorithm) {
        put(ALG.value(), algorithm.toString());
    }

    /**
     * Gets the Algorithm set in the JWT header.
     *
     * @return The Algorithm.
     */
    public abstract Algorithm getAlgorithm();

    /**
     * Gets the string representation of the Algorithm set in the JWT header.
     *
     * @return The algorithm as a String.
     */
    protected String getAlgorithmString() {
        return get(ALG.value()).asString();
    }

    /**
     * Sets a header parameter with the specified key and value.
     * <p>
     * If the key matches one of the reserved header parameter names, then the relevant <tt>set</tt> method is
     * called to set that header parameter with the specified value.
     *
     * @param key The key of the header parameter.
     * @param value The value of the header parameter.
     */
    public void setParameter(String key, Object value) {
        JwtHeaderKey headerKey = getHeaderKey(key.toUpperCase());

        switch (headerKey) {
        case TYP: {
            if (isValueOfType(value, JwtType.class)) {
                setType((JwtType) value);
            } else {
                checkValueIsOfType(value, String.class);
                setType(JwtType.jwtType((String) value));
            }
            break;
        }
        case ALG: {
            if (isValueOfType(value, Algorithm.class)) {
                setAlgorithm((Algorithm) value);
            } else {
                checkValueIsOfType(value, String.class);
                put(ALG.value(), value);
            }
            break;
        }
        default: {
            put(key, value);
        }
        }
    }

    /**
     * Sets header parameters using the values contained in the specified map.
     *
     * @param headers The Map to use to set header parameters.
     * @see #setParameter(String, Object)
     */
    public void setParameters(Map<String, Object> headers) {
        for (String key : headers.keySet()) {
            setParameter(key, headers.get(key));
        }
    }

    /**
     * Gets a header parameter for the specified key.
     * <p>
     * If the key matches one of the reserved header parameter names, then the relevant <tt>get</tt> method is
     * called to get that header parameter.
     *
     * @param key The header parameter key.
     * @return The value stored against the header parameter key.
     */
    public Object getParameter(String key) {
        JwtHeaderKey headerKey = getHeaderKey(key.toUpperCase());

        Object value;

        switch (headerKey) {
        case TYP: {
            value = getType();
            break;
        }
        case ALG: {
            value = getAlgorithm();
            break;
        }
        default: {
            value = get(key).getObject();
        }
        }

        return value;
    }

    /**
     * Gets a header parameter for the specified key and then casts it to the specified type.
     *
     * @param key The header parameter key.
     * @param clazz The class of the required type.
     * @param <T> The required type for the header parameter value.
     * @return The value stored against the header parameter key.
     * @see #getParameter(String)
     */
    public <T> T getParameter(String key, Class<T> clazz) {
        return clazz.cast(getParameter(key));
    }

    /**
     * Returns this JwtHeader's parameters.
     *
     * @return {@code Map} of this JwtHeader's parameters.
     */
    public Map<String, Object> getParameters() {
        return getAll();
    }

    /**
     * Builds the JWT's header into a <code>String</code> representation of a JSON object.
     *
     * @return A JSON string.
     */
    public String build() {
        return toString();
    }
}
