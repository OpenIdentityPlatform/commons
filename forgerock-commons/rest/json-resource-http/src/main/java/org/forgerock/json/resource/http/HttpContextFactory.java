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
 * Copyright 2012-2014 ForgeRock AS.
 */

package org.forgerock.json.resource.http;

import org.forgerock.services.context.Context;
import org.forgerock.json.resource.ResourceException;

/**
 * A factory which is responsible for creating new request {@code Context}s for
 * each JSON request. The returned context must include a
 * {@link org.forgerock.services.context.RootContext} as its root and may include
 * zero or more sub-contexts.
 * <p>
 * As an example, a context factory may return a context chain which includes
 * authentication state information.
 */
public interface HttpContextFactory {

    /**
     * Returns the context which should be used for the provided HTTP request.
     *
     * @param parent
     *            The parent context.
     * @param request
     *            The HTTP request which is about to be processed.
     * @return The context which should be used for the provided HTTP request.
     * @throws ResourceException
     *             If a request context could not be obtained.
     */
    Context createContext(Context parent, org.forgerock.http.protocol.Request request) throws ResourceException;
}
