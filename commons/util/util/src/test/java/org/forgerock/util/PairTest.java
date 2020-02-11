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

import java.math.BigDecimal;
import java.util.Comparator;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.forgerock.util.Pair.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the {@link Pair} class.
 */
@SuppressWarnings("javadoc")
public class PairTest {

    @Test
    public void getters() throws Exception {
        final Pair<BigDecimal, BigDecimal> pair = of(BigDecimal.ONE, BigDecimal.TEN);
        assertThat(pair.getFirst()).isSameAs(BigDecimal.ONE);
        assertThat(pair.getSecond()).isSameAs(BigDecimal.TEN);
    }

    @DataProvider
    public Object[][] pairsEqualDataProvider() {
        final Pair<Integer, Integer> p12 = of(1, 2);
        return new Object[][] {
            { p12, p12 },
            { p12, of(1, 2) },
            { of(null, null), empty() },
        };
    }

    @Test(dataProvider = "pairsEqualDataProvider")
    public void pairsEqual(Pair<Integer, Integer> p1, Pair<Integer, Integer> p2) {
        assertThat(p1).isEqualTo(p2);
        assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
    }

    @Test
    public void morePairsEqual() {
        String left = "left";
        String right = "right";

        Pair<String, String> pairOne = Pair.of(left, right);
        Pair<String, String> pairTwo = Pair.of(left, right);
        Pair<String, String> pairThree = Pair.of(left, right);
        Pair<String, String> pairFour = Pair.of("foo", "bar");

        // equals is reflexive
        assertThat(pairOne).isEqualTo(pairOne);
        assertThat(pairTwo).isEqualTo(pairTwo);
        assertThat(pairThree).isEqualTo(pairThree);
        assertThat(pairFour).isEqualTo(pairFour);

        // symmetric
        assertThat(pairOne).isEqualTo(pairTwo);
        assertThat(pairTwo).isEqualTo(pairOne);

        // transitive
        assertThat(pairOne).isEqualTo(pairThree);
        assertThat(pairTwo).isEqualTo(pairThree);
        assertThat(pairThree).isEqualTo(pairOne);
        assertThat(pairThree).isEqualTo(pairTwo);

        assertThat(pairOne).isNotEqualTo(pairFour);
        assertThat(pairTwo).isNotEqualTo(pairFour);
        assertThat(pairFour).isNotEqualTo(pairOne);
        assertThat(pairFour).isNotEqualTo(pairTwo);

        Pair<String, String> backwards = Pair.of(right, left);

        assertThat(pairOne).isNotEqualTo(backwards);
    }

    @DataProvider
    public Object[][] pairsNotEqualDataProvider() {
        final Pair<Integer, Integer> p12 = of(1, 2);
        return new Object[][] {
            { p12, null },
            { p12, empty() },
            { empty(), p12 },
            { of(null, 2), empty() },
            { empty(), of(null, 2) },
            { of(1, 2), of(2, 1)},
        };
    }

    @Test(dataProvider = "pairsNotEqualDataProvider")
    public void pairsNotEqual(Pair<Integer, Integer> p1, Pair<Integer, Integer> p2) throws Exception {
        assertThat(p1).isNotEqualTo(p2);
    }

    @DataProvider
    public Object[][] pairComparatorDataProvider() {
        return new Object[][] {
            { of(2, 3), of(2, 3), 0 },
            { of(2, 3), of(1, 4), 1 },
            { of(1, 4), of(2, 3), -1 },
            { of(1, 3), of(1, 2), 1 },
            { of(1, 2), of(1, 3), -1 },
        };
    }

    @Test(dataProvider = "pairComparatorDataProvider")
    public void pairComparator(
            Pair<Integer, Integer> p1,
            Pair<Integer, Integer> p2,
            int compareResult) {
        final Comparator<Pair<Integer, Integer>> cmp = getPairComparator();
        assertThat(cmp.compare(p1, p2)).isEqualTo(compareResult);
    }
}
