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

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.forgerock.http.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.util.encode.Base64;

/**
 * ClientInfo gives easy access to client-related information that are available into the request.
 * Supported data includes:
 * <ul>
 *     <li>Remote IP address or hostname</li>
 *     <li>Remote port</li>
 *     <li>Username</li>
 *     <li>Client provided certificates</li>
 *     <li>User-Agent information</li>
 * </ul>
 */
public final class ClientInfoContext extends AbstractContext implements ClientInfo {

    // Persisted attribute names
    private static final String ATTR_REMOTE_USER = "remoteUser";
    private static final String ATTR_REMOTE_ADDRESS = "remoteAddress";
    private static final String ATTR_REMOTE_HOST = "remoteHost";
    private static final String ATTR_REMOTE_PORT = "remotePort";
    private static final String ATTR_CERTIFICATES = "certificates";
    private static final String ATTR_USER_AGENT = "userAgent";

    private static final String X509_TYPE = "X.509";
    private static final String BEGIN_CERTIFICATE = "-----BEGIN CERTIFICATE-----";
    private static final String END_CERTIFICATE = "-----END CERTIFICATE-----";

    private final Collection<? extends Certificate> certificates;

    private ClientInfoContext(Context parent,
                              String remoteUser,
                              String remoteAddress,
                              String remoteHost,
                              int remotePort,
                              List<? extends Certificate> certificates,
                              String userAgent) {
        super(parent, "clientInfo");
        data.put(ATTR_REMOTE_USER, remoteUser);
        data.put(ATTR_REMOTE_ADDRESS, remoteAddress);
        data.put(ATTR_REMOTE_HOST, remoteHost);
        data.put(ATTR_REMOTE_PORT, remotePort);
        // maintain the real list of certificates for Java API
        this.certificates = Collections.unmodifiableCollection(certificates);
        // store Base64-encoded certificates for JSON serialization
        StringBuilder builder = new StringBuilder();
        for (final Certificate certificate : certificates) {
            try {
                builder.append(BEGIN_CERTIFICATE)
                        .append(Base64.encode(certificate.getEncoded()))
                        .append(END_CERTIFICATE);
            } catch (CertificateEncodingException e) {
                throw new IllegalStateException("Unable to serialize certificates", e);
            }
        }
        data.put(ATTR_CERTIFICATES, builder.toString());
        data.put(ATTR_USER_AGENT, userAgent);
    }

    /**
     * Restore from JSON representation.
     *
     * @param savedContext
     *            The JSON representation from which this context's attributes
     *            should be parsed.
     * @param classLoader
     *            The ClassLoader which can properly resolve the persisted class-name.
     */
    public ClientInfoContext(final JsonValue savedContext, final ClassLoader classLoader) {
        super(savedContext, classLoader);
        try {
            this.certificates = Collections.unmodifiableCollection(
                    CertificateFactory.getInstance(X509_TYPE).generateCertificates(
                            new ByteArrayInputStream(data.get(ATTR_CERTIFICATES).asString().getBytes("UTF8"))));
        } catch (CertificateException | UnsupportedEncodingException e) {
            throw new IllegalStateException("Unable to deserialize certificates", e);
        }
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
        private List<? extends Certificate> certificates;
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
        public ClientInfoContextBuilder certificates(Certificate... certificates) {
            if (certificates != null) {
                return certificates(asList(certificates));
            } else {
                return certificates(Collections.<Certificate>emptyList());
            }
        }

        /**
         * Sets the client's certificates.
         *
         * @param certificates The {@code List} of certificates.
         * @return The builder instance.
         * @see #certificates(Certificate...)
         */
        public ClientInfoContextBuilder certificates(List<Certificate> certificates) {
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
        return data.get(ATTR_REMOTE_USER).asString();
    }

    /**
     * Returns the IP address of the client (or last proxy) that sent the request.
     *
     * @return the IP address of the client (or last proxy) that sent the request.
     */
    @Override
    public String getRemoteAddress() {
        return data.get(ATTR_REMOTE_ADDRESS).asString();
    }

    /**
     * Returns the fully qualified name of the client (or last proxy) that sent the request.
     *
     * @return the fully qualified name of the client (or last proxy) that sent the request.
     */
    @Override
    public String getRemoteHost() {
        return data.get(ATTR_REMOTE_HOST).asString();
    }

    /**
     * Returns the source port of the client (or last proxy) that sent the request.
     *
     * @return the source port of the client (or last proxy) that sent the request.
     */
    @Override
    public int getRemotePort() {
        return data.get(ATTR_REMOTE_PORT).asInteger();
    }

    /**
     * Returns the list (possibly empty) of certificate(s) provided by the client.
     * If no certificates are available, an empty list is returned.
     *
     * @return the list (possibly empty) of certificate(s) provided by the client.
     */
    @Override
    public Collection<? extends Certificate> getCertificates() {
        return certificates;
    }

    /**
     * Returns the value of the {@literal User-Agent} HTTP Header (if any, returns {@code null} otherwise).
     *
     * @return the value of the {@literal User-Agent} HTTP Header (if any, returns {@code null} otherwise).
     */
    @Override
    public String getUserAgent() {
        return data.get(ATTR_USER_AGENT).asString();
    }
}
