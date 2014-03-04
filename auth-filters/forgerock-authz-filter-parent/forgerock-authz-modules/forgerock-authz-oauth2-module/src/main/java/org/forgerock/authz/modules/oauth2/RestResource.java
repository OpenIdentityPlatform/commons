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

package org.forgerock.authz.modules.oauth2;

import org.restlet.engine.header.Header;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

/**
 * Simple wrapper around Restlet's {@link ClientResource}, to help facilitate testing.
 *
 * @since 1.4.0
 */
public class RestResource {

    private final ClientResource resource;

    /**
     * Constructs a new RestResource for the provided uri.
     *
     * @param uri The uri of the resource.
     */
    public RestResource(final String uri) {
        resource = new ClientResource(uri);
    }

    /**
     * Gets the resource.
     * <br>
     * If a success status is not returned, then a resource exception is thrown.
     *
     * @return The best representation.
     * @throws ResourceException If the response from the request is unsuccessful.
     */
    public Representation get() {
        return resource.get();
    }

    /**
     * Adds a header onto the request for the resource.
     *
     * @param name The header name.
     * @param value The header value.
     */
    @SuppressWarnings("unchecked")
    public void addHeader(final String name, final String value) {
        Series<Header> headers = (Series<Header>) resource.getRequestAttributes().get("org.restlet.http.headers");
        if (headers == null) {
            headers = new Series<Header>(Header.class);
            resource.getRequestAttributes().put("org.restlet.http.headers", headers);
        }
        headers.set(name, value);
    }
}
