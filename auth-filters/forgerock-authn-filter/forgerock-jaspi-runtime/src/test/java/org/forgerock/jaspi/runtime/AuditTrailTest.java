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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.jaspi.runtime;

import org.forgerock.json.fluent.JsonValue;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AuditTrailTest {

    private AuditTrail auditTrail;

    private AuditApi auditApi;

    @BeforeMethod
    public void setUp() {
        auditApi = mock(AuditApi.class);

        auditTrail = new AuditTrail(auditApi);
    }

    @Test
    public void shouldAuditEmptyAuditMessage() {

        //Given

        //When
        auditTrail.audit();

        //Then
        ArgumentCaptor<JsonValue> auditMessageCaptor = ArgumentCaptor.forClass(JsonValue.class);
        verify(auditApi).audit(auditMessageCaptor.capture());

        JsonValue auditMessage = auditMessageCaptor.getValue();
        assertThat(auditMessage.asMap()).hasSize(2);
        assertThat(auditMessage.get("requestId").getObject()).isNotNull();
        assertThat(auditMessage.get("entries").required().size()).isEqualTo(0);
    }

    @Test
    public void shouldAuditFullSuccessfulAuditMessage() {

        //Given
        auditTrail.auditFailure("MODULE_ONE_ID", "MODULE_ONE_REASON",
                Collections.<String, Object>singletonMap("MODULE_ONE", "INFO"));
        auditTrail.auditFailure("MODULE_TWO_ID", "MODULE_TWO_REASON",
                Collections.<String, Object>singletonMap("MODULE_TWO", "INFO"));
        auditTrail.auditSuccess("MODULE_THREE_ID", Collections.<String, Object>singletonMap("MODULE_THREE", "INFO"));
        auditTrail.completeAuditAsSuccessful("PRINCIPAL");
        auditTrail.setSessionId("SESSION_ID");

        //When
        auditTrail.audit();

        //Then
        ArgumentCaptor<JsonValue> auditMessageCaptor = ArgumentCaptor.forClass(JsonValue.class);
        verify(auditApi).audit(auditMessageCaptor.capture());

        JsonValue auditMessage = auditMessageCaptor.getValue();
        assertThat(auditMessage.asMap()).hasSize(5);
        assertThat(auditMessage.get("requestId").getObject()).isNotNull();
        assertThat(auditMessage.get("result").asString()).isEqualTo("SUCCESSFUL");
        assertThat(auditMessage.get("principal").asString()).isEqualTo("PRINCIPAL");
        assertThat(auditMessage.get("sessionId").asString()).isEqualTo("SESSION_ID");
        assertThat(auditMessage.get("entries").required().size()).isEqualTo(3);

        JsonValue moduleOneEntry = auditMessage.get("entries").get(0);
        JsonValue moduleTwoEntry = auditMessage.get("entries").get(1);
        JsonValue moduleThreeEntry = auditMessage.get("entries").get(2);

        assertThat(moduleOneEntry.asMap()).hasSize(4);
        assertThat(moduleOneEntry.get("moduleId").asString()).isEqualTo("MODULE_ONE_ID");
        assertThat(moduleOneEntry.get("result").asString()).isEqualTo("FAILED");
        assertThat(moduleOneEntry.get("reason").asString()).isEqualTo("MODULE_ONE_REASON");
        assertThat(moduleOneEntry.get("info").asMap()).containsOnly(entry("MODULE_ONE", "INFO"));

        assertThat(moduleTwoEntry.asMap()).hasSize(4);
        assertThat(moduleTwoEntry.get("moduleId").asString()).isEqualTo("MODULE_TWO_ID");
        assertThat(moduleTwoEntry.get("result").asString()).isEqualTo("FAILED");
        assertThat(moduleTwoEntry.get("reason").asString()).isEqualTo("MODULE_TWO_REASON");
        assertThat(moduleTwoEntry.get("info").asMap()).containsOnly(entry("MODULE_TWO", "INFO"));

        assertThat(moduleThreeEntry.asMap()).hasSize(3);
        assertThat(moduleThreeEntry.get("moduleId").asString()).isEqualTo("MODULE_THREE_ID");
        assertThat(moduleThreeEntry.get("result").asString()).isEqualTo("SUCCESSFUL");
        assertThat(moduleThreeEntry.get("info").asMap()).containsOnly(entry("MODULE_THREE", "INFO"));
    }

    @Test
    public void shouldAuditFullFailedAuditMessage() {

        //Given
        auditTrail.auditFailure("MODULE_ONE_ID", "MODULE_ONE_REASON",
                Collections.<String, Object>singletonMap("MODULE_ONE", "INFO"));
        auditTrail.auditFailure("MODULE_TWO_ID", "MODULE_TWO_REASON",
                Collections.<String, Object>singletonMap("MODULE_TWO", "INFO"));
        auditTrail.completeAuditAsFailure("PRINCIPAL");

        //When
        auditTrail.audit();

        //Then
        ArgumentCaptor<JsonValue> auditMessageCaptor = ArgumentCaptor.forClass(JsonValue.class);
        verify(auditApi).audit(auditMessageCaptor.capture());

        JsonValue auditMessage = auditMessageCaptor.getValue();
        assertThat(auditMessage.asMap()).hasSize(4);
        assertThat(auditMessage.get("requestId").getObject()).isNotNull();
        assertThat(auditMessage.get("result").asString()).isEqualTo("FAILED");
        assertThat(auditMessage.get("principal").asString()).isEqualTo("PRINCIPAL");
        assertThat(auditMessage.get("entries").required().size()).isEqualTo(2);

        JsonValue moduleOneEntry = auditMessage.get("entries").get(0);
        JsonValue moduleTwoEntry = auditMessage.get("entries").get(1);

        assertThat(moduleOneEntry.asMap()).hasSize(4);
        assertThat(moduleOneEntry.get("moduleId").asString()).isEqualTo("MODULE_ONE_ID");
        assertThat(moduleOneEntry.get("result").asString()).isEqualTo("FAILED");
        assertThat(moduleOneEntry.get("reason").asString()).isEqualTo("MODULE_ONE_REASON");
        assertThat(moduleOneEntry.get("info").asMap()).containsOnly(entry("MODULE_ONE", "INFO"));

        assertThat(moduleTwoEntry.asMap()).hasSize(4);
        assertThat(moduleTwoEntry.get("moduleId").asString()).isEqualTo("MODULE_TWO_ID");
        assertThat(moduleTwoEntry.get("result").asString()).isEqualTo("FAILED");
        assertThat(moduleTwoEntry.get("reason").asString()).isEqualTo("MODULE_TWO_REASON");
        assertThat(moduleTwoEntry.get("info").asMap()).containsOnly(entry("MODULE_TWO", "INFO"));
    }
}
