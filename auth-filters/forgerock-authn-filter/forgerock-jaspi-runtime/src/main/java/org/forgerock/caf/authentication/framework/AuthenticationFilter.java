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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.caf.authentication.framework;

import static org.forgerock.caf.authentication.framework.AuthenticationFramework.REQUIRED_MESSAGE_TYPES_SUPPORT;
import static org.forgerock.caf.authentication.framework.JaspiAdapters.adapt;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.module.ServerAuthModule;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.forgerock.caf.authentication.api.AsyncServerAuthContext;
import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.http.Context;
import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.ResponseException;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.PromiseImpl;
import org.forgerock.util.promise.Promises;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>A HTTP {@link Filter} that will protect all downstream filters or handlers.</p>
 *
 * <p>The filter instance is created by creating a builder instance, ({@link #builder()}), and
 * providing the modules and configuration, for the particular authentication framework instance,
 * that will be used to authenticate incoming requests and outgoing responses.</p>
 *
 * <p>The authentication framework can be configured with a single session authentication module,
 * which will authenticate requests based on some session identifier, and an ordered list of
 * authentication modules, that are executed in order on a first succeeds wins basis.</p>
 *
 * <p>The authentication framework must be configured with a non-{@code null} {@link AuditApi}
 * instance, so that it can audit authentication outcomes.</p>
 *
 * @since 2.0.0
 */
public final class AuthenticationFilter implements Filter {

    private final AuthenticationFramework runtime;

    /**
     * Creates a new {@code JaspiRuntime} instance that will use the configured {@code authContext}
     * to authenticate incoming request and secure outgoing response messages.
     *
     * @param logger The non-{@code null} {@link Logger} instance.
     * @param auditApi The non-{@code null} {@link AuditApi} instance.
     * @param responseHandler The non-{@code null} {@link ResponseHandler} instance.
     * @param authContext The non-{@code null} {@link AsyncServerAuthContext} instance.
     * @param serviceSubject The non-{@code null} service {@link Subject}.
     * @param initializationPromise A {@link Promise} which will be completed once the configured
     *                              auth modules have been initialised.
     */
    AuthenticationFilter(Logger logger, AuditApi auditApi, Subject serviceSubject, ResponseHandler responseHandler,
            AsyncServerAuthContext authContext, Promise<List<Void>, AuthenticationException> initializationPromise) {
        this.runtime = new AuthenticationFramework(logger, auditApi, responseHandler, authContext, serviceSubject,
                initializationPromise);
    }

    /**
     * Authenticates incoming request messages and if successful calls the downstream filter or
     * handler and then secures the returned response.
     *
     * @param context The request context.
     * @param request The request.
     * @param next The downstream filter or handler in the chain that should only be called if the
     *             request was successfully authenticated.
     * @return A {@code Promise} representing the response to be returned to the client.
     */
    @Override
    public Promise<Response, ResponseException> filter(Context context, Request request, Handler next) {
        return runtime.processMessage(context, request, next);
    }

    @Override
    public String toString() {
        return runtime.toString();
    }

    /**
     * Returns a new {@code AuthenticationFilterBuilder} instance which is to be used to configure
     * an instance of the Authentication Framework.
     *
     * @return An {@code AuthenticationFilterBuilder} instance.
     */
    public static AuthenticationFilterBuilder builder() {
        return new AuthenticationFilterBuilder();
    }

    /**
     * <p>Builder class that configures an Authentication Framework instance.</p>
     *
     * <p>Usage:
     * <pre><code>
     * builder.logger(logger)
     *         .auditApi(auditApi)
     *         .serviceSubject(serviceSubject)
     *         .responseHandler(responseHandler)
     *         .sessionModule(
     *                 configureModule(sessionAuthModule)
     *                         .requestPolicy(sessionAuthModuleRequestPolicy)
     *                         .responsePolicy(sessionAuthModuleResponsePolicy)
     *                         .callbackHandler(sessionAuthModuleHandler)
     *                         .withSettings(sessionAuthModuleSettings))
     *         .authModules(
     *                 configureModule(authModuleOne)
     *                         .requestPolicy(authModuleOneRequestPolicy)
     *                         .responsePolicy(authModuleOneResponsePolicy)
     *                         .callbackHandler(authModuleOneHandler)
     *                         .withSettings(authModuleOneSettings),
     *                 configureModule(authModuleTwo)
     *                         .requestPolicy(authModuleTwoRequestPolicy)
     *                         .responsePolicy(authModuleTwoResponsePolicy)
     *                         .callbackHandler(authModuleTwoHandler)
     *                         .withSettings(authModuleTwoSettings))
     *         .build();
     * </code></pre>
     * </p>
     *
     * @since 2.0.0
     */
    public static class AuthenticationFilterBuilder {

        private String name = "AuthenticationFilter";
        private Logger logger;
        private AuditApi auditApi;
        private Subject serviceSubject = new Subject();
        private final ResponseHandler responseHandler = new ResponseHandler();
        private AuthenticationModuleBuilder sessionAuthModuleBuilder = null;
        private final List<AuthenticationModuleBuilder> authModuleBuilders =
                new ArrayList<AuthenticationModuleBuilder>();

        /**
         * <p>Sets the name of the logger instance that the framework should create and use to log
         * debug messages.</p>
         *
         * <p>If not set, the name defaults to: AuthenticationFilter.</p>
         *
         * @param name The name of the {@code Logger} instance.
         * @return This builder instance.
         */
        public AuthenticationFilterBuilder named(String name) {
            this.name = name;
            return this;
        }

        /**
         * <p>Sets the logger instance that the framework will use to log debug messages.</p>
         *
         * <p>If not set, the name defaults to: AuthenticationFilter.</p>
         *
         * @param logger The {@code Logger} instance.
         * @return This builder instance.
         */
        public AuthenticationFilterBuilder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        /**
         * Sets the, mandatory, {@code AuditApi} instance that the framework will use to audit the
         * authentication result of processed requests.
         *
         * @param auditApi The {@code AuditApi} instance.
         * @return This builder instance.
         */
        public AuthenticationFilterBuilder auditApi(AuditApi auditApi) {
            this.auditApi = auditApi;
            return this;
        }

        /**
         * Sets the service {@code Subject} that contains credentials, for this framework instance,
         * that auth modules can use to secure response messages.
         *
         * @param serviceSubject The service {@code Subject} instance.
         * @return This builder instance.
         */
        public AuthenticationFilterBuilder serviceSubject(Subject serviceSubject) {
            this.serviceSubject = serviceSubject;
            return this;
        }

        /**
         * Adds an additional response handler instance that adds support for protecting resources
         * which return responses with non-JSON content types.
         *
         * @param responseWriter The {@code ResourceExceptionHandler} instance.
         * @return This builder instance.
         */
        public AuthenticationFilterBuilder responseHandler(ResponseWriter responseWriter) {
            this.responseHandler.addResponseWriter(responseWriter);
            return this;
        }

        /**
         * Sets the session authentication module that will be used to validate request sessions
         * and maintain sessions on response messages.
         *
         * @param sessionAuthModuleBuilder A {@code AuthenticationModuleBuilder} instance.
         * @return This builder instance.
         */
        public AuthenticationFilterBuilder sessionModule(AuthenticationModuleBuilder sessionAuthModuleBuilder) {
            if (sessionAuthModuleBuilder != null && sessionAuthModuleBuilder.authModule != null) {
                checkMessageTypeSupport(sessionAuthModuleBuilder.authModule);
                this.sessionAuthModuleBuilder = sessionAuthModuleBuilder;
            }
            return this;
        }

        /**
         * Sets the authentication modules that will be used to validate requests and secure
         * response messages.
         *
         * @param authModuleBuilders A {@code AuthenticationModuleBuilder} instance.
         * @return This builder instance.
         * @see #authModules(java.util.List)
         */
        public AuthenticationFilterBuilder authModules(AuthenticationModuleBuilder... authModuleBuilders) {
            return authModules(Arrays.asList(authModuleBuilders));
        }

        /**
         * Sets the authentication modules that will be used to validate requests and secure
         * response messages.
         *
         * @param authModuleBuilders A {@code AuthenticationModuleBuilder} instance.
         * @return This builder instance.
         * @see #authModules(AuthenticationModuleBuilder...)
         */
        public AuthenticationFilterBuilder authModules(List<AuthenticationModuleBuilder> authModuleBuilders) {
            for (AuthenticationModuleBuilder authModuleBuilder : authModuleBuilders) {
                checkMessageTypeSupport(authModuleBuilder.authModule);
                this.authModuleBuilders.add(authModuleBuilder);
            }
            return this;
        }

        private void checkMessageTypeSupport(AsyncServerAuthModule authModule) {
            Reject.ifFalse(authModule.getSupportedMessageTypes().containsAll(REQUIRED_MESSAGE_TYPES_SUPPORT),
                    "Auth Module, " + authModule.getModuleId() + ", does not support the required message types: "
                            + REQUIRED_MESSAGE_TYPES_SUPPORT);
        }

        /**
         * Creates a new {@code JaspiRuntimeFilter} instance based on the configuration provided.
         *
         * @return A new Authentication Framework filter instance.
         * @throws IllegalStateException If the {@code AuditApi} instance has not been set.
         */
        public AuthenticationFilter build() {
            if (auditApi == null) {
                throw new IllegalStateException("Audit Api must be set");
            }

            if (logger == null) {
                logger = LoggerFactory.getLogger(name);
            }

            AsyncServerAuthModule sessionAuthModule = null;
            List<AsyncServerAuthModule> authModules = new ArrayList<AsyncServerAuthModule>();

            List<Promise<Void, AuthenticationException>> initializationPromises =
                    new ArrayList<Promise<Void, AuthenticationException>>();
            PromiseImpl<Void, AuthenticationException> kicker = PromiseImpl.create();
            initializationPromises.add(kicker);

            if (sessionAuthModuleBuilder != null && sessionAuthModuleBuilder.authModule != null) {
                sessionAuthModule = sessionAuthModuleBuilder.authModule;
                initializationPromises.add(initializeModule(sessionAuthModuleBuilder));
            }
            for (AuthenticationModuleBuilder authModuleBuilder : authModuleBuilders) {
                authModules.add(authModuleBuilder.authModule);
                initializationPromises.add(initializeModule(authModuleBuilder));
            }
            Promise<List<Void>, AuthenticationException> initializationPromise = Promises.when(initializationPromises);
            kicker.handleResult(null);
            return createFilter(logger, auditApi, responseHandler, serviceSubject, sessionAuthModule, authModules,
                    initializationPromise);
        }

        private Promise<Void, AuthenticationException> initializeModule(AuthenticationModuleBuilder moduleBuilder) {
            return moduleBuilder.authModule.initialize(moduleBuilder.requestPolicy, moduleBuilder.responsePolicy,
                    moduleBuilder.handler, moduleBuilder.settings);
        }

        AuthenticationFilter createFilter(Logger logger, AuditApi auditApi, ResponseHandler responseHandler,
                Subject serviceSubject, AsyncServerAuthModule sessionAuthModule,
                List<AsyncServerAuthModule> authModules,
                Promise<List<Void>, AuthenticationException> initializationPromise) {
            return new AuthenticationFilter(logger, auditApi, serviceSubject, responseHandler,
                    new AggregateAuthContext(logger, new SessionAuthContext(logger, sessionAuthModule),
                            new FallbackAuthContext(logger, authModules)), initializationPromise);
        }
    }

    /**
     * <p>Builder class that configures {@link AsyncServerAuthModule}s and
     * {@link ServerAuthModule}s.</p>
     *
     * <p>Usage:
     * <pre><code>
     * configureModule(authModuleOne)
     *         .requestPolicy(authModuleOneRequestPolicy)
     *         .responsePolicy(authModuleOneResponsePolicy)
     *         .callbackHandler(authModuleOneHandler)
     *         .withSettings(authModuleOneSettings);
     * </code></pre>
     * </p>
     *
     * @since 2.0.0
     */
    public static final class AuthenticationModuleBuilder {

        private MessagePolicy requestPolicy;
        private MessagePolicy responsePolicy;
        private CallbackHandler handler;
        private Map<String, Object> settings;
        private AsyncServerAuthModule authModule;

        /**
         * Creates a builder to configure the provided {@code AsyncServerAuthModule} instance.
         *
         * @param authModule The {@code AsyncServerAuthModule} instance.
         * @return This auth module builder instance.
         */
        public static AuthenticationModuleBuilder configureModule(AsyncServerAuthModule authModule) {
            AuthenticationModuleBuilder builder = new AuthenticationModuleBuilder();
            builder.authModule = authModule;
            return builder;
        }

        /**
         * Creates a builder to configure the provided {@code ServerAuthModule} instance.
         *
         * @param authModule The {@code ServerAuthModule} instance.
         * @return This auth module builder instance.
         */
        public static AuthenticationModuleBuilder configureModule(ServerAuthModule authModule) {
            AuthenticationModuleBuilder builder = new AuthenticationModuleBuilder();
            builder.authModule = adapt(authModule);
            return builder;
        }

        /**
         * Sets the request {@code MessagePolicy} that the auth module should use.
         *
         * @param requestPolicy The request {@code MessagePolicy}.
         * @return This auth module builder instance.
         */
        public AuthenticationModuleBuilder requestPolicy(MessagePolicy requestPolicy) {
            this.requestPolicy = requestPolicy;
            return this;
        }

        /**
         * Sets the response {@code MessagePolicy} that the auth module should use.
         *
         * @param responsePolicy The response {@code MessagePolicy}.
         * @return This auth module builder instance.
         */
        public AuthenticationModuleBuilder responsePolicy(MessagePolicy responsePolicy) {
            this.responsePolicy = responsePolicy;
            return this;
        }

        /**
         * Sets the {@code CallbackHandler} that the auth module should use.
         *
         * @param handler The {@code CallbackHandler} instance.
         * @return This auth module builder instance.
         */
        public AuthenticationModuleBuilder callbackHandler(CallbackHandler handler) {
            this.handler = handler;
            return this;
        }

        /**
         * Sets the settings that contain configuration information that the auth module will use
         * to configure itself.
         *
         * @param settings The auth module settings.
         * @return This auth module builder instance.
         */
        public AuthenticationModuleBuilder withSettings(Map<String, Object> settings) {
            this.settings = settings;
            return this;
        }
    }
}
