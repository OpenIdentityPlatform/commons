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
 * Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.audit.handlers.csv;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.audit.handlers.csv.SecureCsvWriterTest.KEYSTORE_FILENAME;
import static org.forgerock.audit.handlers.csv.SecureCsvWriterTest.KEYSTORE_PASSWORD;

import java.io.File;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.List;

import org.forgerock.audit.handlers.csv.CsvSecureVerifier.VerificationResult;
import org.forgerock.audit.retention.TimeStampFileNamingPolicy;
import org.forgerock.audit.secure.JcaKeyStoreHandler;
import org.forgerock.audit.secure.KeyStoreHandlerDecorator;
import org.forgerock.util.encode.Base64;
import org.supercsv.prefs.CsvPreference;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class CsvSecureArchiveVerifierTest {

    @Test
    public void shouldVerifyArchiveLeftUncompressed() throws Exception {
        final File logDirectory = Files.createTempDirectory("CsvSecureArchiveVerifierTest").toFile();
        logDirectory.deleteOnExit();
        final File liveFile = new File(logDirectory, CsvAuditEventHandler.SECURE_CSV_FILENAME_PREFIX + "access.csv");
        final KeyStoreHandlerDecorator keyStoreHandler = new KeyStoreHandlerDecorator(
                new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE, KEYSTORE_FILENAME, KEYSTORE_PASSWORD));
        final CsvAuditEventHandlerConfiguration config = new CsvAuditEventHandlerConfiguration();
        config.getSecurity().setEnabled(true);
        config.getSecurity().setSignatureInterval("5 minutes");
        try (SecureCsvWriter writer = new SecureCsvWriter(liveFile, new String[] { "FOO" },
                CsvPreference.EXCEL_PREFERENCE, config, keyStoreHandler, new SecureRandom())) {
            writer.writeEvent(singletonMap("FOO", "bar"));
        }
        // The rotation names the keystore after the archive name of the default suffix, which ends with .gz, and
        // keeps the file under its name without .gz when the compression fails.
        final File archive = new File(logDirectory, liveFile.getName() + "-2026.10.16-13.00.00");
        assertThat(liveFile.renameTo(archive)).isTrue();
        assertThat(CsvSecureUtils.keyStoreFile(liveFile)
                .renameTo(CsvSecureUtils.keyStoreFile(new File(archive.getPath() + ".gz")))).isTrue();

        final String archiveKeyStorePassword = Base64.encode(
                keyStoreHandler.readSecretKeyFromKeyStore(CsvSecureConstants.ENTRY_PASSWORD).getEncoded());
        final List<VerificationResult> verificationResults = new CsvSecureArchiveVerifier(
                new TimeStampFileNamingPolicy(liveFile, null, null), archiveKeyStorePassword,
                keyStoreHandler.readPublicKeyFromKeyStore(CsvSecureConstants.ENTRY_SIGNATURE),
                CsvPreference.EXCEL_PREFERENCE).verify();

        assertThat(verificationResults).hasSize(1);
        final VerificationResult verificationResult = verificationResults.get(0);
        assertThat(verificationResult.getArchiveFile()).isEqualTo(archive);
        assertThat(verificationResult.hasPassedVerification())
                .as(verificationResult.getFailureReason())
                .isTrue();
    }
}
