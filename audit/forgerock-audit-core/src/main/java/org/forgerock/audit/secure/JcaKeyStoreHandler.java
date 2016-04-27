/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.audit.secure;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * Default implementation of a Keystore handler.
 */
public class JcaKeyStoreHandler implements KeyStoreHandler {

    private final String location;
    private final String password;
    private final String type;
    private KeyStore store;

    /**
     * Creates a new keystore handler.
     *
     * @param type
     *          The type of keystore
     * @param location
     *          The path of the keystore
     * @param password
     *          The password to access the keystore
     * @throws Exception
     *          If an error occurs while initialising the keystore
     */
    public JcaKeyStoreHandler(String type, String location, String password) throws Exception {
        this.location = location;
        this.password = password;
        this.type = type;
        init();
    }

    private void init() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        File ksFile = new File(location);
        store = KeyStore.getInstance(type);
        if (ksFile.exists()) {
            try (InputStream in = new BufferedInputStream(new FileInputStream(location))) {
                store.load(in, password.toCharArray());
            }
        } else {
            // Create an empty one
            store.load(null, password.toCharArray());
        }
    }

    @Override
    public KeyStore getStore() {
        return store;
    }

    @Override
    public void setStore(KeyStore keystore) throws Exception {
        store = keystore;
        store();
    }

    @Override
    public void store() throws Exception {
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(location))) {
            store.store(out, password.toCharArray());
        }
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getLocation() {
        return location;
    }

    @Override
    public String getType() {
        return type;
    }
}