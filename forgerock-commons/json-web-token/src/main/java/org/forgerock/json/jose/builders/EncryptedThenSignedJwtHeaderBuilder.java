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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.json.jose.builders;

import org.forgerock.json.jose.jws.JwsHeader;

/**
 * An implementation of a JWS Header builder that provides a fluent builder pattern to create JWS headers for
 * signed encrypted JWTs.
 * <p>
 * See {@link JwsHeader} for information on the JwsHeader object that this builder creates.
 *
 * @since 2.0.0
 */
public class EncryptedThenSignedJwtHeaderBuilder
        extends JwtSecureHeaderBuilder<EncryptedThenSignedJwtBuilder, EncryptedThenSignedJwtHeaderBuilder> {

    /**
     * Constructs a new JwsHeaderBuilder, parented by the given JwtBuilder.
     *
     * @param jwtBuilder The JwtBuilder instance that this JwsHeaderBuilder is a child of.
     */
    public EncryptedThenSignedJwtHeaderBuilder(EncryptedThenSignedJwtBuilder jwtBuilder) {
        super(jwtBuilder);
    }

    /**
     * Creates a JwsHeader instance from the header parameters set in this builder.
     *
     * @return A JwsHeader instance.
     */
    @Override
    protected JwsHeader build() {
        return new JwsHeader(getHeaders());
    }
}
