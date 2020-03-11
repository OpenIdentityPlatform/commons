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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.json.jose.jwe;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.exceptions.JweDecryptionException;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.util.Utils;
import org.forgerock.util.encode.Base64url;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class EncryptedJwtTest {
    private static final Logger logger = LoggerFactory.getLogger(EncryptedJwtTest.class);
    private KeyPair rsaKeyPair;
    private Key symmetricKey;

    private JwtBuilderFactory jbf;
    private JwtReconstruction jwtReconstruction;

    @BeforeClass
    public void generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        rsaKeyPair = keyPairGenerator.generateKeyPair();

        byte[] keyData = new byte[32];
        Arrays.fill(keyData, (byte) 42);
        symmetricKey = new SecretKeySpec(keyData, "AES");
    }

    @BeforeMethod
    public void createJwtBuilderFactory() {
        jbf = new JwtBuilderFactory();
        jwtReconstruction = new JwtReconstruction();
    }

    @Test(dataProvider = "cbcEncryptionMethods")
    public void shouldNotLeakInformationAboutDecryptionFailures(EncryptionMethod encryptionMethod,
                                                                JweAlgorithm algorithm)
            throws Exception {

        // Check if the JRE supports the key size requested
        final int encryptionKeySize = encryptionMethod.getKeySize() - (encryptionMethod.getKeyOffset() * 8);
        if (Cipher.getMaxAllowedKeyLength(encryptionMethod.getEncryptionAlgorithm()) < encryptionKeySize) {
            throw new SkipException("Install JCE Unlimited Strength to test AES-256 and above");
        }

        // Given
        JwtClaimsSet claims = new JwtBuilderFactory().claims().build();
        String encryptedJwt = new JwtBuilderFactory().jwe(rsaKeyPair.getPublic()).claims(claims)
                                                     .headers().enc(encryptionMethod).alg(algorithm).done().build();

        String[] encryptedJwtParts = encryptedJwt.split("\\."); // header.key.iv.cipherText.tag
        byte[] initialisationVector = Base64url.decode(encryptedJwtParts[2]);
        byte[] cipherText = Base64url.decode(encryptedJwtParts[3]);
        assertThat(cipherText.length).isEqualTo(16); // Should be a single block to make this simple

        // When
        // Simulate a padding oracle attack against the last byte of the cipher text. This may generate different
        // error conditions depending on MAC vs padding errors. It should not be possible to distinguish these
        // cases, otherwise the attacker can easily break the encryption.
        int ivEnd = initialisationVector.length - 1;
        byte pad = 0x01;
        final Set<String> errorMessages = new HashSet<>();
        final Set<String> stackTraces = new HashSet<>();
        for (int i = 0; i < 256; ++i) {
            try {
                // NB: we are assuming CBC-mode block cipher with IV...
                byte newLastIvByte = (byte) ((initialisationVector[ivEnd] ^ i ^ pad) & 0xff);
                initialisationVector[ivEnd] = newLastIvByte;
                encryptedJwtParts[2] = Base64url.encode(initialisationVector);

                jwtReconstruction.reconstructJwt(join(encryptedJwtParts, '.'), EncryptedJwt.class)
                        .decrypt(rsaKeyPair.getPrivate());

            } catch (JweDecryptionException ex) {
                errorMessages.add(ex.getMessage());
                StringWriter buffer = new StringWriter();
                ex.printStackTrace(new PrintWriter(buffer));
                stackTraces.add(buffer.toString());
            }
        }

        // Then
        assertThat(errorMessages).as("Distinct error messages").hasSize(1);
        assertThat(stackTraces).as("Distinct stack traces").hasSize(1);
    }

    @Test(dataProvider = "encryptionMethods")
    public void shouldRoundTrip(EncryptionMethod encryptionMethod, JweAlgorithm jweAlgorithm) throws Exception {
        // Given
        final JwtClaimsSet claims = new JwtClaimsSet();
        claims.setClaim("test1", "This is a test claim");
        claims.setIssuedAtTime(new Date());
        final Key encryptionKey = getEncryptionKey(encryptionMethod, jweAlgorithm);
        final Key decryptionKey = getDecryptionKey(encryptionMethod, jweAlgorithm);

        // When
        String jwt = jbf.jwe(encryptionKey).headers().alg(jweAlgorithm).enc(encryptionMethod).done().claims(claims)
                        .build();
        EncryptedJwt result = jwtReconstruction.reconstructJwt(jwt, EncryptedJwt.class);
        result.decrypt(decryptionKey);

        // Then
        assertThat(result.getClaimsSet().build()).isEqualTo(claims.build());
    }

    @Test(dataProvider = "encryptionMethods", expectedExceptions = JweDecryptionException.class)
    public void shouldRejectInvalidAuthenticationTags(EncryptionMethod encryptionMethod, JweAlgorithm jweAlgorithm) {
        // Given
        final JwtClaimsSet claims = new JwtClaimsSet();
        claims.setClaim("test1", "This is a test claim");
        claims.setIssuedAtTime(new Date());
        final Key encryptionKey = getEncryptionKey(encryptionMethod, jweAlgorithm);
        final Key decryptionKey = getDecryptionKey(encryptionMethod, jweAlgorithm);
        String jwt = jbf.jwe(encryptionKey).headers().alg(jweAlgorithm).enc(encryptionMethod).done().claims(claims)
                        .build();

        // When
        int index = jwt.lastIndexOf('.');
        byte[] tag = Base64url.decode(jwt.substring(index + 1));
        tag[0] ^= 0x01; // Flip a bit in the tag
        String alteredJwt = jwt.substring(0, index) + '.' + Base64url.encode(tag);
        EncryptedJwt result = jwtReconstruction.reconstructJwt(alteredJwt, EncryptedJwt.class);
        result.decrypt(decryptionKey);

        // Then - exception
    }

    @Test(dataProvider = "encryptionMethods", expectedExceptions = JweDecryptionException.class)
    public void shouldRejectInvalidEncryptedKey(EncryptionMethod encryptionMethod, JweAlgorithm jweAlgorithm) {
        if (jweAlgorithm.getAlgorithmType() == JweAlgorithmType.DIRECT) {
            // Not relevant to this mode, throw exception to pass the test
            throw new JweDecryptionException();
        }

        // Given
        final JwtClaimsSet claims = new JwtClaimsSet();
        claims.setClaim("test1", "This is a test claim");
        claims.setIssuedAtTime(new Date());
        final Key encryptionKey = getEncryptionKey(encryptionMethod, jweAlgorithm);
        final Key decryptionKey = getDecryptionKey(encryptionMethod, jweAlgorithm);
        String jwt = jbf.jwe(encryptionKey).headers().alg(jweAlgorithm).enc(encryptionMethod).done().claims(claims)
                        .build();

        // When
        String[] parts = jwt.split("\\.");
        byte[] encryptedKey = Base64url.decode(parts[1]);
        encryptedKey[0] ^= 0x01; // Flip a bit in the key
        String alteredJwt = parts[0] + '.' + Base64url.encode(encryptedKey) + '.' + parts[2] + '.' + parts[3] + '.'
                + parts[4];
        EncryptedJwt result = jwtReconstruction.reconstructJwt(alteredJwt, EncryptedJwt.class);
        result.decrypt(decryptionKey);

        // Then - exception
    }

    @Test
    public void shouldDecompressCorrectly() {
        // Given
        final JwtClaimsSet claims = new JwtClaimsSet();
        for (int i = 0; i < 20; ++i) {
            claims.setClaim(Integer.toString(i), "aaaaaaaaaaaaaaaaaaaa");
        }

        // When
        String jwt = encryptedJwtWithCompression(CompressionAlgorithm.DEF, claims);
        EncryptedJwt result = jwtReconstruction.reconstructJwt(jwt, EncryptedJwt.class);
        result.decrypt(symmetricKey);

        // Then
        assertThat(result.getClaimsSet().build()).isEqualTo(claims.build());
    }

    @Test
    public void shouldCompressWhenAsked() {
        // Given
        final JwtClaimsSet claims = new JwtClaimsSet();
        // Repetitive claim values should compress well
        for (int i = 0; i < 20; ++i) {
            claims.setClaim(Integer.toString(i), "aaaaaaaaaaaaaaaaaaaa");
        }

        // When
        String compressedJwt = encryptedJwtWithCompression(CompressionAlgorithm.DEF, claims);
        String uncompressedJwt = encryptedJwtWithCompression(CompressionAlgorithm.NONE, claims);

        logger.info("Uncompressed size = {} bytes, Compressed size = {} bytes", uncompressedJwt.length(),
                compressedJwt.length());

        // Then
        assertThat(compressedJwt.length()).isLessThan(uncompressedJwt.length());
    }

    private Key getEncryptionKey(EncryptionMethod method, JweAlgorithm algorithm) {
        // For RSA we use the public key, for all others we generate an all-zero secret key of the correct size
        switch (algorithm.getAlgorithmType()) {
        case RSA:
            return rsaKeyPair.getPublic();
        case AES_KEYWRAP:
            switch (algorithm) {
            case A128KW:
                return new SecretKeySpec(new byte[16], "AES");
            case A192KW:
                return new SecretKeySpec(new byte[24], "AES");
            case A256KW:
                return new SecretKeySpec(new byte[32], "AES");
            default:
                throw new IllegalArgumentException("Invalid key-wrap mode: " + algorithm);
            }
        case DIRECT:
            byte[] keyMaterial = new byte[method.getKeySize() / 8];
            return new SecretKeySpec(keyMaterial, "AES");
        default:
            return null;
        }
    }

    private Key getDecryptionKey(EncryptionMethod method, JweAlgorithm algorithm) {
        if (algorithm.getAlgorithmType() == JweAlgorithmType.RSA) {
            return rsaKeyPair.getPrivate();
        } else {
            return getEncryptionKey(method, algorithm);
        }
    }


    @DataProvider
    public Object[][] cbcEncryptionMethods() {
        return new Object[][] {
                { EncryptionMethod.A128CBC_HS256, JweAlgorithm.RSAES_PKCS1_V1_5 },
                { EncryptionMethod.A192CBC_HS384, JweAlgorithm.RSAES_PKCS1_V1_5 },
                { EncryptionMethod.A256CBC_HS512, JweAlgorithm.RSAES_PKCS1_V1_5 }
        };
    }

    @DataProvider
    public Object[][] encryptionMethods() throws Exception {
        final List<Object[]> result = new ArrayList<>();
        int i = 0;
        for (JweAlgorithm jweAlgorithm : JweAlgorithm.values()) {
            if ((jweAlgorithm == JweAlgorithm.A192KW || jweAlgorithm == JweAlgorithm.A256KW)
                && Cipher.getMaxAllowedKeyLength("AES") < 192) {
                logger.warn("Please install JCE Unlimited Strength to test A192KW or A256KW");
                continue;
            }
            for (EncryptionMethod encryptionMethod : EncryptionMethod.values()) {
                try {
                    Cipher.getInstance(encryptionMethod.getTransformation());
                } catch (NoSuchAlgorithmException e) {
                    logger.warn("Skipping EncryptionMethod {} as JRE does not support it", encryptionMethod);
                    continue;
                }
                if (Cipher.getMaxAllowedKeyLength(encryptionMethod.getTransformation())
                        < encryptionMethod.getKeyOffset() * 8) {
                    logger.warn("Skipping EncryptionMethod {} as JRE does not support this key size", encryptionMethod);
                    continue;
                }
                result.add(new Object[]{ encryptionMethod, jweAlgorithm });
            }
        }
        return result.toArray(new Object[0][]);
    }

    private String encryptedJwtWithCompression(CompressionAlgorithm compressionAlgorithm, JwtClaimsSet claims) {
        return jbf.jwe(symmetricKey).headers().alg(JweAlgorithm.DIRECT).enc(EncryptionMethod.A128CBC_HS256)
                .zip(compressionAlgorithm).done().claims(claims).build();
    }

    private String join(String[] parts, char delim) {
        return Utils.joinAsString(Character.toString(delim), (Object[]) parts);
    }
}
