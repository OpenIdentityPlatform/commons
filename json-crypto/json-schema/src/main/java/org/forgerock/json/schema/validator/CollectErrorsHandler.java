/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2013 ForgeRock AS. All rights reserved.
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
package org.forgerock.json.schema.validator;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.json.schema.validator.exceptions.SchemaException;
import org.forgerock.json.schema.validator.exceptions.ValidationException;

/**
 * The CollectErrorsHandler implements the {@link ErrorHandler} and never throws
 * any exception, but collects them so callers can retrieve all of them in one
 * go.
 */
public class CollectErrorsHandler extends ErrorHandler {

    private List<ValidationException> exceptions = new ArrayList<ValidationException>();

    /** {@inheritDoc} */
    @Override
    public void error(ValidationException exception) throws SchemaException {
        exceptions.add(exception);
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public void assembleException() throws ValidationException {
        if (!exceptions.isEmpty()) {
            throw exceptions.get(0);
        }
    }

    /**
     * Returns the collected {@link ValidationException}s.
     *
     * @return the collected {@link ValidationException}s
     */
    public List<ValidationException> getExceptions() {
        return exceptions;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasError() {
        return !this.exceptions.isEmpty();
    }
}
