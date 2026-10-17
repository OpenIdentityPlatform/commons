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
import static org.forgerock.audit.handlers.csv.SecureCsvWriterTest.KEYSTORE_FILENAME;
import static org.forgerock.audit.handlers.csv.SecureCsvWriterTest.KEYSTORE_PASSWORD;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.forgerock.audit.handlers.csv.CsvSecureVerifier.VerificationResult;
import org.forgerock.audit.secure.JcaKeyStoreHandler;
import org.forgerock.audit.secure.KeyStoreHandlerDecorator;
import org.forgerock.audit.secure.KeyStoreSecureStorage;
import org.forgerock.util.encode.Base64;
import org.supercsv.prefs.CsvPreference;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class CsvSecureVerifierTest {

    static final String TRUSTSTORE_FILENAME = "src/test/resources/keystore-verifier.jks";
    static final String TRUSTSTORE_PASSWORD = "password";

    @Test
    public void shouldVerifyValidFile() throws Exception {
        File csvFile = new File("src/test/resources/shouldGeneratePeriodicallySignature-expected.txt");
        KeyStoreHandlerDecorator keyStoreHandler = new KeyStoreHandlerDecorator(
                new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE, TRUSTSTORE_FILENAME, TRUSTSTORE_PASSWORD));
        CsvSecureVerifier csvVerifier = new CsvSecureVerifier(csvFile, CsvPreference.EXCEL_PREFERENCE,
                new KeyStoreSecureStorage(keyStoreHandler,
                        keyStoreHandler.readPublicKeyFromKeyStore(KeyStoreSecureStorage.ENTRY_SIGNATURE)));
        assertThat(csvVerifier.verify().hasPassedVerification()).isTrue();
    }

    @Test
    public void shouldVerifyFileWithHmacOfConcatenatedValues() throws Exception {
        // written before the HMAC of a row covered the column names and where each value ends
        File csvFile = new File("src/test/resources/secureCsvLegacyHmac.csv");
        KeyStoreHandlerDecorator keyStoreHandler = new KeyStoreHandlerDecorator(
                new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE, TRUSTSTORE_FILENAME, TRUSTSTORE_PASSWORD));
        CsvSecureVerifier csvVerifier = new CsvSecureVerifier(csvFile, CsvPreference.EXCEL_PREFERENCE,
                new KeyStoreSecureStorage(keyStoreHandler,
                        keyStoreHandler.readPublicKeyFromKeyStore(KeyStoreSecureStorage.ENTRY_SIGNATURE)));
        VerificationResult verificationResult = csvVerifier.verify();
        assertThat(verificationResult.hasPassedVerification()).as(verificationResult.getFailureReason()).isTrue();
    }

    @DataProvider
    public Object[][] editsOfSignedData() {
        return new Object[][] {
            // Edits that keep the values concatenated, which the HMAC of a row covered before:
            // characters moved across a column boundary
            { "\nbob,x,", "\nbo,bx,", "The HMac at row 2 is not correct." },
            // a value moved into the empty neighbouring column
            { "\n,bob,", "\nbob,,", "The HMac at row 3 is not correct." },
            // the column names swapped in the header
            { "first,second,", "second,first,", "The HMac at row 2 is not correct." },
            // The row as one value that holds the data its HMAC covers, but the leading byte: the values concatenated
            // are these data, which the verifier would accept if the data did not start with a byte UTF-8 never holds.
            { "\nbob,x,", "\n\u0000\u0000\u0000\u0005first\u0000\u0000\u0000\u0003bob"
                    + "\u0000\u0000\u0000\u0006second\u0000\u0000\u0000\u0001x,,",
                    "The HMac at row 2 is not correct." },
            // Characters moved across a column boundary, next to a value that holds the next column name after its
            // length: the names alone would tell where a value ends, but for such a value.
            { "\na\u0000\u0000\u0000\u0006secondb,c,", "\na,b\u0000\u0000\u0000\u0006secondc,",
                    "The HMac at row 4 is not correct." },
            // a column name removed from the header, which the CSV reader reads as null
            { "first,second,", ",second,", "The HMac at row 2 is not correct." }
        };
    }

    @Test(dataProvider = "editsOfSignedData")
    public void shouldNotVerifyEditOfSignedData(String original, String edited, String failureReason)
            throws Exception {
        final File csvFile = writeSecureCsvFile(new String[] { "bob", "x" }, new String[] { null, "bob" },
                new String[] { "a\u0000\u0000\u0000\u0006secondb", "c" });
        VerificationResult verificationResult = verifySecureCsvFile(csvFile);
        assertThat(verificationResult.hasPassedVerification()).as(verificationResult.getFailureReason()).isTrue();

        final String content = new String(Files.readAllBytes(csvFile.toPath()), UTF_8);
        assertThat(content.indexOf(original)).as(original).isNotNegative().isEqualTo(content.lastIndexOf(original));
        Files.write(csvFile.toPath(), content.replace(original, edited).getBytes(UTF_8));

        verificationResult = verifySecureCsvFile(csvFile);
        assertThat(verificationResult.hasPassedVerification()).isFalse();
        assertThat(verificationResult.getFailureReason()).isEqualTo(failureReason);
    }

    @Test
    public void shouldNotVerifyRowWithoutHmac() throws Exception {
        final File csvFile = writeSecureCsvFile(new String[] { "bob", "x" });
        final String content = new String(Files.readAllBytes(csvFile.toPath()), UTF_8);
        final String edited = content.replaceFirst("\nbob,x,[^,\r\n]+,", "\nbob,x,,");
        assertThat(edited).isNotEqualTo(content);
        Files.write(csvFile.toPath(), edited.getBytes(UTF_8));

        final VerificationResult verificationResult = verifySecureCsvFile(csvFile);
        assertThat(verificationResult.hasPassedVerification()).isFalse();
        assertThat(verificationResult.getFailureReason()).isEqualTo("The HMac at row 2 is not correct.");
    }

    @Test(dataProvider = "invalidContent")
    public void shouldNotVerify(String filename) throws Exception {
        File csvFile = new File(filename);
        KeyStoreHandlerDecorator keyStoreHandler = new KeyStoreHandlerDecorator(
                new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE, TRUSTSTORE_FILENAME, TRUSTSTORE_PASSWORD));
        CsvSecureVerifier csvVerifier = new CsvSecureVerifier(csvFile, CsvPreference.EXCEL_PREFERENCE,
                new KeyStoreSecureStorage(keyStoreHandler,
                        keyStoreHandler.readPublicKeyFromKeyStore(KeyStoreSecureStorage.ENTRY_SIGNATURE)));
        assertThat(csvVerifier.verify().hasPassedVerification()).isFalse();
    }

    @Test
    public void shouldNotVerifyEmptyFile() throws Exception {
        // the rotate action archives an audit file that has no event, so its header was never written
        File csvFile = Files.createTempFile("CsvSecureVerifierTest", ".csv").toFile();
        csvFile.deleteOnExit();
        KeyStoreHandlerDecorator keyStoreHandler = new KeyStoreHandlerDecorator(
                new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE, TRUSTSTORE_FILENAME, TRUSTSTORE_PASSWORD));
        CsvSecureVerifier csvVerifier = new CsvSecureVerifier(csvFile, CsvPreference.EXCEL_PREFERENCE,
                new KeyStoreSecureStorage(keyStoreHandler,
                        keyStoreHandler.readPublicKeyFromKeyStore(KeyStoreSecureStorage.ENTRY_SIGNATURE)));
        CsvSecureVerifier.VerificationResult verificationResult = csvVerifier.verify();
        assertThat(verificationResult.hasPassedVerification()).isFalse();
        assertThat(verificationResult.getFailureReason()).isNotEmpty();
    }

    @DataProvider
    public Object[][] invalidContent() {
        return new Object[][] {
            // Invalid header
            { "src/test/resources/secureCsvInvalidHeader.csv" },
            // Invalid HMAC
            { "src/test/resources/secureCsvInvalidHMAC.csv" },
            // Invalid signature
            { "src/test/resources/secureCsvInvalidSignature.csv" },
            // No signature at end of file
            { "src/test/resources/secureCsvInvalidMissingFinalSignature.csv" }
        };
    }

    /** Writes a tamper evident file with the columns first and second, one row per array of values. */
    private static File writeSecureCsvFile(String[]... rows) throws Exception {
        final String[] header = { "first", "second" };
        final File logDirectory = Files.createTempDirectory("CsvSecureVerifierTest").toFile();
        final File csvFile = new File(logDirectory, "tamper-evident.csv");
        final CsvAuditEventHandlerConfiguration config = new CsvAuditEventHandlerConfiguration();
        config.getSecurity().setEnabled(true);
        // leave the final signature to close()
        config.getSecurity().setSignatureInterval("5 minutes");
        try (SecureCsvWriter writer = new SecureCsvWriter(csvFile, header, CsvPreference.EXCEL_PREFERENCE, config,
                signatureKeyStore(), new Random(42))) {
            for (String[] row : rows) {
                final Map<String, String> values = new HashMap<>();
                for (int i = 0; i < header.length; i++) {
                    if (row[i] != null) {
                        values.put(header[i], row[i]);
                    }
                }
                writer.writeEvent(values);
            }
        }
        return csvFile;
    }

    /** Verifies a file that {@link #writeSecureCsvFile} wrote, with its own keystore. */
    private static VerificationResult verifySecureCsvFile(File csvFile) throws Exception {
        final KeyStoreHandlerDecorator signatureKeyStore = signatureKeyStore();
        final String password = Base64.encode(
                signatureKeyStore.readSecretKeyFromKeyStore(CsvSecureConstants.ENTRY_PASSWORD).getEncoded());
        final KeyStoreSecureStorage secureStorage = new KeyStoreSecureStorage(
                new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE,
                        CsvSecureUtils.keyStoreFile(csvFile).getPath(), password),
                signatureKeyStore.readPublicKeyFromKeyStore(KeyStoreSecureStorage.ENTRY_SIGNATURE));
        return new CsvSecureVerifier(csvFile, CsvPreference.EXCEL_PREFERENCE, secureStorage).verify();
    }

    private static KeyStoreHandlerDecorator signatureKeyStore() throws Exception {
        return new KeyStoreHandlerDecorator(
                new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE, KEYSTORE_FILENAME, KEYSTORE_PASSWORD));
    }
}
