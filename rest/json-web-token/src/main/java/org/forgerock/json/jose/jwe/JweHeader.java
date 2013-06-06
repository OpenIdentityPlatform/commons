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

package org.forgerock.json.jose.jwe;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.jose.jws.JwtSecureHeader;

import java.util.Map;

public class JweHeader extends JwtSecureHeader {

    private static final String ENCRYPTION_METHOD_HEADER_KEY = "enc";
    private static final String EPHEMERAL_PUBLIC_KEY_HEADER_KEY = "epk";
    private static final String COMPRESSION_ALGORITHM_HEADER_KEY = "zip";
    private static final String AGREEMENT_PARTY_UINFO_HEADER_KEY = "apu";   //Base64url

    public JweHeader() {
        super();
    }

    public JweHeader(JsonValue value) {
        super(value);
    }

    public JweHeader(Map<String, Object> headerParameters) {
        super(headerParameters);
    }

    @Override
    public JweAlgorithm getAlgorithm() {
        return JweAlgorithm.valueOf(getAlgorithmString());
    }

    public void setEncryptionMethod(EncryptionMethod encryptionMethod) {
        put(ENCRYPTION_METHOD_HEADER_KEY, encryptionMethod.toString());
    }

    public EncryptionMethod getEncryptionMethod() {
        return EncryptionMethod.valueOf(get(ENCRYPTION_METHOD_HEADER_KEY).asString());
    }

    public void setEphemeralPublicKey(String ephemeralPublicKey) {
        put(EPHEMERAL_PUBLIC_KEY_HEADER_KEY, ephemeralPublicKey);
    }

    public String getEphemeralPublicKey() {
        return get(EPHEMERAL_PUBLIC_KEY_HEADER_KEY).asString();
    }

    public void setCompressionAlgorithm(CompressionAlgorithm compressionAlgorithm) {
        put(COMPRESSION_ALGORITHM_HEADER_KEY, compressionAlgorithm.toString());
    }

    public CompressionAlgorithm getCompressionAlgorithm() {
        String compressionAlgorithm = get(COMPRESSION_ALGORITHM_HEADER_KEY).asString();
        if (compressionAlgorithm == null) {
            return null;
        } else {
            return CompressionAlgorithm.valueOf(compressionAlgorithm);
        }
    }

    public void setAgreementPartyUInfo(String agreementPartyUInfo) {
        put(AGREEMENT_PARTY_UINFO_HEADER_KEY, agreementPartyUInfo);
    }

    public String getAgreementPartyUInfo() {
        return get(AGREEMENT_PARTY_UINFO_HEADER_KEY).asString();
    }
}
