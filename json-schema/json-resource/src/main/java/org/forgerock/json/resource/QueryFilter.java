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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2012 ForgeRock AS. All rights reserved.
 */

package org.forgerock.json.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.forgerock.json.fluent.JsonPointer;

/**
 * A filter which can be used to select which JSON resources should be included
 * in the results of a query request.
 */
public final class QueryFilter {

    private static final class AndImpl extends Impl {
        private final List<QueryFilter> subFilters;

        private AndImpl(final List<QueryFilter> subFilters) {
            this.subFilters = subFilters;
        }

        @Override
        protected <R, P> R accept(final QueryFilterVisitor<R, P> v, final P p) {
            return v.visitAndFilter(p, subFilters);
        }
    }

    /*
     * TODO: should value assertions be Objects or Strings? Objects allows use
     * of numbers, during construction, but visitors may need to handle
     * different types (e.g. Date or String representation of a date).
     */

    private static final class BooleanLiteralImpl extends Impl {
        private final boolean value;

        private BooleanLiteralImpl(final boolean value) {
            this.value = value;
        }

        @Override
        protected <R, P> R accept(final QueryFilterVisitor<R, P> v, final P p) {
            return v.visitBooleanLiteralFilter(p, value);
        }
    }

    private static final class EqualsImpl extends Impl {
        private final JsonPointer field;
        private final Object valueAssertion;

        private EqualsImpl(final JsonPointer field, final Object valueAssertion) {
            this.field = field;
            this.valueAssertion = valueAssertion;
        }

        @Override
        protected <R, P> R accept(final QueryFilterVisitor<R, P> v, final P p) {
            return v.visitEqualsFilter(p, field, valueAssertion);
        }
    }

    private static final class ExtendedMatchImpl extends Impl {
        private final JsonPointer field;
        private final String matchingRuleId;
        private final Object valueAssertion;

        private ExtendedMatchImpl(final JsonPointer field, final String matchingRuleId,
                final Object valueAssertion) {
            this.field = field;
            this.matchingRuleId = matchingRuleId;
            this.valueAssertion = valueAssertion;
        }

        @Override
        protected <R, P> R accept(final QueryFilterVisitor<R, P> v, final P p) {
            return v.visitExtendedMatchFilter(p, field, matchingRuleId, valueAssertion);
        }
    }

    private static final class FilterTokenizer implements Iterator<String> {
        private static final int NEED_END_STRING = 2;
        private static final int NEED_START_STRING = 1;
        private static final int NEED_TOKEN = 0;

        private final String filterString;
        private String nextToken;
        private int pos;
        private int state;

        private FilterTokenizer(final String filterString) {
            this.filterString = filterString;
            this.pos = 0;
            this.state = NEED_TOKEN;
            readNextToken();
        }

        @Override
        public boolean hasNext() {
            return nextToken != null;
        }

        @Override
        public String next() {
            final String next = peek();
            readNextToken();
            return next;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return filterString;
        }

        private String peek() {
            if (nextToken == null) {
                throw new NoSuchElementException();
            }
            return nextToken;
        }

        private void readNextToken() {
            switch (state) {
            case NEED_START_STRING:
                final int stringStart = pos;
                for (; pos < filterString.length() && filterString.charAt(pos) != '"'; pos++) {
                    // Do nothing
                }
                nextToken = filterString.substring(stringStart, pos);
                state = NEED_END_STRING;
                break;
            case NEED_END_STRING:
                // NEED_START_STRING guarantees that we are either at the end of the string
                // or the next character is a quote.
                if (pos < filterString.length()) {
                    nextToken = filterString.substring(pos, ++pos);
                } else {
                    nextToken = null;
                }
                state = NEED_TOKEN;
                break;
            default: // NEED_TOKEN:
                if (!skipWhiteSpace()) {
                    nextToken = null;
                } else {
                    final int tokenStart = pos;
                    switch (filterString.charAt(pos++)) {
                    case '(':
                    case ')':
                        break;
                    case '"':
                        state = NEED_START_STRING;
                        break;
                    default:
                        for (; pos < filterString.length(); pos++) {
                            final char c = filterString.charAt(pos);
                            if (c == '(' || c == ')' || c == ' ') {
                                break;
                            }
                        }
                        break;
                    }
                    nextToken = filterString.substring(tokenStart, pos);
                }
            }
        }

        private boolean skipWhiteSpace() {
            for (; pos < filterString.length() && filterString.charAt(pos) == ' '; pos++) {
                // Do nothing
            }
            return pos < filterString.length();
        }
    }

    private static final class GreaterThanImpl extends Impl {
        private final JsonPointer field;
        private final Object valueAssertion;

        private GreaterThanImpl(final JsonPointer field, final Object valueAssertion) {
            this.field = field;
            this.valueAssertion = valueAssertion;
        }

        @Override
        protected <R, P> R accept(final QueryFilterVisitor<R, P> v, final P p) {
            return v.visitGreaterThanFilter(p, field, valueAssertion);
        }
    }

    private static final class GreaterThanOrEqualToImpl extends Impl {
        private final JsonPointer field;
        private final Object valueAssertion;

        private GreaterThanOrEqualToImpl(final JsonPointer field, final Object valueAssertion) {
            this.field = field;
            this.valueAssertion = valueAssertion;
        }

        @Override
        protected <R, P> R accept(final QueryFilterVisitor<R, P> v, final P p) {
            return v.visitGreaterThanOrEqualToFilter(p, field, valueAssertion);
        }
    }

    private static abstract class Impl {
        protected Impl() {
            // Nothing to do.
        }

        protected abstract <R, P> R accept(QueryFilterVisitor<R, P> v, P p);
    }

    private static final class LessThanImpl extends Impl {
        private final JsonPointer field;
        private final Object valueAssertion;

        private LessThanImpl(final JsonPointer field, final Object valueAssertion) {
            this.field = field;
            this.valueAssertion = valueAssertion;
        }

        @Override
        protected <R, P> R accept(final QueryFilterVisitor<R, P> v, final P p) {
            return v.visitLessThanFilter(p, field, valueAssertion);
        }
    }

    private static final class LessThanOrEqualToImpl extends Impl {
        private final JsonPointer field;
        private final Object valueAssertion;

        private LessThanOrEqualToImpl(final JsonPointer field, final Object valueAssertion) {
            this.field = field;
            this.valueAssertion = valueAssertion;
        }

        @Override
        protected <R, P> R accept(final QueryFilterVisitor<R, P> v, final P p) {
            return v.visitLessThanOrEqualToFilter(p, field, valueAssertion);
        }
    }

    private static final class NotImpl extends Impl {
        private final QueryFilter subFilter;

        private NotImpl(final QueryFilter subFilter) {
            this.subFilter = subFilter;
        }

        @Override
        protected <R, P> R accept(final QueryFilterVisitor<R, P> v, final P p) {
            return v.visitNotFilter(p, subFilter);
        }
    }

    private static final class OrImpl extends Impl {
        private final List<QueryFilter> subFilters;

        private OrImpl(final List<QueryFilter> subFilters) {
            this.subFilters = subFilters;
        }

        @Override
        protected <R, P> R accept(final QueryFilterVisitor<R, P> v, final P p) {
            return v.visitOrFilter(p, subFilters);
        }
    }

    private static final class PresentImpl extends Impl {
        private final JsonPointer field;

        private PresentImpl(final JsonPointer field) {
            this.field = field;
        }

        @Override
        protected <R, P> R accept(final QueryFilterVisitor<R, P> v, final P p) {
            return v.visitPresentFilter(p, field);
        }
    }

    private static final QueryFilter ALWAYS_FALSE = new QueryFilter(new BooleanLiteralImpl(false));

    private static final QueryFilter ALWAYS_TRUE = new QueryFilter(new BooleanLiteralImpl(true));

    private static final QueryFilterVisitor<StringBuilder, StringBuilder> TO_STRING_VISITOR =
            new QueryFilterVisitor<StringBuilder, StringBuilder>() {

                @Override
                public StringBuilder visitAndFilter(final StringBuilder p,
                        final List<QueryFilter> subFilters) {
                    p.append('(');
                    boolean isFirst = true;
                    for (final QueryFilter subFilter : subFilters) {
                        if (isFirst) {
                            isFirst = false;
                        } else {
                            p.append(" and ");
                        }
                        subFilter.accept(this, p);
                    }
                    p.append(')');
                    return p;
                }

                @Override
                public StringBuilder visitBooleanLiteralFilter(final StringBuilder p,
                        final boolean value) {
                    // This is not officially supported in SCIM.
                    p.append(value);
                    return p;
                }

                @Override
                public StringBuilder visitEqualsFilter(final StringBuilder p,
                        final JsonPointer field, final Object valueAssertion) {
                    return visitComparator(p, "eq", field, valueAssertion);
                }

                @Override
                public StringBuilder visitExtendedMatchFilter(final StringBuilder p,
                        final JsonPointer field, final String matchingRuleId,
                        final Object valueAssertion) {
                    return visitComparator(p, matchingRuleId, field, valueAssertion);
                }

                @Override
                public StringBuilder visitGreaterThanFilter(final StringBuilder p,
                        final JsonPointer field, final Object valueAssertion) {
                    return visitComparator(p, "gt", field, valueAssertion);
                }

                @Override
                public StringBuilder visitGreaterThanOrEqualToFilter(final StringBuilder p,
                        final JsonPointer field, final Object valueAssertion) {
                    return visitComparator(p, "ge", field, valueAssertion);
                }

                @Override
                public StringBuilder visitLessThanFilter(final StringBuilder p,
                        final JsonPointer field, final Object valueAssertion) {
                    return visitComparator(p, "lt", field, valueAssertion);
                }

                @Override
                public StringBuilder visitLessThanOrEqualToFilter(final StringBuilder p,
                        final JsonPointer field, final Object valueAssertion) {
                    return visitComparator(p, "le", field, valueAssertion);
                }

                @Override
                public StringBuilder visitNotFilter(final StringBuilder p,
                        final QueryFilter subFilter) {
                    // This is not officially supported in SCIM.
                    p.append("nt (");
                    subFilter.accept(this, p);
                    p.append(')');
                    return p;
                }

                @Override
                public StringBuilder visitOrFilter(final StringBuilder p,
                        final List<QueryFilter> subFilters) {
                    p.append('(');
                    boolean isFirst = true;
                    for (final QueryFilter subFilter : subFilters) {
                        if (isFirst) {
                            isFirst = false;
                        } else {
                            p.append(" or ");
                        }
                        subFilter.accept(this, p);
                    }
                    p.append(')');
                    return p;
                }

                @Override
                public StringBuilder visitPresentFilter(final StringBuilder p,
                        final JsonPointer field) {
                    p.append(field.toString());
                    p.append(' ');
                    p.append("pr");
                    return p;
                }

                private StringBuilder visitComparator(final StringBuilder p,
                        final String comparator, final JsonPointer field,
                        final Object valueAssertion) {
                    p.append(field.toString());
                    p.append(' ');
                    p.append(comparator);
                    p.append(' ');
                    if (valueAssertion instanceof Boolean || valueAssertion instanceof Number) {
                        // No need for quotes.
                        p.append(valueAssertion);
                    } else {
                        p.append('"');
                        p.append(valueAssertion);
                        p.append('"');
                    }
                    return p;
                }
            };

    /**
     * Returns a filter which does not match any resources.
     *
     * @return A filter which does not match any resources.
     */
    public static QueryFilter alwaysFalse() {
        return ALWAYS_FALSE;
    }

    /**
     * Returns a filter which matches all resources.
     *
     * @return A filter which matches all resources.
     */
    public static QueryFilter alwaysTrue() {
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
     */
    public static QueryFilter and(final Collection<QueryFilter> subFilters) {
        switch (subFilters.size()) {
        case 0:
            return alwaysTrue();
        case 1:
            return subFilters.iterator().next();
        default:
            return new QueryFilter(new AndImpl(Collections
                    .unmodifiableList(new ArrayList<QueryFilter>(subFilters))));
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
     */
    public static QueryFilter and(final QueryFilter... subFilters) {
        return and(Arrays.asList(subFilters));
    }

    /**
     * Creates a new {@code equality} filter using the provided field name and
     * value assertion.
     *
     * @param field
     *            The name of field within the JSON resource to be compared.
     * @param valueAssertion
     *            The assertion value.
     * @return The newly created {@code equality} filter.
     */
    public static QueryFilter equalTo(final JsonPointer field, final Object valueAssertion) {
        return new QueryFilter(new EqualsImpl(field, valueAssertion));
    }

    /**
     * Creates a new {@code equality} filter using the provided field name and
     * value assertion.
     *
     * @param field
     *            The name of field within the JSON resource to be compared.
     * @param valueAssertion
     *            The assertion value.
     * @return The newly created {@code equality} filter.
     */
    public static QueryFilter equalTo(final String field, final Object valueAssertion) {
        return equalTo(new JsonPointer(field), valueAssertion);
    }

    /**
     * Creates a new {@code extended match} filter using the provided field
     * name, matching rule identifier, and value assertion.
     *
     * @param field
     *            The name of field within the JSON resource to be compared.
     * @param matchingRuleId
     *            The name of the matching rule to use for the comparison.
     * @param valueAssertion
     *            The assertion value.
     * @return The newly created {@code extended match} filter.
     */
    public static QueryFilter extendedMatch(final JsonPointer field, final String matchingRuleId,
            final Object valueAssertion) {
        return new QueryFilter(new ExtendedMatchImpl(field, matchingRuleId, valueAssertion));
    }

    /**
     * Creates a new {@code extended match} filter using the provided field
     * name, matching rule identifier, and value assertion.
     *
     * @param field
     *            The name of field within the JSON resource to be compared.
     * @param matchingRuleId
     *            The name of the matching rule to use for the comparison.
     * @param valueAssertion
     *            The assertion value.
     * @return The newly created {@code extended match} filter.
     */
    public static QueryFilter extendedMatch(final String field, final String matchingRuleId,
            final Object valueAssertion) {
        return extendedMatch(new JsonPointer(field), matchingRuleId, valueAssertion);
    }

    /**
     * Creates a new {@code greater than} filter using the provided field name
     * and value assertion.
     *
     * @param field
     *            The name of field within the JSON resource to be compared.
     * @param valueAssertion
     *            The assertion value.
     * @return The newly created {@code greater than} filter.
     */
    public static QueryFilter greaterThan(final JsonPointer field, final Object valueAssertion) {
        return new QueryFilter(new GreaterThanImpl(field, valueAssertion));
    }

    /**
     * Creates a new {@code greater than} filter using the provided field name
     * and value assertion.
     *
     * @param field
     *            The name of field within the JSON resource to be compared.
     * @param valueAssertion
     *            The assertion value.
     * @return The newly created {@code greater than} filter.
     */
    public static QueryFilter greaterThan(final String field, final Object valueAssertion) {
        return greaterThan(new JsonPointer(field), valueAssertion);
    }

    /**
     * Creates a new {@code greater than or equal to} filter using the provided
     * field name and value assertion.
     *
     * @param field
     *            The name of field within the JSON resource to be compared.
     * @param valueAssertion
     *            The assertion value.
     * @return The newly created {@code greater than or equal to} filter.
     */
    public static QueryFilter greaterThanOrEqualTo(final JsonPointer field,
            final Object valueAssertion) {
        return new QueryFilter(new GreaterThanOrEqualToImpl(field, valueAssertion));
    }

    /**
     * Creates a new {@code greater than or equal to} filter using the provided
     * field name and value assertion.
     *
     * @param field
     *            The name of field within the JSON resource to be compared.
     * @param valueAssertion
     *            The assertion value.
     * @return The newly created {@code greater than or equal to} filter.
     */
    public static QueryFilter greaterThanOrEqualTo(final String field, final Object valueAssertion) {
        return greaterThanOrEqualTo(new JsonPointer(field), valueAssertion);
    }

    /**
     * Creates a new {@code less than} filter using the provided field name and
     * value assertion.
     *
     * @param field
     *            The name of field within the JSON resource to be compared.
     * @param valueAssertion
     *            The assertion value.
     * @return The newly created {@code less than} filter.
     */
    public static QueryFilter lessThan(final JsonPointer field, final Object valueAssertion) {
        return new QueryFilter(new LessThanImpl(field, valueAssertion));
    }

    /**
     * Creates a new {@code less than} filter using the provided field name and
     * value assertion.
     *
     * @param field
     *            The name of field within the JSON resource to be compared.
     * @param valueAssertion
     *            The assertion value.
     * @return The newly created {@code less than} filter.
     */
    public static QueryFilter lessThan(final String field, final Object valueAssertion) {
        return lessThan(new JsonPointer(field), valueAssertion);
    }

    /**
     * Creates a new {@code less than or equal to} filter using the provided
     * field name and value assertion.
     *
     * @param field
     *            The name of field within the JSON resource to be compared.
     * @param valueAssertion
     *            The assertion value.
     * @return The newly created {@code less than or equal to} filter.
     */
    public static QueryFilter lessThanOrEqualTo(final JsonPointer field, final Object valueAssertion) {
        return new QueryFilter(new LessThanOrEqualToImpl(field, valueAssertion));
    }

    /**
     * Creates a new {@code less than or equal to} filter using the provided
     * field name and value assertion.
     *
     * @param field
     *            The name of field within the JSON resource to be compared.
     * @param valueAssertion
     *            The assertion value.
     * @return The newly created {@code less than or equal to} filter.
     */
    public static QueryFilter lessThanOrEqualTo(final String field, final Object valueAssertion) {
        return lessThanOrEqualTo(new JsonPointer(field), valueAssertion);
    }

    /**
     * Creates a new {@code not} filter using the provided sub-filter.
     *
     * @param subFilter
     *            The sub-filter.
     * @return The newly created {@code not} filter.
     */
    public static QueryFilter not(final QueryFilter subFilter) {
        return new QueryFilter(new NotImpl(subFilter));
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
     */
    public static QueryFilter or(final Collection<QueryFilter> subFilters) {
        switch (subFilters.size()) {
        case 0:
            return alwaysFalse();
        case 1:
            return subFilters.iterator().next();
        default:
            return new QueryFilter(new OrImpl(Collections
                    .unmodifiableList(new ArrayList<QueryFilter>(subFilters))));
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
     */
    public static QueryFilter or(final QueryFilter... subFilters) {
        return or(Arrays.asList(subFilters));
    }

    /**
     * Creates a new {@code presence} filter using the provided field name.
     *
     * @param field
     *            The name of field within the JSON resource which must be
     *            present.
     * @return The newly created {@code presence} filter.
     */
    public static QueryFilter present(final JsonPointer field) {
        return new QueryFilter(new PresentImpl(field));
    }

    /**
     * Creates a new {@code presence} filter using the provided field name.
     *
     * @param field
     *            The name of field within the JSON resource which must be
     *            present.
     * @return The newly created {@code presence} filter.
     */
    public static QueryFilter present(final String field) {
        return present(new JsonPointer(field));
    }

    /**
     * Parses the provided string representation of a query filter as a
     * {@code QueryFilter}.
     *
     * @param string
     *            The string representation of a query filter .
     * @return The parsed {@code QueryFilter}.
     * @throws IllegalArgumentException
     *             If {@code string} is not a valid string representation of a
     *             query filter.
     */
    public static QueryFilter valueOf(final String string) {
        // Use recursive descent of following grammar:
        //
        // Expr          = OrExpr
        // OrExpr        = AndExpr ( 'or' AndExpr ) *
        // AndExpr       = NotExpr ( 'and' NotExpr ) *
        // NotExpr       = 'nt' Expr | PrimaryExpr
        // PrimaryExpr   = '(' Expr ')' | Pointer OpName JsonValue | Pointer 'pr' | 'true' | 'false'
        // Pointer       = STRING
        // OpName        = STRING
        // JsonValue     = NUMBER | BOOLEAN | '"' UTF8STRING '"'
        // STRING        = ASCII string not containing white-space
        // UTF8STRING    = UTF-8 string possibly containing white-space
        //
        // See: http://en.wikipedia.org/wiki/Operator-precedence_parser or
        // http://www.engr.mun.ca/~theo/Misc/exp_parsing.htm

        // TODO: protect against stack overflow.

        final FilterTokenizer tokenizer = new FilterTokenizer(string);
        final QueryFilter filter = valueOfOrExpr(tokenizer);
        if (tokenizer.hasNext()) {
            return valueOfIllegalArgument(tokenizer);
        } else {
            return filter;
        }
    }

    private static QueryFilter valueOfAndExpr(final FilterTokenizer tokenizer) {
        QueryFilter filter = valueOfNotExpr(tokenizer);
        List<QueryFilter> subFilters = null;
        while (tokenizer.hasNext() && tokenizer.peek().equals("and")) {
            tokenizer.next();
            if (subFilters == null) {
                subFilters = new LinkedList<QueryFilter>();
                subFilters.add(filter);
            }
            subFilters.add(valueOfNotExpr(tokenizer));
        }
        if (subFilters != null) {
            filter = QueryFilter.and(subFilters);
        }
        return filter;
    }

    private static QueryFilter valueOfIllegalArgument(final FilterTokenizer tokenizer) {
        throw new IllegalArgumentException("Invalid filter '" + tokenizer + "'");
    }

    private static QueryFilter valueOfNotExpr(final FilterTokenizer tokenizer) {
        if (tokenizer.hasNext() && tokenizer.peek().equals("nt")) {
            tokenizer.next();
            final QueryFilter rhs = valueOfOrExpr(tokenizer);
            return QueryFilter.not(rhs);
        } else {
            return valueOfPrimaryExpr(tokenizer);
        }
    }

    private static QueryFilter valueOfOrExpr(final FilterTokenizer tokenizer) {
        QueryFilter filter = valueOfAndExpr(tokenizer);
        List<QueryFilter> subFilters = null;
        while (tokenizer.hasNext() && tokenizer.peek().equals("or")) {
            tokenizer.next();
            if (subFilters == null) {
                subFilters = new LinkedList<QueryFilter>();
                subFilters.add(filter);
            }
            subFilters.add(valueOfAndExpr(tokenizer));
        }
        if (subFilters != null) {
            filter = QueryFilter.or(subFilters);
        }
        return filter;
    }

    private static QueryFilter valueOfPrimaryExpr(final FilterTokenizer tokenizer) {
        if (!tokenizer.hasNext()) {
            return valueOfIllegalArgument(tokenizer);
        }
        String nextToken = tokenizer.next();
        if (nextToken.equals("(")) {
            // Nested expression.
            final QueryFilter filter = valueOfOrExpr(tokenizer);
            if (!tokenizer.hasNext() || !tokenizer.next().equals(")")) {
                return valueOfIllegalArgument(tokenizer);
            }
            return filter;
        } else if (nextToken.equals("true")) {
            return QueryFilter.alwaysTrue();
        } else if (nextToken.equals("false")) {
            return QueryFilter.alwaysFalse();
        } else if (nextToken.equals("\"")) {
            return valueOfIllegalArgument(tokenizer);
        } else {
            // Assertion.
            final JsonPointer pointer = new JsonPointer(nextToken);
            if (!tokenizer.hasNext()) {
                return valueOfIllegalArgument(tokenizer);
            }
            final String operator = tokenizer.next();
            if (operator.equals("pr")) {
                return QueryFilter.present(pointer);
            } else {
                // Read assertion value: NUMBER | BOOLEAN | '"' UTF8STRING '"'
                if (!tokenizer.hasNext()) {
                    return valueOfIllegalArgument(tokenizer);
                }
                final Object assertionValue;
                nextToken = tokenizer.next();
                if (nextToken.equals("\"")) {
                    // UTFSTRING
                    if (!tokenizer.hasNext()) {
                        return valueOfIllegalArgument(tokenizer);
                    }
                    assertionValue = tokenizer.next();
                    if (!tokenizer.hasNext() || !tokenizer.next().equals("\"")) {
                        return valueOfIllegalArgument(tokenizer);
                    }
                } else if (nextToken.equals("true") || nextToken.equals("false")) {
                    assertionValue = Boolean.parseBoolean(nextToken);
                } else {
                    // Must be a number.
                    assertionValue = Long.parseLong(nextToken);
                }

                if (operator.equals("eq")) {
                    return QueryFilter.equalTo(pointer, assertionValue);
                } else if (operator.equals("gt")) {
                    return QueryFilter.greaterThan(pointer, assertionValue);
                } else if (operator.equals("ge")) {
                    return QueryFilter.greaterThanOrEqualTo(pointer, assertionValue);
                } else if (operator.equals("lt")) {
                    return QueryFilter.lessThan(pointer, assertionValue);
                } else if (operator.equals("le")) {
                    return QueryFilter.lessThanOrEqualTo(pointer, assertionValue);
                } else if (operator.matches("[a-zA-Z_0-9.]+")) {
                    return QueryFilter.extendedMatch(pointer, operator, assertionValue);
                } else {
                    return valueOfIllegalArgument(tokenizer);
                }
            }
        }
    }

    private final Impl pimpl;

    private QueryFilter(final Impl pimpl) {
        this.pimpl = pimpl;
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
    public <R, P> R accept(final QueryFilterVisitor<R, P> v, final P p) {
        return pimpl.accept(v, p);
    }

    /**
     * Returns the string representation of this query filter. The string
     * representation is defined to be similar to that of SCIM's, with the
     * following differences:
     * <ul>
     * <li>field references are JSON pointers
     * <li>support for boolean literal expressions, e.g. {@code (true)}
     * <li>support for the logical not operator, e.g.
     * {@code (nt /role eq "user")}
     * </ul>
     *
     * @return The string representation of this query filter.
     */
    @Override
    public String toString() {
        return accept(TO_STRING_VISITOR, new StringBuilder()).toString();
    }

}
