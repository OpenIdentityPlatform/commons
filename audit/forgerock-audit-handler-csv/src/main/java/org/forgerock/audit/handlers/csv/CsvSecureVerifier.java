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

import static org.forgerock.audit.handlers.csv.CsvSecureConstants.*;
import static org.forgerock.audit.handlers.csv.CsvSecureUtils.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Map;

import javax.crypto.SecretKey;

import org.forgerock.audit.secure.SecureStorage;
import org.forgerock.audit.secure.SecureStorageException;
import org.forgerock.util.encode.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;

/**
 * This class aims to verify a secure CSV file.
 */
class CsvSecureVerifier {

    private static final Logger logger = LoggerFactory.getLogger(CsvSecureVerifier.class);

    private File csvFile;
    private final CsvPreference csvPreference;
    private final HmacCalculator hmacCalculator;
    private final SecureStorage secureStorage;
    private String lastHMAC;
    private byte[] lastSignature;
    private String[] headers;

    /**
     * Constructs a new verifier.
     *
     * @param csvFile
     *            the CSV file to verify
     * @param csvPreference
     *            the CSV preference to use
     * @param secureStorage
     *            the secure storage containing keys
     */
    public CsvSecureVerifier(File csvFile, CsvPreference csvPreference, SecureStorage secureStorage) {
        this.csvFile = csvFile;
        this.csvPreference = csvPreference;
        this.secureStorage = secureStorage;

        try {
            SecretKey initialKey = secureStorage.readInitialKey();
            if (initialKey == null) {
                throw new IllegalStateException("Expecting to find an initial key into the keystore.");
            }

            this.hmacCalculator = new HmacCalculator(HMAC_ALGORITHM);
            this.hmacCalculator.setCurrentKey(initialKey.getEncoded());
        } catch (SecureStorageException e) {
            throw new IllegalStateException(e);
        }
    }

    public VerificationResult verify() throws IOException {
        boolean lastRowWasSigned = false;
        try (ICsvMapReader csvReader = newBufferedCsvMapReader()) {
            final String[] header = csvReader.getHeader(true);

            // Ensure header contains HEADER_HMAC and HEADER_SIGNATURE
            int checkCount = 0;
            for (String string : header) {
                if (HEADER_HMAC.equals(string) || HEADER_SIGNATURE.equals(string)) {
                    checkCount++;
                }
            }

            if (!(HEADER_HMAC.equals(header[header.length - 2])
                    && HEADER_SIGNATURE.equals(header[header.length - 1]))) {
                String msg = "Found only " + checkCount + " checked headers from : " + Arrays.toString(header);
                logger.debug(msg);
                return newVerificationFailureResult(msg);
            }
            this.headers = new String[header.length - 2];
            System.arraycopy(header, 0, this.headers, 0, this.headers.length);

            // Check the row one after the other
            Map<String, String> values;
            while ((values = csvReader.read(header)) != null) {
                logger.trace("Verifying row {}", csvReader.getRowNumber());
                lastRowWasSigned = false;
                final String encodedSign = values.get(HEADER_SIGNATURE);
                // The field HEADER_SIGNATURE is filled so let's check that special row
                if (encodedSign != null) {
                    if (csvReader.getRowNumber() == 2) {
                        // Special case : this is a rotated file, do not verify the signature but store it.
                        lastSignature = Base64.decode(encodedSign);
                    } else if (!verifySignature(encodedSign)) {
                        String msg = "The signature at row " + csvReader.getRowNumber() + " is not correct.";
                        logger.trace(msg);
                        return newVerificationFailureResult(msg);
                    } else {
                        logger.trace("The signature at row {} is correct.", csvReader.getRowNumber());
                        lastRowWasSigned = true;
                        // The signature is OK : let's continue to the next row
                        continue;
                    }
                } else {
                    // Otherwise every row must contain a valid HEADER_HMAC
                    if (!verifyHMAC(values, header)) {
                        String msg = "The HMac at row " + csvReader.getRowNumber() + " is not correct.";
                        logger.trace(msg);
                        return newVerificationFailureResult(msg);
                    } else {
                        logger.trace("The HMac at row {} is correct.", csvReader.getRowNumber());
                        // The HMAC is OK : let's continue to the next row
                        continue;
                    }
                }
            }
        }

        try {
            SecretKey currentKey = secureStorage.readCurrentKey();
            if (currentKey != null) {
                boolean keysMatch = Arrays.equals(hmacCalculator.getCurrentKey().getEncoded(), currentKey.getEncoded());
                logger.trace("keysMatch={}, lastRowWasSigned={}", keysMatch, lastRowWasSigned);
                if (!keysMatch) {
                    return newVerificationFailureResult("Final HMAC key doesn't match expected value");
                } else if (!lastRowWasSigned) {
                    return newVerificationFailureResult("Missing final signature");
                } else {
                    return newVerificationSuccessResult();
                }
            } else {
                logger.trace("currentKey is null");
                return newVerificationFailureResult("Final HMAC key is null");
            }
        } catch (SecureStorageException ex) {
            throw new IOException(ex);
        }
    }

    private CsvMapReader newBufferedCsvMapReader() throws FileNotFoundException {
        return new CsvMapReader(new BufferedReader(new FileReader(csvFile)), csvPreference);
    }

    private VerificationResult newVerificationFailureResult(String msg) {
        return new VerificationResult(csvFile, false, msg);
    }

    private VerificationResult newVerificationSuccessResult() {
        return new VerificationResult(csvFile, true, "");
    }

    private boolean verifyHMAC(Map<String, String> values, String[] header) throws IOException {
        try {
            String actualHMAC = values.get(HEADER_HMAC);
            String expectedHMAC = hmacCalculator.calculate(dataToSign(logger, values, dropExtraHeaders(header)));
            if (!actualHMAC.equals(expectedHMAC)) {
                logger.trace("The HMAC is not valid. Expected : {} Found : {}", expectedHMAC, actualHMAC);
                return false;
            } else {
                lastHMAC = actualHMAC;
                return true;
            }
        } catch (SignatureException ex) {
            logger.error(ex.getMessage(), ex);
            throw new IOException(ex);
        }
    }

    private boolean verifySignature(final String encodedSign) throws IOException {
        try {
            byte[] signature = Base64.decode(encodedSign);
            boolean verify = secureStorage.verify(dataToSign(lastSignature, lastHMAC), signature);
            if (!verify) {
                logger.trace("The signature does not match the expecting one.");
                return false;
            } else {
                lastSignature = signature;
                return true;
            }
        } catch (SecureStorageException ex) {
            logger.error(ex.getMessage(), ex);
            throw new IOException(ex);
        }
    }

    private String[] dropExtraHeaders(String... header) {
        // Drop the 2 last headers : HEADER_HMAC and HEADER_SIGNATURE
        return Arrays.copyOf(header, header.length - 2);
    }

    /**
     * Returns the headers of the underlying CSV.
     *
     * @return the headers of the underlying CSV
     */
    public String[] getHeaders() {
        return headers;
    }

    /**
     * Returns the latest read and validated HMAC.
     *
     * @return the latest read and validated HMAC
     */
    public String getLastHMAC() {
        return lastHMAC;
    }

    /**
     * Returns the latest read and validated signature.
     *
     * @return the latest read and validated signature
     */
    public byte[] getLastSignature() {
        return lastSignature;
    }

    static final class VerificationResult {

        private final File archiveFile;
        private final boolean passedVerification;
        private final String failureReason;

        VerificationResult(final File archiveFile, final boolean passedVerification, final String message) {
            this.archiveFile = archiveFile;
            this.passedVerification = passedVerification;
            this.failureReason = message;
        }

        public File getArchiveFile() {
            return archiveFile;
        }

        public boolean hasPassedVerification() {
            return passedVerification;
        }

        public String getFailureReason() {
            return failureReason;
        }
    }
}
