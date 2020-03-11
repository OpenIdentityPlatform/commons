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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.authz.filter.api;

import org.testng.annotations.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

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
        Integer value = 1234;
        context.setAttribute(key, value);

        // When
        Integer result = context.getAttribute(key);

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

        Map<String, Object> expected = new LinkedHashMap<>();
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
    public void shouldEqualSameObjectReference() {

        //Given
        AuthorizationContext contextOne = new AuthorizationContext();
        AuthorizationContext contextTwo = contextOne;

        //When
        boolean equals = contextOne.equals(contextTwo);

        //Then
        assertTrue(equals);
    }

    @Test
    public void shouldNotEqualDifferentObjectReference() {

        //Given
        AuthorizationContext contextOne = new AuthorizationContext();
        AuthorizationContext contextTwo = new AuthorizationContext();
        contextTwo.setAttribute("KEY", "VALUE");

        //When
        boolean equals = contextOne.equals(contextTwo);

        //Then
        assertFalse(equals);
    }

    @Test
    public void shouldNotEqualDifferentObjectType() {

        //Given
        AuthorizationContext contextOne = new AuthorizationContext();
        Object contextTwo = new Object();

        //When
        boolean equals = contextOne.equals(contextTwo);

        //Then
        assertFalse(equals);
    }

    @Test
    public void shouldEqualWithSameAttributeContents() {

        //Given
        AuthorizationContext contextOne = new AuthorizationContext();
        AuthorizationContext contextTwo = new AuthorizationContext();

        //When
        boolean equals = contextOne.equals(contextTwo);

        //Then
        assertTrue(equals);
    }

    @Test
    public void shouldGetHashCode() {

        //Given
        Map<String, Object> attributes = new HashMap<>();
        AuthorizationContext context = new AuthorizationContext(attributes);

        attributes.put("KEY", "VALUE");

        //When
        int hashCode = context.hashCode();

        //Then
        assertEquals(hashCode, attributes.hashCode());
    }
}
