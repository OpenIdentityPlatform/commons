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

package org.forgerock.http.header;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.forgerock.http.protocol.Message;
import org.forgerock.http.util.CaseInsensitiveMap;

/**
 * Utility class for processing values in HTTP header fields.
 */
public final class HeaderUtil {

    /**
     * RFC 1123 {@code HTTP-date} format from <a href="https://tools.ietf.org/html/rfc2616#page-20">RFC 2616</a>
     * section 3.3, which is the preferred standard format.
     */
    private static final String HTTP_DATE_RFC_1123_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";

    /**
     * Legacy RFC 850 date format from <a href="https://tools.ietf.org/html/rfc2616#page-20">RFC 2616</a> section 3.3,
     * which should be supported for parsing only.
     */
    private static final String LEGACY_RFC_850_DATE_FORMAT = "E, dd-MMM-yy HH:mm:ss zzz";

    /**
     * Legacy ANSI C {@code asctime()} date format from
     * <a href="https://tools.ietf.org/html/rfc2616#page-20">RFC 2616</a>
     * section 3.3, which should be supported for parsing only.
     */
    private static final String LEGACY_ANSI_C_DATE_FORMAT = "EEE MMM d HH:mm:ss yyyy";

    /**
     * Regex that matches escaped backslash and double-quote characters, into two matching groups where group 1
     * contains the escape character (which we will discard) and group 2 contains the character we want to retain.
     */
    private static final Pattern UNQUOTE_PATTERN = Pattern.compile("(\\\\)(\\\\|[\"])");

    /** Static methods only. */
    private HeaderUtil() {
        // No implementation required.
    }

    /**
     * Parses an HTTP header value, splitting it into multiple values around the
     * specified separator. Quoted strings are not split into multiple values if
     * they contain separator characters. All leading and trailing white space
     * in values is trimmed. All quotations remain intact.
     * <p>
     * Note: This method is liberal in its interpretation of malformed header
     * values; namely the incorrect use of string and character quoting
     * mechanisms and unquoted white space. If a {@code null} or empty string is
     * supplied as a value, this method yields an empty list.
     *
     * @param value
     *            the header value to be split.
     * @param separator
     *            the separator character to split headers around.
     * @return A list of string representing the split values of the header.
     */
    public static List<String> split(final String value, final char separator) {
        if (separator == '"' || separator == '\\') {
            throw new IllegalArgumentException("invalid separator: " + separator);
        }
        final ArrayList<String> values = new ArrayList<>();
        if (value != null) {
            int length = value.length();
            final StringBuilder sb = new StringBuilder();
            boolean escaped = false;
            boolean quoted = false;
            for (int n = 0, cp; n < length; n += Character.charCount(cp)) {
                cp = value.codePointAt(n);
                if (escaped) {
                    // single-character quoting mechanism per RFC 2616 §2.2
                    sb.appendCodePoint(cp);
                    escaped = false;
                } else if (cp == '\\') {
                    sb.appendCodePoint(cp);
                    if (quoted) {
                        // single-character quoting mechanism per RFC 2616 §2.2
                        escaped = true;
                    }
                } else if (cp == '"') {
                    // quotation marks remain intact here
                    sb.appendCodePoint(cp);
                    quoted = !quoted;
                } else if (cp == separator && !quoted) {
                    // only separator if not in quoted string
                    String s = sb.toString().trim();
                    if (s.length() > 0) {
                        values.add(s);
                    }
                    // reset for next token
                    sb.setLength(0);
                } else {
                    sb.appendCodePoint(cp);
                }
            }
            final String s = sb.toString().trim();
            if (s.length() > 0) {
                values.add(s);
            }
        }
        return values;
    }

    /**
     * Joins a collection of header values into a single header value, with a
     * specified specified separator. A {@code null} or empty collection of
     * header values yeilds a {@code null} return value.
     *
     * @param values
     *            the values to be joined.
     * @param separator
     *            the separator to separate values within the returned value.
     * @return a single header value, with values separated by the separator.
     */
    public static String join(final Collection<String> values, final char separator) {
        if (separator == '"' || separator == '\\') {
            throw new IllegalArgumentException("invalid separator: " + separator);
        }
        final StringBuilder sb = new StringBuilder();
        if (values != null) {
            for (final String s : values) {
                if (s != null) {
                    if (sb.length() > 0) {
                        sb.append(separator).append(' ');
                    }
                    sb.append(s);
                }
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    /**
     * Splits a single HTTP header parameter name and value from an input string
     * value. The input string value is presumed to have been extracted from a
     * collection provided by the {@link #split(String, char)} method.
     * <p>
     * This method returns the parameter name-value pair split into an array of
     * {@code String}s. Element {@code [0]} contains the parameter name; element
     * {@code [1]} contains contains the parameter value or {@code null} if
     * there is no value.
     * <p>
     * A value that is contained within a quoted-string is processed such that
     * the surrounding '"' (quotation mark) characters are removed and
     * single-character quotations hold the character being quoted without the
     * escape '\' (backslash) character. All white space outside of the
     * quoted-string is removed. White space within the quoted-string is
     * retained.
     * <p>
     * Note: This method is liberal in its interpretation of a malformed header
     * value; namely the incorrect use of string and character quoting
     * mechanisms and unquoted white space.
     *
     * @param value
     *            the string to parse the name-value parameter from.
     * @return the name-value pair split into a {@code String} array.
     */
    public static String[] parseParameter(final String value) {
        String[] ss = new String[2];
        boolean inValue = false;
        boolean quoted = false;
        boolean escaped = false;
        int length = value.length();
        final StringBuilder sb = new StringBuilder();
        for (int n = 0, cp; n < length; n += Character.charCount(cp)) {
            cp = value.codePointAt(n);
            if (escaped) {
                // single-character quoting mechanism per RFC 2616 §2.2
                sb.appendCodePoint(cp);
                escaped = false;
            } else if (cp == '\\') {
                if (quoted) {
                    // next character is literal
                    escaped = true;
                } else {
                    // not quoted, push the backslash literal (header probably malformed)
                    sb.appendCodePoint(cp);
                }
            } else if (cp == '"') {
                // toggle quoted status
                quoted = !quoted;
            } else if (!quoted && !inValue && cp == '=') {
                // only separator if in key and not in quoted-string
                ss[0] = sb.toString().trim();
                // reset for next token
                sb.setLength(0);
                inValue = true;
            } else if (!quoted && Character.isWhitespace(cp)) {
                // drop unquoted white space (header probably malformed if not at beginning or end)
            } else {
                sb.appendCodePoint(cp);
            }
        }
        if (!inValue) {
            ss[0] = sb.toString().trim();
        } else {
            ss[1] = sb.toString();
        }
        return ss;
    }

    /**
     * Parses a set of HTTP header parameters from a collection of values. The
     * input collection of values is presumed to have been provided from the
     * {@link #split(String, char)} method.
     * <p>
     * A well-formed parameter contains an attribute and optional value,
     * separated by an '=' (equals sign) character. If the parameter contains no
     * value, it is represented by a {@code null} value in the returned map.
     * <p>
     * Values that are contained in quoted-strings are processed such that the
     * surrounding '"' (quotation mark) characters are removed and
     * single-character quotations hold the character being quoted without the
     * escape '\' (backslash) character. All white space outside of
     * quoted-strings is removed. White space within quoted-strings is retained.
     * <p>
     * Note: This method is liberal in its interpretation of malformed header
     * values; namely the incorrect use of string and character quoting
     * mechanisms and unquoted white space.
     *
     * @param values
     *            the HTTP header parameters.
     * @return a map of parameter name-value pairs.
     */
    public static Map<String, String> parseParameters(final Collection<String> values) {
        final CaseInsensitiveMap<String> map = new CaseInsensitiveMap<>(new HashMap<String, String>());
        if (values != null) {
            for (final String value : values) {
                final String[] param = parseParameter(value);
                if (param[0] != null && param[0].length() > 0 && !map.containsKey(param[0])) {
                    map.put(param[0], param[1]);
                }
            }
        }
        return map;
    }

    /**
     * Encloses a string in quotation marks. Quotation marks and backslash
     * characters are escaped with the single-character quoting mechanism. For
     * more information, see <a href="http://www.ietf.org/rfc/rfc2616.txt">RFC
     * 2616</a> §2.2.
     *
     * @param value
     *            the value to be enclosed in quotation marks.
     * @return the value enclosed in quotation marks.
     */
    public static String quote(final String value) {
        if (value == null) {
            return null;
        }
        final StringBuilder sb = new StringBuilder("\"");
        int length = value.length();
        for (int n = 0, cp; n < length; n += Character.charCount(cp)) {
            cp = value.codePointAt(n);
            if (cp == '\\' || cp == '"') {
                sb.append('\\');
            }
            sb.appendCodePoint(cp);
        }
        sb.append('"');
        return sb.toString();
    }

    /**
     * Unquotes a string following the logic of {@link #quote(String)}.
     *
     * @param value Value to unquote
     * @return Unquoted value
     */
    public static String unquote(final String value) {
        if (value == null) {
            return null;
        }
        final int n = value.length();
        if (n < 2 || value.charAt(0) != '"' || value.charAt(n - 1) != '"') {
            throw new IllegalArgumentException("value is not quoted");
        }
        return UNQUOTE_PATTERN.matcher(value.substring(1, n - 1)).replaceAll("$2");
    }

    /**
     * Parses the named header from the message as a multi-valued comma
     * separated value. If there are multiple headers present then they are
     * first merged and then {@link #split(String, char) split}.
     *
     * @param message
     *            The HTTP request or response.
     * @param name
     *            The name of the header.
     * @return A list of strings representing the split values of the header,
     *         which may be empty if the header was not present in the message.
     */
    public static List<String> parseMultiValuedHeader(Message message, String name) {
        final Collection<String> values = message != null && message.getHeaders().containsKey(name)
                ? message.getHeaders().get(name).getValues() : null;
        return parseMultiValuedHeader(join(values, ','));
    }

    /**
     * Parses the header content as a multi-valued comma separated value.
     *
     * @param header
     *            The HTTP header content.
     * @return A list of strings representing the split values of the header,
     *         which may be empty if the header was {@code null} or empty.
     */
    public static List<String> parseMultiValuedHeader(final String header) {
        return split(header, ',');
    }

    /**
     * Parses the named single-valued header from the message. If there are
     * multiple headers present then only the first is used.
     *
     * @param message
     *            The HTTP request or response.
     * @param name
     *            The name of the header.
     * @return The header value, or {@code null} if the header was not present
     *         in the message.
     */
    public static String parseSingleValuedHeader(Message message, String name) {
        if (message == null || !message.getHeaders().containsKey(name)) {
            return null;
        }
        final Iterator<String> iterator = message.getHeaders().get(name).getValues().iterator();
        final String header = iterator.hasNext()
                ? iterator.next() : null;
        return header != null ? header : null;
    }

    /**
     * Formats a {@code HTTP-date} using RFC 1123 format as specified in
     * <a href="https://tools.ietf.org/html/rfc2616#page-20">RFC 2616</a>.
     *
     * @param date {@link Date} to format
     * @return Formatted {@code HTTP-date}
     */
    public static String formatDate(final Date date) {
        return getDateFormatter(HTTP_DATE_RFC_1123_DATE_FORMAT).format(date);
    }

    /**
     * Parses the supported {@code HTTP-date} formats as specified in
     * <a href="https://tools.ietf.org/html/rfc2616#page-20">RFC 2616</a>.
     *
     * @param s Date {@link String}
     * @return {@link Date} instance, or {@code null} if unable to parse the date or {@code s} is {@code null}
     */
    public static Date parseDate(final String s) {
        if (s == null) {
            return null;
        }
        Date date = parseDate(s, HTTP_DATE_RFC_1123_DATE_FORMAT);
        if (date != null) {
            return date;
        }
        date = parseDate(s, LEGACY_RFC_850_DATE_FORMAT);
        if (date != null) {
            return date;
        }
        return parseDate(s, LEGACY_ANSI_C_DATE_FORMAT);
    }

    /**
     * Parses a date {@link String} using the provided date format.
     *
     * @param s      Date {@link String}
     * @param format Date format
     * @return {@link Date} instance, or {@code null} if unable to parse the date
     */
    private static Date parseDate(final String s, final String format) {
        try {
            return getDateFormatter(format).parse(s);
        } catch (ParseException eee) {
            return null;
        }
    }

    /**
     * Builds a date formatter, configured to use GMT time zone, which is not thread-safe.
     *
     * @param format Date format
     * @return Date formatter using GMT time zone
     */
    private static SimpleDateFormat getDateFormatter(final String format) {
        final SimpleDateFormat formatter = new SimpleDateFormat(format, Locale.ROOT);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        return formatter;
    }
}
