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

package org.forgerock.json.jose.jwk;

import java.util.HashMap;

import org.forgerock.json.JsonValue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class OctJWKTest {

    private static final String KTY = "OCT";
    private static final String K = "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr"
            + "1Z9CAow";
    private static final String KID = "HMAC key used in JWS A.1 example";

    private String json = null;
    private JsonValue jsonValue = null;

    @BeforeClass
    public void setup() {
        //create string json object
        StringBuilder sb = new StringBuilder();
        /*
             {
             "kty":"oct",
             "k":"AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow",
             "kid":"HMAC key used in JWS A.1 example"
             }
         */
        sb.append("{")
                .append("\"kty\"").append(":").append("\"" + KTY + "\"").append(",")
                .append("\"k\"").append(":").append("\"" + K + "\"").append(",")
                .append("\"kid\"").append(":").append("\"" + KID + "\"")
                .append("}");

        json = sb.toString();

        //create json value object
        jsonValue = new JsonValue(new HashMap<>());
        jsonValue.put("kty", KTY);
        jsonValue.put("k", K);
        jsonValue.put("kid", KID);

    }

    @Test
    public void testCreateJWKFromAString() {
        OctJWK jwk = OctJWK.parse(json);

        assert jwk.getKey().equalsIgnoreCase(K);
        assert jwk.get("kty").asString().equalsIgnoreCase(KTY);
        assert jwk.get("kid").asString().equalsIgnoreCase(KID);
    }

    @Test
    public void testCreateJWKFromAJsonValue() {

        OctJWK jwk = OctJWK.parse(jsonValue);

        assert jwk.getKey().equalsIgnoreCase(K);
        assert jwk.get("kty").asString().equalsIgnoreCase(KTY);
        assert jwk.get("kid").asString().equalsIgnoreCase(KID);

    }
}
