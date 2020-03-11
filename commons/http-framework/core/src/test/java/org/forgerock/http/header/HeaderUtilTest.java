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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.http.header;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * This class contains some unit tests for the header utility class.
 */
@SuppressWarnings("javadoc")
public class HeaderUtilTest {

    private static final String ACCEPT_CHARSET_SAMPLE = "Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7";
    private static final String ESCAPING_TYPE_SAMPLE = "Etag: \"\\pub1259380237;gz\\\"";
    private static final String ESCAPING_TYPE_SAMPLE_QUOTED = "\"Etag: \\\"\\\\pub1259380237;gz\\\\\\\"\"";

    @DataProvider
    private Object[][] invalidSeparatorProvider() {
        return new Object[][] {
            { '"' },
            { '\\' } };
    }

    @DataProvider
    private Object[][] validSeparatorProvider() {
        return new Object[][] {
            { ',' },
            { ';' } };
    }

    @Test(dataProvider = "invalidSeparatorProvider", expectedExceptions = IllegalArgumentException.class)
    public void testSplitDoesntAllowSpecificSeparators(final char separator) {
        HeaderUtil.split("Invalid separators throw an exception", separator);
    }

    @Test(dataProvider = "validSeparatorProvider")
    public void testSplitSucceed(final char separator) {
        final List<String> result = HeaderUtil.split(ACCEPT_CHARSET_SAMPLE, separator);
        assertThat(result).hasSize(ACCEPT_CHARSET_SAMPLE.split(String.valueOf(separator)).length);
    }

    @Test(dataProvider = "validSeparatorProvider")
    public void testSplitSucceedEscaping(final char separator) {
        final List<String> result = HeaderUtil.split(ESCAPING_TYPE_SAMPLE, separator);
        assertThat(result).hasSize(1);
    }

    @Test
    public void testJoinOnNullChainReturnsNull() {
        final String result = HeaderUtil.join(null, ';');
        assertThat(result).isNull();
    }

    @Test(dataProvider = "invalidSeparatorProvider", expectedExceptions = IllegalArgumentException.class)
    public void testJoinDoesntAllowSpecificSeparators(final char separator) {
        final ArrayList<String> list = new ArrayList<>();
        list.add(ACCEPT_CHARSET_SAMPLE);
        list.add(ESCAPING_TYPE_SAMPLE);
        HeaderUtil.join(list, separator);
    }

    @Test(dataProvider = "validSeparatorProvider")
    public void testJoinSucceed(final char separator) {
        final ArrayList<String> list = new ArrayList<>();
        list.add(ACCEPT_CHARSET_SAMPLE);
        list.add(ESCAPING_TYPE_SAMPLE);
        final String result = HeaderUtil.join(list, separator);
        // 2 => separator char + space
        assertThat(result).contains(ACCEPT_CHARSET_SAMPLE).contains(ESCAPING_TYPE_SAMPLE)
                .hasSize(ACCEPT_CHARSET_SAMPLE.length() + ESCAPING_TYPE_SAMPLE.length() + 2);
    }

    @Test
    public void testQuoteOnNullChainReturnsNull() {
        final String result = HeaderUtil.quote(null);
        assertThat(result).isNull();
    }

    @Test
    public void testQuoteString() {
        final String result = HeaderUtil.quote(ACCEPT_CHARSET_SAMPLE);
        assertThat(result).isEqualTo("\"" + ACCEPT_CHARSET_SAMPLE + "\"");
    }

    @Test
    public void testQuoteAlreadyQuotedChainSucceed() {
        final String result = HeaderUtil.quote(ESCAPING_TYPE_SAMPLE);
        assertThat(result).isEqualTo(ESCAPING_TYPE_SAMPLE_QUOTED);
    }

    @Test
    public void testUnquote() {
        final String result = HeaderUtil.unquote(ESCAPING_TYPE_SAMPLE_QUOTED);
        assertThat(result).isEqualTo(ESCAPING_TYPE_SAMPLE);
    }

    @Test
    public void testUnquoteOnNull() {
        final String result = HeaderUtil.unquote(null);
        assertThat(result).isNull();
    }

    @Test
    public void testUnquoteOnEmptyQuoted() {
        final String result = HeaderUtil.unquote("\"\"");
        assertThat(result).isEmpty();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testUnquoteOnUnquotedException() {
        HeaderUtil.unquote("not quoted");
    }

    @Test
    public void testFormatDate() {
        final String result = HeaderUtil.formatDate(new Date(0));
        assertThat(result).isEqualTo("Thu, 01 Jan 1970 00:00:00 GMT");
    }

    @Test
    public void testParseRfc1123Date() {
        final Date result = HeaderUtil.parseDate("Sun, 06 Nov 1994 08:49:37 GMT");
        final String formattedDate = HeaderUtil.formatDate(result);
        assertThat(formattedDate).isEqualTo("Sun, 06 Nov 1994 08:49:37 GMT");
    }

    @Test
    public void testParseRfc850Date() {
        final Date result = HeaderUtil.parseDate("Sunday, 06-Nov-1994 08:49:37 GMT");
        final String formattedDate = HeaderUtil.formatDate(result);
        assertThat(formattedDate).isEqualTo("Sun, 06 Nov 1994 08:49:37 GMT");
    }

    @Test
    public void testParseAnsiCDate() {
        final Date result = HeaderUtil.parseDate("Sun Nov  6 08:49:37 1994");
        final String formattedDate = HeaderUtil.formatDate(result);
        assertThat(formattedDate).isEqualTo("Sun, 06 Nov 1994 08:49:37 GMT");
    }

    @Test
    public void testParseDateUnsupportedArguments() {
        assertThat(HeaderUtil.parseDate(null)).isNull();

        assertThat(HeaderUtil.parseDate("")).isNull();

        final Date iso8601FormattedDate = HeaderUtil.parseDate("1994-11-06T08:49:37Z");
        assertThat(iso8601FormattedDate).isNull();
    }
}
