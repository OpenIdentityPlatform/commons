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

package org.forgerock.json.resource;

/**
 * Declare a CREST Application. This interface binds together the CREST {@link ConnectionFactory} that will
 * be used to connect to the API, and the API ID and Version that will be used when describing it.
 */
public interface CrestApplication {
    /**
     * Get the connection factory for the application.
     * @return The factory.
     */
    ConnectionFactory getConnectionFactory();

    /**
     * Get the API ID, that will be used in the {@link org.forgerock.api.models.ApiDescription}.
     * @return The ID.
     */
    String getApiId();

    /**
     * Get the API Version, that will be used in the {@link org.forgerock.api.models.ApiDescription}.
     * @return The Version.
     */
    String getApiVersion();
}
