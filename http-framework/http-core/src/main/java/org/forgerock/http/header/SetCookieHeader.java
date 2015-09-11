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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.http.header;

import static org.forgerock.http.header.HeaderUtil.parseSingleValuedHeader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

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
public class SetCookieHeader implements Header {

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
        List<String> parts = Arrays.asList(value.split(";"));
        Cookie cookie = new Cookie();
        for (String part : parts) {
            String[] nvp = part.split("=");
            if ("Expires".equalsIgnoreCase(nvp[0].trim())) {
                cookie.setExpires(parseDate(nvp[1].trim()));
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
            } else if (cookie.getName() == null || cookie.getName().isEmpty()) {
                cookie.setName(nvp[0].trim());
                cookie.setValue(nvp[1].trim());
            }
        }
        if (cookie.getName() == null || cookie.getName().isEmpty()) {
            return new SetCookieHeader(new Cookie());
        }
        return new SetCookieHeader(cookie);
    }

    /**
     * Constructs a new header, initialized from the specified response message.
     *
     * @param response
     *            The response message to initialize the header from.
     * @return The parsed header.
     */
    public static SetCookieHeader valueOf(Response response) {
        return valueOf(parseSingleValuedHeader(response, NAME));
    }

    private static Integer parseInteger(String s) {
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    private static final String EXPIRES_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";

    private static SimpleDateFormat getDateFormatter() {
        SimpleDateFormat formatter = new SimpleDateFormat(EXPIRES_DATE_FORMAT, Locale.ROOT);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        return formatter;
    }

    private static Date parseDate(String s) {
        try {
            return getDateFormatter().parse(s);
        } catch (ParseException e) {
            return null;
        }
    }

    private final Cookie cookie;

    /**
     * Constructs a new header with the provided cookie.
     *
     * @param cookie The cookie.
     */
    public SetCookieHeader(Cookie cookie) {
        this.cookie = cookie;
    }

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Returns the cookie.
     *
     * @return The cookie.
     */
    public Cookie getCookie() {
        return cookie;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (cookie.getName() != null) {
            sb.append(cookie.getName()).append("=").append(cookie.getValue());
            if (cookie.getExpires() != null) {
                sb.append("; ").append("Expires").append("=").append(getDateFormatter().format(cookie.getExpires()));
            }
            if (cookie.getMaxAge() != null && cookie.getMaxAge() > 0) {
                sb.append("; ").append("Max-Age").append("=").append(cookie.getMaxAge());
            }
            if (cookie.getPath() != null) {
                sb.append("; ").append("Path").append("=").append(cookie.getPath());
            }
            if (cookie.getDomain() != null) {
                sb.append("; ").append("Domain").append("=").append(cookie.getDomain());
            }
            if (cookie.isSecure() != null && cookie.isSecure()) {
                sb.append("; ").append("Secure");
            }
            if (cookie.isHttpOnly() != null && cookie.isHttpOnly()) {
                sb.append("; ").append("HttpOnly");
            }
        }
        return sb.toString();
    }
}
