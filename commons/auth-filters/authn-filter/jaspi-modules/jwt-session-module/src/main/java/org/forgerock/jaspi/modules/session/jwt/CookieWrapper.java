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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.jaspi.modules.session.jwt;

import org.forgerock.http.protocol.Cookie;

/**
 * Wrapper around the CHF {@link Cookie}.
 */
final class CookieWrapper implements JwtSessionCookie {

    private final Cookie cookie;

    CookieWrapper(Cookie cookie) {
        this.cookie = cookie;
    }

    Cookie getCookie() {
        return cookie;
    }

    @Override
    public String getValue() {
        return cookie.getValue();
    }
}
