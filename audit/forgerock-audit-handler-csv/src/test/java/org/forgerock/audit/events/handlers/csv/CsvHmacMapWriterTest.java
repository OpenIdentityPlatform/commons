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
package org.forgerock.audit.events.handlers.csv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;

import org.mockito.Mockito;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.io.ICsvMapWriter;
import org.supercsv.prefs.CsvPreference;
import org.testng.annotations.Test;


public class CsvHmacMapWriterTest {

    @Test
    public void shouldGenerateHMACColumn() throws Exception {
        Writer writer = new StringWriter();
        ICsvMapWriter csvMapWriter = new CsvMapWriter(writer , CsvPreference.STANDARD_PREFERENCE);
        HmacCalculator hmacCalculator = mock(HmacCalculator.class);
        when(hmacCalculator.calculate(Mockito.any(byte[].class))).thenReturn("myHMAC");
        try (CsvHmacMapWriter csvHMACWriter = new CsvHmacMapWriter(csvMapWriter, hmacCalculator)) {
            Map<String, String> values = Collections.singletonMap("foo", "bar");
            String nameMapping = "foo";
            csvHMACWriter.write(values, nameMapping);
        }

        assertThat(writer.toString()).isEqualTo("bar,myHMAC\r\n");
    }

    @Test
    public void shouldGenerateHeaderWithExtraColumns() throws Exception {
        Writer writer = new StringWriter();
        ICsvMapWriter csvMapWriter = new CsvMapWriter(writer , CsvPreference.STANDARD_PREFERENCE);
        HmacCalculator hmacCalculator = mock(HmacCalculator.class);
        when(hmacCalculator.calculate(Mockito.any(byte[].class))).thenReturn("myHMAC");
        try (CsvHmacMapWriter csvHMACWriter = new CsvHmacMapWriter(csvMapWriter, hmacCalculator)) {
            String header = "foo";
            csvHMACWriter.writeHeader(header);
        }

        assertThat(writer.toString()).isEqualTo("foo,HMAC\r\n");
    }
}
