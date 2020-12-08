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

import org.forgerock.json.jose.jwt.Algorithm;

/**
 * An Enum of the possible signing algorithms that can be used to sign a JWT.
 * <p>
 * @see <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-algorithms-11#section-3.1">JWS Algorithms</a>
 *
 * @since 2.0.0
 */
public enum JwsAlgorithm implements Algorithm {

    /** No digital signature or MAC value included. */
    NONE(null, null, JwsAlgorithmType.NONE),
    /** HMAC using SHA-256 hash algorithm. */
    HS256("HmacSHA256", "SHA-256", JwsAlgorithmType.HMAC),
    /** HMAC using SHA-384 hash algorithm. */
    HS384("HmacSHA384", "SHA-384", JwsAlgorithmType.HMAC),
    /** HMAC using SHA-512 hash algorithm. */
    HS512("HmacSHA512", "SHA-512", JwsAlgorithmType.HMAC),
    /** RSA using SHA-256 hash algorithm. **/
    RS256("SHA256withRSA", "SHA-256", JwsAlgorithmType.RSA),
    /** RSA using SHA-384 hash algorithm. **/
    RS384("SHA384withRSA", "SHA-384", JwsAlgorithmType.RSA),
    /** RSA using SHA-256 hash algorithm. **/
    RS512("SHA512withRSA", "SHA-512", JwsAlgorithmType.RSA),
    /** ECDSA using SHA-256 hash algorithm. */
    ES256("SHA256WithECDSA", "SHA-256", JwsAlgorithmType.ECDSA),
    /** ECDSA using SHA-384 hash algorithm. */
    ES384("SHA384WithECDSA", "SHA-384", JwsAlgorithmType.ECDSA),
    /** ECDSA using SHA-512 hash algorithm. */
    ES512("SHA512WithECDSA", "SHA-512", JwsAlgorithmType.ECDSA);

    private final String algorithm;
    private final String mdAlgorithm;
    private final JwsAlgorithmType algorithmType;

    /**
     * Constructs a new JwsAlgorithm with the Java Cryptographic string name of the algorithm and the JwsAlgorithmType
     * of the algorithm.
     *
     * @param algorithm The Java Cryptographic algorithm name.
     * @param mdAlgorithm The MessageDigest algorithm.
     * @param algorithmType The JwsAlgorithmType of the JwsAlgorithm.
     */
    JwsAlgorithm(String algorithm, String mdAlgorithm, JwsAlgorithmType algorithmType) {
        this.algorithm = algorithm;
        this.mdAlgorithm = mdAlgorithm;
        this.algorithmType = algorithmType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Returns the Java-friendly name of the message digest algorithm
     * implementation.
     *
     * @return the Java-friendly name of the message digest algorithm
     *         implementation.
     * @see <a
     *      href="http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html">Standard
     *      Names</a>
     */
    public String getMdAlgorithm() {
        return mdAlgorithm;
    }

    /**
     * Return the standard name of the elliptic curve definition. Only applicable for ECDSA algorithms.
     *
     * @return the curve name or null if not applicable.
     */
    public String getEllipticCurveName() {
        switch (this) {
        case ES256:
            return "P-256";
        case ES384:
            return "P-384";
        case ES512:
            return "P-521"; // Not a typo!
        default:
            return null;
        }
    }

    /**
     * Gets the JwsAlgorithmType of the JwsAlgorithm.
     *
     * @return The JwsAlgorithmType.
     */
    public JwsAlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    /**
     * Gets the JwsAlgorithm constant that matches the given Java Cryptographic algorithm name.
     * <p>
     * If the given algorithm name does not match the algorithm name of any of the constants, then an
     * IllegalArgumentException will be thrown.
     *
     * @param algorithm The Java Cryptographic string algorithm name.
     * @return The matching JwsAlgorithm.
     */
    public static JwsAlgorithm getJwsAlgorithm(String algorithm) {
        for (JwsAlgorithm jwsAlgorithm : JwsAlgorithm.values()) {
            if (algorithm.equalsIgnoreCase(jwsAlgorithm.getAlgorithm())) {
                return jwsAlgorithm;
            }
        }
        throw new IllegalArgumentException("Unknown JwsAlgorithm, " + algorithm);
    }

    /**
     * Turns the JwsAlgorithm constant into a JSON value string.
     *
     * @return {@inheritDoc}
     */
    @Override
    public String toString() {
        return super.toString();
    }
}
