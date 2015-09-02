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

import org.forgerock.selfservice.core.StorageType;
import org.forgerock.util.Reject;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the configuration for an instance of the anonymous process service.
 *
 * @since 0.1.0
 */
public final class ProcessInstanceConfig {

    private List<StageConfig> stageConfigs;
    private String snapshotTokenType;
    private StorageType storageType;

    /**
     * Sets the list of stage configs. The order of the list
     * is the order in which the stages will be processed.
     *
     * @param stageConfigs
     *         the list of stage configs
     *
     * @return this config
     */
    public ProcessInstanceConfig setStageConfigs(List<StageConfig> stageConfigs) {
        Reject.ifNull(stageConfigs);
        this.stageConfigs = new ArrayList<>(stageConfigs);
        return this;
    }

    /**
     * Gets the list of stage configs.
     *
     * @return the list of stage configs
     */
    public List<StageConfig> getStageConfigs() {
        return stageConfigs;
    }

    /**
     * Sets the snapshot token type to use.
     *
     * @param snapshotTokenType
     *         the snapshot token type
     *
     * @return this config
     */
    public ProcessInstanceConfig setSnapshotTokenType(String snapshotTokenType) {
        Reject.ifNull(snapshotTokenType);
        this.snapshotTokenType = snapshotTokenType;
        return this;
    }

    /**
     * Gets the snapshot token type to use.
     *
     * @return the snapshot token type
     */
    public String getSnapshotTokenType() {
        return snapshotTokenType;
    }

    /**
     * Sets the storage type to use. See {@link org.forgerock.selfservice.core.StorageType}.
     *
     * @param storageType
     *         the storage type
     *
     * @return this config
     */
    public ProcessInstanceConfig setStorageType(String storageType) {
        Reject.ifNull(storageType);
        this.storageType = StorageType.valueOf(storageType);
        return this;
    }

    /**
     * Gets the storage type to use.
     *
     * @return the storage type
     */
    public StorageType getStorageType() {
        return storageType;
    }

}
