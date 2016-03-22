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

import static org.forgerock.util.query.QueryFilterOperators.*;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A query string has the following string representation:
 *
 * <pre>
 * Expr           = OrExpr
 * OrExpr         = AndExpr ( 'or' AndExpr ) *
 * AndExpr        = NotExpr ( 'and' NotExpr ) *
 * NotExpr        = '!' PrimaryExpr | PrimaryExpr
 * PrimaryExpr    = '(' Expr ')' | ComparisonExpr | PresenceExpr | LiteralExpr
 * ComparisonExpr = Pointer OpName Value
 * PresenceExpr   = Pointer 'pr'
 * LiteralExpr    = 'true' | 'false'
 * Pointer        = Case-sensitive field specification
 * OpName         = 'eq' |  # equal to
 *                  'co' |  # contains
 *                  'sw' |  # starts with
 *                  'lt' |  # less than
 *                  'le' |  # less than or equal to
 *                  'gt' |  # greater than
 *                  'ge' |  # greater than or equal to
 *                  STRING  # extended operator
 * Value          = NUMBER | BOOLEAN | '"' UTF8STRING '"' | ''' UTF8STRING '''
 * STRING         = ASCII string not containing white-space
 * UTF8STRING     = UTF-8 string possibly containing white-space
 * </pre>
 *
 * Note that white space, parentheses, and exclamation characters need URL
 * when passed via HTTP query strings.
 * <p>
 * ASCII and UTF-8 strings will treat the backslash character as an escape character.
 * For an example, this will allow for the inclusion of quotes or single-quotes within
 * a string that is surrounded by the same type of quotes: "tes\"t".  The backslash
 * character itself will also need to be escaped if it is to be included in the string.
 * <p>
 * In addition to single valued properties (number, boolean, and string), query filters
 * can be applied to multi-valued properties. When operating on properties that are an
 * array or list type the operation should be evaluated on each element in the array,
 * passing if any of the elements in the array or list pass the operation.
 *
 * @param <F> The type of field description used in parsed {@link QueryFilter} objects.
 */
public abstract class QueryFilterParser<F> {

    // Maximum permitted query filter nesting depth.
    private static final int VALUE_OF_MAX_DEPTH = 256;

    /**
     * Parses the field description from the current filter token into the type of field
     * description the QueryFilter uses.
     * @param fieldDescription The token from parsing the query string.
     * @return The field description.
     */
    protected abstract F parseField(String fieldDescription);

    /**
     * Parses the provided string representation of a query filter as a
     * {@code QueryFilter}.
     *
     * @param string
     *            The string representation of a query filter .
     *
     * @return The parsed {@code QueryFilter}.
     * @throws IllegalArgumentException
     *             If {@code string} is not a valid string representation of a
     *             query filter.
     */
    public QueryFilter<F> valueOf(final String string) {
        // Use recursive descent of grammar described in class Javadoc.
        final FilterTokenizer tokenizer = new FilterTokenizer(string);
        final QueryFilter<F> filter = valueOfOrExpr(tokenizer, 0);
        if (tokenizer.hasNext()) {
            return valueOfIllegalArgument(tokenizer);
        } else {
            return filter;
        }
    }

    private void checkDepth(final FilterTokenizer tokenizer, final int depth) {
        if (depth > VALUE_OF_MAX_DEPTH) {
            throw new IllegalArgumentException("The query filter '" + tokenizer
                    + "' cannot be parsed because it contains more than " + VALUE_OF_MAX_DEPTH
                    + " nexted expressions");
        }
    }

    private QueryFilter<F> valueOfAndExpr(final FilterTokenizer tokenizer, final int depth) {
        checkDepth(tokenizer, depth);
        QueryFilter<F> filter = valueOfNotExpr(tokenizer, depth + 1);
        List<QueryFilter<F>> subFilters = null;
        while (tokenizer.hasNext() && tokenizer.peek().equalsIgnoreCase(AND)) {
            tokenizer.next();
            if (subFilters == null) {
                subFilters = new LinkedList<>();
                subFilters.add(filter);
            }
            subFilters.add(valueOfNotExpr(tokenizer, depth + 1));
        }
        if (subFilters != null) {
            filter = QueryFilter.and(subFilters);
        }
        return filter;
    }

    private QueryFilter<F> valueOfIllegalArgument(final FilterTokenizer tokenizer) {
        throw new IllegalArgumentException("Invalid query filter '" + tokenizer + "'");
    }

    private QueryFilter<F> valueOfNotExpr(final FilterTokenizer tokenizer, final int depth) {
        checkDepth(tokenizer, depth);
        if (tokenizer.hasNext() && tokenizer.peek().equalsIgnoreCase(NOT)) {
            tokenizer.next();
            final QueryFilter<F> rhs = valueOfPrimaryExpr(tokenizer, depth + 1);
            return QueryFilter.not(rhs);
        } else {
            return valueOfPrimaryExpr(tokenizer, depth + 1);
        }
    }

    private QueryFilter<F> valueOfOrExpr(final FilterTokenizer tokenizer, final int depth) {
        checkDepth(tokenizer, depth);
        QueryFilter<F> filter = valueOfAndExpr(tokenizer, depth + 1);
        List<QueryFilter<F>> subFilters = null;
        while (tokenizer.hasNext() && tokenizer.peek().equalsIgnoreCase(OR)) {
            tokenizer.next();
            if (subFilters == null) {
                subFilters = new LinkedList<>();
                subFilters.add(filter);
            }
            subFilters.add(valueOfAndExpr(tokenizer, depth + 1));
        }
        if (subFilters != null) {
            filter = QueryFilter.or(subFilters);
        }
        return filter;
    }

    private QueryFilter<F> valueOfPrimaryExpr(final FilterTokenizer tokenizer, final int depth) {
        checkDepth(tokenizer, depth);
        if (!tokenizer.hasNext()) {
            return valueOfIllegalArgument(tokenizer);
        }
        String nextToken = tokenizer.next();
        if (nextToken.equals("(")) {
            // Nested expression.
            final QueryFilter<F> filter = valueOfOrExpr(tokenizer, depth + 1);
            if (!tokenizer.hasNext() || !tokenizer.next().equals(")")) {
                return valueOfIllegalArgument(tokenizer);
            }
            return filter;
        } else if (nextToken.equalsIgnoreCase(TRUE)) {
            return QueryFilter.alwaysTrue();
        } else if (nextToken.equalsIgnoreCase(FALSE)) {
            return QueryFilter.alwaysFalse();
        } else if (nextToken.equals("\"")) {
            return valueOfIllegalArgument(tokenizer);
        } else {
            // Assertion.
            final F pointer = parseField(nextToken);
            if (!tokenizer.hasNext()) {
                return valueOfIllegalArgument(tokenizer);
            }
            final String operator = tokenizer.next();
            if (operator.equalsIgnoreCase(PRESENT)) {
                return QueryFilter.present(pointer);
            } else {
                // Read assertion value: NUMBER | BOOLEAN | '"' UTF8STRING '"'
                if (!tokenizer.hasNext()) {
                    return valueOfIllegalArgument(tokenizer);
                }
                final Object assertionValue;
                nextToken = tokenizer.next();
                if (nextToken.equals("\"")) {
                    // UTF8STRING delimited by quotes
                    if (!tokenizer.hasNext()) {
                        return valueOfIllegalArgument(tokenizer);
                    }
                    assertionValue = tokenizer.next();
                    if (!tokenizer.hasNext() || !tokenizer.next().equals("\"")) {
                        return valueOfIllegalArgument(tokenizer);
                    }
                } else if (nextToken.equals("'")) {
                    // UTF8STRING delimited by single quotes
                    if (!tokenizer.hasNext()) {
                        return valueOfIllegalArgument(tokenizer);
                    }
                    assertionValue = tokenizer.next();
                    if (!tokenizer.hasNext() || !tokenizer.next().equals("'")) {
                        return valueOfIllegalArgument(tokenizer);
                    }
                } else if (nextToken.equalsIgnoreCase(TRUE)
                        || nextToken.equalsIgnoreCase(FALSE)) {
                    assertionValue = Boolean.parseBoolean(nextToken);
                } else if (nextToken.indexOf('.') >= 0) {
                    // Floating point number.
                    assertionValue = Double.parseDouble(nextToken);
                } else {
                    // Must be an integer.
                    assertionValue = Long.parseLong(nextToken);
                }
                try {
                    return comparisonFilter(pointer, operator, assertionValue);
                } catch (final IllegalArgumentException e) {
                    return valueOfIllegalArgument(tokenizer);
                }
            }
        }
    }

    /**
     * Creates a new generic comparison filter using the provided field name,
     * operator, and value assertion. When the provided operator name represents
     * a core operator, e.g. "eq", then this method is equivalent to calling the
     * equivalent constructor, e.g. {@link QueryFilter#equalTo(Object, Object)}.
     * Otherwise, when the operator name does not correspond to a core operator,
     * an extended comparison filter will be returned.
     *
     * @param field
     *            The name of field to be compared.
     * @param operator
     *            The operator to use for the comparison, which must be one of
     *            the core operator names, or a string matching the regular
     *            expression {@code [a-zA-Z_0-9.]+}.
     * @param valueAssertion
     *            The assertion value.
     * @return The newly created generic comparison filter.
     * @throws IllegalArgumentException
     *             If {@code operator} is not a valid operator name.
     */
    private QueryFilter<F> comparisonFilter(final F field, final String operator, final Object valueAssertion) {
        if (operator.equalsIgnoreCase(EQUALS)) {
            return QueryFilter.equalTo(field, valueAssertion);
        } else if (operator.equalsIgnoreCase(GREATER_THAN)) {
            return QueryFilter.greaterThan(field, valueAssertion);
        } else if (operator.equalsIgnoreCase(GREATER_EQUAL)) {
            return QueryFilter.greaterThanOrEqualTo(field, valueAssertion);
        } else if (operator.equalsIgnoreCase(LESS_THAN)) {
            return QueryFilter.lessThan(field, valueAssertion);
        } else if (operator.equalsIgnoreCase(LESS_EQUAL)) {
            return QueryFilter.lessThanOrEqualTo(field, valueAssertion);
        } else if (operator.equalsIgnoreCase(CONTAINS)) {
            return QueryFilter.contains(field, valueAssertion);
        } else if (operator.equalsIgnoreCase(STARTS_WITH)) {
            return QueryFilter.startsWith(field, valueAssertion);
        } else if (operator.matches("[a-zA-Z_0-9.]+")) {
            return QueryFilter.extendedMatch(field, operator, valueAssertion);
        } else {
            throw new IllegalArgumentException("\"" + operator + "\" is not a valid filter operator");
        }
    }

    private static final class FilterTokenizer implements Iterator<String> {
        private static final int NEED_END_STRING = 2;
        private static final int NEED_START_STRING = 1;
        private static final int NEED_TOKEN = 0;

        private String filterString;
        private String nextToken;
        private int pos;
        private int state;
        private char stringDelimiter;

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
                for (; pos < filterString.length() && filterString.charAt(pos) != stringDelimiter; pos++) {
                    if (filterString.charAt(pos) == '\\') {
                        if ((pos + 1) == filterString.length()) {
                            throw new IllegalArgumentException("The filter string cannot end with an escape character");
                        }
                        // Found an escaped character, so remove the '\')
                        filterString = new StringBuilder(filterString).deleteCharAt(pos).toString();
                    }
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
                        stringDelimiter = '"';
                        break;
                    case '\'':
                        state = NEED_START_STRING;
                        stringDelimiter = '\'';
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

}
