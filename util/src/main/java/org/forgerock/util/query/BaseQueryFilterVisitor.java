package org.forgerock.util.query;

import java.util.List;

/**
 * A base implementation of {@link org.forgerock.util.query.QueryFilterVisitor} where
 * all methods throw an {@link java.lang.UnsupportedOperationException} by default -
 * override just the methods you need.
 * @see org.forgerock.util.query.QueryFilterVisitor
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
