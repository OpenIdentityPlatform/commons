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

package org.forgerock.http.apache.sync;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.forgerock.http.handler.HttpClientHandler.OPTION_CONNECT_TIMEOUT;
import static org.forgerock.http.handler.HttpClientHandler.OPTION_HOSTNAME_VERIFIER;
import static org.forgerock.http.handler.HttpClientHandler.OPTION_KEY_MANAGERS;
import static org.forgerock.http.handler.HttpClientHandler.OPTION_MAX_CONNECTIONS;
import static org.forgerock.http.handler.HttpClientHandler.OPTION_RETRY_REQUESTS;
import static org.forgerock.http.handler.HttpClientHandler.OPTION_REUSE_CONNECTIONS;
import static org.forgerock.http.handler.HttpClientHandler.OPTION_SO_TIMEOUT;
import static org.forgerock.http.handler.HttpClientHandler.OPTION_SSLCONTEXT_ALGORITHM;
import static org.forgerock.http.handler.HttpClientHandler.OPTION_SSL_CIPHER_SUITES;
import static org.forgerock.http.handler.HttpClientHandler.OPTION_SSL_ENABLED_PROTOCOLS;
import static org.forgerock.http.handler.HttpClientHandler.OPTION_TEMPORARY_STORAGE;
import static org.forgerock.http.handler.HttpClientHandler.OPTION_TRUST_MANAGERS;
import static org.forgerock.http.util.Lists.asArrayOrNull;

import java.security.GeneralSecurityException;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.apache.NoAuthenticationStrategy;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.spi.HttpClient;
import org.forgerock.http.spi.HttpClientProvider;
import org.forgerock.util.Factory;
import org.forgerock.util.Options;
import org.forgerock.util.time.Duration;

/**
 * An HTTP client implementation provider for Apache HttpClient.
 */
public final class SyncHttpClientProvider implements HttpClientProvider {

    @Override
    public HttpClient newHttpClient(final Options options) throws HttpApplicationException {
        final Factory<Buffer> storage = options.get(OPTION_TEMPORARY_STORAGE);

        final HttpClientBuilder builder = HttpClientBuilder.create();

        // Connection pooling.
        final int maxConnections = options.get(OPTION_MAX_CONNECTIONS);
        builder.setMaxConnTotal(maxConnections);
        builder.setMaxConnPerRoute(maxConnections);
        if (!options.get(OPTION_REUSE_CONNECTIONS)) {
            builder.setConnectionReuseStrategy(NoConnectionReuseStrategy.INSTANCE);
        }
        if (!options.get(OPTION_RETRY_REQUESTS)) {
            builder.disableAutomaticRetries();
        }

        // Timeouts.
        final RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
        final Duration soTimeout = options.get(OPTION_SO_TIMEOUT);
        requestConfigBuilder.setSocketTimeout(soTimeout.isUnlimited() ? 0 : (int) soTimeout
                .to(MILLISECONDS));
        final Duration connectTimeout = options.get(OPTION_CONNECT_TIMEOUT);
        requestConfigBuilder.setConnectTimeout(connectTimeout.isUnlimited() ? 0
                : (int) connectTimeout.to(MILLISECONDS));
        builder.setDefaultRequestConfig(requestConfigBuilder.build());

        // FIXME: where is this setting in HttpClient 4.x?
        // HttpProtocolParams.setVersion(parameters, HttpVersion.HTTP_1_1);

        builder.disableRedirectHandling();

        // SSL
        final SSLContext context;
        try {
            context = SSLContext.getInstance(options.get(OPTION_SSLCONTEXT_ALGORITHM));
            context.init(options.get(OPTION_KEY_MANAGERS), options.get(OPTION_TRUST_MANAGERS), null);
        } catch (final GeneralSecurityException e) {
            throw new HttpApplicationException(e);
        }

        final HostnameVerifier hostnameVerifier;
        switch (options.get(OPTION_HOSTNAME_VERIFIER)) {
        case ALLOW_ALL:
            hostnameVerifier = NoopHostnameVerifier.INSTANCE;
            break;
        default:
            hostnameVerifier = new DefaultHostnameVerifier();
            break;
        }

        List<String> protocols = options.get(OPTION_SSL_ENABLED_PROTOCOLS);
        List<String> ciphers = options.get(OPTION_SSL_CIPHER_SUITES);

        builder.setSSLSocketFactory(new SSLConnectionSocketFactory(context, asArrayOrNull(protocols),
                asArrayOrNull(ciphers), hostnameVerifier));

        // FIXME: is this equivalent to original OpenIG config?
        builder.disableCookieManagement();
        builder.setProxyAuthenticationStrategy(NoAuthenticationStrategy.INSTANCE);
        builder.setTargetAuthenticationStrategy(NoAuthenticationStrategy.INSTANCE);

        return new SyncHttpClient(builder.build(), storage);
    }
}
