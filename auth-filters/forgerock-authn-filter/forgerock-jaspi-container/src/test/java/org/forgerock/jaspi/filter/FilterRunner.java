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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.jaspi.filter;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class FilterRunner {

    private final AuthNFilter authFilter = new AuthNFilter();
    private final FilterConfig filterConfig;
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final FilterChain filterChain;
    private final PrintWriter responseWriter;

    public FilterRunner() {
        filterConfig = mock(FilterConfig.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        responseWriter = mock(PrintWriter.class);
    }

    public void run() throws IOException, ServletException {
        run(null);
    }

    public void run(String moduleConfigurationValue) throws IOException, ServletException {

        given(request.getRequestURL()).willReturn(new StringBuffer("http://localhost:8080/jaspi/resource.jsp"));
        given(request.getContextPath()).willReturn("CONTEXT_PATH");
        given(filterConfig.getInitParameter(AuthNFilter.MODULE_CONFIGURATION_PROPERTY))
                .willReturn(moduleConfigurationValue);
        given(response.getWriter()).willReturn(responseWriter);

        authFilter.init(filterConfig);
        authFilter.doFilter(request, response, filterChain);
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public FilterChain getFilterChain() {
        return filterChain;
    }
}
