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

import static org.forgerock.util.Reject.checkNotNull;

import java.security.Key;

import org.forgerock.json.jose.jwe.SignedThenEncryptedJwt;
import org.forgerock.json.jose.jwe.JweHeader;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.json.jose.jwt.JwtType;

/**
 * Builder for nested signed-then-encrypted JWT. This is the preferred nesting order for OpenID Connect and other
 * tokens.
 *
 * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#SigningOrder">OpenID Connect Signing Order</a>
 * @see SignedJwtBuilderImpl#encrypt(Key)
 */
public class SignedThenEncryptedJwtBuilder extends EncryptedJwtBuilder {
    private SignedJwtBuilderImpl signedJwtBuilder;
    private final SignedThenEncryptedJwtHeaderBuilder headerBuilder;

    /**
     * Constructs the builder with the given signed JWT payload and encryption key.
     * @param signedJwtBuilder the signed jwt builder to wrap with encryption.
     * @param publicKey the encryption key.
     */
    SignedThenEncryptedJwtBuilder(final SignedJwtBuilderImpl signedJwtBuilder, final Key publicKey) {
        super(publicKey);
        this.signedJwtBuilder = checkNotNull(signedJwtBuilder);
        this.headerBuilder = new SignedThenEncryptedJwtHeaderBuilder(this);
    }

    @Override
    public SignedThenEncryptedJwt asJwt() {
        JweHeader header = (JweHeader) headerBuilder.cty(JwtType.JWT.toString()).build();
        SignedJwt signedJwt = signedJwtBuilder.asJwt();

        return new SignedThenEncryptedJwt(header, signedJwt, publicKey);
    }

    @Override
    public SignedThenEncryptedJwtBuilder claims(JwtClaimsSet claims) {
        signedJwtBuilder = signedJwtBuilder.claims(claims);
        return this;
    }

    @Override
    public JweHeaderBuilder<SignedThenEncryptedJwtBuilder> headers() {
        return headerBuilder;
    }

}
