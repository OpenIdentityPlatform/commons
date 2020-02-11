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
 * Helper compares two {@link Number}s to check the maximum constraint.
 *
 * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.10">maximum</a>
 */
public class MaximumHelper implements SimpleValidator<Number> {
    /**
     * This attribute defines the maximum value of the instance property
     * when the validators of the instance value is a number.
     */
    private final Number maximum;

    /**
     * This attribute indicates if the value of the instance (if the
     * instance is a number) can not equal the number defined by the
     * "maximum" attribute.  This is false by default, meaning the instance
     * value can be less then or equal to the maximum value.
     */
    private int exclusiveMaximum = 0;

    /**
     * Create a maximum helper.
     * @param maximum The maximum.
     * @param exclusiveMaximum Whether it is an exclusive maximum.
     */
    public MaximumHelper(Number maximum, boolean exclusiveMaximum) {
        this.maximum = maximum;
        this.exclusiveMaximum = exclusiveMaximum ? 1 : 0;
    }

    @Override
    public void validate(Number node, JsonPointer at, ErrorHandler handler) throws SchemaException {
        if (maximum.getClass().isAssignableFrom(node.getClass())) {
            try {
                Method method = maximum.getClass().getMethod("compareTo", maximum.getClass());
                method.invoke(maximum, node);
                if ((Integer) method.invoke(maximum, node) < exclusiveMaximum) {
                    handler.error(new ValidationException("minimum violation", at));
                }
            } catch (Exception e) {
                handler.error(new ValidationException("Reflection exception at \"compareTo\" method invocation." , e,
                        at));
            }
        } else {
            if (maximum instanceof Float) {
                if (((Float) maximum).compareTo(node.floatValue()) < exclusiveMaximum) {
                    handler.error(new ValidationException("maximum violation", at));
                }
            } else if (maximum instanceof Double) {
                if (((Double) maximum).compareTo(node.doubleValue()) < exclusiveMaximum) {
                    handler.error(new ValidationException("maximum violation", at));
                }
            } else if (maximum instanceof Integer) {
                if (((Integer) maximum).compareTo(node.intValue()) < exclusiveMaximum) {
                    handler.error(new ValidationException("maximum violation", at));
                }
            }  else if (maximum instanceof Long) {
                if (((Long) maximum).compareTo(node.longValue()) < exclusiveMaximum) {
                    handler.error(new ValidationException("maximum violation", at));
                }
            }
        }
    }
}
