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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.util.encode.Base64url;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;

import static org.testng.Assert.assertFalse;

public class RsaJWKTest {

    //RSA parameter values
    private final String N = "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4" +
            "cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMst" +
            "n64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2Q" +
            "vzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbIS" +
            "D08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqbw" +
            "0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw";

    private final String E = "AQAB";
    private final String D = "X4cTteJY_gn4FYPsXB8rdXix5vwsg1FLN5E3EaG6RJoVH-HLLKD9" +
            "M7dx5oo7GURknchnrRweUkC7hT5fJLM0WbFAKNLWY2vv7B6NqXSzUvxT0_YSfqij" +
            "wp3RTzlBaCxWp4doFk5N2o8Gy_nHNKroADIkJ46pRUohsXywbReAdYaMwFs9tv8d" +
            "_cPVY3i07a3t8MN6TNwm0dSawm9v47UiCl3Sk5ZiG7xojPLu4sbg1U2jx4IBTNBz" +
            "nbJSzFHK66jT8bgkuqsk0GjskDJk19Z4qwjwbsnn4j2WBii3RL-Us2lGVkY8fkFz" +
            "me1z0HbIkfz0Y6mqnOYtqc0X4jfcKoAC8Q";
    private final String P = "83i-7IvMGXoMXCskv73TKr8637FiO7Z27zv8oj6pbWUQyLPQBQxtPV" +
            "nwD20R-60eTDmD2ujnMt5PoqMrm8RfmNhVWDtjjMmCMjOpSXicFHj7XOuVIYQyqV" +
            "WlWEh6dN36GVZYk93N8Bc9vY41xy8B9RzzOGVQzXvNEvn7O0nVbfs";
    private final String Q = "3dfOR9cuYq-0S-mkFLzgItgMEfFzB2q3hWehMuG0oCuqnb3vobLyum" +
            "qjVZQO1dIrdwgTnCdpYzBcOfW5r370AFXjiWft_NGEiovonizhKpo9VVS78TzFgx" +
            "kIdrecRezsZ-1kYd_s1qDbxtkDEgfAITAG9LUnADun4vIcb6yelxk";
    private final String DQ = "s9lAH9fggBsoFR8Oac2R_E2gw282rT2kGOAhvIllETE1efrA6huUU" +
            "vMfBcMpn8lqeW6vzznYY5SSQF7pMdC_agI3nG8Ibp1BUb0JUiraRNqUfLhcQb_d9" +
            "GF4Dh7e74WbRsobRonujTYN1xCaP6TO61jvWrX-L18txXw494Q_cgk";
    private final String DP = "s9lAH9fggBsoFR8Oac2R_E2gw282rT2kGOAhvIllETE1efrA6huUU" +
            "vMfBcMpn8lqeW6vzznYY5SSQF7pMdC_agI3nG8Ibp1BUb0JUiraRNqUfLhcQb_d9" +
            "GF4Dh7e74WbRsobRonujTYN1xCaP6TO61jvWrX-L18txXw494Q_cgk";
    private final String QI = "GyM_p6JrXySiz1toFgKbWV-JdI3jQ4ypu9rbMWx3rQJBfmt0FoYzg" +
            "UIZEVFEcOqwemRN81zoDAaa-Bk0KWNGDjJHZDdDmFhW3AN7lI-puxk_mHZGJ11rx" +
            "yR8O55XLSe3SPmRfKwZI6yU24ZxvQKFYItdldUKGzO6Ia6zTKhAVRU";
    private final String ALG = "RS256";
    private final String KID = "2011-04-29";
    private final String KTY = "RSA";

    //json objects
    private String json = null;
    private JsonValue jsonValue = null;

    @BeforeClass
    public void setup(){
        /*{
         "kty":"RSA",
          "n":"0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4
     cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMst
     n64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2Q
     vzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbIS
     D08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqbw
     0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw",
          "e":"AQAB",
          "d":"X4cTteJY_gn4FYPsXB8rdXix5vwsg1FLN5E3EaG6RJoVH-HLLKD9
     M7dx5oo7GURknchnrRweUkC7hT5fJLM0WbFAKNLWY2vv7B6NqXSzUvxT0_YSfqij
     wp3RTzlBaCxWp4doFk5N2o8Gy_nHNKroADIkJ46pRUohsXywbReAdYaMwFs9tv8d
     _cPVY3i07a3t8MN6TNwm0dSawm9v47UiCl3Sk5ZiG7xojPLu4sbg1U2jx4IBTNBz
     nbJSzFHK66jT8bgkuqsk0GjskDJk19Z4qwjwbsnn4j2WBii3RL-Us2lGVkY8fkFz
     me1z0HbIkfz0Y6mqnOYtqc0X4jfcKoAC8Q",
          "p":"83i-7IvMGXoMXCskv73TKr8637FiO7Z27zv8oj6pbWUQyLPQBQxtPV
     nwD20R-60eTDmD2ujnMt5PoqMrm8RfmNhVWDtjjMmCMjOpSXicFHj7XOuVIYQyqV
     WlWEh6dN36GVZYk93N8Bc9vY41xy8B9RzzOGVQzXvNEvn7O0nVbfs",
          "q":"3dfOR9cuYq-0S-mkFLzgItgMEfFzB2q3hWehMuG0oCuqnb3vobLyum
     qjVZQO1dIrdwgTnCdpYzBcOfW5r370AFXjiWft_NGEiovonizhKpo9VVS78TzFgx
     kIdrecRezsZ-1kYd_s1qDbxtkDEgfAITAG9LUnADun4vIcb6yelxk",
          "dp":"G4sPXkc6Ya9y8oJW9_ILj4xuppu0lzi_H7VTkS8xj5SdX3coE0oim
     YwxIi2emTAue0UOa5dpgFGyBJ4c8tQ2VF402XRugKDTP8akYhFo5tAA77Qe_Nmtu
     YZc3C3m3I24G2GvR5sSDxUyAN2zq8Lfn9EUms6rY3Ob8YeiKkTiBj0",
          "dq":"s9lAH9fggBsoFR8Oac2R_E2gw282rT2kGOAhvIllETE1efrA6huUU
     vMfBcMpn8lqeW6vzznYY5SSQF7pMdC_agI3nG8Ibp1BUb0JUiraRNqUfLhcQb_d9
     GF4Dh7e74WbRsobRonujTYN1xCaP6TO61jvWrX-L18txXw494Q_cgk",
          "qi":"GyM_p6JrXySiz1toFgKbWV-JdI3jQ4ypu9rbMWx3rQJBfmt0FoYzg
     UIZEVFEcOqwemRN81zoDAaa-Bk0KWNGDjJHZDdDmFhW3AN7lI-puxk_mHZGJ11rx
     yR8O55XLSe3SPmRfKwZI6yU24ZxvQKFYItdldUKGzO6Ia6zTKhAVRU",
          "alg":"RS256",
          "kid":"2011-04-29"
          }
         */
        //create string rsa JWT
        StringBuilder sb = new StringBuilder();
        sb.append("{")
                .append("\"kty\"").append(":").append("\"" + KTY + "\"").append(",")
                .append("\"n\"").append(":").append("\"" + N + "\"").append(",")
                .append("\"e\"").append(":").append("\"" + E + "\"").append(",")
                .append("\"d\"").append(":").append("\"" + D + "\"").append(",")
                .append("\"p\"").append(":").append("\"" + P + "\"").append(",")
                .append("\"q\"").append(":").append("\"" + Q + "\"").append(",")
                .append("\"dp\"").append(":").append("\"" + DP + "\"").append(",")
                .append("\"dq\"").append(":").append("\"" + DQ + "\"").append(",")
                .append("\"qi\"").append(":").append("\"" + QI + "\"").append(",")
                .append("\"alg\"").append(":").append("\"" + ALG + "\"").append(",")
                .append("\"kid\"").append(":").append("\"" + KID + "\"")
           .append("}");
        json = sb.toString();

        //create json value object
        jsonValue = new JsonValue(new HashMap<String, String>());
        jsonValue.put("kty", KTY);
        jsonValue.put("n", N);
        jsonValue.put("e", E);
        jsonValue.put("d", D);
        jsonValue.put("p", P);
        jsonValue.put("q", Q);
        jsonValue.put("dp", DP);
        jsonValue.put("dq", DQ);
        jsonValue.put("qi", QI);
        jsonValue.put("alg", ALG);
        jsonValue.put("kid", KID);
    }

    @Test
    public void testCreateJWKFromAString(){
        //Given

        //When
        RsaJWK jwk = RsaJWK.parse(json);

        //Then
        assert jwk.getModulus().equals(N);
        assert jwk.getPublicExponent().equals(E);
        assert jwk.getPrivateExponent().equals(D);
        assert jwk.getPrimeP().equals(P);
        assert jwk.getPrimeQ().equals(Q);
        assert jwk.getPrimePExponent().equals(DP);
        assert jwk.getPrimeQExponent().equals(DQ);
        assert jwk.getCRTCoefficient().equals(QI);
    }

    @Test
    public void testCreateJWKFromAJsonValue(){
        //Given

        //When
        RsaJWK jwk = RsaJWK.parse(jsonValue);

        //Then
        assert jwk.getModulus().equals(N);
        assert jwk.getPublicExponent().equals(E);
        assert jwk.getPrivateExponent().equals(D);
        assert jwk.getPrimeP().equals(P);
        assert jwk.getPrimeQ().equals(Q);
        assert jwk.getPrimePExponent().equals(DP);
        assert jwk.getPrimeQExponent().equals(DQ);
        assert jwk.getCRTCoefficient().equals(QI);
    }

    @Test
    public void testCreatePrivateKey(){
        //Given
        RsaJWK jwk = RsaJWK.parse(json);

        //When
        RSAPrivateKey privKey = jwk.toRSAPrivateKey();

        //Then
        testPrivateKey(privKey);
    }

    @Test
    public void testCreatePublicKey(){
        //Given
        RsaJWK jwk = RsaJWK.parse(json);

        //When
        RSAPublicKey pubKey = jwk.toRSAPublicKey();

        //Then
        testPublicKey(pubKey);
    }

    @Test
    public void testCreatePairKey(){
        //Given
        RsaJWK jwk = RsaJWK.parse(json);

        //When
        KeyPair keypair = jwk.toKeyPair();

        //Then
        testPrivateKey((RSAPrivateKey)keypair.getPrivate());
        testPublicKey((RSAPublicKey)keypair.getPublic());
    }

    @Test
    public void testCreateJWKUsingPublicKey(){
        //Given
        KeyPair keypair = null;
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024);
            keypair = generator.genKeyPair();
        } catch (Exception e){
            assertFalse(false, e.getLocalizedMessage());
        }

        //When
        RsaJWK jwk = new RsaJWK((RSAPublicKey)keypair.getPublic(), null, ALG, KID, null, null, null);

        //Then
        BigInteger modulus = new BigInteger(Base64url.decode(jwk.getModulus()));
        BigInteger pubExponent = new BigInteger(Base64url.decode(jwk.getPublicExponent()));
        assert modulus.equals(((RSAPublicKey) keypair.getPublic()).getModulus());
        assert pubExponent.equals(((RSAPublicKey) keypair.getPublic()).getPublicExponent());

    }

    @Test
    public void testCreateJWKUsingPublicKeyandPrivateKey(){
        //Given
        KeyPair keypair = null;
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024);
            keypair = generator.genKeyPair();
        } catch (Exception e){
            assertFalse(false, e.getLocalizedMessage());
        }

        //When
        RsaJWK jwk = new
                RsaJWK((RSAPublicKey)keypair.getPublic(),(RSAPrivateKey)keypair.getPrivate(), null, ALG, KID,
                null, null, null);

        //Then
        BigInteger modulus = new BigInteger(Base64url.decode(jwk.getModulus()));
        BigInteger pubExponent = new BigInteger(Base64url.decode(jwk.getPublicExponent()));
        BigInteger privExponent = new BigInteger(Base64url.decode(jwk.getPrivateExponent()));

        assert modulus.equals(((RSAPublicKey) keypair.getPublic()).getModulus());
        assert pubExponent.equals(((RSAPublicKey) keypair.getPublic()).getPublicExponent());
        assert privExponent.equals(((RSAPrivateKey) keypair.getPrivate()).getPrivateExponent());
    }

    @Test
    public void testCreateJWKUsingPublicKeyandPrivateCert(){
        //Given
        KeyPair keypair = null;
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024);
            keypair = generator.genKeyPair();
        } catch (Exception e){
            assertFalse(false, e.getLocalizedMessage());
        }

        //When
        RsaJWK jwk = new
                RsaJWK((RSAPublicKey)keypair.getPublic(),(RSAPrivateCrtKey)keypair.getPrivate(), null, ALG, KID,
                null, null, null);

        //Then
        BigInteger modulus = new BigInteger(Base64url.decode(jwk.getModulus()));
        BigInteger pubExponent = new BigInteger(Base64url.decode(jwk.getPublicExponent()));
        BigInteger privExponent = new BigInteger(Base64url.decode(jwk.getPrivateExponent()));
        BigInteger primeP = new BigInteger(Base64url.decode(jwk.getPrimeP()));
        BigInteger primeQ = new BigInteger(Base64url.decode(jwk.getPrimeQ()));
        BigInteger primePExponent = new BigInteger(Base64url.decode(jwk.getPrimePExponent()));
        BigInteger primeQExponent = new BigInteger(Base64url.decode(jwk.getPrimeQExponent()));
        BigInteger crtCoefficient = new BigInteger(Base64url.decode(jwk.getCRTCoefficient()));

        assert modulus.equals(((RSAPublicKey) keypair.getPublic()).getModulus());
        assert pubExponent.equals(((RSAPublicKey) keypair.getPublic()).getPublicExponent());
        assert privExponent.equals(((RSAPrivateCrtKey) keypair.getPrivate()).getPrivateExponent());
        assert primeP.equals(((RSAPrivateCrtKey) keypair.getPrivate()).getPrimeP());
        assert primeQ.equals(((RSAPrivateCrtKey) keypair.getPrivate()).getPrimeQ());
        assert primePExponent.equals(((RSAPrivateCrtKey) keypair.getPrivate()).getPrimeExponentP());
        assert primeQExponent.equals(((RSAPrivateCrtKey) keypair.getPrivate()).getPrimeExponentQ());
        assert crtCoefficient.equals(((RSAPrivateCrtKey) keypair.getPrivate()).getCrtCoefficient());
    }

    private void testPrivateKey(RSAPrivateKey privKey){

        BigInteger modulus = new BigInteger(Base64url.decode(N));
        BigInteger privateExponent = new BigInteger(Base64url.decode(D));

        assert privKey.getModulus().equals(modulus);
        assert privKey.getPrivateExponent().equals(privateExponent);

    }

    private void testPublicKey(RSAPublicKey pubKey){
        BigInteger modulus = new BigInteger(Base64url.decode(N));
        BigInteger publicExponent = new BigInteger(Base64url.decode(E));

        assert pubKey.getPublicExponent().equals(publicExponent);
        assert pubKey.getModulus().equals(modulus);
    }
}
