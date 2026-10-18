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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.audit.handlers.csv;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.forgerock.audit.handlers.csv.SecureCsvWriterTest.KEYSTORE_FILENAME;
import static org.forgerock.audit.handlers.csv.SecureCsvWriterTest.KEYSTORE_PASSWORD;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.forgerock.audit.handlers.csv.CsvAuditEventHandlerConfiguration.CsvSecurity;
import org.forgerock.audit.handlers.csv.CsvAuditEventHandlerConfiguration.EventBufferingConfiguration;
import org.forgerock.audit.retention.TimestampFilenameFilter;
import org.forgerock.audit.secure.JcaKeyStoreHandler;
import org.forgerock.audit.secure.KeyStoreHandler;
import org.forgerock.audit.secure.KeyStoreHandlerDecorator;
import org.forgerock.audit.secure.KeyStoreSecureStorage;
import org.forgerock.audit.secure.SecureStorage;
import org.forgerock.util.encode.Base64;
import org.joda.time.format.DateTimeFormat;
import org.supercsv.prefs.CsvPreference;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class CsvWriterTest {

    public static final String TIME_STAMP_FORMAT = "-MM.dd.yy-HH.mm.ss.SSS";
    public static final String PREFIX = "Prefix-";

    private KeyStoreHandlerDecorator keyStoreHandler;
    private Random random;

    @BeforeMethod
    public void setup() throws Exception {
        keyStoreHandler = new KeyStoreHandlerDecorator(
                new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE, KEYSTORE_FILENAME, KEYSTORE_PASSWORD));
        random = new Random(42);
    }

    @Test
    public void shouldCreateBufferedSecureCsvFile() throws Exception {
        final CsvPreference csvPreference = CsvPreference.EXCEL_PREFERENCE;
        final String[] header = new String[] { "child1", "child2", "child3" };

        final Path logDirectory = Files.createTempDirectory("CsvWriterTest");
        logDirectory.toFile().deleteOnExit();
        File csvFile = new File(logDirectory.toFile(), "shouldCreateBufferedSecureCsvFile.csv");

        EventBufferingConfiguration bufferConfig = new EventBufferingConfiguration();
        bufferConfig.setEnabled(true);
        CsvSecurity csvSecurity = new CsvSecurity();
        csvSecurity.setEnabled(true);
        csvSecurity.setSignatureInterval("3 seconds");
        CsvAuditEventHandlerConfiguration configuration = new CsvAuditEventHandlerConfiguration();
        configuration.setBufferingConfiguration(bufferConfig);
        configuration.setSecurity(csvSecurity);
        try (SecureCsvWriter writer = new SecureCsvWriter(csvFile, header, csvPreference, configuration,
                keyStoreHandler, random)) {
            Map<String, String> values = getValues(header, "one-a", "one-b", "one-c");
            writer.writeEvent(values);
        }

        String password = Base64.encode(
                keyStoreHandler.readSecretKeyFromKeyStore(CsvSecureConstants.ENTRY_PASSWORD).getEncoded());
        KeyStoreHandler csvKeyStoreHandler = new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE,
                csvFile.getPath() + ".keystore", password);
        SecureStorage secureStorage = new KeyStoreSecureStorage(csvKeyStoreHandler,
                keyStoreHandler.readPublicKeyFromKeyStore(KeyStoreSecureStorage.ENTRY_SIGNATURE));
        CsvSecureVerifier verifier = new CsvSecureVerifier(csvFile, CsvPreference.EXCEL_PREFERENCE, secureStorage);
        assertThat(verifier.verify().hasPassedVerification()).isTrue();
    }

    @Test
    public void shouldResumeExistingCsvSecureFile() throws Exception {
        // This is more an integration test rather than a unit test.
        // As we're running a little bit out of time, this is the best that can be done.

        final CsvPreference csvPreference = CsvPreference.EXCEL_PREFERENCE;
        String[] header;

        final Path logDirectory = Files.createTempDirectory("CsvWriterTest");
        logDirectory.toFile().deleteOnExit();
        File csvFile = new File(logDirectory.toFile(), "shouldResumeExistingCsvSecureFile.csv");

        header = new String[] { "child1", "child2", "child3" };
        EventBufferingConfiguration bufferConfig = new EventBufferingConfiguration();
        bufferConfig.setEnabled(false);
        CsvSecurity csvSecurity = new CsvSecurity();
        csvSecurity.setEnabled(true);
        csvSecurity.setSignatureInterval("3 seconds");
        CsvAuditEventHandlerConfiguration configuration = new CsvAuditEventHandlerConfiguration();
        configuration.setBufferingConfiguration(bufferConfig);
        configuration.setSecurity(csvSecurity);
        try (SecureCsvWriter writer = new SecureCsvWriter(csvFile, header, csvPreference, configuration,
                keyStoreHandler, random)) {
            Map<String, String> values = getValues(header, "one-a", "one-b", "one-c");
            writer.writeEvent(values);
        }

        try (SecureCsvWriter writer = new SecureCsvWriter(csvFile, header, csvPreference, configuration,
                keyStoreHandler, random)) {
            Map<String, String> values = new HashMap<>(3);
            values.put(header[0], "riri");
            values.put(header[1], "fifi");
            values.put(header[2], "loulou");

            writer.writeEvent(values);
        }

        String password = Base64.encode(
                keyStoreHandler.readSecretKeyFromKeyStore(CsvSecureConstants.ENTRY_PASSWORD).getEncoded());
        KeyStoreHandler csvKeyStoreHandler = new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE,
                csvFile.getPath() + ".keystore", password);
        SecureStorage secureStorage = new KeyStoreSecureStorage(csvKeyStoreHandler,
                keyStoreHandler.readPublicKeyFromKeyStore(KeyStoreSecureStorage.ENTRY_SIGNATURE));
        CsvSecureVerifier verifier = new CsvSecureVerifier(csvFile, CsvPreference.EXCEL_PREFERENCE, secureStorage);
        assertThat(verifier.verify().hasPassedVerification()).isTrue().as("File " + csvFile.getPath());

        // Expecting to fail
        try {
            new SecureCsvWriter(csvFile, new String[] { "child1", "child2", "child3", "enfant4" }, csvPreference,
                    configuration, keyStoreHandler, random);
            fail("Should have failed because headers do not match.");
        } catch (RuntimeException e) {
            // This is ok, we expect to have this exception.
        }
    }

    @DataProvider
    private Object[][] headersThatDoNotMatch() {
        return new Object[][] {
            // a field was removed from the end of the topic schema
            { "child1,child2,child3,child4" },
            // a field was added to the end of the topic schema
            { "child1,child2" },
            // Super CSV reads an empty column name as null
            { "child1,,child3" },
            { "child1,child2," },
        };
    }

    @Test(dataProvider = "headersThatDoNotMatch")
    public void shouldNotResumeCsvFileWithHeadersThatDoNotMatch(String fileHeaders) throws Exception {
        final Path csvFile = Files.createTempFile("CsvWriterTest", ".csv");
        csvFile.toFile().deleteOnExit();
        Files.write(csvFile, List.of(fileHeaders));

        try {
            new StandardCsvWriter(csvFile.toFile(), new String[] { "child1", "child2", "child3" },
                    CsvPreference.EXCEL_PREFERENCE, new CsvAuditEventHandlerConfiguration());
            fail("Should have failed because headers do not match.");
        } catch (IOException e) {
            assertThat(e).hasMessage("Resuming an existing CSV file but the headers do not match.");
        }
        assertThat(Files.readAllLines(csvFile, UTF_8)).containsExactly(fileHeaders);
    }

    @Test
    public void shouldRejectNullHeadersWhenResumingCsvFile() throws Exception {
        final Path csvFile = Files.createTempFile("CsvWriterTest", ".csv");
        csvFile.toFile().deleteOnExit();
        Files.write(csvFile, List.of("child1,child2,child3"));

        try {
            new StandardCsvWriter(csvFile.toFile(), null, CsvPreference.EXCEL_PREFERENCE,
                    new CsvAuditEventHandlerConfiguration());
            fail("Should have failed because headers are null.");
        } catch (NullPointerException e) {
            assertThat(e).hasMessage("The headers can't be null.");
        }
    }

    @Test
    public void shouldNotResumeCsvSecureFileWithEmptyColumnName() throws Exception {
        final CsvPreference csvPreference = CsvPreference.EXCEL_PREFERENCE;
        final String[] header = new String[] { "child1", "child2", "child3" };

        final Path logDirectory = Files.createTempDirectory("CsvWriterTest");
        logDirectory.toFile().deleteOnExit();
        File csvFile = new File(logDirectory.toFile(), "shouldNotResumeCsvSecureFileWithEmptyColumnName.csv");

        EventBufferingConfiguration bufferConfig = new EventBufferingConfiguration();
        bufferConfig.setEnabled(false);
        CsvSecurity csvSecurity = new CsvSecurity();
        csvSecurity.setEnabled(true);
        csvSecurity.setSignatureInterval("3 seconds");
        CsvAuditEventHandlerConfiguration configuration = new CsvAuditEventHandlerConfiguration();
        configuration.setBufferingConfiguration(bufferConfig);
        configuration.setSecurity(csvSecurity);
        try (SecureCsvWriter writer = new SecureCsvWriter(csvFile, header, csvPreference, configuration,
                keyStoreHandler, random)) {
            Map<String, String> values = new HashMap<>(2);
            values.put(header[0], "one-a");
            values.put(header[2], "one-c");
            writer.writeEvent(values);
        }

        // child2 is empty in every row, so a row HMAC of the values alone does not change without its name
        final List<String> lines = Files.readAllLines(csvFile.toPath(), UTF_8);
        lines.set(0, lines.get(0).replace("child2", ""));
        Files.write(csvFile.toPath(), lines, UTF_8);

        try {
            new SecureCsvWriter(csvFile, header, csvPreference, configuration, keyStoreHandler, random);
            fail("Should have failed because headers do not match.");
        } catch (IOException | RuntimeException e) {
            // The constructor throws the IOException as it is, or wraps it in a RuntimeException. The file fails the
            // header check, or the verification when the row HMAC covers the column names.
            final Throwable failure = e instanceof IOException ? e : e.getCause();
            assertThat(failure).isInstanceOf(IOException.class).hasMessageMatching(
                    "Resuming an existing CSV file but the headers do not match\\.|The CSV file was tampered: .*");
        }
        assertThat(Files.readAllLines(csvFile.toPath(), UTF_8)).isEqualTo(lines);
    }

    @Test
    public void shouldAddHeadersToEmptyCsvFile() throws Exception {
        final File csvFile = org.assertj.core.util.Files.newTemporaryFile();
        final String[] headers = new String[]{"child1", "child2", "child3"};
        final CsvPreference csvPreference = CsvPreference.EXCEL_PREFERENCE;
        final EventBufferingConfiguration bufferConfig = new EventBufferingConfiguration();
        bufferConfig.setEnabled(false);
        CsvSecurity csvSecurity = new CsvSecurity();
        csvSecurity.setEnabled(false);
        CsvAuditEventHandlerConfiguration configuration = new CsvAuditEventHandlerConfiguration();
        configuration.setBufferingConfiguration(bufferConfig);
        configuration.setSecurity(csvSecurity);

        final StandardCsvWriter csvWriter = new StandardCsvWriter(csvFile, headers, csvPreference, configuration);
        csvWriter.close();

        final List<String> contents = Files.readAllLines(csvFile.toPath(), Charset.defaultCharset());
        assertThat(contents.size()).isEqualTo(1);
        assertThat(contents.get(0)).isEqualTo("child1,child2,child3");
    }

    @Test
    public void shouldCreateNewAuditFileAfterRotation() throws Exception {

        final CsvPreference csvPreference = CsvPreference.EXCEL_PREFERENCE;
        String[] header;

        final Path logDirectory = Files.createTempDirectory("CsvWriterTest");
        logDirectory.toFile().deleteOnExit();
        File csvFile = new File(logDirectory.toFile(), "shouldCreateNewAuditFileAfterRotation.csv");

        header = new String[] { "child1", "child2", "child3" };
        EventBufferingConfiguration bufferConfig = new EventBufferingConfiguration();
        bufferConfig.setEnabled(false);
        CsvSecurity csvSecurity = new CsvSecurity();
        csvSecurity.setEnabled(true);
        csvSecurity.setSignatureInterval("3 seconds");
        CsvAuditEventHandlerConfiguration configuration =
                createCsvAuditEventHandlerConfigurationWithRotation(bufferConfig, 1000L);
        configuration.setSecurity(csvSecurity);
        try (SecureCsvWriter writer = new SecureCsvWriter(csvFile, header, csvPreference, configuration,
                keyStoreHandler, random)) {
            writeNRows(header, writer, 12);
        }

        // TODO - Commenting this test part out until the verifier can verify a rotated log file.
        // verify the new audit file that was created after the rotation
        //try (CsvMapReader reader = new CsvMapReader(new BufferedReader(new FileReader(csvFile)), csvPreference)) {
        //    CsvSecureVerifier verifier = new CsvSecureVerifier(reader, SecureCsvWriterTest.TRUSTSTORE_FILENAME,
        //            SecureCsvWriterTest.TRUSTSTORE_PASSWORD);
        //    assertThat(verifier.verify()).isTrue();
        //}

        // Verify the archived audit file.
        final TimestampFilenameFilter timestampFilenameFilter =
                new TimestampFilenameFilter(csvFile, PREFIX, DateTimeFormat.forPattern(TIME_STAMP_FORMAT));
        final File[] files = csvFile.getParentFile().listFiles(timestampFilenameFilter);
        assertThat(files).isNotEmpty();
        String password = Base64.encode(
                keyStoreHandler.readSecretKeyFromKeyStore(CsvSecureConstants.ENTRY_PASSWORD).getEncoded());
        KeyStoreHandler csvKeyStoreHandler = new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE,
                files[0].getPath() + ".keystore", password);
        SecureStorage secureStorage = new KeyStoreSecureStorage(csvKeyStoreHandler,
                keyStoreHandler.readPublicKeyFromKeyStore(KeyStoreSecureStorage.ENTRY_SIGNATURE));
        CsvSecureVerifier verifier = new CsvSecureVerifier(files[0], CsvPreference.EXCEL_PREFERENCE, secureStorage);
        assertThat(verifier.verify().hasPassedVerification()).as("File " + csvFile.getPath()).isTrue();
    }

    private Map<String, String> getValues(String[] header, String val1, String val2, String val3) {
        Map<String, String> values = new HashMap<>(3);
        values.put(header[0], val1);
        values.put(header[1], val2);
        values.put(header[2], val3);
        return values;
    }

    private CsvAuditEventHandlerConfiguration createCsvAuditEventHandlerConfigurationWithRotation(
            final EventBufferingConfiguration eventBufferingConfiguration, long maxFileSize) {
        CsvAuditEventHandlerConfiguration configuration = new CsvAuditEventHandlerConfiguration();
        configuration.getFileRotation().setRotationEnabled(true);
        configuration.getFileRotation().setRotationFileSuffix(TIME_STAMP_FORMAT);
        configuration.getFileRotation().setRotationFilePrefix(PREFIX);
        configuration.getFileRotation().setRotationInterval("disabled");
        configuration.getFileRotation().setMaxFileSize(maxFileSize);
        configuration.getFileRetention().setMaxNumberOfHistoryFiles(3);
        configuration.setBufferingConfiguration(eventBufferingConfiguration);
        return configuration;
    }

    private void writeNRows(String[] header, CsvWriter writer, int nbRows) throws IOException {
        for (int i = 1; i <= nbRows; i++) {
            String number = "________________" + String.valueOf(i);
            writer.writeEvent(getValues(header, number + "-A", number + "-B", number + "-C"));
        }
    }
}
