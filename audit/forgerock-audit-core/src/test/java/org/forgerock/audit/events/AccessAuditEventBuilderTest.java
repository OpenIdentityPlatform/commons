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
package org.forgerock.audit.events;

import static java.util.Arrays.*;
import static org.fest.assertions.api.Assertions.*;
import static org.forgerock.audit.events.AccessAuditEventBuilderTest.OpenProductAccessAuditEventBuilder.*;
import static org.forgerock.audit.events.AuditEventBuilder.ID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.servlet.HttpContext;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class AccessAuditEventBuilderTest {

    private static final ObjectMapper mapper = new ObjectMapper(
            new JsonFactory().configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true));

    /**
     * Example builder of audit access events for some imaginary product "OpenProduct".
     */
    static class OpenProductAccessAuditEventBuilder<T extends OpenProductAccessAuditEventBuilder<T>>
            extends AccessAuditEventBuilder<T> {

        @SuppressWarnings("rawtypes")
        public static OpenProductAccessAuditEventBuilder<?> productAccessEvent() {
            return new OpenProductAccessAuditEventBuilder();
        }

        public T openField(String v) {
            jsonValue.put("open", v);
            return self();
        }

    }

    @Test
    public void ensureEventIsCorrectlyBuilt() {
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put("Content-Length", asList("200"));
        headers.put("Content-Type", asList("application/json"));

        AuditEvent event = productAccessEvent()
                .transactionId("transactionId")
                .timestamp(1427293286239l)
                .eventName("IDM-sync-10")
                .client("cip", 1203)
                .server("sip", 80)
                .authorizationId("managed/user", "aegloff", "openidm-admin", "openidm-authorized")
                .authentication("someone@forgerock.com")
                .resourceOperation("action", "reconcile")
                .http("GET", "/some/path", "p1=v1&p2=v2", headers)
                .response("200", 12)
                .openField("value")
                .toEvent();

        JsonValue value = event.getValue();
        assertThat(value.get(TRANSACTION_ID).asString()).isEqualTo("transactionId");
        assertThat(value.get(TIMESTAMP).asString()).isEqualTo("2015-03-25T14:21:26.239Z");
        assertThat(value.get(EVENT_NAME).asString()).isEqualTo("IDM-sync-10");
        assertThat(value.get(SERVER).get(IP).asString()).isEqualTo("sip");
        assertThat(value.get(SERVER).get(PORT).asLong()).isEqualTo(80);
        assertThat(value.get(HTTP).get(METHOD).asString()).isEqualTo("GET");
        assertThat(value.get(HTTP).get(HEADERS).asMapOfList(String.class)).isEqualTo(headers);
        assertThat(value.get(AUTHORIZATION_ID).get(ID).asString()).isEqualTo("aegloff");
        assertThat(value.get(RESOURCE_OPERATION).get(METHOD).asString()).isEqualTo("action");
        assertThat(value.get(RESPONSE).get(STATUS).asString()).isEqualTo("200");
        assertThat(value.get(RESPONSE).get(ELAPSED_TIME).asLong()).isEqualTo(12);
        assertThat(value.get("open").getObject()).isEqualTo("value");
    }

    @Test
    public void ensureBuilderMethodsCanBeCalledInAnyOrder() {
        AuditEvent event1 = productAccessEvent()
                .eventName("IDM-sync-10")
                .server("ip", 80)
                .client("cip", 1203)
                .openField("value")
                .transactionId("transactionId")
                .authentication("someone@forgerock.com")
                .timestamp(1427293286239l)
                .toEvent();
        assertEvent(event1);

        AuditEvent event2 = productAccessEvent()
                .client("cip", 1203)
                .authentication("someone@forgerock.com")
                .openField("value")
                .server("ip", 80)
                .eventName("IDM-sync-10")
                .transactionId("transactionId")
                .timestamp(1427293286239l)
                .toEvent();
        assertEvent(event2);

        AuditEvent event3 = productAccessEvent()
                .openField("value")
                .transactionId("transactionId")
                .client("cip", 1203)
                .server("ip", 80)
                .authentication("someone@forgerock.com")
                .transactionId("transactionId")
                .timestamp(1427293286239l)
                .eventName("IDM-sync-10")
                .toEvent();
        assertEvent(event3);

        AuditEvent event4 = productAccessEvent()
                .authentication("someone@forgerock.com")
                .transactionId("transactionId")
                .client("cip", 1203)
                .openField("value")
                .server("ip", 80)
                .eventName("IDM-sync-10")
                .transactionId("transactionId")
                .timestamp(1427293286239l)
                .toEvent();

        assertEvent(event4);

    }

    @Test
    public void eventWithNoHeader() {
        Map<String, List<String>> headers = Collections.<String, List<String>>emptyMap();

        AuditEvent event = productAccessEvent()
                .eventName("IDM-sync-10")
                .transactionId("transactionId")
                .authentication("someone@forgerock.com")
                .timestamp(1427293286239l)
                .http("GET", "/some/path", "p1=v1&p2=v2", headers)
                .toEvent();

        JsonValue value = event.getValue();
        assertThat(value.get(TRANSACTION_ID).asString()).isEqualTo("transactionId");
        assertThat(value.get(TIMESTAMP).asString()).isEqualTo("2015-03-25T14:21:26.239Z");
        assertThat(value.get(HTTP).get(HEADERS).asMapOfList(String.class)).isEqualTo(headers);
    }

    @Test
    public void canPopulateClientFromHttpContext() throws Exception {
        // Given
        HttpContext httpContext = new HttpContext(jsonFromFile("/httpContext.json"), null);
        DnsUtils dnsUtils = mock(DnsUtils.class);
        given(dnsUtils.getHostName("168.0.0.10")).willReturn("hostname");

        // When
        AuditEvent event = productAccessEvent()
                .eventName("IDM-sync-10")
                .transactionId("transactionId")
                .authentication("someone@forgerock.com")
                .clientFromHttpContext(httpContext, dnsUtils)
                .toEvent();

        // Then
        JsonValue value = event.getValue();
        assertThat(value.get(CLIENT).get(IP).asString()).isEqualTo("168.0.0.10");
        assertThat(value.get(CLIENT).get(HOST).asString()).isEqualTo("hostname");
    }

    @Test
    public void canPopulateHttpFromHttpContext() throws Exception {
        // Given
        HttpContext httpContext = new HttpContext(jsonFromFile("/httpContext.json"), null);

        Map<String, Object> expectedHeaders = new LinkedHashMap<String, Object>();
        expectedHeaders.put("h1", Arrays.asList("h1_v1", "h1_v2"));
        expectedHeaders.put("h2", Arrays.asList("h2_v1", "h2_v2"));

        // When
        AuditEvent event = productAccessEvent()
                .eventName("IDM-sync-10")
                .transactionId("transactionId")
                .authentication("someone@forgerock.com")
                .httpFromHttpContext(httpContext)
                .toEvent();

        // Then
        JsonValue value = event.getValue();
        assertThat(value.get(HTTP).get(METHOD).asString()).isEqualTo("GET");
        assertThat(value.get(HTTP).get(PATH).asString()).isEqualTo("http://product.example.com:8080/path");
        assertThat(value.get(HTTP).get(QUERY_STRING).asString()).isEqualTo("p1=p1_v1&p1=p1_v2&p2=p2_v1&p2=p2_v2");
        assertThat(value.get(HTTP).get(HEADERS).asMap()).isEqualTo(expectedHeaders);
    }

    @Test
    public void canPopulateResourceOperationFromActionRequest() {
        // Given
        Request actionRequest = Requests.newActionRequest("resourceName", "actionId");

        // When
        AuditEvent event = productAccessEvent()
                .eventName("IDM-sync-10")
                .transactionId("transactionId")
                .authentication("someone@forgerock.com")
                .resourceOperationFromRequest(actionRequest)
                .toEvent();

        // Then
        JsonValue value = event.getValue();
        assertThat(value.get(RESOURCE_OPERATION).get(METHOD).asString()).isEqualTo("ACTION");
        assertThat(value.get(RESOURCE_OPERATION).get(ACTION).asString()).isEqualTo("actionId");
    }

    @Test
    public void canPopulateResourceOperationFromRequest() {
        // Given
        Request deleteRequest = Requests.newDeleteRequest("resourceName");

        // When
        AuditEvent event = productAccessEvent()
                .eventName("IDM-sync-10")
                .transactionId("transactionId")
                .authentication("someone@forgerock.com")
                .resourceOperationFromRequest(deleteRequest)
                .toEvent();

        // Then
        JsonValue value = event.getValue();
        assertThat(value.get(RESOURCE_OPERATION).get(METHOD).asString()).isEqualTo("DELETE");
        assertThat(value.get(RESOURCE_OPERATION).isDefined(ACTION)).isFalse();
    }

    private void assertEvent(AuditEvent event) {
        JsonValue value = event.getValue();
        assertThat(value.get("open").getObject()).isEqualTo("value");
        assertThat(value.get(SERVER).get(IP).asString()).isEqualTo("ip");
        assertThat(value.get(SERVER).get(PORT).asLong()).isEqualTo(80);
        assertThat(value.get(CLIENT).get(IP).asString()).isEqualTo("cip");
        assertThat(value.get(CLIENT).get(PORT).asLong()).isEqualTo(1203);
        assertThat(value.get(TRANSACTION_ID).asString()).isEqualTo("transactionId");
        assertThat(value.get(TIMESTAMP).asString()).isEqualTo("2015-03-25T14:21:26.239Z");
        assertThat(value.get(AUTHENTICATION).get(ID).asString()).isEqualTo("someone@forgerock.com");
    }

    private JsonValue jsonFromFile(String resourceFilePath) throws IOException {
        final InputStream configStream = AccessAuditEventBuilderTest.class.getResourceAsStream(resourceFilePath);
        return new JsonValue(mapper.readValue(configStream, Map.class));
    }
}
