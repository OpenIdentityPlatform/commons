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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.services.routing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import org.forgerock.http.ApiProducer;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DelegatingRouteMatcherTest {

    @Mock
    private RouteMatcher<String> delegate;
    @Mock
    private ApiProducer<String> apiProducer;
    private DelegatingRouteMatcher<String> matcher;

    @BeforeMethod
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        matcher = new DelegatingRouteMatcher<>(delegate);
    }

    @Test
    public void testEvaluate() throws Exception {
        // Given
        RouteMatch match = mock(RouteMatch.class);
        given(delegate.evaluate(any(Context.class), anyString())).willReturn(match);
        // When
        RouteMatch result = matcher.evaluate(new RootContext(), "A string");

        // Then
        verify(delegate).evaluate(isA(RootContext.class), eq("A string"));
        assertThat(result).isSameAs(match);
    }

    @Test
    public void testToString() throws Exception {
        given(delegate.toString()).willReturn("the string");
        assertThat(matcher.toString()).isEqualTo("the string");
    }

    @Test
    public void testIdFragment() throws Exception {
        given(delegate.idFragment()).willReturn("the fragment");
        assertThat(matcher.idFragment()).isEqualTo("the fragment");
    }

    @Test
    public void testTransformApi() throws Exception {
        // Given
        given(delegate.transformApi(anyString(), eq(apiProducer))).willReturn("A result");

        // When
        String result = matcher.transformApi("A string", apiProducer);

        // Then
        verify(delegate).transformApi("A string", apiProducer);
        assertThat(result).isEqualTo("A result");
    }

}