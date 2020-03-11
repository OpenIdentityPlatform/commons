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

import java.util.Arrays;
import java.util.List;

/**
 * This class defines the shutdown priorities that are consumed by
 * <code>com.sun.identity.common.ShutdownManager</code>.
 */
public enum ShutdownPriority {

    /**
     * HIGHEST is the priority pre-defined in the system. Components which are
     * registered with this priority will be shutdown first.
     */
    HIGHEST(3),

    /**
     * DEFAULT is the priority pre-defined in the system. Components which are
     * registered with this priority will be shutdown after the componets with
     * HIGHEST priority.
     */
    DEFAULT(2),

    /**
     * LOWEST is the priority pre-defined in the system. Components which are
     * registered with this priority will be shutdown after the componets with
     * HIGHEST or DEFAULT priority.
     */
    LOWEST(1);

    private int priority;

    private ShutdownPriority(int priority) {
        this.priority = priority;
    }

    /**
     * Returns the priority.
     *
     * @return the priority.
     */
    public int getIntValue() {
        return priority;
    }

    /**
     * Returns list of all the priorities (ordered from the highest to the
     * lowest priority) defined in the system.
     *
     * @return list of all the priorities defined in the system.
     */
    public static List<ShutdownPriority> getPriorities() {
        return Arrays.asList(values());
    }
}
