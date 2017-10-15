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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2011-2016 ForgeRock AS.
 */

package org.forgerock.json;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Identifies a specific value within a JSON structure. Conforms with
 * <a href="http://tools.ietf.org/html/draft-pbryan-zyp-json-pointer-02">draft-pbryan-zip-json-pointer-02</a>.
 */
public class JsonPointer implements Iterable<String> {

    /** The reference tokens that make-up the JSON pointer. */
    private String[] tokens = new String[0];

    /**
     * Constructs a JSON pointer, identifying the root value of a JSON structure.
     */
    public JsonPointer() {
        // empty tokens represents pointer to root value
    }

    /**
     * Constructs a JSON pointer, identifying the specified pointer value.
     *
     * @param pointer a string containing the JSON pointer of the value to identify.
     * @throws JsonException if the pointer is malformed.
     */
    public JsonPointer(String pointer) {
        String[] split = pointer.split("/", -1);
        int length = split.length;
        ArrayList<String> list = new ArrayList<>(length);
        for (int n = 0; n < length; n++) {
            if (n == 0 && split[n].length() == 0) {
                continue; // leading slash ignored
            } else if (n == length - 1 && split[n].length() == 0) {
                continue; // trailing slash ignored
            } else {
                list.add(decode(split[n]));
            }
        }
        tokens = list.toArray(tokens);
    }

    /**
     * Constructs a JSON pointer from an array of reference tokens.
     *
     * @param tokens an array of string reference tokens.
     */
    public JsonPointer(String... tokens) {
        this.tokens = Arrays.copyOf(tokens, tokens.length);
    }

    /**
     * Constructs a JSON pointer from an iterable collection of reference tokens.
     *
     * @param iterable an iterable collection of reference tokens.
     */
    public JsonPointer(Iterable<String> iterable) {
        ArrayList<String> list = new ArrayList<>();
        for (String element : iterable) {
            list.add(element);
        }
        tokens = list.toArray(tokens);
    }

    /**
     * Encodes a reference token into a string value suitable to expressing in a JSON
     * pointer string value.
     *
     * @param value the reference token value to be encoded.
     * @return the encode reference token value.
     */
    private String encode(String value) {
        try {
            return new URI(null, null, null, null, value).toASCIIString().substring(1).replaceAll("/", "%2F");
        } catch (URISyntaxException use) { // shouldn't happen
            throw new IllegalStateException(use.getMessage());
        }
    }

    /**
     * Decodes a reference token into a string value that the pointer maintains.
     *
     * @param value the reference token value to decode.
     * @return the decoded reference token value.
     * @throws JsonException if the reference token value is malformed.
     */
    private String decode(String value) {
        try {
            return new URI("#" + value).getFragment();
        } catch (URISyntaxException use) {
            throw new JsonException(use.getMessage());
        }
    }

    /**
     * Returns the number of reference tokens in the pointer.
     *
     * @return the number of reference tokens in the pointer.
     */
    public int size() {
        return tokens.length;
    }

    /**
     * Returns the reference token at the specified position.
     *
     * @param index the index of the reference token to return.
     * @return the reference token at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range.
     */
    public String get(int index) {
        if (index < 0 || index >= tokens.length) {
            throw new IndexOutOfBoundsException();
        }
        return tokens[index];
    }

    /**
     * Returns a newly allocated array of strings, containing the pointer's reference tokens.
     * No references to the array are maintained by the pointer. Hence, the caller is free to
     * modify it.
     *
     * @return a newly allocated array of strings, containing the pointer's reference tokens.
     */
    public String[] toArray() {
        return Arrays.copyOf(tokens, tokens.length);
    }

    /**
     * Returns a pointer to the parent of the JSON value identified by this JSON pointer,
     * or {@code null} if the pointer has no parent JSON value (i.e. references document root).
     *
     * @return a pointer to the parent of of this JSON pointer. Can be null.
     */
    public JsonPointer parent() {
        JsonPointer parent = null;
        if (this.tokens.length > 0) {
            parent = new JsonPointer();
            parent.tokens = Arrays.copyOf(this.tokens, this.tokens.length - 1);
        }
        return parent;
    }

    /**
     * Returns a pointer containing all but the first reference token contained
     * in this pointer, or {@code /} if this pointer contains less than 2
     * reference tokens.
     * <p>
     * This method yields the following results: <blockquote>
     * <table cellpadding=1 cellspacing=0 summary="Examples illustrating usage of relativePointer">
     * <tr>
     * <th>Input</th>
     * <th>Output</th>
     * </tr>
     * <tr>
     * <td align=left>/</td>
     * <td align=left><tt>/</tt></td>
     * </tr>
     * <tr>
     * <td align=left>/a</td>
     * <td align=left><tt>/</tt></td>
     * </tr>
     * <tr>
     * <td align=left>/a/b</td>
     * <td align=left>/b</td>
     * </tr>
     * <tr>
     * <td align=left>/a/b/c</td>
     * <td align=left>/b/c</td>
     * </tr>
     * </table>
     * </blockquote>
     *
     * @return A pointer containing all but the first reference token contained
     *         in this pointer.
     */
    public JsonPointer relativePointer() {
        return tokens.length > 0 ? relativePointer(tokens.length - 1) : this;
    }

    /**
     * Returns a pointer containing the last {@code sz} reference tokens
     * contained in this pointer.
     * <p>
     * This method yields the following results: <blockquote>
     * <table cellpadding=1 cellspacing=0 summary="Examples illustrating usage of relativePointer">
     * <tr>
     * <th>Input</th>
     * <th>sz</th>
     * <th>Output</th>
     * </tr>
     * <tr>
     * <td align=left>/a/b/c</td>
     * <td align=center>0</td>
     * <td align=left>/</td>
     * </tr>
     * <tr>
     * <td align=left>/a/b/c</td>
     * <td align=center>1</td>
     * <td align=left>/c</td>
     * </tr>
     * <tr>
     * <td align=left>/a/b/c</td>
     * <td align=center>2</td>
     * <td align=left>/b/c</td>
     * </tr>
     * <tr>
     * <td align=left>/a/b/c</td>
     * <td align=center>3</td>
     * <td align=left>/a/b/c</td>
     * </tr>
     * </table>
     * </blockquote>
     *
     * @param sz
     *            The number of trailing reference tokens to retain.
     * @return A pointer containing the last {@code sz} reference tokens
     *         contained in this pointer.
     * @throws IndexOutOfBoundsException
     *             If {@code sz} is negative or greater than {@code size()}.
     */
    public JsonPointer relativePointer(int sz) {
        int length = tokens.length;
        if (sz < 0 || sz > length) {
            throw new IndexOutOfBoundsException();
        } else if (sz == length) {
            return this;
        } else if (sz == 0) {
            return new JsonPointer();
        } else {
            JsonPointer relativePointer = new JsonPointer();
            relativePointer.tokens = Arrays.copyOfRange(tokens, length - sz, length);
            return relativePointer;
        }
    }

    /**
     * Returns the last (leaf) reference token of the JSON pointer, or {@code null} if the
     * pointer contains no reference tokens (i.e. references document root).
     *
     * @return the last (leaf) reference token of the JSON pointer if it exists, {@code null} otherwise
     */
    public String leaf() {
        return tokens.length > 0 ? tokens[tokens.length - 1] : null;
    }

    /**
     * Returns a new JSON pointer, which identifies a specified child member of the
     * object identified by this pointer.
     *
     * @param child the name of the child member to identify.
     * @return the child JSON pointer.
     * @throws NullPointerException if {@code child} is {@code null}.
     */
    public JsonPointer child(String child) {
        if (child == null) {
            throw new NullPointerException();
        }
        JsonPointer pointer = new JsonPointer();
        pointer.tokens = Arrays.copyOf(this.tokens, this.tokens.length + 1);
        pointer.tokens[pointer.tokens.length - 1] = child;
        return pointer;
    }

    /**
     * Returns a new JSON pointer, which identifies a specified child element of the
     * array identified by this pointer.
     *
     * @param child the index of the child element to identify.
     * @return the child JSON pointer.
     * @throws IndexOutOfBoundsException if {@code child} is less than zero.
     */
    public JsonPointer child(int child) {
        if (child < 0) {
            throw new IndexOutOfBoundsException();
        }
        return child(Integer.toString(child));
    }

    /**
     * Returns {@code true} if this pointer identifies the root value of a JSON
     * structure. More specifically, it returns {@code true} if this pointer
     * does not contain any reference tokens (i.e. {@code size() == 0}).
     *
     * @return {@code true} if this pointer identifies the root value of a JSON
     *         structure.
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns an iterator over the pointer's reference tokens.
     *
     * @return an iterator over the pointer's reference tokens.
     */
    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            int cursor = 0;
            @Override
            public boolean hasNext() {
                return cursor < tokens.length;
            }
            @Override
            public String next() {
                if (cursor >= tokens.length) {
                    throw new NoSuchElementException();
                }
                return tokens[cursor++];
            }
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Returns the JSON pointer string value.
     *
     * @return the JSON pointer string value.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            sb.append('/').append(encode(token));
        }
        if (sb.length() == 0) {
            sb.append('/');
        }
        return sb.toString();
    }

    /**
     * Compares the specified object with this pointer for equality. Returns {@code true} if
     * and only if the specified object is also a JSON pointer, both pointers have the same
     * size, and all corresponding pairs of reference tokens in the two pointers are equal.
     *
     * @param o the object to be compared for equality with this pointer.
     * @return {@code true} if the specified object is equal to this pointer.
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof JsonPointer
                && ((JsonPointer) o).size() == size()
                && Arrays.equals(tokens, ((JsonPointer) o).tokens);
    }

    /**
     * Returns the hash code value for this pointer.
     *
     * @return the hash code value for this pointer.
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(tokens);
    }
}
