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

package org.forgerock.http.header;

import static java.util.Collections.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.forgerock.http.protocol.Cookie;
import org.forgerock.http.protocol.Header;
import org.forgerock.http.protocol.Response;

/**
 * Processes the <strong>{@code Set-Cookie}</strong> request message header. For
 * more information, see the Http State Management Mechanism specification <a
 * href="http://tools.ietf.org/html/rfc6265">RFC 6265</a>.
 * <p>
 * Note: This implementation is designed to be forgiving when parsing malformed
 * cookies.
 */
public class SetCookieHeader extends Header {

    /** The name of this header. */
    public static final String NAME = "Set-Cookie";

    /**
     * Constructs a new header, initialized from the specified string value.
     *
     * @param value
     *            The value to initialize the header from.
     * @return The parsed header.
     */
    public static SetCookieHeader valueOf(String value) {
        return new SetCookieHeader(singletonList(parseCookie(value)));
    }

    private static Cookie parseCookie(String value) {
        List<String> parts = Arrays.asList(value.split(";"));
        Cookie cookie = new Cookie();
        for (String part : parts) {
            String[] nvp = part.split("=", 2);
            if ("Expires".equalsIgnoreCase(nvp[0].trim())) {
                cookie.setExpires(HeaderUtil.parseDate(nvp[1].trim()));
            } else if ("Max-Age".equalsIgnoreCase(nvp[0].trim())) {
                cookie.setMaxAge(parseInteger(nvp[1].trim()));
            } else if ("Path".equalsIgnoreCase(nvp[0].trim())) {
                cookie.setPath(nvp[1]);
            } else if ("Domain".equalsIgnoreCase(nvp[0].trim())) {
                cookie.setDomain(nvp[1]);
            } else if ("Secure".equalsIgnoreCase(nvp[0].trim())) {
                cookie.setSecure(true);
            } else if ("HttpOnly".equalsIgnoreCase(nvp[0].trim())) {
                cookie.setHttpOnly(true);
            } else if ("SameSite".equalsIgnoreCase(nvp[0].trim())) {
                cookie.setSameSite(Cookie.SameSite.parse(nvp[1].trim()));
            } else if (cookie.getName() == null || cookie.getName().isEmpty()) {
                cookie.setName(nvp[0].trim());
                cookie.setValue(nvp[1].trim());
            }
        }
        if (cookie.getName() == null || cookie.getName().isEmpty()) {
            cookie = new Cookie();
        }
        return cookie;
    }

    /**
     * Constructs a new header, initialized from the specified response message.
     *
     * @param response
     *            The response message to initialize the header from.
     * @return The parsed header.
     */
    public static SetCookieHeader valueOf(Response response) {
        if (response == null || !response.getHeaders().containsKey(NAME)) {
            return null;
        }
        return valueOf(response.getHeaders().get(NAME).getValues());
    }

    /**
     * Constructs a new header, initialized from the specified list of Set-Cookie values.
     *
     * @param values
     *            The values to initialize the header from.
     * @return The parsed header.
     */
    public static SetCookieHeader valueOf(List<String> values) {
        if (values == null) {
            return null;
        }
        List<Cookie> cookies = new ArrayList<>();
        for (String headerValue : values) {
            cookies.add(parseCookie(headerValue));
        }
        return new SetCookieHeader(unmodifiableList(cookies));
    }

    private static Integer parseInteger(String s) {
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    private final List<Cookie> cookies;
    private final List<String> values;

    /**
     * Constructs a new header with the provided cookies.
     *
     * @param cookies The cookies.
     */
    public SetCookieHeader(List<Cookie> cookies) {
        this.cookies = cookies;
        if (cookies != null) {
            this.values = new ArrayList<>();
            for (Cookie cookie : cookies) {
                values.add(toString(cookie));
            }
        } else {
            values = null;
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<String> getValues() {
        return values;
    }

    /**
     * Returns the cookies.
     *
     * @return The cookies.
     */
    public List<Cookie> getCookies() {
        return cookies;
    }

    private String toString(Cookie cookie) {
        StringBuilder sb = new StringBuilder();
        if (cookie.getName() != null) {
            sb.append(cookie.getName()).append("=").append(cookie.getValue());
            if (cookie.getExpires() != null) {
                sb.append("; ").append("Expires").append("=").append(HeaderUtil.formatDate(cookie.getExpires()));
            }
            if (cookie.getMaxAge() != null && cookie.getMaxAge()>-1) {
                sb.append("; ").append("Max-Age").append("=").append(cookie.getMaxAge());
            }
            if (cookie.getPath() != null) {
                sb.append("; ").append("Path").append("=").append(cookie.getPath());
            }
            if (cookie.getDomain() != null) {
                sb.append("; ").append("Domain").append("=").append(cookie.getDomain());
            }
            // Cookies with SameSite=NONE MUST be sent as Secure
            if ((cookie.isSecure() != null && cookie.isSecure()) || cookie.getSameSite() == Cookie.SameSite.NONE) {
                sb.append("; ").append("Secure");
            }
            if (cookie.isHttpOnly() != null && cookie.isHttpOnly()) {
                sb.append("; ").append("HttpOnly");
            }
            if (cookie.getSameSite() != null) {
                sb.append("; SameSite=").append(cookie.getSameSite());
            }
        }
        return sb.toString();
    }

    static class Factory extends HeaderFactory<SetCookieHeader> {

        @Override
        public SetCookieHeader parse(String value) {
            return valueOf(value);
        }

        @Override
        public SetCookieHeader parse(List<String> values) {
            return valueOf(values);
        }
    }
}
