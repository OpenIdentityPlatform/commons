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

import org.forgerock.json.jose.jwt.Algorithm;

public enum JwsAlgorithm implements Algorithm {

    NONE(null, JwsAlgorithmType.NONE),
    HS256("HmacSHA256", JwsAlgorithmType.HMAC),
    HS384("HmacSHA384", JwsAlgorithmType.HMAC),
    HS512("HmacSHA512", JwsAlgorithmType.HMAC);

    private final String algorithm;
    private final JwsAlgorithmType algorithmType;

    private JwsAlgorithm(String algorithm, JwsAlgorithmType algorithmType) {
        this.algorithm = algorithm;
        this.algorithmType = algorithmType;
    }

    @Override
    public String getAlgorithm() {
        return algorithm;
    }

    public JwsAlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
