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

package org.forgerock.caf.authentication.api;

import java.util.Map;

import org.forgerock.services.context.Context;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;

/**
 * <p>The authentication framework uses this {@code MessageContextInfo} to pass messages and
 * message processing state to authentication modules for processing of messages.</p>
 *
 * <p>This class encapsulates a request and response message objects for a message exchange. This
 * class may also be used to associate additional context in the form of key/value pairs, with the
 * encapsulated messages.</p>
 *
 * @see javax.security.auth.message.MessageInfo
 *
 * @since 2.0.0
 */
public interface MessageInfoContext extends Context {

    /**
     * Gets the request object from this {@code MessageContextInfo}.
     *
     * @return The {@code Request} object.
     */
    Request getRequest();

    /**
     * Gets the response object from this {@code MessageContextInfo}.
     *
     * @return The {@code Response} object.
     */
    Response getResponse();

    /**
     * Sets the request object for this {@code MessageContextInfo}.
     *
     * @param request The {@code Request} object.
     */
    void setRequest(Request request);

    /**
     * Sets the response object for this {@code MessageContextInfo}.
     *
     * @param response The {@code Response} object.
     */
    void setResponse(Response response);

    /**
     * <p>Gets the context map for this message exchange.</p>
     *
     * <p>This {@code Map} can contain shared information between
     * {@link org.forgerock.caf.authentication.api.AsyncServerAuthContext}s and
     * {@link org.forgerock.caf.authentication.api.AsyncServerAuthModule}s which will be maintained
     * for a single request.</p>
     *
     * @return The request context map.
     */
    Map<String, Object> getRequestContextMap();
}
