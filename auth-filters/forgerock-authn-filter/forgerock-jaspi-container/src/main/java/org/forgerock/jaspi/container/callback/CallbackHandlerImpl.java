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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.jaspi.container.callback;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.callback.CertStoreCallback;
import javax.security.auth.message.callback.GroupPrincipalCallback;
import javax.security.auth.message.callback.PasswordValidationCallback;
import javax.security.auth.message.callback.PrivateKeyCallback;
import javax.security.auth.message.callback.SecretKeyCallback;
import javax.security.auth.message.callback.TrustStoreCallback;
import java.security.Principal;
import java.util.Set;

/**
 * Default CallbackHandler for Authentication modules to use to request more information about the request and
 * response messages from the JASPI Container.
 */
public class CallbackHandlerImpl implements CallbackHandler {

    /**
     * Called by Authentication modules to request more information about the request and response message.
     * <p>
     * Callbacks currently supported are as follows:
     * <ul>
     *     <li>CallerPrincipalCallback</li>
     *     <li>GroupPrincipalCallback</li>
     * </ul>
     * <p>
     * This method will handle a CallerPrincipalCallback by creating a Principal from the name stored in the Callback
     * and adding it to the Subject from the Callback. If the name is not populated then the Principal stored in the
     * Callback will be added to the Subject instead.
     * <p>
     * This method will handle a GroupPrincipalCallback by create a Principal for each group stored in the Callback
     * and adding them to the Subject from the Callback.
     *
     * @param callbacks An array of Callback objects provided by the Authentication modules.
     * @throws UnsupportedCallbackException If a callback is passed which is not supported by this CallbackHandler.
     */
    public void handle(Callback[] callbacks) throws UnsupportedCallbackException {

        for (Callback callback : callbacks) {

            if (CallerPrincipalCallback.class.isAssignableFrom(callback.getClass())) {
                CallerPrincipalCallback callerPrincipalCallback = (CallerPrincipalCallback) callback;

                Subject subject = callerPrincipalCallback.getSubject();

                final String name = callerPrincipalCallback.getName();
                Principal principal = callerPrincipalCallback.getPrincipal();

                if (name != null) {
                    subject.getPrincipals().add(new Principal() {
                        @Override
                        public String getName() {
                            return name;
                        }
                    });
                } else {
                    subject.getPrincipals().add(principal);
                }

            } else if (GroupPrincipalCallback.class.isAssignableFrom(callback.getClass())) {

                GroupPrincipalCallback groupPrincipalCallback = (GroupPrincipalCallback) callback;

                Subject subject = groupPrincipalCallback.getSubject();
                String[] groups = groupPrincipalCallback.getGroups();
                if (groups == null) {
                    return;
                }

                Set<Principal> principals = subject.getPrincipals();

                for (final String group : groups) {
                    principals.add(new Principal() {
                        @Override
                        public String getName() {
                            return group;
                        }
                    });
                }

            } else if (PasswordValidationCallback.class.isAssignableFrom(callback.getClass())) {
                // JSR-196 Spec states this MUST be implemented but as this is not actually a "real" container
                // we don't need to do this here.
                throw new UnsupportedCallbackException(callback);
            } else if (CertStoreCallback.class.isAssignableFrom(callback.getClass())) {
                //SHOULD implement
                throw new UnsupportedCallbackException(callback);
            } else if (PrivateKeyCallback.class.isAssignableFrom(callback.getClass())) {
                //SHOULD implement
                throw new UnsupportedCallbackException(callback);
            } else if (SecretKeyCallback.class.isAssignableFrom(callback.getClass())) {
                //SHOULD implement
                throw new UnsupportedCallbackException(callback);
            } else if (TrustStoreCallback.class.isAssignableFrom(callback.getClass())) {
                //SHOULD implement
                throw new UnsupportedCallbackException(callback);
//            } else if (HttpCallback.class.isAssignableFrom(callback.getClass())) {
                //SHOULD implement
//                throw new UnsupportedCallbackException(callback);
            }
        }
    }
}
