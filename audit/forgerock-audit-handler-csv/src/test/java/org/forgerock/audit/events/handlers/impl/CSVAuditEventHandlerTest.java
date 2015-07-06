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
package org.forgerock.audit.events.handlers.impl;

import static org.fest.assertions.api.Assertions.*;
import static org.forgerock.json.fluent.JsonValue.*;
import static org.mockito.Mockito.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.ServerContext;
import org.testng.annotations.Test;


@SuppressWarnings("javadoc")
public class CSVAuditEventHandlerTest {

    @Test
    public void testCreateCsvLogEntry() throws Exception {
        Path logDirectory = Files.createTempDirectory("CSVAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        CSVAuditEventHandler csvHandler = createAndConfigureHandler(logDirectory);
        final JsonValue content = json(
                object(
                    field("_id", "1"),
                    field("timestamp", "123456"),
                    field("transactionId", "A10000")));
        CreateRequest createRequest = Requests.newCreateRequest("access", content);

        csvHandler.createInstance(
                new ServerContext(new RootContext()), createRequest, mockResultHandler(Resource.class));

        String expectedContent = "\"_id\",\"timestamp\",\"transactionId\"\n" + "\"1\",\"123456\",\"A10000\"";
        assertThat(logDirectory.resolve("access.csv").toFile()).hasContent(expectedContent);
    }

    private CSVAuditEventHandler createAndConfigureHandler(Path tempDirectory) throws Exception {
        CSVAuditEventHandler handler = new CSVAuditEventHandler();
        CSVAuditEventHandlerConfiguration config = new CSVAuditEventHandlerConfiguration();
        config.setLogDirectory(tempDirectory.toString());
        config.setRecordDelimiter("");
        handler.configure(config);
        addEventsMetaData(handler);
        return handler;
    }

    private void addEventsMetaData(CSVAuditEventHandler handler) throws Exception {
        Map<String, JsonValue> events = new LinkedHashMap<>();
        try (final InputStream configStream = getClass().getResourceAsStream("/events.json")) {
            final JsonValue predefinedEventTypes = new JsonValue(new ObjectMapper().readValue(configStream, Map.class));
            for (String eventTypeName : predefinedEventTypes.keys()) {
                events.put(eventTypeName, predefinedEventTypes.get(eventTypeName));
            }
        }
        handler.setAuditEventsMetaData(events);
    }

    @SuppressWarnings("unchecked")
    private static <T> ResultHandler<T> mockResultHandler(Class<T> type) {
        return mock(ResultHandler.class);
    }

}
