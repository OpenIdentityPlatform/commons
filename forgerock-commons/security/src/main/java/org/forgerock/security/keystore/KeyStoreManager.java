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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.security.keystore;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * A class that manages a Java Key Store and has methods for extracting out public/private keys and certificates.
 */
public class KeyStoreManager {

    private final KeyStore keyStore;

    /**
     * Constructs an instance of the KeyStoreManager.
     *
     * @param keyStore The managed {@link KeyStore}. The key store must already be loaded.
     */
    public KeyStoreManager(final KeyStore keyStore) {
        this.keyStore = keyStore;
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
            throw new KeystoreManagerException("Unable to get certificate: " + certAlias, e);
        }
    }

    /**
     * Gets a X509Certificate from the KeyStore with the given alias.
     *
     * @param certAlias The Certificate Alias.
     * @return The X509Certificate.
     */
    public X509Certificate getX509Certificate(String certAlias) {
        Certificate certificate = getCertificate(certAlias);
        if (certificate instanceof X509Certificate) {
            return (X509Certificate) certificate;
        }
        throw new KeystoreManagerException("Certificate not a X509 Certificate for alias: " + certAlias);
    }

    /**
     * Gets the Public Key from the KeyStore with the given alias.
     *
     * @param keyAlias The Public Key Alias.
     * @return The Public Key.
     */
    public PublicKey getPublicKey(String keyAlias) {
        if (keyAlias == null || keyAlias.isEmpty()) {
            return null;
        }

        X509Certificate cert = getX509Certificate(keyAlias);
        if (cert == null) {
            throw new KeystoreManagerException("Unable to retrieve certificate for alias: " + keyAlias);
        }
        return cert.getPublicKey();
    }

    /**
     * Gets the Private Key from the KeyStore with the given alias.
     *
     * @param keyAlias The Private Key Alias.
     * @param privateKeyPassword The private key password
     * @return The Private Key.
     */
    public PrivateKey getPrivateKey(String keyAlias, String privateKeyPassword) {
        if (keyAlias == null || keyAlias.length() == 0) {
            return null;
        }

        try {
            return (PrivateKey) keyStore.getKey(keyAlias, privateKeyPassword.toCharArray());
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new KeystoreManagerException("unable to get private key:" + keyAlias, e);
        }
    }

    /**
     * Gets the managed {@link KeyStore}.
     * @return The managed {@link KeyStore}.
     */
    public KeyStore getKeyStore() {
        return keyStore;
    }
}
