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

package org.forgerock.json.jose;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.jose.builders.JwtBuilder;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jwe.CompressionAlgorithm;
import org.forgerock.json.jose.jwe.EncryptedJwt;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jwe.JweHeader;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.JwsHeader;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.json.jose.jwt.JwtHeader;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {

    private KeyStore keyStore;

//    @BeforeClass
//    public void setUp() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
//        keyStore = KeyStore.getInstance("JKS");
//        InputStream keystoreStream = ClassLoader.getSystemResourceAsStream("keystore.jks");
//        keyStore.load(keystoreStream, "password".toCharArray());
//    }
//
//    @Test
//    public void should() throws KeyStoreException {
//
//        JweHeader header = new JweHeader(JweAlgorithm.RSAES_PKCS1_V1_5, CompressionMethod.NONE, EncryptionMethod.A128CBC_HS256);
//
//        Map<String, Object> claims = new HashMap<String, Object>();
//        claims.put("CLAIM1", "VALUE1");
//        JwtClaimsSet claimsSet = new JwtClaimsSet(claims);
//
//        EncryptedJwt encryptedJwt = new EncryptedJwt(header, claimsSet);
//        String jwt = encryptedJwt.build(keyStore.getCertificate("jwt-test-ks").getPublicKey());
//
//        int a = 1;
//    }




    String jweHeader = "{\"alg\":\"RSA1_5\",\"enc\":\"A128CBC-HS256\"}";
    String plaintext = "Live long and prosper.";
    byte[] contentEncryptionKey = new byte[]{(byte)4, (byte)211, (byte)31, (byte)197, (byte)84, (byte)157, (byte)252,
            (byte)254, (byte)11, (byte)100, (byte)157, (byte)250, (byte)63, (byte)170, (byte)106, (byte)206, (byte)107,
            (byte)124, (byte)212, (byte)45, (byte)111, (byte)107, (byte)9, (byte)219, (byte)200, (byte)177, (byte)0,
            (byte)240, (byte)143, (byte)156, (byte)44, (byte)207};

    private final static byte[] publicKeyModulus = {
            (byte)177, (byte)119, (byte) 33, (byte) 13, (byte)164, (byte) 30, (byte)108, (byte)121,
            (byte)207, (byte)136, (byte)107, (byte)242, (byte) 12, (byte)224, (byte) 19, (byte)226,
            (byte)198, (byte)134, (byte) 17, (byte) 71, (byte)173, (byte) 75, (byte) 42, (byte) 61,
            (byte) 48, (byte)162, (byte)206, (byte)161, (byte) 97, (byte)108, (byte)185, (byte)234,
            (byte)226, (byte)219, (byte)118, (byte)206, (byte)118, (byte)  5, (byte)169, (byte)224,

            (byte) 60, (byte)181, (byte) 90, (byte) 85, (byte) 51, (byte)123, (byte)  6, (byte)224,
            (byte)  4, (byte)122, (byte) 29, (byte)230, (byte)151, (byte) 12, (byte)244, (byte)127,
            (byte)121, (byte) 25, (byte)  4, (byte) 85, (byte)220, (byte)144, (byte)215, (byte)110,
            (byte)130, (byte) 17, (byte) 68, (byte)228, (byte)129, (byte)138, (byte)  7, (byte)130,
            (byte)231, (byte) 40, (byte)212, (byte)214, (byte) 17, (byte)179, (byte) 28, (byte)124,

            (byte)151, (byte)178, (byte)207, (byte) 20, (byte) 14, (byte)154, (byte)222, (byte)113,
            (byte)176, (byte) 24, (byte)198, (byte) 73, (byte)211, (byte)113, (byte)  9, (byte) 33,
            (byte)178, (byte) 80, (byte) 13, (byte) 25, (byte) 21, (byte) 25, (byte)153, (byte)212,
            (byte)206, (byte) 67, (byte)154, (byte)147, (byte) 70, (byte)194, (byte)192, (byte)183,
            (byte)160, (byte) 83, (byte) 98, (byte)236, (byte)175, (byte) 85, (byte) 23, (byte) 97,

            (byte) 75, (byte)199, (byte)177, (byte) 73, (byte)145, (byte) 50, (byte)253, (byte)206,
            (byte) 32, (byte)179, (byte)254, (byte)236, (byte)190, (byte) 82, (byte) 73, (byte) 67,
            (byte)129, (byte)253, (byte)252, (byte)220, (byte)108, (byte)136, (byte)138, (byte) 11,
            (byte)192, (byte)  1, (byte) 36, (byte)239, (byte)228, (byte) 55, (byte) 81, (byte)113,
            (byte) 17, (byte) 25, (byte)140, (byte) 63, (byte)239, (byte)146, (byte)  3, (byte)172,

            (byte) 96, (byte) 60, (byte)227, (byte)233, (byte) 64, (byte)255, (byte)224, (byte)173,
            (byte)225, (byte)228, (byte)229, (byte) 92, (byte)112, (byte) 72, (byte) 99, (byte) 97,
            (byte) 26, (byte) 87, (byte)187, (byte)123, (byte) 46, (byte) 50, (byte) 90, (byte)202,
            (byte)117, (byte) 73, (byte) 10, (byte)153, (byte) 47, (byte)224, (byte)178, (byte)163,
            (byte) 77, (byte) 48, (byte) 46, (byte)154, (byte) 33, (byte)148, (byte) 34, (byte)228,

            (byte) 33, (byte)172, (byte)216, (byte) 89, (byte) 46, (byte)225, (byte)127, (byte) 68,
            (byte)146, (byte)234, (byte) 30, (byte)147, (byte) 54, (byte)146, (byte)  5, (byte)133,
            (byte) 45, (byte) 78, (byte)254, (byte) 85, (byte) 55, (byte) 75, (byte)213, (byte) 86,
            (byte)194, (byte)218, (byte)215, (byte)163, (byte)189, (byte)194, (byte) 54, (byte)  6,
            (byte) 83, (byte) 36, (byte) 18, (byte)153, (byte) 53, (byte)  7, (byte) 48, (byte) 89,

            (byte) 35, (byte) 66, (byte)144, (byte)  7, (byte) 65, (byte)154, (byte) 13, (byte) 97,
            (byte) 75, (byte) 55, (byte)230, (byte)132, (byte)  3, (byte) 13, (byte)239, (byte) 71  };


    private static final byte[] publicKeyExponent= { 1, 0, 1 };


    private static final byte[] publicKeyPrivateModulus = {
            (byte) 84, (byte) 80, (byte)150, (byte) 58, (byte)165, (byte)235, (byte)242, (byte)123,
            (byte)217, (byte) 55, (byte) 38, (byte)154, (byte) 36, (byte)181, (byte)221, (byte)156,
            (byte)211, (byte)215, (byte)100, (byte)164, (byte) 90, (byte) 88, (byte) 40, (byte)228,
            (byte) 83, (byte)148, (byte) 54, (byte)122, (byte)  4, (byte) 16, (byte)165, (byte) 48,
            (byte) 76, (byte)194, (byte) 26, (byte)107, (byte) 51, (byte) 53, (byte)179, (byte)165,

            (byte) 31, (byte) 18, (byte)198, (byte)173, (byte) 78, (byte) 61, (byte) 56, (byte) 97,
            (byte)252, (byte)158, (byte)140, (byte) 80, (byte) 63, (byte) 25, (byte)223, (byte)156,
            (byte) 36, (byte)203, (byte)214, (byte)252, (byte)120, (byte) 67, (byte)180, (byte)167,
            (byte)  3, (byte) 82, (byte)243, (byte) 25, (byte) 97, (byte)214, (byte) 83, (byte)133,
            (byte) 69, (byte) 16, (byte)104, (byte) 54, (byte)160, (byte)200, (byte) 41, (byte) 83,

            (byte)164, (byte)187, (byte) 70, (byte)153, (byte)111, (byte)234, (byte)242, (byte)158,
            (byte)175, (byte) 28, (byte)198, (byte) 48, (byte)211, (byte) 45, (byte)148, (byte) 58,
            (byte) 23, (byte) 62, (byte)227, (byte) 74, (byte) 52, (byte)117, (byte) 42, (byte) 90,
            (byte) 41, (byte)249, (byte)130, (byte)154, (byte) 80, (byte)119, (byte) 61, (byte) 26,
            (byte)193, (byte) 40, (byte)125, (byte) 10, (byte)152, (byte)174, (byte)227, (byte)225,

            (byte)205, (byte) 32, (byte) 62, (byte) 66, (byte)  6, (byte)163, (byte)100, (byte) 99,
            (byte)219, (byte) 19, (byte)253, (byte) 25, (byte)105, (byte) 80, (byte)201, (byte) 29,
            (byte)252, (byte)157, (byte)237, (byte) 69, (byte)  1, (byte) 80, (byte)171, (byte)167,
            (byte) 20, (byte)196, (byte)156, (byte)109, (byte)249, (byte) 88, (byte)  0, (byte)  3,
            (byte)152, (byte) 38, (byte)165, (byte) 72, (byte) 87, (byte)  6, (byte)152, (byte) 71,

            (byte)156, (byte)214, (byte) 16, (byte) 71, (byte) 30, (byte) 82, (byte) 51, (byte)103,
            (byte) 76, (byte)218, (byte) 63, (byte)  9, (byte) 84, (byte)163, (byte)249, (byte) 91,
            (byte)215, (byte) 44, (byte)238, (byte) 85, (byte)101, (byte)240, (byte)148, (byte)  1,
            (byte) 82, (byte)224, (byte) 91, (byte)135, (byte)105, (byte)127, (byte) 84, (byte)171,
            (byte)181, (byte)152, (byte)210, (byte)183, (byte)126, (byte) 24, (byte) 46, (byte)196,

            (byte) 90, (byte)173, (byte) 38, (byte)245, (byte)219, (byte)186, (byte)222, (byte) 27,
            (byte)240, (byte)212, (byte)194, (byte) 15, (byte) 66, (byte)135, (byte)226, (byte)178,
            (byte)190, (byte) 52, (byte)245, (byte) 74, (byte) 65, (byte)224, (byte) 81, (byte)100,
            (byte) 85, (byte) 25, (byte)204, (byte)165, (byte)203, (byte)187, (byte)175, (byte) 84,
            (byte)100, (byte) 82, (byte) 15, (byte) 11, (byte) 23, (byte)202, (byte)151, (byte)107,

            (byte) 54, (byte) 41, (byte)207, (byte)  3, (byte)136, (byte)229, (byte)134, (byte)131,
            (byte) 93, (byte)139, (byte) 50, (byte)182, (byte)204, (byte) 93, (byte)130, (byte)89   };

    private Key publicKey;
    private Key privateKey;

    private JwtReconstruction jwtReconstruction;

    @BeforeClass
    public void setUp() throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        KeySpec publicKeySpec = new RSAPublicKeySpec(new BigInteger(1, publicKeyModulus), new BigInteger(1, publicKeyExponent));
        KeySpec privateKeySpec = new RSAPrivateKeySpec(new BigInteger(1, publicKeyModulus), new BigInteger(1, publicKeyPrivateModulus));

        publicKey = keyFactory.generatePublic(publicKeySpec);
        privateKey = keyFactory.generatePrivate(privateKeySpec);

        jwtReconstruction = new JwtReconstruction();
    }

//    @Test      //TODO
//    public void shouldJWEUsingRSAES_PKCS1_V1_5andAES_128_CBC_HMAC_SHA_256() {
//
//
//
////
////        String encodedJweHeader = Base64url.encode(jweHeader.getBytes(Utils.CHARSET));
////
////        byte[] plaintextBytes = plaintext.getBytes(Utils.CHARSET);
////
////        Cipher cipher = Cipher.getInstance(algorithm.getTransformation());
////        cipher.init(Cipher.ENCRYPT_MODE, key);
////        encryptedContentEncryptionKey = cipher.doFinal(contentEncryptionKey.getEncoded());
////
//
//
//        JweHeader header = new JweHeader();
//        header.setAlgorithm(JweAlgorithm.RSAES_PKCS1_V1_5);
//        header.setEncryptionMethod(EncryptionMethod.A128CBC_HS256);
//
//        Map<String, Object> claims = new HashMap<String, Object>();
//        claims.put("CLAIM1", "VALUE1");
//        JwtClaimsSet claimsSet = new JwtClaimsSet();
//        claimsSet.setClaims(claims);
//
//        EncryptedJwt encryptedJwt = new EncryptedJwt(header, claimsSet, publicKey);
//        String jwt = encryptedJwt.build();
//
//        int a = 1;
//    }



    @Test
    public void shouldCreatePlaintextJwt() {

        JwsHeader header = new JwsHeader();
        header.setAlgorithm(JwsAlgorithm.NONE);
        header.setHeader("custom1", "v1");

        JwtClaimsSet claimsSet = new JwtClaimsSet();
        claimsSet.setClaim("claim1", "v2");

        SignedJwt signedJwt = new SignedJwt(header, claimsSet, privateKey);

        String jwt = signedJwt.build();

        int a = 1;
    }

    @Test
    public void shouldCreateSignedJwt() {

        JwsHeader header = new JwsHeader();
        header.setAlgorithm(JwsAlgorithm.HS256);
        header.setHeader("custom1", "v1");

        JwtClaimsSet claimsSet = new JwtClaimsSet();
        claimsSet.setClaim("claim1", "v2");

        SignedJwt signedJwt = new SignedJwt(header, claimsSet, privateKey);

        String jwt = signedJwt.build();

        int a = 1;
    }

//    @Test                  //TODO
//    public void shouldCreateEncryptedJwt() {
//
//        JweHeader header = new JweHeader();
//        header.setAlgorithm(JweAlgorithm.RSAES_PKCS1_V1_5);
//        header.setEncryptionMethod(EncryptionMethod.A128CBC_HS256);
//        header.setHeader("custom1", "v1");
//
//        JwtClaimsSet claimsSet = new JwtClaimsSet();
//        claimsSet.setClaim("claim1", "v2");
//
//        EncryptedJwt encryptedJwt = new EncryptedJwt(header, claimsSet, publicKey);
//
//        String jwt = encryptedJwt.build();
//
//        int a = 1;
//    }

    @Test
    public void shouldReconstructPlaintextJwt() {

        String jwt = "eyAiYWxnIjogIk5PTkUiLCAiY3VzdG9tMSI6ICJ2MSIsICJ0eXAiOiAiSldUIiB9.eyAiY2xhaW0xIjogInYyIiB9.";
        Jwt plaintextJwt = jwtReconstruction.reconstructJwt(jwt, Jwt.class);

        int a = 1;

    }

    @Test
    public void shouldVerifySignedJwt() {

        String jwt = "eyAiYWxnIjogIkhTMjU2IiwgImN1c3RvbTEiOiAidjEiLCAidHlwIjogIkpXVCIgfQ.eyAiY2xhaW0xIjogInYyIiB9.M_YXPTDCIL3cA1aXitRWMPVV9fIHSXCUbjw1zx8x-AQ";
        SignedJwt signedJwt = jwtReconstruction.reconstructJwt(jwt, SignedJwt.class);

        signedJwt.verify(privateKey);

        int a = 1;

    }

    @Test
    public void shouldDecryptEncryptedJwt() {

        String jwt = "eyAiYWxnIjogIlJTQUVTX1BLQ1MxX1YxXzUiLCAiY3VzdG9tMSI6ICJ2MSIsICJ0eXAiOiAiSldUIiwgImVuYyI6ICJBMTI4Q0JDX0hTMjU2IiB9.rn7SIxIffpdolojD11YbKztMHFXMiZT-B5Fh7TVaPZYxn7zO0b4Mkfj3jf3mqDYfOYR4KG14X5ghOcK0TFHswpekQfev-oEYfl9bgRjrbPaoLixopPQWqMWfed5v2MAiR9n735nHzhkBZJfGvZNG2DS6Ceu3FDdb6vcfYd2FsYntwVgF751A1eqvgMTblzqXYsS_Q44wVg1BOwVqyC-hC4nNO_jh-lSQbhjmCQXX5hW8_z700Kif52NktjT_zLo9f5RXPRbNsEeSCOK1ApCzC2XUBA69bPgZ5BDj-MM2-hCpzy4k5TyWZYpkiVDLOGRSXB_9htG8Xpl2J14Ou0PW8A.1rtCA-yDnfqdbvA4uEfoNg.i6Y1KRZAZr5KbSaNBJG1QjFJDbGkN9sWnSiu9yaCGlk.kUq4rrkrB3ZbmqetSUsQfA";
        EncryptedJwt encryptedJwt = jwtReconstruction.reconstructJwt(jwt, EncryptedJwt.class);

        encryptedJwt.decrypt(privateKey);

        int a = 1;

    }



//    @Test              //TODO
//    public void shouldTestBuilders() throws MalformedURLException {
//        //Uses cases
//
//
//        JwtBuilder jwtBuilder = new JwtBuilder();
//
//        //Create a Plaintext JWT
//        String plaintextJwt = jwtBuilder           //TODO make builder so Plaintext JWT cannot specify Algorithm
//                .jwt()
//                    .headers()
//                        .header("header1", "v1")
//                        .done()
//                    .claims()
//                        .claim("claim1", "v2")
//                        .typ("typ")
//                        .jti("jti")
//                        .iss("iss")
//                        .prn("prn")
//                        .aud(Arrays.asList(new String[]{"aud1", "aud2"}))
////                        .iat(new Date())       //TODO need to convert Dates to Longs!!
////                        .nbf(new Date())       //TODO need to convert Dates to Longs!!
////                        .exp(new Date())       //TODO need to convert Dates to Longs!!
//                        .done()
//                    .build();
//
//
//        //Create a Signed JWT
//        String signedJwt = jwtBuilder
//                .jws(privateKey)
//                    .headers()
//                        .alg(JwsAlgorithm.HS256)
////                        .jku(new URL("http://example.com"))    //TODO need to process reserved values first!!!
//                        .jwk(new JWK() {
//                            @Override
//                            public String toString() {
//                                return "\"JWK\"";
//                            }
//                        })
////                        .x5u(new URL("http://example.com"))    //TODO need to process reserved values first!!!
//                        .x5t("x5t")
//                        .x5c(Arrays.asList(new String[]{"x5c1", "x5c2"}))
//                        .kid("kid")
//                        .cty("cty")
//                        .crit(Arrays.asList(new String[]{"jku", "cty"}))
//                        .header("header1", "v1")
//                        .done()
//                    .claims()
//                        .claim("claim1", "v2")
//                        .typ("typ")
//                        .jti("jti")
//                        .iss("iss")
//                        .prn("prn")
//                        .aud(Arrays.asList(new String[]{"aud1", "aud2"}))
////                        .iat(new Date())       //TODO need to convert Dates to Longs!!
////                        .nbf(new Date())       //TODO need to convert Dates to Longs!!
////                        .exp(new Date())       //TODO need to convert Dates to Longs!!
//                        .done()
//                    .build();
//
//
//        //Create a Encrypted JWT
//        String encryptedJwt = jwtBuilder
//                .jwe(publicKey)
//                    .headers()
//                        .alg(JweAlgorithm.RSAES_PKCS1_V1_5)
////                        .jku(new URL("http://example.com"))         //TODO need to process reserved values first!!!
//                        .jwk(new JWK() {
//                            @Override
//                            public String toString() {
//                                return "\"JWK\"";
//                            }
//                        })
////                        .x5u(new URL("http://example.com"))           //TODO need to process reserved values first!!!
//                        .x5t("x5t")
//                        .x5c(Arrays.asList(new String[]{"x5c1", "x5c2"}))
//                        .kid("kid")
//                        .cty("cty")
//                        .crit(Arrays.asList(new String[]{"jku", "cty"}))
//                        .enc(EncryptionMethod.A128CBC_HS256)
//                        .epk("epk")
//                        .zip(CompressionAlgorithm.DEF)
//                        .apu("apu")
//                        .header("header1", "v1")
//                        .done()
//                    .claims()
//                        .claim("claim1", "v2")
//                        .typ("typ")
//                        .jti("jti")
//                        .iss("iss")
//                        .prn("prn")
//                        .aud(Arrays.asList(new String[]{"aud1", "aud2"}))
////                        .iat(new Date())       //TODO need to convert Dates to Longs!!
////                        .nbf(new Date())       //TODO need to convert Dates to Longs!!
////                        .exp(new Date())       //TODO need to convert Dates to Longs!!
//                        .done()
//                    .build();
//
//
//        //Reconstruct a Plaintext JWT
//        Jwt pJwt = jwtBuilder.reconstruct(plaintextJwt, Jwt.class);
//
//        //Reconstruct a Signed JWT
//        SignedJwt sJwt = jwtBuilder.reconstruct(signedJwt, SignedJwt.class);
//        sJwt.verify(privateKey);
//
//        //Reconstruct a Encrypted JWT
//        EncryptedJwt eJwt = jwtBuilder.reconstruct(encryptedJwt, EncryptedJwt.class);
//        eJwt.decrypt(privateKey);
//
//        int a = 1;
//    }
}
