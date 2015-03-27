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
package org.forgerock.audit.event;

import static org.fest.assertions.api.Assertions.*;
import static org.forgerock.audit.event.AuditEventBuilderTest.OpenProductAccessAuditEventBuilder.*;

import org.forgerock.json.fluent.JsonValue;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class AuditEventBuilderTest {

    @Test(expectedExceptions= { IllegalStateException.class })
    public void ensureAuditEventContainsMandatoryAttributes() throws Exception {
        productAccessEvent().toEvent();
    }

    @Test
    public void ensureAuditEventContainsTimestampEvenIfNotAdded() throws Exception {
        AuditEvent event = productAccessEvent()
                .transactionId("transactionId")
                .toEvent();
        JsonValue value = event.getValue();
        assertThat(value.get("timestamp").asString()).isNotNull().isNotEmpty();
    }

    @Test(expectedExceptions= { IllegalStateException.class })
    public void ensureAuditEventContainsTransactionId() throws Exception {
        productAccessEvent()
                .timestamp("timestamp")
                .toEvent();
    }

    /**
     * Example builder of audit access events for some imaginary product "OpenProduct".
     */
    @SuppressWarnings("rawtypes")
    static class OpenProductAccessAuditEventBuilder<T extends OpenProductAccessAuditEventBuilder<T>>
        extends AuditEventBuilder.AccessAuditEventBuilder<T> {

        private OpenProductAccessAuditEventBuilder() {
            super();
        }

        public static <T> OpenProductAccessAuditEventBuilder<?> productAccessEvent() {
            return new OpenProductAccessAuditEventBuilder();
        }

        public T openField(String v) {
            jsonValue.put("open", v);
            return self();
        }

        @Override
        protected T self() {
            return (T) this;
        }
    }

    @Test
    public void ensureEventIsCorrectlyBuilt() {
        AuditEvent event = productAccessEvent()
                .transactionId("transactionId")
                .timestamp("timestamp")
                .messageId("IDM-sync-10")
                .client("cip", "cport")
                .server("sip", "sport")
                .authorizationId("managed/user", "aegloff", "openidm-admin", "openidm-authorized")
                .authenticationId("someone@forgerock.com")
                .resourceOperation("action", "reconcile")
                .http("GET", "/some/path", "p1=v1&p2=v2", "Content-Length: 200", "Content-Type: application/json")
                .response("200", "12")
                .openField("value")
                .aField("field", "fieldValue")
                .toEvent();

        JsonValue value = event.getValue();
        assertThat(value.get("transactionId").asString()).isEqualTo("transactionId");
        assertThat(value.get("messageId").asString()).isEqualTo("IDM-sync-10");
        assertThat(value.get("server").get("ip").asString()).isEqualTo("sip");
        assertThat(value.get("server").get("port").asString()).isEqualTo("sport");
        assertThat(value.get("http").get("method").asString()).isEqualTo("GET");
        assertThat(value.get("authorizationId").get("id").asString()).isEqualTo("aegloff");
        assertThat(value.get("resourceOperation").get("method").asString()).isEqualTo("action");
        assertThat(value.get("response").get("status").asString()).isEqualTo("200");
        assertThat(value.get("open").asString()).isEqualTo("value");
        assertThat(value.get("field").asString()).isEqualTo("fieldValue");
    }


    @Test
    public void ensureBuilderMethodsCanBeCalledInAnyOrder() {
        AuditEvent event1 = productAccessEvent()
                .server("ip", "port")
                .client("cip", "cport")
                .openField("value")
                .transactionId("transactionId")
                .timestamp("timestamp")
                .toEvent();
        assertEvent(event1);

        AuditEvent event2 = productAccessEvent()
                .client("cip", "cport")
                .openField("value")
                .server("ip", "port")
                .transactionId("transactionId")
                .timestamp("timestamp")
                .toEvent();
        assertEvent(event2);

        AuditEvent event3 = productAccessEvent()
                .openField("value")
                .transactionId("transactionId")
                .client("cip", "cport")
                .server("ip", "port")
                .transactionId("transactionId")
                .timestamp("timestamp")
                .toEvent();
        assertEvent(event3);

        AuditEvent event4 = productAccessEvent()
                .transactionId("transactionId")
                .client("cip", "cport")
                .openField("value")
                .server("ip", "port")
                .transactionId("transactionId")
                .timestamp("timestamp")
                .toEvent();

        assertEvent(event4);

    }

    private void assertEvent(AuditEvent event) {
        JsonValue value = event.getValue();
        assertThat(value.get("open").getObject()).isEqualTo("value");
        assertThat(value.get("server").get("ip").asString()).isEqualTo("ip");
        assertThat(value.get("server").get("port").asString()).isEqualTo("port");
        assertThat(value.get("client").get("ip").asString()).isEqualTo("cip");
        assertThat(value.get("client").get("port").asString()).isEqualTo("cport");
        assertThat(value.get("transactionId").asString()).isEqualTo("transactionId");
    }
}
