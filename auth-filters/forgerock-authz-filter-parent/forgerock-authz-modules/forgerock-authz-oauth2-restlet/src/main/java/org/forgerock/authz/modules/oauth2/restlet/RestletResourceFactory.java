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
import org.forgerock.authz.modules.oauth2.RestResourceFactory;

/**
 * Simple factory class for creating new {@link RestletResource} instance.
 *
 * @since 1.5.0
 * @see RestletResource
 */
public class RestletResourceFactory implements RestResourceFactory {

    private final JsonParser parser;

    /**
     * Builds a new RestletResourceFactory.
     */
    public RestletResourceFactory() {
        this(new JsonParser());
    }

    /**
     * Builds a new RestletResourceFactory using the given JSON parser.
     *
     * @param parser
     *         used to parse the responses' content
     */
    public RestletResourceFactory(JsonParser parser) {
        this.parser = parser;
    }

    @Override public RestResource resource(String uri) {
        return new RestletResource(parser, uri);
    }
}
