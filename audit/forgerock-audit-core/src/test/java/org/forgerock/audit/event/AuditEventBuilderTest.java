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

import static org.fest.assertions.api.Assertions.assertThat;
import static org.forgerock.audit.event.AuditEventBuilderTest.OpenProductAccessAuditEventBuilder.productAccessEvent;

import org.forgerock.audit.event.AuditEvent;
import org.forgerock.audit.event.AuditEventBuilder;
import org.forgerock.json.fluent.JsonValue;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class AuditEventBuilderTest {

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
        assertThat(value.get("transactionId").getObject()).isEqualTo("transactionId");
        assertThat(value.get("messageId").getObject()).isEqualTo("IDM-sync-10");
        assertThat(value.get("server").get("ip").getObject()).isEqualTo("sip");
        assertThat(value.get("server").get("port").getObject()).isEqualTo("sport");
        assertThat(value.get("http").get("method").getObject()).isEqualTo("GET");
        assertThat(value.get("authorizationId").get("id").getObject()).isEqualTo("aegloff");
        assertThat(value.get("resourceOperation").get("method").getObject()).isEqualTo("action");
        assertThat(value.get("response").get("status").getObject()).isEqualTo("200");
        assertThat(value.get("open").getObject()).isEqualTo("value");
        assertThat(value.get("field").getObject()).isEqualTo("fieldValue");
    }


    @Test
    public void ensureBuilderMethodsCanBeCalledInAnyOrder() {
        AuditEvent event1 = productAccessEvent()
                .server("ip", "port")
                .client("cip", "cport")
                .openField("value")
                .transactionId("transactionId").toEvent();
        assertEvent(event1);

        AuditEvent event2 = productAccessEvent()
                .client("cip", "cport")
                .openField("value")
                .server("ip", "port")
                .transactionId("transactionId").toEvent();
        assertEvent(event2);

        AuditEvent event3 = productAccessEvent()
                .openField("value")
                .transactionId("transactionId")
                .client("cip", "cport")
                .server("ip", "port").toEvent();
        assertEvent(event3);

        AuditEvent event4 = productAccessEvent()
                .transactionId("transactionId")
                .client("cip", "cport")
                .openField("value")
                .server("ip", "port").toEvent();
        assertEvent(event4);

    }

    private void assertEvent(AuditEvent event) {
        JsonValue value = event.getValue();
        assertThat(value.get("open").getObject()).isEqualTo("value");
        assertThat(value.get("server").get("ip").getObject()).isEqualTo("ip");
        assertThat(value.get("server").get("port").getObject()).isEqualTo("port");
        assertThat(value.get("client").get("ip").getObject()).isEqualTo("cip");
        assertThat(value.get("client").get("port").getObject()).isEqualTo("cport");
        assertThat(value.get("transactionId").getObject()).isEqualTo("transactionId");
    }
}
