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

package org.forgerock.json.jose.jws;

import java.security.Key;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

import org.forgerock.json.jose.jws.handlers.ECDSASigningHandler;
import org.forgerock.json.jose.jws.handlers.HmacSigningHandler;
import org.forgerock.json.jose.jws.handlers.NOPSigningHandler;
import org.forgerock.json.jose.jws.handlers.RSASigningHandler;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.util.SignatureUtil;

/**
 * A service to get the appropriate SigningHandler for a specific Java Cryptographic signing algorithm.
 * <p>
 * For details of all supported signing algorithms see {@link JwsAlgorithm}
 *
 * @since 2.0.0
 */
public class SigningManager {

    private final SignatureUtil signatureUtil = SignatureUtil.getInstance();

    /**
     * Constructs an implementation of the SigningHandler which does not perform
     * any signing or verifying.
     *
     * @return an implementation of the SigningHandler which does not perform
     *         any signing or verifying.
     */
    public SigningHandler newNopSigningHandler() {
        return new NOPSigningHandler();
    }

    /**
     * Constructs a new HmacSigningHandler.
     *
     * @param sharedSecret
     *            The shared secret to use to sign the data.
     * @return a new HmacSigningHandler.
     */
    public SigningHandler newHmacSigningHandler(byte[] sharedSecret) {
        return new HmacSigningHandler(sharedSecret);
    }

    /**
     * Constructs a new RSASigningHandler, with a SignatureUtil instance to
     * delegate the signing and verifying calls to.
     *
     * @param key
     *            The key used to sign and verify the signature.
     * @return a new RSASigningHandler, with a SignatureUtil instance to
     *         delegate the signing and verifying calls to.
     */
    public SigningHandler newRsaSigningHandler(Key key) {
        return new RSASigningHandler(key, signatureUtil);
    }

    /**
     * Constructs a new handler for signing ES256 signatures.
     *
     * @param key the elliptic curve private key. Should use the required curve for the given signing algorithm
     *            (P-256 for ES256).
     * @return the signing handler.
     */
    public SigningHandler newEcdsaSigningHandler(ECPrivateKey key) {
        return new ECDSASigningHandler(key);
    }

    /**
     * Constructs a new handler for verifying ES256 signatures.
     * @param key the elliptic curve public key. Should use the required curve for the given signing algorithm (P-256
     *            for ES256).
     * @return the signing handler configured for verification.
     */
    public SigningHandler newEcdsaVerificationHandler(ECPublicKey key) {
        return new ECDSASigningHandler(key);
    }

}
