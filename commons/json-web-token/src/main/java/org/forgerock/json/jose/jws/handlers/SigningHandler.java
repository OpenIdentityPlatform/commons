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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.json.jose.jws.handlers;

import org.forgerock.json.jose.jws.JwsAlgorithm;

/**
 * The interface for SigningHandlers for all the different signing algorithms.
 * <p>
 * Provides methods for signing data and verifying the signatures of data.
 *
 * @since 2.0.0
 */
public interface SigningHandler {

    /**
     * Signs the given String data using the Java Cryptographic algorithm defined by the JwsAlgorithm.
     * The signature is created using the given private key.
     *
     * @param algorithm The JwsAlgorithm defining the Java Cryptographic algorithm.
     * @param data The data to be signed.
     * @return A byte array of the signature.
     */
    byte[] sign(JwsAlgorithm algorithm, String data);

    /**
     * Signs the given raw data bytes using the Java Cryptographic algorithm defined by the JwsAlgorithm.
     *
     * @param algorithm the JWS signature algorithm to use.
     * @param data the raw data to sign.
     * @return the signature.
     */
    byte[] sign(JwsAlgorithm algorithm, byte[] data);

    /**
     * Verifies that the given signature is valid for the given data.
     * <p>
     * Uses the Java Cryptographic algorithm defined by the JwsAlgorithm and private key to create a new signature
     * of the data to compare against the given signature to see if they are identical.
     *
     * @param algorithm The JwsAlgorithm defining the JavaCryptographic algorithm.
     * @param data The data that was signed.
     * @param signature The signature of the data.
     * @return <code>true</code> if the signature is a valid signature of the data.
     */
    boolean verify(JwsAlgorithm algorithm, byte[] data, byte[] signature);
}
