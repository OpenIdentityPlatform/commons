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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.caf.http;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.forgerock.jaspi.modules.session.jwt.JwtSessionCookie;

/**
 * This class creates an API which bridges the differences between the Servlet 2.5 and 3.0 Cookie APIs, as the Servlet
 * 2.5 API does not support HttpOnly cookies and provides no methods to create a HttpOnly cookie.
 *
 * @see javax.servlet.http.Cookie
 * @since 1.5.0
 */
//@Checkstyle:ignore
public class Cookie implements JwtSessionCookie {

    //So as to test both Servlet 2.5 or 3.0 being present
    static boolean isServlet3xPresent = isHttpOnlyCookieSupported();

    private final javax.servlet.http.Cookie cookie;

    private Cookie(String name, String value) {
        this.cookie = new javax.servlet.http.Cookie(name, value);
    }

    private Cookie(javax.servlet.http.Cookie cookie) {
        this.cookie = cookie;
    }

    /**
     * Returns the name of the cookie. The name cannot be changed after creation.
     *
     * @return The name of the cookie.
     */
    public final String getName() {
        return cookie.getName();
    }

    /**
     * Gets the current value of this Cookie.
     *
     * @return The current value of this Cookie.
     */
    @Override
    public final String getValue() {
        return cookie.getValue();
    }

    /**
     * <p>Assigns a new value to this Cookie.</p>
     *
     * <p>If you use a binary value, you may want to use BASE64 encoding.</p>
     *
     * <p>With Version 0 cookies, values should not contain white space, brackets, parentheses, equals signs, commas,
     * double quotes, slashes, question marks, at signs, colons, and semicolons. Empty values may not behave the same
     * way on all browsers.</p>
     *
     * @param value The new value of the cookie.
     */
    public final void setValue(String value) {
        cookie.setValue(value);
    }

    /**
     * <p>Returns the path on the server to which the browser returns this cookie. The cookie is visible to all
     * sub-paths on the server.</p>
     *
     * @return A {@code String} specifying a path that contains a servlet name, for example, <i>/catalog</i>.
     */
    public final String getPath() {
        return cookie.getPath();
    }

    /**
     * <p>Specifies a path for the cookie to which the client should return the cookie.</p>
     *
     * <p>The cookie is visible to all the pages in the directory you specify, and all the pages in that directory's
     * subdirectories. A cookie's path must include the servlet that set the cookie, for example, <i>/catalog</i>,
     * which makes the cookie visible to all directories on the server under <i>/catalog</i>.</p>
     *
     * <p>Consult RFC 2109 (available on the Internet) for more information on setting path names for cookies.</p>
     *
     * @param uri A {@code String} specifying a path
     */
    public final void setPath(String uri) {
        cookie.setPath(uri);
    }

    /**
     * <p>Gets the domain name of this Cookie.</p>
     *
     * <p>Domain names are formatted according to RFC 2109.</p>
     *
     * @return The domain name of this Cookie.
     */
    public final String getDomain() {
        return cookie.getDomain();
    }

    /**
     * <p>Specifies the domain within which this cookie should be presented.</p>
     *
     * <p>The form of the domain name is specified by RFC 2109. A domain name begins with a dot ({@code .example.com})
     * and means that the cookie is visible to servers in a specified Domain Name System (DNS) zone (for example,
     * {@code www.example.com}, but not {@code a.b.example.com}). By default, cookies are only returned to the server
     * that sent them.</p>
     *
     * <p>Domain will only be set if the provided {@code domain} is not {@code null}.</p>
     *
     * @param domain The domain name within which this cookie is visible; form is according to RFC 2109.
     */
    public final void setDomain(String domain) {
        if (domain != null) {
            cookie.setDomain(domain);
        }
    }

    /**
     * <p>Gets the maximum age in seconds of this Cookie.</p>
     *
     * <p>By default, {@code -1} is returned, which indicates that the cookie will persist until browser shutdown.</p>
     *
     * @return An integer specifying the maximum age of the cookie in seconds; if negative, means the cookie persists
     * until browser shutdown.
     */
    public final int getMaxAge() {
        return cookie.getMaxAge();
    }

    /**
     * <p>Sets the maximum age in seconds for this Cookie.</p>
     *
     * <p>A positive value indicates that the cookie will expire after that many seconds have passed. Note that the
     * value is the <i>maximum</i> age when the cookie will expire, not the cookie's current age.</p>
     *
     * <p>A negative value means that the cookie is not stored persistently and will be deleted when the Web browser
     * exits. A zero value causes the cookie to be deleted.</p>
     *
     * @param expiry An integer specifying the maximum age of the cookie in seconds; if negative, means the cookie is
     *               not stored; if zero, deletes the cookie.
     */
    public final void setMaxAge(int expiry) {
        cookie.setMaxAge(expiry);
    }

    /**
     * Returns {@code true} if the browser is sending cookies only over a secure protocol, or {@code false} if the
     * browser can send cookies using any protocol.
     *
     * @return {@code true} if the browser uses a secure protocol, {@code false} otherwise.
     */
    public final boolean isSecure() {
        return cookie.getSecure();
    }

    /**
     * <p>Indicates to the browser whether the cookie should only be sent using a secure protocol, such as HTTPS or SSL.
     * </p>
     *
     * <p>The default value is {@code false}.</p>
     *
     * @param isSecure If {@code true}, sends the cookie from the browser to the server only when using a secure
     *                 protocol; if {@code false}, sent on any protocol.
     */
    public final void setSecure(boolean isSecure) {
        cookie.setSecure(isSecure);
    }

    /**
     * Checks whether this Cookie has been marked as <i>HttpOnly</i>.
     *
     * @return {@code true} if this Cookie has been marked as <i>HttpOnly</i>, {@code false} otherwise.
     */
    public boolean isHttpOnly() {
        return cookie.isHttpOnly();
    }

    /**
     * <p>Marks or unmarks this Cookie as <i>HttpOnly</i>.</p>
     *
     * <p>If {@code isHttpOnly} is set to {@code true}, this cookie is marked as <i>HttpOnly</i>, by adding the
     * {@code HttpOnly} attribute to it.</p>
     *
     * <p><i>HttpOnly</i> cookies are not supposed to be exposed to client-side scripting code, and may therefore help
     * mitigate certain kinds of cross-site scripting attacks.</p>
     *
     * @param isHttpOnly {@code true} if this cookie is to be marked as <i>HttpOnly</i>, {@code false} otherwise.
     */
    public void setHttpOnly(boolean isHttpOnly) {
        cookie.setHttpOnly(isHttpOnly);
    }

    /**
     * Returns the comment describing the purpose of this cookie, or {@code null} if the cookie has no comment.
     *
     * @return The comment of the cookie, or {@code null} if unspecified
     */
    public final String getComment() {
        return cookie.getComment();
    }

    /**
     * Specifies a comment that describes a cookie's purpose. The comment is useful if the browser presents the cookie
     * to the user. Comments are not supported by Netscape Version 0 cookies.
     *
     * @param purpose A {@code String} specifying the comment to display to the user.
     */
    public void setComment(String purpose) {
        cookie.setComment(purpose);
    }

    /**
     * Returns the version of the protocol this cookie complies with. Version 1 complies with RFC 2109, and version 0
     * complies with the original cookie specification drafted by Netscape. Cookies provided by a browser use and
     * identify the browser's cookie version.
     *
     * @return 0 if the cookie complies with the original Netscape specification; 1 if the cookie complies with RFC
     * 2109.
     */
    public final int getVersion() {
        return cookie.getVersion();
    }

    /**
     * <p>Sets the version of the cookie protocol that this Cookie complies with.</p>
     *
     * <p>Version 0 complies with the original Netscape cookie specification. Version 1 complies with RFC 2109.</p>
     *
     * <p>Since RFC 2109 is still somewhat new, consider version 1 as experimental; do not use it yet on production
     * sites.</p>
     *
     * @param version 0 if the cookie should comply with the original Netscape specification; 1 if the cookie should
     *                comply with RFC 2109.
     */
    public final void setVersion(int version) {
        cookie.setVersion(version);
    }

    /**
     * Bridges the gap between Servlet 2.5 and 3.0 APIs by adding support for HttpOnly cookies which the Servlet 2.5 API
     * does not support.
     *
     * @since 1.5.0
     */
    private static final class CookieV2x extends Cookie {

        private boolean isHttpOnly;

        private CookieV2x(String name, String value) {
            super(name, value);
        }

        private CookieV2x(javax.servlet.http.Cookie cookie) {
            super(cookie);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isHttpOnly() {
            return isHttpOnly;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setHttpOnly(boolean isHttpOnly) {
            this.isHttpOnly = isHttpOnly;
        }
    }

    /**
     * Creates a new {@code Cookie} with the provided name and value.
     *
     * @param name The name of the cookie.
     * @param value The value of the Cookie.
     * @return A {@code Cookie} instance.
     * @throws IllegalArgumentException If the cookie name is {@code null} or empty or contains any illegal characters
     * (for example, a comma, space or semicolon) or matches a token reserved for use by the cookie protocol.
     */
    public static Cookie newCookie(String name, String value) {

        if (isServlet3xPresent) {
            return new Cookie(name, value);
        } else {
            return new CookieV2x(name, value);
        }
    }

    static Cookie newCookie(javax.servlet.http.Cookie cookie) {

        if (isServlet3xPresent) {
            return new Cookie(cookie);
        } else {
            return new CookieV2x(cookie);
        }
    }

    /**
     * Adds the provided cookie to the provided {@code HttpServletResponse} as a 'Set-Cookie' header.
     *
     * @param cookie The cookie to add to the response.
     * @param resp The response to add the cookie to.
     */
    public static void addCookie(Cookie cookie, HttpServletResponse resp) {

        if (isServlet3xPresent) {
            resp.addCookie(cookie.cookie);
        } else {
            resp.addHeader("Set-Cookie", SetCookieSupport.generateHeader(cookie));
        }
    }

    /**
     * Adds the provided cookies to the provided {@code HttpServletResponse} as a 'Set-Cookie' header.
     *
     * @param cookies The cookies to add to the response.
     * @param resp The response to add the cookie to.
     */
    public static void addCookies(Collection<Cookie> cookies, HttpServletResponse resp) {
        for (Cookie cookie : cookies) {
            addCookie(cookie, resp);
        }
    }

    /**
     * <p>Gets all of the {@code Cookie}s from the provided {@code HttpServletRequest}.</p>
     *
     * <p>Use this method to convert {@link javax.servlet.http.Cookie}s into {@link Cookie}s so as to use as API which
     * supports both the Servlet 2.5 and 3.0 Cookie API.</p>
     *
     * @param request The request to get the cookies from.
     * @return A {@code Set} of {@code Cookie}s.
     */
    public static Set<Cookie> getCookies(HttpServletRequest request) {
        Set<Cookie> cookies = new HashSet<>();
        if (request.getCookies() != null) {
            for (javax.servlet.http.Cookie cookie : request.getCookies()) {
                cookies.add(newCookie(cookie));
            }
        }
        return cookies;
    }

    private static boolean isHttpOnlyCookieSupported() {
        try {
            javax.servlet.http.Cookie.class.getMethod("setHttpOnly", boolean.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
