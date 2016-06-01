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

import org.forgerock.api.enums.ParameterSource;

/**
 * A extra parameter to an operation.
 */
public @interface Parameter {
    /** The name of the parameter. */
    String name();
    /** The type semantics of the String value. */
    String type();
    /** The default value, if applicable. Should not be an empty string. */
    String defaultValue() default "";
    /** A description of the parameter. */
    String description() default "";
    /** The source of the parameter. Defaults to {@code PATH}. */
    ParameterSource source() default ParameterSource.PATH;
    /** Whether the parameter is required. Defaults to {@code true}. */
    boolean required() default true;
    /** Enumeration of acceptable values, if required. */
    String[] enumValues() default {};
    /** Titles of enumeration values. */
    String[] enumTitles() default {};
}
