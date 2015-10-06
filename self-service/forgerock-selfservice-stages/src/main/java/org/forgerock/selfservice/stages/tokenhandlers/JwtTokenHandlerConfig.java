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

    /**
     * Type of the token handler.
     */
    public static final String TYPE = "jwt";

    @JsonProperty
    private String sharedKey;
    @JsonProperty
    private String keyPairAlgorithm;
    @JsonProperty
    private int keyPairSize;
    @JsonProperty
    private JweAlgorithm jweAlgorithm;
    @JsonProperty
    private EncryptionMethod encryptionMethod;
    @JsonProperty
    private JwsAlgorithm jwsAlgorithm;
    @JsonProperty("tokenExpiry")
    private long tokenLifeTimeInSeconds;

    /**
     * Gets token handler type.
     *
     * @return the type
     */
    public String getType() {
        return TYPE;
    }

    /**
     * Gets the shared key.
     *
     * @return the shared key as bytes
     */
    public byte[] getSharedKey() {
        return sharedKey.getBytes();
    }

    /**
     * Gets the key pair algorithm.
     *
     * @return the key pair algorithm
     */
    public String getKeyPairAlgorithm() {
        return keyPairAlgorithm;
    }

    /**
     * Gets the key pair size.
     *
     * @return the key pair size
     */
    public int getKeyPairSize() {
        return keyPairSize;
    }

    /**
     * Gets the jwe algorithm.
     *
     * @return the jwe algorithm
     */
    public JweAlgorithm getJweAlgorithm() {
        return jweAlgorithm;
    }

    /**
     * Gets the encryption method.
     *
     * @return the encryption method
     */
    public EncryptionMethod getEncryptionMethod() {
        return encryptionMethod;
    }

    /**
     * Gets the jws algorithm.
     *
     * @return the jws algorithm
     */
    public JwsAlgorithm getJwsAlgorithm() {
        return jwsAlgorithm;
    }

    /**
     * Gets the token life time.
     *
     * @return the token life time in seconds
     */
    public long getTokenLifeTimeInSeconds() {

        return tokenLifeTimeInSeconds;
    }

    /**
     * Set the shared key.
     *
     * @param sharedKey the shared key
     */
    public void setSharedKey(String sharedKey) {
        this.sharedKey = sharedKey;
    }

    /**
     * Set the key pair algorithm.
     *
     * @param keyPairAlgorithm the key pair algorithm
     */
    public void setKeyPairAlgorithm(String keyPairAlgorithm) {
        this.keyPairAlgorithm = keyPairAlgorithm;
    }

    /**
     * Set the key pair size..
     *
     * @param keyPairSize the key pair size
     */
    public void setKeyPairSize(int keyPairSize) {
        this.keyPairSize = keyPairSize;
    }

    /**
     * Set the Jwe algorithm.
     *
     * @param jweAlgorithm the JweAlgorithm
     */
    public void setJweAlgorithm(JweAlgorithm jweAlgorithm) {
        this.jweAlgorithm = jweAlgorithm;
    }

    /**
     * Set the encryption method.
     *
     * @param encryptionMethod the encrpytion method
     */
    public void setEncryptionMethod(EncryptionMethod encryptionMethod) {
        this.encryptionMethod = encryptionMethod;
    }

    /**
     * Set the Jws algorithm.
     *
     * @param jwsAlgorithm the JwsAlgorithm
     */
    public void setJwsAlgorithm(JwsAlgorithm jwsAlgorithm) {
        this.jwsAlgorithm = jwsAlgorithm;
    }

    /**
     * Set the token life (seconds).
     *
     * @param tokenLifeTimeInSeconds the token life
     */
    public void setTokenLifeTimeInSeconds(long tokenLifeTimeInSeconds) {
        this.tokenLifeTimeInSeconds = tokenLifeTimeInSeconds;
    }
}
