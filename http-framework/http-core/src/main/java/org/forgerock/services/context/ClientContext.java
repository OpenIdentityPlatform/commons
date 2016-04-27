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

package org.forgerock.services.context;

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

import org.forgerock.json.JsonValue;
import org.forgerock.util.encode.Base64;

/**
 * Client context gives easy access to client-related information that are available into the request.
 * Supported data includes:
 * <ul>
 *     <li>Remote IP address</li>
 *     <li>Remote port</li>
 *     <li>Username</li>
 *     <li>Client provided certificates</li>
 *     <li>User-Agent information</li>
 *     <li>Whether the client is external</li>
 *     <li>Whether the connection to the client is secure</li>
 *     <li>Local port</li>
 *     <li>Local address</li>
 * </ul>
 */
public final class ClientContext extends AbstractContext {

    // Persisted attribute names
    private static final String ATTR_REMOTE_USER = "remoteUser";
    private static final String ATTR_REMOTE_ADDRESS = "remoteAddress";
    private static final String ATTR_REMOTE_PORT = "remotePort";
    private static final String ATTR_CERTIFICATES = "certificates";

    private static final String ATTR_USER_AGENT = "userAgent";
    private static final String ATTR_IS_SECURE = "isSecure";
    private static final String ATTR_IS_EXTERNAL = "isExternal";

    private static final String X509_TYPE = "X.509";
    private static final String BEGIN_CERTIFICATE = "-----BEGIN CERTIFICATE-----";
    private static final String END_CERTIFICATE = "-----END CERTIFICATE-----";

    private static final String ATTR_LOCAL_ADDRESS = "localAddress";
    private static final String ATTR_LOCAL_PORT = "localPort";

    /** Builder for creating {@code ClientContext} instances. */
    public final static class Builder {
        private final Context parent;
        private String remoteUser = "";
        private String remoteAddress = "";
        private int remotePort = -1;
        private List<? extends Certificate> certificates = Collections.emptyList();
        private String userAgent = "";
        private boolean isSecure;
        private String localAddress = "";
        private int localPort = -1;

        private Builder(Context parent) {
            this.parent = parent;
        }

        /**
         * Sets the client's remote user.
         *
         * @param remoteUser The remote user.
         * @return The builder instance.
         */
        public Builder remoteUser(String remoteUser) {
            this.remoteUser = remoteUser;
            return this;
        }

        /**
         * Sets the client's remote address.
         *
         * @param remoteAddress The remove address.
         * @return The builder instance.
         */
        public Builder remoteAddress(String remoteAddress) {
            this.remoteAddress = remoteAddress;
            return this;
        }

        /**
         * Sets the client's remote port.
         *
         * @param remotePort The remote port.
         * @return The builder instance.
         */
        public Builder remotePort(int remotePort) {
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
        public Builder certificates(Certificate... certificates) {
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
        public Builder certificates(List<Certificate> certificates) {
            this.certificates = certificates;
            return this;
        }

        /**
         * Sets the client's user agent.
         *
         * @param userAgent The user agent.
         * @return The builder instance.
         */
        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        /**
         * Sets whether if the client connection is secure.
         * @param isSecure {@code true} if the client connection is secure, {@code false} otherwise.
         * @return The builder instance.
         */
        public Builder secure(boolean isSecure) {
            this.isSecure = isSecure;
            return this;
        }

        /**
         * Sets the local server's address.
         *
         * @param localAddress The local address.
         * @return The builder instance.
         */
        public Builder localAddress(String localAddress) {
            this.localAddress = localAddress;
            return this;
        }

        /**
         * Sets the local server's port.
         *
         * @param localPort The local port.
         * @return The builder instance.
         */
        public Builder localPort(int localPort) {
            this.localPort = localPort;
            return this;
        }

        /**
         * Creates a {@link ClientContext} instance from the specified properties.
         *
         * @return A {@link ClientContext} instance.
         */
        public ClientContext build() {
            if (certificates == null) {
                certificates = Collections.<Certificate>emptyList();
            }
            return new ClientContext(parent, remoteUser, remoteAddress, remotePort, certificates, userAgent, true,
                    isSecure, localAddress, localPort);
        }

    }

    /**
     * Creates a {@link ClientContext.Builder} for creating an external {@link ClientContext} instance.
     *
     * @param parent
     *      The parent context.
     * @return A builder for an external {@code ClientContext} instance.
     */
    public static Builder buildExternalClientContext(Context parent) {
        return new Builder(parent);
    }

    /**
     * Creates an internal {@link ClientContext} instance.
     * All data related to external context (e.g remote address, user agent...) will be set with empty non null values.
     * The returned internal {@link ClientContext} is considered as secure.
     *
     * @param parent
     *      The parent context.
     * @return An internal {@link ClientContext} instance.
     */
    public static ClientContext newInternalClientContext(Context parent) {
        return new ClientContext(parent, "", "", -1, Collections.<Certificate>emptyList(), "", false, true, "", -1);
    }

    private final Collection<? extends Certificate> certificates;

    /**
     * Restore from JSON representation.
     *
     * @param savedContext
     *            The JSON representation from which this context's attributes
     *            should be parsed.
     * @param classLoader
     *            The ClassLoader which can properly resolve the persisted class-name.
     */
    public ClientContext(final JsonValue savedContext, final ClassLoader classLoader) {
        super(savedContext, classLoader);
        try {
            this.certificates = Collections.unmodifiableCollection(
                CertificateFactory.getInstance(X509_TYPE).generateCertificates(
                    new ByteArrayInputStream(data.get(ATTR_CERTIFICATES).asString().getBytes("UTF8"))));
        } catch (CertificateException | UnsupportedEncodingException e) {
            throw new IllegalStateException("Unable to deserialize certificates", e);
        }
    }


    private ClientContext(Context parent,
                          String remoteUser,
                          String remoteAddress,
                          int remotePort,
                          List<? extends Certificate> certificates,
                          String userAgent,
                          boolean isExternal,
                          boolean isSecure,
                          String localAddress,
                          int localPort) {
        super(parent, "client");
        // Maintain the real list of certificates for Java API
        this.certificates = certificates;

        data.put(ATTR_REMOTE_USER, remoteUser);
        data.put(ATTR_REMOTE_ADDRESS, remoteAddress);
        data.put(ATTR_REMOTE_PORT, remotePort);
        data.put(ATTR_CERTIFICATES, serializeCertificates(certificates));
        data.put(ATTR_USER_AGENT, userAgent);
        data.put(ATTR_IS_EXTERNAL, isExternal);
        data.put(ATTR_IS_SECURE, isSecure);
        data.put(ATTR_LOCAL_ADDRESS, localAddress);
        data.put(ATTR_LOCAL_PORT, localPort);
    }

    /** Returns Base64-encoded certificates for JSON serialization. */
    private String serializeCertificates(final List<? extends Certificate> certificates) {
        final StringBuilder builder = new StringBuilder();
        for (final Certificate certificate : certificates) {
            try {
                builder.append(BEGIN_CERTIFICATE)
                    .append(Base64.encode(certificate.getEncoded()))
                    .append(END_CERTIFICATE);
            } catch (CertificateEncodingException e) {
                throw new IllegalStateException("Unable to serialize certificates", e);
            }
        }
        return builder.toString();
    }

    /**
     * Returns the login of the user making this request or an empty string if not known.
     *
     * @return the login of the user making this request or an empty string if not known.
     */
    public String getRemoteUser() {
        return data.get(ATTR_REMOTE_USER).asString();
    }

    /**
     * Returns the IP address of the client (or last proxy) that sent the request
     * or an empty string if the client is internal.
     *
     * @return the IP address of the client (or last proxy) that sent the request
     * or an empty string if the client is internal.
     */
    public String getRemoteAddress() {
        return data.get(ATTR_REMOTE_ADDRESS).asString();
    }

    /**
     * Returns the source port of the client (or last proxy) that sent the request
     * or {@code -1} if the client is internal.
     *
     * @return the source port of the client (or last proxy) that sent the request
     * or {@code -1} if the client is internal.
     */
    public int getRemotePort() {
        return data.get(ATTR_REMOTE_PORT).asInteger();
    }


    /**
     * Returns the collection (possibly empty) of certificate(s) provided by the client.
     * If no certificates are available, an empty list is returned.
     *
     * @return the collection (possibly empty) of certificate(s) provided by the client.
     */
    public Collection<? extends Certificate> getCertificates() {
        return certificates;
    }

    /**
     * Returns the value of the {@literal User-Agent} HTTP Header (if any, returns an empty string otherwise).
     *
     * @return the value of the {@literal User-Agent} HTTP Header (if any, returns an empty string otherwise).
     */
    public String getUserAgent() {
        return data.get(ATTR_USER_AGENT).asString();
    }

    /**
     * Returns {@code true} if this client is external.
     *
     * @return {@code true} if this client is external.
     */
    public boolean isExternal() {
        return data.get(ATTR_IS_EXTERNAL).asBoolean();
    }

    /**
     * Returns {@code true} if this client connection is secure.
     * It is the responsibility to the underlying protocol/implementation
     * to determine whether or not the connection is secure.
     * For example HTTPS and internal connections are meant to be secure.
     *
     * @return {@code true} if this client connection is secure.
     */
    public boolean isSecure() {
        return data.get(ATTR_IS_SECURE).asBoolean();
    }

    /**
     * Returns the IP address of the interface that received the request.
     *
     * @return the IP address of the server that received the request.
     */
    public String getLocalAddress() {
        return data.get(ATTR_LOCAL_ADDRESS).asString();
    }

    /**
     * Returns the port of the interface that received the request.
     *
     * @return the port of the interface that received the request.
     */
    public int getLocalPort() {
        return data.get(ATTR_LOCAL_PORT).asInteger();
    }
}
