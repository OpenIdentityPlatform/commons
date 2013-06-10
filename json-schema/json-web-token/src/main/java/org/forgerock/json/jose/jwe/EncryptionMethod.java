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

package org.forgerock.json.jose.jwe;

public enum EncryptionMethod {

    A128CBC_HS256("AES_128_CBC_HMAC_SHA_256", "AES/CBC/PKCS5Padding", "HMACSHA256", "AES", 16, 256),
    A256CBC_HS512("AES_256_CBC_HMAC_SHA_512", "AES/CBC/PKCS5Padding", "HMACSHA512", "AES", 32, 512);

    private final String name;
    private final String transformation;
    private final String macAlgorithm;
    private final String encryptionAlgorithm;
    private final int keyOffset;
    private final int keySize;

    private EncryptionMethod(String name, String transformation, String macAlgorithm, String encryptionAlgorithm, int keyOffset, int keySize) {
        this.name = name;
        this.transformation = transformation;
        this.macAlgorithm = macAlgorithm;
        this.encryptionAlgorithm = encryptionAlgorithm;
        this.keyOffset = keyOffset;
        this.keySize = keySize;
    }

    public String getName() {
        return name;
    }

    public String getTransformation() {
        return transformation;
    }

    public String getMacAlgorithm() {
        return macAlgorithm;
    }

    public String getEncryptionAlgorithm() {
        return encryptionAlgorithm;
    }

    public int getKeyOffset() {
        return keyOffset;
    }

    public int getKeySize() {
        return keySize;
    }

    @Override
    public String toString() {
        return '"' + super.toString() + '"';
    }
}
