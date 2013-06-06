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

import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jwe.EncryptedJwt;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.Jwt;

import java.security.Key;
import java.util.Date;

public class JwtBuilder {

    public AbstractJwtBuilder jwt() {
        return new SignedJwtBuilder(null);
    }

    public SignedJwtBuilder jws(Key privateKey) {
        return new SignedJwtBuilder(privateKey);
    }

    public EncryptedJwtBuilder jwe(Key publicKey) {
        return new EncryptedJwtBuilder(publicKey);
    }

    public <T extends Jwt> T reconstruct(String jwtString, Class<T> jwtClass) {
        return new JwtReconstruction().reconstructJwt(jwtString, jwtClass);
    }
}
