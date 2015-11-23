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

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;
import static org.forgerock.util.time.Duration.duration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.forgerock.audit.secure.JcaKeyStoreHandler;
import org.forgerock.audit.secure.KeyStoreHandler;
import org.forgerock.audit.secure.KeyStoreHandlerDecorator;
import org.forgerock.audit.secure.KeyStoreSecureStorage;
import org.forgerock.audit.secure.SecureStorage;
import org.forgerock.util.encode.Base64;
import org.forgerock.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.io.CsvMapReader;
import org.supercsv.prefs.CsvPreference;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class SecureCsvWriterTest {

    private static final Logger logger = LoggerFactory.getLogger(SecureCsvWriterTest.class);

    static final String KEYSTORE_FILENAME = "target/test-classes/keystore-signature.jks";
    static final String KEYSTORE_PASSWORD = "password";

    private KeyStoreHandlerDecorator keyStoreHandler;
    private SecureStorage secureStorage;
    private final Duration signatureInterval = duration("100 milliseconds");
    private Random random;

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

        // keytool -genseckey -alias "Password" -keystore src/test/resources/keystore-signature.jks \
        // -storepass password -storetype JCEKS -keypass password -keyalg HmacSHA256 -keysize 256

        // Then list the keystore's content to ensure what is inside.
        // keytool -list -keystore src/test/resources/keystore-signature.jks -storepass password -storetype JCEKS
        keyStoreHandler = new KeyStoreHandlerDecorator(
                new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE, KEYSTORE_FILENAME, KEYSTORE_PASSWORD));

        final KeyStoreHandlerDecorator memoryKeyStoreHandler = new KeyStoreHandlerDecorator(
                new MemoryKeyStoreHandler());

        random = new Random() {
            private static final long serialVersionUID = 1L;
            @Override
            public void nextBytes(byte[] bytes) {
                byte[] src = Base64.decode("UPu9xcETDpWSGaU8/WrW++74y3AOXqDPuLMZtF0IsKE=");
                if (src.length != bytes.length) {
                    throw new RuntimeException("incompatible arrays length : " + src.length + " " + bytes.length);
                }
                System.arraycopy(src, 0, bytes, 0, bytes.length);
            }
        };

        secureStorage = new KeyStoreSecureStorage(memoryKeyStoreHandler,
                keyStoreHandler.readPrivateKeyFromKeyStore(KeyStoreSecureStorage.ENTRY_SIGNATURE));

        KeyStore store = keyStoreHandler.getStore();
        assertThat(store.containsAlias("Password")).isTrue();
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
        final File actual = new File("target/test-classes/shouldGenerateHMACColumn-actual.txt");
        actual.delete();
        final String header = "FOO";
        try (SecureCsvWriter secureCsvWriter = new SecureCsvWriter(
                actual, new String[]{header}, CsvPreference.EXCEL_PREFERENCE, createBasicSecureConfig(), keyStoreHandler, random)) {
            Map<String, String> values;

//            secureCsvWriter.writeHeader(header);

            values = singletonMap(header, "bar");
            secureCsvWriter.writeEvent(values);

            values = singletonMap(header, "quix");
            secureCsvWriter.writeEvent(values);
        }

        assertThat(contentOf(actual)).isEqualTo(contentOf(new File("target/test-classes/shouldGenerateHMACColumn-expected.txt")));
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

        final File actual = new File("target/test-classes/shouldGeneratePeriodicallySignature-actual.txt");
        actual.delete();
        final String header = "FOO";
        try (SecureCsvWriter secureCsvWriter = new SecureCsvWriter(
                actual, new String[]{header}, CsvPreference.EXCEL_PREFERENCE, createBasicSecureConfig(), keyStoreHandler, random)) {

            secureCsvWriter.writeEvent(singletonMap(header, "bar"));

            // A signature has to be generated during this timelapse.
            Thread.sleep(signatureInterval.to(TimeUnit.MILLISECONDS) + 200);

            // We expect :
            // - header
            // - data row with bar + HMAC
            // - signature
            assertThat(contentOf(actual)).isEqualTo(contentOf(new File("target/test-classes/shouldGeneratePeriodicallySignature-partial.txt")));


            secureCsvWriter.writeEvent(singletonMap(header, "quix"));
        }

        // We expect :
        // - header
        // - data row with bar + HMAC
        // - signature
        // - data row with bar + HMAC
        // - signature // because of closing the CsvWriter
        assertThat(contentOf(actual)).isEqualTo(contentOf(new File("target/test-classes/shouldGeneratePeriodicallySignature-expected.txt")));
    }

    private CsvAuditEventHandlerConfiguration createBasicSecureConfig() {
        CsvAuditEventHandlerConfiguration configuration = new CsvAuditEventHandlerConfiguration();
        configuration.getSecurity().setEnabled(true);
        configuration.getSecurity().setSignatureInterval(signatureInterval.toString());
        return configuration;
    }

    @Test
    public void shouldRotateCsvAndKeyStoreFile() throws Exception {
        final Path logDirectory = Files.createTempDirectory("SecureCsvWriterTest");
        final String filename = "shouldRotateCsvAndKeyStoreFile.csv";
        final File actual = new File(logDirectory.toFile(), filename);
        final String header = "FOO";
        CsvAuditEventHandlerConfiguration config = new CsvAuditEventHandlerConfiguration();
        config.getSecurity().setEnabled(true);
        config.getSecurity().setSignatureInterval("5 minutes"); // ensure no periodically added signatures during test
        config.getFileRotation().setRotationEnabled(true);
        config.getFileRotation().setRotationFileSuffix("-MM.dd.yy-kk.mm.ss.SSS");
        config.getFileRotation().setMaxFileSize(20);

        try (SecureCsvWriter secureCsvWriter = new SecureCsvWriter(
                actual, new String[]{header}, CsvPreference.EXCEL_PREFERENCE, config, keyStoreHandler, random)) {
            secureCsvWriter.writeEvent(singletonMap(header, "one"));
            secureCsvWriter.writeEvent(singletonMap(header, "two"));
            secureCsvWriter.writeEvent(singletonMap(header, "three"));
            secureCsvWriter.writeEvent(singletonMap(header, "four"));
            secureCsvWriter.writeEvent(singletonMap(header, "five"));
            secureCsvWriter.writeEvent(singletonMap(header, "six"));
        }

        File[] csvFiles = actual.getParentFile().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(filename) && !name.endsWith(".keystore");
            }
        });
        assertThat(csvFiles).hasSize(7);
        Arrays.sort(csvFiles);
        // Confirm that the first file is the current file
        // (this file is not verifiable currently as it doesn't end with a signature)
        assertThat(csvFiles[0].getName()).isEqualTo(filename);
        // Verify all of the archived files except the final one
        for (int i = 1; i < csvFiles.length - 1; i++) {
            File csvFile = csvFiles[i];
            if (csvFile.getName().equals(filename)) {
                logger.trace("Skipping verification of {} as won't end with signature", csvFile);
                continue;
            }
            logger.trace("Verifying file {}", csvFile);
            try (CsvMapReader reader = new CsvMapReader(new BufferedReader(new FileReader(csvFile)), CsvPreference.EXCEL_PREFERENCE)) {
                String password = Base64.encode(keyStoreHandler.readSecretKeyFromKeyStore(CsvSecureConstants.ENTRY_PASSWORD).getEncoded());
                KeyStoreHandler csvKeyStoreHandler = new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE, csvFile.getPath() + ".keystore", password);
                CsvSecureVerifier verifier = new CsvSecureVerifier(reader, new KeyStoreSecureStorage(csvKeyStoreHandler,
                        keyStoreHandler.readPublicKeyFromKeyStore(KeyStoreSecureStorage.ENTRY_SIGNATURE)));
                assertThat(verifier.verify()).as("File " + csvFile.getName()).isTrue();
            }
        }
        // The final file is not verifiable currently as it doesn't contain any audit events (so no HMAC entry).
        // This means lastHMAC will be null when running CsvSecureVerifier.verifySignature and verification fails
        // See CAUD-225.
        logger.trace("Skipping verification of {} as it doesn't contain a HMAC and" +
                "verification of the closing signature will fail", csvFiles[6].getName());

    }

}
