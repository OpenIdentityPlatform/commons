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

import org.testng.annotations.Test;

public class WarningTest {

    /**
     * Warning-header whose value contains spaces, quoted-double-quote, and quoted-backslash
     */
    private static final String HEADER = "100 localhost:80 \"warning \\\" \\\\ value\"";

    /**
     * Warning-header with optional date
     */
    private static final String HEADER_WITH_DATE = HEADER + " \"Sun, 06 Nov 1994 08:49:37 GMT\"";

    @Test
    public void testHeaderToString() {
        final Warning result = buildWarning();
        assertThat(result.toString()).isEqualTo(HEADER);
    }

    @Test
    public void testHeaderWithDateToString() {
        final Warning result = buildWarningWithDate();
        assertThat(result.toString()).isEqualTo(HEADER_WITH_DATE);
    }

    @Test
    public void testHeaderGetters() {
        final Warning result = buildWarning();
        assertThat(result.getCode()).isEqualTo(100);
        assertThat(result.getAgent()).isEqualTo("localhost:80");
        assertThat(result.getText()).isEqualTo("warning \" \\ value");
        assertThat(result.getDate()).isNull();
    }

    @Test
    public void testHeaderWithDateGetters() {
        final Warning result = buildWarningWithDate();
        assertThat(result.getCode()).isEqualTo(100);
        assertThat(result.getAgent()).isEqualTo("localhost:80");
        assertThat(result.getText()).isEqualTo("warning \" \\ value");
        assertThat(result.getDate()).isEqualTo(HeaderUtil.parseDate("Sun, 06 Nov 1994 08:49:37 GMT"));
    }

    @Test
    public void testHeaderEqualsAndHashCode() {
        final Warning a = buildWarning();
        final Warning b = buildWarning();
        assertThat(a).isEqualTo(a);
        assertThat(a).isNotEqualTo(null);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    public void testHeaderWithDateEqualsAndHashCode() {
        final Warning a = buildWarningWithDate();
        final Warning b = buildWarningWithDate();
        assertThat(a).isEqualTo(a);
        assertThat(a).isNotEqualTo(null);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    public void testParse() {
        assertThat(Warning.valueOf(HEADER)).isEqualTo(buildWarning());

        assertThat(Warning.valueOf(HEADER_WITH_DATE)).isEqualTo(buildWarningWithDate());

        assertThat(Warning.valueOf("bad format")).isNull();
    }

    /**
     * @return {@link Warning} in the expected state
     */
    private static Warning buildWarning() {
        return new Warning(100, "localhost:80", "warning \" \\ value");
    }

    /**
     * @return {@link Warning}, with optional date, in the expected state
     */
    private static Warning buildWarningWithDate() {
        return new Warning(100, "localhost:80", "warning \" \\ value",
                HeaderUtil.parseDate("Sun, 06 Nov 1994 08:49:37 GMT"));
    }
}
