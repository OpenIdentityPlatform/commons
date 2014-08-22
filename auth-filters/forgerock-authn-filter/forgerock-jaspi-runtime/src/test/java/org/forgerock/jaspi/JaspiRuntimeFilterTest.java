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

package org.forgerock.jaspi;

import org.forgerock.jaspi.runtime.AuditApi;
import org.forgerock.jaspi.runtime.ContextFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

public class JaspiRuntimeFilterTest {

    private JaspiRuntimeFilter jaspiRuntimeFilter;

    @BeforeMethod
    public void setUp() throws ServletException {

        ContextFactory contextFactory = mock(ContextFactory.class);
        AuditApi auditApi = mock(AuditApi.class);

        jaspiRuntimeFilter = new JaspiRuntimeFilter(contextFactory, auditApi);

        FilterConfig filterConfig = mock(FilterConfig.class);
        jaspiRuntimeFilter.init(filterConfig);
    }

    @Test
    public void shouldCreateJaspiRuntimeFilterWithDefaultConstructor() {

        //Given

        //When
        JaspiRuntimeFilter runtimeFilter = new JaspiRuntimeFilter();

        //Then
        assertNotNull(runtimeFilter);
    }

    @Test (expectedExceptions = ServletException.class)
    public void shouldThrowServletExceptionWhenNotHttpServletRequest() throws ServletException, IOException {

        //Given
        ServletRequest request = mock(ServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        //When
        jaspiRuntimeFilter.doFilter(request, response, filterChain);

        //Then
        fail();
    }

    @Test (expectedExceptions = ServletException.class)
    public void shouldThrowServletExceptionWhenNotHttpServletResponse() throws ServletException, IOException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        //When
        jaspiRuntimeFilter.doFilter(request, response, filterChain);

        //Then
        fail();
    }

    @Test
    public void shouldDestroyFilter() throws ServletException, IOException {

        //Given

        //When
        jaspiRuntimeFilter.destroy();

        //Then
    }
}
