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

package org.forgerock.json.jose.jwe;

import java.security.Key;

import org.forgerock.json.jose.jwe.handlers.compression.CompressionHandler;
import org.forgerock.json.jose.jwe.handlers.encryption.EncryptionHandler;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.json.jose.jwt.JwtHeader;
import org.forgerock.json.jose.jwt.Payload;
import org.forgerock.json.jose.utils.Utils;
import org.forgerock.util.encode.Base64url;

/**
 * A JWE implementation of the <tt>Jwt</tt> interface.
 * <p>
 * JSON Web Encryption (JWE) is a representing encrypted content using JSON based data structures.
 * <p>
 * @see <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-encryption-11">
 *     JSON Web Encryption Specification</a>
 *
 * @since 2.0.0
 */
public class EncryptedJwt implements Jwt, Payload {

    private final EncryptionManager encryptionManager = new EncryptionManager();
    private final CompressionManager compressionManager = new CompressionManager();

    private final JweHeader header;

    private Payload payload;
    private final Key publicKey;

    private final String encodedHeader;
    private final byte[] encryptedContentEncryptionKey;
    private final byte[] initialisationVector;
    private final byte[] ciphertext;
    private final byte[] authenticationTag;

    /**
     * Constructs a fresh, new EncryptedJwt from the given JweHeader and JwtClaimsSet.
     * <p>
     * The specified public key will be used to perform the encryption of the JWT.
     *
     * @param header The JweHeader containing the header parameters of the JWE.
     * @param payload The claimset of the JWE.
     * @param publicKey The public key to use to perform the encryption.
     */
    public EncryptedJwt(JweHeader header, JwtClaimsSet payload, Key publicKey) {
        this(header, (Payload) payload, publicKey);
    }

    EncryptedJwt(JweHeader header, Payload payload, Key encryptionKey) {
        this.header = header;
        this.payload = payload;
        this.publicKey = encryptionKey;

        this.encodedHeader = null;
        this.encryptedContentEncryptionKey = null;
        this.initialisationVector = null;
        this.ciphertext = null;
        this.authenticationTag = null;
    }

    /**
     * Constructs a reconstructed EncryptedJwt from its constituent parts, the JweHeader, encrypted Content Encryption
     * Key (CEK), initialisation vector, ciphertext and additional authentication data.
     * <p>
     * For use when an encrypted JWT has been reconstructed from its base64url encoded string representation and the
     * JWT needs decrypting.
     *
     * @param header The JweHeader containing the header parameters of the JWE.
     * @param encodedHeader The Base64url encoded JWE header.
     * @param encryptedContentEncryptionKey The encrypted Content Encryption Key (CEK).
     * @param initialisationVector The initialisation vector.
     * @param ciphertext The ciphertext.
     * @param authenticationTag The authentication tag.
     */
    public EncryptedJwt(JweHeader header, String encodedHeader, byte[] encryptedContentEncryptionKey,
            byte[] initialisationVector, byte[] ciphertext, byte[] authenticationTag) {
        this.header = header;
        this.encodedHeader = encodedHeader;
        this.encryptedContentEncryptionKey = encryptedContentEncryptionKey;
        this.initialisationVector = initialisationVector;
        this.ciphertext = ciphertext;
        this.authenticationTag = authenticationTag;

        this.publicKey = null;
    }

    @Override
    public JwtHeader getHeader() {
        return header;
    }

    @Override
    public JwtClaimsSet getClaimsSet() {
        return (JwtClaimsSet) payload;
    }

    /**
     * The payload of the encrypted JWT. This is either the claims set or a nested signed JWT.
     *
     * @return the payload of the encrypted JWT.
     */
    Payload getPayload() {
        return payload;
    }

    @Override
    public String build() {

        EncryptionHandler encryptionHandler = encryptionManager.getEncryptionHandler(header);

        Key contentEncryptionKey = encryptionHandler.getContentEncryptionKey();
        if (contentEncryptionKey == null) {
            contentEncryptionKey = publicKey;
        }
        byte[] encryptedContentEncryptionKey = encryptionHandler.generateJWEEncryptedKey(publicKey,
                contentEncryptionKey);
        String encodedEncryptedKey = Base64url.encode(encryptedContentEncryptionKey);


        byte[] initialisationVector = encryptionHandler.generateInitialisationVector();
        String encodedInitialisationVector = Base64url.encode(initialisationVector);


        String jweHeader = header.build();
        String encodedJweHeader = Utils.base64urlEncode(jweHeader);
        byte[] plaintext = compressPlaintext(header.getCompressionAlgorithm(),
                payload.build().getBytes(Utils.CHARSET));
        byte[] additionalAuthenticatedData = encodedJweHeader.getBytes(Utils.CHARSET);
        JweEncryption cipherTextAndAuthTag = encryptionHandler.encryptPlaintext(contentEncryptionKey,
                initialisationVector, plaintext, additionalAuthenticatedData);

        String encodedCiphertext = Base64url.encode(cipherTextAndAuthTag.getCiphertext());
        String encodedAuthenticationTag = Base64url.encode(cipherTextAndAuthTag.getAuthenticationTag());


        return new StringBuilder(encodedJweHeader)
                .append(".").append(encodedEncryptedKey)
                .append(".").append(encodedInitialisationVector)
                .append(".").append(encodedCiphertext)
                .append(".").append(encodedAuthenticationTag)
                .toString();
    }

    /**
     * Performs the compression of the plaintext, if required.
     * <p>
     * Whether or not compression is applied is based from the CompressionAlgorithm specified.
     *
     * @param compressionAlgorithm The CompressionAlgorithm describing the algorithm to use to compress the plaintext.
     * @param plaintext The plaintext.
     * @return A byte array of the (compressed) plaintext.
     */
    private byte[] compressPlaintext(CompressionAlgorithm compressionAlgorithm, byte[] plaintext) {
        CompressionHandler compressionHandler = compressionManager.getCompressionHandler(compressionAlgorithm);
        return compressionHandler.compress(plaintext);
    }

    /**
     * Decrypts the JWE ciphertext back into a JwtClaimsSet.
     * <p>
     * The same private key must be given here that is the pair to the public key that was used to encrypt the JWT.
     *
     * @param privateKey The private key pair to the public key that encrypted the JWT.
     */
    public void decrypt(Key privateKey) {

        EncryptionHandler encryptionHandler = encryptionManager.getEncryptionHandler(header);

        Key contentEncryptionKey = encryptionHandler.decryptContentEncryptionKey(privateKey,
                encryptedContentEncryptionKey);

        byte[] additionalAuthenticatedData = encodedHeader.getBytes(Utils.CHARSET);

        byte[] plaintext = encryptionHandler.decryptCiphertext(contentEncryptionKey, initialisationVector, ciphertext,
                authenticationTag, additionalAuthenticatedData);
        plaintext = decompressPlaintext(header.getCompressionAlgorithm(), plaintext);

        String decryptedPayload = new String(plaintext, Utils.CHARSET);

        payload = decodePayload(decryptedPayload);
    }

    /**
     * Performs decompression of the given plaintext if required. Whether or not decompression is actually applied
     * depends on the {@link CompressionAlgorithm}.
     *
     * @param compressionAlgorithm the compression algorithm.
     * @param plaintext the plaintext to decompress.
     * @return the decompressed plaintext.
     */
    private byte[] decompressPlaintext(CompressionAlgorithm compressionAlgorithm, byte[] plaintext) {
        CompressionHandler compressionHandler = compressionManager.getCompressionHandler(compressionAlgorithm);
        return compressionHandler.decompress(plaintext);
    }

    /**
     * Decodes the decrypted payload of this JWT.
     *
     * @param decryptedPayload the decrypted payload.
     * @return the decoded payload as either a {@link JwtClaimsSet} or nested {@link SignedJwt}.
     */
    Payload decodePayload(String decryptedPayload) {
        return new JwtClaimsSet(Utils.parseJson(decryptedPayload));
    }
}
