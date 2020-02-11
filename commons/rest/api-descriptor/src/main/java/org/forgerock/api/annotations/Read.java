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

/**
 * Indicates an CREST read method on an annotated POJO. This annotation can be used on
 * methods in both singleton and collection resource request handlers.
 * <p>
 * The annotated method's return type must be:
 * <ul>
 *     <li>A {@code Promise<Resource, ? extends ResourceException>} promise.</li>
 * </ul>
 * If the annotated method is on a collection handler, it must take the following parameters:
 * <ul>
 *     <li>A string parameter for the instance identifier.</li>
 * </ul>
 * The method may also take the following parameters:
 * <ul>
 *     <li>A {@link org.forgerock.services.context.Context} to be given the context.</li>
 *     <li>A {@code org.forgerock.json.resource.ReadRequest} to be given the request.</li>
 * </ul>
 * @see RequestHandler
 * @see SingletonProvider
 * @see CollectionProvider
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Read {
    /** Describe the standard operation details of this action. */
    Operation operationDescription();
}
