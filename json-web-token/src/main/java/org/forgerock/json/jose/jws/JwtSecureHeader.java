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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwt.JwtHeader;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

public abstract class JwtSecureHeader extends JwtHeader {

    private static final String JWK_SET_URL_HEADER_KEY = "jku";
    private static final String JWK_HEADER_KEY = "jwk";
    private static final String X509_URL_HEADER_KEY = "x5u";
    private static final String X509_CERTIFICATE_THUMBPRINT_HEADER_KEY = "x5t";     //Base64url
    private static final String X509_CERTIFICATE_CHAIN_HEADER_KEY = "x5c";          //List<Base64>
    private static final String KEY_ID_HEADER_KEY = "kid";
    private static final String CONTENT_TYPE_HEADER_KEY = "cty";
    private static final String CRITICAL_HEADERS_HEADER_KEY = "ctri";

    public JwtSecureHeader() {
        super();
    }

    public JwtSecureHeader(JsonValue value) {
        super(value);
    }

    public JwtSecureHeader(Map<String, Object> headerParameters) {  //TODO need to process reserved values first!!!
        super(headerParameters);
    }

    public void setJwkSetUrl(URL jwkSetUrl) {
        put(JWK_SET_URL_HEADER_KEY, new String(jwkSetUrl.toString()));
    }

    public URL getJwkSetUrl() {
        try {
            return get(JWK_SET_URL_HEADER_KEY).asURI().toURL();
        } catch (MalformedURLException e) {
            //TODO
            throw new RuntimeException(e);
        }
    }

    public void setJsonWebKey(JWK jsonWebKey) {
        put(JWK_HEADER_KEY, jsonWebKey);
    }

    public JWK getJsonWebKey() { //TODO need custom mapper for jackson!
        return (JWK) get(JWK_HEADER_KEY);
    }

    public void setX509Url(URL x509Url) {
        put(X509_URL_HEADER_KEY, new String(x509Url.toString()));
    }

    public URL getX509Url() {
        try {
            return get(X509_URL_HEADER_KEY).asURI().toURL();
        } catch (MalformedURLException e) {
            //TODO
            throw new RuntimeException(e);
        }
    }

    public void setX509CertificateThumbprint(String x509CertificateThumbprint) {
        put(X509_CERTIFICATE_THUMBPRINT_HEADER_KEY, x509CertificateThumbprint);
    }

    public String getX509CertificateThumbprintHeader() {
        return get(X509_CERTIFICATE_THUMBPRINT_HEADER_KEY).asString();
    }

    public void setX509CertificateChain(List<String> x509CertificateChain) {
        put(X509_CERTIFICATE_CHAIN_HEADER_KEY, x509CertificateChain);
    }

    public List<String> getX509CertificateChain() {
        return get(X509_CERTIFICATE_CHAIN_HEADER_KEY).asList(String.class);
    }

    public void setKeyId(String keyId) {
        put(KEY_ID_HEADER_KEY, keyId);
    }

    public String getKeyId() {
        return get(KEY_ID_HEADER_KEY).asString();
    }

    public void setContentType(String contentType) {
        put(CONTENT_TYPE_HEADER_KEY, contentType);
    }

    public String getContentType() {
        return get(CONTENT_TYPE_HEADER_KEY).asString();
    }

    public void setCriticalHeaders(List<String> criticalHeaders) {
        put(CRITICAL_HEADERS_HEADER_KEY, criticalHeaders);
    }

    public List<String> getCriticalHeaders() {
        return get(CRITICAL_HEADERS_HEADER_KEY).asList(String.class);
    }
}
