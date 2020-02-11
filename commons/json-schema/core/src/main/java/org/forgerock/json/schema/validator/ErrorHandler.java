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

import org.forgerock.json.schema.validator.exceptions.SchemaException;
import org.forgerock.json.schema.validator.exceptions.ValidationException;

/**
 * ErrorHandler is the abstract base class for Validators.
 * <p>
 * If a Validator application needs to implementation of customized error
 * handling, it must implement this class.
 * <p/>
 * Use this handler when call the
 * {@link org.forgerock.json.schema.validator.validators.SimpleValidator#validate(Object,
 * org.forgerock.json.JsonPointer, ErrorHandler)}}
 * method.  The helpers will then report all errors.</p>
 */
public abstract class ErrorHandler {

    private boolean hasError = false;

    /**
     * Process the <code>exception</code> of the Validator.
     * <p/>
     * None of the implementations of {Validator#validate} method throws exception. They call
     * this method to decide what to do if an exception occurs.
     *
     * @param exception the exception that the validator wants to handled
     * @throws SchemaException when the implementation re-throws the <code>exception</code>
     */
    final void handleError(ValidationException exception) throws SchemaException {
        this.hasError = true;
        error(exception);
    }

    /**
     * Receive notification of an error.
     * <p/>
     * <p>For example, a validator would use this callback to
     * report the violation of a validity constraint.
     * The default behaviour is to take no action.</p>
     * <p/>
     * <p>The validator must continue to provide normal validation
     * after invoking this method: it should still be possible
     * for the application to process the document through to the end.
     * If the application cannot do so, then the parser should report
     * a fatal error.</p>
     * <p/>
     * <p>Filters may use this method to report other, non-JSON errors
     * as well.</p>
     *
     * @param exception The error information encapsulated in a
     *                  validation exception.
     * @throws SchemaException Any JSON exception, possibly
     *                             wrapping another exception.
     */
    public abstract void error(ValidationException exception)
            throws SchemaException;

    /**
     * Get the final result of the validation.
     * <p/>
     * The default value is <code>false</code>. If the validator has called the {#handleError} method
     * then it return <code>true</code>.
     *
     * @return true if there was an error during the validation process.
     */
    public boolean hasError() {
        return hasError;
    }

    ///////////////////////////////////////////////////////////////////////////////

    /**
     * Throws the exception if it has an error.
     * @deprecated
     */
    @Deprecated
    final void throwException() throws ValidationException {
        if (hasError) {
            assembleException();
        }
    }

    /**
     * Throws an assembled exception after the validator finished the processing.
     * <p/>
     * Implementation of this method MUST throw an Exception if the {#error()} method
     * was called on this instance before.
     *
     * @throws ValidationException when this instance wraps an error message(s).
     * @deprecated
     */
    @Deprecated
    public abstract void assembleException()
            throws ValidationException;
}
