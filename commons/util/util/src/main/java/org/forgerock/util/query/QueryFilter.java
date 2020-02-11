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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A filter which can be used to select resources, which is compatible with the CREST query filters.
 *
 * @param <F>
 *            The type of the field specification.
 */
public class QueryFilter<F> {

    /**
     * Implementation of logical AND filter.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    private static final class AndImpl<FF> extends Impl<FF> {
        private final List<QueryFilter<FF>> subFilters;

        private AndImpl(final List<QueryFilter<FF>> subFilters) {
            this.subFilters = subFilters;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof AndImpl) {
                return subFilters.equals(((AndImpl<?>) obj).subFilters);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return subFilters.hashCode();
        }

        @Override
        protected <R, P> R accept(final QueryFilterVisitor<R, P, FF> v, final P p) {
            return v.visitAndFilter(p, subFilters);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("And(");
            boolean isFirst = true;
            for (final QueryFilter<FF> subFilter : subFilters) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    builder.append(",");
                }
                builder.append(subFilter.pimpl.toString());
            }
            builder.append(')');
            return builder.toString();
        }
    }

    /**
     * Implementation of boolean literal filter.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    private static final class BooleanLiteralImpl<FF> extends Impl<FF> {
        private final boolean value;

        private BooleanLiteralImpl(final boolean value) {
            this.value = value;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof BooleanLiteralImpl) {
                return value == ((BooleanLiteralImpl<?>) obj).value;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Boolean.valueOf(value).hashCode();
        }

        @Override
        protected <R, P> R accept(final QueryFilterVisitor<R, P, FF> v, final P p) {
            return v.visitBooleanLiteralFilter(p, value);
        }

        @Override
        public String toString() {
            return "BooleanLiteral(" + value + ")";
        }
    }

    /*
     * TODO: should value assertions be Objects or Strings? Objects allows use
     * of numbers, during construction, but visitors may need to handle
     * different types (e.g. Date or String representation of a date).
     */

    /**
     * Abstract implementation of comparator filter - declares field and value assertion.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    private static abstract class ComparatorImpl<FF> extends Impl<FF> {
        protected final FF field;
        protected final Object valueAssertion;

        protected ComparatorImpl(final FF field, final Object valueAssertion) {
            this.field = field;
            this.valueAssertion = valueAssertion;
        }

        @Override
        public final boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof ComparatorImpl) {
                final ComparatorImpl<?> o = (ComparatorImpl<?>) obj;
                return field.equals(o.field) && getClass().equals(o.getClass())
                        && valueAssertion.equals(o.valueAssertion);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return (field.hashCode() * 31 + getClass().hashCode()) * 31
                    + valueAssertion.hashCode();
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + field.toString() + "," + valueAssertion + "]";
        }
    }

    /**
     * Implementation of contains filter.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    private static final class ContainsImpl<FF> extends ComparatorImpl<FF> {
        private ContainsImpl(final FF field, final Object valueAssertion) {
            super(field, valueAssertion);
        }

        @Override
        protected <R, P> R accept(final QueryFilterVisitor<R, P, FF> v, final P p) {
            return v.visitContainsFilter(p, field, valueAssertion);
        }

    }

    /**
     * Implementation of equals filter.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    private static final class EqualsImpl<FF> extends ComparatorImpl<FF> {
        private EqualsImpl(final FF field, final Object valueAssertion) {
            super(field, valueAssertion);
        }

        @Override
        protected <R, P> R accept(final QueryFilterVisitor<R, P, FF> v, final P p) {
            return v.visitEqualsFilter(p, field, valueAssertion);
        }

    }

    /**
     * Implementation of extended match filter.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    private static final class ExtendedMatchImpl<FF> extends ComparatorImpl<FF> {
        private final String operator;

        private ExtendedMatchImpl(final FF field, final String operator,
                final Object valueAssertion) {
            super(field, valueAssertion);
            this.operator = operator;
        }

        @Override
        protected <R, P> R accept(final QueryFilterVisitor<R, P, FF> v, final P p) {
            return v.visitExtendedMatchFilter(p, field, operator, valueAssertion);
        }

        public int hashCode() {
            return (field.hashCode() * 31 + operator.hashCode()) * 31
                    + valueAssertion.hashCode();
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + field.toString() + "," + operator + "," + valueAssertion + "]";
        }
    }

    /**
     * Implementation of greater than filter.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    private static final class GreaterThanImpl<FF> extends ComparatorImpl<FF> {
        private GreaterThanImpl(final FF field, final Object valueAssertion) {
            super(field, valueAssertion);
        }

        @Override
        protected <R, P> R accept(final QueryFilterVisitor<R, P, FF> v, final P p) {
            return v.visitGreaterThanFilter(p, field, valueAssertion);
        }

    }

    /**
     * Implementation of greater than or equal to filter.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    private static final class GreaterThanOrEqualToImpl<FF> extends ComparatorImpl<FF> {
        private GreaterThanOrEqualToImpl(final FF field, final Object valueAssertion) {
            super(field, valueAssertion);
        }

        @Override
        protected <R, P> R accept(final QueryFilterVisitor<R, P, FF> v, final P p) {
            return v.visitGreaterThanOrEqualToFilter(p, field, valueAssertion);
        }

    }

    /**
     * Abstract filter implementation.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    private static abstract class Impl<FF> {
        protected Impl() {
            // Nothing to do.
        }

        @Override
        public abstract boolean equals(Object obj);

        @Override
        public abstract int hashCode();

        protected abstract <R, P> R accept(QueryFilterVisitor<R, P, FF> v, P p);

    }

    /**
     * Implementation of less than filter.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    private static final class LessThanImpl<FF> extends ComparatorImpl<FF> {
        private LessThanImpl(final FF field, final Object valueAssertion) {
            super(field, valueAssertion);
        }

        @Override
        protected <R, P> R accept(final QueryFilterVisitor<R, P, FF> v, final P p) {
            return v.visitLessThanFilter(p, field, valueAssertion);
        }

    }

    /**
     * Implemnetation of less than or equal to filter.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    private static final class LessThanOrEqualToImpl<FF> extends ComparatorImpl<FF> {
        private LessThanOrEqualToImpl(final FF field, final Object valueAssertion) {
            super(field, valueAssertion);
        }

        @Override
        protected <R, P> R accept(final QueryFilterVisitor<R, P, FF> v, final P p) {
            return v.visitLessThanOrEqualToFilter(p, field, valueAssertion);
        }

    }

    /**
     * Implementation of logical NOT filter.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    private static final class NotImpl<FF> extends Impl<FF> {
        private final QueryFilter<FF> subFilter;

        private NotImpl(final QueryFilter<FF> subFilter) {
            this.subFilter = subFilter;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof NotImpl) {
                return subFilter.equals(((NotImpl<?>) obj).subFilter);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return subFilter.hashCode();
        }

        @Override
        protected <R, P> R accept(final QueryFilterVisitor<R, P, FF> v, final P p) {
            return v.visitNotFilter(p, subFilter);
        }

        @Override
        public String toString() {
            // This is not officially supported in SCIM.
            return "Not(" + subFilter.pimpl.toString() + ")";
        }
    }

    /**
     * Implementation of logical OR filter.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    private static final class OrImpl<FF> extends Impl<FF> {
        private final List<QueryFilter<FF>> subFilters;

        private OrImpl(final List<QueryFilter<FF>> subFilters) {
            this.subFilters = subFilters;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof OrImpl) {
                return subFilters.equals(((OrImpl<?>) obj).subFilters);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return subFilters.hashCode();
        }

        @Override
        protected <R, P> R accept(final QueryFilterVisitor<R, P, FF> v, final P p) {
            return v.visitOrFilter(p, subFilters);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Or(");
            boolean isFirst = true;
            for (final QueryFilter<FF> subFilter : subFilters) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    builder.append(",");
                }
                builder.append(subFilter.pimpl.toString());
            }
            builder.append(')');
            return builder.toString();
        }
    }

    /**
     * Implementation of field present filter.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    private static final class PresentImpl<FF> extends Impl<FF> {
        private final FF field;

        private PresentImpl(final FF field) {
            this.field = field;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof PresentImpl) {
                final PresentImpl<?> o = (PresentImpl<?>) obj;
                return field.equals(o.field);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return field.hashCode();
        }

        @Override
        protected <R, P> R accept(final QueryFilterVisitor<R, P, FF> v, final P p) {
            return v.visitPresentFilter(p, field);
        }

        @Override
        public String toString() {
            return field.toString() + " pr";
        }
    }

    /**
     * Implementation of starts with filter.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    private static final class StartsWithImpl<FF> extends ComparatorImpl<FF> {
        private StartsWithImpl(final FF field, final Object valueAssertion) {
            super(field, valueAssertion);
        }

        @Override
        protected <R, P> R accept(final QueryFilterVisitor<R, P, FF> v, final P p) {
            return v.visitStartsWithFilter(p, field, valueAssertion);
        }

    }

    /**
     * Returns a filter which does not match any resources.
     *
     * @return A filter which does not match any resources.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    @SuppressWarnings("unchecked")
    public static <FF> QueryFilter<FF> alwaysFalse() {
        return ALWAYS_FALSE;
    }

    /**
     * Returns a filter which matches all resources.
     *
     * @return A filter which matches all resources.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    @SuppressWarnings("unchecked")
    public static <FF> QueryFilter<FF> alwaysTrue() {
        return ALWAYS_TRUE;
    }

    /**
     * Creates a new {@code and} filter using the provided list of sub-filters.
     * <p>
     * Creating a new {@code and} filter with a {@code null} or empty list of
     * sub-filters is equivalent to calling {@link #alwaysTrue()}.
     *
     * @param subFilters
     *            The list of sub-filters, may be empty or {@code null}.
     * @return The newly created {@code and} filter.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    public static <FF> QueryFilter<FF> and(final Collection<QueryFilter<FF>> subFilters) {
        switch (subFilters.size()) {
        case 0:
            return alwaysTrue();
        case 1:
            return subFilters.iterator().next();
        default:
            return new QueryFilter<>(new AndImpl<>(Collections.unmodifiableList(new ArrayList<>(subFilters))));
        }
    }

    /**
     * Creates a new {@code and} filter using the provided list of sub-filters.
     * <p>
     * Creating a new {@code and} filter with a {@code null} or empty list of
     * sub-filters is equivalent to calling {@link #alwaysTrue()}.
     *
     * @param subFilters
     *            The list of sub-filters, may be empty or {@code null}.
     * @return The newly created {@code and} filter.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    @SafeVarargs
    public static <FF> QueryFilter<FF> and(final QueryFilter<FF>... subFilters) {
        return and(Arrays.asList(subFilters));
    }

    /**
     * Creates a new generic comparison filter using the provided field name,
     * operator, and value assertion. When the provided operator name represents
     * a core operator, e.g. "eq", then this method is equivalent to calling the
     * equivalent constructor, e.g. {@link #equalTo(Object, Object)}.
     * Otherwise, when the operator name does not correspond to a core operator,
     * an extended comparison filter will be returned.
     *
     * @param field
     *            The name of field within the JSON resource to be compared.
     * @param operator
     *            The operator to use for the comparison, which must be one of
     *            the core operator names, or a string matching the regular
     *            expression {@code [a-zA-Z_0-9.]+}.
     * @param valueAssertion
     *            The assertion value.
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     * @return The newly created generic comparison filter.
     * @throws IllegalArgumentException
     *             If {@code operator} is not a valid operator name.
     */
    public static <FF> QueryFilter<FF> comparisonFilter(final FF field, final String operator,
            final Object valueAssertion) {
        if (operator.equalsIgnoreCase("eq")) {
            return QueryFilter.equalTo(field, valueAssertion);
        } else if (operator.equalsIgnoreCase("gt")) {
            return QueryFilter.greaterThan(field, valueAssertion);
        } else if (operator.equalsIgnoreCase("ge")) {
            return QueryFilter.greaterThanOrEqualTo(field, valueAssertion);
        } else if (operator.equalsIgnoreCase("lt")) {
            return QueryFilter.lessThan(field, valueAssertion);
        } else if (operator.equalsIgnoreCase("le")) {
            return QueryFilter.lessThanOrEqualTo(field, valueAssertion);
        } else if (operator.equalsIgnoreCase("co")) {
            return QueryFilter.contains(field, valueAssertion);
        } else if (operator.equalsIgnoreCase("sw")) {
            return QueryFilter.startsWith(field, valueAssertion);
        } else if (operator.matches("[a-zA-Z_0-9.]+")) {
            return new QueryFilter<>(new ExtendedMatchImpl<>(field, operator, valueAssertion));
        } else {
            throw new IllegalArgumentException("\"" + operator
                    + "\" is not a valid filter operator");
        }
    }

    /**
     * Creates a new {@code contains} filter using the provided field name and
     * value assertion. This method is used to check that the string representation
     * of the field contains the provided substring. When operating on a collection
     * of values the operation should be evaluated on each element in the collection,
     * passing if any of the element's string representations contain the provided
     * substring.
     *
     * @param field
     *            The name of field to be compared.
     * @param valueAssertion
     *            The assertion value.
     * @return The newly created {@code contains} filter.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    public static <FF> QueryFilter<FF> contains(final FF field, final Object valueAssertion) {
        return new QueryFilter<>(new ContainsImpl<>(field, valueAssertion));
    }

    /**
     * Creates a new {@code equality} filter using the provided field name and
     * value assertion. This would represent either equality for single values, or
     * contains and equal value for a collection of values.
     *
     * @param field
     *            The name of field to be compared.
     * @param valueAssertion
     *            The assertion value.
     * @return The newly created {@code equality} filter.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    public static <FF> QueryFilter<FF> equalTo(final FF field, final Object valueAssertion) {
        return new QueryFilter<>(new EqualsImpl<>(field, valueAssertion));
    }

    /**
     * Creates a new {@code greater than} filter using the provided field name
     * and value assertion. This method is used to check that the value of the field
     * is greater than the provided value. When operating on a collection of values
     * the operation should be evaluated on each element in the collection, passing
     * if any of the element's values are greater than the provided value.
     *
     * @param field
     *            The name of field to be compared.
     * @param valueAssertion
     *            The assertion value.
     * @return The newly created {@code greater than} filter.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    public static <FF> QueryFilter<FF> greaterThan(final FF field, final Object valueAssertion) {
        return new QueryFilter<>(new GreaterThanImpl<>(field, valueAssertion));
    }

    /**
     * Creates a new {@code greater than or equal to} filter using the provided
     * field name and value assertion. This method is used to check that the value
     * of the field is greater than or equal to the provided value. When operating
     * on a collection of values the operation should be evaluated on each element
     * in the collection, passing if any of the element's values are greater than
     * or equal to the provided value.
     *
     * @param field
     *            The name of field to be compared.
     * @param valueAssertion
     *            The assertion value.
     * @return The newly created {@code greater than or equal to} filter.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    public static <FF> QueryFilter<FF> greaterThanOrEqualTo(final FF field,
            final Object valueAssertion) {
        return new QueryFilter<>(new GreaterThanOrEqualToImpl<>(field, valueAssertion));
    }

    /**
     * Creates a new {@code less than} filter using the provided field name and
     * value assertion. This method is used to check that the value of the field
     * is less than the provided value. When operating on a collection of values
     * the operation should be evaluated on each element in the collection, passing
     * if any of the element's values are less than the provided value.
     *
     * @param field
     *            The name of field to be compared.
     * @param valueAssertion
     *            The assertion value.
     * @return The newly created {@code less than} filter.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    public static <FF> QueryFilter<FF> lessThan(final FF field, final Object valueAssertion) {
        return new QueryFilter<>(new LessThanImpl<>(field, valueAssertion));
    }

    /**
     * Creates a new {@code extended match} filter using the provided
     * field name, operator and value assertion.
     *
     * @param field
     *            The name of field to be compared.
     * @param operator
     *            The operator.
     * @param valueAssertion
     *            The assertion value.
     * @return The newly created {@code less than or equal to} filter.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    public static <FF> QueryFilter<FF> extendedMatch(final FF field, final String operator,
            final Object valueAssertion) {
        return new QueryFilter<>(new ExtendedMatchImpl<>(field, operator, valueAssertion));
    }

    /**
     * Creates a new {@code less than or equal to} filter using the provided
     * field name and value assertion. This method is used to check that the value
     * of the field is less than or equal to the provided value. When operating
     * on a collection of values the operation should be evaluated on each element
     * in the collection, passing if any of the element's values are less than
     * or equal to the provided value.
     *
     * @param field
     *            The name of field to be compared.
     * @param valueAssertion
     *            The assertion value.
     * @return The newly created {@code less than or equal to} filter.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    public static <FF> QueryFilter<FF> lessThanOrEqualTo(final FF field, final Object valueAssertion) {
        return new QueryFilter<>(new LessThanOrEqualToImpl<>(field, valueAssertion));
    }

    /**
     * Creates a new {@code not} filter using the provided sub-filter.
     *
     * @param subFilter
     *            The sub-filter.
     * @return The newly created {@code not} filter.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    public static <FF> QueryFilter<FF> not(final QueryFilter<FF> subFilter) {
        return new QueryFilter<>(new NotImpl<>(subFilter));
    }

    /**
     * Creates a new {@code or} filter using the provided list of sub-filters.
     * <p>
     * Creating a new {@code or} filter with a {@code null} or empty list of
     * sub-filters is equivalent to calling {@link #alwaysFalse()}.
     *
     * @param subFilters
     *            The list of sub-filters, may be empty or {@code null}.
     * @return The newly created {@code or} filter.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    public static <FF> QueryFilter<FF> or(final Collection<QueryFilter<FF>> subFilters) {
        switch (subFilters.size()) {
        case 0:
            return alwaysFalse();
        case 1:
            return subFilters.iterator().next();
        default:
            return new QueryFilter<>(new OrImpl<>(Collections.unmodifiableList(new ArrayList<>(subFilters))));
        }
    }

    /**
     * Creates a new {@code or} filter using the provided list of sub-filters.
     * <p>
     * Creating a new {@code or} filter with a {@code null} or empty list of
     * sub-filters is equivalent to calling {@link #alwaysFalse()}.
     *
     * @param subFilters
     *            The list of sub-filters, may be empty or {@code null}.
     * @return The newly created {@code or} filter.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    @SafeVarargs
    public static <FF> QueryFilter<FF> or(final QueryFilter<FF>... subFilters) {
        return or(Arrays.asList(subFilters));
    }

    /**
     * Creates a new {@code presence} filter using the provided field name.
     *
     * @param field
     *            The name of field which must be
     *            present.
     * @return The newly created {@code presence} filter.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    public static <FF> QueryFilter<FF> present(final FF field) {
        return new QueryFilter<>(new PresentImpl<>(field));
    }

    /**
     * Creates a new {@code starts with} filter using the provided field name
     * and value assertion. This method is used to check that the string representation
     * of the field starts with the provided substring. When operating on a collection
     * of values the operation should be evaluated on each element in the collection,
     * passing if any of the element's string representations starts with the provided
     * substring.
     *
     * @param field
     *            The name of field to be compared.
     * @param valueAssertion
     *            The assertion value.
     * @return The newly created {@code starts with} filter.
     *
     * @param <FF> The type of the field specification. Named to be distinct from the type of the parent class.
     */
    public static <FF> QueryFilter<FF> startsWith(final FF field, final Object valueAssertion) {
        return new QueryFilter<>(new StartsWithImpl<>(field, valueAssertion));
    }

    // @Checkstyle:off
    private static final QueryFilterVisitor<StringBuilder, StringBuilder, Object> TO_STRING_VISITOR =
            new QueryFilterVisitor<StringBuilder, StringBuilder, Object>() {
                @Override
                public StringBuilder visitAndFilter(StringBuilder builder, List<QueryFilter<Object>> subFilters) {
                    return visitCompositeFilter(" and ", builder, subFilters);
                }

                @Override
                public StringBuilder visitOrFilter(StringBuilder builder, List<QueryFilter<Object>> subFilters) {
                    return visitCompositeFilter(" or ", builder, subFilters);
                }

                public StringBuilder visitCompositeFilter(String operation, StringBuilder builder,
                        List<QueryFilter<Object>> subFilters) {
                    builder.append('(');
                    boolean first = true;
                    for (QueryFilter<Object> subfilter : subFilters) {
                        if (!first) {
                            builder.append(operation);
                        } else {
                            first = false;
                        }
                        subfilter.accept(this, builder);
                    }
                    return builder.append(')');
                }

                @Override
                public StringBuilder visitBooleanLiteralFilter(StringBuilder builder, boolean value) {
                    builder.append(value);
                    return builder;
                }

                @Override
                public StringBuilder visitContainsFilter(StringBuilder builder, Object field, Object valueAssertion) {
                    return visitExtendedMatchFilter(builder, field, "co", valueAssertion);
                }

                @Override
                public StringBuilder visitEqualsFilter(StringBuilder builder, Object field, Object valueAssertion) {
                    return visitExtendedMatchFilter(builder, field, "eq", valueAssertion);
                }

                @Override
                public StringBuilder visitExtendedMatchFilter(StringBuilder builder, Object field, String operator,
                        Object valueAssertion) {
                    builder.append(field.toString()).append(" ").append(operator).append(" ");
                    if (valueAssertion instanceof Boolean || valueAssertion instanceof Number) {
                        // No need for quotes.
                        builder.append(valueAssertion);
                    } else {
                        builder.append('"');
                        builder.append(valueAssertion);
                        builder.append('"');
                    }
                    return builder;
                }

                @Override
                public StringBuilder visitGreaterThanFilter(StringBuilder builder, Object field,
                        Object valueAssertion) {
                    return visitExtendedMatchFilter(builder, field, "gt", valueAssertion);
                }

                @Override
                public StringBuilder visitGreaterThanOrEqualToFilter(StringBuilder builder, Object field,
                        Object valueAssertion) {
                    return visitExtendedMatchFilter(builder, field, "ge", valueAssertion);
                }

                @Override
                public StringBuilder visitLessThanFilter(StringBuilder builder, Object field, Object valueAssertion) {
                    return visitExtendedMatchFilter(builder, field, "lt", valueAssertion);
                }

                @Override
                public StringBuilder visitLessThanOrEqualToFilter(StringBuilder builder, Object field,
                        Object valueAssertion) {
                    return visitExtendedMatchFilter(builder, field, "le", valueAssertion);
                }

                @Override
                public StringBuilder visitStartsWithFilter(StringBuilder builder, Object field, Object valueAssertion) {
                    return visitExtendedMatchFilter(builder, field, "sw", valueAssertion);
                }

                @Override
                public StringBuilder visitNotFilter(StringBuilder builder, QueryFilter<Object> subFilter) {
                    builder.append("! (");
                    subFilter.accept(this, builder);
                    return builder.append(')');
                }

                @Override
                public StringBuilder visitPresentFilter(StringBuilder builder, Object field) {
                    return builder.append(field.toString()).append(" pr");
                }
            };
    // @Checkstyle:on

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final QueryFilter ALWAYS_FALSE = new QueryFilter(new BooleanLiteralImpl(false));
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final QueryFilter ALWAYS_TRUE = new QueryFilter(new BooleanLiteralImpl(true));

    /** the filter implementation. */
    protected final Impl<F> pimpl;
    private final QueryFilterVisitor<StringBuilder, StringBuilder, F> toStringVisitor;

    /**
     * Construct a QueryFilter from a base filter implementation.
     *
     * @param pimpl the filter implementation.
     */
    @SuppressWarnings("unchecked")
    protected QueryFilter(final Impl<F> pimpl) {
        this(pimpl, (QueryFilterVisitor<StringBuilder, StringBuilder, F>) TO_STRING_VISITOR);
    }

    /**
     * Construct a QueryFilter from a base filter implementation and a custom toString implementation.
     *
     * @param pimpl the filter implemntation.
     * @param toStringVisitor the visitor to provide a toString implementation.
     */
    protected QueryFilter(final Impl<F> pimpl, QueryFilterVisitor<StringBuilder, StringBuilder, F> toStringVisitor) {
        this.pimpl = pimpl;
        this.toStringVisitor = toStringVisitor;
    }

    /**
     * Applies a {@code QueryFilterVisitor} to this {@code QueryFilter}.
     *
     * @param <R>
     *            The return type of the visitor's methods.
     * @param <P>
     *            The type of the additional parameters to the visitor's
     *            methods.
     * @param v
     *            The filter visitor.
     * @param p
     *            Optional additional visitor parameter.
     * @return A result as specified by the visitor.
     */
    public <R, P> R accept(final QueryFilterVisitor<R, P, F> v, final P p) {
        return pimpl.accept(v, p);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof QueryFilter) {
            return pimpl.equals(((QueryFilter<?>) obj).pimpl);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return pimpl.hashCode();
    }

    /**
     * Returns the string representation of this query filter. The string
     * representation is defined to be similar to that of SCIM's, with the
     * following differences:
     * <ul>
     * <li>field references are JSON pointers
     * <li>support for boolean literal expressions, e.g. {@code (true)}
     * <li>support for the logical not operator, e.g.
     * {@code (! /role eq "user")}
     * </ul>
     *
     * @return The string representation of this query filter.
     */
    @Override
    public String toString() {
        return accept(toStringVisitor, new StringBuilder()).toString();
    }

}
