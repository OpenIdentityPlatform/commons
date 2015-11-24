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
* Copyright 2015 ForgeRock AS.
*/

package org.forgerock.audit.handlers.csv;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.forgerock.util.encode.Base64;

/**
 * This class aims to compute the HMAC for the given data.
 *
 */
class HmacCalculator {

    private SecretKey currentKey;
    private final MessageDigest messageDigest;
    private final Mac mac;
    private final String hmacAlgorithm;

    public HmacCalculator(String hmacAlgorithm) {
        this.hmacAlgorithm = hmacAlgorithm;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            mac = Mac.getInstance(hmacAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    SecretKey getCurrentKey() {
        return currentKey;
    }

    public void setCurrentKey(byte[] bytes) {
        this.currentKey = new SecretKeySpec(bytes, hmacAlgorithm);
    }

    /**
     * Compute the HMAC and returns it as a base64 encoded String.
     *
     * @param data the data used to calculate the HMAC
     * @return the calculated HMAC as a base64 encoded String.
     * @throws SignatureException
     */
    public String calculate(byte[] data) throws SignatureException {
        try {
            mac.reset();
            mac.init(currentKey);

            // compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(data);

            // base64-encode the hmac
            String result = Base64.encode(rawHmac);

            // Compute the next key's iteration
            computeNextKeyIteration();

            return result;
        } catch (InvalidKeyException | IllegalStateException e) {
            throw new SignatureException("Failed to generate HMAC : " + e.getMessage());
        }
    }


    private void computeNextKeyIteration() {
        // k1 = digest(k0)
        messageDigest.reset();
        messageDigest.update(currentKey.getEncoded());
        currentKey = new SecretKeySpec(messageDigest.digest(), messageDigest.getAlgorithm());
    }
}
