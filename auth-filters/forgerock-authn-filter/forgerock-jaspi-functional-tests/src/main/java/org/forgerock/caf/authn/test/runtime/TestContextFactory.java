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

package org.forgerock.caf.authn.test.runtime;

import javax.inject.Singleton;
import javax.security.auth.message.config.ServerAuthContext;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.forgerock.caf.authentication.framework.ContextFactory;
import org.forgerock.guice.core.InjectorHolder;

/**
 * <p>Test implementation of the Jaspi runtime's {@code ContextFactory} interface.</p>
 *
 * <p>Each time the {@link #getContext()} method is called it asks Guice for an instance, so that changes made to the
 * auth module configuration are picked up for the next authentication request.</p>
 *
 * @since 2.0.0
 */
@Singleton
public class TestContextFactory implements ContextFactory {

    /**
     * Factory method called by the Jaspi runtime to get the instance of the {@code ContextFactory} that will be used to
     * get the {@link javax.security.auth.message.config.ServerAuthContext} instance that will be used to authenticate
     * requests.
     *
     * @return An instance of the {@code TestContextFactory}.
     */
    public static ContextFactory getContextFactory() {
        return InjectorHolder.getInstance(ContextFactory.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServerAuthContext getContext() {
        return InjectorHolder.getInstance(Key.get(ServerAuthContext.class, Names.named("ServerAuthContext")));
    }
}
