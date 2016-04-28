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
package org.forgerock.audit.events.handlers;

import java.util.Set;

import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * Abstract AuditEventHandler class.
 */
public abstract class AuditEventHandlerBase implements AuditEventHandler {

    private final String name;
    /** The event topic meta data for the handler. */
    protected final EventTopicsMetaData eventTopicsMetaData;
    private final boolean enabled;

    /**
     * Create a new AuditEventHandler instance.
     *
     * @param name
     *          The name of this AuditEventHandler.
     * @param eventTopicsMetaData
     *          Provides meta-data describing the audit event topics this AuditEventHandler may have to handle.
     * @param acceptedTopics
     *          Audit event topics the AuditEventHandler will handle.
     * @param enabled
     *          Whether or not the audit event handler is enabled.
     *
     */
    protected AuditEventHandlerBase(
            final String name,
            final EventTopicsMetaData eventTopicsMetaData,
            final Set<String> acceptedTopics,
            final boolean enabled) {
        this.name = name;
        this.eventTopicsMetaData = eventTopicsMetaData.filter(acceptedTopics);
        this.enabled = enabled;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<String> getHandledTopics() {
        return eventTopicsMetaData.getTopics();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, String topic,
            ActionRequest request) {
        return new BadRequestException(String.format("Unable to handle action: %s", request.getAction())).asPromise();
    }

}
