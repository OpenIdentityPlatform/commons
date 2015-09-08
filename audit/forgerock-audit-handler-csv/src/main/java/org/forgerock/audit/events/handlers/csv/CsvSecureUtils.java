/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.forgerock.audit.events.handlers.csv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Map;

import javax.crypto.SecretKey;

import org.forgerock.util.encode.Base64;
import org.slf4j.Logger;

/**
 *
 * Holds the methods shared between the CsvSecure classes.
 */
public class CsvSecureUtils {

    /**
     * Writes to the secret Storage. If the data to be written is a key, then writes the older signature also. If it is
     * a signature then writes the older key also
     *
     * @param cryptoMaterial The data to be written to the secret storage
     * @param filename The file for secret storage
     * @param password The password for the file
     * @param alias The kind of cryptoMaterial, whether it is a signature or a key
     * @throws Exception if it fails to write secret data from secret store
     */
    static void writeToKeyStore(SecretKey cryptoMaterial, String alias, String filename, String password) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        // JCEKS to support secret keys.
        KeyStore store = KeyStore.getInstance("jceks");
        File file = new File(filename);
        if (file.exists()) {
            try (final FileInputStream fis = new FileInputStream(file)) {
                store.load(fis, password.toCharArray());
            }
        } else {
            store.load(null, new char[0]);
        }
        if (store.containsAlias(alias)) {
            store.deleteEntry(alias);
        }
        KeyStore.SecretKeyEntry secKeyEntry = new KeyStore.SecretKeyEntry(cryptoMaterial);
        KeyStore.ProtectionParameter params = new KeyStore.PasswordProtection(password.toCharArray());
        store.setEntry(alias, secKeyEntry, params);
        try (final FileOutputStream fos = new FileOutputStream(file)) {
            store.store(fos, password.toCharArray());
        }
    }

    /**
     *
     * @param filename the value of filename
     * @param dataType the value of dataType
     * @param password the value of password
     * @throws IOException
     */
    static PrivateKey readPrivateKeyFromKeyStore(String filename, String dataType, String password) throws IOException {
        try {
            File file = new File(filename);
            if (!file.exists()) {
                return null;
            }
            KeyStore store = KeyStore.getInstance(CsvSecureConstants.KEYSTORE_TYPE);
            try (final FileInputStream fis = new FileInputStream(file)) {
                store.load(fis, password.toCharArray());
            }
            KeyStore.ProtectionParameter params = new KeyStore.PasswordProtection(password.toCharArray());
            KeyStore.PrivateKeyEntry keyentry = (KeyStore.PrivateKeyEntry) store.getEntry(dataType, params);
            return keyentry != null ? keyentry.getPrivateKey() : null;
        } catch (NoSuchAlgorithmException | UnrecoverableEntryException | CertificateException | KeyStoreException ex) {
            throw new IOException(ex);
        }
    }

    /**
     *
     *
     * @param csvSecureVerifier the value of csvSecureVerifier
     */
    static byte[] dataToSign(byte[] lastSignature, String lastHMAC) {
        byte[] toSign;
        if (lastSignature == null) {
            // Only the last HMAC will be signed
            byte[] prevHMAC = Base64.decode(lastHMAC);
            toSign = Arrays.copyOf(prevHMAC, prevHMAC.length);
        } else {
            // Both the last HMAC and the last signature will be signed
            byte[] prevHMAC = Base64.decode(lastHMAC);
            toSign = concat(prevHMAC, lastSignature);
        }
        return toSign;
    }

    private static byte[] concat(byte[]... arrays) {
        int length;

        // Find the length of the result array
        length = 0;
        for (byte[] array : arrays) {
            length += array.length;
        }
        byte[] result = new byte[length];

        // Really concatenate all the arrays
        length = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, length, array.length);
            length += array.length;
        }

        return result;
    }

    /**
     *
     * @param logger the value of logger
     * @param values the value of values
     * @param nameMapping the value of nameMapping
     */
    static byte[] dataToSign(Logger logger, Map<String, ?> values, String... nameMapping) {
        StringBuilder tmp = new StringBuilder();
        for (String h : nameMapping) {
            final Object value = values.get(h);
            if (value != null) {
                tmp.append(value.toString());
            }
        }
        return tmp.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Returns matched secret data from from the secret Storage. At a time there are only 3 things in logger's secure
     * store file - initialkey, currentkey and current signature In the verifier secure store file there is just the
     * initial key of the logger and the currentKey
     *
     * @param filename file for secret storage
     * @param alias The kind of data to be read, whether it is a signature or a key
     * @param password password for the file
     * @return secure data that is matched with dataType
     * @throws Exception if it fails to read secret data from secret store
     */
    static SecretKey readSecretKeyFromKeyStore(String alias, String filename, String password) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableEntryException {
        File file = new File(filename);
        if (!file.exists()) {
            return null;
        }
        KeyStore store;
        try (final FileInputStream fis = new FileInputStream(file)) {
            store = KeyStore.getInstance(CsvSecureConstants.KEYSTORE_TYPE);
            store.load(fis, password.toCharArray());
        }
        KeyStore.ProtectionParameter params = new KeyStore.PasswordProtection(password.toCharArray());
        KeyStore.SecretKeyEntry keyentry = (KeyStore.SecretKeyEntry) store.getEntry(alias, params);
        if (keyentry != null) {
            return keyentry.getSecretKey();
        } else {
            return null;
        }
    }

    /**
     *
     * @param filename the value of filename
     * @param alias the value of alias
     * @param password the value of password
     * @throws IOException
     */
    static PublicKey readPublicKeyFromKeyStore(String filename, String alias, String password) throws IOException {
        try {
            File file = new File(filename);
            if (!file.exists()) {
                return null;
            }
            KeyStore store = KeyStore.getInstance(CsvSecureConstants.KEYSTORE_TYPE);
            try (final FileInputStream fis = new FileInputStream(file)) {
                store.load(fis, password.toCharArray());
            }
            Certificate certificate = store.getCertificate(alias);
            return certificate.getPublicKey();
        } catch (NoSuchAlgorithmException | CertificateException | KeyStoreException ex) {
            throw new IOException(ex);
        }
    }

    private CsvSecureUtils() {
        // Prevent from instantiating
    }


}
