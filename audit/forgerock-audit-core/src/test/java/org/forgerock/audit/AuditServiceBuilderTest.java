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

package org.forgerock.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.audit.AuditServiceBuilder.newAuditService;
import static org.forgerock.audit.IOUtils.jsonFromFile;
import static org.mockito.Mockito.*;

import org.forgerock.audit.AuditServiceBuilder.AuditServiceFactory;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"javadoc", "rawtypes", "unchecked" })
public class AuditServiceBuilderTest {

    private ArgumentCaptor<Map> topicSchemasCaptor;
    private ArgumentCaptor<Map> handlersByNameCaptor;
    private ArgumentCaptor<Map> handlersByTopicCaptor;

    @BeforeMethod
    private void setUp() {
        topicSchemasCaptor = ArgumentCaptor.forClass(Map.class);
        handlersByNameCaptor = ArgumentCaptor.forClass(Map.class);
        handlersByTopicCaptor = ArgumentCaptor.forClass(Map.class);
    }

    @Test
    public void shouldLoadAndInjectCoreTopicSchemasIntoAuditService() throws Exception {
        // Given
        final AuditService auditService = newAuditService().build();
        auditService.startup();

        // When
        Set<String> knownTopics = auditService.getKnownTopics();

        // Then
        assertThat(knownTopics).containsOnly("access", "activity", "authentication", "config");
    }

    @Test
    public void shouldInjectEventsMetaDataIntoAuditEventHandlers() throws Exception {
        AuditEventHandler<?> auditEventHandler = mock(AuditEventHandler.class);
        newAuditService()
                .withAuditEventHandler(auditEventHandler, "mock", Collections.singleton("access"))
                .build();
        final ArgumentCaptor<Map> auditEventMetaDataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(auditEventHandler).setAuditEventsMetaData(auditEventMetaDataCaptor.capture());

        Map<String, JsonValue> auditEventMetaData = auditEventMetaDataCaptor.getValue();
        assertThat(auditEventMetaData).containsKey("access");
        JsonValue accessMetaData = auditEventMetaData.get("access");
        assertThat(accessMetaData.isDefined("schema")).isTrue();
    }

    @Test
    public void shouldInjectDependencyProviderIntoAuditEventHandlers() throws Exception {
        DependencyProvider dependencyProvider = mock(DependencyProvider.class);
        when(dependencyProvider.getDependency(Integer.class)).thenReturn(4);
        AuditEventHandler<?> auditEventHandler = mock(AuditEventHandler.class);

        newAuditService()
                .withDependencyProvider(dependencyProvider)
                .withAuditEventHandler(auditEventHandler, "mock", Collections.singleton("access"))
                .build();

        verify(auditEventHandler).setDependencyProvider(eq(dependencyProvider));
    }

    @Test
    public void shouldPermitAdditionalFieldsToBeAddedToCoreTopicSchemas() throws Exception {
        // Given
        final JsonValue schemaExtensions = loadJson("validCoreTopicSchemaExtension.json");
        final AuditServiceFactory factory = mock(AuditServiceFactory.class);
        final AuditServiceBuilder auditServiceBuilder =
                new AuditServiceBuilder(factory).withCoreTopicSchemaExtensions(schemaExtensions);

        // When
        auditServiceBuilder.build();

        // Then
        verify(factory).newAuditService(
                any(AuditServiceConfiguration.class),
                topicSchemasCaptor.capture(),
                handlersByNameCaptor.capture(),
                handlersByTopicCaptor.capture());

        JsonValue accessSchema = (JsonValue) topicSchemasCaptor.getValue().get("access");
        assertThat(accessSchema.get(pointer("schema/properties")).isDefined("extraField")).isTrue();
    }

    @Test
    public void shouldRejectCoreTopicSchemasExtensionForUnknownTopic() throws Exception {
        // Given
        final JsonValue schemaExtensions = loadJson("validAdditionalTopicSchema.json");
        final AuditServiceFactory factory = mock(AuditServiceFactory.class);
        final AuditServiceBuilder auditServiceBuilder =
                new AuditServiceBuilder(factory).withCoreTopicSchemaExtensions(schemaExtensions);

        // When
        auditServiceBuilder.build();

        // Then
        verify(factory).newAuditService(
                any(AuditServiceConfiguration.class),
                topicSchemasCaptor.capture(),
                handlersByNameCaptor.capture(),
                handlersByTopicCaptor.capture());

        assertThat(topicSchemasCaptor.getValue().containsKey("customTopic")).isFalse();
    }

    @Test
    public void shouldPreventCoreTopicSchemaExistingFieldsFromBeingAltered() throws Exception {
        final JsonValue schemaExtensions = loadJson("invalidCoreTopicSchemaExtension.json");
        final AuditServiceFactory factory = mock(AuditServiceFactory.class);
        final AuditServiceBuilder auditServiceBuilder =
                new AuditServiceBuilder(factory).withCoreTopicSchemaExtensions(schemaExtensions);

        // When
        auditServiceBuilder.build();

        // Then
        verify(factory).newAuditService(
                any(AuditServiceConfiguration.class),
                topicSchemasCaptor.capture(),
                handlersByNameCaptor.capture(),
                handlersByTopicCaptor.capture());

        JsonValue accessSchema = (JsonValue) topicSchemasCaptor.getValue().get("access");
        assertThat(accessSchema.get(pointer("schema/properties/server")).isDefined("name")).isFalse();
    }

    @Test
    public void shouldHandleNullCoreTopicSchemaExtension() throws Exception {
        // Given
        final AuditServiceFactory factory = mock(AuditServiceFactory.class);
        final AuditServiceBuilder auditServiceBuilder =
                new AuditServiceBuilder(factory).withCoreTopicSchemaExtensions(null);

        // When
        auditServiceBuilder.build();

        // Then
        verify(factory).newAuditService(
                any(AuditServiceConfiguration.class),
                topicSchemasCaptor.capture(),
                handlersByNameCaptor.capture(),
                handlersByTopicCaptor.capture());

        JsonValue accessSchema = (JsonValue) topicSchemasCaptor.getValue().get("access");
        assertThat(accessSchema.get(pointer("schema/properties")).isDefined("extraField")).isFalse();
    }

    @Test
    public void shouldHandleCoreTopicSchemaExtensionMissingPropertiesField() throws Exception {
        // Given
        final JsonValue schemaExtensions = loadJson("invalidCoreTopicSchemaExtension.json");
        schemaExtensions.remove(pointer("schema/properties"));
        final AuditServiceFactory factory = mock(AuditServiceFactory.class);
        final AuditServiceBuilder auditServiceBuilder =
                new AuditServiceBuilder(factory).withCoreTopicSchemaExtensions(schemaExtensions);

        // When
        auditServiceBuilder.build();

        // Then
        verify(factory).newAuditService(
                any(AuditServiceConfiguration.class),
                topicSchemasCaptor.capture(),
                handlersByNameCaptor.capture(),
                handlersByTopicCaptor.capture());

        JsonValue accessSchema = (JsonValue) topicSchemasCaptor.getValue().get("access");
        assertThat(accessSchema.get(pointer("schema/properties")).isDefined("extraField")).isFalse();
    }

    @Test
    public void shouldHandleCoreTopicSchemaExtensionMissingSchemaField() throws Exception {
        // Given
        final JsonValue schemaExtensions = loadJson("invalidCoreTopicSchemaExtension.json");
        schemaExtensions.remove("schema");
        final AuditServiceFactory factory = mock(AuditServiceFactory.class);
        final AuditServiceBuilder auditServiceBuilder =
                new AuditServiceBuilder(factory).withCoreTopicSchemaExtensions(schemaExtensions);

        // When
        auditServiceBuilder.build();

        // Then
        verify(factory).newAuditService(
                any(AuditServiceConfiguration.class),
                topicSchemasCaptor.capture(),
                handlersByNameCaptor.capture(),
                handlersByTopicCaptor.capture());

        JsonValue accessSchema = (JsonValue) topicSchemasCaptor.getValue().get("access");
        assertThat(accessSchema.get(pointer("schema/properties")).isDefined("extraField")).isFalse();
    }

    @Test
    public void shouldPermitAdditionalTopicsToDefined() throws Exception {
        // Given
        final JsonValue schema = loadJson("validAdditionalTopicSchema.json");
        final AuditServiceFactory factory = mock(AuditServiceFactory.class);
        final AuditServiceBuilder auditServiceBuilder =
                new AuditServiceBuilder(factory).withAdditionalTopicSchemas(schema);

        // When
        auditServiceBuilder.build();

        // Then
        verify(factory).newAuditService(
                any(AuditServiceConfiguration.class),
                topicSchemasCaptor.capture(),
                handlersByNameCaptor.capture(),
                handlersByTopicCaptor.capture());

        JsonValue customTopic = (JsonValue) topicSchemasCaptor.getValue().get("customTopic");
        assertThat(customTopic.get(pointer("schema/properties")).isDefined("_id")).isTrue();
        assertThat(customTopic.get(pointer("schema/properties")).isDefined("timestamp")).isTrue();
        assertThat(customTopic.get(pointer("schema/properties")).isDefined("transactionId")).isTrue();
        assertThat(customTopic.get(pointer("schema/properties")).isDefined("eventName")).isTrue();
        assertThat(customTopic.get(pointer("schema/properties")).isDefined("customField")).isTrue();
    }

    @Test
    public void shouldRejectAdditionalTopicWithSameNameAsCoreTopic() throws Exception {
        final JsonValue schemaExtensions = loadJson("invalidAdditionalTopicSchema.json");
        final AuditServiceFactory factory = mock(AuditServiceFactory.class);
        final AuditServiceBuilder auditServiceBuilder =
                new AuditServiceBuilder(factory).withAdditionalTopicSchemas(schemaExtensions);

        // When
        auditServiceBuilder.build();

        // Then
        verify(factory).newAuditService(
                any(AuditServiceConfiguration.class),
                topicSchemasCaptor.capture(),
                handlersByNameCaptor.capture(),
                handlersByTopicCaptor.capture());

        JsonValue accessSchema = (JsonValue) topicSchemasCaptor.getValue().get("access");
        assertThat(accessSchema.get(pointer("schema/properties")).isDefined("customField")).isFalse();
    }

    @Test
    public void shouldRejectAdditionalTopicSchemasMissingPropertiesField() throws Exception {
        // Given
        final JsonValue schema = loadJson("validAdditionalTopicSchema.json");
        schema.remove(pointer("/customTopic/schema/properties"));
        final AuditServiceFactory factory = mock(AuditServiceFactory.class);
        final AuditServiceBuilder auditServiceBuilder =
                new AuditServiceBuilder(factory).withAdditionalTopicSchemas(schema);

        // When
        auditServiceBuilder.build();

        // Then
        verify(factory).newAuditService(
                any(AuditServiceConfiguration.class),
                topicSchemasCaptor.capture(),
                handlersByNameCaptor.capture(),
                handlersByTopicCaptor.capture());

        assertThat(topicSchemasCaptor.getValue().containsKey("customTopic")).isFalse();
    }

    @Test
    public void shouldRejectAdditionalTopicSchemasMissingSchemaField() throws Exception {
        // Given
        final JsonValue schema = loadJson("validAdditionalTopicSchema.json");
        schema.remove(pointer("/customTopic/schema"));
        final AuditServiceFactory factory = mock(AuditServiceFactory.class);
        final AuditServiceBuilder auditServiceBuilder =
                new AuditServiceBuilder(factory).withAdditionalTopicSchemas(schema);

        // When
        auditServiceBuilder.build();

        // Then
        verify(factory).newAuditService(
                any(AuditServiceConfiguration.class),
                topicSchemasCaptor.capture(),
                handlersByNameCaptor.capture(),
                handlersByTopicCaptor.capture());

        assertThat(topicSchemasCaptor.getValue().containsKey("customTopic")).isFalse();
    }

    private JsonValue loadJson(String filename) throws IOException {
        return jsonFromFile("/org/forgerock/audit/AuditServiceBuilderTest/" + filename);
    }

    private JsonPointer pointer(String path) {
        return new JsonPointer(path);
    }

    // TODO: Test attempt to register handler with duplicate name
    // TODO: Test attempt to map handler to unknown topic
    // TODO: Test null values passed to methods

}
