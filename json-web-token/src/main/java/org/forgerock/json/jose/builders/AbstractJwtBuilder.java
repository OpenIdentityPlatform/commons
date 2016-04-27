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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.json.jose.builders;

import org.forgerock.json.jose.jwt.JwtClaimsSet;

/**
 * A base implementation for all JwtBuilders that provides the basis of the JWT builder methods.
 *
 * @since 2.0.0
 */
public abstract class AbstractJwtBuilder implements JwtBuilder {

    private JwtHeaderBuilder<?, ?> headerBuilder;
    private JwtClaimsSet claimsSet;

    /**
     * Gets the JwtHeaderBuilder that this JwtBuilder will use to build the JWT's header parameters.
     *
     * @return The JwtHeaderBuilder instance.
     */
    public abstract JwtHeaderBuilder<?, ?> headers();

    /**
     * Sets the JwtHeaderBuilder that this JwtBuilder will use to build the JWT's header parameters.
     *
     * @param headerBuilder The JwtHeaderBuilder instance.
     */
    void setJwtHeaderBuilder(JwtHeaderBuilder<?, ?> headerBuilder) {
        this.headerBuilder = headerBuilder;
    }

    /**
     * Gets the JwtHeaderBuilder that is used to build the JWT's header parameters.
     *
     * @return The JwtHeaderBuilder instance.
     */
    JwtHeaderBuilder<?, ?> getHeaderBuilder() {
        return headerBuilder;
    }

    /**
     * Sets the JwtClaimsSet for this JwtBuilder.
     *
     * @param claimsSet The JwtClaimsSet containing the JWT's claims.
     * @return This AbstractJwtBuilder.
     */
    public AbstractJwtBuilder claims(JwtClaimsSet claimsSet) {
        this.claimsSet = claimsSet;
        return this;
    }

    /**
     * Gets the JwtClaimsSet that has been set in this JwtBuilder.
     *
     * @return The JwtClaimsSet containing the JWT's claims.
     */
    JwtClaimsSet getClaimsSet() {
        return claimsSet;
    }
}
