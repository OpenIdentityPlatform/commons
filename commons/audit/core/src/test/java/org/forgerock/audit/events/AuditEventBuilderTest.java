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
import static org.forgerock.audit.events.AuditEventBuilderTest.OpenProductAuditEventBuilder.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.services.TransactionId;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.services.context.TransactionIdContext;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class AuditEventBuilderTest {

    /**
     * Example builder of audit activity events for some imaginary product "OpenProduct".
     */
    static class OpenProductAuditEventBuilder<T extends OpenProductAuditEventBuilder<T>> extends AuditEventBuilder<T> {

        @SuppressWarnings("rawtypes")
        public static OpenProductAuditEventBuilder<?> productEvent() {
            return new OpenProductAuditEventBuilder();
        }

        public T openField(String v) {
            jsonValue.put("open", v);
            return self();
        }

    }

    @Test(expectedExceptions = { IllegalStateException.class })
    public void ensureAuditEventContainsMandatoryAttributes() throws Exception {
        productEvent().toEvent();
    }

    @Test
    public void ensureAuditEventContainsTimestampEvenIfNotAdded() throws Exception {
        AuditEvent event = productEvent()
                .eventName("AM-CREST-SUCCESSFUL")
                .transactionId("transactionId")
                .toEvent();
        JsonValue value = event.getValue();
        assertThat(value.get(TIMESTAMP).asString()).isNotNull().isNotEmpty();
    }

    @Test(expectedExceptions = { IllegalStateException.class })
    public void ensureAuditEventContainsTransactionId() throws Exception {
        productEvent()
                .eventName("AM-CREST-SUCCESSFUL")
                .timestamp(System.currentTimeMillis())
                .toEvent();
    }

    @Test(expectedExceptions = { IllegalStateException.class })
    public void ensureAuditEventContainsEventName() throws Exception {
        productEvent()
                .transactionId("transactionId")
                .timestamp(System.currentTimeMillis())
                .toEvent();
    }

    @Test
    public void auditEventAuthenticationDetailsAreOptional() throws Exception {
        productEvent()
                .eventName("AM-CREST-SUCCESSFUL")
                .transactionId("transactionId")
                .toEvent();
    }

    @Test
    public void ensureEventIsCorrectlyBuilt() {
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put("Content-Length", asList("200"));
        headers.put("Content-Type", asList("application/json"));

        AuditEvent event = productEvent()
                .eventName("AM-CREST-SUCCESSFUL")
                .transactionId("transactionId")
                .timestamp(1427293286239L)
                .userId("someone@forgerock.com")
                .trackingId("12345")
                .openField("value")
                .toEvent();

        assertEvent(event);
    }

    @Test
    public void ensureBuilderMethodsCanBeCalledInAnyOrder() {
        AuditEvent event = productEvent()
                .userId("someone@forgerock.com")
                .openField("value")
                .transactionId("transactionId")
                .eventName("AM-CREST-SUCCESSFUL")
                .timestamp(1427293286239L)
                .trackingId("12345")
                .toEvent();
        assertEvent(event);
    }

    @Test
    public void canPopulateTransactionIdFromTransactionIdContext() {
        // Given
        TransactionId transactionId = new TransactionId();
        Context context = new TransactionIdContext(new RootContext(), transactionId);

        // When
        AuditEvent event = productEvent()
                .eventName("AM-CREST-SUCCESSFUL")
                .transactionIdFromContext(context)
                .toEvent();

        // Then
        JsonValue value = event.getValue();
        assertThat(value.get(TRANSACTION_ID).asString()).isEqualTo(transactionId.getValue());
    }

    private void assertEvent(AuditEvent event) {
        JsonValue value = event.getValue();
        assertThat(value.get(TRANSACTION_ID).asString()).isEqualTo("transactionId");
        assertThat(value.get(TIMESTAMP).asString()).isEqualTo("2015-03-25T14:21:26.239Z");
        assertThat(value.get(USER_ID).asString()).isEqualTo("someone@forgerock.com");
        assertThat(value.get(TRACKING_IDS).asSet()).containsExactly("12345");
        assertThat(value.get("open").getObject()).isEqualTo("value");
    }

}
