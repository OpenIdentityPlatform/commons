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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2011-2015 ForgeRock AS.
 */

package org.forgerock.json.resource;

/**
 * An exception that indicates that a failure is permanent, i.e. that re-trying
 * alone without addressing the cause is not expected to succeed.
 *
 * @see RetryableException for failures that are temporary instead.
 */
public class PermanentException extends ResourceException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param code
     *            The numeric code of the exception.
     * @param message
     *            The detail message.
     * @param cause
     *            The exception which caused this exception to be thrown.
     */
    public PermanentException(final int code, final String message, final Throwable cause) {
        super(code, message, cause);
    }
}
