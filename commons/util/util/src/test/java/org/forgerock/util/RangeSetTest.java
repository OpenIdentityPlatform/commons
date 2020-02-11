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

import static org.assertj.core.api.Assertions.assertThat;

// TestNG
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class RangeSetTest {

    @Test
    public void positiveStep1With10Elements() {
        RangeSet range = new RangeSet(10, 20, 1); // 10 through 19 inclusive
        assertThat(range.size()).isEqualTo(10);
    }

    @Test
    public void positiveStep2With50Elements() {
        RangeSet range = new RangeSet(0, 100, 2); // 0 through 98 inclusive, in steps of 2
        assertThat(range.size()).isEqualTo(50);
    }

    @Test
    public void negativeStep1With10Elements() {
        RangeSet range = new RangeSet(0, -10, -1); // 0 through -9 inclusive
        assertThat(range.size()).isEqualTo(10);
    }

    @Test
    public void negativeStep5With24Elements() {
        RangeSet range = new RangeSet(-10, -241, -10); // -10 through -240, in steps of -10
        assertThat(range.size()).isEqualTo(24);
    }

    @Test
    public void containsStep5() {
        RangeSet range = new RangeSet(5, 26, 5); // 5 through 25 inclusive
        assertThat(range.contains(15)).isTrue();
    }

    @Test
    public void notContainsStep5() {
        RangeSet range = new RangeSet(5, 26, 5); // 5 through 25 inclusive
        assertThat(range.contains(16)).isFalse();
    }

    @Test
    public void nonsensicalStep() {
        RangeSet range = new RangeSet(0, 100, -1); // impossible mission
        assertThat(range.size()).isEqualTo(0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void illegalStep() {
        new RangeSet(0, 100, 0); // meet infinity
    }
}
