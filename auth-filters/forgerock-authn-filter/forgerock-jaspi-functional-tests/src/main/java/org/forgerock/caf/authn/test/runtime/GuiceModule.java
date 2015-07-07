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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.caf.authn.test.runtime;

import static org.forgerock.caf.authentication.framework.AuthenticationFilter.AuthenticationModuleBuilder.configureModule;
import static org.forgerock.caf.authentication.framework.AuthenticationFilter.AuthenticationModuleBuilder;
import static org.forgerock.json.resource.Requests.newReadRequest;
import static org.forgerock.json.resource.Resources.newInternalConnection;
import static org.forgerock.json.resource.Resources.newSingleton;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.framework.AuditApi;
import org.forgerock.caf.authentication.framework.AuthenticationFilter;
import org.forgerock.caf.authn.test.configuration.ConfigurationResource;
import org.forgerock.http.context.RootContext;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        bind(AuditApi.class).to(TestAuditApi.class);
    }

    @Provides
    Logger getLogger() {
        return LoggerFactory.getLogger("AuthenticationFramework");
    }

    @Provides
    AuthenticationFilter getAuthenticationFilter(Logger logger, AuditApi auditApi,
            @Named("SessionAuthModule") AsyncServerAuthModule sessionAuthModule,
            @Named("AuthModules") List<AsyncServerAuthModule> authModules) {
        List<AuthenticationModuleBuilder> authModuleBuilders = new ArrayList<>();
        for (AsyncServerAuthModule authModule : authModules) {
            authModuleBuilders.add(configureModule(authModule));
        }
        return AuthenticationFilter.builder()
                .logger(logger)
                .auditApi(auditApi)
                .sessionModule(configureModule(sessionAuthModule))
                .authModules(authModuleBuilders)
                .build();
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
    public AsyncServerAuthModule getSessionAuthModule(ConfigurationResource configurationResource, Injector injector)
            throws ResourceException, ClassNotFoundException {

        Resource configuration = newInternalConnection(newSingleton(configurationResource))
                .read(new RootContext(), newReadRequest("configuration"));
        JsonValue sessionModuleConfig = configuration.getContent().get("serverAuthContext").get("sessionModule");
        if (!sessionModuleConfig.isNull()) {
            return (AsyncServerAuthModule) injector.getInstance(
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
    public List<AsyncServerAuthModule> getAuthModules(ConfigurationResource configurationResource, Injector injector)
            throws ResourceException, ClassNotFoundException {

        List<AsyncServerAuthModule> authModules = new ArrayList<>();

        Resource configuration = newInternalConnection(newSingleton(configurationResource))
                .read(new RootContext(), newReadRequest("configuration"));
        JsonValue authModulesConfig = configuration.getContent().get("serverAuthContext").get("authModules");
        for (JsonValue authModuleConfig : authModulesConfig) {
            authModules.add((AsyncServerAuthModule) injector.getInstance(
                    Class.forName(authModuleConfig.get("className").asString())));
        }

        return authModules;
    }
}
