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

import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jws.JwsHeader;
import org.forgerock.json.jose.jwt.Algorithm;

import java.net.URL;
import java.util.List;

public class JwsHeaderBuilder extends JwtSecureHeaderBuilder {

    public JwsHeaderBuilder(SignedJwtBuilder jwtBuilder) {
        super(jwtBuilder);
    }

    @Override
    public JwsHeaderBuilder header(String key, Object value) {
        return (JwsHeaderBuilder) super.header(key, value);
    }

    @Override
    public JwsHeaderBuilder alg(Algorithm algorithm) {
        return (JwsHeaderBuilder) super.alg(algorithm);
    }

    @Override
    public JwsHeaderBuilder jku(URL jku) {
        return (JwsHeaderBuilder) super.jku(jku);
    }

    @Override
    public JwsHeaderBuilder jwk(JWK jwk) {
        return (JwsHeaderBuilder) super.jwk(jwk);
    }

    @Override
    public JwsHeaderBuilder x5u(URL x5u) {
        return (JwsHeaderBuilder) super.x5u(x5u);
    }

    @Override
    public JwsHeaderBuilder x5t(String x5t) {
        return (JwsHeaderBuilder) super.x5t(x5t);
    }

    @Override
    public JwsHeaderBuilder x5c(List<String> x5c) {
        return (JwsHeaderBuilder) super.x5c(x5c);
    }

    @Override
    public JwsHeaderBuilder kid(String kid) {
        return (JwsHeaderBuilder) super.kid(kid);
    }

    @Override
    public JwsHeaderBuilder cty(String cty) {
        return (JwsHeaderBuilder) super.cty(cty);
    }

    @Override
    public JwsHeaderBuilder crit(List<String> crit) {
        return (JwsHeaderBuilder) super.crit(crit);
    }

    @Override
    protected JwsHeader build() {
        return new JwsHeader(getHeaders());
    }
}
