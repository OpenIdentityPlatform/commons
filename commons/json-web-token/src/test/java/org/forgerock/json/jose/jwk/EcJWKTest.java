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

public class EcJWKTest {

    private static final String KTY = "EC";
    private static final String CRV = "NIST P-256";
    private static final String X = "MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4";
    private static final String Y = "4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM";
    private static final String D = "870MB6gfuTJ4HtUnUvYMyJpr5eUZNP4Bk43bVdj3eAE";
    private static final String USE = "enc";
    private static final String KID = "1";

    private String json = null;
    private JsonValue jsonValue = null;

    @BeforeClass
    public void setup() {
        /*
        {"kty":"EC",
        "crv":"P-256",
        "x":"MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4",
        "y":"4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM",
        "d":"870MB6gfuTJ4HtUnUvYMyJpr5eUZNP4Bk43bVdj3eAE",
        "use":"enc",
        "kid":"1"}
        */

        StringBuilder sb = new StringBuilder();
        sb.append("{")
                .append("\"kty\"").append(":").append("\"" + KTY + "\"").append(",")
                .append("\"crv\"").append(":").append("\"" + CRV + "\"").append(",")
                .append("\"x\"").append(":").append("\"" + X + "\"").append(",")
                .append("\"y\"").append(":").append("\"" + Y + "\"").append(",")
                .append("\"d\"").append(":").append("\"" + D + "\"").append(",")
                .append("\"use\"").append(":").append("\"" + USE + "\"").append(",")
                .append("\"kid\"").append(":").append("\"" + KID + "\"")
                .append("}");
        json = sb.toString();

        //create json value object
        jsonValue = new JsonValue(new HashMap<>());
        jsonValue.put("kty", KTY);
        jsonValue.put("crv", CRV);
        jsonValue.put("x", X);
        jsonValue.put("y", Y);
        jsonValue.put("d", D);
        jsonValue.put("use", USE);
        jsonValue.put("kid", KID);

    }

    @Test
    public void testCreateJWKFromAString() {

        //Given

        //When
        EcJWK jwk = EcJWK.parse(json);

        //Then
        assert jwk.getCurve().equals(CRV);
        assert jwk.getX().equals(X);
        assert jwk.getY().equals(Y);
        assert jwk.getD().equals(D);

    }

    @Test
    public void testCreateJWKFromAJsonValue() {
        //Given

        //When
        EcJWK jwk = EcJWK.parse(jsonValue);

        //Then
        assert jwk.getCurve().equals(CRV);
        assert jwk.getX().equals(X);
        assert jwk.getY().equals(Y);
        assert jwk.getD().equals(D);
    }
}
