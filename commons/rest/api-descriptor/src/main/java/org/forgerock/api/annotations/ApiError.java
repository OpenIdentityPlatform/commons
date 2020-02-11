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

/**
 * Details of an error that could be returned.
 */
public @interface ApiError {
    /**
     * An identifier for this error condition. If specified, this error will be defined at the top-level and
     * referenced when used. If not specified, this error will be declared inline.
     */
    String id() default "";
    /** The error code that will be returned in this situation. */
    int code();
    /** A description of the error condition, and what may have caused it. */
    String description();
    /** The schema for the error detail, if relevant. */
    Schema detailSchema() default @Schema;
}
