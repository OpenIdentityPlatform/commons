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

package org.forgerock.jaspi.modules.session.openam;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;

import java.util.Map;

/**
 * Simple REST Client which can be configured to use SSL and make HTTPS REST calls.
 *
 * @since 1.4.0
 */
interface RestClient {

    /**
     * Sets the SslConfiguration for the REST client to use HTTPS when making REST calls.
     *
     * @param sslConfiguration The SslConfiguration.
     */
    void setSslConfiguration(final SslConfiguration sslConfiguration);

    /**
     * Makes a REST POST call to the specified URI, with the specified query parameters.
     *
     * @param uri The URI of the resource to make the REST call to.
     * @param queryParameters The query parameters to set on the REST request.
     * @return The JsonValue response of the REST call.
     * @throws ResourceException If there is any problem making the REST call.
     */
    JsonValue post(final String uri, final Map<String, String> queryParameters) throws ResourceException;
}
