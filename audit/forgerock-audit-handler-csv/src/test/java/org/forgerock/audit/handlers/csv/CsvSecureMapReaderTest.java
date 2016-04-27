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
package org.forgerock.audit.handlers.csv;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.Map;

import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;
import org.testng.annotations.Test;

public class CsvSecureMapReaderTest {

    @Test
    public void shouldIgnoreExtraColumn() throws Exception {
        String line = "foo,HMAC,SIGNATURE\nbar,uLkBIiPY0+yyseXNACJX5SBwqV4RDSN8Ab8Jz7c3cYI=,";
        Reader reader = new StringReader(line);
        ICsvMapReader csvMapReader = new CsvMapReader(reader, CsvPreference.EXCEL_PREFERENCE);
        Map<String, String> values;
        try (CsvSecureMapReader csvHMACReader = new CsvSecureMapReader(csvMapReader)) {
            String[] header = csvHMACReader.getHeader(true);
            values = csvHMACReader.read(header);
        }

        assertThat(values).isEqualTo(Collections.singletonMap("foo", "bar"));
    }

    @Test
    public void shouldIgnoreHMACHeader() throws Exception {
        String line = "foo,HMAC,SIGNATURE";
        Reader reader = new StringReader(line);
        ICsvMapReader csvMapReader = new CsvMapReader(reader, CsvPreference.STANDARD_PREFERENCE);
        String[] header;
        try (CsvSecureMapReader csvHMACReader = new CsvSecureMapReader(csvMapReader)) {
            header = csvHMACReader.getHeader(false);
        }

        assertThat(header).isEqualTo(new String[] { "foo" });
    }
}
