/*
 * Copyright 2014 ForgeRock, AS.
 *
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
 */

package org.forgerock.authz;

import org.testng.annotations.Test;

import javax.servlet.ServletRequest;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * Unit tests for {@link AuthorizationContext}.
 *
 * @since 1.4.0
 */
public class AuthorizationContextTest {

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullKeysForGettingAttributes() {
        // Given
        AuthorizationContext context = new AuthorizationContext();

        // When
        context.getAttribute(null);
    }

    @Test
    public void shouldReturnNullForMissingAttribute() {
        // Given
        AuthorizationContext context = new AuthorizationContext();

        // When
        String result = context.getAttribute("nothing");

        // Then
        assertNull(result);
    }

    @Test
    public void shouldReturnCorrectValueWhenPresent() {
        // Given
        AuthorizationContext context = new AuthorizationContext();
        String key = "test";
        int value = 1234;
        context.setAttribute(key, value);

        // When
        int result = context.getAttribute(key);

        // Then
        assertEquals(result, value);
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void shouldFailFastForIncorrectTypeConversions() {
        // Given
        AuthorizationContext context = new AuthorizationContext();
        String key = "test";
        context.setAttribute(key, 1234);

        // When
        Date d = context.getAttribute(key);

        // Then - exception
        // Note: we could return null for mismatched types, but this is probably programmer error so fail fast.
    }

    @Test
    public void shouldReturnAllAttributesCorrectly() {
        // Given
        AuthorizationContext context = new AuthorizationContext();
        context.setAttribute("one", "one")
               .setAttribute("two", 2)
               .setAttribute("three", 3.0);

        Map<String, Object> expected = new LinkedHashMap<String, Object>();
        expected.put("one", "one");
        expected.put("two", 2);
        expected.put("three", 3.0);

        // When
        Map<String, Object> result = context.getAttributes();

        // Then
        assertEquals(result, expected);
    }

    @Test
    public void shouldReturnDefensiveCopiesOfAttributes() {
        // Given
        AuthorizationContext context = new AuthorizationContext();
        context.setAttribute("one", "one");

        // When
        Map<String, Object> result = context.getAttributes();
        result.put("two", 2);
        result.remove("one");

        // Then
        assertEquals(context.getAttribute("one"), "one"); // Still present
        assertNull(context.getAttribute("two")); // Not added
    }

    @Test
    public void shouldPropagateUpdatesToTheRequestMap() {
        // Given
        ServletRequest request = mock(ServletRequest.class);
        Map<String, Object> contextMap = new LinkedHashMap<String, Object>();
        given(request.getAttribute(AuthorizationContext.ATTRIBUTE_AUTHORIZATION_CONTEXT)).willReturn(contextMap);

        // When
        AuthorizationContext context = AuthorizationContext.forRequest(request);
        context.setAttribute("one", 2);

        // Then the map in the request should also be updated
        assertEquals(contextMap, Collections.singletonMap("one", 2));
    }

}
