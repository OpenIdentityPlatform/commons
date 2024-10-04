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
 * Copyright 2023 3A Systems LLC
 */

package org.forgerock.audit.handlers.json;

import org.forgerock.audit.AuditException;
import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.EventTopicsMetaDataBuilder;
import org.forgerock.audit.json.AuditJsonConfig;
import org.forgerock.json.JsonValue;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import static org.forgerock.audit.json.AuditJsonConfig.parseAuditEventHandlerConfiguration;
import static org.forgerock.json.JsonValue.*;
import static org.testng.Assert.*;
public class JsonStdoutAuditEventHandlerTest {

    private static final EventTopicsMetaData CORE_EVENT_TOPICS =
            EventTopicsMetaDataBuilder.coreTopicSchemas().build();

    @Test
    public void testPublishEvent() throws AuditException, IOException {
        JsonStdoutAuditEventHandlerConfiguration configuration = parseAuditEventHandlerConfiguration(
                JsonStdoutAuditEventHandlerConfiguration.class,
                getAuditConfig("event-handler-config-json-stdout.json"));
        assertTrue(configuration.isElasticsearchCompatible());

        JsonStdoutAuditEventHandler jsonStdoutAuditEventHandler = new JsonStdoutAuditEventHandler(configuration, CORE_EVENT_TOPICS);

        final String output;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             PrintStream printStream = new PrintStream(outputStream)) {
            System.setOut(printStream);
            jsonStdoutAuditEventHandler.publishEvent(null,"TEST_AUDIT", json(object(field("name", "TestEvent"))));
            output = outputStream.toString();
        }
        assertEquals("{\"name\":\"TestEvent\",\"_topic\":\"TEST_AUDIT\"}".concat(System.lineSeparator()), output);
    }

    static JsonValue getAuditConfig(String testConfigFile) throws AuditException {
        String RESOURCE_PATH = "/";
        return AuditJsonConfig.getJson(
                JsonStdoutAuditEventHandlerTest.class.getResourceAsStream(RESOURCE_PATH + testConfigFile));
    }
}
