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
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.util;

import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.util.Utils.joinAsString;
import static org.forgerock.util.Utils.asEnum;

import java.util.Arrays;
import java.util.Collections;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class UtilsTest {

    @Test
    public void joinAsStringStringObject0() {
        assertThat(joinAsString(", ")).isEqualTo("");
    }

    @Test
    public void joinAsStringStringObject1() {
        assertThat(joinAsString(", ", 1)).isEqualTo("1");
    }

    @Test
    public void joinAsStringStringObject2() {
        assertThat(joinAsString(", ", 1, 2)).isEqualTo("1, 2");
    }

    @Test
    public void joinAsStringStringObject3() {
        assertThat(joinAsString(", ", 1, 2, 3)).isEqualTo("1, 2, 3");
    }

    @Test
    public void joinAsStringStringIterable0() {
        assertThat(joinAsString(", ", Collections.emptyList())).isEqualTo("");
    }

    @Test
    public void joinAsStringStringIterable1() {
        assertThat(joinAsString(", ", Collections.singletonList(1))).isEqualTo("1");
    }

    @Test
    public void joinAsStringStringIterable2() {
        assertThat(joinAsString(", ", Arrays.asList(1, 2))).isEqualTo("1, 2");
    }

    @Test
    public void joinAsStringStringIterable3() {
        assertThat(joinAsString(", ", Arrays.asList(1, 2, 3))).isEqualTo("1, 2, 3");
    }

    enum MyAction {
        action1, action2, mIxEdCase
    }

    @DataProvider
    public Object[][] actionNames() {
        return new Object[][] {
                // @formatter:off
                { "action1", MyAction.action1 },
                { "action2", MyAction.action2 },
                { "mixedcase", MyAction.mIxEdCase },

                // @formatter:on
        };
    }

    @Test(dataProvider = "actionNames")
    public void testAsEnum(final String value, final MyAction action) {
        assertThat(asEnum(value, MyAction.class)).isEqualTo(action);
    }

    @Test
    public void testActionRequestAsEnumNullValue() {
        assertThat(asEnum(null, MyAction.class)).isNull();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testActionRequestAsEnumBadEnumType() {
        asEnum("badAction", MyAction.class);
    }
}
