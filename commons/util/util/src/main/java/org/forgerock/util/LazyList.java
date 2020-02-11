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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.util;

// Java SE
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A list with lazy initialization. The factory is called to initialize the list
 * on the first call to one of this object's methods.
 *
 * @param <E>
 *            The type of element contained in this list.
 */
public class LazyList<E> implements List<E> {

    /** The list that this lazy list exposes, once initialized. */
    private List<E> list;

    /** Factory to create the instance of the list to expose. */
    protected Factory<List<E>> factory;

    /**
     * Constructs a new lazy list. Allows factory to be set in subclass
     * constructor.
     */
    protected LazyList() {
    }

    /**
     * Constructs a new lazy list.
     *
     * @param factory
     *            factory to create the list instance to expose.
     */
    public LazyList(Factory<List<E>> factory) {
        this.factory = factory;
    }

    /**
     * Performs lazy initialization of the list if not already performed, and
     * returns the initialized list.
     */
    private List<E> lazy() {
        if (list == null) {
            synchronized (this) {
                if (list == null) {
                    list = factory.newInstance();
                }
            }
        }
        return list;
    }

    /**
     * Returns the number of elements in this list.
     */
    @Override
    public int size() {
        return lazy().size();
    }

    /**
     * Returns {@code true} if this list contains no elements.
     */
    @Override
    public boolean isEmpty() {
        return lazy().isEmpty();
    }

    /**
     * Returns {@code true} if this list contains the specified element.
     *
     * @param o
     *            the element whose presence in this list is to be tested.
     * @return {@code true} if this list contains the specified element.
     */
    @Override
    public boolean contains(Object o) {
        return lazy().contains(o);
    }

    /**
     * Returns an iterator over the elements in this list in proper sequence.
     */
    @Override
    public Iterator<E> iterator() {
        return lazy().iterator();
    }

    /**
     * Returns an array containing all of the elements in this list in proper
     * sequence (from first to last element).
     */
    @Override
    public Object[] toArray() {
        return lazy().toArray();
    }

    /**
     * Returns an array containing all of the elements in this list in proper
     * sequence (from first to last element); the runtime type of the returned
     * array is that of the specified array. If the list fits in the specified
     * array, it is returned therein. Otherwise, a new array is allocated with
     * the runtime type of the specified array and the size of this list.
     *
     * @param a
     *            the array into which the elements of this list are to be
     *            stored.
     * @return an array containing the elements of this list.
     */
    @Override
    public <T> T[] toArray(T[] a) {
        return lazy().toArray(a);
    }

    /**
     * Appends the specified element to the end of this list.
     *
     * @param e
     *            the element to be appended to this list.
     * @return {@code true} if this list changed as a result of the call.
     */
    @Override
    public boolean add(E e) {
        return lazy().add(e);
    }

    /**
     * Removes the first occurrence of the specified element from this list, if
     * it is present.
     *
     * @param o
     *            the element to be removed from this list, if present.
     * @return true if this list contained the specified element.
     */
    @Override
    public boolean remove(Object o) {
        return lazy().remove(o);
    }

    /**
     * Returns {@code true} if this list contains all of the elements of the
     * specified collection.
     *
     * @param c
     *            the collection to be checked for containment in this list.
     * @return {@code true} if this list contains all of the elements of the
     *         specified collection.
     */
    @Override
    public boolean containsAll(Collection<?> c) {
        return lazy().containsAll(c);
    }

    /**
     * Appends all of the elements in the specified collection to the end of
     * this list, in the order that they are returned by the specified
     * collection's iterator.
     *
     * @param c
     *            the collection containing elements to be added to this list.
     * @return {@code true} if this list changed as a result of the call.
     */
    @Override
    public boolean addAll(Collection<? extends E> c) {
        return lazy().addAll(c);
    }

    /**
     * Inserts all of the elements in the specified collection into this list at
     * the specified position.
     *
     * @param index
     *            the index at which to insert the first element from the
     *            specified collection.
     * @param c
     *            the collection containing elements to be added to this list.
     * @return {@code true} if this list changed as a result of the call.
     */
    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        return lazy().addAll(index, c);
    }

    /**
     * Removes from this list all of its elements that are contained in the
     * specified collection.
     *
     * @param c
     *            the collection containing elements to be removed from this
     *            list.
     * @return {@code true} if this list changed as a result of the call.
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        return lazy().removeAll(c);
    }

    /**
     * Retains only the elements in this list that are contained in the
     * specified collection.
     *
     * @param c
     *            the collection containing elements to be retained in this
     *            list.
     * @return {@code true} if this list changed as a result of the call.
     */
    @Override
    public boolean retainAll(Collection<?> c) {
        return lazy().retainAll(c);
    }

    /**
     * Removes all of the elements from this list.
     */
    @Override
    public void clear() {
        lazy().clear();
    }

    /**
     * Compares the specified object with this list for equality.
     *
     * @param o
     *            the object to be compared for equality with this list.
     * @return {@code true} if the specified object is equal to this list.
     */
    @Override
    public boolean equals(Object o) {
        return lazy().equals(o);
    }

    /**
     * Returns the hash code value for this list.
     */
    @Override
    public int hashCode() {
        return lazy().hashCode();
    }

    /**
     * Returns the element at the specified position in this list.
     *
     * @param index
     *            the index of the element to return.
     * @return the element at the specified position in this list.
     */
    @Override
    public E get(int index) {
        return lazy().get(index);
    }

    /**
     * Replaces the element at the specified position in this list with the
     * specified element.
     *
     * @param index
     *            the index of the element to replace.
     * @param element
     *            the element to be stored at the specified position.
     * @return the element previously at the specified position.
     */
    @Override
    public E set(int index, E element) {
        return lazy().set(index, element);
    }

    /**
     * Inserts the specified element at the specified position in this list.
     *
     * @param index
     *            the index at which the specified element is to be inserted.
     * @param element
     *            the element to be inserted.
     */
    @Override
    public void add(int index, E element) {
        lazy().add(index, element);
    }

    /**
     * Removes the element at the specified position in this list.
     *
     * @param index
     *            the index of the element to be removed.
     * @return the element previously at the specified position.
     */
    @Override
    public E remove(int index) {
        return lazy().remove(index);
    }

    /**
     * Returns the index of the first occurrence of the specified element in
     * this list, or {@code -1} if this list does not contain the element.
     *
     * @param o
     *            element to search for.
     * @return the index of the first occurrence, or {@code -1} if no such
     *         element.
     */
    @Override
    public int indexOf(Object o) {
        return lazy().indexOf(o);
    }

    /**
     * Returns the index of the last occurrence of the specified element in this
     * list, or {@code -1} if this list does not contain the element.
     *
     * @param o
     *            the element to search for.
     * @return the index of the last occurrence, or {@code -1} if no such
     *         element.
     */
    @Override
    public int lastIndexOf(Object o) {
        return lazy().lastIndexOf(o);
    }

    /**
     * Returns a list iterator over the elements in this list (in proper
     * sequence).
     */
    @Override
    public ListIterator<E> listIterator() {
        return lazy().listIterator();
    }

    /**
     * Returns a list iterator over the elements in this list (in proper
     * sequence), starting at the specified position in the list.
     *
     * @param index
     *            the index of the first element to be returned from the list
     *            iterator.
     * @return a list iterator, starting at the specified position in the list.
     */
    @Override
    public ListIterator<E> listIterator(int index) {
        return lazy().listIterator(index);
    }

    /**
     * Returns a view of the portion of this list between the specified
     * fromIndex, inclusive, and toIndex, exclusive.
     *
     * @param fromIndex
     *            low endpoint (inclusive) of the subList.
     * @param toIndex
     *            high endpoint (exclusive) of the subList.
     * @return a view of the specified range within this list.
     */
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return lazy().subList(fromIndex, toIndex);
    }
}
