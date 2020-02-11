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

// TestNG
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("javadoc")
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
        assertThat(Reject.checkNotNull(value)).isSameAs(value);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void checkNotNullNull() {
        Long value = null;
        Reject.checkNotNull(value);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void ifTrueTrue() {
        Reject.ifTrue(true);
    }

    @Test
    public void ifTrueFalse() {
        Reject.ifTrue(false);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void ifFalseFalse() {
        Reject.ifFalse(false);
    }

    @Test
    public void ifFalseTrue() {
        Reject.ifFalse(true);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void ifNullVarArgsStrings() {
        String[] args = { "hello", null };
        Reject.ifNull(args);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void ifNullVarArgsObjects() {
        Object[] args = { "hello", null };
        Reject.ifNull(args);
    }
}
