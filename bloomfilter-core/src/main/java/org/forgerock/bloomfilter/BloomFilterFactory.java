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

/**
 * Abstract factory pattern for creating individual bloom filters with the given capacity and false positive
 * probability.
 *
 * @param <T> the type of elements to be contained in the bloom filter.
 */
interface BloomFilterFactory<T> {
    /**
     * Creates a new Bloom Filter with the given capacity and false positive probability.
     *
     * @param expectedInsertions the expected number of elements to be inserted into the bloom filter.
     * @param falsePositiveProbability the desired probability of false positives.
     * @return a new bloom filter satisfying the requirements.
     */
    BloomFilter<T> create(long expectedInsertions, double falsePositiveProbability);
}
