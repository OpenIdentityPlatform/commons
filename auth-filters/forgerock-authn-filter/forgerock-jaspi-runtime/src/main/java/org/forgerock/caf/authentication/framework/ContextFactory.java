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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.caf.authentication.framework;

import javax.security.auth.message.config.ServerAuthContext;

/**
 * A factory which is responsible for creating {@code ServerAuthContext}s for authenticating requests.
 */
public interface ContextFactory {

    /**
     * <p>Returns the context which should be used to authenticate requests.</p>
     *
     * <p>This method is called for each request. This means that the {@code ServerAuthContext} instance can be
     * changed/updated with new ServerAuthModules and any changes will be picked up by the runtime for subsequent
     * authentication requests.</p>
     *
     * @return The context which should be used for authenticating requests.
     */
    ServerAuthContext getContext();
}
