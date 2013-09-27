/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions copyright [year] [name of copyright owner]"
 */
package org.forgerock.json.jose.jwk;

import java.util.HashMap;

import org.forgerock.json.fluent.JsonValue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class EcJWKTest {

    //ECJWK parameters
    private final String KTY = "EC";
    private final String CRV = "NIST P-256";
    private final String X = "MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4";
    private final String Y = "4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM";
    private final String D = "870MB6gfuTJ4HtUnUvYMyJpr5eUZNP4Bk43bVdj3eAE";
    private final String USE = "enc";
    private final String KID = "1";

    //json objects
    private String json = null;
    private JsonValue jsonValue = null;

    @BeforeClass
    public void setup(){
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
        jsonValue = new JsonValue(new HashMap<String, String>());
        jsonValue.put("kty", KTY);
        jsonValue.put("crv", CRV);
        jsonValue.put("x", X);
        jsonValue.put("y", Y);
        jsonValue.put("d", D);
        jsonValue.put("use", USE);
        jsonValue.put("kid", KID);

    }

    @Test
    public void testCreateJWKFromAString(){

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
    public void testCreateJWKFromAJsonValue(){
        //Given

        //When
        EcJWK jwk = EcJWK.parse(jsonValue);

        //Then
        assert jwk.getCurve().equals(CRV);
        assert jwk.getX().equals(X);
        assert jwk.getY().equals(Y);
        assert jwk.getD().equals(D);
    }
    /*
    @Test
    public void testCreatePrivateKey(){
        //Given
        EcJWK jwk = EcJWK.parse(jsonValue);

        //When
        ECPrivateKey privKey = jwk.toECPrivateKey();

        //Then
        validatePrivateKey(privKey);
    }

    @Test
    public void testCreatePublicKey(){
        //Given
        EcJWK jwk = EcJWK.parse(jsonValue);

        //When
        ECPublicKey publicKey = jwk.toECPublicKey();

        //Then
        validatePublicKey(publicKey);

    }

    @Test
    public void testCreatePairKey(){
        //Given
        EcJWK jwk = EcJWK.parse(json);

        //When
        KeyPair keypair = jwk.toKeyPair();

        //Then
        validatePrivateKey((ECPrivateKey)keypair.getPrivate());
        validatePublicKey((ECPublicKey)keypair.getPublic());

    }

    private void validatePrivateKey(ECPrivateKey privateKey){
        ECParameterSpec spec = null;
        ECPrivateKey expectedPriv = null;

        BigInteger d = new BigInteger(Base64url.decode(D));

        try{
            spec = NamedCurve.getECParameterSpec(CRV);
            ECPrivateKeySpec privspec = new ECPrivateKeySpec(d, spec);
            expectedPriv = (ECPrivateKey) ECKeyFactory.INSTANCE.generatePrivate(privspec);
        } catch (Exception e){
            throw new JsonException("Unable to create public EC key.", e);
        }

        assert privateKey.getParams().equals(spec);
        assert privateKey.getS().equals(d);
        assert privateKey.equals(expectedPriv);
    }

    private void validatePublicKey(ECPublicKey publicKey){
        ECParameterSpec spec = null;
        ECPublicKey expectedPub = null;
        try{
            spec = NamedCurve.getECParameterSpec(CRV);
            ECPoint point = new ECPoint(new BigInteger(Base64url.decode(X)),
                    new BigInteger(Base64url.decode(Y)));
            ECPublicKeySpec pubspec = new ECPublicKeySpec(point, spec);
            expectedPub = (ECPublicKey) ECKeyFactory.INSTANCE.generatePublic(pubspec);
        } catch (Exception e){
            throw new JsonException("Unable to create public EC key.", e);
        }

        BigInteger x = new BigInteger(Base64url.decode(X));
        BigInteger y = new BigInteger(Base64url.decode(Y));

        assert publicKey.getParams().equals(spec);
        assert publicKey.getW().getAffineX().equals(x);
        assert publicKey.getW().getAffineY().equals(y);
        assert publicKey.equals(expectedPub);
    }
    */
}
