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

/**
 * An Enum of the possible types of JWE algorithms that can be used to encrypt a JWT.
 * @see JweAlgorithm
 *
 * @since 2.0.0
 */
public enum JweAlgorithmType {

    /** RSA encryption algorithm. */
    RSA,
    /** Direct symmetric encryption. */
    DIRECT,
    /** AES KeyWrap. */
    AES_KEYWRAP;

    /**
     * Turns the JweAlgorithmType constant into a JSON value string.
     *
     * @return {@inheritDoc}
     */
    @Override
    public String toString() {
        return super.toString();
    }
}
