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
package org.forgerock.audit.secure;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

import javax.crypto.SecretKey;


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

    /** The algorithm to use for signing and verifying. */
    public static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    /** The HMAC algorithm to use. */
    public static final String HMAC_ALGORITHM = "HmacSHA256";
    /** The name of the Java Cryptography Extension KeyStore (JCEKS) type. */
    public static final String JCEKS_KEYSTORE_TYPE = "JCEKS";

    private KeyStoreHandlerDecorator keyStoreHandler;
    private Signature verifier;
    private Signature signer;

    /**
     * Creates the storage with a keystore handler, initialized to verify only.
     *
     * @param keyStoreHandler
     *            Handler of a keystore.
     * @param privateKey
     *            The private key used to initialize the signer
     */
    public KeyStoreSecureStorage(KeyStoreHandler keyStoreHandler, PrivateKey privateKey) {
        this(keyStoreHandler, null, privateKey);
    }

    /**
     * Creates the storage with a keystore handler, initialized to verify only.
     *
     * @param keyStoreHandler
     *            Handler of a keystore.
     * @param publicKey
     *            The public key used to initialize the verifier
     */
    public KeyStoreSecureStorage(KeyStoreHandler keyStoreHandler, PublicKey publicKey) {
        this(keyStoreHandler, publicKey, null);
    }

    /**
     * Creates the storage with a keystore handler, initialized to verify only.
     *
     * @param keyStoreHandler
     *            Handler of a keystore.
     * @param publicKey
     *            The public key used to initialize the verifier
     * @param privateKey
     *            The private key used to initialize the signer
     */
    public KeyStoreSecureStorage(KeyStoreHandler keyStoreHandler, PublicKey publicKey, PrivateKey privateKey) {
        setKeyStoreHandler(keyStoreHandler);

        if (privateKey != null) {
            try {
                signer = Signature.getInstance(KeyStoreSecureStorage.SIGNATURE_ALGORITHM);
                signer.initSign(privateKey);
            } catch (InvalidKeyException | NoSuchAlgorithmException e) {
                throw new IllegalArgumentException(e);
            }
        }

        if (publicKey != null) {
            try {
                verifier = Signature.getInstance(KeyStoreSecureStorage.SIGNATURE_ALGORITHM);
                verifier.initVerify(publicKey);
            } catch (InvalidKeyException | NoSuchAlgorithmException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    /**
     * Set the key store handler.
     * @param keyStoreHandler The handler.
     */
    public void setKeyStoreHandler(KeyStoreHandler keyStoreHandler) {
        this.keyStoreHandler = new KeyStoreHandlerDecorator(keyStoreHandler);
    }

    @Override
    public String getPassword() {
        return keyStoreHandler.getPassword();
    }

    @Override
    public SecretKey readCurrentKey() throws SecureStorageException {
        return keyStoreHandler.readSecretKeyFromKeyStore(KeyStoreSecureStorage.ENTRY_CURRENT_KEY);
    }

    @Override
    public SecretKey readInitialKey() throws SecureStorageException {
        return keyStoreHandler.readSecretKeyFromKeyStore(KeyStoreSecureStorage.ENTRY_INITIAL_KEY);
    }

    @Override
    public void writeCurrentSignatureKey(SecretKey key) throws SecureStorageException {
        keyStoreHandler.writeToKeyStore(key, KeyStoreSecureStorage.ENTRY_CURRENT_SIGNATURE,
                keyStoreHandler.getPassword());
        try {
            keyStoreHandler.store();
        } catch (Exception ex) {
            throw new SecureStorageException(ex);
        }
    }

    @Override
    public void writeCurrentKey(SecretKey key) throws SecureStorageException {
        writeKey(key, KeyStoreSecureStorage.ENTRY_CURRENT_KEY);
    }


    @Override
    public void writeInitialKey(SecretKey key) throws SecureStorageException {
        writeKey(key, KeyStoreSecureStorage.ENTRY_INITIAL_KEY);
    }

    private void writeKey(SecretKey key, String alias) throws SecureStorageException {
        keyStoreHandler.writeToKeyStore(key, alias, keyStoreHandler.getPassword());
        try {
            keyStoreHandler.store();
        } catch (Exception ex) {
            throw new SecureStorageException(ex);
        }
    }

    @Override
    public byte[] sign(byte[] signedData) throws SecureStorageException {
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
}
