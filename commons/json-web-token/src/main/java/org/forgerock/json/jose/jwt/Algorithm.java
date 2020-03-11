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

package org.forgerock.json.jose.jwt;

/**
 * The interface for each possible algorithm that can be used to sign and/or encrypt a JWT.
 * <p>
 * @see <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-algorithms-11">
 *     JSON Web Algorithms Specification</a>
 *
 * @since 2.0.0
 */
public interface Algorithm {

    /**
     * Gets the actual name of the algorithm that is understood by Java cryptographic operations.
     *
     * @return The name of the algorithm.
     */
    String getAlgorithm();
}
