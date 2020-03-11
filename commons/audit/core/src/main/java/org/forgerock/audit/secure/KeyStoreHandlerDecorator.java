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

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;

import javax.crypto.SecretKey;

/**
 * Decorate a {@link KeyStoreHandler} in order to add some commons utility methods to read or write keystore's entries.
 */
public class KeyStoreHandlerDecorator implements KeyStoreHandler {

    private final KeyStoreHandler delegate;

    /**
     * Constructs a new {@literal KeyStoreHandlerDecorator}.
     * @param delegate the {@literal KeyStoreHandler} to decorate.
     */
    public KeyStoreHandlerDecorator(KeyStoreHandler delegate) {
        this.delegate = delegate;
    }

    /**
     * Writes to the secret storage using the same password than the {@literal KeyStoreHandler}.
     *
     * @param secretKey
     *            The data to be written to the secret storage
     * @param alias
     *            The kind of cryptoMaterial, whether it is a signature or a key
     * @throws SecureStorageException
     *             if it fails to write secret data from secret store
     */
    public void writeToKeyStore(SecretKey secretKey, String alias) throws SecureStorageException {
        writeToKeyStore(secretKey, alias, getPassword());
    }

    /**
     * Writes to the secret storage.
     *
     * @param secretKey
     *            The data to be written to the secret storage
     * @param alias
     *            The kind of cryptoMaterial, whether it is a signature or a key
     * @param password
     *            The password to read the key
     * @throws SecureStorageException
     *             if it fails to write secret data from secret store
     */
    public void writeToKeyStore(SecretKey secretKey, String alias, String password) throws SecureStorageException {
        // Note that it need JCEKS to support secret keys.
        try {
            KeyStore store = getStore();
            if (store.containsAlias(alias)) {
                store.deleteEntry(alias);
            }
            KeyStore.SecretKeyEntry secKeyEntry = new KeyStore.SecretKeyEntry(secretKey);
            KeyStore.ProtectionParameter params = new KeyStore.PasswordProtection(password.toCharArray());
            store.setEntry(alias, secKeyEntry, params);
        } catch (Exception ex) {
            throw new SecureStorageException(ex);
        }
    }

    /**
     * Get the public key with the given alias.
     * @param alias The alias.
     * @return The key.
     * @throws SecureStorageException If the key could not be read.
     */
    public PublicKey readPublicKeyFromKeyStore(String alias) throws SecureStorageException {
        try {
            KeyStore store = getStore();
            Certificate certificate = store.getCertificate(alias);
            return certificate.getPublicKey();
        } catch (KeyStoreException ex) {
            throw new SecureStorageException("Error when reading public key: " + alias, ex);
        }
    }

    /**
     * Get a private key for the alias using the default password from {@link #getPassword()}.
     * @param alias The alias.
     * @return The key.
     * @throws SecureStorageException If the key could not be read.
     */
    public PrivateKey readPrivateKeyFromKeyStore(String alias) throws SecureStorageException {
        return readPrivateKeyFromKeyStore(alias, getPassword());
    }

    /**
     * Get the private key with the given alias.
     * @param alias The alias.
     * @param password The password to use to access the keystore.
     * @return The key.
     * @throws SecureStorageException If the key could not be read.
     */
    public PrivateKey readPrivateKeyFromKeyStore(String alias, String password) throws SecureStorageException {
        try {
            KeyStore store = getStore();
            KeyStore.ProtectionParameter params = password != null
                    ? new KeyStore.PasswordProtection(password.toCharArray())
                    : null;
            KeyStore.PrivateKeyEntry keyentry = (KeyStore.PrivateKeyEntry) store.getEntry(alias, params);
            return keyentry != null ? keyentry.getPrivateKey() : null;
        } catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException ex) {
            throw new SecureStorageException(ex);
        }
    }

    /**
     * Get the secret key with the given alias using the default password from {@link #getPassword()}.
     * @param alias The alias.
     * @return The key.
     * @throws SecureStorageException If the key could not be read.
     */
    public SecretKey readSecretKeyFromKeyStore(String alias) throws SecureStorageException {
        return readSecretKeyFromKeyStore(alias, getPassword());
    }

    /**
     * Get the secret key with the given alias.
     * @param alias The alias.
     * @param password The password to use to access the keystore.
     * @return The key.
     * @throws SecureStorageException If the key could not be read.
     */
    public SecretKey readSecretKeyFromKeyStore(String alias, String password) throws SecureStorageException {
        try {
            KeyStore store = getStore();
            KeyStore.ProtectionParameter params = new KeyStore.PasswordProtection(password.toCharArray());
            KeyStore.SecretKeyEntry keyentry = (KeyStore.SecretKeyEntry) store.getEntry(alias, params);
            return keyentry != null ? keyentry.getSecretKey() : null;
        } catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException e) {
            throw new SecureStorageException(e);
        }
    }

    @Override
    public KeyStore getStore() {
        return delegate.getStore();
    }

    @Override
    public void setStore(KeyStore keystore) throws Exception {
        delegate.setStore(keystore);
    }

    @Override
    public String getPassword() {
        return delegate.getPassword();
    }

    @Override
    public String getLocation() {
        return delegate.getLocation();
    }

    @Override
    public String getType() {
        return delegate.getType();
    }

    @Override
    public void store() throws Exception {
        delegate.store();
    }

}
