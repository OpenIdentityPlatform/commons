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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.json.jose.jwe;

/**
 * This class represents the result from the encryption process of the JWT plaintext.
 *
 * @author Phill Cunnington
 * @since 2.0.0
 */
public class JweEncryption {

    private final byte[] ciphertext;
    private final byte[] authenticationTag;

    /**
     * Constructs a new JweEncryption object with the given ciphertext and authentication tag.
     *
     * @param ciphertext The ciphertext.
     * @param authenticationTag The authentication tag.
     */
    public JweEncryption(byte[] ciphertext, byte[] authenticationTag) {
        this.ciphertext = ciphertext;
        this.authenticationTag = authenticationTag;
    }

    /**
     * Gets the ciphertext from the result of the encryption.
     *
     * @return The ciphertext.
     */
    public byte[] getCiphertext() {
        return ciphertext;
    }

    /**
     * Gets the authentication tag from the result of the encryption.
     *
     * @return The authentication tag.
     */
    public byte[] getAuthenticationTag() {
        return authenticationTag;
    }
}
