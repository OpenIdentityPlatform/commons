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

import org.forgerock.json.jose.exceptions.JweException;
import org.forgerock.json.jose.jwt.Algorithm;

/**
 * An Enum of the possible encryption algorithms that can be used to encrypt a JWT.
 * <p>
 * @see <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-algorithms-11#section-4.1">JWE Algorithms</a>
 *
 * @since 2.0.0
 */
public enum JweAlgorithm implements Algorithm {

    /** RSA in ECB mode with PKCS1 Padding. */
    RSAES_PKCS1_V1_5("RSA1_5", "RSA/ECB/PKCS1Padding", JweAlgorithmType.RSA),
    /** RSA in ECB mode with OAEP with SHA-1 and MGF1 padding.*/
    RSA_OAEP("RSA-OAEP", "RSA/ECB/OAEPWithSHA-1AndMGF1Padding", JweAlgorithmType.RSA),
    /** RSA in ECB mode with OAEP with SHA-256 and MGF1 with SHA-256 padding. */
    RSA_OAEP_256("RSA-OAEP-256", "RSA/ECB/OAEPWithSHA-256AndMGF1Padding", JweAlgorithmType.RSA),
    /** Direct encryption with a shared symmetric key. */
    DIRECT("dir", null, JweAlgorithmType.DIRECT),
    /** AES-128 KeyWrap. */
    A128KW("A128KW", "AESWrap", JweAlgorithmType.AES_KEYWRAP),
    /** AES-192 KeyWrap. */
    A192KW("A192KW", "AESWrap", JweAlgorithmType.AES_KEYWRAP),
    /** AES-256 KeyWrap. */
    A256KW("A256KW", "AESWrap", JweAlgorithmType.AES_KEYWRAP);

    private final String name;
    private final String transformation;
    private final JweAlgorithmType algorithmType;

    /**
     * Constructs a new JweAlgorithm with the Java Cryptographic string name of the algorithm and The JweAlgorithmType
     * of the algorithm.
     *
     * @param name The header name of the algorithm.
     * @param transformation The Java Cryptographic algorithm name
     * @param algorithmType The JweAlgorithmType of the JweAlgorithm.
     */
    JweAlgorithm(String name, String transformation, JweAlgorithmType algorithmType) {
        this.name = name;
        this.transformation = transformation;
        this.algorithmType = algorithmType;
    }

    @Override
    public String getAlgorithm() {
        return transformation;
    }

    /**
     * Gets the JweAlgorithmType of the JweAlgorithm.
     *
     * @return The JweAlgorithmType.
     */
    public JweAlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    /**
     * Parses the given algorithm string to find the matching EncryptionMethod enum constant.
     *
     * @param algorithm The encryption algorithm.
     * @return The JweAlgorithm enum.
     */
    public static JweAlgorithm parseAlgorithm(String algorithm) {
        for (JweAlgorithm alg : JweAlgorithm.values()) {
            if (alg.name.equals(algorithm)) {
                return alg;
            }
        }
        // Compatibility fix: previous version of that library used to issue a wrong
        // (non-standard) algorithm name. When reconstructing old JWTs, we have to recognize
        // these old values ('RSAES_PKCS1_V1_5')
        if (RSAES_PKCS1_V1_5.name().equals(algorithm)) {
            return RSAES_PKCS1_V1_5;
        }
        throw new JweException("Unknown Encryption Algorithm, " + algorithm);
    }

    /**
     * Turns the JweAlgorithm constant into a JSON value string.
     *
     * @return {@inheritDoc}
     */
    @Override
    public String toString() {
        return name;
    }
}
