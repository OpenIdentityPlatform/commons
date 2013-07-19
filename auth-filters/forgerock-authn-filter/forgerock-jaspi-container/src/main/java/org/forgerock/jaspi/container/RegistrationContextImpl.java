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

import javax.security.auth.message.config.AuthConfigFactory;

/**
 * Represents the layer identifier, application context identifier, and description components of an AuthConfigProvider
 * registration at the factory.
 */
public class RegistrationContextImpl implements AuthConfigFactory.RegistrationContext {

    private final String layer;
    private final String appContext;
    private final String description;
    private final boolean isPersistent;

    /**
     * Constructor to populate the RegistrationContextImpl.
     *
     * @param layer A String identifying the message layer for which the AuthConfigProvider was registered. The
     *              returned value may be null.
     * @param appContext A String identifying the application context for which the AuthConfigProvider was registered.
     *                   The returned value may be null.
     * @param description The description String from the registration, or null if no description string was included
     *                    in the registration.
     * @param isPersistent A boolean indicating whether the registration is the result of a className based
     *                     registration, or an instance-based (for example, self-registration).
     */
    public RegistrationContextImpl(String layer, String appContext, String description, boolean isPersistent) {
        this.layer = layer;
        this.appContext = appContext;
        this.description = description;
        this.isPersistent = isPersistent;
    }

    /**
     * Get the layer name from the registration context.
     *
     * @return A String identifying the message layer for which the AuthConfigProvider was registered. The returned
     *          value may be null.
     */
    public String getMessageLayer() {
        return layer;
    }

    /**
     * Get the application context identifier from the registration context.
     *
     * @return A String identifying the application context for which the AuthConfigProvider was registered. The
     *          returned value may be null.
     */
    public String getAppContext() {
        return appContext;
    }

    /**
     * Get the description from the registration context.
     *
     * @return The description String from the registration, or null if no description string was included in the
     *          registration.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the persisted status from the registration context.
     *
     * @return A boolean indicating whether the registration is the result of a className based registration, or an
     *          instance-based (for example, self-registration).
     */
    public boolean isPersistent() {
        return isPersistent;
    }
}
