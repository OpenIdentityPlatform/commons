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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.json.jose.builders;

import java.net.URL;
import java.util.List;

import org.forgerock.json.jose.jwe.CompressionAlgorithm;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jws.JwtSecureHeader;

/**
 * A base implementation of a JWT header builder, for the common security header parameters shared by the JWS and JWE
 * headers, that provides a fluent builder pattern to creating JWT headers.
 * <p>
 * See {@link org.forgerock.json.jose.jws.JwtSecureHeader} for information on the JwtSecureHeader object that this
 * builder creates.
 *
 * @param <T> the type of JwtBuilder that parents this JwtHeaderBuilder.
 * @param <B> the type of this JwtHeaderBuilder
 *
 * @since 2.0.0
 */
public abstract class JwtSecureHeaderBuilder<T extends JwtBuilder, B extends JwtSecureHeaderBuilder<T, B>>
        extends JwtHeaderBuilder<T, B> {

    /**
     * Constructs a new JwtSecureHeaderBuilder, parented by the given JwtBuilder.
     *
     * @param jwtBuilder The JwtBuilder instance that this JwtSecureHeaderBuilder is a child of.
     */
    public JwtSecureHeaderBuilder(T jwtBuilder) {
        super(jwtBuilder);
    }

    /**
     * Sets the JWK Set URL header parameter for this JWS.
     * <p>
     * @see org.forgerock.json.jose.jws.JwtSecureHeader#setJwkSetUrl(java.net.URL)
     *
     * @param jku The JWK Set URL.
     * @return This JwtSecureHeaderBuilder.
     */
    @SuppressWarnings("unchecked")
    public B jku(URL jku) {
        header("jku", jku);
        return (B) this;
    }

    /**
     * Sets the JSON Web Key header parameter for this JWS.
     * <p>
     * @see org.forgerock.json.jose.jws.JwtSecureHeader#setJsonWebKey(org.forgerock.json.jose.jwk.JWK)
     *
     * @param jwk The JSON Web Key.
     * @return This JwtSecureHeaderBuilder.
     */
    @SuppressWarnings("unchecked")
    public B jwk(JWK jwk) {
        header("jwk", jwk);
        return (B) this;
    }

    /**
     * Sets the X.509 URL header parameter for this JWS.
     * <p>
     * @see org.forgerock.json.jose.jws.JwtSecureHeader#setX509Url(java.net.URL)
     *
     * @param x5u THe X.509 URL.
     * @return This JwtSecureHeaderBuilder.
     */
    @SuppressWarnings("unchecked")
    public B x5u(URL x5u) {
        header("x5u", x5u);
        return (B) this;
    }

    /**
     * Sets the X.509 Certificate Thumbprint header parameter for this JWS.
     * <p>
     * @see org.forgerock.json.jose.jws.JwtSecureHeader#setX509CertificateThumbprint(String)
     *
     * @param x5t The X.509 Certificate Thumbprint.
     * @return This JwtSecureHeaderBuilder.
     */
    @SuppressWarnings("unchecked")
    public B x5t(String x5t) {
        header("x5t", x5t);
        return (B) this;
    }

    /**
     * Sets the X.509 Certificate Chain header parameter for this JWS.
     * <p>
     * @see org.forgerock.json.jose.jws.JwtSecureHeader#setX509CertificateChain(java.util.List)
     *
     * @param x5c The X.509 Certificate Chain.
     * @return This JwtSecureHeaderBuilder.
     */
    @SuppressWarnings("unchecked")
    public B x5c(List<String> x5c) {
        header("x5c", x5c);
        return (B) this;
    }

    /**
     * Sets the Key ID header parameter for this JWS.
     * <p>
     * @see org.forgerock.json.jose.jws.JwtSecureHeader#setKeyId(String)
     *
     * @param kid The Key ID.
     * @return This JwtSecureHeaderBuilder.
     */
    @SuppressWarnings("unchecked")
    public B kid(String kid) {
        header("kid", kid);
        return (B) this;
    }

    /**
     * Sets the content type header parameter for this JWS.
     * <p>
     * @see org.forgerock.json.jose.jws.JwtSecureHeader#setContentType(String)
     *
     * @param cty The content type of the JWS payload.
     * @return This JwtSecureHeaderBuilder.
     */
    @SuppressWarnings("unchecked")
    public B cty(String cty) {
        header("cty", cty);
        return (B) this;
    }

    /**
     * Sets the critical header parameters for this JWS.
     * <p>
     * @see org.forgerock.json.jose.jws.JwtSecureHeader#setCriticalHeaders(java.util.List)
     *
     * @param crit A List of the JWS critical parameters.
     * @return This JwtSecureHeaderBuilder.
     */
    @SuppressWarnings("unchecked")
    public B crit(List<String> crit) {
        header("crit", crit);
        return (B) this;
    }

    /**
     * Sets the Compression Algorithm header parameter for this JWE.
     * <p>
     * @see JwtSecureHeader#setCompressionAlgorithm(CompressionAlgorithm)
     *
     * @param zip The Compression Algorithm.
     * @return This JweHeaderBuilder.
     */
    @SuppressWarnings("unchecked")
    public B zip(CompressionAlgorithm zip) {
        header("zip", zip.toString());
        return (B) this;
    }
}
