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
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.json.fluent;

// Java SE
import java.util.AbstractList;
import java.util.List;

/**
 * TODO: Description.
 *
 * Modification of the wrapped JSON value through this implementation is only allowed if the
 * JSON value is a list.
 *
 * @author Paul C. Bryan
 */
public class JsonValueList<E> extends AbstractList<E> implements JsonValueWrapper {

    /** TODO: Description. */
    private JsonValue jsonValue;

    /**
     * TODO: Description.
     *
     * @param jsonValue TODO.
     * @throws JsonValueException if the {@code jsonValue} is not a List.
     */
    public JsonValueList(JsonValue jsonValue) {
        this.jsonValue = jsonValue.expect(List.class);
    }

    public JsonValue unwrap() {
        return jsonValue;
    }

    /**
     * Returns the number of elements in this list.
     */
    @Override
    public int size() {
        return jsonValue.size();
    }

    /**
     * Returns the element at the specified position in this list.
     *
     * @param index index of the element to return.
     * @return the element at the specified position in this list.
     * @throws IndexOutOfBoundsException if the index is out of range.
     */
    @SuppressWarnings("unchecked")
    @Override
    public E get(int index) {
        if (index < 0 || index >= jsonValue.size()) {
            throw new IndexOutOfBoundsException();
        }
        return (E)(jsonValue.get(index).getWrappedObject());
    }

    /**
     * Replaces the element at the specified position in this list with the specified element.
     *
     * @param index index of the element to replace.
     * @param element element to be stored at the specified position.
     * @return the element previously at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range.
     */
    @Override
    public E set(int index, E element) {
        E result = get(index); // includes index range check
        jsonValue.put(index, element);
        return result;
    }

    /**
     * Inserts the specified element at the specified position in this list.
     *
     * @param index index at which the specified element is to be inserted.
     * @param element element to be inserted.
     * @throws IndexOutOfBoundsException if the index is out of range.
     */
    @Override
    public void add(int index, E element) {
        if (index < 0 || index > jsonValue.size()) {
            throw new IndexOutOfBoundsException();
        }
        jsonValue.add(index, element);
    }

    /**
     * Removes the element at the specified position in this list.
     *
     * @param index the index of the element to be removed.
     * @return the element previously at the specified position.
     */
    @Override
    public E remove(int index) {
        E result = get(index); // includes index range check
        jsonValue.remove(index);
        return result;
    }

    /**
     * Removes all of the elements from this list.
     */
    @Override
    public void clear() {
        jsonValue.clear();
    }

    /**
     * Compares the specified object with this list for equality.
     *
     * @param o the object to be compared for equality with this list.
     * @return {@code true} if the specified object is equal to this list.
     */
    @Override
    public boolean equals(Object o) {
        return jsonValue.getObject().equals(o);
    }

    /**
     * Returns the hash code value for this list.
     */
    @Override
    public int hashCode() {
        return jsonValue.getObject().hashCode();
    }
}
