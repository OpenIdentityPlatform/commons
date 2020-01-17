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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.http.header;

import java.util.Date;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.forgerock.util.Reject;

/**
 * {@link WarningHeader} entry. This class is immutable and thread-safe.
 */
public class Warning {

    /**
     * Regex that matches the various parts of a Warning Header value. The match groups are,
     * <ol>
     * <li>warn-code</li>
     * <li>warn-agent</li>
     * <li>warn-text [warn-date]</li>
     * </ol>
     */
    private static final Pattern PATTERN = Pattern.compile("([0-9]{3}) ([^ ]+) ([\"].*[\"])");

    private final int code;
    private final String agent;
    private final String text;
    private final Date date;

    /**
     * Creates a new instance without optional date.
     *
     * @param code Three digit code
     * @param agent Name or {@code host[:port]} of the server adding the header
     * @param text Text
     */
    public Warning(int code, String agent, String text) {
        this(code, agent, text, null);
    }

    /**
     * Creates a new instance <i>with</i> optional date.
     *
     * @param code Three digit code
     * @param agent Name or {@code host[:port]} of the server adding the header
     * @param text Text (unquoted)
     * @param date Date or {@code null}
     */
    public Warning(int code, String agent, String text, Date date) {
        Reject.ifNull(agent, text);
        this.code = code;
        this.agent = agent;
        this.text = text;
        this.date = date;
    }

    /**
     * Gets the warning's three digit code.
     *
     * @return Three digit code
     */
    public int getCode() {
        return code;
    }

    /**
     * Gets the warning's agent name.
     *
     * @return Name or {@code host[:port]} of the server adding the header
     */
    public String getAgent() {
        return agent;
    }

    /**
     * Gets the warning's text description.
     *
     * @return Text
     */
    public String getText() {
        return text;
    }

    /**
     * Gets the warning's date.
     *
     * @return {@link Date} or {@code null} if not defined
     */
    public Date getDate() {
        return date;
    }

    /**
     * Formats a {@code Warning} header value, according to
     * <a href="http://www.ietf.org/rfc/rfc2616.txt">RFC 2616</a> 14.46.
     *
     * @return Formatted {@code Warning} header
     */
    @Override
    public String toString() {
        String s = String.valueOf(code) + ' ' + agent + ' ' + HeaderUtil.quote(text);
        if (date != null) {
            s += " \"" + HeaderUtil.formatDate(date) + '"';
        }
        return s;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Warning warning = (Warning) o;
        return code == warning.code
                && Objects.equals(agent, warning.agent)
                && Objects.equals(text, warning.text)
                && Objects.equals(date, warning.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, agent, text, date);
    }

    /**
     * Parses a warning-header value (part after {@code Warning:}).
     *
     * @param value Warning-header value
     * @return {@code Warning} instance of {@code null} if could not be parsed
     */
    public static Warning valueOf(final String value) {
        final Matcher m = PATTERN.matcher(value);
        if (m.matches()) {
            final int code = Integer.parseInt(m.group(1));
            final String agent = m.group(2);
            final String tail = m.group(3);
            final String text;
            final String date;
            final int dateIndex = tail.lastIndexOf('"', tail.length() - 2);
            if (dateIndex != 0 && tail.charAt(dateIndex - 1) != '\\') {
                // optional date included
                text = tail.substring(0, dateIndex - 1);
                date = tail.substring(dateIndex);
            } else {
                text = tail;
                date = null;
            }
            return new Warning(code, agent, HeaderUtil.unquote(text), HeaderUtil.parseDate(HeaderUtil.unquote(date)));
        }
        return null;
    }
}
