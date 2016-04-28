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

package org.forgerock.json.schema.validator.validators;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.schema.validator.ErrorHandler;
import org.forgerock.json.schema.validator.exceptions.SchemaException;

/**
 * SimpleValidator is a base interface for all validator implementation.
 * @param <T> The type of node that will be validated.
 */
public interface SimpleValidator<T> {
    /**
     * Validates the <code>node</code> value against the embedded schema object.
     * <p/>
     * The selected error handler defines the behaviour of the validator. The
     * {@link org.forgerock.json.schema.validator.FailFastErrorHandler} throws exception at firs violation.
     * Other customised {@link ErrorHandler} can collect all exceptions and after the validation the
     * examination of the <code>handler</code> contains the final result.
     *
     * @param node      value to validate
     * @param at        JSONPath of the node. null means it's the root node
     * @param handler   customised error handler like {@link org.forgerock.json.schema.validator.FailFastErrorHandler}
     * @throws SchemaException when the <code>node</code> violates with the schema
     */
    public void validate(T node, JsonPointer at, ErrorHandler handler) throws SchemaException;
}
