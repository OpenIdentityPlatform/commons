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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2013 ForgeRock AS.
 */
package org.forgerock.util;


// TestNG
import org.testng.annotations.Test;

// FEST-Assert
import static org.fest.assertions.Assertions.assertThat;


/**
 * Test Reject utilities for proper parameter assumption/validation.
 */
public class RejectTest {

    @Test(expectedExceptions = NullPointerException.class)
    public void ifNullNull() {
        Long value = null;
        Reject.ifNull(value);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void ifNullNullCustomMessage() {
        Long value = null;
        Reject.ifNull(value, "value was null");
    }

    @Test
    public void ifNullNotNull() {
        Long value = Long.MAX_VALUE;
        Reject.ifNull(value);
    }

    @Test
    public void ifNullNotNullCustomMessage() {
        Long value = Long.MAX_VALUE;
        Reject.ifNull(value, "value was null");
    }

    @Test
    public void checkNotNullNotNull() {
        Long value = Long.MAX_VALUE;
        Reject.checkNotNull(value);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void checkNotNullNull() {
        Long value = null;
        Reject.checkNotNull(value);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void ifTrueTrue() {
        Reject.ifTrue(true);
    }

    @Test
    public void ifTrueFalse() {
        Reject.ifTrue(false);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void checkFalseTrue() {
        Reject.checkFalse(true);
    }

    @Test
    public void checkFalseFalse() {
        assertThat(Reject.checkFalse(false)).isFalse();
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void ifFalseFalse() {
        Reject.ifFalse(false);
    }

    @Test
    public void ifFalseTrue() {
        Reject.ifFalse(true);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void checkTrueFalse() {
        Reject.checkTrue(false);
    }

    @Test
    public void checkTrueTrue() {
        assertThat(Reject.checkTrue(true)).isTrue();
    }
}
