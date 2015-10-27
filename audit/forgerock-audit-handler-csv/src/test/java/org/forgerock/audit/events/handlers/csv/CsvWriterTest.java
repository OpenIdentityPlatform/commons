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
import static org.assertj.core.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.audit.events.handlers.EventHandlerConfiguration.EventBufferingConfiguration;
import org.forgerock.audit.events.handlers.csv.CSVAuditEventHandlerConfiguration.CsvSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.io.CsvMapReader;
import org.supercsv.prefs.CsvPreference;
import org.testng.annotations.Test;

public class CsvWriterTest {

    private static final Logger logger = LoggerFactory.getLogger(CsvWriterTest.class);

    @Test
    public void shouldCreateBufferedSecureCsvFile() throws Exception {
        final CsvPreference csvPreference = CsvPreference.EXCEL_PREFERENCE;
        String[] header;

        File csvFile = new File("target/test-classes/CsvWriterTest.csv");
        csvFile.delete();

        header = new String[] { "child1", "child2", "child3" };
        EventBufferingConfiguration bufferConfig = new EventBufferingConfiguration();
        bufferConfig.setEnabled(true);
        bufferConfig.setMaxSize(3);
        CsvSecurity csvSecurity = new CsvSecurity();
        csvSecurity.setFilename(CsvSecureMapWriterTest.KEYSTORE_FILENAME);
        csvSecurity.setPassword(CsvSecureMapWriterTest.KEYSTORE_PASSWORD);
        csvSecurity.setEnabled(true);
        csvSecurity.setSignatureInterval("3 seconds");
        try (CsvWriter writer = new CsvWriter(csvFile, header, csvPreference, bufferConfig, csvSecurity)) {
            Map<String, String> values = new HashMap<>(3);
            values.put(header[0], "pim");
            values.put(header[1], "pam");
            values.put(header[2], "poum");

            writer.writeRow(values);
        }

        try (CsvMapReader reader = new CsvMapReader(new BufferedReader(new FileReader(csvFile)), csvPreference)) {
            CsvSecureVerifier verifier = new CsvSecureVerifier(reader, CsvSecureMapWriterTest.KEYSTORE_FILENAME,
                    CsvSecureMapWriterTest.KEYSTORE_PASSWORD);
            assertThat(verifier.verify()).isTrue();
        }
    }

    @Test
    public void shouldResumeExistingCsvSecureFile() throws Exception {
        // This is more an integration test rather than a unit test.
        // As we're running a little bit out of time, this is the best that can be done.

        final CsvPreference csvPreference = CsvPreference.EXCEL_PREFERENCE;
        String[] header;

        File csvFile = new File("target/test-classes/CsvWriterTest.csv");
        csvFile.delete();

        header = new String[] { "child1", "child2", "child3" };
        EventBufferingConfiguration bufferConfig = new EventBufferingConfiguration();
        bufferConfig.setEnabled(false);
        CsvSecurity csvSecurity = new CsvSecurity();
        csvSecurity.setFilename(CsvSecureMapWriterTest.KEYSTORE_FILENAME);
        csvSecurity.setPassword(CsvSecureMapWriterTest.KEYSTORE_PASSWORD);
        csvSecurity.setEnabled(true);
        csvSecurity.setSignatureInterval("3 seconds");
        try (CsvWriter writer = new CsvWriter(csvFile, header, csvPreference, bufferConfig, csvSecurity)) {
            Map<String, String> values = new HashMap<>(3);
            values.put(header[0], "pim");
            values.put(header[1], "pam");
            values.put(header[2], "poum");

            writer.writeRow(values);
        }

        try (CsvWriter writer = new CsvWriter(csvFile, header, csvPreference, bufferConfig, csvSecurity)) {
            Map<String, String> values = new HashMap<>(3);
            values.put(header[0], "riri");
            values.put(header[1], "fifi");
            values.put(header[2], "loulou");

            writer.writeRow(values);
        }

        try (CsvMapReader reader = new CsvMapReader(new BufferedReader(new FileReader(csvFile)), csvPreference)) {
            CsvSecureVerifier verifier = new CsvSecureVerifier(reader, CsvSecureMapWriterTest.KEYSTORE_FILENAME,
                    CsvSecureMapWriterTest.KEYSTORE_PASSWORD);
            assertThat(verifier.verify()).isTrue();
        }

        // Expecting to fail
        header = new String[] { "child1", "child2", "child3", "enfant4" };
        try (CsvWriter writer = new CsvWriter(csvFile, header, csvPreference, bufferConfig, csvSecurity)) {
            Map<String, String> values = new HashMap<>(3);
            values.put(header[0], "Joe");
            values.put(header[1], "William");
            values.put(header[2], "Jack");
            values.put(header[3], "Averell");

            writer.writeRow(values);
            fail("We should not be able to write Dalton's brothers.");
        } catch (IOException e) {
            // This is ok, we expect to have this exception.
        }
    }

    @Test
    public void shouldAddHeadersToEmptyCsvFile() throws Exception {
        final File csvFile = org.assertj.core.util.Files.newTemporaryFile();
        final String[] headers = new String[] { "child1", "child2", "child3" };
        final CsvPreference csvPreference = CsvPreference.EXCEL_PREFERENCE;
        final EventBufferingConfiguration bufferConfig = new EventBufferingConfiguration();
        bufferConfig.setEnabled(false);
        CsvSecurity csvSecurity = new CsvSecurity();
        csvSecurity.setEnabled(false);

        final CsvWriter csvWriter = new CsvWriter(csvFile, headers, csvPreference, bufferConfig, csvSecurity);
        csvWriter.close();

        final List<String> contents = Files.readAllLines(csvFile.toPath(), Charset.defaultCharset());
        assertThat(contents.size()).isEqualTo(1);
        assertThat(contents.get(0)).isEqualTo("child1,child2,child3");
    }
}
