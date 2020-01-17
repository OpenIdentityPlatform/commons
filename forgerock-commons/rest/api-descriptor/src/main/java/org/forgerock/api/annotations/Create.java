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

import org.forgerock.api.enums.CreateMode;
import org.forgerock.api.enums.CreateSingleton;

/**
 * Indicates an CREST create method on an annotated POJO. This annotation can only be used on
 * collection resource request handlers.
 * <p>
 * The annotated method's return type must be:
 * <ul>
 *     <li>A {@code Promise<JsonValue, ? extends ResourceException>} promise.</li>
 * </ul>
 * The method must take the following parameters:
 * <ul>
 *     <li>A {@code org.forgerock.json.resource.CreateRequest} for the request.</li>
 * </ul>
 * The method may also take the following parameters:
 * <ul>
 *     <li>A {@link org.forgerock.services.context.Context} to be given the context.</li>
 * </ul>
 * @see RequestHandler
 * @see SingletonProvider
 * @see CollectionProvider
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Create {
    /** Describe the standard operation details of this action. */
    Operation operationDescription();
    /**
     * Specify the types of create request that are supported. By default, both ID_FROM_CLIENT (POST-to-collection type)
     * and ID_FROM_SERVER (PUT-to-instance type) are supported.
     */
    CreateMode[] modes() default { CreateMode.ID_FROM_CLIENT, CreateMode.ID_FROM_SERVER };
    /**
     * Specify whether or not the created resource is a singleton or one of a collection. By default this will be
     * worked out from the context, and should rarely have to be set to anything different.
     */
    CreateSingleton singleton() default CreateSingleton.FROM_CONTEXT;
}
