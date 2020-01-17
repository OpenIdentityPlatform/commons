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
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2010–2011 ApexIdentity Inc.
 * Portions Copyright 2011-2016 ForgeRock AS.
 */

package org.forgerock.http.util;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.Locale;

import org.forgerock.http.protocol.Form;

/**
 * Utility class for performing operations on universal resource identifiers.
 */
public final class Uris {

    /** Non-safe characters are escaped as UTF-8 octets using "%" HEXDIG HEXDIG production. */
    private static final char URL_ESCAPE_CHAR = '%';

    /** Look up table for characters which do not need URL encoding in path elements according to RFC 3986. */
    private static final BitSet SAFE_URL_PCHAR_CHARS = new BitSet(128);

    /** Look up table for characters which do not need URL encoding in query string parameters according to RFC 3986. */
    private static final BitSet SAFE_URL_QUERY_CHARS = new BitSet(128);

    /** Look up table for characters which do not need URL encoding in fragments according to RFC 3986. */
    private static final BitSet SAFE_URL_FRAGMENT_CHARS = new BitSet(128);

    /** Look up table for characters which do not need URL encoding in userInfo according to RFC 3986. */
    private static final BitSet SAFE_URL_USERINFO_CHARS = new BitSet(128);

    static {
        /*
         * pchar       = unreserved / pct-encoded / sub-delims / ":" / "@"
         *
         * pct-encoded = "%" HEXDIG HEXDIG
         * unreserved  = ALPHA / DIGIT / "-" / "." / "_" / "~"
         * sub-delims  = "!" / "$" / "&" / "'" / "(" / ")" / "*" / "+" / "," / ";" / "="
         */
        for (char c : "-._~!$&'()*+,;=:@".toCharArray()) {
            SAFE_URL_PCHAR_CHARS.set(c);
        }
        SAFE_URL_PCHAR_CHARS.set('0', '9' + 1);
        SAFE_URL_PCHAR_CHARS.set('a', 'z' + 1);
        SAFE_URL_PCHAR_CHARS.set('A', 'Z' + 1);

        // query = *( pchar / "/" / "?" ) - also encode ? and & since these are parameter separators,
        //                                  as well as + which is used for encoding white space (see w3c).
        SAFE_URL_QUERY_CHARS.or(SAFE_URL_PCHAR_CHARS);
        SAFE_URL_QUERY_CHARS.set('/');
        SAFE_URL_QUERY_CHARS.set('?');
        SAFE_URL_QUERY_CHARS.clear('&');
        SAFE_URL_QUERY_CHARS.clear('=');
        SAFE_URL_QUERY_CHARS.clear('+');

        // fragment = *( pchar / "/" / "?" )
        SAFE_URL_FRAGMENT_CHARS.or(SAFE_URL_PCHAR_CHARS);
        SAFE_URL_FRAGMENT_CHARS.set('/');
        SAFE_URL_FRAGMENT_CHARS.set('?');

        // userInfo = *( unreserved / pct-encoded / sub-delims / ":" )
        SAFE_URL_USERINFO_CHARS.or(SAFE_URL_PCHAR_CHARS);
        SAFE_URL_USERINFO_CHARS.clear('@');
    }

    /**
     * Fast lookup for encoding octets as hex.
     */
    private static final String[] BYTE_TO_HEX = new String[256];
    static {
        for (int i = 0; i < BYTE_TO_HEX.length; i++) {
            BYTE_TO_HEX[i] = String.format(Locale.ROOT, "%02X", i);
        }
    }

    /** Static methods only. */
    private Uris() {
    }

    /**
     * Returns a hierarchical URI constructed from the given components. Differs from the URI
     * constructor by accepting raw versions of userInfo, path, query and fragment components.
     * <p>
     * Unlike {@link #createNonStrict}, this method does not tolerate invalid characters, such
     * as double-quotes, in the query string.
     *
     * @param scheme the scheme component of the URI or {@code null} if none.
     * @param rawUserInfo the raw user-information component of the URI or {@code null} if none.
     * @param host the host component of the URI or {@code null} if none.
     * @param port the port number of the URI or {@code -1} if none.
     * @param rawPath the raw path component of the URI or {@code null} if none.
     * @param rawQuery the raw query component of the URI or {@code null} if none. The raw query must not contain
     *                 characters that should have been percent encoded.
     * @param rawFragment the raw fragment component of the URI or {@code null} if none.
     * @return the URI constructed from the given components.
     * @throws URISyntaxException if the resulting URI would be malformed per RFC 2396.
     */
    public static URI create(String scheme, String rawUserInfo, String host, int port,
            String rawPath, String rawQuery, String rawFragment) throws URISyntaxException {
        StringBuilder sb = new StringBuilder();
        if (scheme != null) {
            sb.append(scheme).append(':');
        }
        if (host != null) {
            sb.append("//");
        }
        if (rawUserInfo != null) {
            sb.append(rawUserInfo).append('@');
        }
        if (host != null) {
            sb.append(host);
            if (port != -1) {
                sb.append(':').append(Integer.toString(port));
            }
        }
        if (rawPath != null) {
            sb.append(rawPath);
        }
        if (rawQuery != null) {
            sb.append('?').append(rawQuery);
        }
        if (rawFragment != null) {
            sb.append("#").append(rawFragment);
        }
        return new URI(sb.toString());
    }

    /**
     * Returns a hierarchical URI constructed from the given components. Differs from the URI
     * constructor by accepting raw versions of userInfo, path, query and fragment components.
     * <p>
     * Unlike {@link #create}, this method tolerates invalid characters, such as double-quotes,
     * in the query string.
     *
     * @param scheme the scheme component of the URI or {@code null} if none.
     * @param rawUserInfo the raw user-information component of the URI or {@code null} if none.
     * @param host the host component of the URI or {@code null} if none.
     * @param port the port number of the URI or {@code -1} if none.
     * @param rawPath the raw path component of the URI or {@code null} if none.
     * @param rawQuery the raw query component of the URI or {@code null} if none. The raw query may contain
     *                 characters that should have been percent encoded.
     * @param rawFragment the raw fragment component of the URI or {@code null} if none.
     * @return the URI constructed from the given components.
     * @throws URISyntaxException if the resulting URI would be malformed per RFC 2396.
     */
    public static URI createNonStrict(String scheme, String rawUserInfo, String host, int port,
                             String rawPath, String rawQuery, String rawFragment) throws URISyntaxException {
        return create(scheme, rawUserInfo, host, port, rawPath, asSafeQuery(rawQuery), rawFragment);
    }

    private static String asSafeQuery(final String rawQuery) throws URISyntaxException {
        if (rawQuery == null) {
            return null;
        }
        // Allocate a bit of extra padding in case a couple of characters need % encoding.
        StringBuilder builder = new StringBuilder(rawQuery.length() + 8);
        for (String param : rawQuery.split("&")) {
            String[] nv = param.split("=", 2);
            if (!nv[0].isEmpty()) {
                if (builder.length() > 0) {
                    builder.append('&');
                }
                try {
                    String name = urlDecodeQueryParameterNameOrValue(nv[0]);
                    builder.append(urlEncodeQueryParameterNameOrValue(name));
                    if (nv.length == 2) {
                        String value = urlDecodeQueryParameterNameOrValue(nv[1]);
                        builder.append('=').append(urlEncodeQueryParameterNameOrValue(value));
                    }
                } catch (Exception e) {
                    throw new URISyntaxException(rawQuery, "The URL query string could not be decoded");
                }
            }
        }
        return builder.toString();
    }

    /**
     * Changes the base scheme, host and port of a request to that specified in a base URI,
     * or leaves them unchanged if the base URI is {@code null}. This implementation only
     * uses scheme, host and port. The remaining components of the URI remain intact.
     *
     * @param uri the URI whose base is to be changed.
     * @param base the URI to base the other URI on.
     * @return the the URI with the new established base.
     */
    public static URI rebase(URI uri, URI base)  {
        if (base == null) {
            return uri;
        }
        String scheme = base.getScheme();
        String host = base.getHost();
        int port = base.getPort();
        if (scheme == null || host == null) {
            return uri;
        }
        try {
            return create(scheme, uri.getRawUserInfo(), host, port, uri.getRawPath(),
                    uri.getRawQuery(), uri.getRawFragment());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns a new URI having the provided query parameters. The scheme,
     * authority, path, and fragment remain unchanged.
     *
     * @param uri
     *            the URI whose query is to be changed.
     * @param query
     *            the form containing the query parameters.
     * @return a new URI having the provided query parameters. The scheme,
     *         authority, path, and fragment remain unchanged.
     */
    public static URI withQuery(final URI uri, final Form query) {
        try {
            return create(uri.getScheme(), uri.getRawUserInfo(), uri.getHost(), uri.getPort(),
                    uri.getRawPath(), query.toQueryString(), uri.getRawFragment());
        } catch (final URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns a new URI having the same scheme, authority and path, but no
     * query nor fragment.
     *
     * @param uri
     *            the URI whose query and fragments are to be removed.
     * @return a new URI having the same scheme, authority and path, but no
     *         query nor fragment.
     */
    public static URI withoutQueryAndFragment(final URI uri) {
        try {
            return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), null, null);
        } catch (final URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Decodes the provided form encoded parameter name or value as per application/x-www-form-urlencoded.
     *
     * @param nameOrValue
     *         the form encoded parameter name or value, which may be {@code null}.
     * @return the decoded form parameter name or value, or {@code null} if {@code nameOrValue} was {@code null}.
     */
    public static String formDecodeParameterNameOrValue(String nameOrValue) {
        try {
            return nameOrValue != null ? URLDecoder.decode(nameOrValue, "UTF-8") : null;
        } catch (UnsupportedEncodingException e) {
            return nameOrValue;
        }
    }

    /**
     * Form encodes the provided parameter name or value as per application/x-www-form-urlencoded.
     *
     * @param nameOrValue
     *         the parameter name or value, which may be {@code null}.
     * @return the form encoded parameter name or value, or {@code null} if {@code nameOrValue} was {@code null}.
     */
    public static String formEncodeParameterNameOrValue(String nameOrValue) {
        try {
            return nameOrValue != null ? URLEncoder.encode(nameOrValue, "UTF-8") : null;
        } catch (UnsupportedEncodingException e) {
            return nameOrValue;
        }
    }

    /**
     * Decodes the provided URL encoded path element as per RFC 3986.
     *
     * @param pathElement
     *            the URL encoded path element, which may be {@code null}.
     * @return the decoded path element, or {@code null} if {@code pathElement} was {@code null}.
     */
    public static String urlDecodePathElement(String pathElement) {
        return urlDecode(pathElement, false);
    }

    /**
     * URL encodes the provided path element as per RFC 3986.
     *
     * @param pathElement
     *         the path element, which may be {@code null}.
     * @return the URL encoded path element, or {@code null} if {@code pathElement} was {@code null}.
     */
    public static String urlEncodePathElement(String pathElement) {
        return urlEncode(pathElement, SAFE_URL_PCHAR_CHARS);
    }

    /**
     * Decodes the provided URL encoded query parameter name or value as per RFC 3986.
     *
     * @param nameOrValue
     *            the URL encoded query parameter name or value, which may be {@code null}.
     * @return the decoded query parameter name or value, or {@code null} if {@code nameOrValue} was {@code null}.
     */
    public static String urlDecodeQueryParameterNameOrValue(String nameOrValue) {
        return urlDecode(nameOrValue, true);
    }

    /**
     * URL encodes the provided query parameter name or value as per RFC 3986. Note that this method does not
     * adhere to the "query" production in RFC 3986, because it is intended for encoding query parameter names or
     * values. Therefore, this method will encode '?' and '=' characters.
     *
     * @param nameOrValue
     *         the query parameter name or value, which may be {@code null}.
     * @return the URL encoded query parameter name or value, or {@code null} if {@code nameOrValue} was {@code null}.
     */
    public static String urlEncodeQueryParameterNameOrValue(String nameOrValue) {
        return urlEncode(nameOrValue, SAFE_URL_QUERY_CHARS);
    }

    /**
     * Decodes the provided URL encoded fragment as per RFC 3986.
     *
     * @param fragment
     *            the URL encoded fragment, which may be {@code null}.
     * @return the decoded fragment, or {@code null} if {@code fragment} was {@code null}.
     */
    public static String urlDecodeFragment(String fragment) {
        return urlDecode(fragment, false);
    }

    /**
     * URL encodes the provided fragment as per RFC 3986.
     *
     * @param fragment
     *         the fragment, which may be {@code null}.
     * @return the URL encoded fragment, or {@code null} if {@code fragment} was {@code null}.
     */
    public static String urlEncodeFragment(String fragment) {
        return urlEncode(fragment, SAFE_URL_FRAGMENT_CHARS);
    }

    /**
     * Decodes the provided URL encoded userInfo as per RFC 3986.
     *
     * @param userInfo
     *            the URL encoded userInfo, which may be {@code null}.
     * @return the decoded userInfo, or {@code null} if {@code userInfo} was {@code null}.
     */
    public static String urlDecodeUserInfo(String userInfo) {
        return urlDecode(userInfo, false);
    }

    /**
     * URL encodes the provided userInfo as per RFC 3986.
     *
     * @param userInfo
     *         the userInfo, which may be {@code null}.
     * @return the URL encoded userInfo, or {@code null} if {@code userInfo} was {@code null}.
     */
    public static String urlEncodeUserInfo(String userInfo) {
        return urlEncode(userInfo, SAFE_URL_USERINFO_CHARS);
    }

    private static String urlDecode(final String s, final boolean decodePlusToSpace) {
        if (s == null) {
            return null;
        }
        // First try fast-path decode of simple ASCII.
        final int size = s.length();
        for (int i = 0; i < size; i++) {
            final char c = s.charAt(i);
            if (isUrlEscapeChar(c) || (decodePlusToSpace && c == '+')) {
                // Slow path.
                return urlDecode0(s, decodePlusToSpace);
            }
        }
        return s;
    }

    private static String urlDecode0(final String s, final boolean decodePlusToSpace) {
        final StringBuilder builder = new StringBuilder(s.length());
        final int size = s.length();
        final byte[] buffer = new byte[size / 3];
        for (int i = 0; i < size;) {
            final char c = s.charAt(i);
            if (decodePlusToSpace && c == '+') {
                builder.append(' ');
                i++;
            } else if (!isUrlEscapeChar(c)) {
                builder.append(c);
                i++;
            } else {
                int bufferPos = 0;
                for (; i < size && isUrlEscapeChar(s.charAt(i)); i += 3) {
                    if ((i + 2) >= size) {
                        throw new IllegalArgumentException(
                                "Path contains an incomplete percent encoding");
                    }
                    final String hexPair = s.substring(i + 1, i + 3);
                    try {
                        final int octet = Integer.parseInt(hexPair, 16);
                        if (octet < 0) {
                            throw new IllegalArgumentException(
                                    "Path contains an invalid percent encoding '" + hexPair + "'");
                        }
                        buffer[bufferPos++] = (byte) octet;
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "Path contains an invalid percent encoding '" + hexPair + "'");
                    }
                }
                builder.append(new String(buffer, 0, bufferPos, StandardCharsets.UTF_8));
            }
        }
        return builder.toString();
    }

    private static String urlEncode(final String s, final BitSet safeChars) {
        if (s == null) {
            return null;
        }
        // First try fast-path encode of simple ASCII.
        final int size = s.length();
        for (int i = 0; i < size; i++) {
            final int c = s.charAt(i);
            if (!safeChars.get(c)) {
                // Slow path.
                return urlEncode0(s, safeChars);
            }
        }
        return s;
    }

    private static String urlEncode0(String s, final BitSet safeChars) {
        final byte[] utf8 = s.getBytes(StandardCharsets.UTF_8);
        final int size = utf8.length;
        final StringBuilder builder = new StringBuilder(size + 16);
        for (final byte b : utf8) {
            final int octet = b & 0xff;
            if (safeChars.get(octet)) {
                builder.append((char) octet);
            } else {
                builder.append(URL_ESCAPE_CHAR);
                builder.append(BYTE_TO_HEX[octet]);
            }
        }
        return builder.toString();
    }

    private static boolean isUrlEscapeChar(final char c) {
        return c == URL_ESCAPE_CHAR;
    }
}
