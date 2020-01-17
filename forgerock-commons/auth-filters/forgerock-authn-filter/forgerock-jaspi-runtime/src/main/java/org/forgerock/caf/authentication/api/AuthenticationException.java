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

package org.forgerock.caf.authentication.api;

import javax.security.auth.message.AuthException;

/**
 * A generic authentication exception which accepts a detail message and/or the cause.
 *
 * @since 2.0.0
 */
public class AuthenticationException extends AuthException {

    private static final long serialVersionUID = 8448867142875092521L;

    /**
     * Creates an {@code AuthenticationException} with the specified detail message.
     *
     * @param message The detail message.
     */
    public AuthenticationException(final String message) {
        super(message);
    }

    /**
     * Creates an {@code AuthenticationException} with the specified cause and a detail message of
     * {@code (cause==null ? null : cause.getMessage())}.
     *
     * @param cause The cause.
     */
    public AuthenticationException(final Throwable cause) {
        super(cause.getMessage());
        initCause(cause);
    }

    /**
     * Creates an {@code AuthenticationException} with the specified detail message and cause.
     *
     * @param message The detail message.
     * @param cause The cause.
     */
    public AuthenticationException(final String message, final Throwable cause) {
        super(message);
        initCause(cause);
    }
}
