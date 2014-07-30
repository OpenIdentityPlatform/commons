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

package org.forgerock.caf.authn.test;

import org.forgerock.jaspi.runtime.JaspiRuntime;
import org.forgerock.json.fluent.JsonValue;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * A protected resource which will set a header on the response to signify that it has been called and write a JSON
 * string to the response containing the principal and context from the request attributes.
 *
 * @since 1.5.0
 */
public class ProtectedResource extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * Response header name to signify that the resource has been called.
     */
    public static final String RESOURCE_CALLED_HEADER = "RESOURCE_CALLED";

    /**
     * Sets a header, "RESOURCE_CALLED:true" on the response to signify that it has been called and write a JSON
     * string to the response containing the principal and context from the request attributes.
     *
     * @param req {@inheritDoc}
     * @param resp {@inheritDoc}
     * @throws ServletException {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader(RESOURCE_CALLED_HEADER, "true");

        String principal = (String) req.getAttribute(JaspiRuntime.ATTRIBUTE_AUTH_PRINCIPAL);
        Map<String, Object> context = (Map<String, Object>) req.getAttribute(JaspiRuntime.ATTRIBUTE_AUTH_CONTEXT);

        JsonValue json = json(object());
        json.put("data", "RESOURCE_DATA");
        if (principal != null) {
            json.add("principal", principal);
        }
        if (context != null) {
            json.add("context", context);
        }

        resp.getWriter().write(json.toString());
    }
}
