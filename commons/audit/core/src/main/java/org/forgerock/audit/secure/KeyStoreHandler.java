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