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

import org.forgerock.json.jose.jws.JwsHeader;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;

import java.security.Key;

public class SignedJwtBuilder extends AbstractJwtBuilder {

    private final Key privateKey;

    public SignedJwtBuilder(Key privateKey) {
        this.privateKey = privateKey;
    }

    @Override
    public JwsHeaderBuilder headers() {
        setJwtHeaderBuilder(new JwsHeaderBuilder(this));
        return (JwsHeaderBuilder) getHeaderBuilder();
    }

    @Override
    public String build() {
        JwsHeader header = (JwsHeader) getHeaderBuilder().build();
        JwtClaimsSet claimsSet = getClaimsSetBuilder().build();
        SignedJwt jwt = new SignedJwt(header, claimsSet, privateKey);
        return jwt.build();
    }
}
