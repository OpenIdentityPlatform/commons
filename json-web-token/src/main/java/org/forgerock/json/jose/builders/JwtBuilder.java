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

package org.forgerock.json.jose.builders;

import org.forgerock.json.jose.jwt.Jwt;

/**
 * The base interface for all JwtBuilders for each type of JWT (plaintext, signed or encrypted).
 *
 * @since 2.0.0
 */
public interface JwtBuilder {

    /**
     * Builds the JWT object from its constituent parts.
     *
     * @return The Jwt.
     */
    Jwt asJwt();

    /**
     * Builds the JWT into a <code>String</code> by calling the <tt>build</tt> method on the JWT object.
     * <p>
     * @see org.forgerock.json.jose.jwt.Jwt#build()
     *
     * @return The base64url encoded UTF-8 parts of the JWT.
     */
    String build();
}
