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
package org.forgerock.selfservice.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import org.forgerock.selfservice.core.StorageType;
import org.forgerock.selfservice.core.config.ProcessInstanceConfig;
import org.forgerock.json.JsonValue;
import org.forgerock.selfservice.core.config.StageConfig;
import org.testng.annotations.Test;

/**
 * Unit test for {@link JsonConfig}.
 *
 * @since 0.2.0
 */
public final class JsonConfigTest {

    private static final Function<StageConfig, String> TO_STAGE_NAME = new Function<StageConfig, String>() {
        @Override
        public String apply(StageConfig stageConfig) {
            return stageConfig.getName();
        }
    };

    /* the stages that are defined in the test resource "selfservice.json" */
    private static final String[] STAGES = new String[] {
        "termsAndConditions",
        "emailValidation",
        "userQuery",
        "retrieveUsername",
        "emailUsername",
        "userDetails",
        "kbaSecurityAnswerDefinitionStage",
        "selfRegistration",
        "resetStage"
    };

    @Test
    public void testConfigFromJson() throws Exception {
        JsonValue json = readConfig("/selfservice.json");
        ProcessInstanceConfig config = new JsonConfig(getClass().getClassLoader()).buildProcessInstanceConfig(json);
        assertThat(config.getStageConfigs().size()).isEqualTo(STAGES.length);
        assertThat(FluentIterable.from(config.getStageConfigs()).transform(TO_STAGE_NAME)).containsExactly(STAGES);
        assertThat(config.getStorageType()).isEqualTo(StorageType.STATELESS);
        assertThat(config.getSnapshotTokenConfig().getType()).isEqualTo("jwt");
    }

    @Test
    public void testCustomStageConfigFromJsonRequiresCustomClassLoader() throws Exception {
        JsonValue json = readConfig("/custom.json");

        // cheezy way to navigate to classpath where custom stage is defined
        final URL customStageBundle = Paths.get(getClass().getResource("").toURI())
                .getParent()
                .getParent()
                .getParent()
                .getParent()
                .getParent()
                .getParent()
                .resolveSibling("custom-stage/target/classes").toUri().toURL();
        try (final URLClassLoader classLoader = new URLClassLoader(
                new URL[] { customStageBundle }, this.getClass().getClassLoader())) {
            ProcessInstanceConfig config = new JsonConfig(classLoader).buildProcessInstanceConfig(json);
            assertThat(config.getStorageType()).isEqualTo(StorageType.STATELESS);
            assertThat(config.getSnapshotTokenConfig().getType()).isEqualTo("jwt");
            List<StageConfig> stageConfigs = config.getStageConfigs();
            assertThat(stageConfigs).hasSize(1);
            assertThat(stageConfigs.get(0).getClass().toString()).endsWith("MathProblemStageConfig");
        }
    }

    @Test
    public void testRegistrationOfCustomStageName() throws Exception {
        JsonValue json = readConfig("/custom-with-name.json");
        Map<String, Class<? extends StageConfig>> namedTypes = new HashMap<>();
        namedTypes.put(CustomStageConfig.NAME, CustomStageConfig.class);

        ProcessInstanceConfig config = new JsonConfig(getClass().getClassLoader(), namedTypes)
                .buildProcessInstanceConfig(json);
        assertThat(config.getStageConfigs().size()).isEqualTo(1);
        assertThat(FluentIterable.from(config.getStageConfigs()).transform(TO_STAGE_NAME))
                .containsExactly(CustomStageConfig.NAME);
        List<StageConfig> stageConfigs = config.getStageConfigs();
        assertThat(stageConfigs).hasSize(1);
        assertThat(stageConfigs.get(0).getClass()).isEqualTo(CustomStageConfig.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCustomStageConfigFromJsonFailsWithDefaultClassLoader() throws Exception {
        JsonValue json = readConfig("/custom.json");
        new JsonConfig(getClass().getClassLoader()).buildProcessInstanceConfig(json);
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonValue readConfig(String configResource) throws IOException {
        return new JsonValue(OBJECT_MAPPER.readValue(getClass().getResource(configResource), Map.class));
    }
}
