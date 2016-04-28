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
 * Copyright 2011-2016 ForgeRock AS.
 */

package org.forgerock.json.schema.validator.exceptions;

import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;

/**
 * Encapsulate a general JSON validator error.
 * <p>
 * If the validator needs to include information about a
 * specific location in an JSON document, it should use the
 * {@link org.forgerock.json.schema.validator.exceptions.ValidationException ValidationException} subclass.
 * </p>
 *
 * @see org.forgerock.json.schema.validator.exceptions.ValidationException
 */
public class SchemaException extends JsonValueException {
    private static final long serialVersionUID = -6791477138664072164L;

    /**
     * Create the exception with the given value and message.
     * @param value The value.
     * @param message The message.
     */
    public SchemaException(JsonValue value, String message) {
        super(value, message);
        this.exception = null;
    }

    /**
     * Create the exception with the given value, message and cause.
     * @param value The value.
     * @param message The message.
     * @param throwable The cause.
     */
    public SchemaException(JsonValue value, String message, Throwable throwable) {
        super(value, message, throwable);
    }


    /**
     * Create a new SchemaException wrapping an existing exception.
     * <p/>
     * <p>The existing exception will be embedded in the new
     * one, and its message will become the default message for
     * the SchemaException.</p>
     *
     * @param value The value.
     * @param e The exception to be wrapped in a SchemaException.
     */
    public SchemaException(JsonValue value, Exception e) {
        super(value);
        this.exception = e;
    }


    /**
     * Create a new SchemaException from an existing exception.
     * <p/>
     * <p>The existing exception will be embedded in the new
     * one, but the new exception will have its own message.</p>
     *
     * @param value The value.
     * @param message The detail message.
     * @param e       The exception to be wrapped in a SchemaException.
     */
    public SchemaException(JsonValue value, String message, Exception e) {
        super(value, message);
        this.exception = e;
    }

    /**
     * Return a detail message for this exception.
     * <p/>
     * <p>If there is an embedded exception, and if the SchemaException
     * has no detail message of its own, this method will return
     * the detail message from the embedded exception.</p>
     *
     * @return The error or warning message.
     */
    @Override
    public String getMessage() {
        String message = super.getMessage();

        if (message == null && exception != null) {
            return exception.getMessage();
        } else {
            return message;
        }
    }


    /**
     * Return the embedded exception, if any.
     *
     * @return The embedded exception, or null if there is none.
     */
    public Exception getException() {
        return exception;
    }


    /**
     * Override toString to pick up any embedded exception.
     *
     * @return A string representation of this exception.
     */
    @Override
    public String toString() {
        if (exception != null) {
            return exception.toString();
        } else {
            return super.toString();
        }
    }


    //////////////////////////////////////////////////////////////////////
    // Internal state.
    //////////////////////////////////////////////////////////////////////


    /**
     * The exception.
     * @serial The embedded exception if tunneling, or null.
     */
    private Exception exception;

}
