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

/**
 * An Enum of the possible types of JWS algorithms that can be used to sign a JWT.
 * @see JwsAlgorithm
 *
 * @since 2.0.0
 */
public enum JwsAlgorithmType {

    /** No digital signature or MAC value included. */
    NONE,
    /** HMAC signing algorithm. */
    HMAC,
    /** RSA signing algorithm. */
    RSA,
    /** Elliptic Curve DSA signing algorithm. */
    ECDSA;

    /**
     * Turns the JwsAlgorithmType constant into a JSON value string.
     *
     * @return {@inheritDoc}
     */
    @Override
    public String toString() {
        return '"' + super.toString() + '"';
    }
}
