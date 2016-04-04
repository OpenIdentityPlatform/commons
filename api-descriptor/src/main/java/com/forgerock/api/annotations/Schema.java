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

package com.forgerock.api.annotations;

/**
 * Specify a schema for the element that is being described.
 */
public @interface Schema {

    /**
     * The schema identifier. If provided, this schema will be added to definitions and referenced from
     * anywhere it is used. If null/empty, the schema will be used inline.
     */
    String id() default "";
    /** The type to produce the schema from. */
    Class<?> fromType() default Void.class;
    /** A classpath resource that contains a JSON Schema structure to be used. */
    String schemaResource() default "";

}
