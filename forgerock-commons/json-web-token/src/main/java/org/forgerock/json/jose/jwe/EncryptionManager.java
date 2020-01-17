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

package org.forgerock.json.jose.jwe;

import org.forgerock.json.jose.exceptions.JweException;
import org.forgerock.json.jose.jwe.handlers.encryption.AESKeyWrapEncryptionHandler;
import org.forgerock.json.jose.jwe.handlers.encryption.DirectEncryptionHandler;
import org.forgerock.json.jose.jwe.handlers.encryption.EncryptionHandler;
import org.forgerock.json.jose.jwe.handlers.encryption.RSAEncryptionHandler;

/**
 * A service to get the appropriate EncryptionHandler for a specified Java Cryptographic encryption algorithm.
 * <p>
 * For details of all supported algorithms see {@link JweAlgorithm} and for all supported encryption methods see
 * {@link EncryptionMethod}
 *
 * @since 2.0.0
 */
public class EncryptionManager {

    /**
     * Gets the appropriate EncryptionHandler that can perform the required encryption algorithm, as described by the
     * JweAlgorithm and EncryptionMethod in the given JweHeader.
     *
     * @param header The JweHeader containing the JweAlgorithm and EncryptionMethod to get the EncryptionHandler for.
     * @return The EncryptionHandler.
     */
    public EncryptionHandler getEncryptionHandler(JweHeader header) {

        switch (header.getAlgorithm().getAlgorithmType()) {
        case RSA:
            return getEncryptionHandler(header.getAlgorithm(), header.getEncryptionMethod());
        case DIRECT:
            return getEncryptionHandler(header.getAlgorithm(), header.getEncryptionMethod());
        case AES_KEYWRAP:
            return getEncryptionHandler(header.getAlgorithm(), header.getEncryptionMethod());
        default: {
            throw new JweException("No Encryption Handler for unknown encryption algorithm, "
                    + header.getAlgorithm() + ".");
        }
        }
    }

    /**
     * Gets the appropriate EncryptionHandler that can perform the required encryption algorithm, as described by the
     * JweAlgorithm and EncryptionMethod.
     *
     * @param algorithm The JweAlgorithm.
     * @param encryptionMethod The EncryptionMethod.
     * @return The EncryptionHandler.
     */
    private EncryptionHandler getEncryptionHandler(JweAlgorithm algorithm, EncryptionMethod encryptionMethod) {

        switch (algorithm) {
        case RSAES_PKCS1_V1_5:
        case RSA_OAEP:
        case RSA_OAEP_256:
            return new RSAEncryptionHandler(encryptionMethod, algorithm);
        case DIRECT:
            return new DirectEncryptionHandler(encryptionMethod);
        case A128KW:
        case A192KW:
        case A256KW:
            return new AESKeyWrapEncryptionHandler(encryptionMethod);
        default:
            throw new JweException("No Encryption Handler for unknown encryption method, "
                    + encryptionMethod + ", with algorithm,  " + algorithm + ".");
        }
    }
}
