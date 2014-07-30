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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.caf.authn.test.runtime;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.forgerock.guice.core.InjectorHolder;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.config.ServerAuthContext;

/**
 * <p>A test implementation of a {@code ServerAuthContext} as the JASPI runtime offers no support for changing the
 * auth module configuration after it has been configured.</p>
 *
 * <p>To get around this, we wrap the configured {@code FallbackServerAuthContext} with this class and we re-read the
 * module configuration for each call to the {@code ServerAuthContext}.</p>
 *
 * @since 1.5.0
 */
public class ConfigurableServerAuthContext implements ServerAuthContext {

    /**
     * Gets the configured {@code ServerAuthContext} to delegate all calls to.
     *
     * @return
     */
    private ServerAuthContext getContext() {
        return InjectorHolder.getInstance(Key.get(ServerAuthContext.class, Names.named("ServerAuthContext")));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject)
            throws AuthException {
        return getContext().validateRequest(messageInfo, clientSubject, serviceSubject);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {
        return getContext().secureResponse(messageInfo, serviceSubject);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject clientSubject) throws AuthException {
        getContext().cleanSubject(messageInfo, clientSubject);
    }
}
