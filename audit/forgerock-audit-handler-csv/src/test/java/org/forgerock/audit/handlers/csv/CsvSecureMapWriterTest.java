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

import org.forgerock.audit.secure.JcaKeyStoreHandler;
import org.forgerock.audit.secure.KeyStoreHandler;
import org.forgerock.audit.secure.KeyStoreSecureStorage;
import org.forgerock.audit.secure.SecureStorage;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;
import static org.forgerock.util.time.Duration.duration;

import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.security.KeyStore;
import java.util.Map;

import org.forgerock.util.time.Duration;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.io.ICsvMapWriter;
import org.supercsv.prefs.CsvPreference;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


@SuppressWarnings("javadoc")
public class CsvSecureMapWriterTest {

    static final String KEYSTORE_FILENAME = "target/test-classes/keystore-signature.jks";
    static final String KEYSTORE_PASSWORD = "password";

    private KeyStoreHandler keyStoreHandler;
    private SecureStorage secureStorage;
    private final Duration signatureInterval = duration("100 milliseconds");

    @BeforeMethod
    public void beforeMethod() throws Exception {
        cleanupKeystore();
    }

    @AfterClass
    public void afterClass() throws Exception {
        cleanupKeystore();
    }

    void cleanupKeystore() throws Exception {
        // This keystore was generated using the following command-line :
        // keytool -genkeypair -alias "Signature" -dname CN=a -keystore src/test/resources/keystore-signature.jks \
        // -storepass password -storetype JCEKS -keypass password -keyalg RSA -sigalg SHA256withRSA

        // keytool -genseckey -alias "InitialKey" -keystore src/test/resources/keystore-signature.jks \
        // -storepass password -storetype JCEKS -keypass password -keyalg HmacSHA256 -keysize 256

        // Then list the keystore's content to ensure what is inside.
        // keytool -list -keystore src/test/resources/keystore-signature.jks -storepass password -storetype JCEKS
        keyStoreHandler = new JcaKeyStoreHandler("JCEKS", KEYSTORE_FILENAME, KEYSTORE_PASSWORD);
        secureStorage = new KeyStoreSecureStorage(keyStoreHandler);

        KeyStore store = keyStoreHandler.getStore();
        assertThat(store.containsAlias("InitialKey")).isTrue();
        assertThat(store.containsAlias("Signature")).isTrue();

        // Clean up the Keystore in order to have deterministic tests
        store.deleteEntry("CurrentKey");
        store.deleteEntry("CurrentSignature");

        keyStoreHandler.store();

        // Export the SecretKey to a separate keystore for the verifier
        // keytool -importkeystore -srckeystore src/test/resources/keystore-signature.jks \
        // -destkeystore forgerock-audit-handler-csv/keystore-verifier.jks \
        // -srcstoretype JCEKS -deststoretype JCEKS -srcstorepass password -deststorepass password \
        // -srcalias InitialKey -destalias InitialKey -srckeypass password -destkeypass password

        // Export the public key
        // keytool -exportcert -alias "Signature" -keystore src/test/resources/keystore-signature.jks \
        // -storepass password -storetype JCEKS -file signature.cert

        // Import the public key
        // keytool -importcert -alias "Signature" -keystore forgerock-audit-handler-csv/keystore-verifier.jks \
        // -storepass password -storetype JCEKS -file signature.cert
//
//        SecretKey key = (SecretKey) store.getKey("InitialKey", keystorePassword.toCharArray());
//
//        KeyStore storeVerifier = KeyStore.getInstance("JCEKS");
//        storeVerifier.load(null, null);
//        KeyStore.SecretKeyEntry secKeyEntry = new KeyStore.SecretKeyEntry(key);
//        KeyStore.ProtectionParameter params = new KeyStore.PasswordProtection(keystorePassword.toCharArray());
//        storeVerifier.setEntry("InitialKey", secKeyEntry, params);
//        File keystoreVerifier = new File("target/test-classes/keystore-verifier.jks");
//        try (FileOutputStream fos = new FileOutputStream(keystoreVerifier)) {
//            storeVerifier.store(fos, keystorePassword.toCharArray());
//        }
    }

    @Test
    public void shouldGenerateHMACColumn() throws Exception {
        File actual = new File("target/test-classes/shouldGenerateHMACColumn-actual.txt");
        Writer writer = new FileWriter(actual);
        ICsvMapWriter csvMapWriter = new CsvMapWriter(writer , CsvPreference.EXCEL_PREFERENCE);
        try (CsvSecureMapWriter csvHMACWriter = new CsvSecureMapWriter(csvMapWriter,
                secureStorage,
                signatureInterval,
                false)) {
            final String header = "FOO";
            Map<String, String> values;

            csvHMACWriter.writeHeader(header);

            values = singletonMap(header, "bar");
            csvHMACWriter.write(values, header);

            values = singletonMap(header, "quix");
            csvHMACWriter.write(values, header);
        }

        assertThat(contentOf(actual)).isEqualTo(contentOf(new File("target/test-classes/shouldGenerateHMACColumn-expected.txt")));
    }

    @Test
    public void shouldGenerateHeaderWithRecordAndBlockSignatureColumns() throws Exception {
        Writer writer = new StringWriter();
        ICsvMapWriter csvMapWriter = new CsvMapWriter(writer , CsvPreference.STANDARD_PREFERENCE);
        try (CsvSecureMapWriter csvHMACWriter = new CsvSecureMapWriter(csvMapWriter,
                secureStorage,
                signatureInterval,
                false)) {
            String header = "FOO";
            csvHMACWriter.writeHeader(header);
        }

        assertThat(writer.toString()).isEqualTo("FOO,HMAC,SIGNATURE\r\n");
    }

    @Test
    public void shouldGeneratePeriodicallySignature() throws Exception {
        // Scenario :
        // - Write header
        // - Write one row
        // - Wait for 200 ms
        // - assert that the file contains header, row, signature
        // - Write one row
        // - close
        // - assert that the file contains header, row, signature

        File actual = new File("target/test-classes/shouldGeneratePeriodicallySignature-actual.txt");
        Writer writer = new FileWriter(actual);
        ICsvMapWriter csvMapWriter = new CsvMapWriter(writer, CsvPreference.EXCEL_PREFERENCE);
        try (CsvSecureMapWriter csvHMACWriter = new CsvSecureMapWriter(csvMapWriter,
                secureStorage,
                signatureInterval,
                false)) {
            final String header = "FOO";
            csvHMACWriter.writeHeader(header);

            csvHMACWriter.write(singletonMap(header, "bar"), header);

            // A signature has to be generated during this timelapse.
            Thread.sleep(200);

            // We expect :
            // - header
            // - data row with bar + HMAC
            // - signature
            assertThat(contentOf(actual)).isEqualTo(contentOf(new File("target/test-classes/shouldGeneratePeriodicallySignature-partial.txt")));


            csvHMACWriter.write(singletonMap(header, "quix"), header);
        }

        // We expect :
        // - header
        // - data row with bar + HMAC
        // - signature
        // - data row with bar + HMAC
        // - signature // because of closing the CsvWriter
        assertThat(contentOf(actual)).isEqualTo(contentOf(new File("target/test-classes/shouldGeneratePeriodicallySignature-expected.txt")));
    }
}
