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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.json.resource.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates an CREST action method on a {@link RequestHandler}-annotated POJO. This annotation can be used on
 * methods in both singleton and collection resource request handlers.
 * <p>
 * The annotated method's return type must be:
 * <ul>
 *     <li>A {@code Promise<JsonValue, ? extends ResourceException>} promise.</li>
 * </ul>
 * If the method is an instance action on a collection handler, it must take the following parameters:
 * <ul>
 *     <li>A string parameter for the instance identifier.</li>
 * </ul>
 * The method may also take the following parameters:
 * <ul>
 *     <li>A {@link org.forgerock.json.resource.ServerContext} to be given the context.</li>
 *     <li>A {@link org.forgerock.json.resource.ActionRequest} to be given the request.</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Action {
    /**
     * The name of the action being exposed. If not supplied, the name of the method is assumed to be the name
     * of the action.
     */
    String value() default "";
}
