/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * $Id$
 */
package org.forgerock.json.schema.validator.exceptions;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonPointer;

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
 * @author $author$
 * @version $Revision$ $Date$
 * @see SchemaException
 */
public class ValidationException extends SchemaException {

    private static final long serialVersionUID = 3267297114570206794L;

    public ValidationException(String string) {
        super(null, string);
    }

    public ValidationException(String string, Throwable throwable) {
        super(null, string, throwable);
    }

    public ValidationException(String string, Throwable throwable, JsonPointer path) {
        super(new JsonValue(null, path), string, throwable);
    }

    public ValidationException(String message, JsonPointer path) {
        super(new JsonValue(null, path), message);
    }

    public ValidationException(String message, JsonPointer path, Object value) {
        super(new JsonValue(value, path), message);
    }

    public ValidationException(Exception e, JsonPointer path) {
        super(new JsonValue(null, path), e);
    }

    public ValidationException(String message, Exception e, JsonPointer path) {
        super(new JsonValue(null, path), message, e);
    }
}
