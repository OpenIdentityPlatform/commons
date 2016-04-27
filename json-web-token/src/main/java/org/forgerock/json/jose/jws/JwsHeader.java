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

package org.forgerock.json.jose.jws;

import java.util.Map;

/**
 * An implementation for the JWS Header parameters.
 *
 * @since 2.0.0
 */
public class JwsHeader extends JwtSecureHeader {

    /**
     * Constructs a new, empty JwsHeader.
     */
    public JwsHeader() {
    }

    /**
     * Constructs a new JwsHeader, with its parameters set to the contents of the given Map.
     *
     * @param headerParameters A Map containing the parameters to be set in the header.
     */
    public JwsHeader(Map<String, Object> headerParameters) {
        super(headerParameters);
    }

    /**
     * Gets the Algorithm set in the JWT header.
     * <p>
     * If there is no algorithm set in the JWT header, then the JwsAlgorithm NONE will be returned.
     *
     * @return {@inheritDoc}
     */
    @Override
    public JwsAlgorithm getAlgorithm() {
        String algorithm = getAlgorithmString();
        if (algorithm == null) {
            return JwsAlgorithm.NONE;
        } else {
            return JwsAlgorithm.valueOf(algorithm);
        }
    }
}
