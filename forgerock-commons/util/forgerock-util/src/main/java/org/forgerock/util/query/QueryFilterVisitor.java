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

package org.forgerock.util.query;

import java.util.List;

/**
 * A visitor of {@code QueryFilter}s, in the style of the visitor design
 * pattern.
 * <p>
 * Classes implementing this interface can query filters in a type-safe manner.
 * When a visitor is passed to a filter's accept method, the corresponding visit
 * method most applicable to that filter is invoked.
 *
 * @param <R>
 *            The return type of this visitor's methods. Use
 *            {@link java.lang.Void} for visitors that do not need to return
 *            results.
 * @param <P>
 *            The type of the additional parameter to this visitor's methods.
 *            Use {@link java.lang.Void} for visitors that do not need an
 *            additional parameter.
 * @param <F>
 *            The type of the field definitions in this visitor's methods.
 */
public interface QueryFilterVisitor<R, P, F> {

    /**
     * Visits an {@code and} filter.
     * <p>
     * <b>Implementation note</b>: for the purposes of matching, an empty
     * sub-filter list should always evaluate to {@code true}.
     *
     * @param p
     *            A visitor specified parameter.
     * @param subFilters
     *            The unmodifiable list of sub-filters.
     * @return Returns a visitor specified result.
     */
    R visitAndFilter(P p, List<QueryFilter<F>> subFilters);

    /**
     * Visits a boolean literal filter.
     *
     * @param p
     *            A visitor specified parameter.
     * @param value
     *            The boolean literal value.
     * @return Returns a visitor specified result.
     */
    R visitBooleanLiteralFilter(P p, boolean value);

    /**
     * Visits a {@code contains} filter.
     *
     * @param p
     *            A visitor specified parameter.
     * @param field
     *            A definition of the field to be compared.
     * @param valueAssertion
     *            The value assertion.
     * @return Returns a visitor specified result.
     */
    R visitContainsFilter(P p, F field, Object valueAssertion);

    /**
     * Visits a {@code equality} filter.
     *
     * @param p
     *            A visitor specified parameter.
     * @param field
     *            A definition of the field to be compared.
     * @param valueAssertion
     *            The value assertion.
     * @return Returns a visitor specified result.
     */
    R visitEqualsFilter(P p, F field, Object valueAssertion);

    /**
     * Visits a {@code comparison} filter.
     *
     * @param p
     *            A visitor specified parameter.
     * @param field
     *            A definition of the field to be compared.
     * @param operator
     *            The operator to use for the comparison, which will not be one of
     *            the core operator names.
     * @param valueAssertion
     *            The value assertion.
     * @return Returns a visitor specified result.
     */
    R visitExtendedMatchFilter(P p, F field, String operator, Object valueAssertion);

    /**
     * Visits a {@code greater than} filter.
     *
     * @param p
     *            A visitor specified parameter.
     * @param field
     *            A definition of the field to be compared.
     * @param valueAssertion
     *            The value assertion.
     * @return Returns a visitor specified result.
     */
    R visitGreaterThanFilter(P p, F field, Object valueAssertion);

    /**
     * Visits a {@code greater than or equal to} filter.
     *
     * @param p
     *            A visitor specified parameter.
     * @param field
     *            A definition of the field to be compared.
     * @param valueAssertion
     *            The value assertion.
     * @return Returns a visitor specified result.
     */
    R visitGreaterThanOrEqualToFilter(P p, F field, Object valueAssertion);

    /**
     * Visits a {@code less than} filter.
     *
     * @param p
     *            A visitor specified parameter.
     * @param field
     *            A definition of the field to be compared.
     * @param valueAssertion
     *            The value assertion.
     * @return Returns a visitor specified result.
     */
    R visitLessThanFilter(P p, F field, Object valueAssertion);

    /**
     * Visits a {@code less than or equal to} filter.
     *
     * @param p
     *            A visitor specified parameter.
     * @param field
     *            A definition of the field to be compared.
     * @param valueAssertion
     *            The value assertion.
     * @return Returns a visitor specified result.
     */
    R visitLessThanOrEqualToFilter(P p, F field, Object valueAssertion);

    /**
     * Visits a {@code not} filter.
     *
     * @param p
     *            A visitor specified parameter.
     * @param subFilter
     *            The sub-filter.
     * @return Returns a visitor specified result.
     */
    R visitNotFilter(P p, QueryFilter<F> subFilter);

    /**
     * Visits an {@code or} filter.
     * <p>
     * <b>Implementation note</b>: for the purposes of matching, an empty
     * sub-filter list should always evaluate to {@code false}.
     *
     * @param p
     *            A visitor specified parameter.
     * @param subFilters
     *            The unmodifiable list of sub-filters.
     * @return Returns a visitor specified result.
     */
    R visitOrFilter(P p, List<QueryFilter<F>> subFilters);

    /**
     * Visits a {@code present} filter.
     *
     * @param p
     *            A visitor specified parameter.
     * @param field
     *            A definition of the field to be compared.
     * @return Returns a visitor specified result.
     */
    R visitPresentFilter(P p, F field);

    /**
     * Visits a {@code starts with} filter.
     *
     * @param p
     *            A visitor specified parameter.
     * @param field
     *            A definition of the field to be compared.
     * @param valueAssertion
     *            The value assertion.
     * @return Returns a visitor specified result.
     */
    R visitStartsWithFilter(P p, F field, Object valueAssertion);

}
