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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;

import org.forgerock.audit.AuditException;
import org.forgerock.audit.AuditService;
import org.forgerock.audit.AuditServiceConfiguration;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class to facilitate creation and configuration of audit service and audit event handlers
 * through JSON.
 */
public class AuditJsonConfig {

    /** Suffix for configuration class names. */
    private static final String CONFIGURATION_CLASS_SUFFIX = "Configuration";
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
        return parseAuditServiceConfiguration(json.toString());
    }

    /**
     * Creates an audit event handler from the provided name and JSON configuration.
     * <p>
     * The JSON configuration is expected to contains a "class" property which provides
     * the class name for the handler to instantiate. It is assumed that the configuration
     * class of the handler is given by adding the "Configuration" suffix to the class name
     * of the handler.
     * For example, hander class is CSVAuditEventHandler and handler configuration class is
     * CSVAuditEventHandlerConfiguration.
     *
     * @param <CFG>
     *          The type of the configuration bean for the event handler.
     * @param handlerName
     *          The name of the handler to create.
     * @param jsonConfig
     *          The configuration of the audit event handler as JSON.
     * @param classLoader
     *          The class loader to use to load the handler and its configuration class.
     * @return the fully configured audit event handler
     * @throws AuditException
     *             If any error occurs during configuration or registration of the handler.
     */
    @SuppressWarnings("unchecked") // Class.forName calls
    public static <CFG> AuditEventHandler<CFG> buildAuditEventHandler(String handlerName, JsonValue jsonConfig,
            ClassLoader classLoader) throws AuditException {
        // TODO: class name should not be provided in customer configuration
        // but through a commons module/service context
        String className = jsonConfig.get(CLASS_FIELD).asString();
        if (className == null) {
            throw new AuditException(String.format("No class is defined for the audit handler %s. "
                    + "You must define a 'class' property in the configuration.", handlerName));
        }
        AuditEventHandler<CFG> eventHandler;
        try {
            eventHandler = (AuditEventHandler<CFG>) Class.forName(className, true, classLoader).newInstance();
            JsonValue conf = jsonConfig.get(CONFIG_FIELD);
            if (conf != null) {
                String configurationClassName = className + CONFIGURATION_CLASS_SUFFIX;
                Class<CFG> klass = (Class<CFG>) Class.forName(configurationClassName, true, classLoader);
                CFG configuration = mapper.readValue(conf.toString(), klass);
                eventHandler.configure(configuration);
            }
            // else assume there is no configuration needed
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new AuditException(String.format("An error occured while trying to instantiate class "
                    + "for the handler '%s' or its configuration", handlerName), e);
        } catch (ResourceException e) {
            throw new AuditException(String.format("An error occured while trying to configure " + "the handler '%s'",
                    handlerName), e);
        } catch (IOException e) {
            throw new AuditException(String.format("An error occured while trying to generate "
                    + "the configuration class for the handler '%s'", handlerName), e);
        }
        return eventHandler;
    }

    /**
     * Returns the set of events topics to use for the event handler corresponding to provided
     * name and JSON configuration.
     * <p>
     * The JSON configuration is expected to contains an "events" field containing an array of
     * values corresponding to the event topics, e.g.
     *
     * <pre>
     *  "events" : [ "access", "activity" ]
     * </pre>
     *
     * However, it is not an error if "events" field is missing or empty.
     *
     * @param name
     *            The name of the event handler
     * @param jsonConfig
     *            The JSON configuration of the event handler.
     * @return the set of event topics the handler must process, which can be empty but
     *         never {@code null}
     */
    public static Set<String> getEvents(String name, JsonValue jsonConfig) {
        Set<String> events = jsonConfig.get(EVENTS_FIELD).asSet(String.class);
        if (events == null) {
            events = Collections.emptySet();
        }
        return events;
    }

    /**
     * Returns the name of the event handler corresponding to provided JSON configuration.
     * <p>
     * The JSON configuration is expected to contains a "name" field identifying the
     * event hander, e.g.
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
    public static String getHandlerName(JsonValue jsonConfig) throws AuditException {
        String name = jsonConfig.get(NAME_FIELD).asString();
        if (name == null) {
            throw new AuditException(String.format("No name is defined for the provided audit handler. "
                    + "You must define a 'name' property in the configuration."));
        }
        return name;
    }

    /**
     * Configures and registers the audit event handler corresponding to the provided JSON configuration
     * to the provided audit service.
     *
     * @param <CFG>
     *          The type of the configuration bean for the event handler.
     * @param jsonConfig
     *          The configuration of the audit event handler as JSON.
     * @param auditService
     *          The service the event handler will be registered to.
     * @throws AuditException
     *             If any error occurs during configuration or registration of the handler.
     */
    public static <CFG> void registerHandlerToService(JsonValue jsonConfig, AuditService auditService)
            throws AuditException {
        registerHandlerToService(jsonConfig, auditService, auditService.getClass().getClassLoader());
    }

    /**
     * Configures and registers the audit event handler corresponding to the provided JSON configuration
     * to the provided audit service, using a specific class loader.
     *
     * @param <CFG>
     *          The type of the configuration bean for the event handler.
     * @param jsonConfig
     *          The configuration of the audit event handler as JSON.
     * @param auditService
     *          The service the event handler will be registered to.
     * @param classLoader
     *          The class loader to use to load the handler and its configuration class.
     * @throws AuditException
     *             If any error occurs during configuration or registration of the handler.
     */
    public static <CFG> void registerHandlerToService(JsonValue jsonConfig, AuditService auditService,
            ClassLoader classLoader) throws AuditException {
        String name = getHandlerName(jsonConfig);
        AuditEventHandler<CFG> handler = buildAuditEventHandler(name, jsonConfig, classLoader);
        Set<String> events = getEvents(name, jsonConfig);
        auditService.register(handler, name, events);
    }

}
