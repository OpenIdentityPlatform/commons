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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("unchecked")
public class BatchingBloomFilterTest {
    private static final int BATCH_SIZE = 5;

    @Mock
    private BloomFilter<Integer> mockDelegate;

    private BatchingBloomFilter<Integer> testFilter;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
        testFilter = new BatchingBloomFilter<Integer>(new ArgumentCloningBloomFilter<Integer>(mockDelegate), BATCH_SIZE);
    }

    @Test
    public void shouldBatchWrites() {
        // Given
        List<Integer> expected = Arrays.asList(1, 2, 3, 4, 5);

        // When
        for (int x : expected) {
            testFilter.add(x);
        }

        // Then
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Collection> batch = ArgumentCaptor.forClass(Collection.class);
        verify(mockDelegate, atLeastOnce()).addAll(batch.capture());
        assertThat(batch.getValue()).containsAll(expected);
        verifyNoMoreInteractions(mockDelegate);
    }

    @Test
    public void shouldIncludeBufferedElementsInMightContains() {
        // Given
        int value = 3;
        testFilter.add(value);
        verifyNoMoreInteractions(mockDelegate); // Value should still be in buffer

        // When
        boolean result = testFilter.mightContain(value);

        // Then
        assertThat(result).as("Buffered value not considered in mightContain").isTrue();
    }

    @Test
    public void shouldIncludeNonBufferedElementsInMightContains() {
        // Given
        List<Integer> expected = Arrays.asList(1, 2, 3, 4, 5);
        testFilter.addAll(expected);
        verify(mockDelegate).addAll(expected); // Ensure passed through to BF
        given(mockDelegate.mightContain(1)).willReturn(true);

        // When
        boolean result = testFilter.mightContain(1);

        // Then
        assertThat(result).isTrue();
        verify(mockDelegate).mightContain(1);
    }

    /**
     * Wrapper to ensure that arguments to the addAll method are copied before passing to the delegate. This is
     * because Mockito only captures arguments by reference, so we can otherwise only verify the final state of the
     * argument and not how it was at the time of the call.
     */
    static class ArgumentCloningBloomFilter<T> implements BloomFilter<T> {
        private final BloomFilter<T> delegate;

        ArgumentCloningBloomFilter(final BloomFilter<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void add(final T element) {
            delegate.add(element);
        }

        @Override
        public void addAll(final Collection<? extends T> elements) {
            // Create a copy so that we can capture in mockito
            delegate.addAll(new ArrayList<T>(elements));
        }

        @Override
        public boolean mightContain(final T element) {
            return delegate.mightContain(element);
        }

        @Override
        public BloomFilterStatistics getStatistics() {
            return delegate.getStatistics();
        }
    }
}