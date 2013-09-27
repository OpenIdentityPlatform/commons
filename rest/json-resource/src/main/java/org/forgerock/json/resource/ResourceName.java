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
 * Copyright 2013 ForgeRock Inc.
 */
package org.forgerock.json.resource;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * A relative path, or URL, to a resource. A resource name is an ordered list of
 * zero or more path elements in big-endian order. The string representation of
 * a resource name conforms to the URL encoding rules defined in RFC 2396.
 * Specifically, all path elements will be encoded according to the rules
 * described in the {@link URLEncoder} documentation.
 * <p>
 * The empty resource name having zero path elements may be obtained by calling
 * {@link #empty()}. Resource names are case sensitive and empty path elements
 * are not allowed.
 * <p>
 * Resource names are immutable and, as such, all mutating {@link List} methods
 * will throw an {@link UnsupportedOperationException}. New resource names can
 * be created from their string representation using {@link #valueOf(String)},
 * or by deriving new resource names from existing values, e.g. using
 * {@link #parent()} or {@link #child(Object)}.
 * <p>
 * Example:
 * 
 * <pre>
 * ResourceName base = ResourceName.valueOf(&quot;commons/rest&quot;);
 * ResourceName child = base.child(&quot;hello world&quot;);
 * child.toString(); // commons/rest/hello+world
 * 
 * ResourceName user = base.child(&quot;users&quot;).child(123);
 * user.toString(); // commons/rest/users/123
 * </pre>
 */
public final class ResourceName extends AbstractList<String> implements Comparable<ResourceName>,
        List<String> {
    private static final ResourceName EMPTY = new ResourceName();

    /**
     * Look up table for characters which do not need URL encoding.
     */
    private static final BitSet SAFE_URL_CHARS = new BitSet(128);
    static {
        /*
         * These characters do not need encoding. A space character only needs
         * converting to '+' but since it mutates the string, we'll exclude it
         * from the list below.
         */
        SAFE_URL_CHARS.set('-');
        SAFE_URL_CHARS.set('_');
        SAFE_URL_CHARS.set('.');
        SAFE_URL_CHARS.set('*');

        /*
         * ASCII alphanumeric characters are ok as well.
         */
        SAFE_URL_CHARS.set('0', '9' + 1);
        SAFE_URL_CHARS.set('a', 'z' + 1);
        SAFE_URL_CHARS.set('A', 'Z' + 1);
    }

    /**
     * Returns the empty resource name whose string representation is the empty
     * string and which has zero path elements.
     * 
     * @return The empty resource name.
     */
    public static ResourceName empty() {
        return EMPTY;
    }

    /**
     * Returns the URL encoding of the string representation of the provided
     * path element.
     * 
     * @param pathElement
     *            The path element to be encoded.
     * @return The URL encoded path element.
     */
    public static String encodePathElement(final Object pathElement) {
        if (pathElement == null) {
            throw new NullPointerException();
        }
        /*
         * Before delegating to the costly URLEncoder class, first try fast-path
         * encode of simple ASCII.
         */
        final String s = pathElement.toString();
        final int size = s.length();
        for (int i = 0; i < size; i++) {
            final int c = s.charAt(i);
            if (!SAFE_URL_CHARS.get(c)) {
                // Contains a character that needs encoding so delegate to URLEncoder.
                try {
                    return URLEncoder.encode(s, "UTF-8");
                } catch (final UnsupportedEncodingException e) {
                    // UTF-8 should always be supported.
                    throw new RuntimeException(e);
                }
            }
        }
        return s;
    }

    /**
     * Creates a new resource name using the provided name template and
     * unencoded path elements. This method first URL encodes each of the path
     * elements and then substitutes them into the template using
     * {@link String#format(String, Object...)}. Finally, the formatted string
     * is parsed as a resource name using {@link #valueOf(String)}.
     * <p>
     * This method may be useful in cases where the structure of a resource name
     * is not known at compile time, for example, it may be obtained from a
     * configuration file. Example usage:
     * 
     * <pre>
     * String template = "rest/users/%s"
     * ResourceName name = ResourceName.format(template, &quot;bjensen&quot;);
     * </pre>
     * 
     * @param template
     *            The resource name template.
     * @param pathElements
     *            The path elements to be URL encoded and then substituted into
     *            the template.
     * @return The formatted template parsed as a resource name.
     * @throws IllegalArgumentException
     *             If the formatted template contains empty path elements.
     * @see #encodePathElement(Object)
     */
    public static ResourceName format(final String template, final Object... pathElements) {
        final String[] encodedPathElements = new String[pathElements.length];
        for (int i = 0; i < pathElements.length; i++) {
            encodedPathElements[i] = encodePathElement(pathElements[i]);
        }
        return valueOf(String.format(template, (Object[]) encodedPathElements));
    }

    /**
     * Parses the provided string representation of a resource name.
     * 
     * @param path
     *            The resource name to be parsed.
     * @return The provided string representation of a resource name.
     * @throws IllegalArgumentException
     *             If the resource name contains empty path elements.
     * @see #toString()
     */
    public static ResourceName valueOf(final String path) {
        if (path.isEmpty()) {
            // Fast-path.
            return EMPTY;
        } else {
            // For performance attempt to determine:
            // - if this is a single element path
            // - if the path contains empty elements (error)
            // - how many elements it contains.
            final int size = path.length();
            List<String> elements = null;
            boolean lastCharWasSlash = false;
            int startOfLastElement = 0;
            boolean lastElementNeedsDecoding = false;
            boolean trimLeadingSlash = false;
            for (int i = 0; i < size; i++) {
                final char c = path.charAt(i);
                if (c == '/') {
                    if (lastCharWasSlash) {
                        throw new IllegalArgumentException("Resource name '" + path
                                + "' contains empty path elements");
                    }
                    lastCharWasSlash = true;
                    if (startOfLastElement != 0) {
                        if (elements == null) {
                            elements = new LinkedList<String>();
                        }
                        final String element = path.substring(startOfLastElement, i);
                        final String decodedElement =
                                lastElementNeedsDecoding ? decodePathElement(element) : element;
                        elements.add(decodedElement);
                    } else {
                        trimLeadingSlash = true;
                    }
                } else if (lastCharWasSlash) {
                    // Reset state for next element.
                    lastCharWasSlash = false;
                    startOfLastElement = i;
                    lastElementNeedsDecoding = isUrlEscapeChar(c);
                } else if (isUrlEscapeChar(c)) {
                    lastElementNeedsDecoding = true;
                }
            }
            // Normalize the path string by removing leading and trailing slashes.
            final String trimmedPath;
            if (trimLeadingSlash || lastCharWasSlash) {
                trimmedPath =
                        path.substring((trimLeadingSlash ? 1 : 0), (lastCharWasSlash ? size - 1
                                : size));
            } else {
                trimmedPath = path;
            }
            // Decode remaining trailing path element.
            if (!lastCharWasSlash) {
                final String element = path.substring(startOfLastElement, size);
                final String decodedElement =
                        lastElementNeedsDecoding ? decodePathElement(element) : element;
                if (elements == null) {
                    // Avoid unnecessary allocation for common case of single path element.
                    return new ResourceName(trimmedPath, decodedElement);
                } else {
                    elements.add(decodedElement);
                }
            }
            return new ResourceName(trimmedPath, elements.toArray(new String[elements.size()]));
        }
    }

    private static String decodePathElement(final String element) {
        try {
            return URLDecoder.decode(element, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            // UTF-8 should always be supported.
            throw new RuntimeException(e);
        }
    }

    private static boolean isUrlEscapeChar(final char c) {
        return c == '+' || c == '%';
    }

    private final String[] elements; // uri decoded
    private final String path; // uri encoded

    /**
     * Creates a new empty resource name whose string representation is the
     * empty string and which has zero path elements. This method is provided in
     * order to comply with the Java Collections Framework recommendations.
     * However, it is recommended that applications use {@link #empty()} in
     * order to avoid unnecessary memory allocation.
     */
    public ResourceName() {
        this.elements = new String[0];
        this.path = "";
    }

    /**
     * Creates a new resource name having the provided path elements.
     * 
     * @param pathElements
     *            The unencoded path elements.
     */
    public ResourceName(final Collection<? extends Object> pathElements) {
        this.elements = new String[pathElements.size()];
        int i = 0;
        final StringBuilder builder = new StringBuilder();
        for (final Object element : pathElements) {
            final String s = element.toString();
            if (i > 0) {
                builder.append('/');
            }
            builder.append(encodePathElement(s));
            this.elements[i++] = s;
        }
        this.path = builder.toString();
    }

    /**
     * Creates a new resource name having the provided path elements.
     * 
     * @param pathElements
     *            The unencoded path elements.
     */
    public ResourceName(final Object... pathElements) {
        this(Arrays.asList(pathElements));
    }

    private ResourceName(final String path, final String... elements) {
        this.elements = elements;
        this.path = path;
    }

    /**
     * Creates a new resource name which is a child of this resource name. The
     * returned resource name will have the same path elements as this resource
     * name and, in addition, the provided path element.
     * 
     * @param pathElement
     *            The unencoded child path element.
     * @return A new resource name which is a child of this resource name.
     */
    public ResourceName child(final Object pathElement) {
        final String[] newElements = new String[elements.length + 1];
        System.arraycopy(elements, 0, newElements, 0, elements.length);
        final String s = pathElement.toString();
        newElements[elements.length] = s;
        final String encodedPathElement = encodePathElement(s);
        final String newPath =
                isEmpty() ? encodedPathElement : new StringBuilder(path).append('/').append(
                        encodedPathElement).toString();
        return new ResourceName(newPath, newElements);
    }

    /**
     * Compares this resource name with the provided resource name. Resource
     * names are compared case sensitively and ancestors sort before
     * descendants.
     */
    @Override
    public int compareTo(final ResourceName o) {
        final int minSize = Math.min(size(), o.size());
        for (int i = 0; i < minSize; i++) {
            final int result = elements[i].compareTo(o.elements[i]);
            if (result != 0) {
                return result;
            }
        }
        return size() - o.size();
    }

    /**
     * Creates a new resource name which is a descendant of this resource name.
     * The returned resource name will have be formed of the concatenation of
     * this resource name and the provided resource name.
     * 
     * @param childPath
     *            The resource name to be appended to this resource name.
     * @return A new resource name which is a descendant of this resource name.
     */
    public ResourceName concat(final ResourceName childPath) {
        if (isEmpty()) {
            return childPath;
        } else if (childPath.isEmpty()) {
            return this;
        } else {
            final String[] newElements = new String[elements.length + childPath.elements.length];
            System.arraycopy(elements, 0, newElements, 0, elements.length);
            System.arraycopy(childPath.elements, 0, newElements, elements.length,
                    childPath.elements.length);
            final String newPath = path + "/" + childPath.path;
            return new ResourceName(newPath, newElements);
        }
    }

    /**
     * Creates a new resource name which is a descendant of this resource name.
     * The returned resource name will have be formed of the concatenation of
     * this resource name and the provided resource name.
     * 
     * @param childPath
     *            The resource name to be appended to this resource name.
     * @return A new resource name which is a descendant of this resource name.
     * @throws IllegalArgumentException
     *             If the the child path contains empty path elements.
     */
    public ResourceName concat(final String childPath) {
        return concat(valueOf(childPath));
    }

    /**
     * Returns the path element at the specified position in this resource name.
     * The path element at position 0 is the top level element (closest to
     * root).
     */
    @Override
    public String get(final int index) {
        return elements[index];
    }

    /**
     * Returns the resource name which is the immediate parent of this resource
     * name, or {@code null} if this resource name is empty.
     * 
     * @return The resource name which is the immediate parent of this resource
     *         name, or {@code null} if this resource name is empty.
     */
    public ResourceName parent() {
        switch (size()) {
        case 0:
            return null;
        case 1:
            return EMPTY;
        default:
            final String[] newElements = new String[elements.length - 1];
            System.arraycopy(elements, 0, newElements, 0, elements.length - 1);
            final String newPath = path.substring(0, path.lastIndexOf('/') /* safe */);
            return new ResourceName(newPath, newElements);
        }
    }

    /**
     * Returns the number of elements in this resource name, or 0 if it is
     * empty.
     */
    @Override
    public int size() {
        return elements.length;
    }

    /**
     * Returns the URL encoded string representation of this resource name.
     * 
     * @return The URL encoded string representation of this resource name.
     * @see #valueOf(String)
     */
    @Override
    public String toString() {
        return path;
    }
}
