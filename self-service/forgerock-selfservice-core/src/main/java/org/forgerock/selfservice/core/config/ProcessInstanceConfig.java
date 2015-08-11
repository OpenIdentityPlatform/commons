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

package org.forgerock.selfservice.core.config;

import org.forgerock.selfservice.core.snapshot.SnapshotTokenType;
import org.forgerock.util.Reject;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the configuration for an instance of the anonymous process service.
 *
 * @since 0.1.0
 */
public final class ProcessInstanceConfig {

    private final List<StageConfig> stageConfigs;
    private final SnapshotTokenType tokenType;
    private final StorageType storageType;

    private ProcessInstanceConfig(Builder builder) {
        stageConfigs = builder.stageConfigs;
        tokenType = builder.tokenType;
        storageType = builder.storageType;
    }

    /**
     * Gets the ordered list of stage configurations.
     *
     * @return stage configurations
     */
    public List<StageConfig> getStageConfigs() {
        return stageConfigs;
    }

    /**
     * Gets the snapshot token type.
     *
     * @return snapshot token type
     */
    public SnapshotTokenType getTokenType() {
        return tokenType;
    }

    /**
     * Gets the storage type, whether local or stateless.
     *
     * @return the storage type
     */
    public StorageType getStorageType() {
        return storageType;
    }

    /**
     * Builder for assisting with the construction of {@link ProcessInstanceConfig}.
     */
    public static final class Builder {

        private List<StageConfig> stageConfigs;
        private SnapshotTokenType tokenType;
        private StorageType storageType;

        private Builder() {
            stageConfigs = new ArrayList<>();
        }

        /**
         * Add a new stage config.
         *
         * @param stageConfig
         *         stage config
         *
         * @return this builder
         */
        public Builder addStageConfig(StageConfig stageConfig) {
            Reject.ifNull(stageConfig);
            stageConfigs.add(stageConfig);
            return this;
        }

        /**
         * Add a list of new stage configs.
         *
         * @param stageConfigs
         *         stage configs
         *
         * @return this builder
         */
        public Builder addStageConfigs(List<StageConfig> stageConfigs) {
            Reject.ifNull(stageConfigs);
            this.stageConfigs.addAll(stageConfigs);
            return this;
        }

        /**
         * Defines the snapshot token type to use.
         *
         * @param tokenType
         *         the snapshot token type
         *
         * @return this builder
         */
        public Builder setTokenType(SnapshotTokenType tokenType) {
            Reject.ifNull(tokenType);
            this.tokenType = tokenType;
            return this;
        }

        /**
         * The store type, whether local or stateless.
         *
         * @param storageType
         *         the storage type
         *
         * @return this builder
         */
        public Builder setStorageType(StorageType storageType) {
            this.storageType = storageType;
            return this;
        }

        /**
         * Builds a config instance.
         *
         * @return a new config instance
         */
        public ProcessInstanceConfig build() {
            Reject.ifTrue(stageConfigs.isEmpty());
            Reject.ifNull(tokenType, storageType);
            return new ProcessInstanceConfig(this);
        }

    }

    /**
     * Indicates whether the service should operate in stateless or stateful mode.
     * <b/>
     * Stateless means that all process state will be pushed into the token that
     * is returned to the client. Whereas stateful (local) will push all state to
     * a local store and the returning token will be used to key that state.
     */
    public enum StorageType {
        /**
         * State should be preserved locally.
         */
        LOCAL,

        /**
         * State should be preserved in a stateless way.
         */
        STATELESS
    }

    /**
     * Provides a new builder instance.
     *
     * @return a builder instance
     */
    public static Builder newBuilder() {
        return new Builder();
    }

}
