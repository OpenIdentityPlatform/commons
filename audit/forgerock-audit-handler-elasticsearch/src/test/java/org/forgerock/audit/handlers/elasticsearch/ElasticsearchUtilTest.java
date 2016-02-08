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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.audit.handlers.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.data.MapEntry;
import org.forgerock.json.JsonValue;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.*;
import static org.forgerock.audit.handlers.elasticsearch.ElasticsearchUtil.*;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ElasticsearchUtilTest {

    private static final String RESOURCE_PATH = "/org/forgerock/audit/handlers/elasticsearch/";

    private static final MapEntry<String, String> FIELD_NAME_PAIR =
            MapEntry.entry("org_forgerock_authentication_principal", "org.forgerock.authentication.principal");

    /**
     * Test that all periods in JSON keys will be replaced by underscores, as required by Elasticsearch.
     */
    @Test
    public void normalizeJsonWithPeriodsInKeysTest() throws Exception {
        // given
        final JsonValue beforeNormalization = resourceAsJsonValue(RESOURCE_PATH + "authEventBeforeNormalization.json");
        final JsonValue afterNormalization = resourceAsJsonValue(RESOURCE_PATH + "authEventAfterNormalization.json");
        assertThat(beforeNormalization).isNotEqualTo(afterNormalization);

        // when
        final Map<String, Object> normalized = new LinkedHashMap<>(1);
        final JsonValue result = replaceKeyPeriodsWithUnderscores(beforeNormalization, normalized);

        // then
        assertThat(result.toString()).isEqualTo(afterNormalization.toString());
        assertThat(normalized).containsKey(ElasticsearchUtil.FIELD_NAMES_FIELD);
        assertThat((Map<String, Object>) normalized.get(ElasticsearchUtil.FIELD_NAMES_FIELD))
                .containsExactly(FIELD_NAME_PAIR);
    }

    @Test
    public void denormalizeJsonWithPeriodsInKeysTest() throws Exception {
        // given
        final JsonValue beforeNormalization = resourceAsJsonValue(RESOURCE_PATH + "authEventBeforeNormalization.json");
        final JsonValue afterNormalization = resourceAsJsonValue(RESOURCE_PATH + "authEventAfterNormalization.json");
        assertThat(beforeNormalization).isNotEqualTo(afterNormalization);

        final Map<String, Object> normalized = new LinkedHashMap<>(1);
        normalized.put(ElasticsearchUtil.FIELD_NAMES_FIELD,
                Collections.singletonMap(FIELD_NAME_PAIR.key, FIELD_NAME_PAIR.value));

        // when
        final JsonValue result = restoreKeyPeriods(afterNormalization, JsonValue.json(normalized));

        // then
        assertThat(result.toString()).isEqualTo(beforeNormalization.toString());
    }

    private JsonValue resourceAsJsonValue(final String resourcePath) throws Exception {
        try (final InputStream configStream = getClass().getResourceAsStream(resourcePath)) {
            return new JsonValue(new ObjectMapper().readValue(configStream, Map.class));
        }
    }
}
