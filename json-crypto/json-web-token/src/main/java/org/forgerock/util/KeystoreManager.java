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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * A class that manages a Java Key Store and has methods for extracting out public/private keys and certificates.
 *
 * @author Phill Cunnington
 * @since 2.0.0
 */
public class KeystoreManager {

    private KeyStore keyStore = null;
    private final String privateKeyPassword;

    /**
     * Constructs an instance of the KeystoreManager.
     *
     * @param privateKeyPassword The private password for the keys.
     * @param keyStoreType The type of Java KeyStore.
     * @param keyStoreFile The file path to the KeyStore.
     * @param keyStorePassword The password for the KeyStore.
     */
    public KeystoreManager(String privateKeyPassword, String keyStoreType, String keyStoreFile,
            String keyStorePassword) {
        this.privateKeyPassword = privateKeyPassword;
        loadKeyStore(keyStoreType, keyStoreFile, keyStorePassword);
    }

    /**
     * Loads the KeyStore based on the given parameters.
     *
     * @param keyStoreType The type of Java KeyStore.
     * @param keyStoreFile The file path to the KeyStore.
     * @param keyStorePassword The password for the KeyStore.
     */
    private void loadKeyStore(String keyStoreType, String keyStoreFile, String keyStorePassword) {
        try {
            keyStore = KeyStore.getInstance(keyStoreType);
            if (keyStoreFile == null || keyStoreFile.isEmpty()) {
                throw new KeystoreManagerException("mapPk2Cert.JKSKeyProvider: KeyStore FileName is null, "
                        + "unable to establish Mapping Public Keys to Certificates!");
            }
            FileInputStream fis = new FileInputStream(keyStoreFile);
            keyStore.load(fis, keyStorePassword.toCharArray());
        } catch (KeyStoreException e) {
            throw new KeystoreManagerException(e);
        } catch (FileNotFoundException e) {
            throw new KeystoreManagerException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new KeystoreManagerException(e);
        } catch (CertificateException e) {
            throw new KeystoreManagerException(e);
        } catch (IOException e) {
            throw new KeystoreManagerException(e);
        }
    }

    /**
     * Gets the certificate from the KeyStore with the given alias.
     *
     * @param certAlias The Certificate Alias.
     * @return The Certificate.
     */
    public Certificate getCertificate(String certAlias)  {
        if (certAlias == null || certAlias.length() == 0) {
            return null;
        }

        try {
            return keyStore.getCertificate(certAlias);
        } catch (KeyStoreException e) {
            throw new KeystoreManagerException(e);
        }
    }

    /**
     * Gets a X509Certificate from the KeyStore with the given alias.
     *
     * @param certAlias The Certificate Alias.
     * @return The X509Certificate.
     */
    public X509Certificate getX509Certificate(String certAlias) {
        return (X509Certificate) getCertificate(certAlias);
    }

    /**
     * Gets the Public Key from the KeyStore with the given alias.
     *
     * @param keyAlias The Public Key Alias.
     * @return The Public Key.
     */
    public PublicKey getPublicKey(String keyAlias) {

        if (keyAlias == null || keyAlias.length() == 0) {
            return null;
        }

        try {
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(keyAlias);
            return cert.getPublicKey();
        } catch (KeyStoreException e) {
            throw new KeystoreManagerException(e);
        }
    }

    /**
     * Gets the Private Key from the KeyStore with the given alias.
     *
     * @param keyAlias The Private Key Alias.
     * @return The Private Key.
     */
    public PrivateKey getPrivateKey(String keyAlias) {

        if (keyAlias == null || keyAlias.length() == 0) {
            return null;
        }

        try {
            return (PrivateKey) keyStore.getKey(keyAlias, privateKeyPassword.toCharArray());
        } catch (KeyStoreException e) {
            throw new KeystoreManagerException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new KeystoreManagerException(e);
        } catch (UnrecoverableKeyException e) {
            throw new KeystoreManagerException(e);
        }
    }
}
