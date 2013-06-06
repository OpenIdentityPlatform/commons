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

package org.forgerock.json.jose.jws;

import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.json.jose.jwt.JwtHeader;
import org.forgerock.json.jose.utils.Utils;
import org.forgerock.util.encode.Base64url;

import java.security.Key;
import java.security.PrivateKey;

public class SignedJwt implements Jwt {

    private final SigningManager signingManager = new SigningManager();

    private final JwsHeader header;
    private final JwtClaimsSet claimsSet;

    private final Key privateKey;

    private final byte[] signingInput;
    private final byte[] signature;

    public SignedJwt(JwsHeader header, JwtClaimsSet claimsSet, Key privateKey) {
        this.header = header;
        this.claimsSet = claimsSet;
        this.privateKey = privateKey;

        this.signingInput = null;
        this.signature = null;
    }

    public SignedJwt(JwsHeader header, JwtClaimsSet claimsSet, byte[] signingInput, byte[] signature) {
        this.header = header;
        this.claimsSet = claimsSet;
        this.signingInput = signingInput;
        this.signature = signature;

        this.privateKey = null;
    }

    @Override
    public JwtHeader getHeader() {
        return header;
    }

    @Override
    public JwtClaimsSet getClaimsSet() {
        return claimsSet;
    }

    @Override
    public String build() {

        String jwsHeader = header.build();
        String encodedHeader = Base64url.encode(jwsHeader.getBytes(Utils.CHARSET));
        String jwsPayload = claimsSet.build();
        String encodedClaims = Base64url.encode(jwsPayload.getBytes(Utils.CHARSET));

        String signingInput = encodedHeader + "." + encodedClaims;

        byte[] signature = signingManager.sign(header.getAlgorithm(), (PrivateKey) privateKey, signingInput);

        return signingInput + "." + Base64url.encode(signature);
    }

    public boolean verify(Key privateKey) {
        return signingManager.verify(header.getAlgorithm(), (PrivateKey) privateKey, signingInput, signature);
    }
}
