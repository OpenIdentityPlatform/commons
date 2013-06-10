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

package org.forgerock.json.jose.common;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.jose.jwe.EncryptedJwt;
import org.forgerock.json.jose.jwe.JweHeader;
import org.forgerock.json.jose.jws.JwsHeader;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.json.jose.jwt.JwtHeader;
import org.forgerock.json.jose.utils.Utils;
import org.forgerock.util.encode.Base64url;

import java.io.IOException;
import java.util.Map;

public class JwtReconstruction {

    public <T extends Jwt> T reconstructJwt(String jwtString, Class<T> jwtClass) {

        Jwt jwt;

        //split into parts
        String[] jwtParts = jwtString.split("\\.", -1);
        if (jwtParts.length != 3 && jwtParts.length != 5) {
            //TODO
            throw new RuntimeException("not right number of dots");
        }

        //first part always header
        //turn into json value
        JsonValue headerJson = new JsonValue(parseJson(new String(Base64url.decode(jwtParts[0]), Utils.CHARSET)));

        if (headerJson.isDefined("enc")) {
            //is encrypted jwt
            jwt = reconstructEncryptedJwt(jwtParts);
        } else {
            //is signed jwt or plaintext
            jwt = reconstructSignedJwt(jwtParts);
        }

        return (T) jwt;
    }

    private Map<String, Object> parseJson(String headerString) {

        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(headerString, Map.class);
        } catch (IOException e) {
            throw new JsonException("Failed to parse json", e);
        }
    }

    private SignedJwt reconstructSignedJwt(String[] jwtParts) {

        String encodedHeader = jwtParts[0];
        String encodedClaimsSet = jwtParts[1];
        String encodedSignature = jwtParts[2];


        String header = new String (Base64url.decode(encodedHeader), Utils.CHARSET);
        String claimsSetString = new String (Base64url.decode(encodedClaimsSet), Utils.CHARSET);
        byte[] signature = Base64url.decode(encodedSignature);

        JwsHeader jwsHeader = new JwsHeader(parseJson(header));

        JwtClaimsSet claimsSet = new JwtClaimsSet(parseJson(claimsSetString));

        return new SignedJwt(jwsHeader, claimsSet, (encodedHeader + "." + encodedClaimsSet).getBytes(Utils.CHARSET), signature);
    }

    private EncryptedJwt reconstructEncryptedJwt(String[] jwtParts) {

        String encodedHeader = jwtParts[0];
        String encodedEncryptedKey = jwtParts[1];
        String encodedInitialisationVector = jwtParts[2];
        String encodedCiphertext = jwtParts[3];
        String encodedAuthenticationTag = jwtParts[4];


        String header = new String(Base64url.decode(encodedHeader), Utils.CHARSET);
        byte[] encryptedContentEncryptionKey = Base64url.decode(encodedEncryptedKey);
        byte[] initialisationVector = Base64url.decode(encodedInitialisationVector);
        byte[] ciphertext = Base64url.decode(encodedCiphertext);
        byte[] authenticationTag = Base64url.decode(encodedAuthenticationTag);


        JweHeader jweHeader = new JweHeader(parseJson(header));


        return new EncryptedJwt(jweHeader, encryptedContentEncryptionKey, initialisationVector, ciphertext, authenticationTag);
    }
}
