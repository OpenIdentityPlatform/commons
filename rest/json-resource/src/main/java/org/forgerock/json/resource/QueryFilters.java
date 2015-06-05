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
 * Copyright 2012-2015 ForgeRock AS. All rights reserved.
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
 *
 * @see org.forgerock.util.query.QueryFilterParser
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
