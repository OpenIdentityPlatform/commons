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
import java.util.LinkedList;
import java.util.List;

import org.forgerock.json.JsonValue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class JWKSetTest {

    private static final String KTY2 = "EC";
    private static final String CRV = "NIST P-256";
    private static final String X = "MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4";
    private static final String Y = "4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM";
    private static final String D = "870MB6gfuTJ4HtUnUvYMyJpr5eUZNP4Bk43bVdj3eAE";
    private static final String USE = "enc";
    private static final String KID2 = "1";

    private static final String KTY1 = "OCT";
    private static final String K = "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr"
            + "1Z9CAow";
    private static final String KID1 = "HMAC key used in JWS A.1 example";

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
        sb.append("{");
        sb.append("\"keys\"").append(":");
        sb.append("[");
        sb.append("{")
                .append("\"kty\"").append(":").append("\"" + KTY1 + "\"").append(",")
                .append("\"k\"").append(":").append("\"" + K + "\"").append(",")
                .append("\"kid\"").append(":").append("\"" + KID1 + "\"")
                .append("}");
        sb.append(",");
        sb.append("{")
                .append("\"kty\"").append(":").append("\"" + KTY2 + "\"").append(",")
                .append("\"crv\"").append(":").append("\"" + CRV + "\"").append(",")
                .append("\"x\"").append(":").append("\"" + X + "\"").append(",")
                .append("\"y\"").append(":").append("\"" + Y + "\"").append(",")
                .append("\"d\"").append(":").append("\"" + D + "\"").append(",")
                .append("\"use\"").append(":").append("\"" + USE + "\"").append(",")
                .append("\"kid\"").append(":").append("\"" + KID2 + "\"")
                .append("}");
        sb.append("]");
        sb.append("}");

        json = sb.toString();

        List<JsonValue> listOfKeys = new LinkedList<>();
        //create json value object
        jsonValue = new JsonValue(new HashMap<>());
        jsonValue.put("kty", KTY1);
        jsonValue.put("k", K);
        jsonValue.put("kid", KID1);
        listOfKeys.add(jsonValue);

        jsonValue = new JsonValue(new HashMap<>());
        jsonValue.put("kty", KTY2);
        jsonValue.put("crv", CRV);
        jsonValue.put("x", X);
        jsonValue.put("y", Y);
        jsonValue.put("d", D);
        jsonValue.put("use", USE);
        jsonValue.put("kid", KID2);
        listOfKeys.add(jsonValue);

        jsonValue = new JsonValue(new HashMap<>());
        jsonValue.put("keys", listOfKeys);
    }

    @Test
    public void testCreateJWKFromAString() {
        JWKSet jwkSet = JWKSet.parse(json);

        List<JWK> jwks = jwkSet.getJWKsAsList();

        OctJWK jwk = (OctJWK) jwks.get(0);
        assert jwk.getKey().equalsIgnoreCase(K);
        assert jwk.get("kty").asString().equalsIgnoreCase(KTY1);
        assert jwk.get("kid").asString().equalsIgnoreCase(KID1);

        EcJWK jwk2 = (EcJWK) jwks.get(1);
        assert jwk2.getCurve().equals(CRV);
        assert jwk2.getX().equals(X);
        assert jwk2.getY().equals(Y);
        assert jwk2.getD().equals(D);
    }

    @Test
    public void testCreateJWKFromAJsonValue() {

        JWKSet jwkSet = JWKSet.parse(jsonValue);

        List<JWK> jwks = jwkSet.getJWKsAsList();

        OctJWK jwk = (OctJWK) jwks.get(0);
        assert jwk.getKey().equalsIgnoreCase(K);
        assert jwk.get("kty").asString().equalsIgnoreCase(KTY1);
        assert jwk.get("kid").asString().equalsIgnoreCase(KID1);

        EcJWK jwk2 = (EcJWK) jwks.get(1);
        assert jwk2.getCurve().equals(CRV);
        assert jwk2.getX().equals(X);
        assert jwk2.getY().equals(Y);
        assert jwk2.getD().equals(D);

    }
}
