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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.json.jose.builders;

import org.forgerock.json.jose.jwe.CompressionAlgorithm;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweHeader;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwt.Algorithm;
import org.forgerock.json.jose.jwt.JwtHeader;

import java.net.URL;
import java.util.List;

public class JweHeaderBuilder extends JwtSecureHeaderBuilder {

    public JweHeaderBuilder(EncryptedJwtBuilder jwtBuilder) {
        super(jwtBuilder);
    }

    @Override
    public JweHeaderBuilder header(String key, Object value) {
        return (JweHeaderBuilder) super.header(key, value);
    }

    @Override
    public JweHeaderBuilder alg(Algorithm algorithm) {
        return (JweHeaderBuilder) super.alg(algorithm);
    }

    @Override
    public JweHeaderBuilder jku(URL jku) {
        return (JweHeaderBuilder) super.jku(jku);
    }

    @Override
    public JweHeaderBuilder jwk(JWK jwk) {
        return (JweHeaderBuilder) super.jwk(jwk);
    }

    @Override
    public JweHeaderBuilder x5u(URL x5u) {
        return (JweHeaderBuilder) super.x5u(x5u);
    }

    @Override
    public JweHeaderBuilder x5t(String x5t) {
        return (JweHeaderBuilder) super.x5t(x5t);
    }

    @Override
    public JweHeaderBuilder x5c(List<String> x5c) {
        return (JweHeaderBuilder) super.x5c(x5c);
    }

    @Override
    public JweHeaderBuilder kid(String kid) {
        return (JweHeaderBuilder) super.kid(kid);
    }

    @Override
    public JweHeaderBuilder cty(String cty) {
        return (JweHeaderBuilder) super.cty(cty);
    }

    @Override
    public JweHeaderBuilder crit(List<String> crit) {
        return (JweHeaderBuilder) super.crit(crit);
    }

    public JweHeaderBuilder enc(EncryptionMethod enc) {
        header("enc", enc.toString());
        return this;
    }

    public JweHeaderBuilder epk(String epk) {
        header("epk", epk);
        return this;
    }

    public JweHeaderBuilder zip(CompressionAlgorithm zip) {
        header("zip", zip.toString());
        return this;
    }

    public JweHeaderBuilder apu(String apu) {
        header("apu", apu);
        return this;
    }

    @Override
    protected JwtHeader build() {
        return new JweHeader(getHeaders());
    }
}
