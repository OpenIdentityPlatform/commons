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
import org.forgerock.json.jose.jwt.JwtHeader;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static org.forgerock.json.jose.jws.JwsHeaderKey.*;

public abstract class JwtSecureHeader extends JwtHeader {

    public JwtSecureHeader() {
    }

    public JwtSecureHeader(Map<String, Object> headers) {
        setHeaders(headers);
    }

    public void setJwkSetUrl(URL jwkSetUrl) {
        put(JKU.value(), new String(jwkSetUrl.toString()));  //TODO JsonValue has problems with to String not being a "String"?!?!
    }

    public URL getJwkSetUrl() {
        try {
            return get(JKU.value()).asURI().toURL();
        } catch (MalformedURLException e) {
            //TODO
            throw new RuntimeException(e);
        }
    }

    public void setJsonWebKey(JWK jsonWebKey) {
        put(JWK.value(), jsonWebKey);
    }

    public JWK getJsonWebKey() { //TODO need custom mapper for jackson! or do it here??
        return (JWK) get(JWK.value()).getObject();
    }

    public void setX509Url(URL x509Url) {
        put(X5U.value(), new String(x509Url.toString()));
    }

    public URL getX509Url() {
        try {
            return get(X5U.value()).asURI().toURL();
        } catch (MalformedURLException e) {
            //TODO
            throw new RuntimeException(e);
        }
    }

    public void setX509CertificateThumbprint(String x509CertificateThumbprint) {
        put(X5T.value(), x509CertificateThumbprint);
    }

    public String getX509CertificateThumbprint() {
        return get(X5T.value()).asString();
    }

    public void setX509CertificateChain(List<String> x509CertificateChain) {
        put(X5C.value(), x509CertificateChain);
    }

    public List<String> getX509CertificateChain() {
        return get(X5C.value()).asList(String.class);
    }

    public void setKeyId(String keyId) {
        put(KID.value(), keyId);
    }

    public String getKeyId() {
        return get(KID.value()).asString();
    }

    public void setContentType(String contentType) {
        put(CTY.value(), contentType);
    }

    public String getContentType() {
        return get(CTY.value()).asString();
    }

    public void setCriticalHeaders(List<String> criticalHeaders) {
        put(CRIT.value(), criticalHeaders);
    }

    public List<String> getCriticalHeaders() {
        return get(CRIT.value()).asList(String.class);
    }

    @Override
    public void setHeader(String key, Object value) {
        JwsHeaderKey headerKey = getHeaderKey(key.toUpperCase());

        switch (headerKey) {
            case JKU: {
                checkValueIsOfType(value, URL.class);
                setJwkSetUrl((URL) value);
                break;
            }
            case JWK: {
                checkValueIsOfType(value, JWK.class);
                setJsonWebKey((JWK) value);
                break;
            }
            case X5U: {
                checkValueIsOfType(value, URL.class);
                setX509Url((URL) value);
                break;
            }
            case X5T: {
                checkValueIsOfType(value, String.class);
                setX509CertificateThumbprint((String) value);
                break;
            }
            case X5C: {
                checkValueIsOfType(value, List.class);
                checkListValuesAreOfType((List) value, String.class);
                setX509CertificateChain((List<String>) value);
                break;
            }
            case KID: {
                checkValueIsOfType(value, String.class);
                setKeyId((String) value);
                break;
            }
            case CTY: {
                checkValueIsOfType(value, String.class);
                setContentType((String) value);
                break;
            }
            case CRIT: {
                checkValueIsOfType(value, List.class);
                checkListValuesAreOfType((List) value, String.class);
                setCriticalHeaders((List<String>) value);
                break;
            }
            default: {
                super.setHeader(key, value);
            }
        }
    }

    @Override
    public Object getHeader(String key) {
        JwsHeaderKey headerKey = getHeaderKey(key.toUpperCase());

        Object value;

        switch (headerKey) {
            case JKU: {
                value = getJwkSetUrl();
                break;
            }
            case JWK: {
                value = getJsonWebKey();
                break;
            }
            case X5U: {
                value = getX509Url();
                break;
            }
            case X5T: {
                value = getX509CertificateThumbprint();
                break;
            }
            case X5C: {
                value = getX509CertificateChain();
                break;
            }
            case KID: {
                value = getKeyId();
                break;
            }
            case CTY: {
                value = getContentType();
                break;
            }
            case CRIT: {
                value = getCriticalHeaders();
                break;
            }
            default: {
                value = super.getHeader(key);
            }
        }

        return value;
    }
}
