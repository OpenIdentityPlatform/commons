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
 * Details of a handler.
 */
public @interface Handler {
    /**
     * The service identifier.
     * <br>
     * If a value is provided this indicates to the API Descriptor framework that you intend to reuse
     * the same service definition multiple times, and that it should define the service globally
     * and bind it to paths by reference instead of by value.
     * <br>
     * Example:
     *
     * <code>
     *   <pre>
     * "services": {
     *   "users:1.0": {
     *     "type": "collection",
     *     "resourceSchema": {
     *       "$ref": "#/definitions/user"
     *     },
     *     ...
     *   }
     * }
     * "paths": {
     *   "/users": {
     *     "1.0": {
     *       "$ref": "#/services/users:1.0"
     *     },
     *   }
     * }
     *   </pre>
     * </code>
     */
    String id() default "";

    /**
     * The schema for all the standard resource operations (CRUDPQ) on this endpoint. Only required if one or more
     * of those operations are supported.
     */
    Schema resourceSchema() default @Schema;

    /** Whether MVCC style requests are supported. */
    boolean mvccSupported();

    /** Service title, for documentation purposes. */
    String title() default "";

    /** Service description, for documentation purposes. */
    String description() default "";

    /** Parameters on service paths and/or endpoints. */
    Parameter[] parameters() default {};
}
