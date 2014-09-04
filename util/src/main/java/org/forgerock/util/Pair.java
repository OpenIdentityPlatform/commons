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
 * Copyright 2014 ForgeRock AS.
 *
 * Our own version of the Pair class, this code heavily based on that found in Apache Commons.
 */
package org.forgerock.util;


import java.io.Serializable;

/**
 * A pair consisting of two elements.
 *
 * @param <L> the left element type
 * @param <R> the right element type
 */
public class Pair<L, R> implements Serializable {

    /** Serialization version */
    private static final long serialVersionUID = 1L;

    /** left hand object */
    private final L left;

    /** right hand object */
    private final R right;

    /**
     * <p>Create a pair from two objects inferring the generic types.</p>
     *
     * @param <L> the left element type
     * @param <R> the right element type
     * @param left  the left element, may be null
     * @param right  the right element, may be null
     * @return a pair formed from the two parameters, not null
     */
    public static <L, R> Pair<L, R> of(final L left, final R right) {
        return new Pair<L, R>(left, right);
    }

    /**
     * CTOR.
     * @param left Left object.
     * @param right Right object.
     */
    public Pair(final L left, final R right) {
        this.left = left;
        this.right = right;
    }

    /**
     * Get the left element from this pair.
     *
     * @return left element which may be null
     */
    public L getLeft() {
        return this.left;
    }

    /**
     * Get the right element from the pair.
     *
     * @return right element which may be null
     */
    public R getRight() {
        return this.right;
    }

    /**
     * Compare two pair objects by the values of the left and right elements,i.e. we call equalValues (as opposed to
     * equalObjects) to determine equality.  Note that we rely on the correct implementation of equals in both the
     * contained objects -- if their equals are futzed then ours will be too.
     *
     * @param obj the other pair object to compare to, or some other random object
     * @return true if the elements of the pair are equal in value (i.e. the equals method is used when the two
     * elements are not null).
     */
    @Override
    public boolean equals(final Object obj) {
        return equalValues(obj);
    }

    /**
     * Compare pair objects by their elements' values, i.e. use the element's own equals function for comparison.
     * @param obj The left hand object to compare
     * @return true if the .equals method of the two elements say they are equal
     */
    public boolean equalValues(Object obj) {
        return compareObjects(obj, true);
    }

    /**
     * Compare pair objects by their element's addresses, i.e. use Java == to compare them.
     * @param obj The left hand object to compare
     * @return true if the == operator says the two elements are equal
     */
    public boolean equalObjects(Object obj) {
        return compareObjects(obj, false);
    }

    /**
     * Compare two Pair objects.  The way in which they are compared is dictated by "value". When true, a value
     * comparison is made such that when the two elements of the pair are compared, equals is used to compare
     * the values.  When value is false, the two elements are compared with ==.  As noted elsewhere, if we invoke
     * the objects own equals method, we rely on it to be correctly implemented.  If it is not, ours is likely to be
     * incorrect too.
     *
     * @param obj The other pair object, or some other random type of object.
     * @param value If true, use Object.equals, if false, do an address compare using ==.
     * @return true if the pair objects compare using the mechanism defined by the value parameter.
     */
    private boolean compareObjects(final Object obj, boolean value) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Pair<?, ?>) {
            Pair<?, ?> other = (Pair<?,?>) obj;

            L thisLeft = getLeft();
            Object otherLeft = other.getLeft();

            R thisRight = getRight();
            Object otherRight = other.getRight();

            boolean leftIsTheSame = false;
            boolean rightIsTheSame = false;

            if (thisLeft == null && otherLeft == null) {
                leftIsTheSame = true;
            } else if (thisLeft != null && otherLeft != null) {
                if (value) {
                    leftIsTheSame = thisLeft.equals(otherLeft);
                } else {
                    leftIsTheSame = thisLeft == otherLeft;
                }
            }

            if (thisRight == null && otherRight == null) {
                rightIsTheSame = true;
            } else if (thisRight != null && otherRight != null) {
                if (value) {
                    rightIsTheSame = thisRight.equals(otherRight);
                } else {
                    rightIsTheSame = thisRight == otherRight;
                }
            }

            return leftIsTheSame && rightIsTheSame;
        }
        return false;
    }

    /**
     * @return the hash code, noting that the hashcode is not symmetrical between left and right members.  This
     * implies Pair&lt;"abc", "def"&gt;.hashCode() is not equal to Pair&lt;"def", "abc"&gt;.hashCode()
     */
    @Override
    public int hashCode() {
        return (getLeft() == null ? 0 : getLeft().hashCode() * 31) + (getRight() == null ? 0 : getRight().hashCode());
    }

    /**
     * @return a string describing the left and right objects.
     */
    @Override
    public String toString() {
        return new StringBuilder()
                .append("(")
                .append(getLeft())
                .append(",")
                .append(getRight())
                .append(")")
                .toString();
    }
}
