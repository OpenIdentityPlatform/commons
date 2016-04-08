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

package org.forgerock.api.enums;

/**
 * Represents API stability.
 */
public enum Stability {
    /**
     * Stable API (default), suggested for use by all clients.
     */
    STABLE,
    /**
     * Internal API that may change or be removed at any time.
     */
    INTERNAL,
    /**
     * Evolving API that may changed at any time.
     */
    EVOLVING,
    /**
     * Deprecated API, that should not be used and may be removed in the future.
     */
    DEPRECATED,
    /**
     * Removed API, that remains available for some technical reason and may return an error if used.
     */
    REMOVED;
}
