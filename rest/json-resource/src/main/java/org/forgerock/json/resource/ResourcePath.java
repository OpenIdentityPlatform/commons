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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.json.resource;

import static java.util.Arrays.asList;
import static org.forgerock.http.util.Paths.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

/**
 * A relative path, or URL, to a resource. A resource path is an ordered list of
 * zero or more path elements in big-endian order. The string representation of
 * a resource path conforms to the URL path encoding rules defined in <a
 * href="http://tools.ietf.org/html/rfc3986#section-3.3">RFC 3986 section
 * 3.3</a>:
 *
 * <pre>
 * {@code
 * path          = path-abempty    ; begins with "/" or is empty
 *                 / ...
 *
 * path-abempty  = *( "/" segment )
 * segment       = *pchar
 * pchar         = unreserved / pct-encoded / sub-delims / ":" / "@"
 *
 * unreserved    = ALPHA / DIGIT / "-" / "." / "_" / "~"
 * pct-encoded   = "%" HEXDIG HEXDIG
 * sub-delims    = "!" / "$" / "&" / "'" / "(" / ")"
 *                 / "*" / "+" / "," / ";" / "="
 *
 * HEXDIG        =  DIGIT / "A" / "B" / "C" / "D" / "E" / "F"
 * ALPHA         =  %x41-5A / %x61-7A   ; A-Z / a-z
 * DIGIT         =  %x30-39             ; 0-9
 * }
 * </pre>
 *
 * The empty resource path having zero path elements may be obtained by calling
 * {@link #empty()}. Resource paths are case insensitive and empty path elements
 * are not allowed. In addition, resource paths will be automatically trimmed
 * such that any leading or trailing slashes are removed. In other words, all
 * resource paths will be considered to be "relative". At the moment the
 * relative path elements "." and ".." are not supported.
 * <p>
 * New resource paths can be created from their string representation using
 * {@link #resourcePath(String)}, or by deriving new resource paths from existing
 * values, e.g. using {@link #parent()} or {@link #child(Object)}.
 * <p>
 * Example:
 *
 * <pre>
 * ResourcePath base = ResourcePath.valueOf(&quot;commons/rest&quot;);
 * ResourcePath child = base.child(&quot;hello world&quot;);
 * child.toString(); // commons/rest/hello%20world
 *
 * ResourcePath user = base.child(&quot;users&quot;).child(123);
 * user.toString(); // commons/rest/users/123
 * </pre>
 */
public final class ResourcePath implements Comparable<ResourcePath>, Iterable<String> {
    private static final ResourcePath EMPTY = new ResourcePath();

    /**
     * Returns the empty resource path whose string representation is the empty
     * string and which has zero path elements.
     *
     * @return The empty resource path.
     */
    public static ResourcePath empty() {
        return EMPTY;
    }

    /**
     * Creates a new resource path using the provided path template and
     * unencoded path elements. This method first URL encodes each of the path
     * elements and then substitutes them into the template using
     * {@link String#format(String, Object...)}. Finally, the formatted string
     * is parsed as a resource path using {@link #resourcePath(String)}.
     * <p>
     * This method may be useful in cases where the structure of a resource path
     * is not known at compile time, for example, it may be obtained from a
     * configuration file. Example usage:
     *
     * <pre>
     * String template = "rest/users/%s"
     * ResourcePath path = ResourcePath.format(template, &quot;bjensen&quot;);
     * </pre>
     *
     * @param template
     *            The resource path template.
     * @param pathElements
     *            The path elements to be URL encoded and then substituted into
     *            the template.
     * @return The formatted template parsed as a resource path.
     * @throws IllegalArgumentException
     *             If the formatted template contains empty path elements.
     * @see org.forgerock.http.util.Paths#urlEncode(Object)
     */
    public static ResourcePath format(final String template, final Object... pathElements) {
        final String[] encodedPathElements = new String[pathElements.length];
        for (int i = 0; i < pathElements.length; i++) {
            encodedPathElements[i] = urlEncode(pathElements[i]);
        }
        return resourcePath(String.format(template, (Object[]) encodedPathElements));
    }

    /**
     * Compiled regular expression for splitting resource paths into path
     * elements.
     */
    private static final Pattern PATH_SPLITTER = Pattern.compile("/");

    /**
     * Parses the provided string representation of a resource path.
     *
     * @param path
     *            The URL-encoded resource path to be parsed.
     * @return The provided string representation of a resource path.
     * @throws IllegalArgumentException
     *             If the resource path contains empty path elements.
     * @see #toString()
     */
    public static ResourcePath resourcePath(final String path) {
        return valueOf(path);
    }

    /**
     * Parses the provided string representation of a resource path.
     *
     * @param path
     *            The URL-encoded resource path to be parsed.
     * @return The provided string representation of a resource path.
     * @throws IllegalArgumentException
     *             If the resource path contains empty path elements.
     * @see #toString()
     */
    public static ResourcePath valueOf(final String path) {
        if (path.isEmpty()) {
            // Fast-path.
            return EMPTY;
        }

        // Split on path separators and trim leading slash or trailing slash.
        final String[] elements = PATH_SPLITTER.split(path, -1);
        final int sz = elements.length;
        final int startIndex = elements[0].isEmpty() ? 1 : 0;
        final int endIndex = sz > 1 && elements[sz - 1].isEmpty() ? sz - 1 : sz;
        if (startIndex == endIndex) {
            return EMPTY;
        }

        // Normalize the path elements checking for empty elements.
        final StringBuilder trimmedPath = new StringBuilder(path.length());
        final StringBuilder normalizedPath = new StringBuilder(path.length());
        for (int i = startIndex; i < endIndex; i++) {
            final String element = elements[i];
            if (element.isEmpty()) {
                throw new IllegalArgumentException("Resource path '" + path
                        + "' contains empty path elements");
            }
            final String normalizedElement = normalizePathElement(element, true);
            if (i != startIndex) {
                trimmedPath.append('/');
                normalizedPath.append('/');
            }
            trimmedPath.append(element);
            normalizedPath.append(normalizedElement);
        }
        return new ResourcePath(trimmedPath.toString(), normalizedPath.toString(), endIndex - startIndex);
    }

    private static String normalizePathElement(final String element, final boolean needsDecoding) {
        if (needsDecoding) {
            return urlEncode(urlDecode(element).toLowerCase(Locale.ENGLISH));
        } else {
            return element.toLowerCase(Locale.ENGLISH);
        }
    }

    private final String path; // uri encoded
    private final String normalizedPath; // uri encoded
    private final int size;

    /**
     * Creates a new empty resource path whose string representation is the
     * empty string and which has zero path elements. This method is provided in
     * order to comply with the Java Collections Framework recommendations.
     * However, it is recommended that applications use {@link #empty()} in
     * order to avoid unnecessary memory allocation.
     */
    public ResourcePath() {
        this.path = this.normalizedPath = "";
        this.size = 0;
    }

    /**
     * Creates a new resource path having the provided path elements.
     *
     * @param pathElements
     *            The unencoded path elements.
     */
    public ResourcePath(final Collection<? extends Object> pathElements) {
        int i = 0;
        final StringBuilder pathBuilder = new StringBuilder();
        final StringBuilder normalizedPathBuilder = new StringBuilder();
        for (final Object element : pathElements) {
            final String s = element.toString();
            if (i > 0) {
                pathBuilder.append('/');
                normalizedPathBuilder.append('/');
            }
            final String encodedPathElement = urlEncode(s);
            pathBuilder.append(encodedPathElement);
            final String normalizedPathElement = normalizePathElement(s, false);
            normalizedPathBuilder.append(urlEncode(normalizedPathElement));
            i++;
        }
        this.path = pathBuilder.toString();
        this.normalizedPath = normalizedPathBuilder.toString();
        this.size = pathElements.size();
    }

    /**
     * Creates a new resource path having the provided path elements.
     *
     * @param pathElements
     *            The unencoded path elements.
     */
    public ResourcePath(final Object... pathElements) {
        this(asList(pathElements));
    }

    private ResourcePath(final String path, final String normalizedPath, final int size) {
        this.path = path;
        this.normalizedPath = normalizedPath;
        this.size = size;
    }

    /**
     * Creates a new resource path which is a child of this resource path. The
     * returned resource path will have the same path elements as this resource
     * path and, in addition, the provided path element.
     *
     * @param pathElement
     *            The unencoded child path element.
     * @return A new resource path which is a child of this resource path.
     */
    public ResourcePath child(final Object pathElement) {
        final String s = pathElement.toString();
        final String encodedPathElement = urlEncode(s);
        final String normalizedPathElement = normalizePathElement(s, false);
        final String normalizedEncodedPathElement = urlEncode(normalizedPathElement);
        if (isEmpty()) {
            return new ResourcePath(encodedPathElement, normalizedEncodedPathElement, 1);
        } else {
            final String newPath = path + "/" + encodedPathElement;
            final String newNormalizedPath = normalizedPath + "/" + normalizedEncodedPathElement;
            return new ResourcePath(newPath, newNormalizedPath, size + 1);
        }
    }

    /**
     * Compares this resource path with the provided resource path. Resource
     * paths are compared case sensitively and ancestors sort before
     * descendants.
     *
     * @param o
     *            {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public int compareTo(final ResourcePath o) {
        return normalizedPath.compareTo(o.normalizedPath);
    }

    /**
     * Creates a new resource path which is a descendant of this resource path.
     * The returned resource path will have be formed of the concatenation of
     * this resource path and the provided resource path.
     *
     * @param suffix
     *            The resource path to be appended to this resource path.
     * @return A new resource path which is a descendant of this resource path.
     */
    public ResourcePath concat(final ResourcePath suffix) {
        if (isEmpty()) {
            return suffix;
        } else if (suffix.isEmpty()) {
            return this;
        } else {
            final String newPath = path + "/" + suffix.path;
            final String newNormalizedPath = normalizedPath + "/" + suffix.normalizedPath;
            return new ResourcePath(newPath, newNormalizedPath, size + suffix.size);
        }
    }

    /**
     * Creates a new resource path which is a descendant of this resource path.
     * The returned resource path will have be formed of the concatenation of
     * this resource path and the provided resource path.
     *
     * @param suffix
     *            The resource path to be appended to this resource path.
     * @return A new resource path which is a descendant of this resource path.
     * @throws IllegalArgumentException
     *             If the the suffix contains empty path elements.
     */
    public ResourcePath concat(final String suffix) {
        return concat(resourcePath(suffix));
    }

    /**
     * Returns {@code true} if {@code obj} is a resource path having the exact
     * same elements as this resource path.
     *
     * @param obj
     *            The object to be compared.
     * @return {@code true} if {@code obj} is a resource path having the exact
     *         same elements as this resource path.
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof ResourcePath) {
            return normalizedPath.equals(((ResourcePath) obj).normalizedPath);
        } else {
            return false;
        }
    }

    /**
     * Returns the path element at the specified position in this resource path.
     * The path element at position 0 is the top level element (closest to
     * root).
     *
     * @param index
     *            The index of the path element to be returned, where 0 is the
     *            top level element.
     * @return The path element at the specified position in this resource path.
     * @throws IndexOutOfBoundsException
     *             If the index is out of range (index &lt; 0 || index &gt;= size()).
     */
    public String get(final int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException();
        }
        int startIndex = 0;
        int endIndex = nextElementEndIndex(path, 0);
        for (int i = 0; i < index; i++) {
            startIndex = endIndex + 1;
            endIndex = nextElementEndIndex(path, startIndex);
        }
        return urlDecode(path.substring(startIndex, endIndex));
    }

    /**
     * Returns a hash code for this resource path.
     *
     * @return A hash code for this resource path.
     */
    @Override
    public int hashCode() {
        return normalizedPath.hashCode();
    }

    /**
     * Returns a resource path which is a subsequence of the path elements
     * contained in this resource path beginning with the first element (0) and
     * ending with the element at position {@code endIndex-1}. The returned
     * resource path will therefore have the size {@code endIndex}. Calling this
     * method is equivalent to:
     *
     * <pre>
     * subSequence(0, endIndex);
     * </pre>
     *
     * @param endIndex
     *            The end index, exclusive.
     * @return A resource path which is a subsequence of the path elements
     *         contained in this resource path.
     * @throws IndexOutOfBoundsException
     *             If {@code endIndex} is bigger than {@code size()}.
     */
    public ResourcePath head(final int endIndex) {
        return subSequence(0, endIndex);
    }

    /**
     * Returns {@code true} if this resource path contains no path elements.
     *
     * @return {@code true} if this resource path contains no path elements.
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns an iterator over the path elements in this resource path. The
     * returned iterator will not support the {@link Iterator#remove()} method
     * and will return path elements starting with index 0, then 1, then 2, etc.
     *
     * @return An iterator over the path elements in this resource path.
     */
    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            private int startIndex = 0;
            private int endIndex = nextElementEndIndex(path, 0);

            @Override
            public boolean hasNext() {
                return startIndex < path.length();
            }

            @Override
            public String next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                final String element = path.substring(startIndex, endIndex);
                startIndex = endIndex + 1;
                endIndex = nextElementEndIndex(path, startIndex);
                return urlDecode(element);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }

    /**
     * Returns the last path element in this resource path. Calling this method
     * is equivalent to:
     *
     * <pre>
     * resourcePath.get(resourcePath.size() - 1);
     * </pre>
     *
     * @return The last path element in this resource path.
     */
    public String leaf() {
        return get(size() - 1);
    }

    /**
     * Returns the resource path which is the immediate parent of this resource
     * path, or {@code null} if this resource path is empty.
     *
     * @return The resource path which is the immediate parent of this resource
     *         path, or {@code null} if this resource path is empty.
     */
    public ResourcePath parent() {
        switch (size()) {
        case 0:
            return null;
        case 1:
            return EMPTY;
        default:
            final String newPath = path.substring(0, path.lastIndexOf('/') /* safe */);
            final String newNormalizedPath =
                    normalizedPath.substring(0, normalizedPath.lastIndexOf('/') /* safe */);
            return new ResourcePath(newPath, newNormalizedPath, size - 1);
        }
    }

    /**
     * Returns the number of elements in this resource path, or 0 if it is
     * empty.
     *
     * @return The number of elements in this resource path, or 0 if it is
     *         empty.
     */
    public int size() {
        return size;
    }

    /**
     * Returns {@code true} if this resource path is equal to or begins with the
     * provided resource resource path.
     *
     * @param prefix
     *            The resource path prefix.
     * @return {@code true} if this resource path is equal to or begins with the
     *         provided resource resource path.
     */
    public boolean startsWith(final ResourcePath prefix) {
        if (size == prefix.size) {
            return equals(prefix);
        } else if (size < prefix.size) {
            return false;
        } else if (prefix.size == 0) {
            return true;
        } else {
            return normalizedPath.startsWith(prefix.normalizedPath)
                    && normalizedPath.charAt(prefix.normalizedPath.length()) == '/';
        }
    }

    /**
     * Returns {@code true} if this resource path is equal to or begins with the
     * provided resource resource path.
     *
     * @param prefix
     *            The resource path prefix.
     * @return {@code true} if this resource path is equal to or begins with the
     *         provided resource resource path.
     * @throws IllegalArgumentException
     *             If the the prefix contains empty path elements.
     */
    public boolean startsWith(final String prefix) {
        return startsWith(resourcePath(prefix));
    }

    /**
     * Returns a resource path which is a subsequence of the path elements
     * contained in this resource path beginning with the element at position
     * {@code beginIndex} and ending with the element at position
     * {@code endIndex-1}. The returned resource path will therefore have the
     * size {@code endIndex - beginIndex}.
     *
     * @param beginIndex
     *            The beginning index, inclusive.
     * @param endIndex
     *            The end index, exclusive.
     * @return A resource path which is a subsequence of the path elements
     *         contained in this resource path.
     * @throws IndexOutOfBoundsException
     *             If {@code beginIndex} is negative, or {@code endIndex} is
     *             bigger than {@code size()}, or if {@code beginIndex} is
     *             bigger than {@code endIndex}.
     */
    public ResourcePath subSequence(final int beginIndex, final int endIndex) {
        if (beginIndex < 0 || endIndex > size || beginIndex > endIndex) {
            throw new IndexOutOfBoundsException();
        }
        if (beginIndex == 0 && endIndex == size) {
            return this;
        }
        if (endIndex - beginIndex == 0) {
            return EMPTY;
        }
        final String subPath = subPath(path, beginIndex, endIndex);
        final String subNormalizedPath = subPath(normalizedPath, beginIndex, endIndex);
        return new ResourcePath(subPath, subNormalizedPath, endIndex - beginIndex);
    }

    /**
     * Returns a resource path which is a subsequence of the path elements
     * contained in this resource path beginning with the element at position
     * {@code beginIndex} and ending with the last element in this resource
     * path. The returned resource path will therefore have the size
     * {@code size() - beginIndex}. Calling this method is equivalent to:
     *
     * <pre>
     * subSequence(beginIndex, size());
     * </pre>
     *
     * @param beginIndex
     *            The beginning index, inclusive.
     * @return A resource path which is a subsequence of the path elements
     *         contained in this resource path.
     * @throws IndexOutOfBoundsException
     *             If {@code beginIndex} is negative, or if {@code beginIndex}
     *             is bigger than {@code size()}.
     */
    public ResourcePath tail(final int beginIndex) {
        return subSequence(beginIndex, size);
    }

    /**
     * Returns the URL path encoded string representation of this resource path.
     *
     * @return The URL path encoded string representation of this resource path.
     * @see #resourcePath(String)
     */
    @Override
    public String toString() {
        return path;
    }

    private int nextElementEndIndex(final String s, final int startIndex) {
        final int index = s.indexOf('/', startIndex);
        return index < 0 ? s.length() : index;
    }

    private String subPath(final String s, final int beginIndex, final int endIndex) {
        int startCharIndex = 0;
        int endCharIndex = nextElementEndIndex(s, 0);
        for (int i = 0; i < beginIndex; i++) {
            startCharIndex = endCharIndex + 1;
            endCharIndex = nextElementEndIndex(s, startCharIndex);
        }
        int tmpStartCharIndex;
        for (int i = beginIndex + 1; i < endIndex; i++) {
            tmpStartCharIndex = endCharIndex + 1;
            endCharIndex = nextElementEndIndex(s, tmpStartCharIndex);
        }
        return s.substring(startCharIndex, endCharIndex);
    }
}
