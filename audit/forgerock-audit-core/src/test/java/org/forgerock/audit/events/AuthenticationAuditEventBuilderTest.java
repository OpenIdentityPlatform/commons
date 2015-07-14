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
import static org.forgerock.audit.events.AuthenticationAuditEventBuilderTest.OpenProductAuthenticationAuditEventBuilder.*;
import static org.forgerock.json.fluent.JsonValue.array;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class AuthenticationAuditEventBuilderTest {

    /**
     * Example builder of audit authentication events for some imaginary product "OpenProduct".
     */
    static class OpenProductAuthenticationAuditEventBuilder<T extends OpenProductAuthenticationAuditEventBuilder<T>>
            extends AuthenticationAuditEventBuilder<T> {

        @SuppressWarnings("rawtypes")
        public static OpenProductAuthenticationAuditEventBuilder<?> productAuthenticationEvent() {
            return new OpenProductAuthenticationAuditEventBuilder();
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

        AuditEvent event = productAuthenticationEvent()
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .eventName("AM-AUTHENTICATION-SUCCESS")
                .authentication("someone@forgerock.com")
                .sessionId("sessionId")
                .principal(new LinkedList<String>() {{
                    add("admin");
                }})
                .context(new LinkedHashMap<String, Object>() {{
                    put("contextKey", "contextValue");
                }})
                .entries(new LinkedList<Map<String, Object>>() {{
                    add(new LinkedHashMap<String, Object>() {{
                        put("moduleId", "datastore");
                        put("result", Status.SUCCESSFUL.toString());
                        put("info", new LinkedHashMap<>());
                    }});
                }})
                .result(Status.SUCCESSFUL)
                .openField("value")
                .toEvent();

        assertEvent(event);
    }

    @Test
    public void ensureBuilderMethodsCanBeCalledInAnyOrder() {
        AuditEvent event = productAuthenticationEvent()
                .eventName("AM-AUTHENTICATION-SUCCESS")
                .authentication("someone@forgerock.com")
                .sessionId("sessionId")
                .principal(new LinkedList<String>() {{
                    add("admin");
                }})
                .context(new LinkedHashMap<String, Object>() {{
                    put("contextKey", "contextValue");
                }})
                .entries(new LinkedList<Map<String, Object>>() {{
                    add(new LinkedHashMap<String, Object>() {{
                        put("moduleId", "datastore");
                        put("result", Status.SUCCESSFUL.toString());
                        put("info", new LinkedHashMap<>());
                    }});
                }})
                .result(Status.SUCCESSFUL)
                .openField("value")
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .toEvent();
        assertEvent(event);
    }

    private void assertEvent(AuditEvent event) {
        JsonValue value = event.getValue();
        assertThat(value.get(TRANSACTION_ID).asString()).isEqualTo("transactionId");
        assertThat(value.get(TIMESTAMP).asString()).isEqualTo("2015-03-25T14:21:26.239Z");
        assertThat(value.get(EVENT_NAME).asString()).isEqualTo("AM-AUTHENTICATION-SUCCESS");
        assertThat(value.get(AUTHENTICATION).get(ID).asString()).isEqualTo("someone@forgerock.com");
        assertThat(value.get(RESULT).asEnum(Status.class)).isEqualTo(Status.SUCCESSFUL);
        assertThat(value.get(SESSION_ID).asString()).isEqualTo("sessionId");
        assertThat(value.get(PRINCIPAL).asList()).containsExactly("admin");
        assertThat(value.get(CONTEXT).getObject()).isNotNull();
        assertThat(value.get(CONTEXT).get("contextKey").asString()).isEqualTo("contextValue");
        assertThat(value.get(ENTRIES).getObject()).isNotNull();
        assertThat(value.get(ENTRIES).get(0).get("moduleId").asString()).isEqualTo("datastore");
        assertThat(value.get(ENTRIES).get(0).get("result").asEnum(Status.class)).isEqualTo(Status.SUCCESSFUL);
        assertThat(value.get(ENTRIES).get(0).get("info").getObject()).isNotNull();
        assertThat(value.get("open").getObject()).isEqualTo("value");
    }

}
