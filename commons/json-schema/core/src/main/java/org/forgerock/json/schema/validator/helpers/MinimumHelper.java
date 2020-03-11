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

package org.forgerock.json.schema.validator.helpers;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.schema.validator.ErrorHandler;
import org.forgerock.json.schema.validator.exceptions.SchemaException;
import org.forgerock.json.schema.validator.exceptions.ValidationException;
import org.forgerock.json.schema.validator.validators.SimpleValidator;

import java.lang.reflect.Method;

/**
 * Helper compares two {@link Number}s to check the minimum constraint.
 *
 * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.9">minimum</a>
 */
public class MinimumHelper implements SimpleValidator<Number> {

    /**
     * This attribute defines the minimum value of the instance property
     * when the validators of the instance value is a number.
     */
    private final Number minimum;
    /**
     * This attribute indicates if the value of the instance (if the
     * instance is a number) can not equal the number defined by the
     * "minimum" attribute.  This is false by default, meaning the instance
     * value can be greater then or equal to the minimum value.
     */
    private int exclusiveMinimum = 0;

    /**
     * Create a minimum helper.
     * @param minimum The minimum.
     * @param exclusiveMinimum Whether it is an exclusive minimum.
     */
    public MinimumHelper(Number minimum, boolean exclusiveMinimum) {
        this.minimum = minimum;
        this.exclusiveMinimum = exclusiveMinimum ? -1 : 0;
    }

    @Override
    public void validate(Number node, JsonPointer at, ErrorHandler handler) throws SchemaException {

        if (minimum.getClass().isAssignableFrom(node.getClass())) {
            try {
                Method method = minimum.getClass().getMethod("compareTo", minimum.getClass());
                method.invoke(minimum, node);
                if ((Integer) method.invoke(minimum, node) > exclusiveMinimum) {
                    handler.error(new ValidationException("minimum violation", at));
                }
            } catch (Exception e) {
                handler.error(new ValidationException("Reflection exception at \"compareTo\" method invocation." , e,
                        at));
            }
        } else {
            if (minimum instanceof Float) {
                if (((Float) minimum).compareTo(node.floatValue()) > exclusiveMinimum) {
                    handler.error(new ValidationException("minimum violation", at));
                }
            } else if (minimum instanceof Double) {
                if (((Double) minimum).compareTo(node.doubleValue()) > exclusiveMinimum) {
                    handler.error(new ValidationException("minimum violation", at));
                }
            } else if (minimum instanceof Integer) {
                if (((Integer) minimum).compareTo(node.intValue()) > exclusiveMinimum) {
                    handler.error(new ValidationException("minimum violation", at));
                }
            } else if (minimum instanceof Long) {
                if (((Long) minimum).compareTo(node.longValue()) > exclusiveMinimum) {
                    handler.error(new ValidationException("minimum violation", at));
                }
            }
        }
    }
}
