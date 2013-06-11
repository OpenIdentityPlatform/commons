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

package org.forgerock.json.jose.jwe;

import org.forgerock.json.jose.jwt.Algorithm;

public enum JweAlgorithm implements Algorithm {

    RSAES_PKCS1_V1_5("RSA/ECB/PKCS1Padding", JweAlgorithmType.RSA);

    private final String transformation;
    private final JweAlgorithmType algorithmType;

    private JweAlgorithm(String transformation, JweAlgorithmType algorithmType) {
        this.transformation = transformation;
        this.algorithmType = algorithmType;
    }

    @Override
    public String getAlgorithm() {
        return transformation;
    }

    public JweAlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
