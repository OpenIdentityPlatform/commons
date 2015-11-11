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

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import org.forgerock.audit.handlers.csv.CsvAuditEventHandlerConfiguration;
import org.forgerock.audit.handlers.jdbc.JdbcAuditEventHandlerConfiguration;
import org.forgerock.audit.handlers.syslog.SyslogAuditEventHandlerConfiguration;
import org.forgerock.json.JsonValue;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

@SuppressWarnings("javadoc")
public class TranslationPropertiesTest {

    //checkstyle:off
    private static final ObjectMapper mapper = new ObjectMapper();
    // checkstyle:on

    private Set<String> translationKeys;

    @SuppressWarnings("unchecked")
    @BeforeMethod
    protected void setUp() throws IOException {
        translationKeys = (Set<String>) (Set) loadTranslations().keySet();
    }

    @Test
    public void translationsExistForAllAuditServiceConfigurationProperties() throws Exception {
        assertThat(translationKeys).containsAll(
                propertyTranslationKeysOf(jsonSchemaForPojo(AuditServiceConfiguration.class)));
    }

    @Test
    public void translationsExistForAllCsvAuditEventHandlerConfigurationProperties() throws Exception {
        assertThat(translationKeys).containsAll(
                propertyTranslationKeysOf(jsonSchemaForPojo(CsvAuditEventHandlerConfiguration.class)));
    }

    @Test
    public void translationsExistForAllJdbcAuditEventHandlerConfigurationProperties() throws Exception {
        assertThat(translationKeys).containsAll(
                propertyTranslationKeysOf(jsonSchemaForPojo(JdbcAuditEventHandlerConfiguration.class)));
    }

    @Test
    public void translationsExistForAllSyslogAuditEventHandlerConfigurationProperties() throws Exception {
        assertThat(translationKeys).containsAll(
                propertyTranslationKeysOf(jsonSchemaForPojo(SyslogAuditEventHandlerConfiguration.class)));
    }

    private Properties loadTranslations() throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("commonsAuditTranslation" +
                ".properties")) {
            properties.load(inputStream);
        }
        return properties;
    }

    private Set<String> propertyTranslationKeysOf(final JsonValue schemaObject) throws IOException {
        final Set<String> keys = new HashSet<>();
        for (String propertyName : schemaObject.get("properties").keys()) {
            JsonValue property = schemaObject.get("properties").get(propertyName);
            keys.add(property.get("description").asString());
            if ("object".equals(property.get("type").asString())) {
                keys.addAll(propertyTranslationKeysOf(property));
            }
        }
        return keys;
    }

    private JsonValue jsonSchemaForPojo(final Class<?> pojoClass) throws IOException {
        SchemaFactoryWrapper visitor = new SchemaFactoryWrapper();
        mapper.acceptJsonFormatVisitor(mapper.constructType(pojoClass), visitor);
        JsonSchema jsonSchema = visitor.finalSchema();
        jsonSchema.setId("/");
        return new JsonValue(mapper.readValue(mapper.writeValueAsString(jsonSchema), Map.class));
    }
}
