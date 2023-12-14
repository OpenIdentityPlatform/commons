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