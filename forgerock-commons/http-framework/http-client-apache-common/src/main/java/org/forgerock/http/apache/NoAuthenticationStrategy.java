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

package org.forgerock.http.apache;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthOption;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.client.AuthenticationStrategy;
import org.apache.http.protocol.HttpContext;

/**
 * An authentication strategy that never performs authentication.
 */
public final class NoAuthenticationStrategy implements AuthenticationStrategy {

    /**
     * An {@link AuthenticationStrategy} singleton instance that never performs authentication.
     */
    public static final AuthenticationStrategy INSTANCE = new NoAuthenticationStrategy();

    /**
     * Singleton only.
     */
    private NoAuthenticationStrategy() { }

    @Override
    public void authFailed(final HttpHost host, final AuthScheme authScheme, final HttpContext context) {
        // Nothing to do.
    }

    @Override
    public void authSucceeded(final HttpHost host, final AuthScheme authScheme, final HttpContext context) {
        // Nothing to do.
    }

    @Override
    public Map<String, Header> getChallenges(final HttpHost host, final HttpResponse response,
            final HttpContext context) throws MalformedChallengeException {
        return Collections.emptyMap();
    }

    @Override
    public boolean isAuthenticationRequested(final HttpHost host, final HttpResponse response,
            final HttpContext context) {
        return false;
    }

    @Override
    public Queue<AuthOption> select(final Map<String, Header> challenges, final HttpHost host,
            final HttpResponse response, final HttpContext context) throws MalformedChallengeException {
        return new LinkedList<>();
    }
}
