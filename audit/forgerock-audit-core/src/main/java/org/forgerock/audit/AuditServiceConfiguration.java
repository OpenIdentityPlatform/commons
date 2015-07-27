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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration of the audit service.
 * <p>
 * This configuration object can be created from JSON. Example of valid JSON configuration:
 * <pre>
 *   {
 *     "enabled" : true,
 *     "handlerForQueries" : "csv"
 *   }
 * </pre>
 */
public class AuditServiceConfiguration {

    @JsonProperty(required = true)
    private String handlerForQueries;

    @JsonProperty
    private boolean createEnabled = true;

    /**
     * Empty constructor.
     */
    public AuditServiceConfiguration() {
        // empty constructor
    }

    /**
     * Copy-constructor, in order to obtain a copy from an existing configuration.
     *
     * @param config an existing configuration
     */
    public AuditServiceConfiguration(AuditServiceConfiguration config) {
        handlerForQueries = config.getHandlerForQueries();
        createEnabled = config.isCreateEnabled();
    }

    /**
     * Returns the name of the handler to use for querying the audit events.
     *
     * @return the name of the handler.
     */
    public String getHandlerForQueries() {
        return handlerForQueries;
    }

    /**
     * Sets the name of the handler to use for querying the audit events.
     *
     * @param name
     *            the name of the handler.
     */
    public void setHandlerForQueries(String name) {
        handlerForQueries = name;
    }

    /**
     * Returns true if logging of audit events is currently enabled.
     *
     * @return whether logging of audit events is currently enabled.
     */
    public boolean isCreateEnabled() {
        return createEnabled;
    }

    /**
     * Sets whether audit events are passed to registered handlers.
     *
     * @param value The new state.
     */
    public void setCreateEnabled(boolean value) {
        this.createEnabled = value;
    }

}
