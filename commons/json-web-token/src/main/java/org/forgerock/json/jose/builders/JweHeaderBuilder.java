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

import org.forgerock.json.jose.jwe.CompressionAlgorithm;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweHeader;
import org.forgerock.json.jose.jwt.JwtHeader;

/**
 * An implementation of a JWE Header builder that provides a fluent builder pattern to create JWE headers.
 * <p>
 * See {@link JweHeader} for information on the JweHeader object that this builder creates.
 *
 * @since 2.0.0
 * @param <B> the concrete JWT builder type that headers are being built for.
 */
public class JweHeaderBuilder<B extends EncryptedJwtBuilder> extends JwtSecureHeaderBuilder<B, JweHeaderBuilder<B>> {

    /**
     * Constructs a new JweHeaderBuilder, parented by the given JwtBuilder.
     *
     * @param jwtBuilder The JwtBuilder instance that this JweHeaderBuilder is a child of.
     */
    public JweHeaderBuilder(B jwtBuilder) {
        super(jwtBuilder);
    }

    /**
     * Sets the Encryption Method header parameter for this JWE.
     * <p>
     * @see org.forgerock.json.jose.jwe.JweHeader#setEncryptionMethod(org.forgerock.json.jose.jwe.EncryptionMethod)
     *
     * @param enc The Encryption Method.
     * @return This JweHeaderBuilder.
     */
    public JweHeaderBuilder<B> enc(EncryptionMethod enc) {
        header("enc", enc.toString());
        return this;
    }

    /**
     * Sets the Ephemeral Public Key header parameter for this JWE.
     * <p>
     * @see org.forgerock.json.jose.jwe.JweHeader#setEphemeralPublicKey(org.forgerock.json.jose.jwk.JWK)
     *
     * @param epk The Ephemeral Public Key.
     * @return This JweHeaderBuilder.
     */
    public JweHeaderBuilder<B> epk(String epk) {
        header("epk", epk);
        return this;
    }

    // Overridden purely to preserve type compatibility with older version
    @Override
    public JweHeaderBuilder<B> zip(CompressionAlgorithm zip) {
        return super.zip(zip);
    }

    /**
     * Sets the Agreement PartyUInfo header parameter for this JWE.
     * <p>
     * @see org.forgerock.json.jose.jwe.JweHeader#setAgreementPartyUInfo(String)
     *
     * @param apu The Agreement PartyUInfo.
     * @return This JweHeaderBuilder.
     */
    public JweHeaderBuilder<B> apu(String apu) {
        header("apu", apu);
        return this;
    }

    /**
     * Creates a JweHeader instance from the header parameters set in this builder.
     *
     * @return A JweHeader instance.
     */
    @Override
    protected JwtHeader build() {
        return new JweHeader(getHeaders());
    }
}
