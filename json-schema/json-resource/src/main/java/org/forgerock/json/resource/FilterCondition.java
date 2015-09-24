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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.json.resource;

import org.forgerock.services.context.Context;

/**
 * A condition which controls whether or not a filter will be invoked or not.
 *
 * @see Filters#conditionalFilter(FilterCondition, Filter)
 */
public interface FilterCondition {

    /**
     * Returns {@code true} if the conditional filter should be invoked, or
     * {@code false} if processing should continue directly to the next filter
     * in the filter chain.
     *
     * @param context
     *            The filter chain context.
     * @param request
     *            The request to be filtered.
     * @return {@code true} if the conditional filter should be invoked.
     */
    boolean matches(Context context, Request request);
}
