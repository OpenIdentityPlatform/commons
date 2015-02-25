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

import org.forgerock.guava.common.hash.Funnel;

/**
 *
 */
public enum ConcurrencyStrategy {
    COPY_ON_WRITE {
        @Override
        <T> BloomFilterFactory<T> getFactory(final Funnel<? super T> funnel) {
            return new CopyOnWriteBloomFilterFactory<T>(funnel);
        }
    },
    SYNCHRONIZED {
        @Override
        <T> BloomFilterFactory<T> getFactory(final Funnel<? super T> funnel) {
            return new SynchronizedBloomFilterFactory<T>(funnel);
        }
    }
    ;

    abstract <T> BloomFilterFactory<T> getFactory(Funnel<? super T> funnel);

    private static final class CopyOnWriteBloomFilterFactory<T> implements BloomFilterFactory<T> {
        private final Funnel<? super T> funnel;

        CopyOnWriteBloomFilterFactory(final Funnel<? super T> funnel) {
            this.funnel = funnel;
        }

        @Override
        public BloomFilter<T> create(final long expectedInsertions, final double falsePositiveProbability) {
            return new CopyOnWriteBloomFilter<T>(funnel, expectedInsertions, falsePositiveProbability);
        }

    }

    private static final class SynchronizedBloomFilterFactory<T> implements BloomFilterFactory<T> {
        private final Funnel<? super T> funnel;

        SynchronizedBloomFilterFactory(final Funnel<? super T> funnel) {
            this.funnel = funnel;
        }

        @Override
        public BloomFilter<T> create(final long expectedInsertions, final double falsePositiveProbability) {
            return new SynchronizedBloomFilter<T>(funnel, expectedInsertions, falsePositiveProbability);
        }
    }
}
