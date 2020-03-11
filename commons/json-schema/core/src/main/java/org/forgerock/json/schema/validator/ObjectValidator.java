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

import org.forgerock.json.schema.validator.validators.Validator;

import java.util.Map;

/**
 * The ObjectValidator is a sample implementation of how to use the validator.
 * <p/>
 * The validators was designed to keep in the memory or other cache and validate multiple instances.
 * This implementation creates a new {@link Validator} each time
 * and uses the {@link FailFastErrorHandler} to validate the instance object.
 */
public final class ObjectValidator {
    /**
     * Validate the given <code>node</code> against the <code>schema</code>
     * <p/>
     * This implementation is pessimistic and returns false if any exception was thrown.
     *
     * @param node   instance to validate
     * @param schema schema for validation
     * @return true if the object does not violates the schema otherwise false
     */
    public static boolean validate(Object node, Map<String, Object> schema) {
        boolean isValid = true;

        try {
            Validator v = ObjectValidatorFactory.getTypeValidator(schema);
            ErrorHandler handler = new FailFastErrorHandler();
            v.validate(node, null, handler);
        } catch (Throwable e) {
            isValid = false;
        }
        return isValid;
    }

    private ObjectValidator() {
        // utility class
    }
}
