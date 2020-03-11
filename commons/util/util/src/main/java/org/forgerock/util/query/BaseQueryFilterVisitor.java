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
 * A base implementation of {@link org.forgerock.util.query.QueryFilterVisitor} where
 * all methods throw an {@link java.lang.UnsupportedOperationException} by default -
 * override just the methods you need.
 *
 * @see org.forgerock.util.query.QueryFilterVisitor
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
public abstract class BaseQueryFilterVisitor<R, P, F> implements QueryFilterVisitor<R, P, F> {
    @Override
    public R visitAndFilter(P p, List<QueryFilter<F>> subFilters) {
        throw new UnsupportedOperationException();
    }

    @Override
    public R visitBooleanLiteralFilter(P p, boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public R visitContainsFilter(P p, F field, Object valueAssertion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public R visitEqualsFilter(P p, F field, Object valueAssertion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public R visitExtendedMatchFilter(P p, F field, String operator, Object valueAssertion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public R visitGreaterThanFilter(P p, F field, Object valueAssertion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public R visitGreaterThanOrEqualToFilter(P p, F field, Object valueAssertion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public R visitLessThanFilter(P p, F field, Object valueAssertion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public R visitLessThanOrEqualToFilter(P p, F field, Object valueAssertion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public R visitNotFilter(P p, QueryFilter<F> subFilter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public R visitOrFilter(P p, List<QueryFilter<F>> subFilters) {
        throw new UnsupportedOperationException();
    }

    @Override
    public R visitPresentFilter(P p, F field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public R visitStartsWithFilter(P p, F field, Object valueAssertion) {
        throw new UnsupportedOperationException();
    }
}
