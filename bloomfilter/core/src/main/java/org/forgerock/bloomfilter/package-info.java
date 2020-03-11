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

/**
 * <h1>ForgeRock Bloom Filters</h1>
 * Implementations of thread-safe, scalable and rolling Bloom Filters. These are Set-like data structures that can
 * scale to very large numbers of entries while only using a small amount of memory (a few bits) per element. The
 * trade-off is that the set membership operation may report false positives (i.e., it may claim that an item is a
 * member of the set when it isn't). The probability of false positives can be tuned by increasing the amount of
 * memory used.
 * <p/>
 * The {@link org.forgerock.bloomfilter.BloomFilter} interface describes the general contract of bloom filters in
 * more detail, and the {@link org.forgerock.bloomfilter.BloomFilters} utility class provides static factory and
 * builder methods for constructing bloom filters for various requirements.
 *
 * <h2>Example</h2>
 * <pre>{@code
 *     BloomFilter<CharSequence> blacklistedSessions = BloomFilters.create(Funnels.stringFunnel(UTF8))
 *              .withInitialCapacity(10000)         // Initial size
 *              .withCapacityGrowthFactor(2.0)      // Double size when full
 *              .withFalsePositiveProbability(0.01) // 1% probability of false positives
 *              .withWriteBatchSize(1000)           // Batch writes
 *              .build();
 *
 *     blacklistedSessions.add("Some session token");
 *
 *     if (blacklistedSessions.mightContain("Some other session")) {
 *         // Take steps to confirm if token is actually black listed or not.
 *     }
 * }</pre>
 *
 * <h2>Scalable and Rolling Bloom Filters</h2>
 * Beyond fixed-capacity Bloom Filters, whose probability of false positives rapidly increases once they have reached
 * capacity, this package also provides <em>scalable</em> and <em>rolling</em> Bloom Filters. The former are an
 * efficient and flexible implementation of the classic <a
 * href="http://www.sciencedirect.com/science/article/pii/S0020019006003127">Scalable Bloom Filters</a> paper by
 * Almeida et al., <em>Information Processing Letters</em>, 101(6), p.255&ndash;261, 2007. The latter are a
 * time-limited variation on this idea, whereby buckets in the scalable bloom filter can expire over time, freeing up
 * memory. The buckets are then recycled ensuring that memory usage is kept reasonable.
 * <p/>
 * Scalable Bloom Filters are useful for storing sets of objects where you do not know <em>a priori</em> the number
 * of elements you might need to store. By dynamically expanding the capacity of the Bloom Filter, as well as
 * reducing the false positive probability of subsequent buckets according to a geometric series, the Scalable Bloom
 * Filter can expand to accommodate orders of magnitude more elements that originally estimated, while preserving the
 * overall configured false positive probability. Use the {@link
 * org.forgerock.bloomfilter.BloomFilters.BloomFilterBuilder#withFalsePositiveProbabilityScaleFactor(double)} and
 * {@link org.forgerock.bloomfilter.BloomFilters.BloomFilterBuilder#withCapacityGrowthFactor(double)} builder methods
 * to configure the scale factors for capacity and false positive probability in these implementations. The defaults
 * (0.8 and 2.0 respectively) provide a good trade off of memory growth and performance.
 * <p/>
 * Rolling Bloom Filters allow elements in a Bloom Filter to expire over time. Use the {@link
 * org.forgerock.bloomfilter.BloomFilters.BloomFilterBuilder#withExpiryStrategy(org.forgerock.bloomfilter.ExpiryStrategy)}
 * method to configure how elements in your Bloom Filter will expire. By default, elements do not expire.
 *
 * <h2>Concurrency Strategies</h2>
 * The implementations provided are currently all thread-safe, and adopt a flexible approach to concurrency control.
 * Two concurrency strategies are currently supported:
 * <ul>
 *     <li><em>SYNCHRONIZED</em> - uses synchronized blocks to ensure mutual exclusion of critical sections. For
 *     fixed-capacity bloom filters all methods are mutually exclusive. For scalable and rolling bloom filters,
 *     individual buckets are independently synchronized, allowing some additional concurrency. This implementation
 *     provides moderate read and write performance.</li>
 *
 *     <li><em>COPY_ON_WRITE</em> - provides very fast read access at the cost of extremely expensive write
 *     operations, which must make a copy of (part of) the Bloom Filter to apply any changes. Write batching (see
 *     below) can be used to amortize write costs over many operations, however this implementation will still
 *     create additional temporary garbage and pressure on the garbage collector. Suitable for situations in which
 *     read performance (mightContain) is paramount and writes are relatively rare (and can tolerate increased
 *     latency).</li>
 * </ul>
 * Use the {@link org.forgerock.bloomfilter.BloomFilters.BloomFilterBuilder#withConcurrencyStrategy(org.forgerock.bloomfilter.ConcurrencyStrategy)}
 * method to specify the concurrency strategy to use. The default is COPY_ON_WRITE.
 *
 * <h2>Write Batching</h2>
 * To compensate for the relatively poor performance of COPY_ON_WRITE concurrency (see previous section), the
 * implementation supports <em>write batching</em>. When enabled, individual calls to the {@link
 * org.forgerock.bloomfilter.BloomFilter#add(java.lang.Object)} method will be buffered in a traditional concurrent
 * collection class until the write batch size is reached. At this point, the buffer will be flushed to the
 * underlying bloom filter implementation in a single operation. This amortizes the cost of copying the underlying
 * collection, at the cost of increased worst-case latencies (when the buffer is flushed) and increased memory churn.
 * The implementation is very highly optimised, supporting very high throughput of both writes and reads, and so is a
 * good choice when throughput is paramount and occasional high write latencies can be tolerated. Use
 * {@link org.forgerock.bloomfilter.BloomFilters.BloomFilterBuilder#withWriteBatchSize(int)} to enable write batching.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Bloom_filter">Bloom Filter Wikipedia entry</a>
 */
package org.forgerock.bloomfilter;