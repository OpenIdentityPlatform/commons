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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.jaspi.test.server.endpoint;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ResourceException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public class EndpointUtils {

    private static final String PARAM_PRETTY_PRINT = "_prettyPrint";
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    static JsonGenerator getJsonGenerator(final HttpServletRequest req,
            final HttpServletResponse resp) throws IOException {
        final JsonGenerator writer =
                JSON_MAPPER.getJsonFactory().createJsonGenerator(resp.getOutputStream());
        writer.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);

        // Enable pretty printer if requested.
        final String[] values = getParameter(req, PARAM_PRETTY_PRINT);
        if (values != null) {
            try {
                if (asBooleanValue(PARAM_PRETTY_PRINT, values)) {
                    writer.useDefaultPrettyPrinter();
                }
            } catch (final ResourceException e) {
                // Ignore because we may be trying to obtain a generator in
                // order to output an error.
            }
        }
        return writer;
    }

    @SuppressWarnings("unchecked")
    private static String[] getParameter(final HttpServletRequest req, final String parameter) {
        // Need to do case-insensitive matching.
        Map<String, String[]> parameterMap = (Map<String, String[]>) req.getParameterMap();
        for (final Map.Entry<String, String[]> p : parameterMap.entrySet()) {
            if (p.getKey().equalsIgnoreCase(parameter)) {
                return p.getValue();
            }
        }
        return null;
    }

    private static boolean asBooleanValue(final String name, final String[] values)
            throws ResourceException {
        final String value = asSingleValue(name, values);
        return Boolean.parseBoolean(value);
    }

    private static String asSingleValue(final String name, final String[] values) throws ResourceException {
        if (values == null || values.length == 0) {
            // FIXME: i18n.
            throw new BadRequestException("No values provided for the request parameter \'" + name
                    + "\'");
        } else if (values.length > 1) {
            // FIXME: i18n.
            throw new BadRequestException(
                    "Multiple values provided for the single-valued request parameter \'" + name
                            + "\'");
        }
        return values[0];
    }
}
