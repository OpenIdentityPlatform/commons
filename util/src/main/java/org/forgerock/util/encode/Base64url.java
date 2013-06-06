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
            for (int i = 0; i < (4 - modulus); i++) {
                content += "=";
            }
        }

        return Base64.decode(content);
    }
}
