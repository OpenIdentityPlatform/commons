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

import static org.assertj.core.api.Assertions.*;
import static org.forgerock.http.header.HeaderFactory.FACTORIES;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.assertj.core.util.Lists;
import org.forgerock.http.protocol.Request;
import org.testng.annotations.Test;

public class WarningHeaderTest {

    /**
     * Warning-header whose value contains spaces, quoted-double-quote, and quoted-backslash
     */
    private static final String HEADER = "100 localhost:80 \"warning \\\" \\\\ value\"";

    /**
     * Warning-header with optional date
     */
    private static final String HEADER_WITH_DATE = HEADER + " \"Sun, 06 Nov 1994 08:49:37 GMT\"";

    private final HeaderFactory<?> factory = FACTORIES.get(WarningHeader.NAME);

    @Test
    public void testHeaderFactoryRegistration() {
        assertThat(factory).isNotNull().isInstanceOf(WarningHeader.Factory.class);
    }

    @Test
    public void testParseFormattedHeader() throws MalformedHeaderException {
        final WarningHeader expected = WarningHeader.newWarning("localhost:80", "%s \" \\ value", "warning");
        final WarningHeader actual = (WarningHeader) factory.parse(HEADER);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testParseHeaderList() throws MalformedHeaderException {
        final List<String> headerList = Arrays.asList(HEADER, HEADER);
        final WarningHeader expected = buildMultiWarningHeader();
        final WarningHeader actual = (WarningHeader) factory.parse(headerList);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testParseHeaderWithDate() throws MalformedHeaderException {
        final WarningHeader expected = buildWarningHeaderWithDate();
        final WarningHeader actual = (WarningHeader) factory.parse(HEADER_WITH_DATE);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testParseHeaderListFromRequest() {
        final Request request = new Request();
        request.getHeaders().add(buildWarningHeader());
        request.getHeaders().add(buildWarningHeader());

        final WarningHeader expected = buildMultiWarningHeader();
        final WarningHeader actual = WarningHeader.valueOf(request);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testGetWarnings() {
        final Date date = HeaderUtil.parseDate("Sun, 06 Nov 1994 08:49:37 GMT");
        final List<Warning> expected = new ArrayList<>();
        expected.add(new Warning(100, "localhost:80", "warning \" \\ value", date));
        final List<Warning> actual = buildWarningHeaderWithDate().getWarnings();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testHashCode() {
        final int expected = 497950499;
        final int actual = buildWarningHeader().hashCode();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testEquals() {
        final WarningHeader warningHeader = buildWarningHeader();
        // object equality
        assertThat(warningHeader).isEqualTo(buildWarningHeader());
        // instance equality
        assertThat(warningHeader).isEqualTo(warningHeader);
        // not null
        assertThat(warningHeader).isNotNull();
    }

    /**
     * @return {@link WarningHeader} in the expected state
     */
    private static WarningHeader buildWarningHeader() {
        return new WarningHeader(new Warning(100, "localhost:80", "warning \" \\ value"));
    }

    /**
     * @return {@link WarningHeader}, with multiple entries, in the expected state
     */
    private static WarningHeader buildMultiWarningHeader() {
        final WarningHeader expected = new WarningHeader(Lists.<Warning>emptyList());
        return expected.add(100, "localhost:80", "warning \" \\ value")
                .add(100, "localhost:80", "warning \" \\ value");
    }

    /**
     * @return {@link WarningHeader}, with optional date, in the expected state
     */
    private static WarningHeader buildWarningHeaderWithDate() {
        final Date date = HeaderUtil.parseDate("Sun, 06 Nov 1994 08:49:37 GMT");
        return new WarningHeader(new Warning(100, "localhost:80", "warning \" \\ value", date));
    }
}
