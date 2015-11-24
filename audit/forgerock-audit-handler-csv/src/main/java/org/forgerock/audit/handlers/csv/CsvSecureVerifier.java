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

import static org.forgerock.audit.handlers.csv.CsvSecureConstants.*;
import static org.forgerock.audit.handlers.csv.CsvSecureUtils.*;

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
import org.supercsv.io.ICsvMapReader;

/**
 * This class aims to verify a secure CSV file.
 */
class CsvSecureVerifier {

    private static final Logger logger = LoggerFactory.getLogger(CsvSecureVerifier.class);

    private ICsvMapReader csvReader;
    private final HmacCalculator hmacCalculator;
    private final SecureStorage secureStorage;
    private String lastHMAC;
    private byte[] lastSignature;
    private String[] headers;

    /**
     * Constructs a new verifier
     *
     * @param csvReader
     *            the underlying reader to read the CSV file
     * @param secureStorage
     *            the secure storage containing keys
     */
    public CsvSecureVerifier(ICsvMapReader csvReader, SecureStorage secureStorage) {
        this.csvReader = csvReader;
        this.secureStorage = secureStorage;

        try {
            SecretKey initialKey = secureStorage.readInitialKey();
            if (initialKey == null) {
                throw new IllegalStateException("Expecting to find an initial key into the keystore.");
            }
            logger.info("Starting the verifier with the key " + Base64.encode(initialKey.getEncoded()));

            this.hmacCalculator = new HmacCalculator(HMAC_ALGORITHM);
            this.hmacCalculator.setCurrentKey(initialKey.getEncoded());
        } catch (SecureStorageException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean verify() throws IOException {
        final String[] header = csvReader.getHeader(true);

        // Ensure header contains HEADER_HMAC and HEADER_SIGNATURE
        int checkCount = 0;
        for (String string : header) {
            if (HEADER_HMAC.equals(string) || HEADER_SIGNATURE.equals(string)) {
                checkCount++;
            }
        }

        if (!(HEADER_HMAC.equals(header[header.length - 2]) && HEADER_SIGNATURE.equals(header[header.length - 1]))) {
            logger.info("Found only " + checkCount + " checked headers from : " + Arrays.toString(header));
            return false;
        }
        this.headers = new String[header.length - 2];
        System.arraycopy(header, 0, this.headers, 0, this.headers.length);

        // Check the row one after the other
        boolean lastRowWasSigned = false;
        Map<String, String> values;
        while ((values = csvReader.read(header)) != null) {
            logger.info("Verifying row " + csvReader.getRowNumber());
            lastRowWasSigned = false;
            final String encodedSign = values.get(HEADER_SIGNATURE);
            // The field HEADER_SIGNATURE is filled so let's check that special row
            if (encodedSign != null) {
                if (csvReader.getRowNumber() == 2) {
                    // Special case : this is a rotated file, do not verify the signature but store it.
                    lastSignature = Base64.decode(encodedSign);
                } else if (!verifySignature(encodedSign)) {
                    logger.info("The signature at row " + csvReader.getRowNumber() + " is not correct.");
                    return false;
                } else {
                    logger.info("The signature at row " + csvReader.getRowNumber() + " is correct.");
                    lastRowWasSigned = true;
                    // The signature is OK : let's continue to the next row
                    continue;
                }
            } else {
                // Otherwise every row must contain a valid HEADER_HMAC
                if (!verifyHMAC(values, header)) {
                    logger.info("The HMac at row " + csvReader.getRowNumber() + " is not correct.");
                    return false;
                } else {
                    logger.info("The HMac at row " + csvReader.getRowNumber() + " is correct.");
                    // The HMAC is OK : let's continue to the next row
                    continue;
                }
            }
        }
        return lastRowWasSigned;
    }

    private boolean verifyHMAC(Map<String, String> values, String[] header) throws IOException {
        try {
            String actualHMAC = values.get(HEADER_HMAC);
            String expectedHMAC = hmacCalculator.calculate(dataToSign(logger, values, dropExtraHeaders(header)));
            if (!actualHMAC.equals(expectedHMAC)) {
                logger.info("The HMAC is not valid. Expected : " + expectedHMAC + " Found : " + actualHMAC);
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
                logger.info("The signature does not match the expecting one.");
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
     * Returns the headers of the underlying CSV
     *
     * @return the headers of the underlying CSV
     */
    public String[] getHeaders() {
        return headers;
    }

    /**
     * Returns the latest read and validated HMAC
     *
     * @return the latest read and validated HMAC
     */
    public String getLastHMAC() {
        return lastHMAC;
    }

    /**
     * Returns the latest read and validated signature
     *
     * @return the latest read and validated signature
     */
    public byte[] getLastSignature() {
        return lastSignature;
    }
}
