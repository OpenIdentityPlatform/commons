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
import static org.forgerock.audit.events.AuditEventBuilderTest.OpenProductAuditEventBuilder.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.SecurityContext;
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

    @Test(expectedExceptions= { IllegalStateException.class })
    public void ensureAuditEventContainsMandatoryAttributes() throws Exception {
        productEvent().toEvent();
    }

    @Test
    public void ensureAuditEventContainsTimestampEvenIfNotAdded() throws Exception {
        AuditEvent event = productEvent()
                .eventName("AM-CREST-SUCCESS")
                .transactionId("transactionId")
                .authentication("someone@forgerock.com")
                .toEvent();
        JsonValue value = event.getValue();
        assertThat(value.get(TIMESTAMP).asString()).isNotNull().isNotEmpty();
    }

    @Test(expectedExceptions= { IllegalStateException.class })
    public void ensureAuditEventContainsTransactionId() throws Exception {
        productEvent()
                .eventName("AM-CREST-SUCCESS")
                .timestamp(System.currentTimeMillis())
                .authentication("someone@forgerock.com")
                .toEvent();
    }

    @Test(expectedExceptions= { IllegalStateException.class })
    public void ensureAuditEventContainsEventName() throws Exception {
        productEvent()
                .transactionId("transactionId")
                .timestamp(System.currentTimeMillis())
                .authentication("someone@forgerock.com")
                .toEvent();
    }

    @Test(expectedExceptions= { IllegalStateException.class })
    public void ensureAuditEventContainsAuthentication() throws Exception {
        productEvent()
                .eventName("AM-CREST-SUCCESS")
                .transactionId("transactionId")
                .timestamp(System.currentTimeMillis())
                .toEvent();
    }

    @Test
    public void ensureEventIsCorrectlyBuilt() {
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put("Content-Length", asList("200"));
        headers.put("Content-Type", asList("application/json"));

        AuditEvent event = productEvent()
                .eventName("AM-CREST-SUCCESS")
                .transactionId("transactionId")
                .timestamp(1427293286239l)
                .authentication("someone@forgerock.com")
                .openField("value")
                .toEvent();

        assertEvent(event);
    }

    @Test
    public void ensureBuilderMethodsCanBeCalledInAnyOrder() {
        AuditEvent event = productEvent()
                .authentication("someone@forgerock.com")
                .openField("value")
                .transactionId("transactionId")
                .eventName("AM-CREST-SUCCESS")
                .timestamp(1427293286239l)
                .toEvent();
        assertEvent(event);
    }

    @Test
    public void canPopulateTransactionIdFromRootContext() {
        // Given
        RootContext context = new RootContext();

        // When
        AuditEvent event = productEvent()
                .eventName("AM-CREST-SUCCESS")
                .transactionIdFromRootContext(context)
                .authentication("someone@forgerock.com")
                .toEvent();

        // Then
        JsonValue value = event.getValue();
        assertThat(value.get(TRANSACTION_ID).asString()).isEqualTo(context.getId());
    }

    @Test
    public void canPopulateAuthenticationIdFromSecurityContext() {
        // Given
        RootContext rootContext = new RootContext();
        String authenticationId = "username";
        Context context = new SecurityContext(rootContext, authenticationId, null);

        // When
        AuditEvent event = productEvent()
                .eventName("AM-CREST-SUCCESS")
                .transactionId("transactionId")
                .authenticationFromSecurityContext(context)
                .toEvent();

        // Then
        JsonValue value = event.getValue();
        assertThat(value.get(AUTHENTICATION).get(ID).asString()).isEqualTo(authenticationId);
    }

    private void assertEvent(AuditEvent event) {
        JsonValue value = event.getValue();
        assertThat(value.get(TRANSACTION_ID).asString()).isEqualTo("transactionId");
        assertThat(value.get(TIMESTAMP).asString()).isEqualTo("2015-03-25T14:21:26.239Z");
        assertThat(value.get(AUTHENTICATION).get(ID).asString()).isEqualTo("someone@forgerock.com");
        assertThat(value.get("open").getObject()).isEqualTo("value");
    }

}
