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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.authz.modules.oauth2;

/**
 * Represents an exception whilst accessing an OAuth2 access token info.
 *
 * @since 1.5.0
 */
public class RestResourceException extends Exception {

    /**
     * Serial Version UID.
     */
    public static final long serialVersionUID = -1L;

    private final StatusCode status;

    /**
     * Constructs a new RestResourceException with message.
     *
     * @param message
     *         The exception message.
     */
    public RestResourceException(final String message) {
        this(message, (StatusCode) null);
    }

    /**
     * Constructs a new RestResourceException with message and an HTTP status code.
     *
     * @param message
     *         The exception message.
     * @param status
     *         Http status code
     */
    public RestResourceException(final String message, final int status) {
        this(message, new StatusCode(status));
    }

    /**
     * Constructs a new RestResourceException with message and an HTTP status code.
     *
     * @param message
     *         The exception message.
     * @param status
     *         Http status code
     */
    public RestResourceException(final String message, final StatusCode status) {
        this(message, null, status);
    }

    /**
     * Constructs a new RestResourceException with message and an underlying cause.
     *
     * @param message
     *         The exception message.
     * @param cause
     *         The underlying cause.
     */
    public RestResourceException(final String message, final Throwable cause) {
        this(message, cause, null);
    }

    /**
     * Constructs a new RestResourceException with message, an underlying cause and an HTTP status code.
     *
     * @param message
     *         The exception message.
     * @param cause
     *         The underlying cause.
     * @param status
     *         Http status code
     */
    public RestResourceException(final String message, final Throwable cause, final int status) {
        this(message, cause, new StatusCode(status));
    }

    /**
     * Constructs a new RestResourceException with message, an underlying cause and an HTTP status code.
     *
     * @param message
     *         The exception message.
     * @param cause
     *         The underlying cause.
     * @param status
     *         Http status code
     */
    public RestResourceException(final String message, final Throwable cause, final StatusCode status) {
        super(message, cause);
        this.status = status;
    }

    /**
     * Returns the HTTP status code get whilst accessing the resource (maybe {@literal null} if the error cause did not
     * happen during an Http interaction).
     *
     * @return the HTTP status code get whilst accessing the resource.
     */
    public StatusCode getStatus() {
        return status;
    }

    /**
     * Represents an HTTP status code (usually an error status code because we're in an Exception).
     */
    public static class StatusCode {

        private final int code;

        /**
         * Builds a new StatusCode for the given HTTP status code.
         * @param code http status code
         */
        public StatusCode(final int code) {
            this.code = code;
        }

        /**
         * Returns the http status code.
         * @return the http status code
         */
        public int getCode() {
            return code;
        }
    }
}
