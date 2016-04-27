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

package org.forgerock.json.resource;

import org.forgerock.http.routing.Version;

/**
 * Common response object of all resource responses.
 */
public interface Response {

    /**
     * Sets the API version of the resource that the request was routed to.
     *
     * @param version The resource API version.
     */
    void setResourceApiVersion(Version version);

    /**
     * Gets the API version of the resource that the request was routed to.
     *
     * @return The resource API version.
     */
    Version getResourceApiVersion();
}
