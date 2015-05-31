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

import java.util.HashMap;
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
                .timestamp(1427293286239l)
                .eventType("AM-AUTHENTICATION-SUCCESS")
                .authentication("someone@forgerock.com", Operation.LOGIN, Status.SUCCEEDED, "Module", "DataStore")
                .openField("value")
                .toEvent();

        assertEvent(event);
    }

    @Test
    public void ensureBuilderMethodsCanBeCalledInAnyOrder() {
        AuditEvent event = productAuthenticationEvent()
                .eventType("AM-AUTHENTICATION-SUCCESS")
                .authentication("someone@forgerock.com", Operation.LOGIN, Status.SUCCEEDED, "Module", "DataStore")
                .openField("value")
                .transactionId("transactionId")
                .timestamp(1427293286239l)
                .toEvent();
        assertEvent(event);
    }

    private void assertEvent(AuditEvent event) {
        JsonValue value = event.getValue();
        assertThat(value.get(TRANSACTION_ID).asString()).isEqualTo("transactionId");
        assertThat(value.get(TIMESTAMP).asString()).isEqualTo("2015-03-25T14:21:26.239Z");
        assertThat(value.get(EVENT_NAME).asString()).isEqualTo("AM-AUTHENTICATION-SUCCESS");
        assertThat(value.get(AUTHENTICATION).get(ID).asString()).isEqualTo("someone@forgerock.com");
        assertThat(value.get(AUTHENTICATION).get(OPERATION).asString()).isEqualTo("LOGIN");
        assertThat(value.get(AUTHENTICATION).get(STATUS).asString()).isEqualTo("SUCCEEDED");
        assertThat(value.get(AUTHENTICATION).get(METHOD).get(TYPE).asString()).isEqualTo("Module");
        assertThat(value.get(AUTHENTICATION).get(METHOD).get(DETAIL).asString()).isEqualTo("DataStore");
        assertThat(value.get("open").getObject()).isEqualTo("value");
    }

}
