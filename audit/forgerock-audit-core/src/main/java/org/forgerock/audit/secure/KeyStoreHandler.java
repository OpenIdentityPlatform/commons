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

import java.security.KeyStore;

/**
 * Handles the access to a KeyStore.
 */
public interface KeyStoreHandler {

    /**
     * Get the keystore.
     *
     * @return the keystore.
     */
    public KeyStore getStore();

    /**
     * Sets the keystore.
     *
     * @param keystore
     *          The keystore to use
     * @throws Exception
     *          If an error occurs
     */
    public void setStore(KeyStore keystore) throws Exception;

    /**
     * Returns the password.
     *
     * @return the password used to access the keystore
     */
    public String getPassword();

    /**
     * Returns the path to the keystore.
     *
     * @return the path
     */
    public String getLocation();

    /**
     * Returns the type of the keystore.
     *
     * @return the keystore type
     */
    public String getType();

    /**
     * Saves the keystore.
     *
     * @throws Exception
     *          If an error occurs.
     */
    public void store() throws Exception;
}