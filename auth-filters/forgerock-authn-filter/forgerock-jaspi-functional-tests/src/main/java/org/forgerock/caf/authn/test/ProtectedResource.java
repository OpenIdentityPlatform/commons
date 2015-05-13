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

package org.forgerock.caf.authn.test;

import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

import java.util.Map;

import org.forgerock.caf.authentication.framework.AuthenticationFramework;
import org.forgerock.http.Context;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpContext;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * A protected resource which will set a header on the response to signify that it has been called and write a JSON
 * string to the response containing the principal and context from the request attributes.
 *
 * @since 1.5.0
 */
public class ProtectedResource implements Handler {

    private static final long serialVersionUID = 1L;

    /**
     * Response header name to signify that the resource has been called.
     */
    public static final String RESOURCE_CALLED_HEADER = "RESOURCE_CALLED";

    /**
     * Sets a header, "RESOURCE_CALLED:true" on the response to signify that it has been called and write a JSON
     * string to the response containing the principal and context from the request attributes.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     */
    @Override
    public Promise<Response, NeverThrowsException> handle(Context context, Request request) {

        Response response = new Response().setStatus(Status.OK);

        response.getHeaders().putSingle(RESOURCE_CALLED_HEADER, "true");

        String principal = (String) context.asContext(HttpContext.class).getAttributes().get(AuthenticationFramework.ATTRIBUTE_AUTH_PRINCIPAL);
        Map<String, Object> requestContextMap = (Map<String, Object>) context.asContext(HttpContext.class).getAttributes().get(AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT);

        JsonValue json = json(object());
        json.put("data", "RESOURCE_DATA");
        if (principal != null) {
            json.add("principal", principal);
        }
        if (requestContextMap != null) {
            json.add("context", requestContextMap);
        }
        response.setEntity(json.getObject());
        return Promises.newResultPromise(response);
    }
}
