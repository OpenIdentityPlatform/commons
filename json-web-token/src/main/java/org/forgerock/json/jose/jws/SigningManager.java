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

package org.forgerock.json.jose.jws;

import org.forgerock.json.jose.utils.Utils;
import org.forgerock.util.SignatureUtil;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

public class SigningManager {

    private final SignatureUtil signatureUtil = SignatureUtil.getInstance();

    public byte[] sign(JwsAlgorithm algorithm, PrivateKey privateKey, String data) {

        byte[] signature;

        switch (algorithm.getAlgorithmType()) {
            case NONE: {
                signature = "".getBytes(Utils.CHARSET);
                break;
            }
            case HMAC: {
                signature = signWithHMAC(algorithm.getAlgorithm(), privateKey, data.getBytes(Utils.CHARSET));
                break;
            }
            case RSA: {
                signature = signWithRSA(algorithm.getAlgorithm(), privateKey, data);
                break;
            }
            default: {
                //TODO exception
                throw new RuntimeException("Blah blah");
            }
        }

        return signature;
    }

    public byte[] signWithHMAC(String algorithm, Key key, byte[] data) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            byte[] secretByte = key.getEncoded();
            SecretKey secretKey = new SecretKeySpec(secretByte, algorithm.toUpperCase());
            mac.init(secretKey);
            return mac.doFinal(data);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new RuntimeException(e);
        }
    }

    public byte[] signWithRSA(String algorithm, PrivateKey key, String data) {
        try {
            return signatureUtil.sign(key, algorithm, data);
        } catch (SignatureException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new RuntimeException(e);
        }
    }

    public boolean verify(JwsAlgorithm algorithm, PrivateKey privateKey, byte[] data, byte[] signature) {

        boolean verified;

        switch (algorithm.getAlgorithmType()) {
            case NONE: {

                if (signature.length == 0) {
                    verified = true;
                } else {
                    verified = false;
                }
                break;
            }
            case HMAC: {
                verified = verifyWithHmac(algorithm.getAlgorithm(), privateKey, data, signature);
                break;
            }
            case RSA: {
                verified = verifyWithRSA(algorithm.getAlgorithm(), null, data, signature);  //TODO get cert/key
                break;
            }
            default: {
                //TODO exception
                throw new RuntimeException("Blah blah");
            }
        }

        return verified;
    }

    private boolean verifyWithHmac(String algorithm, PrivateKey privateKey, byte[] data, byte[] signature) {
        byte[] signed = signWithHMAC(algorithm, privateKey, data);
        return Arrays.equals(signed, signature);
    }

    private boolean verifyWithRSA(String algorithm, X509Certificate certificate, byte[] data, byte[] signature) {
        try {
            return signatureUtil.verify(certificate, algorithm, new String(data, Utils.CHARSET), signature);  //TODO
        } catch (SignatureException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new RuntimeException(e);
        }
    }
}
