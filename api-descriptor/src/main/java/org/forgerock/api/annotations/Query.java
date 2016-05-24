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

import org.forgerock.api.enums.CountPolicy;
import org.forgerock.api.enums.PagingMode;
import org.forgerock.api.enums.QueryType;

/**
 * Indicates an CREST query method on an annotated POJO. This annotation can only be used on
 * collection resource request handlers.
 * <p>
 * The annotated method's return type must be:
 * <ul>
 *     <li>A {@code Promise<QueryResponse, ? extends ResourceException>} promise.</li>
 * </ul>
 * The method must take the following parameters:
 * <ul>
 *     <li>A {@code org.forgerock.json.resource.QueryResourceHandler} for the results of the query.</li>
 *     <li>A {@code org.forgerock.json.resource.QueryRequest} for the request.</li>
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
public @interface Query {
    /** Describe the standard operation details of this action. */
    Operation operationDescription();
    /** The type of query this method supports. */
    QueryType type();
    /** The paging modes that can be used with this query. */
    PagingMode[] pagingModes() default {};
    /**
     * The count policies that can be used with this query.
     * If the array is empty, this means that the query does not support any form of count policy,
     * and no value for count policy should be specified
     */
    CountPolicy[] countPolicies() default {};
    /**
     * The query ID - required only when {@code type} is {@code ID}. If not supplied but required, the name of the
     * annotated method is used.
     */
    String id() default "";
    /** The set of fields that can be used in a query filter. Required only when the {@code type} is {@code FILTER}. */
    String[] queryableFields() default {};
    /** The keys that can be used to sort the results of the query. */
    String[] sortKeys() default {};
}
