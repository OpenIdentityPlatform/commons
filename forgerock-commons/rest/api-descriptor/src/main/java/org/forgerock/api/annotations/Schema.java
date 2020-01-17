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
 * Specify a schema for the element that is being described.
 * <p>
 * This annotation can also be used to specify an {@code id} for a schema defined by a type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Schema {

    /**
     * The schema identifier. If provided, this schema will be added to definitions and referenced from
     * anywhere it is used. If null/empty, the schema will be used inline.
     */
    String id() default "";
    /** The type to produce the schema from. */
    Class<?> fromType() default Void.class;
    /**
     * A classpath resource that contains a JSON Schema structure to be used. The path is relative to the type
     * that the annotated resource method is part of, and will be resolved using the
     * {@link Class#getResourceAsStream(String)} method on that class instance.
     */
    String schemaResource() default "";

}
