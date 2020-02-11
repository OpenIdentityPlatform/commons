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

import static org.forgerock.audit.events.handlers.FileBasedEventHandlerConfiguration.FileRotation.DEFAULT_ROTATION_FILE_SUFFIX;
import static org.forgerock.audit.handlers.csv.CsvSecureConstants.KEYSTORE_TYPE;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.crypto.SecretKey;

import org.forgerock.audit.handlers.csv.CsvSecureVerifier.VerificationResult;
import org.forgerock.audit.retention.FileNamingPolicy;
import org.forgerock.audit.retention.TimeStampFileNamingPolicy;
import org.forgerock.audit.secure.JcaKeyStoreHandler;
import org.forgerock.audit.secure.KeyStoreHandlerDecorator;
import org.forgerock.audit.secure.KeyStoreSecureStorage;
import org.forgerock.audit.secure.SecureStorageException;
import org.forgerock.util.Option;
import org.forgerock.util.Options;
import org.forgerock.util.annotations.VisibleForTesting;
import org.forgerock.util.encode.Base64;
import org.supercsv.prefs.CsvPreference;

/**
 * Command line interface for verifying an archived set of tamper evident CSV audit log files for a particular topic.
 */
public final class CsvSecureArchiveVerifierCli {

    private static final Option<Path> ARCHIVE_DIRECTORY = Option.of(Path.class, null);
    private static final Option<String> TOPIC = Option.of(String.class, null);
    private static final Option<String> PREFIX = Option.of(String.class, "");
    private static final Option<String> SUFFIX = Option.of(String.class, DEFAULT_ROTATION_FILE_SUFFIX);
    private static final Option<Path> KEYSTORE_FILE = Option.of(Path.class, null);
    private static final Option<String> KEYSTORE_PASSWORD = Option.of(String.class, null);

    @VisibleForTesting
    static PrintStream out = System.out;
    @VisibleForTesting
    static PrintStream err = System.err;
    @VisibleForTesting
    static FileNamingPolicyFactory fileNamingPolicyFactory = new DefaultFileNamingPolicyFactory();

    /**
     * Entry point for CLI.
     *
     * @param args command line arguments.
     */
    public static void main(final String[] args) {

        Options options = new OptionsParser(out, err).parse(args);
        if (options == null) {
            return;
        }

        final Path archiveDirectory = options.get(ARCHIVE_DIRECTORY);
        final String topic = options.get(TOPIC);
        final File liveFile = new File(archiveDirectory.toFile(), topic + ".csv");
        final String prefix = options.get(PREFIX) + CsvAuditEventHandler.SECURE_CSV_FILENAME_PREFIX;
        final String suffix = options.get(SUFFIX);
        final FileNamingPolicy fileNamingPolicy = fileNamingPolicyFactory.newFileNamingPolicy(liveFile, suffix, prefix);
        final Path keystoreFile = options.get(KEYSTORE_FILE);
        final String keystorePassword = options.get(KEYSTORE_PASSWORD);

        final KeyStoreHandlerDecorator keyStoreHandler = getKeyStoreHandlerDecorator(keystoreFile, keystorePassword);
        if (keyStoreHandler == null) {
            return;
        }

        final PublicKey publicKey = getSignaturePublicKey(keyStoreHandler);
        if (publicKey == null) {
            return;
        }

        final String password = getKeystorePassword(keyStoreHandler);
        if (password == null) {
            return;
        }

        final CsvSecureArchiveVerifier archiveVerifier =
                new CsvSecureArchiveVerifier(fileNamingPolicy, password, publicKey, CsvPreference.EXCEL_PREFERENCE);
        final List<CsvSecureVerifier.VerificationResult> verificationResults = archiveVerifier.verify();

        printVerificationResults(verificationResults, out);
    }

    private static KeyStoreHandlerDecorator getKeyStoreHandlerDecorator(
            final Path keystoreFile, final String keystorePassword) {
        try {
            return new KeyStoreHandlerDecorator(
                    new JcaKeyStoreHandler(KEYSTORE_TYPE, keystoreFile.toFile().getAbsolutePath(), keystorePassword));
        } catch (Exception e) {
            err.println("Unable to open keystore");
            return null;
        }
    }

    private static PublicKey getSignaturePublicKey(KeyStoreHandlerDecorator keyStoreHandler) {
        try {
            return keyStoreHandler.readPublicKeyFromKeyStore(KeyStoreSecureStorage.ENTRY_SIGNATURE);
        } catch (SecureStorageException e) {
            err.println("Unable to read " + KeyStoreSecureStorage.ENTRY_SIGNATURE + " public key from keystore");
            return null;
        }
    }

    private static String getKeystorePassword(KeyStoreHandlerDecorator keyStoreHandler) {
        try {
            final SecretKey passwordKey = keyStoreHandler.readSecretKeyFromKeyStore(CsvSecureConstants.ENTRY_PASSWORD);
            return Base64.encode(passwordKey.getEncoded());
        } catch (SecureStorageException e) {
            err.println("Unable to read " + CsvSecureConstants.ENTRY_PASSWORD + " secret key from keystore");
            return null;
        }
    }

    static void printVerificationResults(final List<VerificationResult> verificationResults, final PrintStream out) {
        for (final VerificationResult verificationResult : verificationResults) {
            String filename = verificationResult.getArchiveFile().getName();
            if (verificationResult.hasPassedVerification()) {
                out.println("PASS    " + filename);
            } else {
                out.println("FAIL    " + filename + "    " + verificationResult.getFailureReason());
            }
        }
    }

    static final class OptionsParser {

        static final String FLAG_ARCHIVE_DIRECTORY = "--archive";
        static final String FLAG_TOPIC = "--topic";
        static final String FLAG_PREFIX = "--prefix";
        static final String FLAG_SUFFIX = "--suffix";
        static final String FLAG_KEYSTORE_FILE = "--keystore";
        static final String FLAG_KEYSTORE_PASSWORD = "--password";

        private static final String DESC_ARCHIVE_DIRECTORY = "path to directory containing files to verify";
        private static final String DESC_TOPIC = "name of topic fileset to verify";
        private static final String DESC_PREFIX = "prefix prepended to archive files";
        private static final String DESC_SUFFIX = "format of timestamp suffix appended to archive files";
        private static final String DESC_KEYSTORE_FILE = "path to keystore file";
        private static final String DESC_KEYSTORE_PASSWORD = "keystore file password";

        private final PrintStream out;
        private final PrintStream err;

        OptionsParser(final PrintStream out, final PrintStream err) {
            this.out = out;
            this.err = err;
        }

        Options parse(final String[] args) {
            Options options = Options.defaultOptions();

            if (args.length == 0) {
                printHelp();
                return null;
            }

            Set<String> flagsSeen = new HashSet<>();
            for (int i = 0; i < args.length; i += 2) {
                final boolean isLastArgument = args.length == i + 1;
                final String currentArgument = args[i];
                if (flagsSeen.contains(currentArgument)) {
                    err.println(currentArgument + " should only be provided once");
                    return null;
                }
                flagsSeen.add(currentArgument);
                final String nextArgument = isLastArgument ? null : args[i + 1];
                switch (currentArgument) {
                case FLAG_ARCHIVE_DIRECTORY:
                    options.set(ARCHIVE_DIRECTORY,
                            getPathOption(nextArgument, FLAG_ARCHIVE_DIRECTORY, DESC_ARCHIVE_DIRECTORY));
                    break;
                case FLAG_TOPIC:
                    options.set(TOPIC, getStringOption(nextArgument, FLAG_TOPIC, DESC_TOPIC));
                    break;
                case FLAG_PREFIX:
                    options.set(PREFIX, getStringOption(nextArgument, FLAG_PREFIX, DESC_PREFIX));
                    break;
                case FLAG_SUFFIX:
                    options.set(SUFFIX, getStringOption(nextArgument, FLAG_SUFFIX, DESC_SUFFIX));
                    break;
                case FLAG_KEYSTORE_FILE:
                    options.set(KEYSTORE_FILE, getPathOption(nextArgument, FLAG_KEYSTORE_FILE, DESC_KEYSTORE_FILE));
                    break;
                case FLAG_KEYSTORE_PASSWORD:
                    options.set(KEYSTORE_PASSWORD,
                            getStringOption(nextArgument, FLAG_KEYSTORE_PASSWORD, DESC_KEYSTORE_PASSWORD));
                    break;
                default:
                    err.println("Unknown flag " + currentArgument);
                    return null;
                }
            }

            if (!flagsSeen.contains(FLAG_ARCHIVE_DIRECTORY) && options.get(ARCHIVE_DIRECTORY) == null) {
                err.println(DESC_ARCHIVE_DIRECTORY + " must be specified using flag " + FLAG_ARCHIVE_DIRECTORY);
                return null;
            }
            if (!flagsSeen.contains(FLAG_TOPIC) && options.get(TOPIC) == null) {
                err.println(DESC_TOPIC + " must be specified using flag " + FLAG_TOPIC);
                return null;
            }
            if (!flagsSeen.contains(FLAG_KEYSTORE_FILE) && options.get(KEYSTORE_FILE) == null) {
                err.println(DESC_KEYSTORE_FILE + " must be specified using flag " + FLAG_KEYSTORE_FILE);
                return null;
            }
            if (!flagsSeen.contains(FLAG_KEYSTORE_PASSWORD) && options.get(KEYSTORE_PASSWORD) == null) {
                err.println(DESC_KEYSTORE_PASSWORD + " must be specified using flag " + FLAG_KEYSTORE_PASSWORD);
                return null;
            }

            return options;
        }

        private void printHelp() {
            out.println(String.format("arguments: %s <path> %s <topic> [%s <prefix>] "
                    + "[%s <suffix>] %s <path> %s <password>", FLAG_ARCHIVE_DIRECTORY, FLAG_TOPIC, FLAG_PREFIX,
                    FLAG_SUFFIX, FLAG_KEYSTORE_FILE, FLAG_KEYSTORE_PASSWORD));
            out.println("");
            out.println(String.format("   %-15s %s", FLAG_ARCHIVE_DIRECTORY, DESC_ARCHIVE_DIRECTORY));
            out.println(String.format("   %-15s %s", FLAG_TOPIC, DESC_TOPIC));
            out.println(String.format("   %-15s %s", FLAG_PREFIX, DESC_PREFIX));
            out.println(String.format("   %-15s %s", FLAG_SUFFIX, DESC_SUFFIX));
            out.println(String.format("   %-15s %s", FLAG_KEYSTORE_FILE, DESC_KEYSTORE_FILE));
            out.println(String.format("   %-15s %s", FLAG_KEYSTORE_PASSWORD, DESC_KEYSTORE_PASSWORD));
        }

        private Path getPathOption(String nextArgument, String flag, String description) {
            if (nextArgument == null) {
                err.println(flag + " flag must be followed by " + description);
                return null;
            }
            final File file = new File(nextArgument);
            if (!file.exists()) {
                err.println(file + " not found");
                return null;
            }
            return file.toPath();
        }

        private String getStringOption(String nextArgument, String flag, String description) {
            if (nextArgument == null) {
                err.println(flag + " flag must be followed by " + description);
                return null;
            }
            return nextArgument;
        }

    }

    /**
     * This interface exists solely to allow tests to replace the default FileNamingPolicy used by {@link #main}.
     * <br/>
     * The default policy {@link TimeStampFileNamingPolicy} sorts files by their timestamp meta-data but this is only
     * accurate to the nearest second and therefore doesn't correctly sort the files generated by tests (as multiple
     * files can be created within a single second).
     */
    interface FileNamingPolicyFactory {

        FileNamingPolicy newFileNamingPolicy(File liveFile, String suffix, String prefix);

    }

    static class DefaultFileNamingPolicyFactory implements FileNamingPolicyFactory {

        @Override
        public FileNamingPolicy newFileNamingPolicy(File liveFile, String suffix, String prefix) {
            return new TimeStampFileNamingPolicy(liveFile, suffix, prefix);
        }
    }

    private CsvSecureArchiveVerifierCli() {
        // never created
    }

}
