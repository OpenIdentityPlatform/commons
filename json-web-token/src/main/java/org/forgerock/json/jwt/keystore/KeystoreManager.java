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

package org.forgerock.json.jwt.keystore;

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

public class KeystoreManager {

    private KeyStore keyStore = null;
    private final String privateKeyPassword;

    public KeystoreManager(String privateKeyPassword, String keyStoreType, String keyStoreFile,
            String keyStorePassword) {
        this.privateKeyPassword = privateKeyPassword;
        loadKeyStore(keyStoreType, keyStoreFile, keyStorePassword);
    }

    private void loadKeyStore(String keyStoreType, String keyStoreFile, String keyStorePassword) {
        try {
            keyStore = KeyStore.getInstance(keyStoreType);
            if ( (keyStoreFile == null) || (keyStoreFile.isEmpty()) ) {
    //            logger.error("mapPk2Cert.JKSKeyProvider: KeyStore FileName is null, "
    //                    + "unable to establish Mapping Public Keys to Certificates!");
                return;
            }
            FileInputStream fis = new FileInputStream(keyStoreFile);
            keyStore.load(fis, keyStorePassword.toCharArray());
        } catch (KeyStoreException e) {

        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (CertificateException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public Certificate getCertificate(String certAlias)  {
        try {
            return keyStore.getCertificate(certAlias);
        } catch (KeyStoreException e) {
//            logger.error(e.getMessage());
        }
        return null;
    }

    public X509Certificate getX509Certificate (
            String certAlias) {

        if (certAlias == null || certAlias.length() == 0) {
            return null;
        }

        X509Certificate cert = null;
        try {
            cert = (X509Certificate) keyStore.getCertificate(certAlias);
        } catch (KeyStoreException e) {
//            logger.error("Unable to get cert alias:" + certAlias, e);
        }
        return cert;
    }

    public PublicKey getPublicKey(String keyAlias) {

        if (keyAlias == null || keyAlias.length() == 0) {
            return null;
        }

        PublicKey publicKey = null;
        try {
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(keyAlias);
            publicKey = cert.getPublicKey();
        } catch (KeyStoreException e) {
//            logger.error("Unable to get public key:" + keyAlias, e);
        }
        return publicKey;
    }

    public java.security.PrivateKey getPrivateKey (String certAlias) {
        java.security.PrivateKey key = null;
        try {
            key = (PrivateKey) keyStore.getKey(certAlias, privateKeyPassword.toCharArray());
        } catch (KeyStoreException e) {
//            logger.error(e.getMessage());
        } catch (NoSuchAlgorithmException e) {
//            logger.error(e.getMessage());
        } catch (UnrecoverableKeyException e) {
//            logger.error(e.getMessage());
        }
        return key;
    }
}
