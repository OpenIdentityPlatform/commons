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
 *      Copyright 2011 ForgeRock AS
 */
package org.forgerock.i18n.maven;

import static org.fest.assertions.Assertions.assertThat;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests the MessagePropertyKey class.
 */
@Test
public final class MessagePropertyKeyTest {

    /**
     * Data provider for {@link #testValueOfInvalidString(String)}.
     *
     * @return Test data.
     */
    @DataProvider(parallel = true)
    public Object[][] invalidMessagePropertyKeyStrings() {
        return new Object[][] { { "" }, { "1" }, { "1A" }, { "message" },
            { "ANOTHER_message" }, { "ANOTHER-MESSAGE" }, { "A MESSAGE" }, };
    }

    /**
     * Tests the {@code valueOf(String s)} method with invalid key strings.
     *
     * @param s
     *            The key string.
     * @throws IllegalArgumentException
     *             The expected exception.
     */
    @Test(dataProvider = "invalidMessagePropertyKeyStrings",
            expectedExceptions = IllegalArgumentException.class)
    public void testValueOfInvalidString(final String s) {
        MessagePropertyKey.valueOf(s);
    }

    /**
     * Tests the {@code valueOf(String s)} method with valid key strings.
     *
     * @param s
     *            The key string.
     * @param name
     *            The expected name.
     * @param ordinal
     *            The expected ordinal.
     */
    @Test(dataProvider = "validMessagePropertyKeyStrings")
    public void testValueOfValidString(final String s, final String name,
            final int ordinal) {
        final MessagePropertyKey key = MessagePropertyKey.valueOf(s);
        assertThat(key.toString()).isEqualTo(s);
        assertThat(key.getName()).isEqualTo(name);
        assertThat(key.getOrdinal()).isEqualTo(ordinal);
    }

    /**
     * Data provider for {@link #testValueOfValidString(String, String, int)}.
     *
     * @return Test data.
     */
    @DataProvider(parallel = true)
    public Object[][] validMessagePropertyKeyStrings() {
        return new Object[][] { { "MESSAGE", "MESSAGE", -1 },
            { "ANOTHER_MESSAGE", "ANOTHER_MESSAGE", -1 },
            { "YET_ANOTHER_MESSAGE", "YET_ANOTHER_MESSAGE", -1 },
            { "M111ABC_ABC_111_XYZ", "M111ABC_ABC_111_XYZ", -1 },
            { "MESSAGE_1", "MESSAGE", 1 },
            { "MESSAGE_123", "MESSAGE", 123 },
            { "ANOTHER_MESSAGE_1", "ANOTHER_MESSAGE", 1 },
            { "ANOTHER_MESSAGE_123", "ANOTHER_MESSAGE", 123 },
            { "YET_ANOTHER_MESSAGE_1", "YET_ANOTHER_MESSAGE", 1 },
            { "YET_ANOTHER_MESSAGE_123", "YET_ANOTHER_MESSAGE", 123 },
            { "M111ABC_ABC_111_XYZ_1", "M111ABC_ABC_111_XYZ", 1 },
            { "M111ABC_ABC_111_XYZ_123", "M111ABC_ABC_111_XYZ", 123 }, };
    }

    /**
     * Data provider for {@link #testIsPresent(String, String, boolean)}.
     *
     * @return Test data.
     */
    @DataProvider(parallel = true)
    public Object[][] isPresent() {
        return new Object[][] {
            { "TEST_MESSAGE", "LocalizableMessage m = TEST_MESSAGE.get();",
              true },
            { "TEST_MESSAGE", "LocalizableMessage m = TEST_MESSAGE", true },
            { "TEST_MESSAGE", "TEST_MESSAGE.get();", true },
            { "TEST_MESSAGE", "TEST_MESSAGE", true },
            { "TEST_MESSAGE",
              "LocalizableMessage m = TEST_MESSAGE1.get();", false },
            { "TEST_MESSAGE",
              "LocalizableMessage m = NOT_TEST_MESSAGE.get();", false }, };
    }

    /**
     * Tests the {@code isPresent} method.
     *
     * @param s
     *            The key string.
     * @param line
     *            The line of text to compare.
     * @param expectedResult
     *            The expected result.
     */
    @Test(dataProvider = "isPresent")
    public void testIsPresent(final String s, final String line,
            final boolean expectedResult) {
        final MessagePropertyKey key = MessagePropertyKey.valueOf(s);
        assertThat(key.isPresent(line)).isEqualTo(expectedResult);
    }

}
