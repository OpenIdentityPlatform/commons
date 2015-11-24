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

import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;

import org.forgerock.audit.secure.JcaKeyStoreHandler;
import org.forgerock.audit.secure.KeyStoreHandlerDecorator;
import org.forgerock.audit.secure.KeyStoreSecureStorage;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class CsvSecureVerifierTest {

    static final String TRUSTSTORE_FILENAME = "src/test/resources/keystore-verifier.jks";
    static final String TRUSTSTORE_PASSWORD = "password";

    @Test
    public void shouldVerifyValidFile() throws Exception {
        Reader reader = new FileReader("src/test/resources/shouldGeneratePeriodicallySignature-expected.txt");
        ICsvMapReader csvMapReader = new CsvMapReader(reader, CsvPreference.EXCEL_PREFERENCE);
        KeyStoreHandlerDecorator keyStoreHandler = new KeyStoreHandlerDecorator(
                new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE, TRUSTSTORE_FILENAME, TRUSTSTORE_PASSWORD));
        CsvSecureVerifier csvVerifier = new CsvSecureVerifier(csvMapReader, new KeyStoreSecureStorage(keyStoreHandler, 
                keyStoreHandler.readPublicKeyFromKeyStore(KeyStoreSecureStorage.ENTRY_SIGNATURE)));
        assertThat(csvVerifier.verify()).isTrue();
    }

    @Test(dataProvider = "invalidContent")
    public void shouldNotVerify(String content) throws Exception {
        StringReader reader = new StringReader(content);
        ICsvMapReader csvMapReader = new CsvMapReader(reader, CsvPreference.EXCEL_PREFERENCE);

        KeyStoreHandlerDecorator keyStoreHandler = new KeyStoreHandlerDecorator(
                new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE, TRUSTSTORE_FILENAME, TRUSTSTORE_PASSWORD));
        CsvSecureVerifier csvVerifier = new CsvSecureVerifier(csvMapReader, new KeyStoreSecureStorage(keyStoreHandler,
                keyStoreHandler.readPublicKeyFromKeyStore(KeyStoreSecureStorage.ENTRY_SIGNATURE)));
        assertThat(csvVerifier.verify()).isFalse();
    }

    @DataProvider
    public Object[][] invalidContent() {
        return new Object[][] {
            // Invalid header
            { "foo,bar" },
            // Invalid HMAC
            { "foo,HMAC,SIGNATURE\nbar,quix," },
            // Invalid signature
            {"foo,HMAC,SIGNATURE\nbar,quix,\n,,blabla" },
            // No signature at end of file
            { "foo,HMAC,SIGNATURE\nbar,uLkBIiPY0+yyseXNACJX5SBwqV4RDSN8Ab8Jz7c3cYI=," }
        };
    }
}
