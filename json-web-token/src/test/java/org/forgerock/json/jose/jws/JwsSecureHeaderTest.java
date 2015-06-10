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

package org.forgerock.json.jose.jws;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwk.KeyUse;
import org.forgerock.json.jose.jwk.OctJWK;
import org.forgerock.json.jose.utils.Utils;
import org.forgerock.util.encode.Base64;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
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
        header.setParameter("jku", jwtSetUrl);

        //When
        URL actualJwtSetUrl = header.getJwkSetUrl();

        //Then
        assertEquals(actualJwtSetUrl, jwtSetUrl);
    }

    @Test
    public void shouldGetJsonWebKey() {

        //Given
        JwsHeader header = new JwsHeader();
        JWK jsonWebKey = new OctJWK(KeyUse.ENC, "", "1", "5", null, null, null);
        header.setParameter("jwk", jsonWebKey);

        //When
        JWK actualJsonWebKey = header.getJsonWebKey();

        //Then
        assertEquals(actualJsonWebKey.toString(), jsonWebKey.toString());
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
        header.setParameter("x5u", x509Url);

        //When
        URL actualX509Url = header.getX509Url();

        //Then
        assertEquals(actualX509Url, x509Url);
    }

    @Test
    public void shouldSetX509CertificateThumbprint() {

        //Given
        JwsHeader header = new JwsHeader();

        //When
        header.setX509CertificateThumbprint("CERT_THUMBPRINT");

        //Then
        assertTrue(header.get("x5t").required().isString());
        assertEquals(header.get("x5t").asString(), Base64.encode("CERT_THUMBPRINT".getBytes(Utils.CHARSET)));
    }

    @Test
    public void shouldGetX509CertificateThumbprint() {

        //Given
        JwsHeader header = new JwsHeader();
        header.setParameter("x5t", "CERT_THUMBPRINT");

        //When
        String actualX509CertificateThumbprint = header.getX509CertificateThumbprint();

        //Then
        assertEquals(actualX509CertificateThumbprint, Base64.encode("CERT_THUMBPRINT".getBytes(Utils.CHARSET)));
    }

    @Test
    public void shouldSetX509CertificateChain() {

        //Given
        JwsHeader header = new JwsHeader();
        List<String> x509CertificateChain = new ArrayList<>();
        x509CertificateChain.add("CERT_CHAIN1");
        x509CertificateChain.add("CERT_CHAIN2");

        //When
        header.setX509CertificateChain(x509CertificateChain);

        //Then
        assertTrue(header.get("x5c").required().isList());
        assertEquals(header.get("x5c").asList(String.class).size(), 2);
        assertTrue(header.get("x5c").asList(String.class)
                .contains(Base64.encode("CERT_CHAIN1".getBytes(Utils.CHARSET))));
        assertTrue(header.get("x5c").asList(String.class)
                .contains(Base64.encode("CERT_CHAIN2".getBytes(Utils.CHARSET))));
    }

    @Test
    public void shouldGetX509CertificateChain() {

        //Given
        JwsHeader header = new JwsHeader();
        List<String> x509CertificateChain = new ArrayList<>();
        x509CertificateChain.add("CERT_CHAIN1");
        x509CertificateChain.add("CERT_CHAIN2");
        List<String> base64EncodedX509CertificateChain = new ArrayList<>();
        base64EncodedX509CertificateChain.add(Base64.encode("CERT_CHAIN1".getBytes(Utils.CHARSET)));
        base64EncodedX509CertificateChain.add(Base64.encode("CERT_CHAIN2".getBytes(Utils.CHARSET)));
        header.setParameter("x5c", x509CertificateChain);

        //When
        List<String> actualX509CertificateChain = header.getX509CertificateChain();

        //Then
        assertEquals(actualX509CertificateChain, base64EncodedX509CertificateChain);
    }

    @Test
    public void shouldSetKeyId() {

        //Given
        JwsHeader header = new JwsHeader();

        //When
        header.setKeyId("KEY_ID");

        //Then
        assertTrue(header.get("kid").required().isString());
        assertEquals(header.get("kid").asString(), "KEY_ID");
    }

    @Test
    public void shouldGetKeyId() {

        //Given
        JwsHeader header = new JwsHeader();
        header.setParameter("kid", "KEY_ID");

        //When
        String actualKeyId = header.getKeyId();

        //Then
        assertEquals(actualKeyId, "KEY_ID");
    }

    @Test
    public void shouldSetContentType() {

        //Given
        JwsHeader header = new JwsHeader();

        //When
        header.setContentType("CONTENT_TYPE");

        //Then
        assertTrue(header.get("cty").required().isString());
        assertEquals(header.get("cty").asString(), "CONTENT_TYPE");
    }

    @Test
    public void shouldGetContentType() {

        //Given
        JwsHeader header = new JwsHeader();
        header.setParameter("cty", "CONTENT_TYPE");

        //When
        String actualContentType = header.getContentType();

        //Then
        assertEquals(actualContentType, "CONTENT_TYPE");
    }

    @Test
    public void shouldGetCriticalHeaders() {

        //Given
        JwsHeader header = new JwsHeader();
        List<String> criticalHeaders = new ArrayList<>();
        criticalHeaders.add("CRITICAL_HEADER1");
        criticalHeaders.add("CRITICAL_HEADER2");
        header.setParameter("crit", criticalHeaders);

        //When
        List<String> actualCriticalHeaders = header.getCriticalHeaders();

        //Then
        assertEquals(actualCriticalHeaders, criticalHeaders);
    }
}
