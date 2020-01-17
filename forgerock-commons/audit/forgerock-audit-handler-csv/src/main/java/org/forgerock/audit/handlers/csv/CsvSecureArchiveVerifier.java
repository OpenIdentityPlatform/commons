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
 */
package org.forgerock.audit.handlers.csv;

import static org.forgerock.audit.handlers.csv.CsvSecureConstants.KEYSTORE_TYPE;

import java.io.File;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import org.forgerock.audit.handlers.csv.CsvSecureVerifier.VerificationResult;
import org.forgerock.audit.retention.FileNamingPolicy;
import org.forgerock.audit.secure.JcaKeyStoreHandler;
import org.forgerock.audit.secure.KeyStoreHandler;
import org.forgerock.audit.secure.KeyStoreSecureStorage;
import org.forgerock.audit.secure.SecureStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.prefs.CsvPreference;

/**
 * Responsible for locating and verifying an archived set of tamper evident CSV audit log files for a particular topic.
 */
class CsvSecureArchiveVerifier {

    private static final Logger logger = LoggerFactory.getLogger(CsvSecureArchiveVerifier.class);

    private final FileNamingPolicy fileNamingPolicy;
    private final String keystorePassword;
    private final PublicKey publicKey;
    private final CsvPreference csvPreference;

    CsvSecureArchiveVerifier(final FileNamingPolicy fileNamingPolicy, final String keystorePassword,
            final PublicKey publicKey, CsvPreference csvPreference) {
        this.keystorePassword = keystorePassword;
        this.publicKey = publicKey;
        this.fileNamingPolicy = fileNamingPolicy;
        this.csvPreference = csvPreference;
    }

    List<VerificationResult> verify() {
        List<File> archiveFiles = fileNamingPolicy.listFiles();
        List<VerificationResult> verificationResults = new ArrayList<>(archiveFiles.size());
        for (File archiveFile : archiveFiles) {
            logger.trace("Verifying file {}", archiveFile);
            VerificationResult verificationResult;
            try {
                verificationResult = verifyArchiveFile(archiveFile, keystorePassword, publicKey);
            } catch (Exception e) {
                verificationResult = new VerificationResult(archiveFile, false, e.getMessage());
            }
            verificationResults.add(verificationResult);
        }
        return verificationResults;
    }

    private VerificationResult verifyArchiveFile(File archiveFile, String keystorePassword, PublicKey publicKey)
            throws Exception {
        SecureStorage secureStorage = openSecureStorageForCsvFile(archiveFile, keystorePassword, publicKey);
        CsvSecureVerifier verifier = new CsvSecureVerifier(archiveFile, csvPreference, secureStorage);
        return verifier.verify();
    }

    private SecureStorage openSecureStorageForCsvFile(File csvFile, String keystorePassword, PublicKey publicKey)
            throws Exception {
        String keystorePath = csvFile.getPath() + ".keystore";
        KeyStoreHandler keyStoreHandler = new JcaKeyStoreHandler(KEYSTORE_TYPE, keystorePath, keystorePassword);
        return new KeyStoreSecureStorage(keyStoreHandler, publicKey);
    }

}
