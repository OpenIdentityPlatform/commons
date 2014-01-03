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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.jaspi.exceptions;

import javax.security.auth.message.AuthException;

/**
 * A sub-type of an AuthException which can accept a cause for this exception.
 *
 * @since 1.3.0
 */
public class JaspiAuthException extends AuthException {

    private static final long serialVersionUID = -1L;

    private final Throwable cause;

    /**
     * Constructs a new JaspiAuthException with the specified detail message.
     *
     * @param message The detail message.
     */
    public JaspiAuthException(final String message) {
        super(message);
        this.cause = null;
    }

    /**
     * Constructs a new JaspiAuthException with the specified cause and a detail message of
     * (cause==null ? null : cause.getMessage()).
     *
     * @param cause The cause.
     */
    public JaspiAuthException(final Throwable cause) {
        super(cause.getMessage());
        this.cause = cause;
    }

    /**
     * Constructs a new JaspiAuthException with the specified detail message and cause.
     *
     * @param message The detail message.
     * @param cause The cause.
     */
    public JaspiAuthException(final String message, final Throwable cause) {
        super(message);
        this.cause = cause;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Throwable getCause() {
        return cause;
    }
}
