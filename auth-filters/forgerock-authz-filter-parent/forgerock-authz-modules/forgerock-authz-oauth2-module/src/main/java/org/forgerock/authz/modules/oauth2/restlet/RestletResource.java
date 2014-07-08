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

package org.forgerock.authz.modules.oauth2.restlet;

import org.forgerock.authz.modules.oauth2.RestResource;
import org.forgerock.authz.modules.oauth2.RestResourceException;
import org.forgerock.json.fluent.JsonValue;
import org.restlet.engine.header.Header;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

import java.io.IOException;

/**
 * Simple wrapper around Restlet's {@link org.restlet.resource.ClientResource}.
 *
 * @since 1.5.0
 */
public class RestletResource implements RestResource {

    private final JsonParser parser;
    private final ClientResource resource;

    /**
     * Constructs a new RestletResource for the provided uri.
     *
     * @param parser JSON parser to use for parsing the response.
     * @param uri The uri of the resource.
     */
    public RestletResource(JsonParser parser, String uri) {
        this.parser = parser;
        resource = new ClientResource(uri);
    }

    /**
     * Gets the resource.
     * <br>
     * If a success status is not returned, then an exception is thrown.
     *
     * @return The JSON response content.
     * @throws RestResourceException If the response from the request is unsuccessful.
     */
    @Override public JsonValue get() throws RestResourceException {
        try {
            final Representation representation = resource.get();
            return parser.parse(representation.getText());
        } catch (ResourceException e) {
            if (e.getStatus() == null) {
                throw new RestResourceException("Error whilst getting the resource", e);
            }
            throw new RestResourceException("Response from the request is unsuccessful",
                                            e,
                                            e.getStatus().getCode());
        } catch (IOException e) {
            throw new RestResourceException("Cannot parse the response content as Json", e);
        }
    }

    /**
     * Adds a header onto the request for the resource.
     *
     * @param name The header name.
     * @param value The header value.
     */
    @Override@SuppressWarnings("unchecked")
    public void addHeader(String name, String value) {
        Series<Header> headers = (Series<Header>) resource.getRequestAttributes().get("org.restlet.http.headers");
        if (headers == null) {
            headers = new Series<Header>(Header.class);
            resource.getRequestAttributes().put("org.restlet.http.headers", headers);
        }
        headers.set(name, value);
    }
}
