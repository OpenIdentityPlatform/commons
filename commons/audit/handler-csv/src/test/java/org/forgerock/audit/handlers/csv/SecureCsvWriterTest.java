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
 * Portions Copyrighted 2026 3A Systems, LLC
 */
package org.forgerock.audit.handlers.csv;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.contentOf;
import static org.forgerock.audit.handlers.csv.CsvSecureConstants.*;
import static org.forgerock.util.time.Duration.duration;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PublicKey;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

import org.forgerock.audit.handlers.csv.CsvSecureVerifier.VerificationResult;
import org.forgerock.audit.retention.FileNamingPolicy;
import org.forgerock.audit.retention.TimeStampFileNamingPolicy;
import org.forgerock.audit.secure.JcaKeyStoreHandler;
import org.forgerock.audit.secure.KeyStoreHandlerDecorator;
import org.forgerock.audit.secure.KeyStoreSecureStorage;
import org.forgerock.audit.secure.SecureStorage;
import org.forgerock.audit.secure.SecureStorageException;
import org.forgerock.audit.util.GzipUtil;
import org.forgerock.util.encode.Base64;
import org.forgerock.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.prefs.CsvPreference;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class SecureCsvWriterTest {

    private static final Logger logger = LoggerFactory.getLogger(SecureCsvWriterTest.class);
    private static final String NEW_LINE = System.lineSeparator();

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
                new JcaKeyStoreHandler(KEYSTORE_TYPE, KEYSTORE_FILENAME, KEYSTORE_PASSWORD));

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
        // Use a signature interval long enough to never fire during the test, so that the final
        // signature is deterministically written by close() and the file content is predictable.
        try (SecureCsvWriter secureCsvWriter = new SecureCsvWriter(
                actual, new String[]{header}, CsvPreference.EXCEL_PREFERENCE,
                createBasicSecureConfig("5 minutes"), keyStoreHandler, random)) {
            Map<String, String> values;

//            secureCsvWriter.writeHeader(header);

            values = singletonMap(header, "bar");
            secureCsvWriter.writeEvent(values);

            values = singletonMap(header, "quix");
            secureCsvWriter.writeEvent(values);
        }

        assertThat(contentOf(actual)).isEqualToIgnoringWhitespace(contentOf(
                new File("target/test-classes/shouldGenerateHMACColumn-expected.txt")));
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
                actual, new String[]{header}, CsvPreference.EXCEL_PREFERENCE,
                createBasicSecureConfig(signatureInterval.toString()), keyStoreHandler, random)) {

            secureCsvWriter.writeEvent(singletonMap(header, "bar"));

            // A signature has to be generated during this timelapse.
            Thread.sleep(signatureInterval.to(TimeUnit.MILLISECONDS) + 200);

            // We expect :
            // - header
            // - data row with bar + HMAC
            // - signature
            assertThat(contentOf(actual)).isEqualToIgnoringWhitespace(contentOf(
                    new File("target/test-classes/shouldGeneratePeriodicallySignature-partial.txt")));


            secureCsvWriter.writeEvent(singletonMap(header, "quix"));
        }

        // We expect :
        // - header
        // - data row with bar + HMAC
        // - signature
        // - data row with bar + HMAC
        // - signature // because of closing the CsvWriter
        assertThat(contentOf(actual)).isEqualToIgnoringWhitespace(contentOf(
                new File("target/test-classes/shouldGeneratePeriodicallySignature-expected.txt")));
    }

    @Test
    public void shouldStopSchedulerAndCloseFileWhenFinalSignatureFails() throws Exception {
        final Path logDirectory = Files.createTempDirectory("SecureCsvWriterTest");
        final File actual = new File(logDirectory.toFile(), "tamper-evident-test.csv");
        final String header = "FOO";
        // The scheduler of the writer creates its thread in the group of the thread that creates the writer.
        final ThreadGroup writerThreads = new ThreadGroup("SecureCsvWriterTest");
        // A signature interval long enough to leave the final signature to close().
        final FutureTask<SecureCsvWriter> creation = new FutureTask<>(() -> new SecureCsvWriter(
                actual, new String[]{header}, CsvPreference.EXCEL_PREFERENCE,
                createBasicSecureConfig("5 minutes"), keyStoreHandler, random));
        final Thread creator = new Thread(writerThreads, creation);
        creator.start();
        final SecureCsvWriter secureCsvWriter = creation.get();
        creator.join();
        secureCsvWriter.writeEvent(singletonMap(header, "one"));
        assertThat(writerThreads.activeCount()).as("scheduler threads").isPositive();

        // A keystore cannot be stored over a directory, so the final signature fails.
        final File keyStoreFile = new File(actual.getPath() + ".keystore");
        assertThat(keyStoreFile.delete()).isTrue();
        assertThat(keyStoreFile.mkdir()).isTrue();
        assertThatThrownBy(secureCsvWriter::close)
                .isInstanceOf(IOException.class)
                .hasCauseInstanceOf(SecureStorageException.class);

        // The scheduler thread is not a daemon thread, it would keep the JVM from exiting.
        assertThreadsTerminate(writerThreads);

        // The file is closed: an event written after close() does not reach it, whether the write fails or not.
        final long length = actual.length();
        catchThrowable(() -> secureCsvWriter.writeEvent(singletonMap(header, "two")));
        assertThat(actual.length()).isEqualTo(length);
    }

    /** Fails if a thread of the group is still alive after 5 s. */
    static void assertThreadsTerminate(ThreadGroup group) throws InterruptedException {
        final Thread[] threads = new Thread[group.activeCount() + 1];
        final int count = group.enumerate(threads);
        for (int i = 0; i < count; i++) {
            threads[i].join(5000);
            assertThat(threads[i].isAlive()).as(threads[i].getName() + " alive").isFalse();
        }
    }

    @Test(timeOut = 60_000)
    public void shouldForceRotationWhileAnotherThreadWritesAnEvent() throws Exception {
        final Path logDirectory = Files.createTempDirectory("SecureCsvWriterTest");
        final File actual =
                new File(logDirectory.toFile(), CsvAuditEventHandler.SECURE_CSV_FILENAME_PREFIX + "access.csv");
        final String header = "FOO";
        CsvAuditEventHandlerConfiguration config = new CsvAuditEventHandlerConfiguration();
        config.getSecurity().setEnabled(true);
        config.getSecurity().setSignatureInterval("5 minutes");
        config.getFileRotation().setRotationEnabled(true);
        config.getFileRotation().setRotationFileSuffix("-yyyy.MM.dd-HH.mm.ss.SSS");
        config.setRotationRetentionCheckInterval("5 minutes");
        final SecureCsvWriter secureCsvWriter = new SecureCsvWriter(
                actual, new String[]{header}, CsvPreference.EXCEL_PREFERENCE, config, keyStoreHandler, random);
        // a pending signature, which the rotation writes first
        secureCsvWriter.writeEvent(singletonMap(header, "one"));

        final FutureTask<Boolean> rotation = new FutureTask<>(secureCsvWriter::forceRotation);
        final Thread rotator = new Thread(rotation, "rotator");
        rotator.setDaemon(true);
        final FutureTask<Void> publication = new FutureTask<>(() -> {
            // hold the signature lock, and only it, as writeEvent() holds it while it writes to the rotatable writer
            secureCsvWriter.beforeRollingOver();
            try {
                rotator.start();
                awaitWaitingForLockOf(rotator, Thread.currentThread());
                secureCsvWriter.writeEvent(singletonMap(header, "two"));
            } finally {
                secureCsvWriter.afterRollingOver();
            }
            return null;
        });
        final Thread publisher = new Thread(publication, "publisher");
        publisher.setDaemon(true);
        publisher.start();

        publication.get(10, TimeUnit.SECONDS);
        assertThat(rotation.get(10, TimeUnit.SECONDS)).isTrue();
        secureCsvWriter.close();

        // both events are in the archive, before the signature that the rotation writes
        final SecretKey keystorePasswordKey = keyStoreHandler.readSecretKeyFromKeyStore(ENTRY_PASSWORD);
        final String keystorePassword = Base64.encode(keystorePasswordKey.getEncoded());
        final PublicKey publicKey = keyStoreHandler.readPublicKeyFromKeyStore(KeyStoreSecureStorage.ENTRY_SIGNATURE);
        final FileNamingPolicy fileNamingPolicy =
                new TimeStampFileNamingPolicy(actual, "-yyyy.MM.dd-HH.mm.ss.SSS", "");
        final List<VerificationResult> verificationResults = new CsvSecureArchiveVerifier(
                fileNamingPolicy, keystorePassword, publicKey, CsvPreference.EXCEL_PREFERENCE).verify();
        assertThat(verificationResults).hasSize(1);
        final VerificationResult archive = verificationResults.get(0);
        assertThat(archive.hasPassedVerification()).as(archive.getFailureReason()).isTrue();
        assertThat(contentOf(archive.getArchiveFile())).containsPattern("(?m)^one,").containsPattern("(?m)^two,");
    }

    /** Waits until the thread waits for a lock that the owner holds. */
    private static void awaitWaitingForLockOf(final Thread thread, final Thread owner) throws InterruptedException {
        final ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!isWaitingForLockOf(threads, thread, owner) && System.nanoTime() < deadline) {
            Thread.sleep(1);
        }
        assertThat(isWaitingForLockOf(threads, thread, owner))
                .as(thread.getName() + " waits for a lock of " + owner.getName())
                .isTrue();
    }

    private static boolean isWaitingForLockOf(final ThreadMXBean threads, final Thread thread, final Thread owner) {
        final ThreadInfo threadInfo = threads.getThreadInfo(thread.getId());
        return threadInfo != null && threadInfo.getLockOwnerId() == owner.getId();
    }

    private CsvAuditEventHandlerConfiguration createBasicSecureConfig(String interval) {
        CsvAuditEventHandlerConfiguration configuration = new CsvAuditEventHandlerConfiguration();
        configuration.getSecurity().setEnabled(true);
        configuration.getSecurity().setSignatureInterval(interval);
        return configuration;
    }

    @DataProvider
    private Object[][] rotationFileSuffixes() {
        return new Object[][] {
            { "-yyyy.MM.dd-HH.mm.ss.SSS" },
            // the archives are gzip-compressed
            { "-yyyy.MM.dd-HH.mm.ss.SSS.gz" }
        };
    }

    @Test(dataProvider = "rotationFileSuffixes")
    public void shouldRotateCsvAndKeyStoreFile(final String rotationFileSuffix) throws Exception {
        final Path logDirectory = Files.createTempDirectory("SecureCsvWriterTest");
        final String filename = CsvAuditEventHandler.SECURE_CSV_FILENAME_PREFIX + "shouldRotateCsvAndKeyStoreFile.csv";
        final File actual = new File(logDirectory.toFile(), filename);
        final String header = "FOO";
        CsvAuditEventHandlerConfiguration config = new CsvAuditEventHandlerConfiguration();
        config.getSecurity().setEnabled(true);
        config.getSecurity().setSignatureInterval("5 minutes"); // ensure no periodically added signatures during test
        config.getFileRotation().setRotationEnabled(true);
        config.getFileRotation().setRotationFileSuffix(rotationFileSuffix);
        config.getFileRotation().setMaxFileSize(20);
        config.setRotationRetentionCheckInterval("5 minutes");
        try (SecureCsvWriter secureCsvWriter = new SecureCsvWriter(
                actual, new String[]{header}, CsvPreference.EXCEL_PREFERENCE, config, keyStoreHandler, random)) {
            secureCsvWriter.writeEvent(singletonMap(header, "one"));
            secureCsvWriter.writeEvent(singletonMap(header, "two"));
            secureCsvWriter.writeEvent(singletonMap(header, "three"));
            secureCsvWriter.writeEvent(singletonMap(header, "four"));
            secureCsvWriter.writeEvent(singletonMap(header, "five"));
            secureCsvWriter.writeEvent(singletonMap(header, "six"));
        }

        final SecretKey keystorePasswordKey = keyStoreHandler.readSecretKeyFromKeyStore(ENTRY_PASSWORD);
        final String keystorePassword = Base64.encode(keystorePasswordKey.getEncoded());
        final PublicKey publicKey = keyStoreHandler.readPublicKeyFromKeyStore(KeyStoreSecureStorage.ENTRY_SIGNATURE);
        final FileNamingPolicy fileNamingPolicy =
                new TimeStampFileNamingPolicyWithNamedBasedOrdering(actual, rotationFileSuffix, "");
        final CsvSecureArchiveVerifier archiveVerifier = new CsvSecureArchiveVerifier(
                fileNamingPolicy, keystorePassword, publicKey, CsvPreference.EXCEL_PREFERENCE);

        // Verify the expected files were created
        assertThat(actual).exists();
        assertThat(fileNamingPolicy.listFiles()).isNotEmpty().hasSize(6)
                .allMatch(file -> file.getName().endsWith(".gz") == rotationFileSuffix.endsWith(".gz"),
                        "compressed if the suffix ends with .gz");

        // Verify the contents of the files
        List<VerificationResult> verificationResults = archiveVerifier.verify();
        VerificationResult finalVerificationResult = verificationResults.remove(5);
        for (final VerificationResult verificationResult : verificationResults) {
            assertThat(verificationResult.hasPassedVerification())
                    .as("File " + verificationResult.getArchiveFile() + ": " + verificationResult.getFailureReason())
                    .isTrue();
        }
        // The final file is not verifiable currently as it doesn't contain any audit events (so no HMAC entry).
        // This means lastHMAC will be null when running CsvSecureVerifier.verifySignature and verification fails
        // See CAUD-225.
        logger.trace("Skipping verification of {} as it doesn't contain a HMAC and "
                + "verification of the closing signature will fail",
                finalVerificationResult.getArchiveFile().getName());
        assertThat(finalVerificationResult.hasPassedVerification())
                .as("File " + finalVerificationResult.getArchiveFile() + " cannot be verified as it contains no HMAC")
                .isFalse(); // TODO: Fix this

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        CsvSecureArchiveVerifierCli.out = new PrintStream(out);
        CsvSecureArchiveVerifierCli.err = new PrintStream(err);

        String[] args = {
            CsvSecureArchiveVerifierCli.OptionsParser.FLAG_ARCHIVE_DIRECTORY, logDirectory.toString(),
            CsvSecureArchiveVerifierCli.OptionsParser.FLAG_TOPIC, "shouldRotateCsvAndKeyStoreFile",
            CsvSecureArchiveVerifierCli.OptionsParser.FLAG_PREFIX, "",
            CsvSecureArchiveVerifierCli.OptionsParser.FLAG_SUFFIX, rotationFileSuffix,
            CsvSecureArchiveVerifierCli.OptionsParser.FLAG_KEYSTORE_FILE, KEYSTORE_FILENAME,
            CsvSecureArchiveVerifierCli.OptionsParser.FLAG_KEYSTORE_PASSWORD, KEYSTORE_PASSWORD
        };
        CsvSecureArchiveVerifierCli.fileNamingPolicyFactory =
            new CsvSecureArchiveVerifierCli.FileNamingPolicyFactory() {
                @Override
                public FileNamingPolicy newFileNamingPolicy(final File liveFile, final String suffix,
                        final String prefix) {
                    return new TimeStampFileNamingPolicyWithNamedBasedOrdering(liveFile, suffix, prefix);
                }
            };
        CsvSecureArchiveVerifierCli.main(args);

        String expectedOutput = "";
        List<File> files = fileNamingPolicy.listFiles();
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            if (i < files.size() - 1) {
                expectedOutput += "PASS    " + file.getName() + NEW_LINE;
            } else {
                // last file cannot be verified as it doesn't contain any hmac entries
                expectedOutput += "FAIL    " + file.getName() + "    The signature at row 3 is not correct." + NEW_LINE;
            }
        }
        assertThat(out.toString()).isEqualTo(expectedOutput);
        assertThat(err.toString()).isEqualTo("");
    }

    @Test
    public void shouldRotateKeyStoreFileWhenFileNameHasRegexCharacters() throws Exception {
        final Path logDirectory = Files.createTempDirectory("SecureCsvWriterTest");
        // '+' is a regex quantifier
        final File actual =
                new File(logDirectory.toFile(), CsvAuditEventHandler.SECURE_CSV_FILENAME_PREFIX + "a+b.csv");
        final String rotationFileSuffix = "-yyyy.MM.dd-HH.mm.ss.SSS";
        CsvAuditEventHandlerConfiguration config = new CsvAuditEventHandlerConfiguration();
        config.getSecurity().setEnabled(true);
        config.getSecurity().setSignatureInterval("5 minutes");
        config.getFileRotation().setRotationEnabled(true);
        config.getFileRotation().setRotationFileSuffix(rotationFileSuffix);
        config.setRotationRetentionCheckInterval("5 minutes");
        try (SecureCsvWriter secureCsvWriter = new SecureCsvWriter(
                actual, new String[]{"FOO"}, CsvPreference.EXCEL_PREFERENCE, config, keyStoreHandler, random)) {
            secureCsvWriter.writeEvent(singletonMap("FOO", "one"));
            assertThat(secureCsvWriter.forceRotation()).isTrue();
        }

        final String keystorePassword =
                Base64.encode(keyStoreHandler.readSecretKeyFromKeyStore(ENTRY_PASSWORD).getEncoded());
        final PublicKey publicKey = keyStoreHandler.readPublicKeyFromKeyStore(KeyStoreSecureStorage.ENTRY_SIGNATURE);
        final List<VerificationResult> verificationResults = new CsvSecureArchiveVerifier(
                new TimeStampFileNamingPolicy(actual, rotationFileSuffix, null), keystorePassword, publicKey,
                CsvPreference.EXCEL_PREFERENCE).verify();
        assertThat(verificationResults).hasSize(1);
        assertThat(verificationResults.get(0).hasPassedVerification())
                .as(verificationResults.get(0).getFailureReason())
                .isTrue();
    }

    static class TimeStampFileNamingPolicyWithNamedBasedOrdering extends TimeStampFileNamingPolicy {

        public TimeStampFileNamingPolicyWithNamedBasedOrdering(
                final File initialFile, final String timeStampFormat, final String prefix) {
            super(initialFile, timeStampFormat, prefix);
        }

        /**
         * List the files in the initial file directory that match the prefix, name and suffix format.
         * {@inheritDoc}
         */
        @Override
        public List<File> listFiles() {
            List<File> fileList = super.listFiles();
            // make sure the files are sorted from oldest to newest by filename - timestamps won't be accurate enough.
            // Without .gz, so that a collision name (<timestamp>.1.gz) sorts after <timestamp>.gz.
            fileList.sort(Comparator.comparing(file -> GzipUtil.removeGzipSuffix(file).getName()));
            return fileList;
        }
    }

}
