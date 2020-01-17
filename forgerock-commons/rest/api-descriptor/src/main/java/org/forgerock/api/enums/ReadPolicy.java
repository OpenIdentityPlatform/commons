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

import org.forgerock.api.models.Schema;

/**
 * Enum that represents the {@link Schema} read policies.
 */
public enum ReadPolicy {
    /**
     * Property is readable via client APIs and <em>visible</em> in the user-interface.
     */
    USER,
    /**
     * Property is hidden from user-interface, but readable via client APIs. This is the default value if
     * {@code ReadPolicy} is not specified.
     */
    CLIENT,
    /**
     * Property is available internally, but not exposed to client APIs. Applications must actively filter this
     * value from client API responses.
     */
    SERVER
}
