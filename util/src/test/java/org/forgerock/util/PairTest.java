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
 */
package org.forgerock.util;

import org.forgerock.util.Pair;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test the pair class
 */
public class PairTest {

    @Test
    public void shouldRetrieveValuesCorrectly() {
        String left = "left";
        String right = "right";

        Pair<String, String> pair = Pair.of(left, right);

        assertThat(pair.getLeft() == left && pair.getRight() == right);
    }

    @Test
    public void shouldRetrieveDifferentTypesCorrectly() {
        String left = "left";
        Integer right = 1;

        Pair<String, Integer> pair = Pair.of(left, right);

        assertThat(pair.getLeft() == left && pair.getRight() == right);
    }

    @Test
    public void equalShouldWork() {
        String left = "left";
        String right = "right";

        Pair<String, String> pairOne = Pair.of(left, right);
        Pair<String, String> pairTwo = Pair.of(right, left);
        Pair<String, String> pairThree = Pair.of(right, left);
        Pair<String, String> pairFour = Pair.of("foo", "bar");

        assertThat(pairOne.equals(pairOne));

        assertThat(pairOne.equals(pairTwo));
        assertThat(pairTwo.equals(pairOne));
        assertThat(pairOne.equals(pairThree));
        assertThat(pairTwo.equals(pairThree));
        assertThat(pairThree.equals(pairOne));
        assertThat(pairThree.equals(pairTwo));

        assertThat(!pairOne.equals(pairFour));
        assertThat(!pairTwo.equals(pairFour));
        assertThat(!pairFour.equals(pairOne));
        assertThat(!pairFour.equals(pairTwo));

        Pair<String, String> backwards = Pair.of(right, left);

        assertThat(!pairOne.equals(backwards));

        left = "1";
        Integer otherRight = 1;

        Pair<String, Integer> otherPairOne = Pair.of(left, otherRight);
        Pair<Integer, String> otherBackwardPair = Pair.of(otherRight, left);

        assertThat(!otherPairOne.equals(otherBackwardPair));

        Pair<String, Integer> emptyOne = Pair.of(null, null);
        Pair<Integer, String> emptyTwo = Pair.of(null, null);

        assertThat(pairOne.equals(pairOne));
        assertThat(pairTwo.equals(pairTwo));
        assertThat(pairOne.equals(pairTwo));
    }

    @Test
    public void hashCodeShouldWork() {
        String left = "L";
        String right = "R";

        Pair<String, String> pairOne = Pair.of(left, right);
        Pair<String, String> pairTwo = Pair.of(left, right);
        Pair<String, String> backwards = Pair.of(right, left);

        assertThat(pairOne.hashCode() == pairOne.hashCode());
        assertThat(pairOne.hashCode() == pairTwo.hashCode());
        assertThat(backwards.hashCode() == backwards.hashCode());

        left = "1";
        Integer otherRight = 1;
        Pair<String, Integer> otherPairOne = Pair.of(left, otherRight);
        Pair<Integer, String> otherBackwards = Pair.of(otherRight, left);

        // hmmm... can't assert that two objects hashCode values are not equal, since a valid (but exceptionally bad)
        // implementation of hashCode could be to return a constant.  This would mean unequal objects had the same
        // hashCode value.  All we can assert is that equal objects have equal hashCodes.
        assertThat(otherPairOne.hashCode() == otherPairOne.hashCode());
        assertThat(otherBackwards.hashCode() == otherBackwards.hashCode());
    }
}
