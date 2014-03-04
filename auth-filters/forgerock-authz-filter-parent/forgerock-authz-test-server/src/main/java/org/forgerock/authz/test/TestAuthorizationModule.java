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
import org.forgerock.authz.AuthorizationModule;
import org.forgerock.json.fluent.JsonValue;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

/**
 * A basic authorization module that copies all request parameters into the authorization context. It authorizes
 * requests based on whether there is a magic parameter present and with the correct magic value.
 *
 * @since 12.0.0
 */
public class TestAuthorizationModule implements AuthorizationModule {
    private static final String DEFAULT_MAGIC_PARAM = "letmein";
    private static final String DEFAULT_MAGIC_VALUE = "please";

    /** Magic Param Name. */
    public static final String CONFIG_MAGIC_PARAM_NAME = "magicParamName";
    /** Magic Param Value. */
    public static final String CONFIG_MAGIC_PARAM_VALUE = "magicParamValue";

    private String magicParam;
    private String magicValue;

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialise(JsonValue config) {
        if (config != null && config.isMap()) {
            this.magicParam = config.asMap().containsKey(CONFIG_MAGIC_PARAM_NAME)
                                    ? config.asMap().get(CONFIG_MAGIC_PARAM_NAME).toString()
                                    : DEFAULT_MAGIC_PARAM;
            this.magicValue = config.asMap().containsKey(CONFIG_MAGIC_PARAM_VALUE)
                                    ? config.asMap().get(CONFIG_MAGIC_PARAM_VALUE).toString()
                                    : DEFAULT_MAGIC_VALUE;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean authorize(HttpServletRequest servletRequest, AuthorizationContext context) {

        // Check that the magic parameter is present in the request
        boolean allowed = magicValue.equalsIgnoreCase(servletRequest.getParameter(magicParam));

        // Copy all request parameters into the authorization context
        @SuppressWarnings("unchecked")
        Enumeration<String> paramNames = servletRequest.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String param = paramNames.nextElement();
            String[] values = servletRequest.getParameterValues(param);
            List<String> paramValues = new ArrayList<String>(Arrays.asList(values));
            context.setAttribute(param, paramValues);
        }

        return allowed;
    }
}
