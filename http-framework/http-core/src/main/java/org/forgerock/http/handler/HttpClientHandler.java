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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.http.handler;

import static org.forgerock.util.time.Duration.duration;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

import org.forgerock.services.context.Context;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.io.IO;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.spi.HttpClient;
import org.forgerock.http.spi.HttpClientProvider;
import org.forgerock.http.spi.Loader;
import org.forgerock.util.Factory;
import org.forgerock.util.Option;
import org.forgerock.util.Options;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.time.Duration;

/**
 * An HTTP client for sending requests to remote servers.
 */
public final class HttpClientHandler implements Handler, Closeable {

    /**
     * The TCP connect timeout for new HTTP connections. The default timeout is
     * 10 seconds.
     */
    public static final Option<Duration> OPTION_CONNECT_TIMEOUT = Option.withDefault(duration("10 seconds"));

    /**
     * The TCP socket timeout when waiting for HTTP responses. The default
     * timeout is 10 seconds.
     */
    public static final Option<Duration> OPTION_SO_TIMEOUT = Option.withDefault(duration("10 seconds"));

    /**
     * Specifies whether HTTP connections should be kept alive an reused for
     * additional requests. By default, connections will be reused if possible.
     */
    public static final Option<Boolean> OPTION_REUSE_CONNECTIONS = Option.withDefault(true);

    /**
     * Specifies whether requests should be retried if a failure is detected. By
     * default requests will be retried.
     */
    public static final Option<Boolean> OPTION_RETRY_REQUESTS = Option.withDefault(true);

    /**
     * Specifies the list of key managers that should be used when configuring
     * SSL/TLS connections. By default the system key manager(s) will be used.
     */
    public static final Option<KeyManager[]> OPTION_KEY_MANAGERS = Option.of(KeyManager[].class, null);

    /**
     * The strategy which should be used for loading the
     * {@link HttpClientProvider}. By default, the provider will be loaded using
     * a {@code ServiceLoader}.
     *
     * @see Loader#SERVICE_LOADER
     */
    public static final Option<Loader> OPTION_LOADER = Option.of(Loader.class, Loader.SERVICE_LOADER);

    /**
     * Specifies the maximum number of connections that should be pooled by the
     * HTTP client. At most 64 connections will be cached by default.
     */
    public static final Option<Integer> OPTION_MAX_CONNECTIONS = Option.withDefault(64);

    /**
     * Specifies the temporary storage that should be used for storing HTTP
     * responses. By default {@link IO#newTemporaryStorage()} is used.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final Option<Factory<Buffer>> OPTION_TEMPORARY_STORAGE = (Option) Option.of(
            Factory.class, IO.newTemporaryStorage());

    /**
     * Specifies the list of trust managers that should be used when configuring
     * SSL/TLS connections. By default the system trust manager(s) will be used.
     */
    public static final Option<TrustManager[]> OPTION_TRUST_MANAGERS = Option.of(TrustManager[].class, null);

    /** The client implementation. */
    private final HttpClient httpClient;

    /**
     * SSL host name verification policies.
     */
    public enum HostnameVerifier {
        /**
         * Accepts any host name (disables host name verification).
         */
        ALLOW_ALL,

        /**
         * Requires that the host name matches the host name presented in the
         * certificate. Wild-cards only match a single domain.
         */
        STRICT;
    }

    /**
     * Specifies the SSL host name verification policy. The default is to allow
     * all host names.
     */
    public static final Option<HostnameVerifier> OPTION_HOSTNAME_VERIFIER = Option.of(
            HostnameVerifier.class, HostnameVerifier.ALLOW_ALL);

    /**
     * SSLContext algorithm to be used when making SSL/TLS connections.
     */
    public static final Option<String> OPTION_SSLCONTEXT_ALGORITHM = Option.withDefault("TLS");

    /**
     * List of SSL protocols to be enabled on the HttpClient.
     * Defaults to the list of SSL protocols supported by the Java runtime.
     *
     * @see <a href="http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#jssenames">
     *     JDK 7 Supported Protocols</a>
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static final Option<List<String>> OPTION_SSL_ENABLED_PROTOCOLS =
            (Option) Option.of(List.class, Collections.<String>emptyList());

    /**
     * List of JSSE ciphers to be enabled on the HttpClient.
     * Defaults to the list of ciphers supported by the Java runtime.
     *
     * @see <a href="http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#ciphersuites">
     *     JDK 7 Cipher Suite Names</a>
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static final Option<List<String>> OPTION_SSL_CIPHER_SUITES =
            (Option) Option.of(List.class, Collections.<String>emptyList());

    /**
     * Creates a new HTTP client using default client options. The returned
     * client must be closed when it is no longer needed by the application.
     *
     * @throws HttpApplicationException
     *             If no client provider could be found.
     */
    public HttpClientHandler() throws HttpApplicationException {
        this(Options.unmodifiableDefaultOptions());
    }

    /**
     * Creates a new HTTP client using the provided client options. The returned
     * client must be closed when it is no longer needed by the application.
     *
     * @param options
     *            The options which will be used to configure the client.
     * @throws HttpApplicationException
     *             If no client provider could be found, or if the client could
     *             not be configured using the provided set of options.
     * @throws NullPointerException
     *             If {@code options} was {@code null}.
     */
    public HttpClientHandler(final Options options) throws HttpApplicationException {
        Reject.ifNull(options);
        final Loader loader = options.get(OPTION_LOADER);
        final HttpClientProvider factory = loader.load(HttpClientProvider.class, options);
        if (factory == null) {
            throw new HttpApplicationException("No HTTP client provider found");
        }
        this.httpClient = factory.newHttpClient(options);
    }

    /**
     * Completes all pending requests and release resources associated with
     * underlying implementation.
     *
     * @throws IOException
     *             if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        httpClient.close();
    }

    /**
     * Sends an HTTP request to a remote server and returns a {@code Promise}
     * representing the asynchronous response.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
        return httpClient.sendAsync(request);
    }
}
