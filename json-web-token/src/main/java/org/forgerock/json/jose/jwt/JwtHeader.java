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

import java.util.Map;

import static org.forgerock.json.jose.jwt.JwtHeaderKey.*;

public abstract class JwtHeader extends JWObject {

    public JwtHeader() {
        put(TYP.value(), JwtType.JWT.toString()); //TODO determine dynamically, i.e. when signing and encrypted JWT
    }

    public JwtHeader(Map<String, Object> headers) {
        this();
        setHeaders(headers);
    }

    public void setAlgorithm(Algorithm algorithm) {
        put(ALG.value(), algorithm.toString());
    }

    public abstract Algorithm getAlgorithm();

    protected String getAlgorithmString() {
        return get(ALG.value()).asString();
    }

    public JwtType getJwtType() {
        return JwtType.valueOf(get(TYP.value()).asString().toUpperCase());
    }

    public void setHeader(String key, Object value) {
        JwtHeaderKey headerKey = getHeaderKey(key.toUpperCase());

        switch (headerKey) {
            case TYP: {
//                checkValueIsOfType(value, String.class);
//                setType((String) value);    //TODO what to do here as only system can set typ header???
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

    public void setHeaders(Map<String, Object> headers) {
        for (String key : headers.keySet()) {
            setHeader(key, headers.get(key));
        }
    }

    public Object getHeader(String key) {
        JwtHeaderKey headerKey = getHeaderKey(key.toUpperCase());

        Object value;

        switch (headerKey) {
            case TYP: {
                value = getJwtType();
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

    public <T> T getHeader(String key, Class<T> clazz) {
        return (T) getHeader(key);
    }

    public String build() {
        return toString();
    }
}
