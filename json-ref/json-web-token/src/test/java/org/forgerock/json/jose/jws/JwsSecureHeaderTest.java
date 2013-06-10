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

package org.forgerock.json.jose.jws;

import org.forgerock.json.jose.jwk.JWK;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class JwsSecureHeaderTest {

    @Test
    public void shouldSetJwkSetUrl() throws MalformedURLException {

        //Given
        JwsHeader header = new JwsHeader();
        URL jwtSetUrl = new URL("https://example.com");

        //When
        header.setJwkSetUrl(jwtSetUrl);

        //Then
        assertTrue(header.get("jku").required().isString());
        assertEquals(header.get("jku").asString(), "https://example.com");
        assertEquals(new URL(header.get("jku").asString()), jwtSetUrl);
    }

    @Test
    public void shouldGetJwkSetUrl() throws MalformedURLException {

        //Given
        JwsHeader header = new JwsHeader();
        URL jwtSetUrl = new URL("https://example.com");
        header.setHeader("jku", jwtSetUrl);

        //When
        URL actualJwtSetUrl = header.getJwkSetUrl();

        //Then
        assertEquals(actualJwtSetUrl, jwtSetUrl);
    }

//    @Test            //TODO
//    public void shouldSetJsonWebKey() throws MalformedURLException {
//
//        //Given
//        JwsHeader header = new JwsHeader();
//        JWK jsonWebKey = new JWK() {
//            @Override
//            public String toString() {
//                return "JWK";
//            }
//        };
//
//        //When
//        header.setJsonWebKey(jsonWebKey);
//
//        //Then
//        assertTrue(header.get("jwk").required().isString());      //TODO instead of checking is string here, do it after calling toString on jsonValue and checking for presence of expected string...
//        assertEquals(header.get("jwk").asString(), "JWK");
//    }

    @Test
    public void shouldGetJsonWebKey() throws MalformedURLException {

        //Given
        JwsHeader header = new JwsHeader();
        JWK jsonWebKey = new JWK() {
            @Override
            public String toString() {
                return "JWK";
            }
        };
        header.setHeader("jwk", jsonWebKey);

        //When
        JWK actualJsonWebKey = header.getJsonWebKey();

        //Then
        assertEquals(actualJsonWebKey, jsonWebKey);
    }

    @Test
    public void shouldSetX509Url() throws MalformedURLException {

        //Given
        JwsHeader header = new JwsHeader();
        URL x509Url = new URL("https://example.com");

        //When
        header.setX509Url(x509Url);

        //Then
        assertTrue(header.get("x5u").required().isString());
        assertEquals(header.get("x5u").asString(), "https://example.com");
        assertEquals(new URL(header.get("x5u").asString()), x509Url);
    }

    @Test
    public void shouldGetX509Url() throws MalformedURLException {

        //Given
        JwsHeader header = new JwsHeader();
        URL x509Url = new URL("https://example.com");
        header.setHeader("x5u", x509Url);

        //When
        URL actualX509Url = header.getX509Url();

        //Then
        assertEquals(actualX509Url, x509Url);
    }

    @Test
    public void shouldSetX509CertificateThumbprint() throws MalformedURLException {

        //Given
        JwsHeader header = new JwsHeader();

        //When
        header.setX509CertificateThumbprint("CERT_THUMBPRINT");

        //Then
        assertTrue(header.get("x5t").required().isString());
        assertEquals(header.get("x5t").asString(), "CERT_THUMBPRINT");
    }

    @Test
    public void shouldGetX509CertificateThumbprint() throws MalformedURLException {

        //Given
        JwsHeader header = new JwsHeader();
        header.setHeader("x5t", "CERT_THUMBPRINT");

        //When
        String actualX509CertificateThumbprint = header.getX509CertificateThumbprint();

        //Then
        assertEquals(actualX509CertificateThumbprint, "CERT_THUMBPRINT");
    }

    @Test
    public void shouldSetX509CertificateChain() throws MalformedURLException {

        //Given
        JwsHeader header = new JwsHeader();
        List<String> x509CertificateChain = new ArrayList<String>();
        x509CertificateChain.add("CERT_CHAIN1");
        x509CertificateChain.add("CERT_CHAIN2");

        //When
        header.setX509CertificateChain(x509CertificateChain);

        //Then
        assertTrue(header.get("x5c").required().isList());
        assertEquals(header.get("x5c").asList(String.class).size(), 2);
        assertTrue(header.get("x5c").asList(String.class).contains("CERT_CHAIN1"));
        assertTrue(header.get("x5c").asList(String.class).contains("CERT_CHAIN2"));
    }

    @Test
    public void shouldGetX509CertificateChain() throws MalformedURLException {

        //Given
        JwsHeader header = new JwsHeader();
        List<String> x509CertificateChain = new ArrayList<String>();
        x509CertificateChain.add("CERT_CHAIN1");
        x509CertificateChain.add("CERT_CHAIN2");
        header.setHeader("x5c", x509CertificateChain);

        //When
        List<String> actualX509CertificateChain = header.getX509CertificateChain();

        //Then
        assertEquals(actualX509CertificateChain, x509CertificateChain);
    }

    @Test
    public void shouldSetKeyId() throws MalformedURLException {

        //Given
        JwsHeader header = new JwsHeader();

        //When
        header.setKeyId("KEY_ID");

        //Then
        assertTrue(header.get("kid").required().isString());
        assertEquals(header.get("kid").asString(), "KEY_ID");
    }

    @Test
    public void shouldGetKeyId() throws MalformedURLException {

        //Given
        JwsHeader header = new JwsHeader();
        header.setHeader("kid", "KEY_ID");

        //When
        String actualKeyId = header.getKeyId();

        //Then
        assertEquals(actualKeyId, "KEY_ID");
    }

    @Test
    public void shouldSetContentType() throws MalformedURLException {

        //Given
        JwsHeader header = new JwsHeader();

        //When
        header.setContentType("CONTENT_TYPE");

        //Then
        assertTrue(header.get("cty").required().isString());
        assertEquals(header.get("cty").asString(), "CONTENT_TYPE");
    }

    @Test
    public void shouldGetContentType() throws MalformedURLException {

        //Given
        JwsHeader header = new JwsHeader();
        header.setHeader("cty", "CONTENT_TYPE");

        //When
        String actualContentType = header.getContentType();

        //Then
        assertEquals(actualContentType, "CONTENT_TYPE");
    }

//    @Test        //TODO
//    public void shouldSetCriticalHeaders() throws MalformedURLException {
//
//        //Given
//        JwsHeader header = new JwsHeader();
//        List<String> criticalHeaders = new ArrayList<String>();
//        criticalHeaders.add("CRITICAL_HEADER1");
//        criticalHeaders.add("CRITICAL_HEADER2");
//
//        //When
//        header.setX509CertificateChain(criticalHeaders);
//
//        //Then
//        assertTrue(header.get("crit").required().isList());
//        assertEquals(header.get("crit").asList(String.class).size(), 2);
//        assertTrue(header.get("crit").asList(String.class).contains("CRITICAL_HEADER1"));
//        assertTrue(header.get("crit").asList(String.class).contains("CRITICAL_HEADER2"));
//    }

    @Test
    public void shouldGetCriticalHeaders() throws MalformedURLException {

        //Given
        JwsHeader header = new JwsHeader();
        List<String> criticalHeaders = new ArrayList<String>();
        criticalHeaders.add("CRITICAL_HEADER1");
        criticalHeaders.add("CRITICAL_HEADER2");
        header.setHeader("crit", criticalHeaders);

        //When
        List<String> actualCriticalHeaders = header.getCriticalHeaders();

        //Then
        assertEquals(actualCriticalHeaders, criticalHeaders);
    }

//    @Test        //TODO
//    public void shouldGetHeaders() throws MalformedURLException {
//
//        //Given
//        JwsHeader header = new JwsHeader();
//        URL jwtSetUrl = new URL("https://example.com");
//        header.put("jku", jwtSetUrl);
//        JWK jsonWebKey = new JWK() {
//            @Override
//            public String toString() {
//                return "JWK";
//            }
//        };
//        header.put("jwk", jsonWebKey);
//        URL x509Url = new URL("https://example.com");
//        header.put("x5u", x509Url);
//        header.put("x5t", "X509_CERTIFICATE_CHAIN");
//        List<String> x509CertificateChain = new ArrayList<String>();
//        x509CertificateChain.add("CERT_CHAIN");
//        header.put("x5c", x509CertificateChain);
//        header.put("kid", "KEY_ID");
//        header.put("cty", "CONTENT_TYPE");
//        List<String> criticalHeaders = new ArrayList<String>();
//        criticalHeaders.add("CRITICAL_HEADER");
//        header.put("crit", criticalHeaders);
//        header.put("KEY1", "HEADER1");
//        header.put("KEY2", true);
//        header.put("KEY3", 1234L);
//        header.put("KEY4", 1234);
//
//        //When
//        Object jku = header.getHeader("jku");
//        Object jwk = header.getHeader("jwk");
//        Object x5u = header.getHeader("x5u");
//        Object x5t = header.getHeader("x5t");
//        Object x5c = header.getHeader("x5c");
//        Object kid = header.getHeader("kid");
//        Object cty = header.getHeader("cty");
//        Object crit = header.getHeader("crit");
//        Object key1 = header.getHeader("KEY1");
//        Object key2 = header.getHeader("KEY2");
//        Object key3 = header.getHeader("KEY3");
//        Object key4 = header.getHeader("KEY4");
//
//        //Then
//        assertEquals(jku, jwtSetUrl);
//        assertEquals(jwk, jsonWebKey);
//        assertEquals(x5u, x509Url);
//        assertEquals(x5t, "CERT_THUMBPRINT");
//        assertEquals(x5c, x509CertificateChain);
//        assertEquals(kid, "KEY_ID");
//        assertEquals(cty, "CONTENT_TYPE");
//        assertEquals(crit, criticalHeaders);
//        assertEquals(key1, "HEADER1");
//        assertEquals(key2, true);
//        assertEquals(key3, 1234L);
//        assertEquals(key4, 1234);
//    }

//    @Test    //TODO
//    public void shouldCreateJwsHeaderWithMap() throws MalformedURLException {
//
//        //Given
//        Map<String, Object> headers = new HashMap<String, Object>();
//        URL jwtSetUrl = new URL("https://example.com");
//        headers.put("jku", jwtSetUrl);
//        JWK jsonWebKey = new JWK() {
//            @Override
//            public String toString() {
//                return "JWK";
//            }
//        };
//        headers.put("jwk", jsonWebKey);
//        URL x509Url = new URL("https://example.com");
//        headers.put("x5u", x509Url);
//        headers.put("x5t", "X509_CERTIFICATE_CHAIN");
//        List<String> x509CertificateChain = new ArrayList<String>();
//        x509CertificateChain.add("CERT_CHAIN");
//        headers.put("x5c", x509CertificateChain);
//        headers.put("kid", "KEY_ID");
//        headers.put("cty", "CONTENT_TYPE");
//        List<String> criticalHeaders = new ArrayList<String>();
//        criticalHeaders.add("CRITICAL_HEADER");
//        headers.put("crit", criticalHeaders);
//        headers.put("KEY1", "HEADER1");
//        headers.put("KEY2", true);
//        headers.put("KEY3", 1234L);
//        headers.put("KEY4", 1234);
//
//        //When
//        JwsHeader header = new JwsHeader(headers);
//
//        //Then
//        assertTrue(header.get("jku").required().isString());
//        assertEquals(header.get("jku").asString(), "https://example.com");
//        assertEquals(new URL(header.get("jku").asString()), jwtSetUrl);
//
//        assertTrue(header.get("jwk").required().isString());
//        assertEquals(header.get("jwk").asString(), "JWK");
//
//        assertTrue(header.get("x5u").required().isString());
//        assertEquals(header.get("x5u").asString(), "https://example.com");
//        assertEquals(new URL(header.get("x5u").asString()), x509Url);
//
//        assertTrue(header.get("x5t").required().isString());
//        assertEquals(header.get("x5t").asString(), "CERT_THUMBPRINT");
//
//        assertTrue(header.get("x5c").required().isList());
//        assertEquals(header.get("x5c").asList(String.class).size(), 1);
//        assertTrue(header.get("x5c").asList(String.class).contains("CERT_CHAIN"));
//
//        assertTrue(header.get("kid").required().isString());
//        assertEquals(header.get("kid").asString(), "KEY_ID");
//
//        assertTrue(header.get("cty").required().isString());
//        assertEquals(header.get("cty").asString(), "CONTENT_TYPE");
//
//        assertTrue(header.get("crit").required().isList());
//        assertEquals(header.get("crit").asList(String.class).size(), 1);
//        assertTrue(header.get("crit").asList(String.class).contains("CRITICAL_HEADER"));
//
//        assertTrue(header.isDefined("KEY1"));
//        assertTrue(header.get("KEY1").required().isString());
//        assertEquals(header.get("KEY1").asString(), "HEADER1");
//
//        assertTrue(header.isDefined("KEY2"));
//        assertTrue(header.get("KEY2").required().isBoolean());
//        assertEquals(header.get("KEY2").asBoolean(), (Boolean) true);
//
//        assertTrue(header.isDefined("KEY3"));
//        assertTrue(header.get("KEY3").required().isNumber());
//        assertEquals(header.get("KEY3").asLong(), (Long) 1234L);
//
//        assertTrue(header.isDefined("KEY4"));
//        assertTrue(header.get("KEY4").required().isNumber());
//        assertEquals(header.get("KEY4").asInteger(), (Integer) 1234);
//    }
}
