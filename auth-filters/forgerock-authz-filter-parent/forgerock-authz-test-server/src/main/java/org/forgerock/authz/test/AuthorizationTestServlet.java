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
 * Copyright 2014 ForgeRock, AS.
 */

package org.forgerock.authz.test;

import org.forgerock.authz.AuthorizationContext;
import org.forgerock.json.fluent.JsonValue;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * A simple servlet resource that can be used to test authorization filters. The servlet returns any authorization
 * context attributes that have been set on the session as a JSON object.
 *
 * @since 1.4.0
 */
@SuppressWarnings("serial") // HttpServlet implements Serializable for unknown reasons
public class AuthorizationTestServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {

        // Check if there is any authz context present on the request
        AuthorizationContext context = AuthorizationContext.forRequest(request);

        JsonValue json = JsonValue.json(context.getAttributes());

        // Note: JsonValue.toString() may not produce valid JSON, but it should be sufficient for testing.
        response.setContentType("application/json");
        response.getWriter().write(json.toString());
        response.getWriter().close();
    }
}
