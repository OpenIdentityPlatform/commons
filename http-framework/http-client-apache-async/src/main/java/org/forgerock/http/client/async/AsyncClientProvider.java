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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.http.client.async;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.forgerock.http.HttpClientHandler.*;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.security.GeneralSecurityException;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.protocol.HttpContext;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.client.ahc.NoAuthenticationStrategy;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.spi.ClientImpl;
import org.forgerock.http.spi.ClientImplProvider;
import org.forgerock.util.Factory;
import org.forgerock.util.Option;
import org.forgerock.util.Options;
import org.forgerock.util.time.Duration;

/**
 * Creates and configures a {@link ClientImpl} instance built around Apache HTTP Async Client component.
 *
 * @see <a href="https://hc.apache.org/httpcomponents-asyncclient-dev/index.html">Apache HTTP Async Client</a>
 */
public class AsyncClientProvider implements ClientImplProvider {

    /**
     * Specify the number of worker threads. If not set, the async client implementation manages this setting itself
     * (by default this is number of CPU + 1).
     */
    public static final Option<Integer> OPTION_WORKER_THREADS = Option.of(Integer.class, null);

    /**
     * A redirect strategy that never performs a redirect.
     */
    private static final RedirectStrategy DISABLE_REDIRECT = new RedirectStrategy() {
        @Override
        public boolean isRedirected(final HttpRequest request, final HttpResponse response,
                final HttpContext context) throws ProtocolException {
            return false;
        }

        @Override
        public HttpUriRequest getRedirect(final HttpRequest request, final HttpResponse response,
                final HttpContext context) throws ProtocolException {
            return null;
        }
    };

    @Override
    public ClientImpl newClientImpl(final Options options) throws HttpApplicationException {

        final Factory<Buffer> storage = options.get(OPTION_TEMPORARY_STORAGE);

        // SSL
        final SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(options.get(OPTION_KEY_MANAGERS),
                            options.get(OPTION_TRUST_MANAGERS), null);
        } catch (final GeneralSecurityException e) {
            throw new HttpApplicationException("Can't create SSL Context", e);
        }

        HostnameVerifier verifier = new DefaultHostnameVerifier();
        switch (options.get(OPTION_HOSTNAME_VERIFIER)) {
        case ALLOW_ALL:
            verifier = NoopHostnameVerifier.INSTANCE;
            break;
        }

        // Create a registry of custom connection session strategies for supported protocol schemes
        Registry<SchemeIOSessionStrategy> registry =
                RegistryBuilder.<SchemeIOSessionStrategy>create()
                        .register("http", NoopIOSessionStrategy.INSTANCE)
                        .register("https", new SSLIOSessionStrategy(sslContext, verifier))
                        .build();

        // Timeouts
        final Duration soTimeout = options.get(OPTION_SO_TIMEOUT);
        final Duration connectTimeout = options.get(OPTION_CONNECT_TIMEOUT);
        // FIXME GSA Can we support requestConnectTimeout ?

        // Create I/O reactor configuration
        IOReactorConfig.Builder reactorBuilder = IOReactorConfig.custom();
        if (!connectTimeout.isUnlimited()) {
            reactorBuilder.setConnectTimeout((int) connectTimeout.to(MILLISECONDS));
        }
        if (!soTimeout.isUnlimited()) {
            reactorBuilder.setSoTimeout((int) soTimeout.to(MILLISECONDS));
        }
        Integer threadCount = options.get(OPTION_WORKER_THREADS);
        if (threadCount != null) {
            reactorBuilder.setIoThreadCount(threadCount);
        }
        IOReactorConfig ioReactorConfig = reactorBuilder.build();

        // Create a custom I/O reactor
        ConnectingIOReactor reactor;
        try {
            reactor = new DefaultConnectingIOReactor(ioReactorConfig);
        } catch (IOReactorException e) {
            throw new HttpApplicationException("Cannot create I/O Reactor", e);
        }

        // Create a connection manager with custom configuration.
        PoolingNHttpClientConnectionManager manager = new PoolingNHttpClientConnectionManager(reactor, registry);

        // Connection pooling
        final int maxConnections = options.get(OPTION_MAX_CONNECTIONS);
        manager.setMaxTotal(maxConnections);
        manager.setDefaultMaxPerRoute(maxConnections);

        // FIXME GSA Couldn't find how to configure retries in async http client
        //if (!options.get(OPTION_RETRY_REQUESTS)) {
        //    builder.disableAutomaticRetries();
        //}

        // Create a client with the given custom dependencies and configuration.
        HttpAsyncClientBuilder builder = HttpAsyncClients.custom();

        if (!options.get(OPTION_REUSE_CONNECTIONS)) {
            builder.setConnectionReuseStrategy(NoConnectionReuseStrategy.INSTANCE);
        }

        // TODO Uncomment when we'll have a user-agent Option
        // builder.setUserAgent("CHF/1.0");

        CloseableHttpAsyncClient client = builder.setConnectionManager(manager)
                .disableCookieManagement()
                .setRedirectStrategy(DISABLE_REDIRECT)
                .setTargetAuthenticationStrategy(NoAuthenticationStrategy.INSTANCE)
                .setProxyAuthenticationStrategy(NoAuthenticationStrategy.INSTANCE)
                .build();
        client.start();
        return new AsyncClient(client, storage);
    }
}
