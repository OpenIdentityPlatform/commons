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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.forgerock.api.enums.HandlerVariant;

/**
 * A marker annotation to indicate that the annotated class should be interpreted as an annotated POJO CREST
 * resource.
 * <p>
 * Individual operations can then be supported by either annotating a method with the relevant annotation, or
 * by naming the method according to the following convention:
 * <ul>
 *     <li>{@code create} for create methods.</li>
 *     <li>{@code read} for read methods.</li>
 *     <li>{@code update} for update methods.</li>
 *     <li>{@code delete} for delete methods.</li>
 *     <li>{@code patch} for patch methods.</li>
 *     <li>{@code query} for query methods.</li>
 * </ul>
 * Note that action methods do not have a convention and MUST be annotated using the {@link Action} annotation.
 *
 * @see Create
 * @see Read
 * @see Update
 * @see Delete
 * @see Patch
 * @see Action
 * @see Query
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RequestHandler {

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

    /** The variant of request handler. */
    HandlerVariant variant();
}
