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

import java.net.URL;
import java.util.List;

public abstract class JwtSecureHeaderBuilder extends JwtHeaderBuilder {

    public JwtSecureHeaderBuilder(AbstractJwtBuilder jwtBuilder) {
        super(jwtBuilder);
    }

    public JwtSecureHeaderBuilder jku(URL jku) {
        header("jku", jku);
        return this;
    }

    public JwtSecureHeaderBuilder jwk(JWK jwk) {
        header("jwk", jwk);
        return this;
    }

    public JwtSecureHeaderBuilder x5u(URL x5u) {
        header("x5u", x5u);
        return this;
    }

    public JwtSecureHeaderBuilder x5t(String x5t) {
        header("x5t", x5t);
        return this;
    }

    public JwtSecureHeaderBuilder x5c(List<String> x5c) {
        header("x5c", x5c);
        return this;
    }

    public JwtSecureHeaderBuilder kid(String kid) {
        header("kid", kid);
        return this;
    }

    public JwtSecureHeaderBuilder cty(String cty) {
        header("cty", cty);
        return this;
    }

    public JwtSecureHeaderBuilder crit(List<String> crit) {
        header("crit", crit);
        return this;
    }
}
