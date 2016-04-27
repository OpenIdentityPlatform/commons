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
package org.forgerock.selfservice.stages.user;

import java.util.List;
import java.util.Set;
import org.forgerock.json.JsonPointer;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;

/**
 * A {@link QueryFilterVisitor} that validates a query filter. Methods in this visitor class returns false when
 * the query filter field is not present in the white-list of JsonPointers OR the query filter contains
 * operators co, sw, le, ge, lt, gt, pr and regex.
 *
 * @since 0.5.0
 */
final class QueryFilterValidator implements QueryFilterVisitor<Boolean, Set<JsonPointer>, JsonPointer> {

    @Override
    public Boolean visitAndFilter(Set<JsonPointer> whiteList, List<QueryFilter<JsonPointer>> subFilters) {
        return validateFields(whiteList, subFilters);
    }

    @Override
    public Boolean visitBooleanLiteralFilter(Set<JsonPointer> whiteList, boolean booleanLiteral) {
        return false;
    }

    @Override
    public Boolean visitContainsFilter(Set<JsonPointer> whiteList, JsonPointer field, Object valueAssertion) {
        return false;
    }

    @Override
    public Boolean visitEqualsFilter(Set<JsonPointer> whiteList, JsonPointer field, Object valueAssertion) {
        return validateField(whiteList, field);
    }

    @Override
    public Boolean visitExtendedMatchFilter(Set<JsonPointer> whiteList, JsonPointer field, String operator,
                Object valueAssertion) {
        return false;
    }

    @Override
    public Boolean visitGreaterThanFilter(Set<JsonPointer> whiteList, JsonPointer field, Object valueAssertion) {
        return false;
    }

    @Override
    public Boolean visitGreaterThanOrEqualToFilter(Set<JsonPointer> whiteList, JsonPointer field,
                Object valueAssertion) {
        return false;
    }

    @Override
    public Boolean visitLessThanFilter(Set<JsonPointer> whiteList, JsonPointer field, Object valueAssertion) {
        return false;
    }

    @Override
    public Boolean visitLessThanOrEqualToFilter(Set<JsonPointer> whiteList, JsonPointer field, Object valueAssertion) {
        return false;
    }

    @Override
    public Boolean visitNotFilter(Set<JsonPointer> whiteList, QueryFilter<JsonPointer> queryFilter) {
        return queryFilter.accept(this, whiteList);
    }

    @Override
    public Boolean visitOrFilter(Set<JsonPointer> whiteList, List<QueryFilter<JsonPointer>> subFilters) {
        return validateFields(whiteList, subFilters);
    }

    @Override
    public Boolean visitPresentFilter(Set<JsonPointer> whiteList, JsonPointer field) {
        return false;
    }

    @Override
    public Boolean visitStartsWithFilter(Set<JsonPointer> whiteList, JsonPointer field, Object valueAssertion) {
        return false;
    }

    private Boolean validateFields(Set<JsonPointer> whiteList, List<QueryFilter<JsonPointer>> subFilters) {
        for (QueryFilter<JsonPointer> filter : subFilters) {
            if (!filter.accept(this, whiteList)) {
                return false;
            }
        }
        return true;
    }

    private Boolean validateField(Set<JsonPointer> whiteList, JsonPointer field) {
        return whiteList.contains(field);
    }

}
