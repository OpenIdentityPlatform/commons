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

package org.forgerock.json.jose.common;

import java.util.HashMap;
import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.exceptions.InvalidJwtException;
import org.forgerock.json.jose.exceptions.JwtReconstructionException;
import org.forgerock.json.jose.jwe.CompressionManager;
import org.forgerock.json.jose.jwe.EncryptedJwt;
import org.forgerock.json.jose.jwe.JweHeader;
import org.forgerock.json.jose.jwe.SignedThenEncryptedJwt;
import org.forgerock.json.jose.jws.EncryptedThenSignedJwt;
import org.forgerock.json.jose.jws.JwsHeader;
import org.forgerock.json.jose.jws.SignedEncryptedJwt;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.json.jose.jwt.JwtType;
import org.forgerock.json.jose.utils.Utils;
import org.forgerock.util.encode.Base64url;

/**
 * A service that provides a method for reconstruct a JWT string back into its relevant JWT object,
 * (SignedJwt, EncryptedJwt, SignedEncryptedJwt).
 *
 * @since 2.0.0
 */
public class JwtReconstruction {
    private static final String PAYLOAD_CONTENT_TYPE = "cty";
    private static final String JWT_TYPE = "typ";
    private static final String ENCRYPTION_METHOD = "enc";
    private static final String ALGORITHM = "alg";

    private static final int JWS_NUM_PARTS = 3;
    private static final int JWE_NUM_PARTS = 5;

    /**
     * Reconstructs the given JWT string into a JWT object of the specified type.
     *
     * @param jwtString The JWT string.
     * @param jwtClass The JWT class to reconstruct the JWT string to.
     * @param <T> The type of JWT the JWT string represents.
     * @return The reconstructed JWT object.
     * @throws InvalidJwtException If the jwt does not consist of the correct number of parts.
     * @throws JwtReconstructionException If the jwt does not consist of the correct number of parts.
     */
    public <T extends Jwt> T reconstructJwt(String jwtString, Class<T> jwtClass) {


        //split into parts
        String[] jwtParts = jwtString.split("\\.", -1);
        if (jwtParts.length != 3 && jwtParts.length != 5) {
            throw new InvalidJwtException("not right number of dots, " + jwtParts.length);
        }

        //first part always header
        //turn into json value
        JsonValue headerJson = new JsonValue(Utils.parseJson(Utils.base64urlDecode(jwtParts[0])));
        JwtType contentType = null;
        if (headerJson.isDefined(PAYLOAD_CONTENT_TYPE)) {
            contentType = JwtType.jwtType(headerJson.get(PAYLOAD_CONTENT_TYPE).asString());
        }

        JwtType jwtType = null;
        if (headerJson.isDefined(JWT_TYPE)) {
            jwtType = JwtType.jwtType(headerJson.get(JWT_TYPE).asString());
        }

        final Jwt jwt;
        if (headerJson.isDefined(ENCRYPTION_METHOD)) {
            //is encrypted jwt
            verifyNumberOfParts(jwtParts, JWE_NUM_PARTS);
            jwt = reconstructEncryptedJwt(jwtParts);
        } else if (JwtType.JWE == contentType || JwtType.JWT == contentType || JwtType.JWE == jwtType) {
            verifyNumberOfParts(jwtParts, JWS_NUM_PARTS);
            jwt = reconstructSignedEncryptedJwt(jwtParts);
        } else if (headerJson.isDefined(ALGORITHM)) {
            //is signed jwt
            verifyNumberOfParts(jwtParts, JWS_NUM_PARTS);
            jwt = reconstructSignedJwt(jwtParts);
        } else {
            //plaintext jwt
            verifyNumberOfParts(jwtParts, JWS_NUM_PARTS);
            if (!jwtParts[2].isEmpty()) {
                throw new InvalidJwtException("Third part of Plaintext JWT not empty.");
            }
            jwt = reconstructSignedJwt(jwtParts);
        }

        return jwtClass.cast(jwt);
    }

    /**
     * Verifies that the JWT parts are the required length for the JWT type being reconstructed.
     *
     * @param jwtParts The JWT parts.
     * @param required The required number of parts.
     * @throws JwtReconstructionException If the jwt does not consist of the correct number of parts.
     */
    private void verifyNumberOfParts(String[] jwtParts, int required) {
        if (jwtParts.length != required) {
            throw new JwtReconstructionException("Not the correct number of JWT parts. Expecting, " + required
                    + ", actually, " + jwtParts.length);
        }
    }

    /**
     * Reconstructs a Signed JWT from the given JWT string parts.
     * <p>
     * As a plaintext JWT is a JWS with an empty signature, this method should be used to reconstruct plaintext JWTs
     * as well as signed JWTs.
     *
     * @param jwtParts The three base64url UTF-8 encoded string parts of a plaintext or signed JWT.
     * @return A SignedJwt object.
     */
    private SignedJwt reconstructSignedJwt(String[] jwtParts) {

        String encodedHeader = jwtParts[0];
        String encodedClaimsSet = jwtParts[1];
        String encodedSignature = jwtParts[2];

        String header = Utils.base64urlDecode(encodedHeader);

        byte[] signature = Base64url.decode(encodedSignature);

        JwsHeader jwsHeader = new JwsHeader(Utils.parseJson(header));

        byte[] payload = new CompressionManager().decompress(jwsHeader.getCompressionAlgorithm(), encodedClaimsSet);
        JwtClaimsSet claimsSet = new JwtClaimsSet(Utils.parseJson(new String(payload, Utils.CHARSET)));

        return new SignedJwt(jwsHeader, claimsSet, (encodedHeader + "." + encodedClaimsSet).getBytes(Utils.CHARSET),
                signature);
    }

    /**
     * Reconstructs an encrypted JWT from the given JWT string parts.
     *
     * @param jwtParts The five base64url UTF-8 encoded string parts of an encrypted JWT.
     * @return An EncryptedJwt object.
     */
    private EncryptedJwt reconstructEncryptedJwt(String[] jwtParts) {

        String encodedHeader = jwtParts[0];
        String encodedEncryptedKey = jwtParts[1];
        String encodedInitialisationVector = jwtParts[2];
        String encodedCiphertext = jwtParts[3];
        String encodedAuthenticationTag = jwtParts[4];


        String header = Utils.base64urlDecode(encodedHeader);
        byte[] encryptedContentEncryptionKey = Base64url.decode(encodedEncryptedKey);
        byte[] initialisationVector = Base64url.decode(encodedInitialisationVector);
        byte[] ciphertext = Base64url.decode(encodedCiphertext);
        byte[] authenticationTag = Base64url.decode(encodedAuthenticationTag);


        JweHeader jweHeader = new JweHeader(Utils.parseJson(header));

        if (jweHeader.getContentType() != null) {
            return new SignedThenEncryptedJwt(jweHeader, encodedHeader, encryptedContentEncryptionKey,
                    initialisationVector, ciphertext, authenticationTag);
        } else {
            return new EncryptedJwt(jweHeader, encodedHeader, encryptedContentEncryptionKey, initialisationVector,
                    ciphertext, authenticationTag);
        }
    }

    /**
     * Reconstructs a signed and encrypted JWT from the given JWT string parts.
     * <p>
     * First reconstructs the nested encrypted JWT from within the signed JWT and then reconstructs the signed JWT using
     * the reconstructed nested EncryptedJwt.
     *
     * @param jwtParts The three base64url UTF-8 encoded string parts of a signed JWT.
     * @return A SignedEncryptedJwt object.
     */
    private EncryptedThenSignedJwt reconstructSignedEncryptedJwt(String[] jwtParts) {

        String encodedHeader = jwtParts[0];
        String encodedPayload = jwtParts[1];
        String encodedSignature = jwtParts[2];


        String header = Utils.base64urlDecode(encodedHeader);
        String payloadString = Utils.base64urlDecode(encodedPayload);
        byte[] signature = Base64url.decode(encodedSignature);

        //split into parts
        String[] encryptedJwtParts = payloadString.split("\\.", -1);
        verifyNumberOfParts(encryptedJwtParts, JWE_NUM_PARTS);
        EncryptedJwt encryptedJwt = reconstructEncryptedJwt(encryptedJwtParts);

        Map<String, Object> combinedHeader = new HashMap<>(encryptedJwt.getHeader().getParameters());
        combinedHeader.putAll(Utils.parseJson(header));

        JwsHeader jwsHeader = new JwsHeader(combinedHeader);

        // This can be changed to return EncryptedThenSignedJwt once SignedEncryptedJwt is removed
        return new SignedEncryptedJwt(jwsHeader, encryptedJwt,
                (encodedHeader + "." + encodedPayload).getBytes(Utils.CHARSET), signature);
    }
}
