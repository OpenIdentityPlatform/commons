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

package org.forgerock.http.routing;

/**
 * The default routing behaviour to use when no {@literal Accept-API-Version}
 * is set on the request.
 */
public enum DefaultVersionBehaviour {

    /**
     * Will route to the latest version of the resource.
     */
    LATEST,
    /**
     * Will route to the oldest version of the resource.
     */
    OLDEST,
    /**
     * Will not attempt to route to any version of the resource, instead will
     * return an error to the client.
     */
    NONE
}
