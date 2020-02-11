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

package org.forgerock.json.resource;

/**
 * An enum of count policy types.
 *
 * @see QueryRequest#setTotalPagedResultsPolicy(CountPolicy)
 * @see QueryResponse#getTotalPagedResultsPolicy()
 */
public enum CountPolicy {
    /**
     * There should be no count returned. No overhead should be incurred.
     */
    NONE,

    /**
     * Estimated count may be used. If no estimation is available it is up to the implementor whether to return
     * an {@link #EXACT} count or {@link #NONE}. It should be known to the client which was used as in
     * {@link QueryResponse#getTotalPagedResultsPolicy()}
     */
    ESTIMATE,

    /**
     * Exact count is required.
     */
    EXACT
}
