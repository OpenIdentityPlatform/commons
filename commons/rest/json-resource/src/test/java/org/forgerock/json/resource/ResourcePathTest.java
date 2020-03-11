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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.json.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.resource.ResourcePath.resourcePath;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Tests {@link ResourcePath}.
 */
@SuppressWarnings("javadoc")
public final class ResourcePathTest {

    @DataProvider
    public Object[][] invalidStrings() {
        // @formatter:off
        return new Object[][] {
            { "//" },
            { "one//two" },
            { "//one/two" },
            { "one/two//" },
            { "one/two//three" },
            { "//one/two/three" },
            { "one/two/three//" },
        };
        // @formatter:on
    }

    @Test(dataProvider = "invalidStrings", expectedExceptions = IllegalArgumentException.class)
    public void valueOfShouldRejectEmptyPathElements(final String value) {
        resourcePath(value);
    }

    @DataProvider
    public Object[][] valueOfStrings() {
        // @formatter:off
        return new Object[][] {
            // URL encoded path | normalized path | URL decoded path elements.

            // Test empty resource name and normalization.
            { "", "", e() },
            { "/", "", e() },

            // Test non-empty resource names and slash trimming.
            { "users", "users", e("users") },
            { "/users", "users", e("users") },
            { "users/", "users", e("users") },
            { "/users/", "users", e("users") },
            { "users/1", "users/1", e("users", "1") },
            { "/users/1", "users/1", e("users", "1") },
            { "users/1/", "users/1", e("users", "1") },
            { "/users/1/", "users/1", e("users", "1") },

            // Safe characters which do not need percent encoding according to RFC 3986.
            { "abcdefghijklmnopqrstuvwxyz", "abcdefghijklmnopqrstuvwxyz", e("abcdefghijklmnopqrstuvwxyz") },
            { "ABCDEFGHIJKLMNOPQRSTUVWXYZ", "abcdefghijklmnopqrstuvwxyz", e("ABCDEFGHIJKLMNOPQRSTUVWXYZ") },
            { "01234567890", "01234567890", e("01234567890") },
            { "-._~!$&'()*+,;=:@", "-._~!$&'()*+,;=:@", e("-._~!$&'()*+,;=:@") },

            // A selection of unsafe characters which do need percent encoding according to RFC 3986.
            { "%00%1F%20%22%23%25%2F%3C%3E%3F%5B%5C%5D%5E%60%7B%7C%7D%7F%C2%80%C3%BF",
              "%00%1F%20%22%23%25%2F%3C%3E%3F%5B%5C%5D%5E%60%7B%7C%7D%7F%C2%80%C3%BF",
              e("\u0000\u001f \"#%/<>?[\\]^`{|}\u007f\u0080\u00ff") },

            // Percent encoded safe characters should be normalized to their safe equivalent.
            { "%21%24%30%3A%41%5A%5F%61%7A", "!$0:az_az", e("!$0:AZ_az") },

            // Unsafe characters which have not been percent encoded should be normalized to their
            // percent encoded form. Note the inclusion of a Unicode surrogate pair at the end.
            { "\u0000\u001f \"#<>?[\\]^`{|}\u007f\u0080\u00ff\ud800\udc00",
              "%00%1F%20%22%23%3C%3E%3F%5B%5C%5D%5E%60%7B%7C%7D%7F%C2%80%C3%BF%F0%90%80%80",
              e("\u0000\u001f \"#<>?[\\]^`{|}\u007f\u0080\u00ff\ud800\udc00") },

            // Some more realistic examples.
            { "foo%30%41%42%43/test/", "foo0abc/test", e("foo0ABC", "test") },
            { "hello+world/test/user", "hello+world/test/user", e("hello+world", "test", "user") },
            { "hello+world/test%2Fuser", "hello+world/test%2Fuser", e("hello+world", "test/user") },
            { "test/hello%20world/user", "test/hello%20world/user", e("test", "hello world", "user") },
        };
        // @formatter:on
    }

    private String[] e(final String... elements) {
        return elements;
    }

    @Test(dataProvider = "valueOfStrings")
    public void testValueOf(final String path, final String normalizedPath, final String[] elements) {
        final ResourcePath name = resourcePath(path);
        assertThat((Iterable<String>) name).hasSize(elements.length);
        assertThat(name.size()).isEqualTo(elements.length);
        if (elements.length == 0) {
            assertThat((Object) name).isSameAs(ResourcePath.empty());
        } else {
            assertThat((Iterable<String>) name).containsOnly(elements);
        }
        assertThat((Object) name).isEqualTo(resourcePath(normalizedPath));
        assertThat((Object) resourcePath(normalizedPath)).isEqualTo(name);
    }

    @Test(dataProvider = "valueOfStrings")
    public void testConstructorCollection(final String path, final String normalizedPath,
            final String[] elements) {
        final ResourcePath name = new ResourcePath(Arrays.asList(elements));
        assertThat((Iterable<String>) name).hasSize(elements.length);
        assertThat(name.size()).isEqualTo(elements.length);
        assertThat((Iterable<String>) name).containsOnly(elements);
        assertThat((Object) name).isEqualTo(resourcePath(normalizedPath));
        assertThat((Object) resourcePath(normalizedPath)).isEqualTo(name);
    }

    @Test(dataProvider = "valueOfStrings")
    public void testConstructorVarargs(final String path, final String normalizedPath,
            final String[] elements) {
        final ResourcePath name = new ResourcePath((Object[]) elements);
        assertThat((Iterable<String>) name).hasSize(elements.length);
        assertThat(name.size()).isEqualTo(elements.length);
        assertThat((Iterable<String>) name).containsOnly(elements);
        assertThat((Object) name).isEqualTo(resourcePath(normalizedPath));
        assertThat((Object) resourcePath(normalizedPath)).isEqualTo(name);
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
        final ResourcePath actualParent = resourcePath(child).parent();
        assertThat((Object) actualParent).isEqualTo(
                parent != null ? resourcePath(parent) : null);
        assertThat(actualParent != null ? actualParent.toString() : null).isEqualTo(parent);
    }

    @DataProvider
    public Object[][] child() {
        // @formatter:off
        return new Object[][] {
            { "", 123, "123" },
            { "users", 123, "users/123" },
            { "users", "BJENSEN", "users/BJENSEN" },
            { "users", "bjensen", "users/bjensen" },
            { "users", "hello /+world",  "users/hello%20%2F+world"},
        };
        // @formatter:on
    }

    @Test(dataProvider = "child")
    public void testChild(final String base, final Object element, final String expected) {
        final ResourcePath parent = resourcePath(base);
        final ResourcePath child = parent.child(element);
        assertThat((Object) child).isEqualTo(resourcePath(expected));
        assertThat(child.toString()).isEqualTo(expected);
    }

    @Test(dataProvider = "child")
    public void testFormat(final String base, final Object element, final String expected) {
        final ResourcePath child = ResourcePath.format(base + "/%s", element);
        assertThat((Object) child).isEqualTo(resourcePath(expected));
        assertThat(child.toString()).isEqualTo(expected);
    }

    @Test(dataProvider = "child")
    public void testLeaf(final String base, final Object element, final String path) {
        final ResourcePath name = resourcePath(path);
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
            { "Users/%30", "users/0", 0 },
            { "Users/this th%41t", "users/this%20That", 0 },
            { "Users/this+that", "users/this That", 1 },
            { "Users/this+that", "users/this+That", 0 }
        };
        // @formatter:on
    }

    @Test(dataProvider = "compare")
    public void testCompareTo(final String first, final String second, final int expected) {
        final ResourcePath firstPath = resourcePath(first);
        final ResourcePath secondPath = resourcePath(second);
        if (expected < 0) {
            assertThat(firstPath.compareTo(secondPath)).isLessThan(0);
        } else if (expected > 0) {
            assertThat(firstPath.compareTo(secondPath)).isGreaterThan(0);
        } else {
            assertThat(firstPath.compareTo(secondPath)).isEqualTo(0);
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
    public void testConcatResourcePath(final String first, final String second,
            final String expected) {
        final ResourcePath firstPath = resourcePath(first);
        final ResourcePath secondPath = resourcePath(second);
        assertThat((Object) firstPath.concat(secondPath)).isEqualTo(resourcePath(expected));
        assertThat(firstPath.concat(secondPath).toString()).isEqualTo(expected);
    }

    @Test(dataProvider = "concat")
    public void testConcatString(final String first, final String second, final String expected) {
        final ResourcePath firstPath = resourcePath(first);
        assertThat((Object) firstPath.concat(second)).isEqualTo(resourcePath(expected));
        assertThat(firstPath.concat(second).toString()).isEqualTo(expected);
    }

    @Test
    public void testNotEquals() {
        final ResourcePath value = resourcePath("hello/world");
        assertThat((Object) value).isNotEqualTo("hello/world");
    }

    @Test
    public void testEquals() {
        final ResourcePath value1 = resourcePath("hello/world");
        final ResourcePath value2 = resourcePath("HELLO/WORLD");
        assertThat((Object) value1).isEqualTo(value2);
    }

    @Test
    public void testHashCode() {
        final ResourcePath value1 = resourcePath("hello/world");
        final ResourcePath value2 = resourcePath("HELLO/WORLD");
        assertThat(value1.hashCode()).isNotEqualTo(0);
        assertThat(value1.hashCode()).isEqualTo(value2.hashCode());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testValueOfInvalidString() {
        resourcePath("must/not/contain//empty/elements");
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testImmutableIterator() {
        final Iterator<String> i = resourcePath("hello/world").iterator();
        assertThat(i.next()).isEqualTo("hello");
        i.remove();
    }

    @DataProvider
    public Object[][] subSequence() {
        // @formatter:off
        return new Object[][] {
            { "", 0, 0, "" },
            { "one", 0, 0, "" },
            { "one", 0, 1, "one" },
            { "one/TWO/three", 0, 0, "" },
            { "one/TWO/three", 0, 1, "one" },
            { "one/TWO/three", 0, 2, "one/two" },
            { "one/TWO/three", 0, 3, "one/two/three" },
            { "one/TWO/three", 1, 3, "two/three" },
            { "one/TWO/three", 2, 3, "three" },
            { "one/TWO/three", 3, 3, "" },
        };
        // @formatter:on
    }

    @Test(dataProvider = "subSequence")
    public void testSubSequnce(final String path, final int beginIndex, final int endIndex,
            final String expected) {
        final ResourcePath name = resourcePath(path);
        final ResourcePath expectedPath = resourcePath(expected);
        final ResourcePath actualPath = name.subSequence(beginIndex, endIndex);
        assertThat((Object) actualPath).isEqualTo(expectedPath);
        assertThat(actualPath.size()).isEqualTo(endIndex - beginIndex);
    }

    @Test
    public void testHead() {
        ResourcePath name = resourcePath("ONE/TWO/THREE/FOUR");
        ResourcePath expected = resourcePath("ONE/two");
        assertThat((Object) name.head(2)).isEqualTo(expected);

    }

    @Test
    public void testTail() {
        ResourcePath name = resourcePath("ONE/TWO/THREE/FOUR");
        ResourcePath expected = resourcePath("THREE/four");
        assertThat((Object) name.tail(2)).isEqualTo(expected);
    }

    @DataProvider
    public Object[][] startsWith() {
        // @formatter:off
        return new Object[][] {
            { "", "", true },
            { "one", "", true },
            { "one", "on", false },
            { "one", "one", true },
            { "one/two", "one", true },
            { "one/two", "on", false },
            { "one/two", "one/two", true },
            { "one/two", "one/tw", false },
            { "one/two/three", "one", true },
            { "one/two/three", "one/two", true },
            { "one/two/three", "one/two/three", true },
            { "one/two/three", "one/two/t", false },
            { "one/two/three", "one/two/threee", false },
            { "one/two/three", "one/two/three/four", false },
        };
        // @formatter:on
    }

    @Test(dataProvider = "startsWith")
    public void testStartsWith(final String name, final String prefix, final boolean expected) {
        assertThat(resourcePath(name).startsWith(prefix)).isEqualTo(expected);
    }
}
