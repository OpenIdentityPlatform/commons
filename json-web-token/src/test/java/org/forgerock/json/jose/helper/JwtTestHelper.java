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

package org.forgerock.json.jose.helper;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Map;

import org.forgerock.json.jose.utils.Utils;
import org.forgerock.util.encode.Base64url;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Helper class to create encoded JWT strings.
 */
@SuppressWarnings("javadoc")
public class JwtTestHelper {

    @SuppressWarnings("unchecked")
    public static Map<String, Object> jsonToMap(String json) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, Map.class);
    }

    public static String encodedPlaintextJwt(String header, String payload) {
        String headerString = Base64url.encode(header.toString().getBytes(Utils.CHARSET));
        String payloadString = Base64url.encode(payload.getBytes(Utils.CHARSET));
        return headerString + "." + payloadString + ".";
    }

    public static String encodedSignedJwt(String header, String payload, byte[] signature) {
        String headerString = Base64url.encode(header.toString().getBytes(Utils.CHARSET));
        String payloadString = Base64url.encode(payload.getBytes(Utils.CHARSET));
        String signatureString = Base64url.encode(signature);
        return headerString + "." + payloadString + "." + signatureString;
    }

    public static byte[] signWithRSA(String s, String algorithm, PrivateKey privateKey) throws SignatureException,
            NoSuchAlgorithmException, InvalidKeyException {

        Signature signature = Signature.getInstance(algorithm);
        signature.initSign(privateKey);
        signature.update(s.getBytes());
        return signature.sign();
    }
}
