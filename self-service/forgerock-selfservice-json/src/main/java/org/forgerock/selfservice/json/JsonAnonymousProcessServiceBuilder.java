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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.selfservice.json;

import java.util.HashMap;
import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.selfservice.core.AnonymousProcessService;
import org.forgerock.selfservice.core.ProcessStore;
import org.forgerock.selfservice.core.ProgressStageProvider;
import org.forgerock.selfservice.core.config.ProcessInstanceConfig;
import org.forgerock.selfservice.core.config.StageConfig;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandlerFactory;
import org.forgerock.util.Reject;

/**
 * Builder for {@link AnonymousProcessService} from JSON config and AnonymousProcessService requirements.
 *
 * @since 20.0.0
 */
public final class JsonAnonymousProcessServiceBuilder {

    private ClassLoader classLoader = getClass().getClassLoader(); // assume this ClassLoader if not supplied
    private Map<String, Class<? extends StageConfig>> stageConfigMappings = new HashMap<>();
    private JsonValue jsonConfig;
    private ProgressStageProvider progressStageProvider;
    private SnapshotTokenHandlerFactory tokenHandlerFactory;
    private ProcessStore processStore;

    private JsonAnonymousProcessServiceBuilder() {
        // prevent direct instantiation
    }

    /**
     * Construct a new JsonAnonymousProcessServiceBuilder.
     *
     * @return the JsonAnonymousProcesssServiceBuilder.
     */
    public static JsonAnonymousProcessServiceBuilder newBuilder() {
        return new JsonAnonymousProcessServiceBuilder();
    }

    /**
     * Set the ClassLoader.
     *
     * @param classLoader the ClassLoader
     * @return this builder instance
     */
    public JsonAnonymousProcessServiceBuilder withClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    /**
     * Provide additional named type-mapping, if desired.
     *
     * @param name the {@code name} attribute value to associate with the new stage config type
     * @param type the {@link StageConfig} type to associate
     * @return this builder instance
     */
    public JsonAnonymousProcessServiceBuilder withStageConfigMapping(String name, Class<? extends StageConfig> type) {
        this.stageConfigMappings.put(name, type);
        return this;
    }

    /**
     * Set the JSON config from which to build the {@link ProcessInstanceConfig}.
     *
     * @param jsonConfig the JSON config for a {@link ProcessInstanceConfig}.
     * @return this builder instance
     */
    public JsonAnonymousProcessServiceBuilder withJsonConfig(JsonValue jsonConfig) {
        this.jsonConfig = jsonConfig;
        return this;
    }

    /**
     * Sets the {@link ProgressStageProvider}.
     *
     * @param progressStageProvider the {@link ProgressStageProvider}.
     * @return this builder instance
     */
    public JsonAnonymousProcessServiceBuilder withProgressStageProvider(ProgressStageProvider progressStageProvider) {
        this.progressStageProvider = progressStageProvider;
        return this;
    }

    /**
     * Sets the {@link SnapshotTokenHandlerFactory}.
     *
     * @param tokenHandlerFactory the {@link SnapshotTokenHandlerFactory}.
     * @return this builder instance
     */
    public JsonAnonymousProcessServiceBuilder withTokenHandlerFactory(SnapshotTokenHandlerFactory tokenHandlerFactory) {
        this.tokenHandlerFactory = tokenHandlerFactory;
        return this;
    }

    /**
     * Sets the {@link ProcessStore}.
     *
     * @param processStore the {@link ProcessStore}.
     * @return this builder instance
     */
    public JsonAnonymousProcessServiceBuilder withProcessStore(ProcessStore processStore) {
        this.processStore = processStore;
        return this;
    }

    /**
     * Build an {@link AnonymousProcessService} from the JSON config and the other elements.
     *
     * @return the {@link AnonymousProcessService}
     */
    public AnonymousProcessService build() {
        Reject.ifNull(classLoader, jsonConfig, progressStageProvider, tokenHandlerFactory, processStore);
        ProcessInstanceConfig config = new JsonConfig(classLoader, stageConfigMappings)
                .buildProcessInstanceConfig(jsonConfig);
        Reject.ifNull(config.getStageConfigs(), config.getSnapshotTokenConfig(), config.getStorageType());
        Reject.ifTrue(config.getStageConfigs().isEmpty());
        return new AnonymousProcessService(config, progressStageProvider, tokenHandlerFactory, processStore,
                classLoader);
    }
}
