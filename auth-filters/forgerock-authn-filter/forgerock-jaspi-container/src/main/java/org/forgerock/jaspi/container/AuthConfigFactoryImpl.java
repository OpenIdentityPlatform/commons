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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.jaspi.container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.message.AuthException;
import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.config.AuthConfigProvider;
import javax.security.auth.message.config.RegistrationListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Map;

/**
 * Provides methods to register and retrieve AuthConfigProvider instances for a given message layer and application
 * context. This factory is used to obtain the factory -> provider -> config -> context in that order. The context can
 * be used to validate a request and secure the response. A single factory is used by the AuthnFilter; providers need
 * to be populated separately.
 *
 * The following represents a typical sequence of calls for obtaining a server authentication context, and then using
 * it to secure a request.
 *
 * <ol>
 *     <li>AuthConfigFactory factory = AuthConfigFactory.getFactory();</li>
 *     <li>AuthConfigProvider provider = factory.getConfigProvider(layer,appID,listener);</li>
 *     <li>ServerAuthConfig config = provider.getServerAuthConfig(layer,appID,cbh);</li>
 *     <li>String authContextID = config.getAuthContextID(messageInfo);</li>
 *     <li>ClientAuthContext context = config.getAuthContext(authContextID,subject,properties);</li>
 *     <li>context.secureRequest(messageInfo,subject);</li>
 * </ol>
 *
 * A system-wide AuthConfigFactory implementation can be set by invoking setFactory, and retrieved using getFactory.
 */
public class AuthConfigFactoryImpl extends AuthConfigFactory {

    private final static Logger DEBUG = LoggerFactory.getLogger(AuthConfigFactoryImpl.class);

    private AuthConfigProviderMap authConfigProviderMap = new AuthConfigProviderMap();
    private RegistrationIdGenerator registrationIdGenerator = new RegistrationIdGenerator();

    /**
     * Returns the singleton instance of the AuthConfigFactory.
     *
     * @return The AuthConfigFactory instance.
     */
    public static AuthConfigFactory getInstance() {
        return AuthConfigFactoryHolder.INSTANCE.getInstance();
    }

    /**
     * Registers this instance as the system-wide auth config factory implementation.
     */
    AuthConfigFactoryImpl() {
    }

    /**
     * Get a registered AuthConfigProvider from the factory, registered for the identified message layer and
     * application context and registers the given RegistrationListener (if non-null), against the message layer and
     * application context.
     *
     * Rules used to retrieve registered AuthConfigProviders:
     *
     * <ul>
     *     <li>
     *         The provider specifically registered for the given layer and appContext arguments shall be selected.
     *     </li>
     *     <li>
     *         The provider specifically registered for the value passed as the appContext argument and for all
     *         (that is, null) layers shall be selected.
     *     </li>
     *     <li>
     *         The provider specifically registered for the value passed as the layer argument and for all (that is,
     *         null) appContexts shall be selected.
     *     </li>
     *     <li>
     *         The provider registered for all (that is, null) layers and for all (that is, null) appContexts shall be
     *         selected.
     *     </li>
     *     <li>
     *         The factory shall return null.
     *     </li>
     * </ul>
     *
     * @param layer A String identifying the message layer for which the registered AuthConfigProvider is to be
     *              returned. The value of this argument may be null.
     * @param appContext A String that identifies the application messaging context for which the registered
     *                   AuthConfigProvider is to be returned. The value of this argument may be null.
     * @param listener The RegistrationListener whose notify method is to be invoked if the corresponding registration
     *                 is unregistered or replaced. The value of this argument may be null.
     * @return The implementation of the AuthConfigProvider interface registered at the factory for the layer and
     *          appContext, or null if no AuthConfigProvider is selected. An argument listener is attached even if the
     *          return value is null.
     */
    @Override
    public AuthConfigProvider getConfigProvider(String layer, String appContext, RegistrationListener listener) {

        String requestedRegistrationId = registrationIdGenerator.generateRegistrationId(layer, appContext);
        String registrationId = requestedRegistrationId;

        if (!authConfigProviderMap.hasAuthConfigProviderRegistration(registrationId)) {
            registrationId = registrationIdGenerator.generateRegistrationId(null, appContext);
        }

        if (!authConfigProviderMap.hasAuthConfigProviderRegistration(registrationId)) {
            registrationId = registrationIdGenerator.generateRegistrationId(layer, null);
        }

        if (!authConfigProviderMap.hasAuthConfigProviderRegistration(registrationId)) {
            registrationId = registrationIdGenerator.generateRegistrationId(null, null);
        }

        AuthConfigProvider authConfigProvider = authConfigProviderMap.getAuthConfigProvider(registrationId);

        if (listener != null) {
            authConfigProviderMap.addRegistrationListener(requestedRegistrationId, listener);
        }

        return authConfigProvider;
    }

    /**
     * Registers the AuthConfigProvider in the factory keyed by the message layer and application context.
     *
     * At most one registration may exist within the factory for a given combination of message layer and appContext.
     * Any pre-existing registration with identical values for layer and appContext is replaced by a subsequent
     * registration. When replacement occurs, the registration identifier, layer, and appContext identifier remain
     * unchanged, and the AuthConfigProvider (with initialization properties) and description are replaced.
     *
     * The returned registration id will be unique within the Java process and will never assign a previously used
     * registration identifier to a registration whose message layer and or appContext identifier differ from the
     * previous use.
     *
     * @param className The fully qualified name of an AuthConfigProvider implementation class (or null). Calling this
     *                  method with a null value for this parameter shall cause getConfigProvider to return null when
     *                  it is called with layer and appContext values for which the resulting registration is the best
     *                  match.
     * @param properties A Map object containing the initialization properties to be passed to the properties
     *                   argument of the provider constructor. This argument may be null. When this argument is not
     *                   null, all the values and keys occurring in the Map must be of type String.
     * @param layer A String identifying the message layer for which the provider will be registered at the factory.
     *              A null value may be passed as an argument for this parameter, in which case the provider is
     *              registered at all layers.
     * @param appContext A String value that may be used by a runtime to request a configuration object from this
     *                   provider. A null value may be passed as an argument for this parameter, in which case the
     *                   provider is registered for all configuration ids (at the indicated layers).
     * @param description A text String describing the provider. This value may be null.
     * @return A String identifier assigned by the factory to the provider registration,
     *          and that may be used to remove the registration from the factory. Or null if there was a problem
     *          constructing an instance of the AuthConfigProvider.
     */
    @Override
    public String registerConfigProvider(String className, Map properties, String layer, String appContext,
            String description) {

        AuthConfigProvider authConfigProvider;
        try {
            authConfigProvider = constructAuthConfigProvider(className, properties);
        } catch (Exception e) {
            DEBUG.error("Could not instantiate AuthConfigProvider, {}", className);
            // Cannot throw error message as interface does not allow.
            throw new RuntimeException(MessageFormat.format("Could not instantiate AuthConfigProvider, {0}",
                    className));
        }

        String registrationId = registrationIdGenerator.generateRegistrationId(layer, appContext);
        registerConfigProvider(registrationId, authConfigProvider, layer, appContext, description, false);

        return registrationId;
    }

    /**
     * Constructs an instance of the AuthConfigProvider, using the required constructor with a property Map and
     * AuthConfigFactory, which is null as self-registration for the AuthConfigProvider is not required.
     *
     * @param className The fully qualified name of an AuthConfigProvider implementation class (not null).
     * @param properties A Map object containing the initialization properties to be passed to the properties
     *                   argument of the provider constructor
     * @return An instance of the AuthConfigProvider with the given class name.
     * @throws ClassNotFoundException If a class cannot be found with the given class name.
     * @throws NoSuchMethodException If the AuthConfigProvider class does not have the required constructor.
     * @throws IllegalAccessException If the AuthConfigProvider's constructor is not accessible.
     * @throws InvocationTargetException If an exception is thrown from the AuthConfigProvider's constructor.
     * @throws InstantiationException If the AuthConfigProvider class could not be constructed.
     */
    private AuthConfigProvider constructAuthConfigProvider(String className, Map properties)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException,
            InstantiationException {

        Class<?> authConfigProviderClass = Class.forName(className);
        Constructor constructor = authConfigProviderClass.getConstructor(Map.class, AuthConfigFactory.class);

        return (AuthConfigProvider) constructor.newInstance(properties, null);
    }

    /**
     * Registers the AuthConfigProvider in the factory keyed by the message layer and application context.
     *
     * At most one registration may exist within the factory for a given combination of message layer and appContext.
     * Any pre-existing registration with identical values for layer and appContext is replaced by a subsequent
     * registration. When replacement occurs, the registration identifier, layer, and appContext identifier remain
     * unchanged, and the AuthConfigProvider (with initialization properties) and description are replaced.
     *
     * The returned registration id will be unique within the Java process and will never assign a previously used
     * registration identifier to a registration whose message layer and or appContext identifier differ from the
     * previous use.
     *
     * @param provider The AuthConfigProvider to be registered at the factory (or null). Calling this method with a
     *                 null value for this parameter shall cause getConfigProvider to return null when it is called
     *                 with layer and appContext values for which the resulting registration is the best match.
     * @param layer A String identifying the message layer for which the provider will be registered at the factory.
     *              A null value may be passed as an argument for this parameter, in which case the provider is
     *              registered at all layers.
     * @param appContext A String value that may be used by a runtime to request a configuration object from this
     *                   provider. A null value may be passed as an argument for this parameter, in which case the
     *                   provider is registered for all configuration ids (at the indicated layers).
     * @param description A text String describing the provider. This value may be null.
     * @return A String identifier assigned by the factory to the provider registration, and that may be used to remove
     *          the registration from the factory.
     */
    @Override
    public String registerConfigProvider(AuthConfigProvider provider, String layer, String appContext,
                String description) {

        String registrationId = registrationIdGenerator.generateRegistrationId(layer, appContext);
        registerConfigProvider(registrationId, provider, layer, appContext, description, false);

        return registrationId;
    }

    /**
     * Performs the actual registration of the AuthConfigProvider in the factory.
     *
     * Checks to see if an AuthConfigProvider has already been registered with the same message layer and application
     * context and if so will replace the AuthConfigProvider instance and description in the RegistrationContext.
     *
     * @param registrationId A String that identifies a provider registration at the factory.
     * @param authConfigProvider The AuthConfigProvider to be registered at the factory (or null). Calling this method
     *                           with a null value for this parameter shall cause getConfigProvider to return null
     *                           when it is called with layer and appContext values for which the resulting
     *                           registration is the best match.
     * @param layer A String identifying the message layer for which the provider will be registered at the factory.
     *              A null value may be passed as an argument for this parameter, in which case the provider is
     *              registered at all layers.
     * @param appContext A String value that may be used by a runtime to request a configuration object from this
     *                   provider. A null value may be passed as an argument for this parameter, in which case the
     *                   provider is registered for all configuration ids (at the indicated layers).
     * @param description A text String describing the provider. This value may be null.
     * @param isPersistent A boolean indicating whether the registration is the result of a className based
     *                     registration, or an instance-based (for example, self-) registration. Only registrations
     *                     performed using the five argument registerConfigProvider method are persistent.
     */
    private void registerConfigProvider(String registrationId, AuthConfigProvider authConfigProvider, String layer,
            String appContext, String description, boolean isPersistent) {

        if (authConfigProviderMap.hasAuthConfigProviderRegistration(registrationId)) {
            try {
                authConfigProviderMap.replaceAuthConfigProvider(registrationId, authConfigProvider);
            } catch (AuthException e) {
                // This should never happen as have already checked if provider is already in map
                authConfigProviderMap.addAuthConfigProvider(registrationId, authConfigProvider);
            }
        } else {
            authConfigProviderMap.addAuthConfigProvider(registrationId, authConfigProvider);
        }
        authConfigProviderMap.addRegistrationContext(registrationId, new RegistrationContextImpl(layer, appContext,
                description, isPersistent));
        notifyRegistrationListeners(registrationId);
    }

    /**
     * Calls the notify(layer, appContext) method on all registered RegistrationListeners for the given registration
     * id.
     *
     * @param registrationId A String that identifies a provider registration at the factory
     */
    private void notifyRegistrationListeners(String registrationId) {

        String[] layerAppContextPair = registrationIdGenerator.decodeRegistrationId(registrationId);
        String layer = null;
        String appContext = null;
        if (layerAppContextPair != null && layerAppContextPair.length > 0) {
            if (registrationId.indexOf("<>") > 0) {
                layer = layerAppContextPair[0];
                if (layerAppContextPair.length > 1) {
                    appContext = layerAppContextPair[1];
                }
            } else {
                appContext = layerAppContextPair[0];
            }
        }

        for (RegistrationListener regListener : authConfigProviderMap.getRegistrationListener(registrationId)) {
            regListener.notify(layer, appContext);
        }
    }

    /**
     * Removes the identified provider registration from the factory and invoke any listeners associated with the
     * removed registration.
     *
     * @param registrationId A String that identifies a provider registration at the factory
     * @return True if there was a registration with the specified identifier and it was removed. Return false if the
     * registrationID was invalid.
     */
    @Override
    public boolean removeRegistration(String registrationId) {
        boolean result = authConfigProviderMap.removeAuthConfigProviderRegistration(registrationId);
        notifyRegistrationListeners(registrationId);
        return result;
    }

    /**
     * Disassociates the listener from all the provider registrations whose layer and appContext values are matched
     * by the corresponding arguments to this method.
     *
     * @param listener The RegistrationListener to be detached.
     * @param layer A String identifying the message layer or null.
     * @param appContext A String value identifying the application contex or null.
     * @return An array of String values where each value identifies a provider registration from which the listener
     *          was removed. An empty array if the listener was not removed from any registrations.
     */
    @Override
    public String[] detachListener(RegistrationListener listener, String layer, String appContext) {
        String registrationId = registrationIdGenerator.generateRegistrationId(layer, appContext);
        return authConfigProviderMap.removeRegistrationListener(registrationId, listener);
    }

    /**
     * Get the registration identifiers for all registrations of the provider instance at the factory.
     *
     * @param provider The AuthConfigurationProvider whose registration identifiers are to be returned.
     *                 If this argument is null, then all the IDs of the active registrations within the factory are to
     *                 be returned.
     * @return An array of String values where each value identifies a provider registration at the factory.
     *          An empty array when there are no registrations at the factory for the identified provider.
     */
    @Override
    public String[] getRegistrationIDs(AuthConfigProvider provider) {
        return authConfigProviderMap.getRegistrationIds(provider);
    }

    /**
     * Get the the registration context for the identified registration.
     *
     * @param registrationId A String that identifies a provider registration at the factory
     * @return A RegistrationContext or null. When a Non-null value is returned, it is a copy of the registration
     *          context corresponding to the registration. Null is returned when the registration identifier does not
     *          correspond to an active registration.
     */
    @Override
    public RegistrationContext getRegistrationContext(String registrationId) {
        RegistrationContext regContext =  authConfigProviderMap.getRegistrationContext(registrationId);
        return new RegistrationContextImpl(regContext.getMessageLayer(), regContext.getAppContext(),
                regContext.getDescription(), regContext.isPersistent());
    }

    /**
     * Cause the factory to reprocess its persistent declarative representation of provider registrations.
     */
    @Override
    public void refresh() {
        // Not required as there is only ever one AuthConfigProvider in this JASPI Container implementation.
    }

    /**
     * A class that provides a method to generate registration ids for AuthConfigProvider registration according to
     * the JASPI (JSR-196) specification.
     */
    static class RegistrationIdGenerator {

        private static final String SEPARATOR = "<>";

        /**
         * The returned registration id will be unique within the Java process and will never assign a previously used
         * registration identifier to a registration whose message layer and or appContext identifier differ from the
         * previous use.
         *
         * Generates a registration id that will be unique within the Java process and will never generate a
         * previously used registration id to a registration whose message layer and or appContext identifier
         * differ from the previous use.
         *
         * @param layer A String identifying the message layer or null.
         * @param appContext A String value identifying the application context or null.
         * @return A String that identifies a provider registration at the factory.
         */
        public String generateRegistrationId(String layer, String appContext) {
            StringBuilder regId = new StringBuilder();
            if (layer != null) {
                regId.append(layer);
            }
            regId.append(SEPARATOR);
            if (appContext != null) {
                regId.append(appContext);
            }
            return regId.toString();
        }

        /**
         * Decodes the registration id into message layer and application context.
         *
         * @param registrationId A String that identifies a provider registration at the factory.
         * @return A String array with the message layer at index 0 and application context at index 1.
         */
        public String[] decodeRegistrationId(String registrationId) {
            return registrationId.split(SEPARATOR);
        }
    }
}


