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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.json.jose.jws.handlers;

import org.forgerock.json.jose.exceptions.JwsSigningException;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.utils.Utils;
import org.forgerock.util.Reject;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * An implementation of the SigningHandler which can sign and verify using algorithms from the HMAC family.
 *
 * @since 2.0.0
 */
public class HmacSigningHandler implements SigningHandler {

    private final byte[] sharedSecret;

    /**
     * Constructs a new HmacSigningHandler.
     *
     * @param sharedSecret The shared secret to use to sign the data.
     */
    public HmacSigningHandler(byte[] sharedSecret) {
        Reject.ifNull(sharedSecret, "Shared secret cannot be null.");
        this.sharedSecret = sharedSecret.clone();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] sign(JwsAlgorithm algorithm, String data) {
        return signWithHMAC(algorithm.getAlgorithm(), sharedSecret, data.getBytes(Utils.CHARSET));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] sign(final JwsAlgorithm algorithm, final byte[] data) {
        return signWithHMAC(algorithm.getAlgorithm(), sharedSecret, data);
    }

    /**
     * Performs the creation of the MAC for the data using the given Java Cryptographic algorithm.
     *
     * @param algorithm The Java Cryptographic algorithm.
     * @param sharedSecret The shared secret to use to sign the data.
     * @param data The data to sign.
     * @return A byte array of the signature.
     */
    private byte[] signWithHMAC(String algorithm, byte[] sharedSecret, byte[] data) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            SecretKey secretKey = new SecretKeySpec(sharedSecret, algorithm.toUpperCase());
            mac.init(secretKey);
            return mac.doFinal(data);
        } catch (NoSuchAlgorithmException e) {
            throw new JwsSigningException("Unsupported Signing Algorithm, " + algorithm, e);
        } catch (InvalidKeyException e) {
            throw new JwsSigningException(e);
        }
    }

    /**
     * Verifies that the given signature is valid for the given data.
     * <p>
     * Uses the Java Cryptographic algorithm defined by the JwsAlgorithm and private key to create a new signature
     * of the data to compare against the given signature to see if they are identical.
     *
     * This implementation avoids timing attacks by enforcing checking of each element of the
     * array against one another. We do not rely on Arrays.equal or other methods which
     * may return early upon discovering a mistake.
     *
     * @param algorithm The JwsAlgorithm defining the JavaCryptographic algorithm.
     * @param data The data that was signed.
     * @param signature The signature of the data.
     * @return <code>true</code> if the signature is a valid signature of the data.
     */
    @Override
    public boolean verify(JwsAlgorithm algorithm, byte[] data, byte[] signature) {
        byte[] signed = signWithHMAC(algorithm.getAlgorithm(), sharedSecret, data);
        return MessageDigest.isEqual(signed, signature);
    }
}
