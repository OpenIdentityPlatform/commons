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

import java.io.File;

import org.forgerock.audit.secure.JcaKeyStoreHandler;
import org.forgerock.audit.secure.KeyStoreHandlerDecorator;
import org.forgerock.audit.secure.KeyStoreSecureStorage;
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
        CsvSecureVerifier csvVerifier = new CsvSecureVerifier(csvFile, new KeyStoreSecureStorage(keyStoreHandler,
                keyStoreHandler.readPublicKeyFromKeyStore(KeyStoreSecureStorage.ENTRY_SIGNATURE)));
        assertThat(csvVerifier.verify().hasPassedVerification()).isTrue();
    }

    @Test(dataProvider = "invalidContent")
    public void shouldNotVerify(String filename) throws Exception {
        File csvFile = new File(filename);
        KeyStoreHandlerDecorator keyStoreHandler = new KeyStoreHandlerDecorator(
                new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE, TRUSTSTORE_FILENAME, TRUSTSTORE_PASSWORD));
        CsvSecureVerifier csvVerifier = new CsvSecureVerifier(csvFile, new KeyStoreSecureStorage(keyStoreHandler,
                keyStoreHandler.readPublicKeyFromKeyStore(KeyStoreSecureStorage.ENTRY_SIGNATURE)));
        assertThat(csvVerifier.verify().hasPassedVerification()).isFalse();
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
}
