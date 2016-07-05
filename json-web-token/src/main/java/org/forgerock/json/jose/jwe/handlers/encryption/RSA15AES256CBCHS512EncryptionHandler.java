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

package org.forgerock.json.jose.jwe.handlers.encryption;

import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jws.SigningManager;

/**
 * An implementation of an EncryptionHandler that provides encryption and decryption methods using the JweAlgorithm
 * RSAES_PCKS1_V1_5 and EncryptionMethod A256CBC_HS512.
 *
 * @since 2.5.0
 * @deprecated Use {@link RSAEncryptionHandler} and {@link AESCBCHMACSHA2ContentEncryptionHandler} instead.
 */
@Deprecated
public class RSA15AES256CBCHS512EncryptionHandler extends AbstractRSAESPkcs1V15AesCbcHmacEncryptionHandler {

    /**
     * Constructs a new RSA15AES256CBCHS512EncryptionHandler.
     *
     * @param signingManager A {@code SigningManager} instance.
     */
    public RSA15AES256CBCHS512EncryptionHandler(final SigningManager signingManager) {
        super(signingManager, EncryptionMethod.A256CBC_HS512);
    }
}
