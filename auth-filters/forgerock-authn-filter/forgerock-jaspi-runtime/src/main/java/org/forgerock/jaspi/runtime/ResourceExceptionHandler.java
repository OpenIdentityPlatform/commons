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

package org.forgerock.jaspi.runtime;

import org.forgerock.json.resource.ResourceException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;

/**
 * Handle ResourceException responses for different media types. Implementations should be thread-safe.
 */
public interface ResourceExceptionHandler {

    /**
     * Can this implementation handle a specific request. Implementations will want to check headers such as
     * {@code Accept} and {@code Content-Type}.
     * @param request The current request.
     * @return If the implementation can give a response to the request.
     */
    boolean canHandle(HttpServletRequest request);

    /**
     * Write the details of the exception out, and set the content type of the response.
     * @param exception The exception to be written.
     * @param response The response to write the details to.
     * @throws java.io.IOException In the event of writing failure.
     */
    void write(ResourceException exception, HttpServletResponse response) throws IOException;

}
