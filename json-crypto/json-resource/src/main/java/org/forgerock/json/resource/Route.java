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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.json.resource;

/**
 * An opaque handle for a route which has been registered in a {@link UriRouter uriRouter} or
 * {@link VersionRouter versionRouter}. A reference to a route should be maintained if there is a chance that the route
 * will need to be removed from the router at a later time.
 *
 * @see UriRouter
 * @see VersionRouter
 */
public interface Route {
}
