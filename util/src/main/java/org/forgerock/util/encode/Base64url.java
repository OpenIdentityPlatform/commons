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

package org.forgerock.util.encode;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;

public class Base64url {

    public static String encode(byte[] content) {
        String base64EncodedString = Base64.encode(content);

        return base64EncodedString.replaceAll("\\+", "-")
                .replaceAll("/", "_")
                .replaceAll("=", "");
    }

    public static byte[] decode(String content) {

        content = content.replaceAll("-", "+")
                .replaceAll("_", "/");

        int modulus;
        if ((modulus = content.length() % 4) != 0) {
            for (int i = 0; i < modulus; i++) {
                content += "=";
            }
        }

        return Base64.decode(content);
    }
}

class SecurityListings
{
    public static void main(String[] args)
    {
        for (Provider provider : Security.getProviders())
        {
            System.out.println("Provider: " + provider.getName());
            for (Provider.Service service : provider.getServices())
            {
                System.out.println("  Algorithm: " + service.getAlgorithm());
            }
        }


        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");


            Key key = null;
            AlgorithmParameterSpec algorithmParameterSpec;


            cipher.init(Cipher.ENCRYPT_MODE, key);

            int a = 1;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InvalidKeyException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}