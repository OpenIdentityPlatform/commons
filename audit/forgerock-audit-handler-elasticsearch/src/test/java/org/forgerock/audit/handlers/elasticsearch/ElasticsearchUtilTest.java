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

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.*;
import static org.forgerock.audit.handlers.elasticsearch.ElasticsearchUtil.*;

import java.io.InputStream;
import java.util.Scanner;

public class ElasticsearchUtilTest {

    private static final String RESOURCE_PATH = "/org/forgerock/audit/handlers/elasticsearch/";

    /**
     * Test that all periods in JSON keys will be replaced by underscores, as required by Elasticsearch.
     */
    @Test
    public void normalizeJsonWithPeriodsInKeysTest() throws Exception {
        // given
        final String beforeNormalization = resourceAsString(RESOURCE_PATH + "authEventBeforeNormalization.json");
        final String afterNormalization = resourceAsString(RESOURCE_PATH + "authEventAfterNormalization.json");
        assertThat(beforeNormalization).isNotEqualTo(afterNormalization);

        // when
        final String result = replaceKeyPeriodsWithUnderscores(beforeNormalization);

        // then
        assertThat(result).isEqualTo(afterNormalization);
    }

    private String resourceAsString(final String resourcePath) throws Exception {
        try (final InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            return new Scanner(inputStream, "UTF-8").useDelimiter("\\A").next();
        }
    }
}
