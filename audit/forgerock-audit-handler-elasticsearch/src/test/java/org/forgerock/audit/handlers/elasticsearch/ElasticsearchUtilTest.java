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

public class ElasticsearchUtilTest {

    private static final String AUTH_JSON_BEFORE_NORMALIZATION = "{ \"context\": { \"component\": " +
            "\"repo/internal/user\", \"roles\": [ \"openidm-admin\", \"openidm-authorized\" ], \"ipAddress\": " +
            "\"0:0:0:0:0:0:0:1\", \"id\": \"openidm-admin\" }, \"entries\": [ { \"moduleId\": \"JwtSession\", " +
            "\"result\": \"FAILED\", \"reason\": {  }, \"info\": {  } }, { \"moduleId\": \"STATIC_USER\", \"result\":" +
            " \"FAILED\", \"reason\": {  }, \"info\": { \"org.forgerock.authentication.principal\": \"openidm-admin\"" +
            " } }, { \"moduleId\": \"MANAGED_USER\", \"result\": \"FAILED\", \"reason\": {  }, \"info\": { \"org" +
            ".forgerock.authentication.principal\": \"openidm-admin\" } }, { \"moduleId\": \"INTERNAL_USER\", " +
            "\"result\": \"SUCCESSFUL\", \"info\": { \"org.forgerock.authentication.principal\": \"openidm-admin\" } " +
            "} ], \"principal\": [ \"openidm-admin\" ], \"result\": \"SUCCESSFUL\", \"userId\": \"openidm-admin\", " +
            "\"transactionId\": \"177c080f-5f21-467e-bf1a-26c54d62f355-207\", \"timestamp\": \"2016-01-28T23:09:57" +
            ".225Z\", \"eventName\": \"authentication\", \"trackingIds\": [ \"4043c668-57c2-4cd4-a48e-3de49db68e7a\" " +
            "], \"_id\": \"177c080f-5f21-467e-bf1a-26c54d62f355-215\" }";

    private static final String AUTH_JSON_AFTER_NORMALIZATION = "{ \"context\": { \"component\": " +
            "\"repo/internal/user\", \"roles\": [ \"openidm-admin\", \"openidm-authorized\" ], \"ipAddress\": " +
            "\"0:0:0:0:0:0:0:1\", \"id\": \"openidm-admin\" }, \"entries\": [ { \"moduleId\": \"JwtSession\", " +
            "\"result\": \"FAILED\", \"reason\": {  }, \"info\": {  } }, { \"moduleId\": \"STATIC_USER\", \"result\":" +
            " \"FAILED\", \"reason\": {  }, \"info\": { \"org_forgerock_authentication_principal\": \"openidm-admin\"" +
            " } }, { \"moduleId\": \"MANAGED_USER\", \"result\": \"FAILED\", \"reason\": {  }, \"info\": { " +
            "\"org_forgerock_authentication_principal\": \"openidm-admin\" } }, { \"moduleId\": \"INTERNAL_USER\", " +
            "\"result\": \"SUCCESSFUL\", \"info\": { \"org_forgerock_authentication_principal\": \"openidm-admin\" } " +
            "} ], \"principal\": [ \"openidm-admin\" ], \"result\": \"SUCCESSFUL\", \"userId\": \"openidm-admin\", " +
            "\"transactionId\": \"177c080f-5f21-467e-bf1a-26c54d62f355-207\", \"timestamp\": \"2016-01-28T23:09:57" +
            ".225Z\", \"eventName\": \"authentication\", \"trackingIds\": [ \"4043c668-57c2-4cd4-a48e-3de49db68e7a\" " +
            "], \"_id\": \"177c080f-5f21-467e-bf1a-26c54d62f355-215\" }";

    /**
     * Test that all periods in JSON keys will be replaced by underscores, as required by Elasticsearch.
     */
    @Test
    public void normalizeJsonWithPeriodsInKeysTest() {
        final String result = replaceKeyPeriodsWithUnderscores(AUTH_JSON_BEFORE_NORMALIZATION);
        assertThat(result).isEqualTo(AUTH_JSON_AFTER_NORMALIZATION);
    }
}
