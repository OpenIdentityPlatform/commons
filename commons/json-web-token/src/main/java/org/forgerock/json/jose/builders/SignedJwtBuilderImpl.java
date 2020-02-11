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

import java.security.Key;

import org.forgerock.json.jose.jws.JwsHeader;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.JwtClaimsSet;

/**
 * An implementation of a JwtBuilder that can build a JWT and sign it, resulting in a SignedJwt object.
 *
 * @since 2.0.0
 */
public class SignedJwtBuilderImpl extends AbstractJwtBuilder implements SignedJwtBuilder {

    private final SigningHandler signingHandler;

    /**
     * Constructs a new SignedJwtBuilderImpl that will use the given private key to sign the JWT.
     *
     * @param signingHandler The SigningHandler instance used to sign the JWS.
     */
    public SignedJwtBuilderImpl(SigningHandler signingHandler) {
        this.signingHandler = signingHandler;
    }

    /**
     * Gets the JwsHeaderBuilder that this JwtBuilder will use to build the JWS' header parameters.
     *
     * @return The JwsHeaderBuilder instance.
     */
    @Override
    public JwsHeaderBuilder headers() {
        setJwtHeaderBuilder(new JwsHeaderBuilder(this));
        return (JwsHeaderBuilder) getHeaderBuilder();
    }

    /**
     * Sets the JwtClaimsSet for this JwtBuilder.
     *
     * @param claimsSet {@inheritDoc}
     * @return This SignedJwtBuilderImpl.
     */
    @Override
    public SignedJwtBuilderImpl claims(JwtClaimsSet claimsSet) {
        return (SignedJwtBuilderImpl) super.claims(claimsSet);
    }

    /**
     * Wraps the signed JWT in an outer encrypted JWE envelope.
     *
     * @param encryptionKey the key to use for encryption. This should either be a symmetric secret key or a public key.
     * @return the nested encrypted signed JWT builder.
     */
    public SignedThenEncryptedJwtBuilder encrypt(Key encryptionKey) {
        return new SignedThenEncryptedJwtBuilder(this, encryptionKey);
    }

    @Override
    public SignedJwt asJwt() {
        JwtHeaderBuilder<?, ?> headerBuilder = getHeaderBuilder();
        JwsHeader header;
        if (headerBuilder == null) {
            header = new JwsHeader();
        } else {
            header = (JwsHeader) getHeaderBuilder().build();
        }
        JwtClaimsSet claimsSet = getClaimsSet();
        if (claimsSet == null) {
            claimsSet = new JwtClaimsSet();
        }
        return new SignedJwt(header, claimsSet, signingHandler);
    }

    /**
     * Builds the JWS into a <code>String</code> by calling the <tt>build</tt> method on the JWS object.
     * <p>
     * @see org.forgerock.json.jose.jws.SignedJwt#build()
     *
     * @return The base64url encoded UTF-8 parts of the JWS.
     */
    @Override
    public String build() {
        return asJwt().build();
    }
}
