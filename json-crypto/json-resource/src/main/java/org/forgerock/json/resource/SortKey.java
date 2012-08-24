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
 * Copyright 2012 ForgeRock AS. All rights reserved.
 */

package org.forgerock.json.resource;

import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonPointer;

/**
 * A sort key which can be used to specify the order in which JSON resources
 * should be included in the results of a query request.
 */
public final class SortKey {

    /**
     * Creates a new ascending-order sort key for the provided JSON field.
     *
     * @param field
     *            The sort key field.
     * @return A new ascending-order sort key.
     */
    public static SortKey ascendingOrder(final JsonPointer field) {
        return new SortKey(field, true);
    }

    /**
     * Creates a new ascending-order sort key for the provided JSON field.
     *
     * @param field
     *            The sort key field.
     * @return A new ascending-order sort key.
     * @throws IllegalArgumentException
     *             If {@code field} is not a valid JSON pointer.
     */
    public static SortKey ascendingOrder(final String field) {
        try {
            return ascendingOrder(new JsonPointer(field));
        } catch (JsonException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Creates a new descending-order sort key for the provided JSON field.
     *
     * @param field
     *            The sort key field.
     * @return A new descending-order sort key.
     */
    public static SortKey descendingOrder(final JsonPointer field) {
        return new SortKey(field, false);
    }

    /**
     * Creates a new descending-order sort key for the provided JSON field.
     *
     * @param field
     *            The sort key field.
     * @return A new descending-order sort key.
     * @throws IllegalArgumentException
     *             If {@code field} is not a valid JSON pointer.
     */
    public static SortKey descendingOrder(final String field) {
        try {
            return descendingOrder(new JsonPointer(field));
        } catch (JsonException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Creates a new sort key having the same field as the provided key, but in
     * reverse sort order.
     *
     * @param key
     *            The sort key to be reversed.
     * @return The reversed sort key.
     */
    public static SortKey reverseOrder(final SortKey key) {
        return new SortKey(key.field, !key.isAscendingOrder);
    }

    private final JsonPointer field;

    private final boolean isAscendingOrder;

    private SortKey(final JsonPointer field, final boolean isAscendingOrder) {
        this.field = field;
        this.isAscendingOrder = isAscendingOrder;
    }

    /**
     * Returns the sort key field.
     *
     * @return The sort key field.
     */
    public JsonPointer getField() {
        return field;
    }

    /**
     * Returns {@code true} if this sort key is in ascending order, or
     * {@code false} if it is in descending order.
     *
     * @return {@code true} if this sort key is in ascending order, or
     *         {@code false} if it is in descending ord)er.
     */
    public boolean isAscendingOrder() {
        return isAscendingOrder;
    }

    /**
     * Parses the provided string as a sort key. If the string does not begin
     * with a plus or minus symbol, then the sort key will default to ascending
     * order.
     *
     * @param s
     *            The string representation of a sort key as specified in
     *            {@link #toString()}.
     * @return The parsed sort key.
     * @throws IllegalArgumentException
     *             If {@code s} is not a valid sort key.
     */
    public static SortKey valueOf(String s) {
        if (s.length() == 0) {
            throw new IllegalArgumentException("Empty sort key");
        }

        switch (s.charAt(0)) {
        case '-':
            return descendingOrder(s.substring(1));
        case '+':
            return ascendingOrder(s.substring(1));
        default:
            return ascendingOrder(s);
        }
    }

    /**
     * Returns the string representation of this sort key. It will be composed
     * of a plus symbol, if the key is ascending, or a minus symbol, if the key
     * is descending, followed by the field name.
     *
     * @return The string representation of this sort key.
     */
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(isAscendingOrder ? '+' : '-');
        builder.append(field);
        return builder.toString();
    }
}
