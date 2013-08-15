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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.jaspi.container;

import org.forgerock.jaspi.filter.AuthNFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * HttpServletRequestWrapper which adds the authenticated principal and authentication context to the HttpRequest.
 *
 * @author Phill Cunnington
 */
public class AuthHttpServletRequestWrapper extends HttpServletRequestWrapper {

    private final String principal;

    /**
     * Constructs a new AuthHttpServletRequestWrapper.
     *
     * @param request The original HttpServletRequest.
     * @param principal The authenticated principal.
     * @param context The authentication context.
     */
    public AuthHttpServletRequestWrapper(HttpServletRequest request, String principal, Map<String, Object> context) {
        super(request);
        request.setAttribute(AuthNFilter.ATTRIBUTE_AUTH_CONTEXT, context);
        this.principal = principal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHeader(String name) {
        if (AuthNFilter.ATTRIBUTE_AUTH_PRINCIPAL.equalsIgnoreCase(name)) {
            return principal;
        }
        return super.getHeader(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Enumeration getHeaderNames() {
        List<String> names = Collections.list(super.getHeaderNames());
        names.add(AuthNFilter.ATTRIBUTE_AUTH_PRINCIPAL);
        return Collections.enumeration(names);
    }
}
