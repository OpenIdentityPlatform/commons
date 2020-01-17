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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.json.jose.jws;

import static org.forgerock.json.jose.jws.JwsHeaderKey.CRIT;
import static org.forgerock.json.jose.jws.JwsHeaderKey.CTY;
import static org.forgerock.json.jose.jws.JwsHeaderKey.JKU;
import static org.forgerock.json.jose.jws.JwsHeaderKey.JWK;
import static org.forgerock.json.jose.jws.JwsHeaderKey.KID;
import static org.forgerock.json.jose.jws.JwsHeaderKey.X5C;
import static org.forgerock.json.jose.jws.JwsHeaderKey.X5T;
import static org.forgerock.json.jose.jws.JwsHeaderKey.X5U;
import static org.forgerock.json.jose.jws.JwsHeaderKey.getHeaderKey;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.forgerock.json.jose.exceptions.JwtRuntimeException;
import org.forgerock.json.jose.jwe.CompressionAlgorithm;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwt.JwtHeader;
import org.forgerock.json.jose.utils.Utils;
import org.forgerock.util.encode.Base64;

/**
 * A base implementation for the common security header parameters shared by the JWS and JWE headers.
 *
 * @since 2.0.0
 */
public abstract class JwtSecureHeader extends JwtHeader {
    private static final String COMPRESSION_ALGORITHM_HEADER_KEY = "zip";

    /**
     * Constructs a new, empty JwtSecureHeader.
     */
    public JwtSecureHeader() {
    }

    /**
     * Constructs a new JwtSecureHeader, with its parameters set to the contents of the given Map.
     *
     * @param headers A Map containing the parameters to be set in the header.
     */
    public JwtSecureHeader(Map<String, Object> headers) {
        setParameters(headers);
    }

    /**
     * Sets the JWK Set URL header parameter for this JWS.
     * <p>
     * A URI that refers to a resource for a set of JSON-encoded public keys, one of which corresponds to the key used
     * to digitally sign the JWS.
     * <p>
     * The keys MUST be encoded as a JSON Web Key Set (JWK Set).
     * <p>
     * The protocol used to acquire the resource MUST provide integrity protection and the identity of the server MUST
     * be validated.
     *
     * @param jwkSetUrl The JWK Set URL.
     */
    public void setJwkSetUrl(URL jwkSetUrl) {
        put(JKU.value(), new String(jwkSetUrl.toString()));
    }

    /**
     * Gets the JWK Set URL header parameter for this JWS.
     *
     * @return The JWK Set URL.
     */
    public URL getJwkSetUrl() {
        try {
            String url = get(JKU.value()).asString();
            return url != null
                    ? new URL(url)
                    : null;
        } catch (MalformedURLException e) {
            throw new JwtRuntimeException(e);
        }
    }

    /**
     * Sets the JSON Web Key header parameter for this JWS.
     * <p>
     * The public key that corresponds to the key used to digitally sign the JWS. This key is represented as a JSON Web
     * Key (JWK).
     *
     * @param jsonWebKey The JSON Web Key.
     */
    public void setJsonWebKey(JWK jsonWebKey) {
        put(JWK.value(), jsonWebKey);
    }

    /**
     * Gets the JSON Web Key header parameter for this JWS.
     *
     * @return The JSON Web Key.
     */
    public JWK getJsonWebKey() {
        return (JWK) get(JWK.value()).getObject();
    }

    /**
     * Sets the X.509 URL header parameter for this JWS.
     * <p>
     * A URI that refers to a resource for the X.509 public key certificate or certificate chain corresponding to the
     * key used to digitally sign the JWS.
     * <p>
     * The certificate containing the public key corresponding to the key used to digitally sign the JWS MUST be the
     * first certificate. This MAY be followed by additional certificates, with each subsequent certificate being the
     * one used to certify the previous one.
     * <p>
     * The protocol used to acquire the resource MUST provide integrity protection and the identity of the server MUST
     * be validated.
     *
     * @param x509Url The X.509 URL.
     */
    public void setX509Url(URL x509Url) {
        put(X5U.value(), new String(x509Url.toString()));
    }

    /**
     * Gets the X.509 URL header parameter for this JWS.
     *
     * @return The X.509 URL.
     */
    public URL getX509Url() {
        try {
            String url = get(X5U.value()).asString();
            return url != null
                    ? new URL(url)
                    : null;
        } catch (MalformedURLException e) {
            throw new JwtRuntimeException(e);
        }
    }

    /**
     * Sets the X.509 Certificate Thumbprint header parameter for this JWS.
     * <p>
     * A base64url encoded SHA-1 thumbprint (a.k.a. digest) of the DER encoding of the X.509 certificate corresponding
     * to the key used to digitally sign the JWS.
     * <p>
     * This method will perform the base64url encoding so the x509CertificateThumbprint must be the SHA-1 digest.
     *
     * @param x509CertificateThumbprint The X.509 Certificate Thumbprint.
     */
    public void setX509CertificateThumbprint(String x509CertificateThumbprint) {
        put(X5T.value(), Utils.base64urlEncode(x509CertificateThumbprint));
    }

    /**
     * Gets the X.509 Certificate Thumbprint header parameter for this JWS.
     *
     * @return The X.509 Certificate Thumbprint.
     */
    public String getX509CertificateThumbprint() {
        return get(X5T.value()).asString();
    }

    /**
     * Sets the X.509 Certificate Chain header parameter for this JWS.
     * <p>
     * Contains the list of X.509 public key certificate or certificate chain corresponding to the key used to
     * digitally sign the JWS.
     * Each entry in the list is a base64 encoded DER PKIX certificate value.
     * This method will perform the base64 encoding of each entry so the entries in the list must be the DER PKIX
     * certificate values.
     * <p>
     * The certificate containing the public key corresponding to the key used to digitally sign the JWS MUST be the
     * first certificate. This MAY be followed by additional certificates, with each subsequent certificate being the
     * one used to certify the previous one.
     * <p>
     *
     * @param x509CertificateChain The X.509 Certificate Chain.
     */
    public void setX509CertificateChain(List<String> x509CertificateChain) {
        List<String> encodedCertChain = new ArrayList<>();
        for (String x509Cert : x509CertificateChain) {
            encodedCertChain.add(Base64.encode(x509Cert.getBytes(Utils.CHARSET)));
        }
        put(X5C.value(), encodedCertChain);
    }

    /**
     * Gets the X.509 Certificate Chain header parameter for this JWS.
     *
     * @return The X.509 Certificate Chain.
     */
    public List<String> getX509CertificateChain() {
        return get(X5C.value()).asList(String.class);
    }

    /**
     * Sets the Key ID header parameter for this JWS.
     * <p>
     * Indicates which key was used to secure the JWS, allowing originators to explicitly signal a change of key to
     * recipients.
     *
     * @param keyId The Key ID.
     */
    public void setKeyId(String keyId) {
        put(KID.value(), keyId);
    }

    /**
     * Gets the Key ID header parameter for this JWS.
     *
     * @return The Key ID.
     */
    public String getKeyId() {
        return get(KID.value()).asString();
    }

    /**
     * Sets the content type header parameter for this JWS.
     * <p>
     * Declares the type of the secured content (the Payload).
     *
     * @param contentType The content type of this JWS' payload.
     */
    public void setContentType(String contentType) {
        put(CTY.value(), contentType);
    }

    /**
     * Gets the content type header parameter for this JWS.
     *
     * @return The content type of this JWS' payload.
     */
    public String getContentType() {
        return get(CTY.value()).asString();
    }

    /**
     * Sets the critical header parameters for this JWS.
     * <p>
     * This header parameter indicates that extensions to the JWS specification are being used that MUST be understood
     * and processed.
     * <p>
     * The criticalHeaders parameter cannot be an empty list.
     *
     * @param criticalHeaders A List of the critical parameters.
     */
    public void setCriticalHeaders(List<String> criticalHeaders) {
        if (criticalHeaders != null && criticalHeaders.isEmpty()) {
            throw new JwtRuntimeException("Critical Headers parameter cannot be an empty list");
        }
        put(CRIT.value(), criticalHeaders);
    }

    /**
     * Gets the critical header parameters for this JWS.
     *
     * @return A List of the critical parameters.
     */
    public List<String> getCriticalHeaders() {
        return get(CRIT.value()).asList(String.class);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void setParameter(String key, Object value) {
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
            checkListValuesAreOfType((List<?>) value, String.class);
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
            checkListValuesAreOfType((List<?>) value, String.class);
            setCriticalHeaders((List<String>) value);
            break;
        }
        default: {
            super.setParameter(key, value);
        }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getParameter(String key) {
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
            value = super.getParameter(key);
        }
        }

        return value;
    }

    /**
     * Sets the Compression Algorithm header parameter for this JWE.
     * <p>
     * If present, the value of the Compression Algorithm header parameter MUST be CompressionAlgorithm constant DEF.
     *
     * @param compressionAlgorithm The Compression Algorithm.
     */
    public void setCompressionAlgorithm(CompressionAlgorithm compressionAlgorithm) {
        put(COMPRESSION_ALGORITHM_HEADER_KEY, compressionAlgorithm.toString());
    }

    /**
     * Gets the Compression Algorithm header parameter for this JWE.
     *
     * @return The Compression Algorithm.
     */
    public CompressionAlgorithm getCompressionAlgorithm() {
        String compressionAlgorithm = get(COMPRESSION_ALGORITHM_HEADER_KEY).asString();
        if (compressionAlgorithm == null) {
            return CompressionAlgorithm.NONE;
        } else {
            return CompressionAlgorithm.valueOf(compressionAlgorithm);
        }
    }

}
