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

package org.forgerock.util.thread.listener;

/**
 * Interface used by shutdown managers to allow for thread safe
 * adding and removing of shutdown listeners.
 */
public interface ShutdownManager {

    /**
     * Adds a ShutdownListener to this ShutdownManager with the default priority.
     *
     * @param listener The listener to be added.
     */
    void addShutdownListener(ShutdownListener listener);

    /**
     * Adds a ShutdownListener to this ShutdownManager with the supplied priority.
     *
     * @param listener The listener to be added.
     * @param priority The priority of the listener when added.
     */
    void addShutdownListener(ShutdownListener listener, ShutdownPriority priority);

    /**
     * Reaplces an existing ShutdownListener with the new ShutdownListener.
     *
     * @param oldListener To be replaced.
     * @param newListener The replacement.
     * @param priority Replacement listeners priority.
     */
    void replaceShutdownListener(ShutdownListener oldListener, ShutdownListener newListener, ShutdownPriority priority);

    /**
     * Removes a ShutdownListener from this ShutdownManager.
     *
     * @param listener The listener to be removed.
     */
    void removeShutdownListener(ShutdownListener listener);

    /**
     * Shuts down all the listeners in this ShutdownManager.
     */
    void shutdown();

}
