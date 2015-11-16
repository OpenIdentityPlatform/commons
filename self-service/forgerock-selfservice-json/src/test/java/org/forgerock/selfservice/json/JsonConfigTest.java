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

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.selfservice.core.StorageType;
import org.forgerock.selfservice.core.config.ProcessInstanceConfig;
import org.forgerock.json.JsonValue;
import org.testng.annotations.Test;

/**
 * Unit test for {@link JsonConfig}.
 *
 * @since 0.2.0
 */
public final class JsonConfigTest {

    @Test
    public void testConfigFromJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonValue json = new JsonValue(mapper.readValue(getClass().getResource("/selfservice.json"), Map.class));
        ProcessInstanceConfig config = JsonConfig.buildProcessInstanceConfig(json);
        assertThat(config.getStorageType()).isEqualTo(StorageType.STATELESS);
        assertThat(config.getSnapshotTokenConfig().getType()).isEqualTo("jwt");
    }

}
