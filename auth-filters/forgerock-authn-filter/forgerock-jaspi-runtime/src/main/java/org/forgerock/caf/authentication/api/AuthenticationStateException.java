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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.caf.authentication.api;

import org.forgerock.json.JsonValueException;

/**
 * An exception that is thrown during
 * {@link org.forgerock.caf.authentication.api.AuthenticationState} operations.
 *
 * @since 2.0.0
 */
public class AuthenticationStateException extends RuntimeException {

    private static final long serialVersionUID = -8084970886710807796L;

    /**
     * Constructs a new {@code AuthenticationStateException} with the specified cause.
     *
     * @param cause The cause.
     */
    AuthenticationStateException(JsonValueException cause) {
        super(cause);
    }
}
