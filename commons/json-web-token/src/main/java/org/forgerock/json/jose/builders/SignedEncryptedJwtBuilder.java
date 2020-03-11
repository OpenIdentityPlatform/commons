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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.json.jose.builders;

import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.handlers.SigningHandler;

/**
 * Builds encrypted and then signed nested JWTs.
 *
 * @deprecated Use {@link EncryptedThenSignedJwtBuilder} instead.
 */
@Deprecated
public class SignedEncryptedJwtBuilder extends EncryptedThenSignedJwtBuilder {

    /**
     * Constructs a new SignedEncryptedJwtBuilder that will use the given EncryptedJwtBuilder, to build the nested
     * Encrypted JWT, and the private key and JwsAlgorithm to sign the outer JWT.
     *
     * @param encryptedJwtBuilder The EncryptedJwtBuilder instance.
     * @param signingHandler The SigningHandler instance used to sign the JWS.
     * @param jwsAlgorithm The JwsAlgorithm to use when signing the JWT.
     */
    public SignedEncryptedJwtBuilder(final EncryptedJwtBuilder encryptedJwtBuilder,
            final SigningHandler signingHandler,
            final JwsAlgorithm jwsAlgorithm) {
        super(encryptedJwtBuilder, signingHandler, jwsAlgorithm);
    }
}
