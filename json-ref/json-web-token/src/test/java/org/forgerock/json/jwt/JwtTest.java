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

package org.forgerock.json.jwt;

import com.sun.identity.shared.encode.Base64;
import org.json.JSONException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

public class JwtTest {

    private JwtBuilder jwtBuilder;

    private KeyStore keyStore;

    @BeforeClass
    public void setUp() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        jwtBuilder = new JwtBuilder();

        keyStore = KeyStore.getInstance("JKS");
        InputStream keystoreStream = ClassLoader.getSystemResourceAsStream("keystore.jks");
        keyStore.load(keystoreStream, "password".toCharArray());
    }

    private PrivateKey getPrivateKey(String alias, char[] password) throws UnrecoverableKeyException,
            NoSuchAlgorithmException, KeyStoreException {
        return (PrivateKey) keyStore.getKey(alias, password);

    }

    private X509Certificate getCertificate(String alias) throws KeyStoreException {
        return (X509Certificate) keyStore.getCertificate(alias);
    }

    @Test
    public void shouldCreatePlaintextJwt() throws JWTBuilderException {

        //Given
        Map<String, Object> jwtValues = new HashMap<String, Object>();
        jwtValues.put("KEY2", true);
        jwtValues.put("KEY3", 123);
        jwtValues.put("KEY4", 1362397486273L);
        jwtValues.put("KEY5", 45.43);

        //When
        String plaintextJwt = jwtBuilder.jwt()
                .header("H1", "HEADER1")
                .header("H2", "HEADER2")
                .content("KEY1", "VALUE1")
                .content(jwtValues)
                .build();

        //Then
        String expectedHeader = "{ \"H2\": \"HEADER2\", \"H1\": \"HEADER1\", \"typ\": \"JWT\" }";
        String expectedEncodedHeader = Base64.encode(expectedHeader.getBytes());
        String expectedContent = "{ \"KEY2\": true, \"KEY1\": \"VALUE1\", \"KEY4\": 1362397486273, \"KEY3\": 123, " +
                "\"KEY5\": 45.43 }";
        String expectedEncodedContent = Base64.encode(expectedContent.getBytes());
        String expectedThirdPart = "";
        String expectedEncodedThirdPart = Base64.encode(expectedThirdPart.getBytes());

        int headerEndIndex = plaintextJwt.indexOf(".");
        String actualEncodedHeader = plaintextJwt.substring(0, headerEndIndex);
        String actualHeader = new String(Base64.decode(actualEncodedHeader));

        int contentEndIndex = plaintextJwt.indexOf(".", headerEndIndex + 1);
        String actualEncodedContent = plaintextJwt.substring(headerEndIndex + 1, contentEndIndex);
        String actualContent = new String(Base64.decode(actualEncodedContent));

        String actualEncodedThirdPart = plaintextJwt.substring(contentEndIndex + 1);
        String actualThirdPart = new String(Base64.decode(actualEncodedThirdPart));

        String expectedPlaintextJwt = expectedEncodedHeader + "." + expectedEncodedContent + "." + expectedThirdPart;

        assertEquals(plaintextJwt, expectedPlaintextJwt);
        assertEquals(actualEncodedHeader, expectedEncodedHeader);
        assertEquals(actualEncodedContent, expectedEncodedContent);
        assertEquals(actualEncodedThirdPart, expectedEncodedThirdPart);
        assertEquals(actualHeader, expectedHeader);
        assertEquals(actualContent, expectedContent);
        assertEquals(actualThirdPart, expectedThirdPart);
    }

    @Test
    public void shouldReconstructPlaintextJwt() throws JSONException {

        //Given

        //When
        String plaintextJwtString = "eyAiSDIiOiAiSEVBREVSMiIsICJIMSI6ICJIRUFERVIxIiwgInR5cCI6ICJKV1QiIH0=" +
                ".eyAiS0VZMiI6IHRydWUsICJLRVkxIjogIlZBTFVFMSIsICJLRVkzIjogMTIzLCAiS0VZNiI6IDQ1LjQzLCAiS0V" +
                "ZNSI6IDEzNjIzOTc0ODYyNzMgfQ==" +
                ".";

        //When
        PlaintextJwt reconstructedJwt = (PlaintextJwt) new JwtBuilder().recontructJwt(plaintextJwtString);

        //Then
        assertEquals(reconstructedJwt.headerKeySet().size(), 3);
        assertEquals(reconstructedJwt.getHeader("H1"), "HEADER1");
        assertEquals(reconstructedJwt.getHeader("H2"), "HEADER2");
        assertEquals(reconstructedJwt.getHeader("typ"), "JWT");
        assertEquals(reconstructedJwt.contentKeySet().size(), 5);
        assertEquals(reconstructedJwt.getContent("KEY1"), "VALUE1");
        assertEquals(reconstructedJwt.getContent("KEY2"), Boolean.TRUE);
        assertEquals(reconstructedJwt.getContent("KEY3"), Integer.valueOf(123));
        assertEquals(reconstructedJwt.getContent("KEY5"), Long.valueOf(1362397486273L));
        assertEquals(reconstructedJwt.getContent("KEY6"), Double.valueOf(45.43));
    }

    @Test
    public void shouldSignAndVerifyUsingHMAC() throws KeyStoreException, CertificateException,
            NoSuchAlgorithmException, IOException, UnrecoverableKeyException, SignatureException {

        //Given
        PrivateKey key = getPrivateKey("jwt-test-ks", "password".toCharArray());
        X509Certificate certificate = getCertificate("jwt-test-ks");

        //When
        byte[] signature = JwsSignatureUtil.getInstance().sign(key, JwsAlgorithm.HS256, "123abc");

        boolean verified = JwsSignatureUtil.getInstance().verify(JwsAlgorithm.HS256, key, certificate, "123abc",
                signature);

        //Then
        assertTrue(verified);
    }

    @Test
    public void shouldSignAndVerifyUsingRSA() throws KeyStoreException, CertificateException,
            NoSuchAlgorithmException, IOException, UnrecoverableKeyException, SignatureException {

        //Given
        PrivateKey key = getPrivateKey("jwt-test-ks", "password".toCharArray());
        X509Certificate certificate = getCertificate("jwt-test-ks");


        //When
        byte[] signature = JwsSignatureUtil.getInstance().sign(key, JwsAlgorithm.RS256, "123abc");

        boolean verified = JwsSignatureUtil.getInstance().verify(JwsAlgorithm.RS256, key, certificate, "123abc",
                signature);

        //Then
        assertTrue(verified);
    }

    @Test
    public void shouldCreateSignedHS256Jwt() throws JWTBuilderException, KeyStoreException, IOException,
            CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {

        //Given
        PrivateKey key = getPrivateKey("jwt-test-ks", "password".toCharArray());
        Map<String, Object> jwtValues = new HashMap<String, Object>();
        jwtValues.put("KEY2", true);
        jwtValues.put("KEY3", 123);
        jwtValues.put("KEY4", 1362397486273L);
        jwtValues.put("KEY5", 45.43);

        //When
        String signedJwt = jwtBuilder.jwt()
                .header("H1", "HEADER1")
                .header("H2", "HEADER2")
                .content("KEY1", "VALUE1")
                .content(jwtValues)
                .sign(JwsAlgorithm.HS256, key)
                .build();

        //Then
        String expectedHeader = "{ \"alg\": \"HS256\", \"H2\": \"HEADER2\", \"H1\": \"HEADER1\", \"typ\": \"JWT\" }";
        String expectedEncodedHeader = Base64.encode(expectedHeader.getBytes());
        String expectedContent = "{ \"KEY2\": true, \"KEY1\": \"VALUE1\", \"KEY4\": 1362397486273, " +
                "\"KEY3\": 123, \"KEY5\": 45.43 }";
        String expectedEncodedContent = Base64.encode(expectedContent.getBytes());
        String expectedEncodedThirdPart = "bFPwBcagnfyCbNv6Cx8QH6Xf51Ps5HmEdvAuoKQfUr8=";

        int headerEndIndex = signedJwt.indexOf(".");
        String actualEncodedHeader = signedJwt.substring(0, headerEndIndex);
        String actualHeader = new String(Base64.decode(actualEncodedHeader));

        int contentEndIndex = signedJwt.indexOf(".", headerEndIndex + 1);
        String actualEncodedContent = signedJwt.substring(headerEndIndex + 1, contentEndIndex);
        String actualContent = new String(Base64.decode(actualEncodedContent));

        String actualEncodedThirdPart = signedJwt.substring(contentEndIndex + 1);

        assertEquals(signedJwt, expectedEncodedHeader + "." + expectedEncodedContent + "."
                + expectedEncodedThirdPart);
        assertEquals(actualEncodedHeader, expectedEncodedHeader);
        assertEquals(actualEncodedContent, expectedEncodedContent);
        assertEquals(actualEncodedThirdPart, expectedEncodedThirdPart);
        assertEquals(actualHeader, expectedHeader);
        assertEquals(actualContent, expectedContent);
    }

    @Test
    public void shouldReconstructSignedHS256Jwt() throws JSONException, CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException, JWTBuilderException, UnrecoverableKeyException {

        //Given
        PrivateKey key = getPrivateKey("jwt-test-ks", "password".toCharArray());
        X509Certificate certificate = getCertificate("jwt-test-ks");

        Map<String, Object> jwtValues = new HashMap<String, Object>();
        jwtValues.put("KEY2", true);
        jwtValues.put("KEY3", 123);
        jwtValues.put("KEY4", 1362397486273L);
        jwtValues.put("KEY5", 45.43);

        //When
        String signedJwtString = new PlaintextJwt()
                .header("H1", "HEADER1")
                .header("H2", "HEADER2")
                .content("KEY1", "VALUE1")
                .content(jwtValues)
                .sign(JwsAlgorithm.HS256, key)
                .build();

        //When
        SignedJwt reconstructedJwt = (SignedJwt) new JwtBuilder().recontructJwt(signedJwtString);

        //Then
        assertTrue(reconstructedJwt.verify(key, certificate));
        assertEquals(reconstructedJwt.getJwt().headerKeySet().size(), 4);
        assertEquals(reconstructedJwt.getJwt().getHeader("alg"), "HS256");
        assertEquals(reconstructedJwt.getJwt().getHeader("H1"), "HEADER1");
        assertEquals(reconstructedJwt.getJwt().getHeader("H2"), "HEADER2");
        assertEquals(reconstructedJwt.getJwt().getHeader("typ"), "JWT");
        assertEquals(reconstructedJwt.getJwt().contentKeySet().size(), 5);
        assertEquals(reconstructedJwt.getJwt().getContent("KEY1"), "VALUE1");
        assertEquals(reconstructedJwt.getJwt().getContent("KEY2"), Boolean.TRUE);
        assertEquals(reconstructedJwt.getJwt().getContent("KEY3"), Integer.valueOf(123));
        assertEquals(reconstructedJwt.getJwt().getContent("KEY4"), Long.valueOf(1362397486273L));
        assertEquals(reconstructedJwt.getJwt().getContent("KEY5"), Double.valueOf(45.43));
    }

    @Test
    public void shouldCreateSignedRS256Jwt() throws JWTBuilderException, KeyStoreException, IOException,
            CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {

        //Given
        PrivateKey key = getPrivateKey("jwt-test-ks", "password".toCharArray());
        Map<String, Object> jwtValues = new HashMap<String, Object>();
        jwtValues.put("KEY2", true);
        jwtValues.put("KEY3", 123);
        jwtValues.put("KEY4", 1362397486273L);
        jwtValues.put("KEY5", 45.43);

        //When
        String signedJwt = jwtBuilder.jwt()
                .header("H1", "HEADER1")
                .header("H2", "HEADER2")
                .content("KEY1", "VALUE1")
                .content(jwtValues)
                .sign(JwsAlgorithm.RS256, key)
                .build();

        //Then
        String expectedHeader = "{ \"alg\": \"RS256\", \"H2\": \"HEADER2\", \"H1\": \"HEADER1\", \"typ\": \"JWT\" }";
        String expectedEncodedHeader = Base64.encode(expectedHeader.getBytes());
        String expectedContent = "{ \"KEY2\": true, \"KEY1\": \"VALUE1\", \"KEY4\": 1362397486273, " +
                "\"KEY3\": 123, \"KEY5\": 45.43 }";
        String expectedEncodedContent = Base64.encode(expectedContent.getBytes());
        String expectedEncodedThirdPart = "meSd7BUEhKJyMYwNd8Ydtg4gAenJNHsYLucWxmPi5i8roOm8JFKjV5x0Czt6sm8LRXA8i" +
                "/2zAwgGBnXC1cxvYJMcFGGHOJ5vPImIffcFBc8wGUcvg6G5ooKMkxjKKFSt3eQPBmYAsKVymMYUBGDFCBgdm+mk7ptYO6Pk" +
                "oeAmvQUt9JRTkMKEuJNwiLtkif6OakB+RuRDIT/lBQGdYp/Qqxm/pt8WBTuF/EYvZVWisVnxRI8WlsBofcfpP4VhFRPE15T" +
                "+wVSfnlaxuKY0Boy1+xcU91Tg9/Mh2PsXgYYWe6CbXxSazXTKcdzhrrziyMh+6pYmHVkUOXgx0a/vjXSfcg==";

        int headerEndIndex = signedJwt.indexOf(".");
        String actualEncodedHeader = signedJwt.substring(0, headerEndIndex);
        String actualHeader = new String(Base64.decode(actualEncodedHeader));

        int contentEndIndex = signedJwt.indexOf(".", headerEndIndex + 1);
        String actualEncodedContent = signedJwt.substring(headerEndIndex + 1, contentEndIndex);
        String actualContent = new String(Base64.decode(actualEncodedContent));

        String actualEncodedThirdPart = signedJwt.substring(contentEndIndex + 1);

        assertEquals(signedJwt, expectedEncodedHeader + "." + expectedEncodedContent + "."
                + expectedEncodedThirdPart);
        assertEquals(actualEncodedHeader, expectedEncodedHeader);
        assertEquals(actualEncodedContent, expectedEncodedContent);
        assertEquals(actualEncodedThirdPart, expectedEncodedThirdPart);
        assertEquals(actualHeader, expectedHeader);
        assertEquals(actualContent, expectedContent);
    }

    @Test
    public void shouldReconstructSignedRS256Jwt() throws JSONException, CertificateException,
            NoSuchAlgorithmException, IOException, KeyStoreException, JWTBuilderException, UnrecoverableKeyException {

        //Given
        PrivateKey key = getPrivateKey("jwt-test-ks", "password".toCharArray());
        X509Certificate certificate = getCertificate("jwt-test-ks");

        Map<String, Object> jwtValues = new HashMap<String, Object>();
        jwtValues.put("KEY2", true);
        jwtValues.put("KEY3", 123);
        jwtValues.put("KEY4", 1362397486273L);
        jwtValues.put("KEY5", 45.43);

        //When
        String signedJwtString = jwtBuilder.jwt()
                .header("H1", "HEADER1")
                .header("H2", "HEADER2")
                .content("KEY1", "VALUE1")
                .content(jwtValues)
                .sign(JwsAlgorithm.RS256, key)
                .build();

        //When
        SignedJwt reconstructedJwt = (SignedJwt) new JwtBuilder().recontructJwt(signedJwtString);

        //Then
        assertTrue(reconstructedJwt.verify(key, certificate));
        assertEquals(reconstructedJwt.getJwt().headerKeySet().size(), 4);
        assertEquals(reconstructedJwt.getJwt().getHeader("alg"), "RS256");
        assertEquals(reconstructedJwt.getJwt().getHeader("H1"), "HEADER1");
        assertEquals(reconstructedJwt.getJwt().getHeader("H2"), "HEADER2");
        assertEquals(reconstructedJwt.getJwt().getHeader("typ"), "JWT");
        assertEquals(reconstructedJwt.getJwt().contentKeySet().size(), 5);
        assertEquals(reconstructedJwt.getJwt().getContent("KEY1"), "VALUE1");
        assertEquals(reconstructedJwt.getJwt().getContent("KEY2"), Boolean.TRUE);
        assertEquals(reconstructedJwt.getJwt().getContent("KEY3"), Integer.valueOf(123));
        assertEquals(reconstructedJwt.getJwt().getContent("KEY4"), Long.valueOf(1362397486273L));
        assertEquals(reconstructedJwt.getJwt().getContent("KEY5"), Double.valueOf(45.43));
    }

    @Test (expectedExceptions = UnsupportedOperationException.class)
    public void shouldThrowUnsupportedOperationExceptionWhenEncryptingJwt() {

        //Given
        Map<String, Object> jwtValues = new HashMap<String, Object>();
        jwtValues.put("KEY2", true);
        jwtValues.put("KEY3", 123);
        jwtValues.put("KEY4", 1362397486273L);
        jwtValues.put("KEY5", 45.43);

        PlaintextJwt plaintextJwt = jwtBuilder.jwt()
                .header("H1", "HEADER1")
                .header("H2", "HEADER2")
                .content("KEY1", "VALUE1")
                .content(jwtValues);

        //When
        plaintextJwt.encrypt();

        //Then
        fail();
    }

    @Test (expectedExceptions = UnsupportedOperationException.class)
    public void shouldThrowUnsupportedOperationExceptionWhenEncryptingSignedJwt() throws KeyStoreException,
            CertificateException, NoSuchAlgorithmException, IOException, UnrecoverableKeyException {

        //Given
        PrivateKey key = getPrivateKey("jwt-test-ks", "password".toCharArray());
        Map<String, Object> jwtValues = new HashMap<String, Object>();
        jwtValues.put("KEY2", true);
        jwtValues.put("KEY3", 123);
        jwtValues.put("KEY4", 1362397486273L);
        jwtValues.put("KEY5", 45.43);

        SignedJwt signedJwt = jwtBuilder.jwt()
                .header("H1", "HEADER1")
                .header("H2", "HEADER2")
                .content("KEY1", "VALUE1")
                .content(jwtValues)
                .sign(JwsAlgorithm.HS256, key);

        //When
        signedJwt.encrypt();

        //Then
        fail();
    }
}
