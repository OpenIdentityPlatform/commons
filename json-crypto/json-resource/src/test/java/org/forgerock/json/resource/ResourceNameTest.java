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
package org.forgerock.json.resource;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Arrays;
import java.util.Iterator;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests {@link ResourceName}.
 */
@SuppressWarnings("javadoc")
public final class ResourceNameTest {

    @DataProvider
    public Object[][] valueOfStrings() {
        // @formatter:off
        return new Object[][] {
            // Test empty resource name and normalization.
            { "", "", e() },
            { "/", "", e() },
            // Test non-empty resource names and normalization.
            { "users", "users", e("users") },
            { "/users", "users", e("users") },
            { "users/", "users", e("users") },
            { "/users/", "users", e("users") },
            { "users/1", "users/1", e("users", "1") },
            { "/users/1", "users/1", e("users", "1") },
            { "users/1/", "users/1", e("users", "1") },
            { "/users/1/", "users/1", e("users", "1") },
            // Test decoding.
            { "hello+world/test%2Fuser", "hello+world/test%2Fuser", e("hello world", "test/user") },
        };
        // @formatter:on
    }

    private Object[] e(final String... elements) {
        return elements;
    }

    @Test(dataProvider = "valueOfStrings")
    public void testValueOf(final String path, final String normalizedPath, final Object[] elements) {
        final ResourceName name = ResourceName.valueOf(path);
        assertThat(name).hasSize(elements.length);
        if (elements.length == 0) {
            assertThat((Object) name).isSameAs(ResourceName.empty());
        } else {
            assertThat(name).containsOnly(elements);
        }
        assertThat(name.toString()).isEqualTo(normalizedPath);
    }

    @Test(dataProvider = "valueOfStrings")
    public void testConstructorCollection(final String path, final String normalizedPath,
            final Object[] elements) {
        final ResourceName name = new ResourceName(Arrays.asList(elements));
        assertThat(name).hasSize(elements.length);
        assertThat(name).containsOnly(elements);
        assertThat(name.toString()).isEqualTo(normalizedPath);
    }

    @Test(dataProvider = "valueOfStrings")
    public void testConstructorVarargs(final String path, final String normalizedPath,
            final Object[] elements) {
        final ResourceName name = new ResourceName(elements);
        assertThat(name).hasSize(elements.length);
        assertThat(name).containsOnly(elements);
        assertThat(name.toString()).isEqualTo(normalizedPath);
    }

    @DataProvider
    public Object[][] parent() {
        // @formatter:off
        return new Object[][] {
            { "", null },
            { "users", "" },
            { "users/1", "users" },
            { "hello+world/test%2Fuser", "hello+world" },
        };
        // @formatter:on
    }

    @Test(dataProvider = "parent")
    public void testParent(final String child, final String parent) {
        final ResourceName actualParent = ResourceName.valueOf(child).parent();
        assertThat((Object) actualParent).isEqualTo(
                parent != null ? ResourceName.valueOf(parent) : null);
        assertThat(actualParent != null ? actualParent.toString() : null).isEqualTo(parent);
    }

    @DataProvider
    public Object[][] child() {
        // @formatter:off
        return new Object[][] {
            { "users", 123, "users/123" },
            { "users", "bjensen", "users/bjensen" },
            { "users", "hello /world",  "users/hello+%2Fworld"},
        };
        // @formatter:on
    }

    @Test(dataProvider = "child")
    public void testChild(final String base, final Object element, final String expected) {
        final ResourceName parent = ResourceName.valueOf(base);
        final ResourceName child = parent.child(element);
        assertThat((Object) child).isEqualTo(ResourceName.valueOf(expected));
        assertThat(child.toString()).isEqualTo(expected);
    }

    @Test(dataProvider = "child")
    public void testFormat(final String base, final Object element, final String expected) {
        final ResourceName child = ResourceName.format(base + "/%s", element);
        assertThat((Object) child).isEqualTo(ResourceName.valueOf(expected));
        assertThat(child.toString()).isEqualTo(expected);
    }

    @Test(dataProvider = "child")
    public void testLeaf(final String base, final Object element, final String path) {
        final ResourceName name = ResourceName.valueOf(path);
        assertThat(name.leaf()).isEqualTo(element.toString());
    }

    @DataProvider
    public Object[][] compare() {
        // @formatter:off
        return new Object[][] {
            { "", "", 0 },
            { "users", "users", 0 },
            { "users/1", "users/1", 0 },
            { "users", "", 1 },
            { "", "users", -1 },
            { "users/1", "users", 1 },
            { "users", "users/1", -1 },
            { "users/1", "users/2", -1 },
            { "users/2", "users/1", 1 },
        };
        // @formatter:on
    }

    @Test(dataProvider = "compare")
    public void testCompareTo(final String first, final String second, final int expected) {
        final ResourceName firstName = ResourceName.valueOf(first);
        final ResourceName secondName = ResourceName.valueOf(second);
        if (expected < 0) {
            assertThat(firstName.compareTo(secondName)).isLessThan(0);
        } else if (expected > 0) {
            assertThat(firstName.compareTo(secondName)).isGreaterThan(0);
        } else {
            assertThat(firstName.compareTo(secondName)).isEqualTo(0);
        }
    }

    @DataProvider
    public Object[][] concat() {
        // @formatter:off
        return new Object[][] {
            { "a/b", "c/d", "a/b/c/d" },
            { "", "c/d", "c/d" },
            { "a/b", "", "a/b" },
        };
        // @formatter:on
    }

    @Test(dataProvider = "concat")
    public void testConcatResourceName(final String first, final String second,
            final String expected) {
        final ResourceName firstName = ResourceName.valueOf(first);
        final ResourceName secondName = ResourceName.valueOf(second);
        assertThat((Object) firstName.concat(secondName)).isEqualTo(ResourceName.valueOf(expected));
        assertThat(firstName.concat(secondName).toString()).isEqualTo(expected);
    }

    @Test(dataProvider = "concat")
    public void testConcatString(final String first, final String second, final String expected) {
        final ResourceName firstName = ResourceName.valueOf(first);
        assertThat((Object) firstName.concat(second)).isEqualTo(ResourceName.valueOf(expected));
        assertThat(firstName.concat(second).toString()).isEqualTo(expected);
    }

    @Test
    public void testNotEquals() {
        final ResourceName value = ResourceName.valueOf("hello/world");
        assertThat((Object) value).isNotEqualTo("hello/world");
    }

    @Test
    public void testHashCode() {
        final ResourceName value = ResourceName.valueOf("hello/world");
        assertThat(value.hashCode()).isNotEqualTo(0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testValueOfInvalidString() {
        ResourceName.valueOf("must/not/contain//empty/elements");
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testImmutableIterator() {
        final Iterator<String> i = ResourceName.valueOf("hello/world").iterator();
        assertThat(i.next()).isEqualTo("hello");
        i.remove();
    }
}
