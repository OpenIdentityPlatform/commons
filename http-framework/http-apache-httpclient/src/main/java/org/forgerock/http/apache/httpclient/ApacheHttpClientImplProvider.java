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
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2009 Sun Microsystems Inc.
 * Portions Copyright 2010–2011 ApexIdentity Inc.
 * Portions Copyright 2011-2015 ForgeRock AS.
 */

package org.forgerock.http.apache.httpclient;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthOption;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.client.AuthenticationStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.forgerock.http.Client;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.spi.ClientImpl;
import org.forgerock.http.spi.ClientImplProvider;
import org.forgerock.http.util.Duration;
import org.forgerock.http.util.Options;
import org.forgerock.util.Factory;

/**
 * An HTTP client implementation provider for Apache HttpClient.
 */
public final class ApacheHttpClientImplProvider implements ClientImplProvider {
    /**
     * An authentication strategy that never performs authentication.
     */
    private static final AuthenticationStrategy NO_AUTH = new AuthenticationStrategy() {

        @Override
        public void authFailed(final HttpHost authhost, final AuthScheme authScheme,
                final HttpContext context) {
            // Nothing to do.
        }

        @Override
        public void authSucceeded(final HttpHost authhost, final AuthScheme authScheme,
                final HttpContext context) {
            // Nothing to do.
        }

        @Override
        public Map<String, Header> getChallenges(final HttpHost authhost,
                final HttpResponse response, final HttpContext context)
                throws MalformedChallengeException {
            return Collections.emptyMap();
        }

        @Override
        public boolean isAuthenticationRequested(final HttpHost authhost,
                final HttpResponse response, final HttpContext context) {
            return false;
        }

        @Override
        public Queue<AuthOption> select(final Map<String, Header> challenges,
                final HttpHost authhost, final HttpResponse response, final HttpContext context)
                throws MalformedChallengeException {
            return new LinkedList<AuthOption>();
        }
    };

    @Override
    public ClientImpl newClientImpl(final Options options) throws HttpApplicationException {
        final Factory<Buffer> storage = options.get(Client.OPTION_TEMPORARY_STORAGE);

        final HttpClientBuilder builder = HttpClientBuilder.create();

        // Connection pooling.
        final int maxConnections = options.get(Client.OPTION_MAX_CONNECTIONS);
        builder.setMaxConnTotal(maxConnections);
        builder.setMaxConnPerRoute(maxConnections);
        if (!options.get(Client.OPTION_REUSE_CONNECTIONS)) {
            builder.setConnectionReuseStrategy(NoConnectionReuseStrategy.INSTANCE);
        }
        if (!options.get(Client.OPTION_RETRY_REQUESTS)) {
            builder.disableAutomaticRetries();
        }

        // Timeouts.
        final RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
        final Duration soTimeout = options.get(Client.OPTION_SO_TIMEOUT);
        requestConfigBuilder.setSocketTimeout(soTimeout.isUnlimited() ? 0 : (int) soTimeout
                .to(MILLISECONDS));
        final Duration connectTimeout = options.get(Client.OPTION_CONNECT_TIMEOUT);
        requestConfigBuilder.setConnectTimeout(connectTimeout.isUnlimited() ? 0
                : (int) connectTimeout.to(MILLISECONDS));
        builder.setDefaultRequestConfig(requestConfigBuilder.build());

        // FIXME: where is this setting in HttpClient 4.x?
        // HttpProtocolParams.setVersion(parameters, HttpVersion.HTTP_1_1);

        builder.disableRedirectHandling();

        // SSL
        final SSLContext context;
        try {
            context = SSLContext.getInstance("TLS");
            context.init(options.get(Client.OPTION_KEY_MANAGERS), options
                    .get(Client.OPTION_TRUST_MANAGERS), null);
        } catch (final GeneralSecurityException e) {
            throw new HttpApplicationException(e);
        }
        builder.setSslcontext(context);

        switch (options.get(Client.OPTION_HOSTNAME_VERIFIER)) {
        case ALLOW_ALL:
            builder.setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            break;
        case BROWSER_COMPATIBLE:
            builder.setHostnameVerifier(SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
            break;
        default:
            builder.setHostnameVerifier(SSLConnectionSocketFactory.STRICT_HOSTNAME_VERIFIER);
            break;
        }

        // FIXME: is this equivalent to original OpenIG config?
        builder.disableCookieManagement();
        builder.setProxyAuthenticationStrategy(NO_AUTH);
        builder.setTargetAuthenticationStrategy(NO_AUTH);

        return new ApacheHttpClientImpl(builder.build(), storage);
    }
}
