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

import org.forgerock.json.jose.jwt.JwtClaimsSet;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JwtClaimsSetBuilder {

    private final AbstractJwtBuilder jwtBuilder;

    private final Map<String, Object> claims = new HashMap<String, Object>();

    public JwtClaimsSetBuilder(AbstractJwtBuilder jwtBuilder) {
        this.jwtBuilder = jwtBuilder;
    }

    public JwtClaimsSetBuilder claim(String key, Object claim) {
        claims.put(key, claim);
        return this;
    }

    public JwtClaimsSetBuilder claims(Map<String, Object> claims) {
        this.claims.putAll(claims);
        return this;
    }

    public JwtClaimsSetBuilder typ(String typ) {
        return claim("typ", typ);
    }

    public JwtClaimsSetBuilder jti(String jti) {
        return claim("jti", jti);
    }

    public JwtClaimsSetBuilder iss(String iss) {
        return claim("iss", iss);
    }

    public JwtClaimsSetBuilder prn(String prn) {
        return claim("prn", prn);
    }

    public JwtClaimsSetBuilder aud(List<String> aud) {
        return claim("aud", aud);
    }

    public JwtClaimsSetBuilder iat(Date iat) {
        return claim("iat", iat);
    }

    public JwtClaimsSetBuilder nbf(Date nbf) {
        return claim("nbf", nbf);
    }

    public JwtClaimsSetBuilder exp(Date exp) {
        return claim("exp", exp);
    }

    public AbstractJwtBuilder done() {
        return jwtBuilder;
    }

    protected JwtClaimsSet build() {
        return new JwtClaimsSet(claims);
    }
}
