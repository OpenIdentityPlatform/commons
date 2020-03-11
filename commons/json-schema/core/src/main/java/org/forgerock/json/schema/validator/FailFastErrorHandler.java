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

package org.forgerock.json.schema.validator;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.schema.validator.exceptions.SchemaException;
import org.forgerock.json.schema.validator.exceptions.ValidationException;

/**
 * FailFastErrorHandler implements the {@link ErrorHandler} in a way it re-throws the exception
 * at first time.
 * <p/>
 * The exception prevents the validator to continue the validation of an already invalid object.
 */
public class FailFastErrorHandler extends ErrorHandler {

    private SchemaException ex = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public void error(ValidationException exception) throws SchemaException {
        this.ex = exception;
        throw ex;
    }

    /**
     * Wrap in a {@link ValidationException} if not already of that type.
     * @throws ValidationException when there is any error wrapped inside the handler.
     */
    @Override
    @Deprecated
    public void assembleException() throws ValidationException {
        if (ex instanceof ValidationException) {
            throw (ValidationException) ex;
        } else if (null != ex) {
            throw new ValidationException(ex, new JsonPointer());
        }
    }
}
