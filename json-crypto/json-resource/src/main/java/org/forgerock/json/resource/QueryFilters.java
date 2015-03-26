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

import java.util.List;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterParser;
import org.forgerock.util.query.QueryFilterVisitor;

/**
 * Convenience methods to create {@link org.forgerock.util.query.QueryFilter} that
 * specify fields in terms of {@link org.forgerock.json.fluent.JsonPointer} instances.
 * <p>
 * A query string has the following string representation:
 *
 * <pre>
 * Expr           = OrExpr
 * OrExpr         = AndExpr ( 'or' AndExpr ) *
 * AndExpr        = NotExpr ( 'and' NotExpr ) *
 * NotExpr        = '!' PrimaryExpr | PrimaryExpr
 * PrimaryExpr    = '(' Expr ')' | ComparisonExpr | PresenceExpr | LiteralExpr
 * ComparisonExpr = Pointer OpName JsonValue
 * PresenceExpr   = Pointer 'pr'
 * LiteralExpr    = 'true' | 'false'
 * Pointer        = JSON pointer
 * OpName         = 'eq' |  # equal to
 *                  'co' |  # contains
 *                  'sw' |  # starts with
 *                  'lt' |  # less than
 *                  'le' |  # less than or equal to
 *                  'gt' |  # greater than
 *                  'ge' |  # greater than or equal to
 *                  STRING  # extended operator
 * JsonValue      = NUMBER | BOOLEAN | '"' UTF8STRING '"'
 * STRING         = ASCII string not containing white-space
 * UTF8STRING     = UTF-8 string possibly containing white-space
 * </pre>
 *
 * Note that white space, parentheses, and exclamation characters need URL
 * encoding in HTTP query strings.
 *
 * @see org.forgerock.util.query.QueryFilter
 */
public final class QueryFilters {

    private static final QueryFilterParser<JsonPointer> parser = new QueryFilterParser<JsonPointer>() {
        @Override
        protected JsonPointer parseField(String s) {
            return new JsonPointer(s);
        }
    };

    public static QueryFilter<JsonPointer> parse(String query) {
        return parser.valueOf(query);
    }

    private QueryFilters() {}

}
