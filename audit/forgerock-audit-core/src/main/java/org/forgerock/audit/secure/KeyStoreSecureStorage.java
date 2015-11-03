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
package org.forgerock.audit.secure;

import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;

import javax.crypto.SecretKey;

import org.forgerock.util.annotations.VisibleForTesting;

/**
 * Implementation of a secure storage using a keystore.
 */
public class KeyStoreSecureStorage implements SecureStorage {

    /** The initial key used to calculate the HEADER_HMAC. */
    public static final String ENTRY_INITIAL_KEY = "InitialKey";

    /** The alias to lookup the private/public signature key into the keystore. */
    public static final String ENTRY_SIGNATURE = "Signature";

    /** The last signature inserted into the file. */
    public static final String ENTRY_CURRENT_SIGNATURE = "CurrentSignature";

    /** The current key used to calculate the HEADER_HMAC. */
    public static final String ENTRY_CURRENT_KEY = "CurrentKey";

    public static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    public static final String HMAC_ALGORITHM = "HmacSHA256";
    public static final String JCEKS_KEYSTORE_TYPE = "JCEKS";

    private final KeyStoreHandler keyStoreHandler;
    private final Signature verifier;
    private final Signature signer;
    private final boolean verifyOnly;

    /**
     * Creates the storage with a keystore handler.
     *
     * @param keyStoreHandler
     *          Handler of a keystore.
     */
    public KeyStoreSecureStorage(KeyStoreHandler keyStoreHandler) {
        this(keyStoreHandler, false);
    }

    /**
     * Creates the storage with a keystore handler and an option to verify only.
     *
     * @param keyStoreHandler
     *            Handler of a keystore.
     * @param verifyOnly
     *            Indicates if storage should be used for verify operation only. In that case, no signer will be
     *            initialised with the signature private key.
     */
    public KeyStoreSecureStorage(KeyStoreHandler keyStoreHandler, boolean verifyOnly) {
        this.keyStoreHandler = keyStoreHandler;
        this.verifyOnly = verifyOnly;

        if (verifyOnly) {
            signer = null;
        }
        else {
            try {
                signer = Signature.getInstance(KeyStoreSecureStorage.SIGNATURE_ALGORITHM);
                signer.initSign(readSignaturePrivateKey());
            } catch (SecureStorageException | InvalidKeyException | NoSuchAlgorithmException e) {
                throw new IllegalArgumentException(e);
            }
        }

        try {
            verifier = Signature.getInstance(KeyStoreSecureStorage.SIGNATURE_ALGORITHM);
            PublicKey publicKey = readSignaturePublicKey();
            if (publicKey == null) {
                throw new IllegalStateException("Expected to find a public key within the entry named "
                        + KeyStoreSecureStorage.ENTRY_SIGNATURE + " in the secure storage");
            }
            verifier.initVerify(publicKey);
        } catch (InvalidKeyException | NoSuchAlgorithmException | SecureStorageException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String getPassword() {
        return keyStoreHandler.getPassword();
    }

    @Override
    public PublicKey readSignaturePublicKey() throws SecureStorageException {
        return readPublicKeyFromKeyStore(keyStoreHandler, KeyStoreSecureStorage.ENTRY_SIGNATURE);
    }

    @Override
    public PrivateKey readSignaturePrivateKey() throws SecureStorageException {
        return readPrivateKeyFromKeyStore(keyStoreHandler, KeyStoreSecureStorage.ENTRY_SIGNATURE, getPassword());
    }

    @Override
    public SecretKey readCurrentKey() throws SecureStorageException {
        return readSecretKeyFromKeyStore(keyStoreHandler, KeyStoreSecureStorage.ENTRY_CURRENT_KEY, getPassword());
    }

    @Override
    public SecretKey readInitialKey() throws SecureStorageException {
        return readSecretKeyFromKeyStore(keyStoreHandler, KeyStoreSecureStorage.ENTRY_INITIAL_KEY, getPassword());
    }

    @Override
    public void writeCurrentSignatureKey(SecretKey key) throws SecureStorageException {
        writeToKeyStore(keyStoreHandler, key, KeyStoreSecureStorage.ENTRY_CURRENT_SIGNATURE, keyStoreHandler.getPassword());
    }

    @Override
    public void writeCurrentKey(SecretKey key) throws SecureStorageException {
        writeToKeyStore(keyStoreHandler, key, KeyStoreSecureStorage.ENTRY_CURRENT_KEY, keyStoreHandler.getPassword());
    }

    @Override
    public byte[] sign(byte[] signedData) throws SecureStorageException {
        if (verifyOnly) {
            throw new SecureStorageException("Signing is not enabled, verify mode only");
        }
        try {
            signer.update(signedData);
            return signer.sign();
        } catch (SignatureException e) {
            throw new SecureStorageException(e);
        }
    }

    @Override
    public boolean verify(byte[] signedData, byte[] signature) throws SecureStorageException {
        try {
            verifier.update(signedData);
            return verifier.verify(signature);
        } catch (SignatureException e) {
            throw new SecureStorageException(e);
        }
    }

    /**
     * Writes to the secret storage.
     *
     * @param keyStoreHandler
     *            The key store containing the key.
     * @param secretKey
     *            The data to be written to the secret storage
     * @param alias
     *            The kind of cryptoMaterial, whether it is a signature or a key
     * @param password
     *            The password to read the key
     * @throws SecureStorageException
     *             if it fails to write secret data from secret store
     */
     @VisibleForTesting
     public void writeToKeyStore(KeyStoreHandler keyStoreHandler, SecretKey secretKey, String alias, String password)
            throws SecureStorageException {
        // Note that it need JCEKS to support secret keys.
        try {
            KeyStore store = keyStoreHandler.getStore();
            if (store.containsAlias(alias)) {
                store.deleteEntry(alias);
            }
            KeyStore.SecretKeyEntry secKeyEntry = new KeyStore.SecretKeyEntry(secretKey);
            KeyStore.ProtectionParameter params = new KeyStore.PasswordProtection(password.toCharArray());
            store.setEntry(alias, secKeyEntry, params);
        } catch (KeyStoreException ex) {
            throw new SecureStorageException(ex);
        }
    }

    private PublicKey readPublicKeyFromKeyStore(KeyStoreHandler keyStoreHandler, String alias)
            throws SecureStorageException {
        try {
            KeyStore store = keyStoreHandler.getStore();
            Certificate certificate = store.getCertificate(alias);
            return certificate.getPublicKey();
        } catch (KeyStoreException ex) {
            throw new SecureStorageException("Error when reading public key: " + alias, ex);
        }
    }

    private PrivateKey readPrivateKeyFromKeyStore(KeyStoreHandler keyStoreHandler, String alias, String password)
            throws SecureStorageException {
        try {
            KeyStore store = keyStoreHandler.getStore();
            KeyStore.ProtectionParameter params = password != null ?
                    new KeyStore.PasswordProtection(password.toCharArray()) : null;
            KeyStore.PrivateKeyEntry keyentry = (KeyStore.PrivateKeyEntry) store.getEntry(alias, params);
            return keyentry != null ? keyentry.getPrivateKey() : null;
        } catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException ex) {
            throw new SecureStorageException(ex);
        }
    }

    private SecretKey readSecretKeyFromKeyStore(KeyStoreHandler keyStoreHandler, String alias, String password)
            throws SecureStorageException {
        try {
            KeyStore store = keyStoreHandler.getStore();
            KeyStore.ProtectionParameter params = new KeyStore.PasswordProtection(password.toCharArray());
            KeyStore.SecretKeyEntry keyentry = (KeyStore.SecretKeyEntry) store.getEntry(alias, params);
            return keyentry != null ? keyentry.getSecretKey() : null;
        } catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException e) {
            throw new SecureStorageException(e);
        }
    }
}
