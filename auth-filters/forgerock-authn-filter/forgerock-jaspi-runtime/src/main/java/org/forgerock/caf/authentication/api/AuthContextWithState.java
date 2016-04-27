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

package org.forgerock.caf.authentication.api;

/**
 * <p>{@link org.forgerock.caf.authentication.api.AsyncServerAuthContext} implementations should
 * implement this interface when the {@code AsyncServerAuthContext} has its own implementation of
 * a {@link AuthenticationState} that it will be using to store and maintain state for a single
 * request.</p>
 *
 * <p>The {@link MessageContext} will use the {@link #createAuthenticationState()} method on this
 * interface to create a new instance of the {@code AuthenticationState} for each request message.
 * </p>
 *
 * @since 2.0.0
 */
public interface AuthContextWithState {

    /**
     * <p>Creates an instance of a specific type of {@code AuthenticationState}.</p>
     *
     * <p><strong>Must</strong> return a new {@code AuthenticationState} instance for each
     * invocation.</p>
     *
     * @return A new {@code AuthenticationState} instance.
     */
    AuthenticationState createAuthenticationState();
}
