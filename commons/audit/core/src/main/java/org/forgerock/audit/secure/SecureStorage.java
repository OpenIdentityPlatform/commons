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

import javax.crypto.SecretKey;

/**
 * Represents a storage for secure keys, to be used for signing files.
 */
public interface SecureStorage {

    /**
     * Writes the current signature key.
     *
     * @param key
     *          The secret key
     * @throws SecureStorageException
     *          If an errors occurs.
     */
    void writeCurrentSignatureKey(SecretKey key) throws SecureStorageException;

    /**
     * Reads the current key.
     *
     * @return the current key
     * @throws SecureStorageException
     *          If an errors occurs.
     */
    SecretKey readCurrentKey() throws SecureStorageException;

    /**
     * Writes the current key.
     *
     * @param key the current key
     * @throws SecureStorageException
     *          If an errors occurs.
     */
    void writeCurrentKey(SecretKey key) throws SecureStorageException;

    /**
     * Reads the initial key.
     *
     * @return the initial key
     * @throws SecureStorageException
     *          If an errors occurs.
     */
    SecretKey readInitialKey() throws SecureStorageException;

    /**
     * Writes the initial key.
     *
     * @param key the initial key
     * @throws SecureStorageException
     *          If an errors occurs.
     */
    void writeInitialKey(SecretKey key) throws SecureStorageException;

    /**
     * Signs the provided data.
     *
     * @param signedData
     *          The data to sign.
     * @return the signed data
     * @throws SecureStorageException If an error occured during signing process.
     */
    byte[] sign(byte[] signedData) throws SecureStorageException;

    /**
     * Verifies that signed data corresponds to signature.
     *
     * @param signedData
     *          the data to verify
     * @param signature
     *          the signature
     * @return {@code true} if data corresponds, {@code false} otherwise
     * @throws SecureStorageException If an error occured during the verification process.
     */
    boolean verify(byte[] signedData, byte[] signature) throws SecureStorageException;

    /**
     * Returns the password used to access the storage.
     *
     * @return the password
     */
    String getPassword();

}
