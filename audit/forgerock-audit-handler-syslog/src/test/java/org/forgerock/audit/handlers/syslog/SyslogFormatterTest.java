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

import static org.assertj.core.api.Assertions.*;
import static org.forgerock.audit.events.AccessAuditEventBuilder.accessEvent;
import static org.forgerock.audit.events.ActivityAuditEventBuilder.activityEvent;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.audit.events.AuditEvent;
import org.forgerock.json.JsonValue;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyslogFormatterTest {

    @Test
    public void canFormatAuditEventAsSyslogMessage() throws Exception {
        // given
        SyslogFormatter syslogFormatter = newSyslogFormatter("OpenAM", Facility.LOCAL5, "server.name");

        AuditEvent auditEvent = accessEvent()
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .eventName("AM-ACCESS-ATTEMPT")
                .authentication("someone@forgerock.com")
                .resourceOperation("/some/path", "CREST", "action", "reconcile")
                .response("200", 12)
                .toEvent();

        // when
        String formattedEvent = syslogFormatter.format("access", auditEvent.getValue());
        SyslogMessage syslogMessage = readSyslogMessage(formattedEvent);

        // then
        assertThat(syslogMessage.priority).isEqualTo(174);
        assertThat(syslogMessage.syslogSpecVersion).isEqualTo(1);
        assertThat(syslogMessage.timestamp).isEqualTo("2015-03-25T14:21:26.239Z");
        assertThat(syslogMessage.hostname).isEqualTo("server.name");
        assertThat(syslogMessage.appName).isEqualTo("OpenAM");
        assertThat(syslogMessage.msgId).isEqualTo("AM-ACCESS-ATTEMPT");
        assertThat(syslogMessage.structuredDataId).isEqualTo("access.OpenAM@36733");
        assertThat(syslogMessage.structuredData.get("transactionId")).isEqualTo("transactionId");
        assertThat(syslogMessage.structuredData.get("authentication.id")).isEqualTo("someone@forgerock.com");
        assertThat(syslogMessage.structuredData.get("resourceOperation.uri")).isEqualTo("/some/path");
        assertThat(syslogMessage.structuredData.get("resourceOperation.protocol")).isEqualTo("CREST");
        assertThat(syslogMessage.structuredData.get("resourceOperation.operation.method")).isEqualTo("action");
        assertThat(syslogMessage.structuredData.get("resourceOperation.operation.detail")).isEqualTo("reconcile");
        assertThat(syslogMessage.structuredData.get("response.status")).isEqualTo("200");
        assertThat(syslogMessage.structuredData.get("response.elapsedTime")).isEqualTo("12");
        assertThat(syslogMessage.structuredData.get("response.message")).isEqualTo("");
    }

    @Test
    public void setsPriorityBasedOnFacility() throws Exception {
        // given
        SyslogFormatter syslogFormatter = newSyslogFormatter("OpenAM", Facility.LOCAL7, "server.name");

        AuditEvent auditEvent = accessEvent()
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .eventName("AM-ACCESS-ATTEMPT")
                .toEvent();

        // when
        String formattedEvent = syslogFormatter.format("access", auditEvent.getValue());
        SyslogMessage syslogMessage = readSyslogMessage(formattedEvent);

        // then
        assertThat(syslogMessage.priority).isEqualTo(190);
    }

    @Test
    public void replacesNullLocalHostNameWithNIL() throws Exception {
        // given
        SyslogFormatter syslogFormatter = newSyslogFormatter("OpenAM", Facility.LOCAL5, null);

        AuditEvent auditEvent = accessEvent()
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .eventName("AM-ACCESS-ATTEMPT")
                .toEvent();

        // when
        String formattedEvent = syslogFormatter.format("access", auditEvent.getValue());
        SyslogMessage syslogMessage = readSyslogMessage(formattedEvent);

        // then
        assertThat(syslogMessage.hostname).isEqualTo("-");
    }

    @Test
    public void setsAppNameFromConfiguration() throws Exception {
        // given
        SyslogFormatter syslogFormatter = newSyslogFormatter("OpenIDM", Facility.LOCAL5, "server.name");

        AuditEvent auditEvent = accessEvent()
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .eventName("AM-ACCESS-ATTEMPT")
                .toEvent();

        // when
        String formattedEvent = syslogFormatter.format("access", auditEvent.getValue());
        SyslogMessage syslogMessage = readSyslogMessage(formattedEvent);

        // then
        assertThat(syslogMessage.appName).isEqualTo("OpenIDM");
    }

    @Test
    public void createsUniqueStructuredDataIdPerAppNameAndAuditEventTopic() throws Exception {
        // given
        SyslogFormatter syslogFormatter = newSyslogFormatter("OpenDJ", Facility.LOCAL5, "server.name");

        AuditEvent auditEvent = activityEvent()
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .eventName("AM-ACCESS-ATTEMPT")
                .resourceOperation("/managed/users/admin", "CREST", "READ")
                .toEvent();

        // when
        String formattedEvent = syslogFormatter.format("activity", auditEvent.getValue());
        SyslogMessage syslogMessage = readSyslogMessage(formattedEvent);

        // then
        assertThat(syslogMessage.structuredDataId).isEqualTo("activity.OpenDJ@36733");
    }

    @Test
    public void filtersAuditEventFieldsWrittenToStructuredData() throws Exception {
        // given
        SyslogFormatter syslogFormatter = newSyslogFormatter("OpenAM", Facility.LOCAL5, "server.name");

        AuditEvent auditEvent = accessEvent()
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .eventName("AM-ACCESS-ATTEMPT")
                .toEvent();

        // when
        String formattedEvent = syslogFormatter.format("access", auditEvent.getValue());
        SyslogMessage syslogMessage = readSyslogMessage(formattedEvent);

        // then
        // As Syslog is write-only, there's little value in including the _id of the event instance
        assertThat(syslogMessage.structuredData.get("_id")).isNull();
        // As timestamp is already mapped to the TIMESTAMP field, it shouldn't be repeated in the STRUCTURED-DATA
        assertThat(syslogMessage.structuredData.get("timestamp")).isNull();
        // As eventName is already mapped to the MSGID field, it shouldn't be repeated in the STRUCTURED-DATA
        assertThat(syslogMessage.structuredData.get("eventName")).isNull();
    }

    @Test
    public void escapesDoubleQuotesInStructuredDataParamValues() throws Exception {
        // given
        SyslogFormatter syslogFormatter = newSyslogFormatter("OpenAM", Facility.LOCAL5, "server.name");

        AuditEvent auditEvent = accessEvent()
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .eventName("AM-ACCESS-ATTEMPT")
                .authorizationId("managed/user", "aegloff", "openidm-admin", "openidm-authorized")
                .toEvent();

        // when
        String formattedEvent = syslogFormatter.format("access", auditEvent.getValue());
        SyslogMessage syslogMessage = readSyslogMessage(formattedEvent);

        // then
        assertThat(syslogMessage.structuredData.get("authorizationId.roles"))
                .isEqualTo("[ \"openidm-admin\", \"openidm-authorized\" ]");
    }


    private SyslogFormatter newSyslogFormatter(String productName, Facility facility, String localHostName)
            throws Exception {

        Map<String, JsonValue> auditEventDefinitions = loadAuditEventDefinitions();

        SyslogAuditEventHandlerConfiguration config = new SyslogAuditEventHandlerConfiguration();
        config.setProductName(productName);
        config.setFacility(facility);

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

}
