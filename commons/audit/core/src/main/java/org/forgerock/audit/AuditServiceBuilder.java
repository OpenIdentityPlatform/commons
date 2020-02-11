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

import static org.forgerock.audit.events.EventTopicsMetaDataBuilder.coreTopicSchemas;

import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.events.handlers.AuditEventHandlerFactory;
import org.forgerock.audit.events.handlers.DependencyProviderAuditEventHandlerFactory;
import org.forgerock.audit.events.handlers.EventHandlerConfiguration;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.util.Reject;
import org.forgerock.util.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Builder for AuditService.
 */
public final class AuditServiceBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AuditServiceBuilder.class);

    private final AuditServiceFactory auditServiceFactory;
    private AuditServiceConfiguration auditServiceConfiguration = new AuditServiceConfiguration();
    private AuditEventHandlerFactory auditEventHandlerFactory =
            new DependencyProviderAuditEventHandlerFactory(new DependencyProviderBase());
    private Map<String, HandlerRegistration> handlerRegistrations = new LinkedHashMap<>();
    private Set<AuditEventHandler> prebuiltHandlers = new LinkedHashSet<>();
    private EventTopicsMetaData eventTopicsMetaData = coreTopicSchemas().build();

    @VisibleForTesting
    AuditServiceBuilder(AuditServiceFactory auditServiceFactory) {
        this.auditServiceFactory = auditServiceFactory;
    }

    /**
     * Factory method for new instances of this builder.
     *
     * @return A new instance of the AuditServiceBuilder.
     */
    public static AuditServiceBuilder newAuditService() {
        return new AuditServiceBuilder(new AuditServiceFactory());
    }

    /**
     * Sets the AuditServiceConfiguration that is to be passed to the AuditService.
     * <p/>
     * AuditServiceConfiguration embodies the configuration state that can be set by system administrators.
     *
     * @param auditServiceConfiguration
     *          user-facing configuration that is to be applied to the AuditService.
     * @return this builder for method-chaining.
     */
    public AuditServiceBuilder withConfiguration(AuditServiceConfiguration auditServiceConfiguration) {
        Reject.ifNull(auditServiceConfiguration, "Audit service configuration cannot be null");
        this.auditServiceConfiguration = auditServiceConfiguration;
        return this;
    }

    /**
     * Set the topic metadata that should be used by the audit service and the handlers.
     * @param eventTopicsMetaData The metadata.
     * @return This builder.
     */
    public AuditServiceBuilder withEventTopicsMetaData(EventTopicsMetaData eventTopicsMetaData) {
        Reject.ifNull(eventTopicsMetaData, "Audit service event topic meta-data cannot be null");
        this.eventTopicsMetaData = eventTopicsMetaData;
        return this;
    }

    /**
     * Register the DependencyProvider, after which, an AuditEventHandler can be registered and
     * receive this provider.  The dependency provider allows the handler to obtain resources or
     * objects from the product which integrates the Audit Service.
     *
     * @param dependencyProvider
     *            the DependencyProvider to register.
     * @return this builder for method-chaining.
     */
    public AuditServiceBuilder withDependencyProvider(DependencyProvider dependencyProvider) {
        Reject.ifNull(dependencyProvider, "Audit event handler DependencyProvider cannot be null");
        this.auditEventHandlerFactory = new DependencyProviderAuditEventHandlerFactory(dependencyProvider);
        return this;
    }

    /**
     * Register factory for creating instances of {@link AuditEventHandler}.
     *
     * @param auditEventHandlerFactory
     *            the AuditEventHandlerFactory to register.
     * @return this builder for method-chaining.
     */
    public AuditServiceBuilder withAuditEventHandlerFactory(AuditEventHandlerFactory auditEventHandlerFactory) {
        Reject.ifNull(auditEventHandlerFactory, "AuditEventHandlerFactory cannot be null");
        this.auditEventHandlerFactory = auditEventHandlerFactory;
        return this;
    }

    /**
     * Register an AuditEventHandler. After that registration, that AuditEventHandler can be referred with the given
     * name. This AuditEventHandler will only be notified about the events specified in the parameter events.
     *
     * @param clazz
     *            the AuditEventHandler type to register.
     * @param configuration
     *            the handler configuration.
     * @throws AuditException
     *             if already asked to register a handler with the same name.
     * @return this builder for method-chaining.
     */
    public AuditServiceBuilder withAuditEventHandler(
            Class<? extends AuditEventHandler> clazz, EventHandlerConfiguration configuration) throws AuditException {

        Reject.ifNull(clazz, "Audit event handler clazz cannot be null");
        Reject.ifNull(configuration, "Audit event handler configuration cannot be null");
        Reject.ifNull(configuration.getName(), "Audit event handler name cannot be null");

        rejectIfHandlerNameAlreadyTaken(configuration.getName());
        handlerRegistrations.put(configuration.getName(), new HandlerRegistration<>(clazz, configuration));
        return this;
    }

    /**
     * Register an AuditEventHandler.
     *
     * @param auditEventHandler
     *            the AuditEventHandler to register.
     * @throws AuditException
     *             if already asked to register a handler with the same name.
     * @return this builder for method-chaining.
     */
    public AuditServiceBuilder withAuditEventHandler(AuditEventHandler auditEventHandler) throws AuditException {
        Reject.ifNull(auditEventHandler, "Audit event handler cannot be null");
        rejectIfHandlerNameAlreadyTaken(auditEventHandler.getName());
        prebuiltHandlers.add(auditEventHandler);
        return this;
    }

    private void rejectIfHandlerNameAlreadyTaken(String name) throws AuditException {
        if (handlerRegistrations.containsKey(name)) {
            throw new AuditException("There is already a handler registered for " + name);
        }
        for (AuditEventHandler handler : prebuiltHandlers) {
            if (handler.getName() != null && handler.getName().equals(name)) {
                throw new AuditException("There is already a handler registered for " + name);
            }
        }
    }

    /**
     * Creates a new AuditService instance.
     * <p/>
     * Instances receive their configuration when constructed and cannot be reconfigured. Where "hot-swappable"
     * reconfiguration is required, an instance of {@link AuditServiceProxy} should be used as a proxy. The old
     * AuditService should fully shutdown before the new instance is started. Care must be taken to ensure that
     * no other threads can interact with this object while {@link AuditService#startup()} and
     * {@link AuditService#shutdown()} methods are running.
     * <p/>
     * After construction, the AuditService will be in the 'STARTING' state until {@link AuditService#startup()}
     * is called. When in the 'STARTING' state, a call to any method other than {@link AuditService#startup()}
     * will lead to {@link ServiceUnavailableException}.
     * <p/>
     * After {@link AuditService#startup()} is called, assuming startup succeeds, the AuditService will then be in
     * the 'RUNNING' state and further calls to {@link AuditService#startup()} will be ignored.
     * <p/>
     * Calling {@link AuditService#shutdown()} will put the AuditService into the 'SHUTDOWN' state; once shutdown, the
     * AuditService will remain in this state and cannot be restarted. Further calls to {@link AuditService#shutdown()}
     * will be ignored. When in the 'SHUTDOWN' state, a call to any method other than {@link AuditService#shutdown()}
     * will lead to {@link ServiceUnavailableException}.
     * <p/>
     * When instances are no longer needed, {@link AuditService#shutdown()} should be called to ensure that any
     * buffered audit events are flushed and that all open file handles or connections are closed.
     *
     * @return a new AuditService instance.
     */
    public AuditService build() {
        Set<AuditEventHandler> handlers = buildAuditEventHandlers(eventTopicsMetaData);
        return auditServiceFactory.newAuditService(auditServiceConfiguration, eventTopicsMetaData, handlers);
    }


    private Set<AuditEventHandler> buildAuditEventHandlers(final EventTopicsMetaData eventTopicsMetaData) {
        Set<AuditEventHandler> handlers = new LinkedHashSet<>(prebuiltHandlers);
        for (HandlerRegistration handlerRegistration : handlerRegistrations.values()) {
            logger.debug("Registering handler '{}' for {} topics",
                    handlerRegistration.configuration.getName(),
                    handlerRegistration.configuration.getTopics().toString());
            try {
                // Only build the handler if it is enabled.
                if (handlerRegistration.configuration.isEnabled()) {
                    handlers.add(auditEventHandlerFactory.create(
                            handlerRegistration.configuration.getName(),
                            handlerRegistration.clazz,
                            handlerRegistration.configuration,
                            eventTopicsMetaData));
                }
            } catch (AuditException e) {
                logger.error(e.getMessage(), e);
            }
        }
        logger.debug("Registered {}", handlers.toString());
        return handlers;
    }

    /**
     * Captures details of a handler registration request.
     * <p/>
     * Calls to {@link AuditServiceBuilder#withAuditEventHandler} are lazily-processed when
     * {@link AuditServiceBuilder#build()} is called so that all event topic schema meta-data
     * is available for validation of the mapping from topics to handlers without constraining
     * the order in which the builder's methods should be called.
     */
    private static final class HandlerRegistration<C extends EventHandlerConfiguration> {

        final Class<? extends AuditEventHandler> clazz;
        final C configuration;

        private HandlerRegistration(Class<? extends AuditEventHandler> clazz, C configuration) {
            this.clazz = clazz;
            this.configuration = configuration;
        }
    }

    /**
     * This class exists solely to provide a 'seam' that can be mocked during unit testing.
     */
    @VisibleForTesting
    static class AuditServiceFactory {

        AuditService newAuditService(
                final AuditServiceConfiguration configuration,
                final EventTopicsMetaData eventTopicsMetaData,
                final Set<AuditEventHandler> auditEventHandlers) {
            return new AuditServiceImpl(configuration, eventTopicsMetaData, auditEventHandlers);
        }
    }
}
