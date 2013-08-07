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

import org.forgerock.json.jose.exceptions.JweException;
import org.forgerock.json.jose.jwe.handlers.encryption.EncryptionHandler;
import org.forgerock.json.jose.jwe.handlers.encryption.RSA15AES128CBCHS256EncryptionHandler;
import org.forgerock.json.jose.jws.SigningManager;

/**
 * A service to get the appropriate EncryptionHandler for a specified Java Cryptographic encryption algorithm.
 * <p>
 * For details of all supported algorithms see {@link JweAlgorithm} and for all supported encryption methods see
 * {@link EncryptionMethod}
 *
 * @author Phill Cunnington
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

        switch (header.getAlgorithm()) {
        case RSAES_PKCS1_V1_5: {
            return getEncryptionHandler(header.getAlgorithm(), header.getEncryptionMethod());
        }
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

        switch (encryptionMethod) {
        case A128CBC_HS256: {
            return new RSA15AES128CBCHS256EncryptionHandler(new SigningManager());
        }
        case A256CBC_HS512: {
            throw new JweException(new UnsupportedOperationException("A256CBC_HS512 not yet supported"));
        }
        default: {
            throw new JweException("No Encryption Handler for unknown encryption method, "
                    + encryptionMethod + ", with algorithm,  " + algorithm + ".");
        }
        }
    }
}
