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
import static org.forgerock.audit.events.ActivityAuditEventBuilderTest.OpenProductActivityAuditEventBuilder.*;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.Requests;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class ActivityAuditEventBuilderTest {

    /**
     * Example builder of audit activity events for some imaginary product "OpenProduct".
     */
    static class OpenProductActivityAuditEventBuilder<T extends OpenProductActivityAuditEventBuilder<T>>
            extends ActivityAuditEventBuilder<T> {

        @SuppressWarnings("rawtypes")
        public static OpenProductActivityAuditEventBuilder<?> productActivityEvent() {
            return new OpenProductActivityAuditEventBuilder();
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

        AuditEvent event = productActivityEvent()
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .eventName("AM-REALM-CREATE")
                .trackingId("12345")
                .trackingId("67890")
                .userId("someone@forgerock.com")
                .runAs("admin")
                .objectId("some/resource")
                .operation("customAction")
                .before("{ \"name\": \"Old\", \"revision\": 1 }")
                .after("{ \"name\": \"New\", \"revision\": 2 }")
                .changedFields("name", "revision")
                .revision("2")
                .openField("value")
                .toEvent();

        assertEvent(event);
    }

    @Test
    public void canPopulateResourceOperationFromContextAndRequest() throws Exception {
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put("Content-Length", asList("200"));
        headers.put("Content-Type", asList("application/json"));

        Request request = Requests.newActionRequest("some/resource", "customAction");

        AuditEvent event = productActivityEvent()
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .eventName("AM-REALM-CREATE")
                .userId("someone@forgerock.com")
                .objectIdFromCrestRequest(request)
                .operationFromCrestRequest(request)
                .toEvent();

        assertThat(event.getValue().get(OBJECT_ID).asString().equals("some/resource"));
        assertThat(event.getValue().get(OPERATION).asString().equals("http"));
    }

    @Test
    public void ensureBuilderMethodsCanBeCalledInAnyOrder() {
        AuditEvent event = productActivityEvent()
                .eventName("AM-REALM-CREATE")
                .userId("someone@forgerock.com")
                .trackingId("12345")
                .runAs("admin")
                .objectId("some/resource")
                .operation("customAction")
                .before("{ \"name\": \"Old\", \"revision\": 1 }")
                .trackingId("67890")
                .after("{ \"name\": \"New\", \"revision\": 2 }")
                .changedFields("name", "revision")
                .revision("2")
                .openField("value")
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .toEvent();
        assertEvent(event);
    }

    @Test
    public void beforeAndAfterFieldsCanStoreString() {
        String beforeState = "<rev>1</rev>";
        String afterState = "<rev>2</rev>";

        AuditEvent event = productActivityEvent()
                .timestamp(1427293286239L)
                .transactionId("transactionId")
                .eventName("AM-REALM-CREATE")
                .objectId("thing")
                .operation("UPDATE")
                .before(beforeState)
                .after(afterState)
                .toEvent();

        assertThat(event.getValue().get(BEFORE).isString()).describedAs("before stored as object").isTrue();
        assertThat(event.getValue().get(BEFORE).asString()).isEqualTo(beforeState);
        assertThat(event.getValue().get(AFTER).isString()).describedAs("after stored as object").isTrue();
        assertThat(event.getValue().get(AFTER).asString()).isEqualTo(afterState);
    }

    @Test
    public void beforeAndAfterFieldsCanStoreNavigableJson() {
        JsonValue beforeState = json(object(field("rev", "1")));
        JsonValue afterState = json(object(field("rev", "2")));

        AuditEvent event = productActivityEvent()
                .timestamp(1427293286239L)
                .transactionId("transactionId")
                .eventName("AM-REALM-CREATE")
                .objectId("thing")
                .operation("UPDATE")
                .before(beforeState)
                .after(afterState)
                .toEvent();

        assertThat(event.getValue().get(BEFORE).isMap()).describedAs("before stored as object").isTrue();
        assertThat(event.getValue().get(BEFORE).get("rev").asString()).isEqualTo("1");
        assertThat(event.getValue().get(AFTER).isMap()).describedAs("after stored as object").isTrue();
        assertThat(event.getValue().get(AFTER).get("rev").asString()).isEqualTo("2");
    }

    private void assertEvent(AuditEvent event) {
        JsonValue value = event.getValue();
        assertThat(value.get(TRANSACTION_ID).asString()).isEqualTo("transactionId");
        assertThat(value.get(TIMESTAMP).asString()).isEqualTo("2015-03-25T14:21:26.239Z");
        assertThat(value.get(EVENT_NAME).asString()).isEqualTo("AM-REALM-CREATE");
        assertThat(value.get(TRACKING_IDS).asSet()).containsExactly("12345", "67890");
        assertThat(value.get(USER_ID).asString()).isEqualTo("someone@forgerock.com");
        assertThat(value.get(RUN_AS).asString()).isEqualTo("admin");
        assertThat(value.get(OBJECT_ID).asString()).isEqualTo("some/resource");
        assertThat(value.get(OPERATION).asString()).isEqualTo("customAction");
        assertThat(value.get(BEFORE).asString()).isEqualTo("{ \"name\": \"Old\", \"revision\": 1 }");
        assertThat(value.get(AFTER).asString()).isEqualTo("{ \"name\": \"New\", \"revision\": 2 }");
        assertThat(value.get(CHANGED_FIELDS).asList(String.class)).containsExactly("name", "revision");
        assertThat(value.get(REVISION).asString()).isEqualTo("2");
        assertThat(value.get("open").getObject()).isEqualTo("value");
    }

}
