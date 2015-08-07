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

    public List<StageConfig> getStageConfigs() {
        return stageConfigs;
    }

    public SnapshotTokenType getTokenType() {
        return tokenType;
    }

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

        public Builder addStageConfig(StageConfig stageConfig) {
            Reject.ifNull(stageConfig);
            stageConfigs.add(stageConfig);
            return this;
        }

        public Builder addStageConfigs(List<StageConfig> stageConfigs) {
            Reject.ifNull(stageConfigs);
            this.stageConfigs.addAll(stageConfigs);
            return this;
        }

        public Builder setTokenType(SnapshotTokenType tokenType) {
            Reject.ifNull(tokenType);
            this.tokenType = tokenType;
            return this;
        }

        public Builder setStorageType(StorageType storageType) {
            this.storageType = storageType;
            return this;
        }

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
        LOCAL, STATELESS
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
