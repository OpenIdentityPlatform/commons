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

package org.forgerock.caf.authn.test.runtime;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import org.forgerock.caf.authn.test.configuration.ConfigurationResource;
import org.forgerock.jaspi.context.FallbackServerAuthContext;
import org.forgerock.jaspi.runtime.AuditApi;
import org.forgerock.jaspi.runtime.JaspiRuntime;
import org.forgerock.jaspi.runtime.RuntimeResultHandler;
import org.forgerock.jaspi.runtime.context.ContextHandler;
import org.forgerock.jaspi.utils.MessageInfoUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.RootContext;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.auth.message.module.ServerAuthModule;
import java.util.ArrayList;
import java.util.List;

import static org.forgerock.json.resource.Requests.newReadRequest;
import static org.forgerock.json.resource.Resources.newInternalConnection;
import static org.forgerock.json.resource.Resources.newSingleton;

/**
 * Guice module for wiring the JASPI runtime.
 *
 * @since 1.5.0
 */
@org.forgerock.guice.core.GuiceModule
public class GuiceModule extends AbstractModule {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configure() {
        bind(MessageInfoUtils.class).in(Singleton.class);
        bind(RuntimeResultHandler.class).in(Singleton.class);
        bind(ServerAuthContext.class).to(ConfigurableServerAuthContext.class).in(Singleton.class);
        bind(AuditApi.class).to(TestAuditApi.class);
    }

    /**
     * Provider for the {@code ContextHandler} instance.
     *
     * @param messageInfoUtils An instance of the {@code MessageInfoUtils}.
     * @return The {@code ContextHandler} instance.
     */
    @Provides
    @Inject
    @Singleton
    public ContextHandler getContextHandler(MessageInfoUtils messageInfoUtils) {
        return new ContextHandler(messageInfoUtils);
    }

    /**
     * Provider for the JASPI runtime instance.
     *
     * @param serverAuthContext The {@code ServerAuthContext} instance.
     * @param resultHandler An instance of the {@code RuntimeResultHandler}.
     * @param auditApi An instance of the {@code AuditApi}.
     * @return The JASPI runtime instance.
     */
    @Provides
    @Inject
    @Singleton
    public JaspiRuntime getJaspiRuntime(ServerAuthContext serverAuthContext,
            RuntimeResultHandler resultHandler, AuditApi auditApi) {
        return new JaspiRuntime(serverAuthContext, resultHandler, auditApi);
    }

    /**
     * Provider for the JASPI runtime's {@code ServerAuthContext}.
     *
     * @param messageInfoUtils An instance of the {@code MessageInfoUtils}.
     * @param contextHandler An instance of the {@code ContextHandler}.
     * @param sessionAuthModule The configured "Session" auth module.
     * @param authModules The configured auth modules.
     * @return A {@code ServerAuthContext} instance.
     * @throws AuthException If any of the configured {@code ServerAuthModule} instance do not support the
     * request/response message type.
     */
    @Provides
    @Named("ServerAuthContext")
    @Inject
    public ServerAuthContext getServerAuthContext(MessageInfoUtils messageInfoUtils, ContextHandler contextHandler,
            @Named("SessionAuthModule") ServerAuthModule sessionAuthModule,
            @Named("AuthModules") List<ServerAuthModule> authModules) throws AuthException {
        return new FallbackServerAuthContext(messageInfoUtils, contextHandler, sessionAuthModule, authModules);
    }

    /**
     * Provider for the JASPI runtime's "Session" auth module.
     *
     * @param configurationResource An instance of the {@code ConfigurationResource}.
     * @param injector The Guice Injector instance.
     * @return The "Session" auth module instance, or {@code null} if no "Session" auth module configured.
     * @throws ResourceException If there is a problem reading the runtime's module configuration.
     * @throws ClassNotFoundException If the configured {@code ServerAuthContext} class could not be found.
     */
    @Provides
    @Named("SessionAuthModule")
    @Inject
    public ServerAuthModule getSessionAuthModule(ConfigurationResource configurationResource, Injector injector)
            throws ResourceException, ClassNotFoundException {

        Resource configuration = newInternalConnection(newSingleton(configurationResource))
                .read(new RootContext(), newReadRequest("configuration"));
        JsonValue sessionModuleConfig = configuration.getContent().get("serverAuthContext").get("sessionModule");
        if (!sessionModuleConfig.isNull()) {
            return (ServerAuthModule) injector.getInstance(
                    Class.forName(sessionModuleConfig.get("className").asString()));
        } else {
            return null;
        }
    }

    /**
     * Provider for the JASPI runtime's auth modules.
     *
     * @param configurationResource An instance of the {@code ConfigurationResource}.
     * @param injector The Guice Injector instance.
     * @return A {@code List} of {@code ServerAuthModule} instances.
     * @throws ResourceException If there is a problem reading the runtime's module configuration.
     * @throws ClassNotFoundException If the configured {@code ServerAuthContext} class could not be found.
     */
    @Provides
    @Named("AuthModules")
    @Inject
    public List<ServerAuthModule> getAuthModules(ConfigurationResource configurationResource, Injector injector)
            throws ResourceException, ClassNotFoundException {

        List<ServerAuthModule> authModules = new ArrayList<ServerAuthModule>();

        Resource configuration = newInternalConnection(newSingleton(configurationResource))
                .read(new RootContext(), newReadRequest("configuration"));
        JsonValue authModulesConfig = configuration.getContent().get("serverAuthContext").get("authModules");
        for (JsonValue authModuleConfig : authModulesConfig) {
            authModules.add((ServerAuthModule) injector.getInstance(
                    Class.forName(authModuleConfig.get("className").asString())));
        }

        return authModules;
    }
}
