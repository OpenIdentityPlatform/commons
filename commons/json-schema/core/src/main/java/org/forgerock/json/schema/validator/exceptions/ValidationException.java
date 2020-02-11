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
import org.forgerock.json.JsonPointer;

/**
 * Encapsulate a JSON validator error.
 * <p/>
 * <p>This exception may include information for locating the error
 * in the original JSON document object.  Note that although the application
 * will receive a ValidationException as the argument to the handlers
 * in the {@link org.forgerock.json.schema.validator.ErrorHandler ErrorHandler} interface,
 * the application is not actually required to throw the exception;
 * instead, it can simply read the information in it and take a
 * different action.</p>
 * <p/>
 * <p>Since this exception is a subclass of {@link SchemaException
 * SchemaException}, it inherits the ability to wrap another exception.</p>
 *
 * @see SchemaException
 */
public class ValidationException extends SchemaException {

    private static final long serialVersionUID = 3267297114570206794L;

    /**
     * Create an exception with the given message.
     * @param string The message.
     */
    public ValidationException(String string) {
        super(null, string);
    }

    /**
     * Create an exception with the given message and cause.
     * @param string The message.
     * @param throwable The cause.
     */
    public ValidationException(String string, Throwable throwable) {
        super(null, string, throwable);
    }

    /**
     * Create an exception with the given message, cause and path.
     * @param string The message.
     * @param throwable The cause.
     * @param path The path.
     */
    public ValidationException(String string, Throwable throwable, JsonPointer path) {
        super(new JsonValue(null, path), string, throwable);
    }

    /**
     * Create an exception with the given message and path.
     * @param message The message.
     * @param path The path.
     */
    public ValidationException(String message, JsonPointer path) {
        super(new JsonValue(null, path), message);
    }

    /**
     * Create an exception with the given message, value and path.
     * @param message The message.
     * @param path The path.
     * @param value The value.
     */
    public ValidationException(String message, JsonPointer path, Object value) {
        super(new JsonValue(value, path), message);
    }

    /**
     * Create an exception with the given cause and path.
     * @param e The cause.
     * @param path The path.
     */
    public ValidationException(Exception e, JsonPointer path) {
        super(new JsonValue(null, path), e);
    }

    /**
     * Create an exception with the given message, cause and path.
     * @param message The message.
     * @param e The cause.
     * @param path The path.
     */
    public ValidationException(String message, Exception e, JsonPointer path) {
        super(new JsonValue(null, path), message, e);
    }
}
