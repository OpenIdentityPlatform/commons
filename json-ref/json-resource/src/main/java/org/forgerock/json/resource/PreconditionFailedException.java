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
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2011-2013 ForgeRock AS.
 */

package org.forgerock.json.resource;

/**
 * An exception that is thrown to indicate that a resource's current version
 * does not match the version provided. Equivalent to HTTP status: 412
 * Precondition Failed.
 */
public class PreconditionFailedException extends ResourceException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with {@code null} as its detail message.
     */
    public PreconditionFailedException() {
        super(ResourceException.VERSION_MISMATCH);
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message
     *            The detail message.
     */
    public PreconditionFailedException(final String message) {
        super(ResourceException.VERSION_MISMATCH, message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message
     *            The detail message.
     * @param cause
     *            The exception which caused this exception to be thrown.
     */
    public PreconditionFailedException(final String message, final Throwable cause) {
        super(ResourceException.VERSION_MISMATCH, message, cause);
    }

    /**
     * Constructs a new exception with the specified cause.
     *
     * @param cause
     *            The exception which caused this exception to be thrown.
     */
    public PreconditionFailedException(final Throwable cause) {
        super(ResourceException.VERSION_MISMATCH, cause);
    }
}
