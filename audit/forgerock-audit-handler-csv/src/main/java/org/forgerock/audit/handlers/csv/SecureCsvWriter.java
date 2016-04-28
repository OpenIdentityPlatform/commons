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

import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.forgerock.audit.handlers.csv.CsvSecureConstants.HEADER_HMAC;
import static org.forgerock.audit.handlers.csv.CsvSecureConstants.HEADER_SIGNATURE;
import static org.forgerock.audit.handlers.csv.CsvSecureConstants.SIGNATURE_ALGORITHM;
import static org.forgerock.audit.handlers.csv.CsvSecureUtils.dataToSign;
import static org.forgerock.util.Reject.checkNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.forgerock.audit.events.handlers.writers.RotatableWriter;
import org.forgerock.audit.events.handlers.writers.TextWriter;
import org.forgerock.audit.events.handlers.writers.TextWriterAdapter;
import org.forgerock.audit.events.handlers.writers.RotatableWriter.RolloverLifecycleHook;
import org.forgerock.audit.rotation.RotationContext;
import org.forgerock.audit.rotation.RotationHooks;
import org.forgerock.audit.secure.JcaKeyStoreHandler;
import org.forgerock.audit.secure.KeyStoreHandler;
import org.forgerock.audit.secure.KeyStoreHandlerDecorator;
import org.forgerock.audit.secure.KeyStoreSecureStorage;
import org.forgerock.audit.secure.SecureStorageException;
import org.forgerock.util.Reject;
import org.forgerock.util.annotations.VisibleForTesting;
import org.forgerock.util.encode.Base64;
import org.forgerock.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.prefs.CsvPreference;

/**
 * Responsible for writing to a CSV file; silently adds 2 last columns : HMAC and SIGNATURE.
 * The column HMAC is filled with the HMAC calculation of the current row and a key.
 * The column SIGNATURE is filled with the signature calculation of the last HMAC and the last signature if any.
 */
class SecureCsvWriter implements CsvWriter, RolloverLifecycleHook {

    private static final Logger logger = LoggerFactory.getLogger(SecureCsvWriter.class);

    private final CsvFormatter csvFormatter;
    private final String[] headers;
    private Writer csvWriter;
    private RotatableWriter rotatableWriter;

    private HmacCalculator hmacCalculator;
    private final ScheduledExecutorService scheduler;
    private final ReentrantLock signatureLock = new ReentrantLock();
    private final Runnable signatureTask;
    private KeyStoreSecureStorage secureStorage;
    private final Duration signatureInterval;
    private ScheduledFuture<?> scheduledSignature;

    private String lastHMAC;
    private byte[] lastSignature;
    private boolean headerWritten = false;
    private final Random random;
    private File keyStoreFile;
    private String keyStorePassword;

    SecureCsvWriter(File csvFile, String[] headers, CsvPreference csvPreference,
            CsvAuditEventHandlerConfiguration config, KeyStoreHandler keyStoreHandler, Random random)
            throws IOException {
        Reject.ifFalse(config.getSecurity().isEnabled(), "SecureCsvWriter should only be used if security is enabled");
        final boolean fileAlreadyInitialized = csvFile.exists() && csvFile.length() > 0;
        this.random = random;
        this.keyStoreFile = new File(csvFile.getPath() + ".keystore");
        this.headers = checkNotNull(headers, "The headers can't be null.");
        this.csvFormatter = new CsvFormatter(csvPreference);
        this.csvWriter = constructWriter(csvFile, fileAlreadyInitialized, config);
        this.hmacCalculator = new HmacCalculator(CsvSecureConstants.HMAC_ALGORITHM);

        try {
            KeyStoreHandlerDecorator keyStoreHandlerDecorated = new KeyStoreHandlerDecorator(keyStoreHandler);
            SecretKey password = keyStoreHandlerDecorated.readSecretKeyFromKeyStore(CsvSecureConstants.ENTRY_PASSWORD);
            if (password == null) {
                throw new IllegalArgumentException(format(
                        "No '%s' symmetric key found in the provided keystore: %s. This key must be provided.",
                        CsvSecureConstants.ENTRY_PASSWORD, keyStoreHandlerDecorated.getLocation()));
            }
            this.keyStorePassword = Base64.encode(password.getEncoded());
            KeyStoreHandler hmacKeyStoreHandler =
                    new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE, keyStoreFile.getPath(), keyStorePassword);
            PublicKey publicSignatureKey =
                    keyStoreHandlerDecorated.readPublicKeyFromKeyStore(CsvSecureConstants.ENTRY_SIGNATURE);
            PrivateKey privateSignatureKey =
                    keyStoreHandlerDecorated.readPrivateKeyFromKeyStore(CsvSecureConstants.ENTRY_SIGNATURE);
            if (publicSignatureKey == null || privateSignatureKey == null) {
                throw new IllegalArgumentException(format(
                        "No '%s' signing key found in the provided keystore: %s. This key must be provided.",
                        CsvSecureConstants.ENTRY_SIGNATURE, keyStoreHandlerDecorated.getLocation()));
            }
            this.secureStorage = new KeyStoreSecureStorage(hmacKeyStoreHandler, publicSignatureKey,
                    privateSignatureKey);
            final CsvAuditEventHandlerConfiguration.CsvSecurity securityConfiguration = config.getSecurity();
            if (fileAlreadyInitialized) {
                // Run the CsvVerifier to check that the file was not tampered.
                CsvSecureVerifier verifier = new CsvSecureVerifier(csvFile, csvPreference, secureStorage);
                CsvSecureVerifier.VerificationResult verificationResult = verifier.verify();
                if (!verificationResult.hasPassedVerification()) {
                    throw new IOException("The CSV file was tampered: " + verificationResult.getFailureReason());
                }

                // Assert that the 2 headers are equal.
                final String[] actualHeaders = verifier.getHeaders();
                if (actualHeaders != null) {
                    if (actualHeaders.length != headers.length) {
                        throw new IOException("Resuming an existing CSV file but the headers do not match.");
                    }
                    for (int idx = 0; idx < actualHeaders.length; idx++) {
                        if (!actualHeaders[idx].equals(headers[idx])) {
                            throw new IOException("Resuming an existing CSV file but the headers do not match.");
                        }
                    }
                }

                SecretKey currentKey = secureStorage.readCurrentKey();
                if (currentKey == null) {
                    throw new IllegalStateException("We are supposed to resume but there is not entry for CurrentKey.");
                }
                this.hmacCalculator.setCurrentKey(currentKey.getEncoded());

                setLastHMAC(verifier.getLastHMAC());
                setLastSignature(verifier.getLastSignature());
                this.headerWritten = true;
            } else {
                initHmacCalculatorWithRandomData();
            }

            this.signatureInterval = securityConfiguration.getSignatureIntervalDuration();
            this.scheduler = Executors.newScheduledThreadPool(1);
            this.signatureTask = new Runnable() {
                @Override
                public void run() {
                    try {
                        writeSignature(csvWriter);
                    } catch (Exception ex) {
                        logger.error("An error occurred while writing the signature", ex);
                    }
                }
            };
        } catch (Exception e) {
            throw new RuntimeException("Error when initializing a secure CSV writer", e);
        }
    }

    @Override
    public void beforeRollingOver() {
        // Prevent deadlock in case rotation/retention is enabled.
        // Rotation will trigger pre and post rotation actions which write to the file,
        // so no concurrent write must be performed during this time.
        signatureLock.lock();
    }

    @Override
    public void afterRollingOver() {
        signatureLock.unlock();
    }

    private void initHmacCalculatorWithRandomData() throws SecureStorageException {
        this.hmacCalculator.setCurrentKey(getRandomBytes());
        // As we start to work, store the key as the initial one and the current one too
        secureStorage.writeInitialKey(hmacCalculator.getCurrentKey());
        secureStorage.writeCurrentKey(hmacCalculator.getCurrentKey());
    }

    private byte[] getRandomBytes() {
        byte[] randomBytes = new byte[32];
        this.random.nextBytes(randomBytes);
        return randomBytes;
    }

    private Writer constructWriter(File csvFile, boolean append, CsvAuditEventHandlerConfiguration config)
            throws IOException {
        TextWriter textWriter;
        if (config.getFileRotation().isRotationEnabled()) {
            rotatableWriter = new RotatableWriter(csvFile, config, append, this);
            rotatableWriter.registerRotationHooks(new SecureCsvWriterRotationHooks());
            textWriter = rotatableWriter;
        } else {
            textWriter = new TextWriter.Stream(new FileOutputStream(csvFile, append));
        }

        if (config.getBuffering().isEnabled()) {
            logger.warn("Secure CSV logging does not support buffering. Buffering config will be ignored.");
        }
        return new TextWriterAdapter(textWriter);
    }

    @Override
    public void flush() throws IOException {
        csvWriter.flush();
    }

    @Override
    public void close() throws IOException {
        flush();
        signatureLock.lock();
        try {
            forceWriteSignature(csvWriter);
        } finally {
            signatureLock.unlock();
        }
        scheduler.shutdown();
        try {
            while (!scheduler.awaitTermination(500, MILLISECONDS)) {
                logger.debug("Waiting to terminate the scheduler.");
            }
        } catch (InterruptedException ex) {
            logger.error("Unable to terminate the scheduler", ex);
            Thread.currentThread().interrupt();
        }
        csvWriter.close();
    }

    private void forceWriteSignature(Writer writer) throws IOException {
        if (scheduledSignature != null && scheduledSignature.cancel(false)) {
            // We were able to cancel it before it starts, so let's generate the signature now.
            writeSignature(writer);
        }
    }

    public void writeHeader(String... header) throws IOException {
        writeHeader(csvWriter, header);
    }

    public void writeHeader(Writer writer, String... header) throws IOException {
        String[] newHeader = addExtraColumns(header);
        writer.write(csvFormatter.formatHeader(newHeader));
        logger.trace("Header written to file");
        headerWritten = true;
    }

    @VisibleForTesting
    void writeSignature(Writer writer) throws IOException {
        // We have to prevent from writing another line between the signature calculation
        // and the signature's row write, as the calculation uses the lastHMAC.
        signatureLock.lock();
        try {
            lastSignature = secureStorage.sign(dataToSign(lastSignature, lastHMAC));
            logger.trace("Calculated new Signature");
            Map<String, String> values = singletonMap(HEADER_SIGNATURE, Base64.encode(lastSignature));
            writeEvent(writer, values);
            logger.trace("Signature written to file");

            // Store the current signature into the Keystore
            secureStorage.writeCurrentSignatureKey(new SecretKeySpec(lastSignature, SIGNATURE_ALGORITHM));
            logger.trace("Signature written to secureStorage");
        } catch (SecureStorageException ex) {
            logger.error(ex.getMessage(), ex);
            throw new IOException(ex);
        } finally {
            signatureLock.unlock();
            flush();
        }
    }

    /**
     * Forces rotation of the writer.
     * <p>
     * Rotation is possible only if file rotation is enabled.
     *
     * @return {@code true} if rotation was done, {@code false} otherwise.
     * @throws IOException
     *          If an error occurs
     */
    @Override
    public boolean forceRotation() throws IOException {
        return rotatableWriter != null ? rotatableWriter.forceRotation() : false;
    }

    /**
     * Write a row into the CSV files.
     * @param values The keys of the {@link Map} have to match the column's header.
     * @throws IOException
     */
    @Override
    public void writeEvent(Map<String, String> values) throws IOException {
        writeEvent(csvWriter, values);
    }

    /**
     * Write a row into the CSV files.
     * @param values The keys of the {@link Map} have to match the column's header.
     * @throws IOException
     */
    public void writeEvent(Writer writer, Map<String, String> values) throws IOException {
        signatureLock.lock();
        try {
            if (!headerWritten) {
                writeHeader(headers);
            }
            String[] extendedHeaders = addExtraColumns(headers);

            Map<String, String> extendedValues = new HashMap<>(values);
            if (!values.containsKey(CsvSecureConstants.HEADER_SIGNATURE)) {
                insertHMACSignature(extendedValues, headers);
            }

            writer.write(csvFormatter.formatEvent(extendedValues, extendedHeaders));
            writer.flush();
            // Store the current key
            secureStorage.writeCurrentKey(hmacCalculator.getCurrentKey());

            // Schedule a signature task only if needed.
            if (!values.containsKey(HEADER_SIGNATURE)
                    && (scheduledSignature == null || scheduledSignature.isDone())) {
                logger.trace("Triggering a new signature task to be executed in {}", signatureInterval);
                try {
                    scheduledSignature = scheduler.schedule(signatureTask, signatureInterval.getValue(),
                            signatureInterval.getUnit());
                } catch (RejectedExecutionException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        } catch (SecureStorageException ex) {
            throw new IOException(ex);
        } finally {
            signatureLock.unlock();
        }
    }

    private void insertHMACSignature(Map<String, String> values, String[] nameMapping) throws IOException {
        try {
            lastHMAC = hmacCalculator.calculate(dataToSign(logger, values, nameMapping));
            values.put(CsvSecureConstants.HEADER_HMAC, lastHMAC);
        } catch (SignatureException ex) {
            logger.error(ex.getMessage(), ex);
            throw new IOException(ex);
        }
    }

    private String[] addExtraColumns(String... header) {
        String[] newHeader = new String[header.length + 2];
        System.arraycopy(header, 0, newHeader, 0, header.length);
        newHeader[header.length] = HEADER_HMAC;
        newHeader[header.length + 1] = HEADER_SIGNATURE;
        return newHeader;
    }

    private void setLastHMAC(String lastHMac) {
        this.lastHMAC = lastHMac;
    }

    private void setLastSignature(byte[] lastSignature) {
        this.lastSignature = lastSignature;
    }

    private void writeLastSignature(Writer writer) throws IOException {
        // We have to prevent from writing another line between the signature calculation
        // and the signature's row write, as the calculation uses the lastHMAC.
        signatureLock.lock();
        try {
            Map<String, String> values = singletonMap(HEADER_SIGNATURE, Base64.encode(lastSignature));
            writeEvent(writer, values);
            logger.trace("Signature from previous file written to new file");
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
            throw new IOException(ex);
        } finally {
            signatureLock.unlock();
        }
    }

    private class SecureCsvWriterRotationHooks implements RotationHooks {

        @Override
        public void preRotationAction(RotationContext context) throws IOException {
            // ensure the final signature is written
            forceWriteSignature(context.getWriter());
        }

        @Override
        public void postRotationAction(RotationContext context) throws IOException {
            // Rename the keystore and create a new one.
            String currentName = keyStoreFile.getName();
            String nextName = currentName.replaceFirst(context.getInitialFile().getName(),
                    context.getNextFile().getName());
            final File nextFile = new File(keyStoreFile.getParent(), nextName);
            logger.trace("Renaming keystore file {} to {}", currentName, nextName);
            boolean renamed = keyStoreFile.renameTo(nextFile);
            if (!renamed) {
                logger.error("Unable to rename {} to {}", keyStoreFile.getAbsolutePath(), nextFile.getAbsolutePath());
            }
            try {
                secureStorage.setKeyStoreHandler(new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE,
                        keyStoreFile.getPath(), keyStorePassword));
                logger.trace("Updated secureStorage to reference new keyStoreFile");
                initHmacCalculatorWithRandomData();
            } catch (Exception ex) {
                throw new IOException(ex);
            }

            Writer writer = context.getWriter();
            writeHeader(writer, headers);
            // ensure the signature chaining along the files
            writeLastSignature(writer);
            // In case of low traffic we still want the headers to be written into the file
            writer.flush();
        }
    }
}
