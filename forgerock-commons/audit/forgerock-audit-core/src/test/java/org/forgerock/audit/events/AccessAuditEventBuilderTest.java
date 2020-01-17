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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.audit.events;


import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.audit.events.AccessAuditEventBuilder.ResponseStatus.SUCCESSFUL;
import static org.forgerock.audit.events.AccessAuditEventBuilderTest.OpenProductAccessAuditEventBuilder.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.services.context.Context;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("javadoc")
public class AccessAuditEventBuilderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper(
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
        Map<String, List<String>> requestHeaders = new HashMap<>();
        requestHeaders.put("Content-Length", singletonList("200"));
        requestHeaders.put("Content-Type", singletonList("application/json"));
        requestHeaders.put("Cookie", singletonList("iPlanetDirectoryPro=sensitive; sessionId=sensitive"));

        Map<String, List<String>> responseHeaders = new HashMap<>();
        responseHeaders.put("Cache-Control", singletonList("max-age=3600"));
        responseHeaders.put("Set-Cookie", singletonList("UserID=JohnDoe; Max-Age=3600; Version=1"));

        Map<String, List<String>> queryParameters = new HashMap<>();
        queryParameters.put("parameter1", asList("value1", "value2"));
        queryParameters.put("parameter2", asList("value1", "value2"));

        Map<String, String> expectedCookies = new HashMap<>();
        expectedCookies.put("iPlanetDirectoryPro", "sensitive");
        expectedCookies.put("sessionId", "sensitive");

        AuditEvent event = productAccessEvent()
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .eventName("IDM-sync-10")
                .trackingId("12345")
                .trackingId("67890")
                .client("cip", 1203)
                .server("sip", 80)
                .userId("someone@forgerock.com")
                .request("CREST", "reconcile")
                .httpRequest(false, "GET", "/some/path", queryParameters, requestHeaders)
                .httpResponse(responseHeaders)
                .response(SUCCESSFUL, "200", 12, TimeUnit.MILLISECONDS)
                .openField("value")
                .toEvent();

        JsonValue value = event.getValue();
        assertThat(value.get(TRANSACTION_ID).asString()).isEqualTo("transactionId");
        assertThat(value.get(TIMESTAMP).asString()).isEqualTo("2015-03-25T14:21:26.239Z");
        assertThat(value.get(EVENT_NAME).asString()).isEqualTo("IDM-sync-10");
        assertThat(value.get(TRACKING_IDS).asSet()).containsExactly("12345", "67890");
        assertThat(value.get(SERVER).get(IP).asString()).isEqualTo("sip");
        assertThat(value.get(SERVER).get(PORT).asLong()).isEqualTo(80);
        assertThat(value.get(HTTP).get(REQUEST).get(METHOD).asString()).isEqualTo("GET");
        assertThat(value.get(HTTP).get(REQUEST).get(HEADERS).asMapOfList(String.class)).isEqualTo(requestHeaders);
        assertThat(value.get(HTTP).get(REQUEST).get(QUERY_PARAMETERS).asMapOfList(String.class))
                .isEqualTo(queryParameters);
        assertThat(value.get(HTTP).get(REQUEST).get(COOKIES).asMap(String.class)).isEqualTo(expectedCookies);
        assertThat(value.get(HTTP).get(RESPONSE).get(HEADERS).asMapOfList(String.class)).isEqualTo(responseHeaders);
        assertThat(value.get(REQUEST).get(PROTOCOL).asString()).isEqualTo("CREST");
        assertThat(value.get(REQUEST).get(OPERATION).asString()).isEqualTo("reconcile");
        assertThat(value.get(RESPONSE).get(STATUS).asString()).isEqualTo("SUCCESSFUL");
        assertThat(value.get(RESPONSE).get(STATUS_CODE).asString()).isEqualTo("200");
        assertThat(value.get(RESPONSE).get(ELAPSED_TIME).asLong()).isEqualTo(12);
        assertThat(value.get(RESPONSE).get(ELAPSED_TIME_UNITS).asString()).isEqualTo(TimeUnit.MILLISECONDS.name());
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
                .userId("someone@forgerock.com")
                .timestamp(1427293286239L)
                .toEvent();
        assertEvent(event1);

        AuditEvent event2 = productAccessEvent()
                .client("cip", 1203)
                .userId("someone@forgerock.com")
                .openField("value")
                .server("ip", 80)
                .eventName("IDM-sync-10")
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .toEvent();
        assertEvent(event2);

        AuditEvent event3 = productAccessEvent()
                .openField("value")
                .transactionId("transactionId")
                .client("cip", 1203)
                .server("ip", 80)
                .userId("someone@forgerock.com")
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .eventName("IDM-sync-10")
                .toEvent();
        assertEvent(event3);

        AuditEvent event4 = productAccessEvent()
                .userId("someone@forgerock.com")
                .transactionId("transactionId")
                .client("cip", 1203)
                .openField("value")
                .server("ip", 80)
                .eventName("IDM-sync-10")
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .toEvent();

        assertEvent(event4);

    }

    @Test
    public void eventWithNoHeader() {
        Map<String, List<String>> headers = Collections.emptyMap();

        AuditEvent event = productAccessEvent()
                .eventName("IDM-sync-10")
                .transactionId("transactionId")
                .userId("someone@forgerock.com")
                .timestamp(1427293286239L)
                .httpRequest(false, "GET", "/some/path", Collections.<String, List<String>>emptyMap(), headers)
                .toEvent();

        JsonValue value = event.getValue();
        assertThat(value.get(TRANSACTION_ID).asString()).isEqualTo("transactionId");
        assertThat(value.get(TIMESTAMP).asString()).isEqualTo("2015-03-25T14:21:26.239Z");
        assertThat(value.get(HTTP).get(REQUEST).get(HEADERS).asMapOfList(String.class)).isEqualTo(headers);
    }

    @Test
    public void canPopulateCookiesFromProperCaseHttpHeader() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Cookie", Collections.singletonList("cookie1=01; cookie2=02; cookie3=03"));

        AuditEvent event = productAccessEvent()
                .eventName("IDM-sync-10")
                .transactionId("transactionId")
                .userId("someone@forgerock.com")
                .timestamp(1427293286239L)
                .httpRequest(false, "GET", "/some/path", Collections.<String, List<String>>emptyMap(), headers)
                .toEvent();

        Map<String, String> cookies = event.getValue().get(HTTP).get(REQUEST).get(COOKIES).asMap(String.class);
        assertThat(cookies).isNotEmpty().hasSize(3);
        assertThat(cookies).containsEntry("cookie1", "01");
        assertThat(cookies).containsEntry("cookie2", "02");
        assertThat(cookies).containsEntry("cookie3", "03");
    }

    @Test
    public void canPopulateCookiesFromLowerCaseHttpHeader() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("cookie", Collections.singletonList("cookie1=01; cookie2=02; cookie3=03"));

        AuditEvent event = productAccessEvent()
                .eventName("IDM-sync-10")
                .transactionId("transactionId")
                .userId("someone@forgerock.com")
                .timestamp(1427293286239L)
                .httpRequest(false, "GET", "/some/path", Collections.<String, List<String>>emptyMap(), headers)
                .toEvent();

        Map<String, String> cookies = event.getValue().get(HTTP).get(REQUEST).get(COOKIES).asMap(String.class);
        assertThat(cookies).isNotEmpty().hasSize(3);
        assertThat(cookies).containsEntry("cookie1", "01");
        assertThat(cookies).containsEntry("cookie2", "02");
        assertThat(cookies).containsEntry("cookie3", "03");
    }

    @Test
    public void canPopulateClientFromClientContext() throws Exception {
        // Given
        Context context = new HttpContext(jsonFromFile("/httpContextAndClientContext.json"),
                Thread.currentThread().getContextClassLoader());

        // When
        AuditEvent event = productAccessEvent()
                .eventName("IDM-sync-10")
                .transactionId("transactionId")
                .userId("someone@forgerock.com")
                .clientFromContext(context)
                .toEvent();

        // Then
        JsonValue value = event.getValue();
        assertThat(value.get(CLIENT).get(IP).asString()).isEqualTo("168.0.0.10");
        assertThat(value.get(CLIENT).get(PORT).asInteger()).isEqualTo(55646);
    }

    @Test
    public void canPopulateServerFromClientContext() throws Exception {
        // Given
        Context context = new HttpContext(jsonFromFile("/httpContextAndClientContext.json"),
                Thread.currentThread().getContextClassLoader());

        // When
        AuditEvent event = productAccessEvent()
                .eventName("IDM-sync-10")
                .transactionId("transactionId")
                .userId("someone@forgerock.com")
                .serverFromContext(context)
                .toEvent();

        // Then
        JsonValue value = event.getValue();
        assertThat(value.get(SERVER).get(IP).asString()).isEqualTo("127.0.0.1");
        assertThat(value.get(SERVER).get(PORT).asInteger()).isEqualTo(8080);
    }

    @Test
    public void canPopulateHttpFromHttpContext() throws Exception {
        // Given
        Context context = new HttpContext(jsonFromFile("/httpContextAndClientContext.json"),
                Thread.currentThread().getContextClassLoader());

        Map<String, Object> expectedHeaders = new LinkedHashMap<>();
        expectedHeaders.put("h1", asList("h1_v1", "h1_v2"));
        expectedHeaders.put("h2", asList("h2_v1", "h2_v2"));
        expectedHeaders.put("Host", singletonList("product.example.com:8080"));

        Map<String, Object> expectedParameters = new LinkedHashMap<>();
        expectedParameters.put("p1", asList("p1_v1", "p1_v2"));
        expectedParameters.put("p2", asList("p2_v1", "p2_v2"));

        // When
        AuditEvent event = productAccessEvent()
                .eventName("IDM-sync-10")
                .transactionId("transactionId")
                .userId("someone@forgerock.com")
                .httpFromContext(context)
                .toEvent();

        // Then
        JsonValue value = event.getValue();
        final JsonValue httpRequest = value.get(HTTP).get(REQUEST);
        assertThat(httpRequest.get(METHOD).asString()).isEqualTo("GET");
        assertThat(httpRequest.get(PATH).asString()).isEqualTo("http://product.example.com:8080/path");
        assertThat(httpRequest.get(QUERY_PARAMETERS).asMapOfList(String.class)).isEqualTo(expectedParameters);
        assertThat(httpRequest.get(HEADERS).asMap()).isEqualTo(expectedHeaders);
        Map<String, String> cookies = event.getValue().get(HTTP).get(REQUEST).get(COOKIES).asMap(String.class);
        assertThat(cookies).isNotEmpty().hasSize(3);
        assertThat(cookies).containsEntry("cookie1", "01");
        assertThat(cookies).containsEntry("cookie2", "02");
        assertThat(cookies).containsEntry("cookie3", "03");
    }

    @Test
    public void canPopulateResourceOperationFromActionRequest() {
        // Given
        Request actionRequest = Requests.newActionRequest("resourceName", "actionId");

        // When
        AuditEvent event = productAccessEvent()
                .eventName("IDM-sync-10")
                .transactionId("transactionId")
                .userId("someone@forgerock.com")
                .requestFromCrestRequest(actionRequest)
                .toEvent();

        // Then
        JsonValue value = event.getValue();
        assertThat(value.get(REQUEST).get(PROTOCOL).asString()).isEqualTo("CREST");
        assertThat(value.get(REQUEST).get(OPERATION).asString()).isEqualTo("ACTION");
        assertThat(value.get(REQUEST).get(DETAIL).get("action").asString()).isEqualTo("actionId");
    }

    @Test
    public void canPopulateResourceOperationFromRequest() {
        // Given
        Request deleteRequest = Requests.newDeleteRequest("resourceName");

        // When
        AuditEvent event = productAccessEvent()
                .eventName("IDM-sync-10")
                .transactionId("transactionId")
                .userId("someone@forgerock.com")
                .requestFromCrestRequest(deleteRequest)
                .toEvent();

        // Then
        JsonValue value = event.getValue();
        assertThat(value.get(REQUEST).get(PROTOCOL).asString()).isEqualTo("CREST");
        assertThat(value.get(REQUEST).get(OPERATION).asString()).isEqualTo("DELETE");
    }

    @Test
    public void canPopulateServerFromContext() throws Exception {
        // Given
        Context context = new HttpContext(jsonFromFile("/httpContextAndClientContext.json"),
                Thread.currentThread().getContextClassLoader());

        // When
        AuditEvent event = productAccessEvent()
                .eventName("IDM-sync-10")
                .transactionId("transactionId")
                .userId("someone@forgerock.com")
                .serverFromContext(context)
                .toEvent();

        // Then
        JsonValue value = event.getValue();
        assertThat(value.get(SERVER).get(PORT).asInteger()).isEqualTo(8080);
        assertThat(value.get(SERVER).get(IP).asString()).isEqualTo("127.0.0.1");
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
        assertThat(value.get(USER_ID).asString()).isEqualTo("someone@forgerock.com");
    }

    private JsonValue jsonFromFile(String resourceFilePath) throws IOException {
        final InputStream configStream = AccessAuditEventBuilderTest.class.getResourceAsStream(resourceFilePath);
        return new JsonValue(MAPPER.readValue(configStream, Map.class));
    }
}
