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

package org.forgerock.json.jose.jwe;

import java.util.Map;

import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jws.JwtSecureHeader;

/**
 * An implementation for the JWE Header parameters.
 *
 * @since 2.0.0
 */
public class JweHeader extends JwtSecureHeader {

    private static final String ENCRYPTION_METHOD_HEADER_KEY = "enc";
    private static final String EPHEMERAL_PUBLIC_KEY_HEADER_KEY = "epk";
    private static final String AGREEMENT_PARTY_UINFO_HEADER_KEY = "apu";   //Base64url

    /**
     * Constructs an new, empty JweHeader.
     */
    public JweHeader() {
        super();
    }

    /**
     * Constructs a new JweHeader with its parameters set to the contents of the given Map.
     *
     * @param headerParameters A Map containing the parameters to be set in the header.
     */
    public JweHeader(Map<String, Object> headerParameters)  {
        super(headerParameters);
    }

    /**
     * Gets the Algorithm set in the JWT header.
     * <p>
     * If there is no algorithm set in the JWT header, then the JweAlgorithm NONE will be returned.
     *
     * @return {@inheritDoc}
     */
    @Override
    public JweAlgorithm getAlgorithm() {
        return JweAlgorithm.parseAlgorithm(getAlgorithmString());
    }

    /**
     * Sets the Encryption Method header parameter for this JWE.
     * <p>
     * Identifies the block encryption algorithm used to encrypt the Plaintext to produce the Ciphertext.
     *
     * @param encryptionMethod The Encryption Method.
     */
    public void setEncryptionMethod(EncryptionMethod encryptionMethod) {
        put(ENCRYPTION_METHOD_HEADER_KEY, encryptionMethod.toString());
    }

    /**
     * Gets the Encryption Method header parameter for this JWE.
     *
     * @return The Encryption Method.
     */
    public EncryptionMethod getEncryptionMethod() {
        return EncryptionMethod.parseMethod(get(ENCRYPTION_METHOD_HEADER_KEY).asString());
    }

    /**
     * Sets the Ephemeral Public Key header parameter for this JWE.
     * <p>
     * For use in key agreement algorithms. When the Algorithm header parameter value specified identifies an algorithm
     * for which "epk" is a parameter, this parameter MUST be present if REQUIRED by the algorithm.
     *
     * @param ephemeralPublicKey The Ephemeral Public Key.
     */
    public void setEphemeralPublicKey(JWK ephemeralPublicKey) {
        put(EPHEMERAL_PUBLIC_KEY_HEADER_KEY, ephemeralPublicKey.toString());
    }

    /**
     * Gets the Ephemeral Public Key header parameter for this JWE.
     *
     * @return The Ephemeral Public Key.
     */
    public String getEphemeralPublicKey() {
        return get(EPHEMERAL_PUBLIC_KEY_HEADER_KEY).asString();
    }


    /**
     * Sets the Agreement PartyUInfo header parameter for this JWE.
     * <p>
     * For use with key agreement algorithms (such as "ECDH-ES"), represented as a base64url encoded string.
     * <p>
     * This method will perform the base64url encoding so the agreementPartyUInfo must be the un-encoded String value
     * of the Agreement PartyUInfo.
     *
     * @param agreementPartyUInfo The Agreement PartyUInfo.
     */
    public void setAgreementPartyUInfo(String agreementPartyUInfo) {
        put(AGREEMENT_PARTY_UINFO_HEADER_KEY, agreementPartyUInfo);
    }

    /**
     * Gets the Agreement PartyUInfo header parameter for this JWE.
     *
     * @return The Agreement PartyUInfo.
     */
    public String getAgreementPartyUInfo() {
        return get(AGREEMENT_PARTY_UINFO_HEADER_KEY).asString();
    }

    @Override
    public void setParameter(String key, Object value) {
        JweHeaderKey headerKey = JweHeaderKey.getHeaderKey(key.toUpperCase());

        switch (headerKey) {
        case ENC: {
            if (isValueOfType(value, EncryptionMethod.class)) {
                setEncryptionMethod((EncryptionMethod) value);
            }
            checkValueIsOfType(value, String.class);
            setEncryptionMethod(EncryptionMethod.parseMethod((String) value));
            break;
        }
        case EPK: {
            checkValueIsOfType(value, JWK.class);
            setEphemeralPublicKey((JWK) value);
            break;
        }
        case ZIP: {
            if (isValueOfType(value, CompressionAlgorithm.class)) {
                setCompressionAlgorithm((CompressionAlgorithm) value);
            }
            checkValueIsOfType(value, String.class);
            setCompressionAlgorithm(CompressionAlgorithm.parseAlgorithm((String) value));
            break;
        }
        case APU: {
            checkValueIsOfType(value, String.class);
            setAgreementPartyUInfo((String) value);
            break;
        }
        default: {
            super.setParameter(key, value);
        }
        }
    }

    @Override
    public Object getParameter(String key) {
        JweHeaderKey headerKey = JweHeaderKey.getHeaderKey(key.toUpperCase());

        Object value;

        switch (headerKey) {
        case ENC: {
            value = getEncryptionMethod();
            break;
        }
        case EPK: {
            value = getEphemeralPublicKey();
            break;
        }
        case ZIP: {
            value = getCompressionAlgorithm();
            break;
        }
        case APU: {
            value = getAgreementPartyUInfo();
            break;
        }
        default: {
            value = super.getParameter(key);
        }
        }

        return value;
    }
}
