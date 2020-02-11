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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.audit.AuditServiceBuilder.newAuditService;
import static org.forgerock.audit.JsonValueUtils.jsonFromFile;
import static org.forgerock.audit.events.EventTopicsMetaDataBuilder.coreTopicSchemas;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.forgerock.audit.AuditServiceBuilder.AuditServiceFactory;
import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.events.handlers.AuditEventHandlerFactory;
import org.forgerock.audit.events.handlers.impl.PassThroughAuditEventHandler;
import org.forgerock.audit.events.handlers.impl.PassThroughAuditEventHandlerConfiguration;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings({"javadoc", "rawtypes", "unchecked" })
public class AuditServiceBuilderTest {

    private ArgumentCaptor<EventTopicsMetaData> topicSchemasCaptor;

    @BeforeMethod
    private void setUp() {
        topicSchemasCaptor = ArgumentCaptor.forClass(EventTopicsMetaData.class);
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
    public void shouldProvideAuditEventHandlerConstructorParameters() throws Exception {
        // Given
        AuditEventHandlerFactory factory = mock(AuditEventHandlerFactory.class);
        Class<PassThroughAuditEventHandler> clazz = PassThroughAuditEventHandler.class;
        PassThroughAuditEventHandlerConfiguration config = new PassThroughAuditEventHandlerConfiguration();
        config.setName("mock");
        config.setTopics(Collections.singleton("access"));
        PassThroughAuditEventHandler handler = mock(PassThroughAuditEventHandler.class);
        ArgumentCaptor<EventTopicsMetaData> eventTopicsMetaData = ArgumentCaptor.forClass(EventTopicsMetaData.class);
        given(factory.create(eq("mock"), eq(clazz), eq(config), eventTopicsMetaData.capture())).willReturn(handler);
        given(handler.getName()).willReturn("mock");

        // When
        newAuditService()
                .withAuditEventHandler(clazz, config)
                .withAuditEventHandlerFactory(factory)
                .build();

        // Then
        assertThat(eventTopicsMetaData.getValue().getTopics()).contains("access");
        JsonValue accessMetaData = eventTopicsMetaData.getValue().getSchema("access");
        assertThat(accessMetaData.isDefined("schema")).isTrue();
    }

    @Test
    public void shouldPermitAdditionalFieldsToBeAddedToCoreTopicSchemas() throws Exception {
        // Given
        final JsonValue schemaExtensions = loadJson("validCoreTopicSchemaExtension.json");
        final AuditServiceFactory factory = mock(AuditServiceFactory.class);
        final AuditServiceBuilder auditServiceBuilder = new AuditServiceBuilder(factory)
                .withEventTopicsMetaData(coreTopicSchemas().withCoreTopicSchemaExtensions(schemaExtensions).build());

        // When
        auditServiceBuilder.build();

        // Then
        verify(factory).newAuditService(
                any(AuditServiceConfiguration.class),
                topicSchemasCaptor.capture(),
                any(Set.class));

        JsonValue accessSchema = (JsonValue) topicSchemasCaptor.getValue().getSchema("access");
        assertThat(accessSchema.get(pointer("schema/properties")).isDefined("extraField")).isTrue();
    }

    @Test
    public void shouldRejectCoreTopicSchemasExtensionForUnknownTopic() throws Exception {
        // Given
        final JsonValue schemaExtensions = loadJson("validAdditionalTopicSchema.json");
        final AuditServiceFactory factory = mock(AuditServiceFactory.class);
        final AuditServiceBuilder auditServiceBuilder = new AuditServiceBuilder(factory)
                .withEventTopicsMetaData(coreTopicSchemas().withCoreTopicSchemaExtensions(schemaExtensions).build());

        // When
        auditServiceBuilder.build();

        // Then
        verify(factory).newAuditService(
                any(AuditServiceConfiguration.class),
                topicSchemasCaptor.capture(),
                any(Set.class));

        assertThat(topicSchemasCaptor.getValue().getTopics()).doesNotContain("customTopic");
    }

    @Test
    public void shouldPreventCoreTopicSchemaExistingFieldsFromBeingAltered() throws Exception {
        final JsonValue schemaExtensions = loadJson("invalidCoreTopicSchemaExtension.json");
        final AuditServiceFactory factory = mock(AuditServiceFactory.class);
        final AuditServiceBuilder auditServiceBuilder = new AuditServiceBuilder(factory)
                .withEventTopicsMetaData(coreTopicSchemas().withCoreTopicSchemaExtensions(schemaExtensions).build());

        // When
        auditServiceBuilder.build();

        // Then
        verify(factory).newAuditService(
                any(AuditServiceConfiguration.class),
                topicSchemasCaptor.capture(),
                any(Set.class));

        JsonValue accessSchema = topicSchemasCaptor.getValue().getSchema("access");
        assertThat(accessSchema.get(pointer("schema/properties/server")).isDefined("name")).isFalse();
    }

    @Test
    public void shouldHandleNullCoreTopicSchemaExtension() throws Exception {
        // Given
        final AuditServiceFactory factory = mock(AuditServiceFactory.class);
        final AuditServiceBuilder auditServiceBuilder = new AuditServiceBuilder(factory);

        // When
        auditServiceBuilder.build();

        // Then
        verify(factory).newAuditService(
                any(AuditServiceConfiguration.class),
                topicSchemasCaptor.capture(),
                any(Set.class));

        JsonValue accessSchema = topicSchemasCaptor.getValue().getSchema("access");
        assertThat(accessSchema.get(pointer("schema/properties")).isDefined("extraField")).isFalse();
    }

    @Test
    public void shouldHandleCoreTopicSchemaExtensionMissingPropertiesField() throws Exception {
        // Given
        final JsonValue schemaExtensions = loadJson("invalidCoreTopicSchemaExtension.json");
        schemaExtensions.remove(pointer("schema/properties"));
        final AuditServiceFactory factory = mock(AuditServiceFactory.class);
        final AuditServiceBuilder auditServiceBuilder = new AuditServiceBuilder(factory)
                .withEventTopicsMetaData(coreTopicSchemas().withCoreTopicSchemaExtensions(schemaExtensions).build());

        // When
        auditServiceBuilder.build();

        // Then
        verify(factory).newAuditService(
                any(AuditServiceConfiguration.class),
                topicSchemasCaptor.capture(),
                any(Set.class));

        JsonValue accessSchema = topicSchemasCaptor.getValue().getSchema("access");
        assertThat(accessSchema.get(pointer("schema/properties")).isDefined("extraField")).isFalse();
    }

    @Test
    public void shouldHandleCoreTopicSchemaExtensionMissingSchemaField() throws Exception {
        // Given
        final JsonValue schemaExtensions = loadJson("invalidCoreTopicSchemaExtension.json");
        schemaExtensions.remove("schema");
        final AuditServiceFactory factory = mock(AuditServiceFactory.class);
        final AuditServiceBuilder auditServiceBuilder = new AuditServiceBuilder(factory)
                .withEventTopicsMetaData(coreTopicSchemas().withCoreTopicSchemaExtensions(schemaExtensions).build());

        // When
        auditServiceBuilder.build();

        // Then
        verify(factory).newAuditService(
                any(AuditServiceConfiguration.class),
                topicSchemasCaptor.capture(),
                any(Set.class));

        JsonValue accessSchema = topicSchemasCaptor.getValue().getSchema("access");
        assertThat(accessSchema.get(pointer("schema/properties")).isDefined("extraField")).isFalse();
    }

    @Test
    public void shouldPermitAdditionalTopicsToBeDefined() throws Exception {
        // Given
        final JsonValue additionalSchemas = loadJson("validAdditionalTopicSchema.json");
        final AuditServiceFactory factory = mock(AuditServiceFactory.class);
        final AuditServiceBuilder auditServiceBuilder = new AuditServiceBuilder(factory)
                .withEventTopicsMetaData(coreTopicSchemas().withAdditionalTopicSchemas(additionalSchemas).build());

        // When
        auditServiceBuilder.build();

        // Then
        verify(factory).newAuditService(
                any(AuditServiceConfiguration.class),
                topicSchemasCaptor.capture(),
                any(Set.class));

        JsonValue customTopic = topicSchemasCaptor.getValue().getSchema("customTopic");
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
        final AuditServiceBuilder auditServiceBuilder = new AuditServiceBuilder(factory)
                .withEventTopicsMetaData(coreTopicSchemas().withAdditionalTopicSchemas(schemaExtensions).build());

        // When
        auditServiceBuilder.build();

        // Then
        verify(factory).newAuditService(
                any(AuditServiceConfiguration.class),
                topicSchemasCaptor.capture(),
                any(Set.class));

        JsonValue accessSchema = topicSchemasCaptor.getValue().getSchema("access");
        assertThat(accessSchema.get(pointer("schema/properties")).isDefined("customField")).isFalse();
    }

    @Test
    public void shouldRejectAdditionalTopicSchemasMissingPropertiesField() throws Exception {
        // Given
        final JsonValue additionalSchemas = loadJson("validAdditionalTopicSchema.json");
        additionalSchemas.remove(pointer("/customTopic/schema/properties"));
        final AuditServiceFactory factory = mock(AuditServiceFactory.class);
        final AuditServiceBuilder auditServiceBuilder = new AuditServiceBuilder(factory)
                .withEventTopicsMetaData(coreTopicSchemas().withAdditionalTopicSchemas(additionalSchemas).build());

        // When
        auditServiceBuilder.build();

        // Then
        verify(factory).newAuditService(
                any(AuditServiceConfiguration.class),
                topicSchemasCaptor.capture(),
                any(Set.class));

        assertThat(topicSchemasCaptor.getValue().getTopics()).doesNotContain("customTopic");
    }

    @Test
    public void shouldRejectAdditionalTopicSchemasMissingSchemaField() throws Exception {
        // Given
        final JsonValue additionalSchemas = loadJson("validAdditionalTopicSchema.json");
        additionalSchemas.remove(pointer("/customTopic/schema"));
        final AuditServiceFactory factory = mock(AuditServiceFactory.class);
        final AuditServiceBuilder auditServiceBuilder = new AuditServiceBuilder(factory)
                .withEventTopicsMetaData(coreTopicSchemas().withAdditionalTopicSchemas(additionalSchemas).build());

        // When
        auditServiceBuilder.build();

        // Then
        verify(factory).newAuditService(
                any(AuditServiceConfiguration.class),
                topicSchemasCaptor.capture(),
                any(Set.class));

        assertThat(topicSchemasCaptor.getValue().getTopics()).doesNotContain("customTopic");
    }

    /**
     * Tests that only enabled handlers are instantiated when the audit service is built.
     *
     * @throws Exception
     */
    @Test
    public void shouldNotInstantiateDisabledHandlers() throws Exception {
        // Given
        final AuditServiceFactory factory = new AuditServiceFactory();
        EventTopicsMetaData eventTopicsMetaData = coreTopicSchemas().build();
        Set<String> topics = new HashSet<>();
        topics.add("authentication");

        PassThroughAuditEventHandlerConfiguration disabledConfig = new PassThroughAuditEventHandlerConfiguration();
        disabledConfig.setName("disabledHandler");
        disabledConfig.setEnabled(false);
        disabledConfig.setTopics(topics);

        PassThroughAuditEventHandlerConfiguration enabledConfig = new PassThroughAuditEventHandlerConfiguration();
        enabledConfig.setName("enabledHandler");
        enabledConfig.setEnabled(true);
        enabledConfig.setTopics(topics);

        // When
        AuditService auditService = new AuditServiceBuilder(factory)
                .withEventTopicsMetaData(eventTopicsMetaData)
                .withAuditEventHandler(PassThroughAuditEventHandler.class, enabledConfig)
                .withAuditEventHandler(PassThroughAuditEventHandler.class, disabledConfig)
                .build();

        // Then
        Collection<AuditEventHandler> registeredHandlers = auditService.getRegisteredHandlers();
        assertThat(registeredHandlers.size()).isEqualTo(1);
        for (AuditEventHandler registeredHandler : registeredHandlers) {
            assertThat(registeredHandler.isEnabled()).isTrue();
        }
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
