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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.security.keystore;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Paths;
import java.security.KeyStore;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class KeyStoreBuilderTest {

    private static final String KEY_STORE_PASSWORD = "Passw0rd1";

    /**
     * Lists the keystore types and the key stores generated for each type.
     * The key stores were generated with the following commands:
     * <ul>
     *     <li>keytool -keystore keystore.pfx -storetype PKCS12 -genkey -alias key</li>
     *     <li>keytool -keystore keystore.jceks -storetype JCEKS -genkey -alias key</li>
     *     <li>keytool -keystore keystore.jks -storetype JKS -genkey -alias key</li>
     * </ul>
     * @return The {@link KeyStoreType} and generated key store filenames.
     */
    @DataProvider
    private Object[][] fileBasedKeyStoresWithFileName() {
        return new Object[][] {
                {KeyStoreType.JKS, "/keystore.jks"},
                {KeyStoreType.JCEKS, "/keystore.jceks"},
                {KeyStoreType.PKCS12, "/keystore.pfx"}
        };
    }

    @DataProvider
    private Object[][] fileBasedKeyStoresWithFileNameAndProviders() {
        return new Object[][] {
                {KeyStoreType.JKS, "/keystore.jks", "SUN"},
                {KeyStoreType.JCEKS, "/keystore.jceks", "SunJCE"},
                {KeyStoreType.PKCS12, "/keystore.pfx", "SunJSSE"}
        };
    }

    @Test(dataProvider = "fileBasedKeyStoresWithFileName")
    public void shouldLoadExistingKeyStore(final KeyStoreType keyStoreType, final String keyStoreFileName)
            throws Exception {
        // given
        final String absoluteFileName =
                Paths.get(getClass().getResource(keyStoreFileName).toURI()).toFile().getAbsolutePath();

        // when
        final KeyStore keyStore = new KeyStoreBuilder()
                .withKeyStoreFile(absoluteFileName)
                .withPassword(KEY_STORE_PASSWORD)
                .withKeyStoreType(keyStoreType)
                .build();

        // then
        assertThat(keyStore).isNotNull();
    }

    @Test(dataProvider = "fileBasedKeyStoresWithFileNameAndProviders")
    public void shouldLoadExistingKeyStoreWithGivenProvider(final KeyStoreType keyStoreType,
            final String keyStoreFileName, final String keyStoreProvider) throws Exception {
        // given
        final String absoluteFileName =
                Paths.get(getClass().getResource(keyStoreFileName).toURI()).toFile().getAbsolutePath();

        // when
        final KeyStore keyStore = new KeyStoreBuilder()
                .withKeyStoreFile(absoluteFileName)
                .withPassword(KEY_STORE_PASSWORD)
                .withKeyStoreType(keyStoreType)
                .withProvider(keyStoreProvider)
                .build();

        // then
        assertThat(keyStore).isNotNull();
    }

    @Test(dataProvider = "fileBasedKeyStoresWithFileName")
    public void shouldLoadExistingKeyStoreUsingFileParameter(final KeyStoreType keyStoreType,
            final String keyStoreFileName) throws Exception {
        // given
        final File keyStoreFile =
                Paths.get(getClass().getResource(keyStoreFileName).toURI()).toFile();

        // when
        final KeyStore keyStore = new KeyStoreBuilder()
                .withKeyStoreFile(keyStoreFile)
                .withPassword(KEY_STORE_PASSWORD)
                .withKeyStoreType(keyStoreType)
                .build();

        // then
        assertThat(keyStore).isNotNull();
    }

    @Test(dataProvider = "fileBasedKeyStoresWithFileNameAndProviders")
    public void shouldLoadExistingKeyStoreWithGivenProviderUsingFileParameter(final KeyStoreType keyStoreType,
            final String keyStoreFileName, final String keyStoreProvider) throws Exception {
        // given
        final File keystoreFile =
                Paths.get(getClass().getResource(keyStoreFileName).toURI()).toFile();

        // when
        final KeyStore keyStore = new KeyStoreBuilder()
                .withKeyStoreFile(keystoreFile)
                .withPassword(KEY_STORE_PASSWORD)
                .withKeyStoreType(keyStoreType)
                .withProvider(keyStoreProvider)
                .build();

        // then
        assertThat(keyStore).isNotNull();
    }
}
