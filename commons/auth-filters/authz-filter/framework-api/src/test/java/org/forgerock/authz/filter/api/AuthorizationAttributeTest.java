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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Unit tests for {@link AuthorizationAttribute}.
 */
public class AuthorizationAttributeTest {

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullKeys() {
        new AuthorizationAttribute<>(null);
    }

    @Test
    public void shouldGetCorrectAttribute() {
        // Given
        AuthorizationContext context = mock(AuthorizationContext.class);
        String key = "abc";
        AuthorizationAttribute<String> attribute = new AuthorizationAttribute<>(key);

        // When
        attribute.get(context);

        // Then
        verify(context).getAttribute(key);
    }

    @Test
    public void shouldSetCorrectAttribute() {
        // Given
        AuthorizationContext context = mock(AuthorizationContext.class);
        String key = "abc";
        int value = 123;
        AuthorizationAttribute<Integer> attribute = new AuthorizationAttribute<>(key);

        // When
        attribute.set(context, value);

        // Then
        verify(context).setAttribute(key, value);
    }

    @Test
    public void shouldEqualSameObjectReference() {

        //Given
        AuthorizationAttribute<String> attributeOne = new AuthorizationAttribute<>("KEY");
        AuthorizationAttribute<String> attributeTwo = attributeOne;

        //When
        boolean equals = attributeOne.equals(attributeTwo);

        //Then
        assertTrue(equals);
    }

    @Test
    public void shouldNotEqualDifferentObjectType() {

        //Given
        AuthorizationAttribute<String> attributeOne = new AuthorizationAttribute<>("KEY");
        Object attributeTwo = new Object();

        //When
        boolean equals = attributeOne.equals(attributeTwo);

        //Then
        assertFalse(equals);
    }

    @Test
    public void shouldEqualWithSameAttribute() {

        //Given
        AuthorizationAttribute<String> attributeOne = new AuthorizationAttribute<>("KEY");
        AuthorizationAttribute<String> attributeTwo = new AuthorizationAttribute<>("KEY");

        //When
        boolean equals = attributeOne.equals(attributeTwo);

        //Then
        assertTrue(equals);
    }

    @Test
    public void shouldNotEqualDifferentObjectReference() {

        //Given
        AuthorizationAttribute<String> attributeOne = new AuthorizationAttribute<>("KEY");
        AuthorizationAttribute<String> attributeTwo = new AuthorizationAttribute<>("KEY_TWO");

        //When
        boolean equals = attributeOne.equals(attributeTwo);

        //Then
        assertFalse(equals);
    }

    @Test
    public void shouldGetHashCode() {

        //Given

        //When
        AuthorizationAttribute<String> authorizationAttribute = new AuthorizationAttribute<>("KEY");

        //Then
        assertEquals(authorizationAttribute.hashCode(), "KEY".hashCode());
    }

    @Test
    public void shouldToString() {

        //Given

        //When
        AuthorizationAttribute<String> authorizationAttribute = new AuthorizationAttribute<>("KEY");

        //Then
        assertTrue(authorizationAttribute.toString().contains("KEY"));
    }
}
