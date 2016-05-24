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
 * A marker annotation to indicate that the annotated class should be interpreted as an annotated CREST
 * collection provider resource.
 * <p>
 * Individual operations can then be supported by either annotating a method with the relevant annotation, or
 * by naming the method according to the following convention:
 * <ul>
 *     <li>{@code create} for create methods.</li>
 *     <li>{@code read} for read methods.</li>
 *     <li>{@code update} for update methods.</li>
 *     <li>{@code delete} for delete methods.</li>
 *     <li>{@code patch} for patch methods.</li>
 *     <li>{@code query} for generic query methods.</li>
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
 * @see Queries
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CollectionProvider {
    /** The name of the path parameter to use in the path descriptors. */
    Parameter pathParam() default @Parameter(name = "id", type = "string");
    /** The details of the handler. */
    Handler details();
}
