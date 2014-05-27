/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.forgerock.util.promise.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;

import static org.forgerock.util.Iterables.filter;
import static org.forgerock.util.Iterables.map;


/**
 * Test the Iterables methods.
 */
public class IterablesTest {

    private static final Predicate<String> IGNORE_G_WORDS =
            new Predicate<String>() {
                @Override
                public boolean apply(String s) {
                    return !s.toLowerCase().startsWith("g");
                }
            };

    private static final Predicate<String> ALWAYS_TRUE =
            new Predicate<String>() {
                @Override
                public boolean apply(String s) {
                    return true;
                }
            };

    private static final Predicate<String> ALWAYS_FALSE =
            new Predicate<String>() {
                @Override
                public boolean apply(String s) {
                    return false;
                }
            };

    @DataProvider
    public Object[][] filterCollections() {
        return new Object[][] {
                // @formatter:off
                { new String[] { "goo", "gah", "boo", "bah", "zoo" }, "bbz" },
                { new String[] { "boo", "baz", "goo", "gah", "zoo" }, "bbz" },
                { new String[] { "boo", "goo", "bah", "zoo", "gah" }, "bbz" }

                // @formatter:on
        };
    }

    @Test(dataProvider = "filterCollections")
    public void testFilter(final String[] value, final String firstLetters) {
        StringBuilder sb = new StringBuilder();
        for (String element : filter(Arrays.asList(value), IGNORE_G_WORDS)) {
            sb.append(element.charAt(0));
        }
        assertThat(sb.toString()).isEqualTo(firstLetters);
    }

    @Test
    public void testNoElementsHasNext()
    {
        assertThat(filter(new ArrayList<String>(), ALWAYS_TRUE).iterator().hasNext()).isFalse();
    }

    @Test(expectedExceptions = NoSuchElementException.class)
    public void testNoElementsNext()
    {
        filter(new ArrayList<String>(), ALWAYS_TRUE).iterator().next();
    }

    @Test
    public void testNextWithoutHasNextTruePredicate()
    {
        assertThat(filter(Arrays.asList(new String[] { "a" }), ALWAYS_TRUE).iterator().next()).isEqualTo("a");
    }

    @Test(expectedExceptions = NoSuchElementException.class)
    public void testNextWithoutHasNextFalsePredicate()
    {
        filter(Arrays.asList(new String[] { "a" }), ALWAYS_FALSE).iterator().next();
    }

    @Test
    public void testRemoveTruePredicate()
    {
        List<String> list = new ArrayList<String>();
        list.add("a");
        Iterator<String> iterator = filter(list, ALWAYS_TRUE).iterator();
        assertThat(iterator.next()).isEqualTo("a");
        iterator.remove();
        assertThat(list.contains("a")).isFalse();
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testRemoveFalsePredicate()
    {
        List<String> list = new ArrayList<String>();
        list.add("a");
        filter(list, ALWAYS_FALSE).iterator().remove();
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testRemoveEmpty()
    {
        filter(new ArrayList<String>(), ALWAYS_TRUE).iterator().remove();
    }

    private static final Function<String, Character, NeverThrowsException> FIRST_LETTERS =
            new Function<String, Character, NeverThrowsException>() {
                @Override
                public Character apply(String value) throws NeverThrowsException {
                    return value.charAt(0);
                }
            };

    @DataProvider
    public Object[][] mapCollections() {
        return new Object[][] {
                // @formatter:off
                { new String[] { "goo", "gah", "boo", "bah", "zoo" }, "ggbbz" },
                { new String[] { "boo", "baz", "goo", "gah", "zoo" }, "bbggz" },
                { new String[] { "boo", "goo", "bah", "zoo", "gah" }, "bgbzg" }
                // @formatter:on
        };
    }

    @Test(dataProvider = "mapCollections")
    public void testMap(final String[] value, final String firstLetters) {
        StringBuilder sb = new StringBuilder();
        for (Character element : map(Arrays.asList(value), FIRST_LETTERS)) {
            sb.append(element);
        }
        assertThat(sb.toString()).isEqualTo(firstLetters);
    }

}
