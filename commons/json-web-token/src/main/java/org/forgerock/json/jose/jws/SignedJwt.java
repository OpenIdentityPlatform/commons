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

package org.forgerock.json.jose.jws;

import org.forgerock.json.jose.jwe.CompressionManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.json.jose.jwt.Payload;
import org.forgerock.json.jose.utils.Utils;
import org.forgerock.util.encode.Base64url;

/**
 * A JWS implementation of the <tt>Jwt</tt> interface.
 * <p>
 * JSON Web Signature (JWS) is a means of representing content secured with digital signatures or Message
 * Authentication Codes (MACs) using JSON based data structures.
 * <p>
 * @see <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-signature-11">JSON Web Signature Specification</a>
 *
 * @since 2.0.0
 */
public class SignedJwt implements Jwt, Payload {

    private final JwsHeader header;
    private final Payload payload;

    private final SigningHandler signingHandler;

    private final byte[] signingInput;
    private final byte[] signature;

    /**
     * Constructs a fresh, new SignedJwt from the given JwsHeader and JwtClaimsSet.
     * <p>
     * The specified private key will be used in the creation of the JWS signature.
     *
     * @param header The JwsHeader containing the header parameters of the JWS.
     * @param claimsSet The JwtClaimsSet containing the claims of the JWS.
     * @param signingHandler The SigningHandler instance used to sign the JWS.
     */
    public SignedJwt(JwsHeader header, JwtClaimsSet claimsSet, SigningHandler signingHandler) {
        this.header = header;
        this.payload = claimsSet;
        this.signingHandler = signingHandler;

        this.signingInput = null;
        this.signature = null;
    }

    /**
     * Constructs a reconstructed SignedJwt from its constituent parts, the JwsHeader, JwtClaimsSet, signing input and
     * signature.
     * <p>
     * For use when a signed JWT has been reconstructed from its base64url encoded string representation and the
     * signature needs verifying.
     *
     * @param header The JwsHeader containing the header parameters of the JWS.
     * @param claimsSet The JwsClaimsSet containing the claims of the JWS.
     * @param signingInput The original data that was signed, being the base64url encoding of the JWS header and
     *                     claims set concatenated using a "." character.
     * @param signature The resulting signature of signing the signing input.
     */
    public SignedJwt(JwsHeader header, JwtClaimsSet claimsSet, byte[] signingInput, byte[] signature) {
        this.header = header;
        this.payload = claimsSet;
        this.signingInput = signingInput;
        this.signature = signature;

        this.signingHandler = null;
    }

    /**
     * Constructs a fresh, new SignedJwt from the given JwsHeader and nested Encrypted JWT.
     * <p>
     * The specified private key will be used in the creation of the JWS signature.
     *
     * @param header The JwsHeader containing the header parameters of the JWS.
     * @param nestedPayload The nested payload that will be the payload of this JWS.
     * @param signingHandler The SigningHandler instance used to sign the JWS.
     */
    protected SignedJwt(JwsHeader header, Payload nestedPayload, SigningHandler signingHandler) {
        this.header = header;
        this.payload = nestedPayload;
        this.signingHandler = signingHandler;

        this.signingInput = null;
        this.signature = null;
    }

    /**
     * Constructs a reconstructed SignedJwt from its constituent parts, the JwsHeader, nested Encrypted JWT, signing
     * input and signature.
     * <p>
     * For use when a signed nested encrypted JWT has been reconstructed from its base64url encoded string
     * representation and the signature needs verifying.
     *
     * @param header The JwsHeader containing the header parameters of the JWS.
     * @param nestedPayload The nested payload that is the payload of the JWS.
     * @param signingInput The original data that was signed, being the base64url encoding of the JWS header and
     *                     payload concatenated using a "." character.
     * @param signature The resulting signature of signing the signing input.
     */
    protected SignedJwt(JwsHeader header, Payload nestedPayload, byte[] signingInput, byte[] signature) {
        this.header = header;
        this.payload = nestedPayload;
        this.signingInput = signingInput;
        this.signature = signature;

        this.signingHandler = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JwsHeader getHeader() {
        return header;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JwtClaimsSet getClaimsSet() {
        return (JwtClaimsSet) payload;
    }

    /**
     * Gets the payload for the JWS, which will either be a JWT Claims Set, {@link #getClaimsSet()}, or a nested
     * EncryptedJwt, {@link org.forgerock.json.jose.jwe.EncryptedJwt}.
     *
     * @return The JWS' payload.
     * @see EncryptedThenSignedJwt
     */
    protected Payload getPayload() {
        return payload;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String build() {

        String jwsHeader = header.build();
        String encodedHeader = Utils.base64urlEncode(jwsHeader);
        String jwsPayload = payload.build();

        String encodedClaims = new CompressionManager().compress(header.getCompressionAlgorithm(), jwsPayload);

        String signingInput = encodedHeader + "." + encodedClaims;

        byte[] signature = signingHandler.sign(header.getAlgorithm(), signingInput);

        return signingInput + "." + Base64url.encode(signature);
    }

    /**
     * Verifies that the JWS signature is valid for the contents of its payload.
     * <p>
     * The same private key must be given here as was used to create the signature.
     *
     * @param signingHandler The SigningHandler instance used to verify the JWS.
     * @return <code>true</code> if the signature matches the JWS Header and payload.
     */
    public boolean verify(SigningHandler signingHandler) {
        return signingHandler.verify(header.getAlgorithm(), signingInput, signature);
    }
}
