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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.jaspi.runtime.config;

import org.forgerock.jaspi.runtime.context.ContextHandler;
import org.forgerock.jaspi.utils.MessageInfoUtils;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.config.ServerAuthContext;

/**
 * Factory interface for getting the ServerAuthContext instance that the JaspiRuntime should be configured to use
 * to authenticate requests.
 *
 * @since 1.3.0
 */
public interface ServerContextFactory {

    /**
     * Returns the ServerAuthContext instance that the JaspiRuntime will be configured to use.
     *
     * @param messageInfoUtils An instance of the MessageInfoUtils.
     * @param handler An instance of a CallbackHandler.
     * @param contextHandler An instance of the ContextHandler.
     * @return A ServerAuthContext instance.
     * @throws AuthException If there is an error getting the ServerAuthContext instance.
     */
    ServerAuthContext getServerAuthContext(final MessageInfoUtils messageInfoUtils, final CallbackHandler handler,
            final ContextHandler contextHandler) throws AuthException;
}
