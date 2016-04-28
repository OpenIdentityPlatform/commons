//@Checkstyle:ignoreFor 24
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Copied from, https://github.com/apache/tomcat/blob/trunk/java/org/apache/tomcat/util/http/SetCookieSupport.java
 *
 * Portions Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.caf.http;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Support class for generating Set-Cookie header values.
 *
 * @since 12.0.0
 */
public class SetCookieSupport {

    // Other fields
    private static final String OLD_COOKIE_PATTERN = "EEE, dd-MMM-yyyy HH:mm:ss z";
    private static final ThreadLocal<DateFormat> OLD_COOKIE_FORMAT =
        new ThreadLocal<DateFormat>() {
            @Override
            protected DateFormat initialValue() {
                DateFormat df =
                        new SimpleDateFormat(OLD_COOKIE_PATTERN, Locale.US);
                df.setTimeZone(TimeZone.getTimeZone("GMT"));
                return df;
            }
        };
    private static final String ANCIENT_DATE;

    static {
        ANCIENT_DATE = OLD_COOKIE_FORMAT.get().format(new Date(10000));
    }

    /**
     * Method added to support transforming javax.servlet.http.Cookie instances into the String format needed by the
     * AdviceContext.
     * @param cookie The javax.servlet.http.Cookie instance whose String representation is desired
     * @return the String representation of the cookie parameter
     */
    public String generateHeader(javax.servlet.http.Cookie cookie) {
        return generateHeader(Cookie.newCookie(cookie));
    }

    /**
     * Generates the 'Set-Cookie' header string for the provided {@code Cookie}.
     *
     * @param cookie The {@code Cookie} to generate the header string for.
     * @return The {@code String} representation of the {@code Cookie}.
     */
    static String generateHeader(Cookie cookie) {
        /*
         * The spec allows some latitude on when to send the version attribute
         * with a Set-Cookie header. To be nice to clients, we'll make sure the
         * version attribute is first. That means checking the various things
         * that can cause us to switch to a v1 cookie first.
         *
         * Note that by checking for tokens we will also throw an exception if a
         * control character is encountered.
         */
        int version = cookie.getVersion();
        String value = cookie.getValue();
        String path = cookie.getPath();
        String domain = cookie.getDomain();
        String comment = cookie.getComment();

        if (version == 0) {
            // Check for the things that require a v1 cookie
            if (needsQuotes(value) || comment != null || needsQuotes(path) || needsQuotes(domain)) {
                version = 1;
            }
        }

        // Now build the cookie header
        StringBuffer buf = new StringBuffer(); // can't use StringBuilder due to DateFormat

        // Just use the name supplied in the Cookie
        buf.append(cookie.getName());
        buf.append("=");

        // Value
        maybeQuote(buf, value);

        // Add version 1 specific information
        if (version == 1) {
            // Version=1 ... required
            buf.append("; Version=1");

            // Comment=comment
            if (comment != null) {
                buf.append("; Comment=");
                maybeQuote(buf, comment);
            }
        }

        // Add domain information, if present
        if (domain != null) {
            buf.append("; Domain=");
            maybeQuote(buf, domain);
        }

        // Max-Age=secs ... or use old "Expires" format
        int maxAge = cookie.getMaxAge();
        if (maxAge >= 0) {
            if (version > 0) {
                buf.append("; Max-Age=");
                buf.append(maxAge);
            }
            // IE6, IE7 and possibly other browsers don't understand Max-Age.
            // They do understand Expires, even with V1 cookies!
            if (version == 0) {
                // Wdy, DD-Mon-YY HH:MM:SS GMT ( Expires Netscape format )
                buf.append("; Expires=");
                // To expire immediately we need to set the time in past
                if (maxAge == 0) {
                    buf.append(ANCIENT_DATE);
                } else {
                    OLD_COOKIE_FORMAT.get().format(
                            new Date(System.currentTimeMillis() + maxAge * 1000L),
                            buf,
                            new FieldPosition(0));
                }
            }
        }

        // Path=path
        if (path != null) {
            buf.append("; Path=");
            maybeQuote(buf, path);
        }

        // Secure
        if (cookie.isSecure()) {
            buf.append("; Secure");
        }

        // HttpOnly
        if (cookie.isHttpOnly()) {
            buf.append("; HttpOnly");
        }
        return buf.toString();
    }

    private static void maybeQuote(StringBuffer buf, String value) {
        if (value == null || value.isEmpty()) {
            buf.append("\"\"");
        } else if (alreadyQuoted(value)) {
            buf.append('"');
            escapeDoubleQuotes(buf, value, 1, value.length() - 1);
            buf.append('"');
        } else if (needsQuotes(value)) {
            buf.append('"');
            escapeDoubleQuotes(buf, value, 0, value.length());
            buf.append('"');
        } else {
            buf.append(value);
        }
    }

    private static void escapeDoubleQuotes(StringBuffer b, String s, int beginIndex, int endIndex) {
        if (s.indexOf('"') == -1 && s.indexOf('\\') == -1) {
            b.append(s);
            return;
        }

        for (int i = beginIndex; i < endIndex; i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                b.append('\\').append('\\');
            } else if (c == '"') {
                b.append('\\').append('"');
            } else {
                b.append(c);
            }
        }
    }

    private static boolean needsQuotes(String value) {
        if (value == null) {
            return false;
        }

        int i = 0;
        int len = value.length();

        if (alreadyQuoted(value)) {
            i++;
            len--;
        }

        for (; i < len; i++) {
            char c = value.charAt(i);
            if ((c < 0x20 && c != '\t') || c >= 0x7f) {
                throw new IllegalArgumentException(
                        "Control character in cookie value or attribute.");
            }
        }
        return false;
    }

    private static boolean alreadyQuoted(String value) {
        return value.length() >= 2
                && value.charAt(0) == '\"'
                && value.charAt(value.length() - 1) == '\"';
    }
}
