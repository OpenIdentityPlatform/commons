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

package org.forgerock.caf.authn.test.modules;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.module.ServerAuthModule;
import java.util.Map;

/**
 * A test auth module in which the {@link #validateRequest(MessageInfo, Subject, Subject)} and
 * {@link #secureResponse(MessageInfo, Subject)} methods return values can be decided based on the value of two request
 * headers.
 *
 * @since 1.5.0
 */
public class AuthModuleUnsupportedMessageTypes implements ServerAuthModule {

    /**
     * Does nothing.
     *
     * @param requestMessagePolicy {@inheritDoc}
     * @param responseMessagePolicy {@inheritDoc}
     * @param callbackHandler {@inheritDoc}
     * @param config {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void initialize(MessagePolicy requestMessagePolicy, MessagePolicy responseMessagePolicy,
            CallbackHandler callbackHandler, Map config) {
    }

    /**
     * Returns the {@code String} and {@code Integer} classes, to force an unsupported message types scenario.
     *
     * @return {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Class[] getSupportedMessageTypes() {
        return new Class[]{String.class, Integer.class};
    }

    /**
     * Returns SUCCESS.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject) {
        return AuthStatus.SUCCESS;
    }

    /**
     * Returns SEND_SUCCESS.
     *
     * @param messageInfo {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) {
        return AuthStatus.SEND_SUCCESS;
    }

    /**
     * Does nothing.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     */
    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject clientSubject) {
    }
}
