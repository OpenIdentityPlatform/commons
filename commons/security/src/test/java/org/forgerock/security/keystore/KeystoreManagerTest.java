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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.security.keystore;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class KeystoreManagerTest {
    private static final String KEY_STORE_PASSWORD = "Passw0rd1";
    private static final String KEY_ALIAS = "key";

    @DataProvider
    private Object[][] fileBasedKeyStores() {
        return new Object[][] {
                {KeyStoreType.JKS, "/keystore.jks"},
                {KeyStoreType.JCEKS, "/keystore.jceks"},
                {KeyStoreType.PKCS12, "/keystore.pfx"}
        };
    }

    @Test(dataProvider = "fileBasedKeyStores")
    public void shouldInitialize(final KeyStoreType keyStoreType, final String keyStoreFileName)
            throws Exception {
        // given
        final KeyStore keyStore = loadKeyStore(keyStoreType, keyStoreFileName);
        final KeyStoreManager keyStoreManager = new KeyStoreManager(keyStore);

        // when

        // then
        assertThat(keyStoreManager.getKeyStore()).isEqualTo(keyStore);
    }

    @Test(dataProvider = "fileBasedKeyStores")
    public void shouldGetPublicKey(final KeyStoreType keyStoreType, final String keyStoreFileName)
            throws Exception {
        // given
        final KeyStore keyStore = loadKeyStore(keyStoreType, keyStoreFileName);
        final KeyStoreManager keyStoreManager = new KeyStoreManager(keyStore);

        // when
        final PublicKey key = keyStoreManager.getPublicKey(KEY_ALIAS);

        // then
        assertThat(key).isNotNull();
    }

    @Test(dataProvider = "fileBasedKeyStores")
    public void shouldGetPrivateKey(final KeyStoreType keyStoreType, final String keyStoreFileName)
            throws Exception {
        // given
        final KeyStore keyStore = loadKeyStore(keyStoreType, keyStoreFileName);
        final KeyStoreManager keyStoreManager = new KeyStoreManager(keyStore);

        // when
        final PrivateKey key = keyStoreManager.getPrivateKey(KEY_ALIAS, KEY_STORE_PASSWORD);

        // then
        assertThat(key).isNotNull();
    }

    @Test(dataProvider = "fileBasedKeyStores")
    public void shouldGetCertificate(final KeyStoreType keyStoreType, final String keyStoreFileName)
            throws Exception {
        // given
        final KeyStore keyStore = loadKeyStore(keyStoreType, keyStoreFileName);
        final KeyStoreManager keyStoreManager = new KeyStoreManager(keyStore);

        // when
        final Certificate certificate = keyStoreManager.getCertificate(KEY_ALIAS);

        // then
        assertThat(certificate).isNotNull();
    }

    @Test(dataProvider = "fileBasedKeyStores")
    public void shouldGetX509Certificate(final KeyStoreType keyStoreType, final String keyStoreFileName)
            throws Exception {
        // given
        final KeyStore keyStore = loadKeyStore(keyStoreType, keyStoreFileName);
        final KeyStoreManager keyStoreManager = new KeyStoreManager(keyStore);

        // when
        final X509Certificate certificate = keyStoreManager.getX509Certificate(KEY_ALIAS);

        // then
        assertThat(certificate).isNotNull();
    }

    private KeyStore loadKeyStore(final KeyStoreType keyStoreType, final String keyStoreFileName)
            throws Exception {
        final KeyStore keyStore = KeyStore.getInstance(keyStoreType.name());
        try (final InputStream keyStoreFile = getClass().getResourceAsStream(keyStoreFileName)) {
            keyStore.load(keyStoreFile, KEY_STORE_PASSWORD.toCharArray());
        }
        return keyStore;
    }
}
