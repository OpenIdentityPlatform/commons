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

import java.util.NoSuchElementException;

/**
 * A pool of available bloom filters for use in a {@link BloomFilterChain}.
 *
 * @param <T> the type of elements stored in bloom filters contained in this pool.
 * @see GeometricSeriesBloomFilterPool
 */
interface BloomFilterPool<T> {
    /**
     * Returns the next available bloom filter from the pool.
     * @throws NoSuchElementException if the pool has been exhausted.
     */
    BloomFilter<T> nextAvailable();

    /**
     * Releases a bloom filter back to the pool to be reused.
     *
     * @param released the bloom filter to release.
     */
    void release(BloomFilter<T> released);

    /**
     * The overall false positive probability that this pool is trying to achieve.
     */
    double getOverallFalsePositiveProbability();
}
