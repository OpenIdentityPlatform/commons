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
 * Copyright © 2010–2011 ApexIdentity Inc. All rights reserved.
 * Portions Copyrighted 2011-2015 ForgeRock AS.
 */

package org.forgerock.json;

/**
 * An exception that is thrown during JSON value operations.
 */
public class JsonValueException extends JsonException {

    /** Serializable class a version number. */
    static final long serialVersionUID = 1L;

    /** The JSON value for which the exception was thrown. */
    private final JsonValue value;

    /**
     * Constructs a new exception with the specified JSON value and {@code null}
     * as its detail message.
     *
     * @param value
     *            The JSON value.
     */
    public JsonValueException(JsonValue value) {
        this.value = value;
    }

    /**
     * Constructs a new exception with the specified JSON value and detail
     * message.
     *
     * @param value
     *            The JSON value.
     * @param message
     *            The message.
     */
    public JsonValueException(JsonValue value, String message) {
        super(message);
        this.value = value;
    }

    /**
     * Constructs a new exception with the specified JSON value and cause.
     *
     * @param value
     *            The JSON value.
     * @param cause
     *            The cause.
     */
    public JsonValueException(JsonValue value, Throwable cause) {
        super(cause);
        this.value = value;
    }

    /**
     * Constructs a new exception with the specified JSON value, detail message
     * and cause.
     *
     * @param value
     *            The JSON value.
     * @param message
     *            The message.
     * @param cause
     *            The cause.
     */
    public JsonValueException(JsonValue value, String message, Throwable cause) {
        super(message, cause);
        this.value = value;
    }

    /**
     * Returns the detail message string of this exception.
     *
     * @return The detail message string of this exception.
     */
    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        String message = super.getMessage();
        if (value != null) {
            sb.append(value.getPointer().toString());
        }
        if (value != null && message != null) {
            sb.append(": ");
        }
        if (message != null) {
            sb.append(message);
        }
        return sb.toString();
    }

    /**
     * Returns the JSON value for which the exception was thrown.
     *
     * @return The JSON value for which the exception was thrown.
     */
    public JsonValue getJsonValue() {
        return value;
    }
}
