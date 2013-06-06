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

import org.forgerock.json.jose.jwe.handlers.EncryptionHandler;
import org.forgerock.json.jose.jwe.handlers.RSA1_5_AES128CBC_HS256EncryptionHandler;
import org.forgerock.json.jose.jws.SigningManager;

public class EncryptionManager {

    public EncryptionHandler getEncryptionHandler(JweHeader header) {

        switch ((JweAlgorithm) header.getAlgorithm()) {
            case RSAES_PKCS1_V1_5: {
                switch (header.getEncryptionMethod()) {
                    case A128CBC_HS256: {
                        return new RSA1_5_AES128CBC_HS256EncryptionHandler(new SigningManager());
                    }
                    case A256CBC_HS512: {
                        throw new UnsupportedOperationException("A256CBC_HS512 not yet supported");
                    }
                    default: {
                        //TODO
                        throw new RuntimeException("Blah blah");
                    }
                }
            }
            default: {
                //TODO
                throw new RuntimeException("Blah blah");
            }
        }
    }
}
