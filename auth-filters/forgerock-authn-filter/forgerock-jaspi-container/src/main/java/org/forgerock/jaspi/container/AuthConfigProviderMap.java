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

import javax.security.auth.message.AuthException;
import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.config.AuthConfigProvider;
import javax.security.auth.message.config.RegistrationListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages the data structure to store AuthConfigProvider, RegistrationListeners and RegistrationContext
 * registrations keyed by a registration id.
 */
public class AuthConfigProviderMap {

    private final Map<String, AuthConfigProvider> authConfigProviders = new HashMap<String, AuthConfigProvider>();
    private final Map<String, Set<RegistrationListener>> registrationListeners =
            new HashMap<String, Set<RegistrationListener>>();
    private final Map<String, AuthConfigFactory.RegistrationContext> registrationContexts =
            new HashMap<String, AuthConfigFactory.RegistrationContext>();

    /**
     * Checks if an AuthConfigProvider has been registered with the given registration id.
     *
     * @param registrationId A String that identifies a provider registration.
     * @return True if such a registration exists, otherwise False.
     */
    public synchronized boolean hasAuthConfigProviderRegistration(String registrationId) {
        return authConfigProviders.containsKey(registrationId);
    }

    /**
     * Gets a AuthConfigProvider instance which is registered with the given registration id.
     *
     * @param registrationId A String that identifies a provider registration.
     * @return The AuthConfigProvider instance registered with the registration id.
     */
    public synchronized AuthConfigProvider getAuthConfigProvider(String registrationId) {
        return authConfigProviders.get(registrationId);
    }

    /**
     * Adds the RegistrationListener to a collection of RegistrationListeners for the given registration id.
     *
     * @param registrationId A String that identifies a provider registration.
     * @param listener A RegistrationListener instance to by notified on replacement/removal of the associated
     *                 AuthConfigProvider.
     */
    public synchronized void addRegistrationListener(String registrationId, RegistrationListener listener) {
        Set<RegistrationListener> regListeners = registrationListeners.get(registrationId);
        if (regListeners == null) {
            regListeners = new HashSet<RegistrationListener>();
        }
        regListeners.add(listener);
        registrationListeners.put(registrationId, regListeners);
    }

    /**
     * Adds the AuthConfigProvider instance keyed by both registration id and layer, appContext combination.
     *
     * @param registrationId A String that identifies a provider registration.
     * @param authConfigProvider The AuthConfigProvider instance to add.
     */
    public synchronized void addAuthConfigProvider(String registrationId, AuthConfigProvider authConfigProvider) {
        authConfigProviders.put(registrationId, authConfigProvider);
    }

    /**
     * Will replace the current AuthConfigProvider keyed by the registration id.
     *
     * @param registrationId A String that identifies a provider registration.
     * @param authConfigProvider The AuthConfigProvider instance to replace the current registration with.
     * @throws AuthException If there is no current AuthConfigProvider currently associated with the registration id.
     */
    public void replaceAuthConfigProvider(String registrationId, AuthConfigProvider authConfigProvider)
            throws AuthException {
        if (getAuthConfigProvider(registrationId) == null) {
            throw new AuthException("No AuthConfigProvider is currently associated with registration id, "
                    + registrationId + ". Cannot replace with new AuthConfigProvider.");
        }
        authConfigProviders.put(registrationId, authConfigProvider);
    }

    /**
     * Adds the RegistrationContext instance with the given registration id.
     *
     * @param registrationId A String that identifies a provider registration.
     * @param registrationContext The RegistrationContext instance to add.
     */
    public synchronized void addRegistrationContext(String registrationId,
            AuthConfigFactory.RegistrationContext registrationContext) {
        registrationContexts.put(registrationId, registrationContext);
    }

    /**
     * Will remove the AuthConfigProvider instance with the given registration id.
     *
     * If no AuthConfigProvider instance is associated with the registration id, then no operation will be performed
     * and the method will return false.
     *
     * @param registrationId A String that identifies a provider registration.
     * @return True if an entry is removed with the registration id, False is no such entry exists.
     */
    public synchronized boolean removeAuthConfigProviderRegistration(String registrationId) {
        return authConfigProviders.remove(registrationId) != null;
    }

    /**
     * Removes the listener from all the provider registrations whose layer and appContext values are matched
     * by the corresponding arguments to this method.
     *
     * Will only ever return an array with one registration id.
     *
     * @param registrationId A String that identifies a provider registration.
     * @param listener The RegistrationListener instance to remove from all registrations.
     * @return An array of String values where each value identifies a provider registration from which the listener
     *          was removed. An empty array if the listener was not removed from any registrations.
     */
    public synchronized String[] removeRegistrationListener(String registrationId, RegistrationListener listener) {

        List<String> registrationIds = new ArrayList<String>();

        Set<RegistrationListener> regListeners = registrationListeners.get(registrationId);

        if (regListeners != null) {
            if (regListeners.remove(listener)) {
                registrationIds.add(registrationId);
            }
        }

        return registrationIds.toArray(new String[0]);
    }

    /**
     * Get the registration ids for all registrations of the AuthConfigProvider instance.
     *
     * @param provider The AuthConfigProvider instance to find registrations of.
     * @return An array of String values where each value identifies a provider registration at the factory.
     *          An empty array when there are no registrations at the factory for the identified provider.
     */
    public synchronized String[] getRegistrationIds(AuthConfigProvider provider) {

        List<String> registrationIds = new ArrayList<String>();

        for (String registrationId : authConfigProviders.keySet()) {

            AuthConfigProvider authConfigProvider = authConfigProviders.get(registrationId);

            if (authConfigProvider.equals(provider))  {
                registrationIds.add(registrationId);
            }
        }

        return registrationIds.toArray(new String[0]);
    }

    /**
     * Gets the RegistrationContext registered for the given registration id.
     *
     * @param registrationId A String that identifies a provider registration.
     * @return A RegistrationContext instance associated with the registration id, or null.
     */
    public synchronized AuthConfigFactory.RegistrationContext getRegistrationContext(String registrationId) {
        return registrationContexts.get(registrationId);
    }

    /**
     * Gets the RegistrationListeners registered for the given registration id.
     *
     * @param registrationId A String that identifies a provider registration.
     * @return A Set of RegistrationListeners instance associated with the registration id, or an empty set.
     */
    public synchronized Set<RegistrationListener> getRegistrationListener(String registrationId) {
        Set<RegistrationListener> regListeners = registrationListeners.get(registrationId);
        if (regListeners == null) {
            regListeners = new HashSet<RegistrationListener>();
        }
        return regListeners;
    }
}
