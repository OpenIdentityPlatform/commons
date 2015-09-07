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

package org.forgerock.audit.handlers.syslog;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.*;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.audit.events.AuditEvent;
import org.forgerock.audit.events.AuditEventBuilder;
import org.forgerock.audit.handlers.syslog.SyslogAuditEventHandlerConfiguration.SeverityFieldMapping;
import org.forgerock.json.JsonValue;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyslogFormatterTest {

    @Test
    public void canFormatAuditEventAsSyslogMessage() throws Exception {
        // given
        SyslogFormatter syslogFormatter = newSyslogFormatter("OpenAM", Facility.LOCAL5, "server.name");

        AuditEvent auditEvent = firstTestTopic()
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .eventName("AM-ACCESS-ATTEMPT")
                .authentication("someone@forgerock.com")
                .field1("foo", "bar")
                .field4("123456789")
                .toEvent();

        // when
        String formattedEvent = syslogFormatter.format("firstTestTopic", auditEvent.getValue());
        SyslogMessage syslogMessage = readSyslogMessage(formattedEvent);

        // then
        assertThat(syslogMessage.priority).isEqualTo(174);
        assertThat(syslogMessage.syslogSpecVersion).isEqualTo(1);
        assertThat(syslogMessage.timestamp).isEqualTo("2015-03-25T14:21:26.239Z");
        assertThat(syslogMessage.hostname).isEqualTo("server.name");
        assertThat(syslogMessage.appName).isEqualTo("OpenAM");
        assertThat(syslogMessage.msgId).isEqualTo("AM-ACCESS-ATTEMPT");
        assertThat(syslogMessage.structuredDataId).isEqualTo("firstTestTopic.OpenAM@36733");
        assertThat(syslogMessage.structuredData.get("transactionId")).isEqualTo("transactionId");
        assertThat(syslogMessage.structuredData.get("field1.field2")).isEqualTo("foo");
        assertThat(syslogMessage.structuredData.get("field1.field3")).isEqualTo("bar");
        assertThat(syslogMessage.structuredData.get("field4")).isEqualTo("123456789");
    }

    @Test
    public void setsPriorityBasedOnFacility() throws Exception {
        // given
        SyslogFormatter syslogFormatter = newSyslogFormatter("OpenAM", Facility.LOCAL7, "server.name");

        AuditEvent auditEvent = firstTestTopic()
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .eventName("AM-ACCESS-ATTEMPT")
                .toEvent();

        // when
        String formattedEvent = syslogFormatter.format("firstTestTopic", auditEvent.getValue());
        SyslogMessage syslogMessage = readSyslogMessage(formattedEvent);

        // then
        assertThat(syslogMessage.priority).isEqualTo(190);
    }

    @Test
    public void canMapTopicRootFieldToSeverityLevel() throws Exception {
        // given
        SeverityFieldMapping severityFieldMapping = new SeverityFieldMapping();
        severityFieldMapping.setTopic("firstTestTopic");
        severityFieldMapping.setField("field4");
        severityFieldMapping.setValueMappings(Collections.singletonMap("SEVERE", Severity.EMERGENCY));

        SyslogFormatter syslogFormatter = newSyslogFormatter(
                "OpenAM", Facility.LOCAL7, "server.name", singletonList(severityFieldMapping));

        AuditEvent auditEvent = firstTestTopic()
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .eventName("AM-ACCESS-ATTEMPT")
                .field4("EMERGENCY")
                .toEvent();

        // when
        String formattedEvent = syslogFormatter.format("firstTestTopic", auditEvent.getValue());
        SyslogMessage syslogMessage = readSyslogMessage(formattedEvent);

        // then
        assertThat(syslogMessage.priority).isEqualTo(184);
    }

    @Test
    public void canMapTopicNestedFieldToSeverityLevel() throws Exception {
        // given
        SeverityFieldMapping severityFieldMapping = new SeverityFieldMapping();
        severityFieldMapping.setTopic("firstTestTopic");
        severityFieldMapping.setField("field1/field2");
        severityFieldMapping.setValueMappings(Collections.singletonMap("SEVERE", Severity.EMERGENCY));

        SyslogFormatter syslogFormatter = newSyslogFormatter(
                "OpenAM", Facility.LOCAL7, "server.name", singletonList(severityFieldMapping));

        AuditEvent auditEvent = firstTestTopic()
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .eventName("AM-ACCESS-ATTEMPT")
                .field1("EMERGENCY", "")
                .toEvent();

        // when
        String formattedEvent = syslogFormatter.format("firstTestTopic", auditEvent.getValue());
        SyslogMessage syslogMessage = readSyslogMessage(formattedEvent);

        // then
        assertThat(syslogMessage.priority).isEqualTo(184);
    }

    @Test
    public void defaultsToInformationSeverityLevelIfMappedTopicIsNull() throws Exception {
        // given
        SeverityFieldMapping severityFieldMapping = new SeverityFieldMapping();
        severityFieldMapping.setTopic(null);
        severityFieldMapping.setField("field4");
        severityFieldMapping.setValueMappings(Collections.singletonMap("SEVERE", Severity.EMERGENCY));

        SyslogFormatter syslogFormatter = newSyslogFormatter(
                "OpenAM", Facility.LOCAL7, "server.name", singletonList(severityFieldMapping));

        AuditEvent auditEvent = firstTestTopic()
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .eventName("AM-ACCESS-ATTEMPT")
                .field4("EMERGENCY")
                .toEvent();

        // when
        String formattedEvent = syslogFormatter.format("firstTestTopic", auditEvent.getValue());
        SyslogMessage syslogMessage = readSyslogMessage(formattedEvent);

        // then
        assertThat(syslogMessage.priority).isEqualTo(190);
    }

    @Test
    public void defaultsToInformationSeverityLevelIfMappedTopicUnknown() throws Exception {
        // given
        SeverityFieldMapping severityFieldMapping = new SeverityFieldMapping();
        severityFieldMapping.setTopic("unknownTopic");
        severityFieldMapping.setField("field4");
        severityFieldMapping.setValueMappings(Collections.singletonMap("SEVERE", Severity.EMERGENCY));

        SyslogFormatter syslogFormatter = newSyslogFormatter(
                "OpenAM", Facility.LOCAL7, "server.name", singletonList(severityFieldMapping));

        AuditEvent auditEvent = firstTestTopic()
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .eventName("AM-ACCESS-ATTEMPT")
                .field4("EMERGENCY")
                .toEvent();

        // when
        String formattedEvent = syslogFormatter.format("firstTestTopic", auditEvent.getValue());
        SyslogMessage syslogMessage = readSyslogMessage(formattedEvent);

        // then
        assertThat(syslogMessage.priority).isEqualTo(190);
    }

    @Test
    public void defaultsToInformationSeverityLevelIfMappedTopicFieldIsNull() throws Exception {
        // given
        SeverityFieldMapping severityFieldMapping = new SeverityFieldMapping();
        severityFieldMapping.setTopic("firstTestTopic");
        severityFieldMapping.setField(null);
        severityFieldMapping.setValueMappings(Collections.singletonMap("SEVERE", Severity.EMERGENCY));

        SyslogFormatter syslogFormatter = newSyslogFormatter(
                "OpenAM", Facility.LOCAL7, "server.name", singletonList(severityFieldMapping));

        AuditEvent auditEvent = firstTestTopic()
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .eventName("AM-ACCESS-ATTEMPT")
                .field4("EMERGENCY")
                .toEvent();

        // when
        String formattedEvent = syslogFormatter.format("firstTestTopic", auditEvent.getValue());
        SyslogMessage syslogMessage = readSyslogMessage(formattedEvent);

        // then
        assertThat(syslogMessage.priority).isEqualTo(190);
    }

    @Test
    public void defaultsToInformationSeverityLevelIfMappedTopicFieldUnknown() throws Exception {
        // given
        SeverityFieldMapping severityFieldMapping = new SeverityFieldMapping();
        severityFieldMapping.setTopic("firstTestTopic");
        severityFieldMapping.setField("unknownField");
        severityFieldMapping.setValueMappings(Collections.singletonMap("SEVERE", Severity.EMERGENCY));

        SyslogFormatter syslogFormatter = newSyslogFormatter(
                "OpenAM", Facility.LOCAL7, "server.name", singletonList(severityFieldMapping));

        AuditEvent auditEvent = firstTestTopic()
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .eventName("AM-ACCESS-ATTEMPT")
                .field4("EMERGENCY")
                .toEvent();

        // when
        String formattedEvent = syslogFormatter.format("firstTestTopic", auditEvent.getValue());
        SyslogMessage syslogMessage = readSyslogMessage(formattedEvent);

        // then
        assertThat(syslogMessage.priority).isEqualTo(190);
    }

    @Test
    public void defaultsToInformationSeverityLevelIfMappedTopicFieldValueIsNull() throws Exception {
        // given
        SeverityFieldMapping severityFieldMapping = new SeverityFieldMapping();
        severityFieldMapping.setTopic("firstTestTopic");
        severityFieldMapping.setField("field4");
        severityFieldMapping.setValueMappings(Collections.singletonMap("SEVERE", Severity.EMERGENCY));

        SyslogFormatter syslogFormatter = newSyslogFormatter(
                "OpenAM", Facility.LOCAL7, "server.name", singletonList(severityFieldMapping));

        AuditEvent auditEvent = firstTestTopic()
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .eventName("AM-ACCESS-ATTEMPT")
                .toEvent();

        // when
        String formattedEvent = syslogFormatter.format("firstTestTopic", auditEvent.getValue());
        SyslogMessage syslogMessage = readSyslogMessage(formattedEvent);

        // then
        assertThat(syslogMessage.priority).isEqualTo(190);
    }

    @Test
    public void defaultsToInformationSeverityLevelIfMappedTopicFieldValueIsNotMapped() throws Exception {
        // given
        SeverityFieldMapping severityFieldMapping = new SeverityFieldMapping();
        severityFieldMapping.setTopic("firstTestTopic");
        severityFieldMapping.setField("field4");
        severityFieldMapping.setValueMappings(Collections.singletonMap("SEVERE", Severity.EMERGENCY));

        SyslogFormatter syslogFormatter = newSyslogFormatter(
                "OpenAM", Facility.LOCAL7, "server.name", singletonList(severityFieldMapping));

        AuditEvent auditEvent = firstTestTopic()
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .eventName("AM-ACCESS-ATTEMPT")
                .field4("panic")
                .toEvent();

        // when
        String formattedEvent = syslogFormatter.format("firstTestTopic", auditEvent.getValue());
        SyslogMessage syslogMessage = readSyslogMessage(formattedEvent);

        // then
        assertThat(syslogMessage.priority).isEqualTo(190);
    }

    @Test
    public void replacesNullLocalHostNameWithNIL() throws Exception {
        // given
        SyslogFormatter syslogFormatter = newSyslogFormatter("OpenAM", Facility.LOCAL5, null);

        AuditEvent auditEvent = firstTestTopic()
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .eventName("AM-ACCESS-ATTEMPT")
                .toEvent();

        // when
        String formattedEvent = syslogFormatter.format("firstTestTopic", auditEvent.getValue());
        SyslogMessage syslogMessage = readSyslogMessage(formattedEvent);

        // then
        assertThat(syslogMessage.hostname).isEqualTo("-");
    }

    @Test
    public void setsAppNameFromConfiguration() throws Exception {
        // given
        SyslogFormatter syslogFormatter = newSyslogFormatter("OpenIDM", Facility.LOCAL5, "server.name");

        AuditEvent auditEvent = firstTestTopic()
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .eventName("AM-ACCESS-ATTEMPT")
                .toEvent();

        // when
        String formattedEvent = syslogFormatter.format("firstTestTopic", auditEvent.getValue());
        SyslogMessage syslogMessage = readSyslogMessage(formattedEvent);

        // then
        assertThat(syslogMessage.appName).isEqualTo("OpenIDM");
    }

    @Test
    public void createsUniqueStructuredDataIdPerAppNameAndAuditEventTopic() throws Exception {
        // given
        SyslogFormatter syslogFormatter = newSyslogFormatter("OpenDJ", Facility.LOCAL5, "server.name");

        AuditEvent auditEvent = secondTestTopic()
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .eventName("AM-ACCESS-ATTEMPT")
                .toEvent();

        // when
        String formattedEvent = syslogFormatter.format("secondTestTopic", auditEvent.getValue());
        SyslogMessage syslogMessage = readSyslogMessage(formattedEvent);

        // then
        assertThat(syslogMessage.structuredDataId).isEqualTo("secondTestTopic.OpenDJ@36733");
    }

    @Test
    public void escapesSpecialCharactersInStructuredDataParamValues() throws Exception {
        // given
        SyslogFormatter syslogFormatter = newSyslogFormatter("OpenAM", Facility.LOCAL5, "server.name");

        AuditEvent auditEvent = firstTestTopic()
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .eventName("AM-ACCESS-ATTEMPT")
                .field1("A list with escaped characters", "\"]\\")
                .field4("\"]\\")
                .toEvent();

        // when
        String formattedEvent = syslogFormatter.format("firstTestTopic", auditEvent.getValue());
        SyslogMessage syslogMessage = readSyslogMessage(formattedEvent);

        // then
        assertThat(syslogMessage.structuredData.get("field1.field2")).isEqualTo("A list with escaped characters");
        assertThat(syslogMessage.structuredData.get("field1.field3")).isEqualTo("\"]\\");
        assertThat(syslogMessage.structuredData.get("field4")).isEqualTo("\"]\\");
    }

    private SyslogFormatter newSyslogFormatter(String productName, Facility facility, String localHostName)
            throws Exception {
        return newSyslogFormatter(productName, facility, localHostName, Collections.<SeverityFieldMapping>emptyList());
    }

    private SyslogFormatter newSyslogFormatter(String productName, Facility facility, String localHostName,
            List<SeverityFieldMapping> severityFieldMappings) throws Exception {

        Map<String, JsonValue> auditEventDefinitions = loadAuditEventDefinitions();

        SyslogAuditEventHandlerConfiguration config = new SyslogAuditEventHandlerConfiguration();
        config.setProductName(productName);
        config.setFacility(facility);
        config.setSeverityFieldMappings(severityFieldMappings);

        LocalHostNameProvider localHostNameProvider = mock(LocalHostNameProvider.class);
        given(localHostNameProvider.getLocalHostName()).willReturn(localHostName);

        return new SyslogFormatter(auditEventDefinitions, config, localHostNameProvider);
    }

    private SyslogMessage readSyslogMessage(String message) {

        Pattern pattern = Pattern.compile(
                "<(\\d{1,3})>" +       // PRI
                "(\\d)\\s" +           // VERSION
                "(\\S*)\\s" +          // TIMESTAMP
                "(\\S*)\\s" +          // HOSTNAME
                "(\\S*)\\s" +          // APP-NAME
                "(\\S*)\\s" +          // PROCID
                "(\\S*)\\s" +          // MSGID
                "\\[(\\S*)" +          // ID
                "\\s([\\s\\S]*)\\]" +  // key-values
                ".*(\n)?");

        Matcher matcher = pattern.matcher(message);
        boolean matches = matcher.find();
        if (!matches) {
            throw new IllegalArgumentException("Not a well formed Syslog message: " + message);
        }

        SyslogMessage syslogMessage = new SyslogMessage();
        syslogMessage.priority = Integer.parseInt(matcher.group(1));
        syslogMessage.syslogSpecVersion = Integer.parseInt(matcher.group(2));
        syslogMessage.timestamp = matcher.group(3);
        syslogMessage.hostname = matcher.group(4);
        syslogMessage.appName = matcher.group(5);
        syslogMessage.procId = matcher.group(6);
        syslogMessage.msgId = matcher.group(7);
        syslogMessage.msgId = matcher.group(7);
        syslogMessage.structuredDataId = matcher.group(8);

        char[] sdChars = matcher.group(9).toCharArray();
        int i = 0;

        while (i < sdChars.length) {
            // parse the key
            StringBuilder key = new StringBuilder();
            while (i < sdChars.length) {
                if (sdChars[i] == '=') {
                    break;
                }
                key.append(sdChars[i]);
                i++;
            }
            // skip the '=' character
            i++;
            // skip the double-quote character
            i++;
            // parse the value taking care to interpret '\', '"', ']' based on preceding escape characters
            StringBuilder value = new StringBuilder();
            int escapeChars = 0;
            while (i < sdChars.length) {
                if (sdChars[i] == '\\') {
                    if (escapeChars == 1) {
                        value.append('\\'); // only output '\' if it was escaped
                        escapeChars = 0;
                    } else {
                        escapeChars = 1;
                    }
                } else if (sdChars[i] == '\"') {
                    if (escapeChars == 0) {
                        break; // an unescaped '"' character signals the end of this value
                    } else {
                        value.append('\"');
                        escapeChars = 0;
                    }
                } else if (sdChars[i] == ']') {
                    if (escapeChars == 0) {
                        throw new IllegalStateException("']' character must be escaped");
                    }
                    value.append(sdChars[i]);
                    escapeChars = 0;
                } else {
                    value.append(sdChars[i]);
                    escapeChars = 0;
                }
                i++;
            }
            // record the key-value pair
            syslogMessage.structuredData.put(key.toString(), value.toString());
            // skip the double-quote
            i++;
            // skip the ' ' character
            i++;
        }

        syslogMessage.msg = matcher.group(9);

        return syslogMessage;
    }

    private static class SyslogMessage {
        int priority;
        int syslogSpecVersion;
        String timestamp;
        String hostname;
        String appName;
        String procId;
        String msgId;
        String structuredDataId;
        Map<String, String> structuredData = new HashMap<>();
        String msg;
    }

    private Map<String, JsonValue> loadAuditEventDefinitions() throws Exception {
        Map<String, JsonValue> events = new LinkedHashMap<>();
        try (final InputStream configStream = getClass().getResourceAsStream("/events.json")) {
            final JsonValue predefinedEventTypes = new JsonValue(new ObjectMapper().readValue(configStream, Map.class));
            for (String eventTypeName : predefinedEventTypes.keys()) {
                events.put(eventTypeName, predefinedEventTypes.get(eventTypeName));
            }
        }
        return events;
    }

    private static TestTopicBuilder firstTestTopic() {
        return new TestTopicBuilder();
    }

    private static TestTopicBuilder secondTestTopic() {
        return new TestTopicBuilder();
    }

    private static class TestTopicBuilder extends AuditEventBuilder<TestTopicBuilder> {

        public final TestTopicBuilder field1(String field2, String field3) {
            jsonValue.put("field1", json(object(
                    field("field2", field2),
                    field("field3", field3))));
            return this;
        }

        public final TestTopicBuilder field4(String field4) {
            jsonValue.put("field4", field4);
            return this;
        }
    }

}
