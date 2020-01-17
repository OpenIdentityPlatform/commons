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

import java.util.HashMap;
import java.util.Map;

import org.forgerock.json.jose.jwt.Algorithm;
import org.forgerock.json.jose.jwt.JwtHeader;

/**
 * A base implementation of a JWT header builder that provides a fluent builder pattern to creating JWT headers.
 * <p>
 * See {@link JwtHeader} for information on the JwtHeader object that this builder creates.
 *
 * @param <T> the type of JwtBuilder that parents this JwtHeaderBuilder.
 * @param <B> the type of this JwtHeaderBuilder
 *
 * @since 2.0.0
 */
public abstract class JwtHeaderBuilder<T extends JwtBuilder, B extends JwtHeaderBuilder<T, B>> {

    private final T jwtBuilder;

    private final Map<String, Object> headers = new HashMap<>();

    /**
     * Constructs a new JwtHeaderBuilder, parented by the given JwtBuilder.
     *
     * @param jwtBuilder The JwtBuilder instance that this JwtHeaderBuilder is a child of.
     */
    public JwtHeaderBuilder(T jwtBuilder) {
        this.jwtBuilder = jwtBuilder;
    }

    /**
     * Adds a custom header parameter to the JWT header.
     * <p>
     * @see JwtHeader#setParameter(String, Object)
     *
     * @param key The header parameter key.
     * @param value The header parameter value.
     * @return This JwtHeaderBuilder.
     */
    @SuppressWarnings("unchecked")
    public B header(String key, Object value) {
        headers.put(key, value);
        return (B) this;
    }

    /**
     * Adds a customer header parameter to the JWT header if the value is not null.
     *
     * @param key The header parameter key.
     * @param value The header parameter value, or {@literal null} if not specified.
     * @return This JwtHeaderBuilder.
     */
    @SuppressWarnings("unchecked")
    public B headerIfNotNull(String key, Object value) {
        if (value != null) {
            header(key, value);
        }
        return (B) this;
    }

    /**
     * Sets the algorithm used to perform cryptographic signing and/or encryption on the JWT.
     * <p>
     * @see JwtHeader#setAlgorithm(org.forgerock.json.jose.jwt.Algorithm)
     *
     * @param algorithm The algorithm.
     * @return This JwtHeaderBuilder.
     */
    @SuppressWarnings("unchecked")
    public B alg(Algorithm algorithm) {
        header("alg", algorithm.toString());
        return (B) this;
    }

    /**
     * Marks the end to the building of the JWT header.
     *
     * @return The parent JwtBuilder for this JwtHeaderBuilder instance.
     */
    public T done() {
        return jwtBuilder;
    }

    /**
     * Gets the header parameters for the JWT.
     *
     * @return The JWT's header parameters.
     */
    protected Map<String, Object> getHeaders() {
        return headers;
    }

    /**
     * Creates a JwtHeader instance from the header parameters set in this builder.
     *
     * @return A JwtHeader instance.
     */
    protected abstract JwtHeader build();
}
