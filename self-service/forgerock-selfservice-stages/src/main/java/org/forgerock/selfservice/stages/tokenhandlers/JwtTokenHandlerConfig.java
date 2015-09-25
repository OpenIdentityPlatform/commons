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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.selfservice.stages.tokenhandlers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenConfig;

/**
 * Configuration for the {@link JwtTokenHandler}.
 *
 * @since 0.2.0
 */
public final class JwtTokenHandlerConfig implements SnapshotTokenConfig {

    public static final String TYPE = "jwt";

    private final String sharedKey;
    private final String keyPairAlgorithm;

    private final int keyPairSize;
    private final JweAlgorithm jweAlgorithm;
    private final EncryptionMethod encryptionMethod;
    private final JwsAlgorithm jwsAlgorithm;
    private final long tokenLifeTimeInSeconds;

    @JsonCreator
    public JwtTokenHandlerConfig(
            @JsonProperty("sharedKey") String sharedKey,
            @JsonProperty("keyPairAlgorithm") String keyPairAlgorithm,
            @JsonProperty("keyPairSize") int keyPairSize,
            @JsonProperty("jweAlgorithm") JweAlgorithm jweAlgorithm,
            @JsonProperty("encryptionMethod") EncryptionMethod encryptionMethod,
            @JsonProperty("jwsAlgorithm") JwsAlgorithm jwsAlgorithm,
            @JsonProperty("tokenExpiry") long tokenLifeTimeInSeconds) {
        this.sharedKey = sharedKey;
        this.keyPairAlgorithm = keyPairAlgorithm;
        this.keyPairSize = keyPairSize;
        this.jweAlgorithm = jweAlgorithm;
        this.encryptionMethod = encryptionMethod;
        this.jwsAlgorithm = jwsAlgorithm;
        this.tokenLifeTimeInSeconds = tokenLifeTimeInSeconds;

    }

    public String getType() {
        return TYPE;
    }

    public byte[] getSharedKey() {
        return sharedKey.getBytes();
    }

    public String getKeyPairAlgorithm() {
        return keyPairAlgorithm;
    }

    public int getKeyPairSize() {
        return keyPairSize;
    }

    public JweAlgorithm getJweAlgorithm() {
        return jweAlgorithm;
    }

    public EncryptionMethod getEncryptionMethod() {
        return encryptionMethod;
    }

    public JwsAlgorithm getJwsAlgorithm() {
        return jwsAlgorithm;
    }

    public long getTokenLifeTimeInSeconds() {
        return tokenLifeTimeInSeconds;
    }
}
