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

package org.forgerock.bloomfilter;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;

/**
 * A type of set data structure for which the contains method may return false positives. Implementations typically
 * can store very large numbers of elements in memory at the cost of some loss of accuracy in determining set
 * membership.
 *
 * @param <E> the type of elements contained in the set.
 */
@ThreadSafe
public interface BloomFilter<E> {

    /**
     * Adds the specified element to this set if it is not already possibly present. After a call to this method,
     * subsequent calls to {@link #mightContain(Object)} will return {@code true} for the same object.
     *
     * @param element the element to add to this set.
     * @return {@code true} if the set changed as a result of adding this element. A positive result indicates that
     * the element was not previously in the set, while a negative result may or may not indicate that it was already
     * in the set.
     */
    boolean add(E element);

    /**
     * Adds all of the specified elements to this set if they are not possibly already present.
     *
     * @param elements the elements to add to the set.
     * @return {@code true} if the set changed as a result of adding these elements. A positive result indicates that
     * at least one element of the collection was not present in the set. A negative result may or may not indicate
     * that one of the elements was not present in the set previously.
     */
    boolean addAll(Collection<? extends E> elements);

    /**
     * Checks if the given element <em>might</em> be a member of this set. If this method returns {@code false}, then
     * the given object is definitely not a member of the set. If the result is {@code true} then the object may or
     * may not be a member of this set, with a certain probability of false positives.
     *
     * @param element the element to check for membership in this set.
     * @return {@code false} if the element is definitely not in the set, or {@code true} if it might be.
     */
    boolean mightContain(E element);

    /**
     * Gets a snapshot of the current statistics of the set.
     */
    BloomFilterStatistics statistics();
}
