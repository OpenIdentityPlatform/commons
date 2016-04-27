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
package org.forgerock.util;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.util.Utils.joinAsString;
import static org.forgerock.util.Utils.asEnum;

import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class UtilsTest {

    @DataProvider
    public Object[][] joinAsStringVarargsDataProvider() {
        return new Object[][] {
            { new Object[] {}, "" },
            { new Object[] {1}, "1" },
            { new Object[] {1, 2}, "1, 2" },
            { new Object[] {1, 2, 3}, "1, 2, 3" },
        };
    }

    @Test(dataProvider = "joinAsStringVarargsDataProvider")
    public void joinAsStringVarargs(Object[] data, String expectedResult) {
        assertThat(joinAsString(", ", data)).isEqualTo(expectedResult);
    }

    @Test(dataProvider = "joinAsStringVarargsDataProvider")
    public void joinAsStringBuilderVarargs(Object[] data, String expectedResult) {
        StringBuilder sb = new StringBuilder();
        joinAsString(sb, ", ", data);
        assertThat(sb.toString()).isEqualTo(expectedResult);
    }

    @DataProvider
    public Object[][] joinAsStringIterableDataProvider() {
        return new Object[][] {
            { asList(), "" },
            { asList(1), "1" },
            { asList(1, 2), "1, 2" },
            { asList(1, 2, 3), "1, 2, 3" },
        };
    }

    @Test(dataProvider = "joinAsStringIterableDataProvider")
    public void joinAsStringIterable(List<Integer> data, String expectedResult) {
        assertThat(joinAsString(", ", data)).isEqualTo(expectedResult);
    }

    @Test(dataProvider = "joinAsStringIterableDataProvider")
    public void joinAsStringBuilderVarargs(List<Integer> data, String expectedResult) {
        StringBuilder sb = new StringBuilder();
        joinAsString(sb, ", ", data);
        assertThat(sb.toString()).isEqualTo(expectedResult);
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
