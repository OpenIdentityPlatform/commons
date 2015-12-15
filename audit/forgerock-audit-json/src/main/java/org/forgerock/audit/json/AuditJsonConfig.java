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
package org.forgerock.audit.json;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.forgerock.audit.AuditException;
import org.forgerock.audit.AuditServiceBuilder;
import org.forgerock.audit.AuditServiceConfiguration;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.events.handlers.EventHandlerConfiguration;
import org.forgerock.audit.util.JsonValueUtils;
import org.forgerock.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;

/**
 * Utility class to facilitate creation and configuration of audit service and audit event handlers
 * through JSON.
 */
public class AuditJsonConfig {
    private static final Logger logger = LoggerFactory.getLogger(AuditJsonConfig.class);

    /** Field containing the name of an event handler. */
    private static final String NAME_FIELD = "name";
    /** Field containing the implementation class of an event handler. */
    private static final String CLASS_FIELD = "class";
    /** Field containing the configuration of an event handler. */
    private static final String CONFIG_FIELD = "config";
    /** Field containing events topics to process for an event handler. */
    private static final String EVENTS_FIELD = "events";

    /** The mapper from JSON structure to Java object. */
    //checkstyle:off
    private static final ObjectMapper mapper = new ObjectMapper();
    // checkstyle:on

    private static final AnnotationIntrospector defaultAnnotationIntrospector = new JacksonAnnotationIntrospector();
    private static final AnnotationIntrospector helpAppenderAnnotationIntrospector =
            new HelpAppenderAnnotationIntrospector();

    private AuditJsonConfig() {
        // prevent instantiation of the class
    }

    /**
     * Returns a JSON value from the provided input stream.
     *
     * @param input
     *          Input stream containing an arbitrary JSON structure.
     * @return the JSON value corresponding to the JSON structure
     * @throws AuditException
     *          If an error occurs.
     */
    public static JsonValue getJson(InputStream input) throws AuditException {
        try {
            return new JsonValue(mapper.readValue(input, LinkedHashMap.class));
        } catch (IOException e) {
            throw new AuditException(String.format("Unable to retrieve json value from json input stream"), e);
        }
    }

    /**
     * Returns the audit service configuration from the provided input stream.
     *
     * @param input
     *          Input stream containing JSON configuration of the audit service.
     * @return the configuration object
     * @throws AuditException
     *          If any error occurs.
     */
    public static AuditServiceConfiguration parseAuditServiceConfiguration(InputStream input) throws AuditException {
        try {
            return mapper.readValue(input, AuditServiceConfiguration.class);
        } catch (IOException e) {
            throw new AuditException(String.format("Unable to retrieve class %s from json input stream",
                    AuditServiceConfiguration.class), e);
        }
    }

    /**
     * Returns the audit service configuration from the provided JSON string.
     *
     * @param json
     *          JSON string representing the configuration of the audit service.
     * @return the configuration object
     * @throws AuditException
     *          If any error occurs.
     */
    public static AuditServiceConfiguration parseAuditServiceConfiguration(String json) throws AuditException {
        if (json == null) {
            return new AuditServiceConfiguration();
        }
        try {
            return mapper.readValue(json, AuditServiceConfiguration.class);
        } catch (IOException e) {
            throw new AuditException(String.format("Unable to retrieve class %s from json: %s",
                    AuditServiceConfiguration.class, json), e);
        }
    }

    /**
     * Returns the audit service configuration from the provided JSON value.
     *
     * @param json
     *          JSON value representing the configuration of the audit service.
     * @return the configuration object
     * @throws AuditException
     *          If any error occurs.
     */
    public static AuditServiceConfiguration parseAuditServiceConfiguration(JsonValue json) throws AuditException {
        return parseAuditServiceConfiguration(JsonValueUtils.extractValueAsString(json, "/"));
    }

    /**
     * Configures and registers the audit event handler corresponding to the provided JSON configuration
     * to the provided audit service.
     *
     * @param jsonConfig
     *          The configuration of the audit event handler as JSON.
     * @param auditServiceBuilder
     *          The builder for the service the event handler will be registered to.
     * @throws AuditException
     *             If any error occurs during configuration or registration of the handler.
     */
    public static void registerHandlerToService(JsonValue jsonConfig, AuditServiceBuilder auditServiceBuilder)
            throws AuditException {
        registerHandlerToService(jsonConfig, auditServiceBuilder, auditServiceBuilder.getClass().getClassLoader());
    }

    /**
     * Configures and registers the audit event handler corresponding to the provided JSON configuration
     * to the provided audit service, using a specific class loader.
     *
     * @param jsonConfig
     *          The configuration of the audit event handler as JSON.
     * @param auditServiceBuilder
     *          The builder for the service the event handler will be registered to.
     * @param classLoader
     *          The class loader to use to load the handler and its configuration class.
     * @throws AuditException
     *             If any error occurs during configuration or registration of the handler.
     */
    public static void registerHandlerToService(JsonValue jsonConfig,
            AuditServiceBuilder auditServiceBuilder, ClassLoader classLoader) throws AuditException {
        String name = getHandlerName(jsonConfig);
        Class<? extends AuditEventHandler> handlerClass = getAuditEventHandlerClass(name, jsonConfig, classLoader);
        Class<? extends EventHandlerConfiguration> configClass =
                getAuditEventHandlerConfigurationClass(name, handlerClass, classLoader);
        EventHandlerConfiguration configuration = parseAuditEventHandlerConfiguration(configClass, jsonConfig);
        auditServiceBuilder.withAuditEventHandler(handlerClass, configuration);
    }

    /**
     * Returns the name of the event handler corresponding to provided JSON configuration.
     * <p>
     * The JSON configuration is expected to contains a "name" field identifying the
     * event handler, e.g.
     * <pre>
     *  "name" : "passthrough"
     * </pre>
     *
     * @param jsonConfig
     *          The JSON configuration of the event handler.
     * @return the name of the event handler
     * @throws AuditException
     *          If an error occurs.
     */
    private static String getHandlerName(JsonValue jsonConfig) throws AuditException {
        String name = jsonConfig.get(CONFIG_FIELD).get(NAME_FIELD).asString();
        if (name == null) {
            throw new AuditException(String.format("No name is defined for the provided audit handler. "
                    + "You must define a 'name' property in the configuration."));
        }
        return name;
    }

    /**
     * Creates an audit event handler factory from the provided JSON configuration.
     * <p>
     * The JSON configuration is expected to contains a "class" property which provides
     * the class name for the handler factory to instantiate.
     *
     * @param jsonConfig
     *          The configuration of the audit event handler as JSON.
     * @param classLoader
     *          The class loader to use to load the handler and its configuration class.
     * @return the fully configured audit event handler
     * @throws AuditException
     *             If any error occurs during configuration or registration of the handler.
     */
    @SuppressWarnings("unchecked") // Class.forName calls
    private static Class<? extends AuditEventHandler> getAuditEventHandlerClass(
            String handlerName, JsonValue jsonConfig, ClassLoader classLoader) throws AuditException {
        // TODO: class name should not be provided in customer configuration
        // but through a commons module/service context
        String className = jsonConfig.get(CLASS_FIELD).asString();
        if (className == null) {
            String errorMessage = String.format("No class is defined for the audit handler %s. "
                    + "You must define a 'class' property in the configuration.", handlerName);
            throw new AuditException(errorMessage);
        }
        try {
            return (Class<? extends AuditEventHandler>) Class.forName(className, true, classLoader);
        } catch (ClassNotFoundException e) {
            String errorMessage = String.format("Invalid class is defined for the audit handler %s.", handlerName);
            throw new AuditException(errorMessage, e);
        }
    }

    @SuppressWarnings("unchecked") // Class.forName calls
    private static Class<? extends EventHandlerConfiguration> getAuditEventHandlerConfigurationClass(
            String handlerName, Class<? extends AuditEventHandler> handlerClass, ClassLoader classLoader)
            throws AuditException {
        String className = handlerClass.getName() + "Configuration";
        try {
            return (Class<? extends EventHandlerConfiguration>) Class.forName(className, true, classLoader);
        } catch (ClassNotFoundException e) {
            String errorMessage = String.format("Unable to locate configuration class %s for the audit handler %s.",
                    className, handlerName);
            throw new AuditException(errorMessage, e);
        }
    }

    /**
     * Returns the audit event handler configuration from the provided JSON string.
     *
     * @param <C>
     *          The type of the configuration bean for the event handler.
     * @param jsonConfig
     *          The configuration of the audit event handler as JSON.
     * @return the fully configured audit event handler
     * @throws AuditException
     *             If any error occurs while instantiating the configuration from JSON.
     */
    private static <C extends EventHandlerConfiguration> C parseAuditEventHandlerConfiguration(
            Class<C> clazz, JsonValue jsonConfig) throws AuditException {
        C configuration = null;
        JsonValue conf = jsonConfig.get(CONFIG_FIELD);
        if (conf != null) {
            configuration = mapper.convertValue(conf.getObject(), clazz);
        }
        return configuration;
    }

    /**
     * Gets the configuration schema for an audit event handler as json schema. The supplied json config must contain
     * a field called class with the value of the audit event handler implementation class.
     * @param className The class name to get the configuration for.
     * @param classLoader The {@link ClassLoader} to use to load the event handler and event handler config class.
     * @return The config schema as json schema.
     * @throws AuditException If any error occurs parsing the config class for schema.
     */
    public static JsonValue getAuditEventHandlerConfigurationSchema(final String className,
            final ClassLoader classLoader) throws AuditException {
        final Class<? extends EventHandlerConfiguration> eventHandlerConfiguration =
                getAuditEventHandlerConfigurationClass(
                        className,
                        getAuditEventHandlerClass(
                                className,
                                json(object(field("class", className))),
                                classLoader),
                        classLoader);
        try {
            mapper.setAnnotationIntrospector(helpAppenderAnnotationIntrospector);
            SchemaFactoryWrapper visitor = new SchemaFactoryWrapper();
            mapper.acceptJsonFormatVisitor(mapper.constructType(eventHandlerConfiguration), visitor);
            JsonSchema jsonSchema = visitor.finalSchema();
            final JsonValue schema = json(mapper.readValue(mapper.writeValueAsString(jsonSchema), Map.class));
            mapper.setAnnotationIntrospector(defaultAnnotationIntrospector);
            return schema;
        } catch (IOException e) {
            final String error = String.format("Unable to parse configuration class schema for configuration class %s",
                    eventHandlerConfiguration.getName());
            logger.error(error, e);
            throw new AuditException(error, e);
        }
    }

    /**
     * Extends the default {@link JacksonAnnotationIntrospector} and overrides the {@link JsonPropertyDescription}
     * annotation inorder to append ".help" to the description.
     */
    private static class HelpAppenderAnnotationIntrospector extends JacksonAnnotationIntrospector {
        @Override
        public String findPropertyDescription(Annotated ann) {
            JsonPropertyDescription desc = _findAnnotation(ann, JsonPropertyDescription.class);
            return (desc == null) ? null : desc.value().concat(".help");
        }
    }

}
