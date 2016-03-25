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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.forgerock.audit.handlers.csv.CsvAuditEventHandlerConfiguration;
import org.forgerock.audit.handlers.elasticsearch.ElasticsearchAuditEventHandlerConfiguration;
import org.forgerock.audit.handlers.jdbc.JdbcAuditEventHandlerConfiguration;
import org.forgerock.audit.handlers.jms.JmsAuditEventHandlerConfiguration;
import org.forgerock.audit.handlers.syslog.SyslogAuditEventHandlerConfiguration;
import org.forgerock.json.JsonValue;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;

@SuppressWarnings("javadoc")
/**
 * Tests that all the properties declared in the various CAUD config files have an entry in the
 * commonsAudiTranslation.properties file.
 *
 * When a new configuration class is created it should be added to the auditEventHandlerConfigurations data provider
 * so that it can be included in these tests.
 */
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

    @DataProvider
    public Object[][] auditEventHandlerConfigurations() throws Exception{
        return new Class[][] {
                {AuditServiceConfiguration.class},
                {CsvAuditEventHandlerConfiguration.class},
                {JdbcAuditEventHandlerConfiguration.class},
                {SyslogAuditEventHandlerConfiguration.class},
                {ElasticsearchAuditEventHandlerConfiguration.class},
                {JmsAuditEventHandlerConfiguration.class}
        };
    }


    @Test(dataProvider = "auditEventHandlerConfigurations")
    public void translationsExistForAllAuditConfigurationClasses(Class clazz) throws Exception {
        assertThat(translationKeys)
                .containsAll(propertyTranslationKeysOf(jsonSchemaForPojo(clazz)))
                .as("Testing class %s has all translations mapped", clazz.getCanonicalName());
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
        return new JsonValue(mapper.readValue(mapper.writeValueAsString(jsonSchema), Map.class));
    }
}
