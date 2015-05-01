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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.caf.authentication.framework;

import java.util.Collection;

import org.forgerock.guava.common.net.MediaType;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.resource.ResourceException;

/**
 * Handle ResourceException responses for different media types. Implementations should be thread-safe.
 */
public interface ResourceExceptionHandler {

    /**
     * Which media types can this handler handle.
     *
     * @return A list of media types.
     */
    Collection<MediaType> handles();

    /**
     * Write the details of the exception out, and set the content type of the response.
     *
     * @param exception The exception to be written.
     * @param response The response to write the details to.
     */
    void write(ResourceException exception, Response response);

    /**
     * A short but useful description of this response handler. Description should include the
     * media type this response handler handles.
     */
    @Override
    String toString();
}
