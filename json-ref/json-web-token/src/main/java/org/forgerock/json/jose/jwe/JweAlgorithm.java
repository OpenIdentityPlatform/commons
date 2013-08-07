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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.json.jose.jwe;

import org.forgerock.json.jose.jwt.Algorithm;

/**
 * An Enum of the possible encryption algorithms that can be used to encrypt a JWT.
 * <p>
 * @see <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-algorithms-11#section-4.1">JWE Algorithms</a>
 *
 * @author Phill Cunnington
 * @since 2.0.0
 */
public enum JweAlgorithm implements Algorithm {

    /** RSA in ECB mode with PKCS1 Padding. */
    RSAES_PKCS1_V1_5("RSA/ECB/PKCS1Padding", JweAlgorithmType.RSA);

    private final String transformation;
    private final JweAlgorithmType algorithmType;

    /**
     * Constructs a new JweAlgorithm with the Java Cryptographic string name of the algorithm and The JweAlgorithmType
     * of the algorithm.
     *
     * @param transformation The Java Cryptographic algorithm name
     * @param algorithmType The JweAlgorithmType of the JweAlgorithm.
     */
    private JweAlgorithm(String transformation, JweAlgorithmType algorithmType) {
        this.transformation = transformation;
        this.algorithmType = algorithmType;
    }

    /**
     * {@inheritDoc}
     */
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
     * Turns the JweAlgorithm constant into a JSON value string.
     *
     * @return {@inheritDoc}
     */
    @Override
    public String toString() {
        return super.toString();
    }
}
