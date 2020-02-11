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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define JSON Schema {@code additionalProperties}, which is useful when working with key/value
 * JSON data structures.
 * <p>
 * For example, the following JSON Schema defines a map from string (key) to string (value),
 * </p>
 * <pre>
 * {
 *   "type": "object",
 *   "additionalProperties": {
 *     "type": "string"
 *   }
 * }
 * </pre>
 * Note that keys are always strings in JSON, so the schema does not define that fact.
 * <p>
 * The annotation in this example would be used as follows,
 * </p>
 * <pre>
 * &#064;AdditionalProperties(String.class)
 * private static class MyMap extends HashMap<String, String> {}
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface AdditionalProperties {
    /**
     * The type to produce the additional-properties schema from.
     */
    Class<?> value() default Void.class;
}
