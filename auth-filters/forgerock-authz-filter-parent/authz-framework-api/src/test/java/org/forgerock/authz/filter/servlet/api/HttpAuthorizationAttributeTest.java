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

package org.forgerock.authz.filter.servlet.api;

import org.forgerock.authz.filter.api.AuthorizationAttribute;
import org.forgerock.authz.filter.api.AuthorizationContext;
import org.testng.annotations.Test;

import javax.servlet.ServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;

/**
 * Unit tests for {@link org.forgerock.authz.filter.api.AuthorizationAttribute}.
 */
public class HttpAuthorizationAttributeTest {

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullKeys() {
        new AuthorizationAttribute<Object>(null);
    }

    @Test
    public void shouldGetCorrectAttribute() {
        // Given
        AuthorizationContext context = mock(AuthorizationContext.class);
        String key = "abc";
        HttpAuthorizationAttribute<String> attribute = new HttpAuthorizationAttribute<String>(key);

        // When
        attribute.get(context);

        // Then
        verify(context).getAttribute(key);
    }

    @Test
    public void shouldUseAssociatedContext() {
        // Given
        ServletRequest request = mock(ServletRequest.class);
        Map<String, Object> context = new LinkedHashMap<String, Object>();
        String value = "123";
        String key = "abc";
        context.put(key, value);
        HttpAuthorizationAttribute<String> attribute = new HttpAuthorizationAttribute<String>(key);
        given(request.getAttribute(HttpAuthorizationContext.ATTRIBUTE_AUTHORIZATION_CONTEXT)).willReturn(context);

        // When
        String result = attribute.get(request);

        // Then
        assertEquals(result, value);
    }

    @Test
    public void shouldSetCorrectAttribute() {
        // Given
        AuthorizationContext context = mock(AuthorizationContext.class);
        String key = "abc";
        int value = 123;
        HttpAuthorizationAttribute<Integer> attribute = new HttpAuthorizationAttribute<Integer>(key);

        // When
        attribute.set(context, value);

        // Then
        verify(context).setAttribute(key, value);
    }

    @Test
    public void shouldSetAttributeInCorrectContext() {
        // Given
        ServletRequest request = mock(ServletRequest.class);
        String value = "123";
        String key = "abc";
        HttpAuthorizationAttribute<String> attribute = new HttpAuthorizationAttribute<String>(key);
        Map<String, Object> context = new LinkedHashMap<String, Object>();
        given(request.getAttribute(HttpAuthorizationContext.ATTRIBUTE_AUTHORIZATION_CONTEXT)).willReturn(context);

        // When
        attribute.set(request, value);

        // Then
        assertEquals(context, Collections.singletonMap(key, value));
    }
}
