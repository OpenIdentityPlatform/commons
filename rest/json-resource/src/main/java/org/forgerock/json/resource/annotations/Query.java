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
 * Indicates an CREST query method on a {@link RequestHandler}-annotated POJO. This annotation can only be used on
 * collection resource request handlers.
 * <p>
 * The annotated method's return type must be:
 * <ul>
 *     <li>A {@code Promise<QueryResult, ? extends ResourceException>} promise.</li>
 * </ul>
 * The method must take the following parameters:
 * <ul>
 *     <li>A {@link org.forgerock.json.resource.QueryResultHandler} for the results of the query.</li>
 *     <li>A {@link org.forgerock.json.resource.QueryRequest} for the request.</li>
 * </ul>
 * The method may also take the following parameters:
 * <ul>
 *     <li>A {@link org.forgerock.json.resource.ServerContext} to be given the context.</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Query {
}
