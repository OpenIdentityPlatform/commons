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

package org.forgerock.guice.core;

import com.google.inject.Stage;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class GuiceInitialisationFilterTest {

    private GuiceInitialisationFilter initialisationFilter;

    @BeforeClass
    public void setUp() {
        initialisationFilter = new GuiceInitialisationFilter();
    }

    @Test
    public void shouldInitializeContext() {

        //Given
        ServletContextEvent servletContextEvent = mock(ServletContextEvent.class);
        ServletContext servletContext = mock(ServletContext.class);
        given(servletContextEvent.getServletContext()).willReturn(servletContext);

        //When
        initialisationFilter.contextInitialized(servletContextEvent);

        //Then
        assertThat(InjectorConfiguration.getStage()).isEqualTo(Stage.PRODUCTION);
    }

    @Test
    public void shouldInitializeContextWithStage() {

        //Given
        ServletContextEvent servletContextEvent = mock(ServletContextEvent.class);
        ServletContext servletContext = mock(ServletContext.class);
        given(servletContextEvent.getServletContext()).willReturn(servletContext);
        given(servletContext.getInitParameter(Stage.class.getCanonicalName())).willReturn("DEVELOPMENT");

        //When
        initialisationFilter.contextInitialized(servletContextEvent);

        //Then
        assertThat(InjectorConfiguration.getStage()).isEqualTo(Stage.DEVELOPMENT);
    }

    @Test
    public void shouldDestroyContext() {

        //Given
        ServletContextEvent servletContextEvent = mock(ServletContextEvent.class);

        //When
        initialisationFilter.contextDestroyed(servletContextEvent);

        //Then
        //Just getting here is a pass
    }
}
