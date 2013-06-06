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

package org.forgerock.json.jose.builders;

import org.forgerock.json.jose.jwt.Algorithm;
import org.forgerock.json.jose.jwt.JwtHeader;

import java.util.HashMap;
import java.util.Map;

public abstract class JwtHeaderBuilder {

    private final AbstractJwtBuilder jwtBuilder;

    private final Map<String, Object> headers = new HashMap<String, Object>();

    public JwtHeaderBuilder(AbstractJwtBuilder jwtBuilder) {
        this.jwtBuilder = jwtBuilder;
    }

    public JwtHeaderBuilder header(String key, Object value) {
        headers.put(key, value);
        return this;
    }

    public JwtHeaderBuilder alg(Algorithm algorithm) {
        header("alg", algorithm.toString());
        return this;
    }

    public AbstractJwtBuilder done() {
        return jwtBuilder;
    }

    protected Map<String, Object> getHeaders() {
        return headers;
    }

    protected abstract JwtHeader build();
}
