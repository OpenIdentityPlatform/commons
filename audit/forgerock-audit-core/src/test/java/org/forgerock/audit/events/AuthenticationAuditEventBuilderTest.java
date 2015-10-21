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
import static org.assertj.core.api.Assertions.*;
import static org.forgerock.audit.events.AuthenticationAuditEventBuilderTest.OpenProductAuthenticationAuditEventBuilder.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.json.JsonValue;
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
                .userId("someone@forgerock.com")
                .eventName("AM-AUTHENTICATION-SUCCESSFUL")
                .trackingId("12345")
                .trackingId("67890")
                .principal(Collections.singletonList("admin"))
                .context(Collections.<String, Object>singletonMap("contextKey", "contextValue"))
                .entries(Collections.singletonList(authenticationModule()))
                .result(Status.SUCCESSFUL)
                .openField("value")
                .toEvent();

        assertEvent(event);
    }

    @Test
    public void ensureBuilderMethodsCanBeCalledInAnyOrder() {
        AuditEvent event = productAuthenticationEvent()
                .eventName("AM-AUTHENTICATION-SUCCESSFUL")
                .userId("someone@forgerock.com")
                .trackingId("12345")
                .principal(Collections.singletonList("admin"))
                .context(Collections.<String, Object>singletonMap("contextKey", "contextValue"))
                .entries(Collections.singletonList(authenticationModule()))
                .trackingId("67890")
                .result(Status.SUCCESSFUL)
                .openField("value")
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .toEvent();
        assertEvent(event);
    }

    private Map<String, Object> authenticationModule() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("moduleId", "datastore");
        result.put("result", Status.SUCCESSFUL.toString());
        result.put("info", new LinkedHashMap<>());
        return result;
    }

    private void assertEvent(AuditEvent event) {
        JsonValue value = event.getValue();
        assertThat(value.get(TRANSACTION_ID).asString()).isEqualTo("transactionId");
        assertThat(value.get(TIMESTAMP).asString()).isEqualTo("2015-03-25T14:21:26.239Z");
        assertThat(value.get(EVENT_NAME).asString()).isEqualTo("AM-AUTHENTICATION-SUCCESSFUL");
        assertThat(value.get(USER_ID).asString()).isEqualTo("someone@forgerock.com");
        assertThat(value.get(RESULT).asEnum(Status.class)).isEqualTo(Status.SUCCESSFUL);
        assertThat(value.get(TRACKING_IDS).asSet()).containsExactly("12345", "67890");
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
