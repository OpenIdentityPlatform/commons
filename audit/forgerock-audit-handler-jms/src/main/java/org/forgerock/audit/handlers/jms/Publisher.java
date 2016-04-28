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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.audit.handlers.jms;

import org.forgerock.json.resource.ResourceException;

/**
 * Defines a generic interface for audit event publishers.
 *
 * @param <T> The type of object that this publisher will publish as a representive of the audit event.
 */
public interface Publisher<T> {

    /**
     * Initialize services that are reused by individual publish calls.
     *
     * @throws ResourceException If the publisher cannot be started.
     */
    void startup() throws ResourceException;

    /**
     * Cleanup services that were initialized with {@link #startup()}.
     *
     * @throws ResourceException If the publisher cannot be shut down.
     */
    void shutdown() throws ResourceException;

    /**
     * implement this to deliver the audit event representation to the service.
     *
     * @param message representative object of the audit event
     * @throws ResourceException If the message cannot be published.
     */
    void publish(T message) throws ResourceException;
}