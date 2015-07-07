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

package org.forgerock.http.context;

import static java.util.Arrays.asList;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

import org.forgerock.http.Context;

/**
 * ClientInfo gives easy access to client-related information that are available into the request.
 * Supported data includes:
 * <ul>
 *     <li>Remote IP address or hostname</li>
 *     <li>Remote port</li>
 *     <li>Username</li>
 *     <li>Client provided X509 certificates</li>
 *     <li>User-Agent information</li>
 * </ul>
 */
public final class ClientInfoContext extends ServerContext implements ClientInfo {

    private final String remoteUser;
    private final String remoteAddress;
    private final String remoteHost;
    private final int remotePort;
    private final List<X509Certificate> certificates;
    private final String userAgent;

    private ClientInfoContext(Context parent,
                              String remoteUser,
                              String remoteAddress,
                              String remoteHost,
                              int remotePort,
                              List<X509Certificate> certificates,
                              String userAgent) {
        super(parent, "clientInfo");
        this.remoteUser = remoteUser;
        this.remoteAddress = remoteAddress;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        this.certificates = Collections.unmodifiableList(certificates);
        this.userAgent = userAgent;
    }

    /**
     * Builder for creating {@code ClientInfoContext} instances.
     */
    public final static class ClientInfoContextBuilder {

        private final Context parent;
        private String remoteUser;
        private String remoteAddress;
        private String remoteHost;
        private int remotePort;
        private List<X509Certificate> certificates;
        private String userAgent;

        private ClientInfoContextBuilder(Context parent) {
            this.parent = parent;
        }

        /**
         * Sets the client's remote user.
         *
         * @param remoteUser The remote user.
         * @return The builder instance.
         */
        public ClientInfoContextBuilder remoteUser(String remoteUser) {
            this.remoteUser = remoteUser;
            return this;
        }

        /**
         * Sets the client's remote address.
         *
         * @param remoteAddress The remove address.
         * @return The builder instance.
         */
        public ClientInfoContextBuilder remoteAddress(String remoteAddress) {
            this.remoteAddress = remoteAddress;
            return this;
        }

        /**
         * Sets the client's remote host.
         *
         * @param remoteHost The remote host.
         * @return The builder instance.
         */
        public ClientInfoContextBuilder remoteHost(String remoteHost) {
            this.remoteHost = remoteHost;
            return this;
        }

        /**
         * Sets the client's remote port.
         *
         * @param remotePort The remote port.
         * @return The builder instance.
         */
        public ClientInfoContextBuilder remotePort(int remotePort) {
            this.remotePort = remotePort;
            return this;
        }

        /**
         * Sets the client's certificates.
         *
         * @param certificates The list of certificates.
         * @return The builder instance.
         * @see #certificates(List)
         */
        public ClientInfoContextBuilder certificates(X509Certificate... certificates) {
            if (certificates != null) {
                return certificates(asList(certificates));
            } else {
                return certificates(Collections.<X509Certificate>emptyList());
            }
        }

        /**
         * Sets the client's certificates.
         *
         * @param certificates The {@code List} of certificates.
         * @return The builder instance.
         * @see #certificates(X509Certificate...)
         */
        public ClientInfoContextBuilder certificates(List<X509Certificate> certificates) {
            this.certificates = certificates;
            return this;
        }

        /**
         * Sets the client's user agent.
         *
         * @param userAgent The user agent.
         * @return The builder instance.
         */
        public ClientInfoContextBuilder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        /**
         * Creates a {@code ClientInfoContext} instance from the specifed properties.
         *
         * @return A {@code ClientInfoContext} instance.
         */
        public ClientInfoContext build() {
            return new ClientInfoContext(parent, remoteUser, remoteAddress, remoteHost, remotePort, certificates,
                    userAgent);
        }
    }

    /**
     * Creates a {@code ClientInfoContextBuilder} for creating {@code ClientInfoContext} instances.
     *
     * @param parent The parent context.
     * @return A builder for a {@code ClientInfoContext} instance.
     */
    public static ClientInfoContextBuilder builder(Context parent) {
        return new ClientInfoContextBuilder(parent);
    }

    /**
     * Returns the login of the user making this request or {@code null} if not known.
     *
     * @return the login of the user making this request or {@code null} if not known.
     */
    @Override
    public String getRemoteUser() {
        return remoteUser;
    }

    /**
     * Returns the IP address of the client (or last proxy) that sent the request.
     *
     * @return the IP address of the client (or last proxy) that sent the request.
     */
    @Override
    public String getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * Returns the fully qualified name of the client (or last proxy) that sent the request.
     *
     * @return the fully qualified name of the client (or last proxy) that sent the request.
     */
    @Override
    public String getRemoteHost() {
        return remoteHost;
    }

    /**
     * Returns the source port of the client (or last proxy) that sent the request.
     *
     * @return the source port of the client (or last proxy) that sent the request.
     */
    @Override
    public int getRemotePort() {
        return remotePort;
    }

    /**
     * Returns the list (possibly empty) of X509 certificate(s) provided by the client.
     * If no certificates are available, an empty list is returned.
     *
     * @return the list (possibly empty) of X509 certificate(s) provided by the client.
     */
    @Override
    public List<X509Certificate> getCertificates() {
        return certificates;
    }

    /**
     * Returns the value of the {@literal User-Agent} HTTP Header (if any, returns {@code null} otherwise).
     *
     * @return the value of the {@literal User-Agent} HTTP Header (if any, returns {@code null} otherwise).
     */
    @Override
    public String getUserAgent() {
        return userAgent;
    }
}
