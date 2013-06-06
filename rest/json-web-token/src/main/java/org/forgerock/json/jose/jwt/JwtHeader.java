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

package org.forgerock.json.jose.jwt;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.jose.jws.JwsAlgorithm;

import java.util.HashMap;
import java.util.Map;

public abstract class JwtHeader extends JsonValue {    //TODO maybe add reserved headers to json and just get them by key name

    private static final String TYPE_HEADER_KEY = "typ";
    private static final String ALGORITHM_HEADER_KEY = "alg";       //TODO can actually by media types to in the case of JWS

    public JwtHeader() {
        super(new HashMap<String, Object>());
    }

    public JwtHeader(JsonValue value) {
        super(value);
    }

    public JwtHeader(Map<String, Object> headerParameters) {
        super(headerParameters);
    }

    public void setAlgorithm(Algorithm algorithm) {
        put(ALGORITHM_HEADER_KEY, algorithm.toString());
    }

    public abstract Algorithm getAlgorithm();

    protected String getAlgorithmString() {
        return getHeaderParameter(ALGORITHM_HEADER_KEY).asString();
    }

    public JwtType getJwtType() {
        return JwtType.valueOf(get(TYPE_HEADER_KEY).asString());
    }

    public void addHeaderParameter(String key, Object value) {
        add(key, value);                  //TODO include required header parameters
    }

    public void addHeaderParameters(Map<String, Object> headerParameters) {
        for (String key : headerParameters.keySet()) {
            addHeaderParameter(key, headerParameters.get(key));
        }
    }

    public JsonValue getHeaderParameter(String key) {
        return get(key);            //TODO include required header parameters
    }

    public String build() {
        add(TYPE_HEADER_KEY, JwtType.JWT.toString()); //TODO determine dynamically, i.e. when signing and encrypted JWT
        return toString();
    }

    @Override
    public void add(String key, Object value) throws JsonValueException {
        if (value != null) {
            super.add(key, value);
        }
    }

    @Override
    public void put(String key, Object value) throws JsonValueException {
        if (value != null) {
            super.put(key, value);
        }
    }
}
