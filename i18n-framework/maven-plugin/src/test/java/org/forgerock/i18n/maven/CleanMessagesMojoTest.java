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
 * Tests the CleanMessagesMojo class.
 */
@Test
public final class CleanMessagesMojoTest {

    /**
     * Data provider for {@link #testIsContinuedOnNextLine}.
     *
     * @return Test data.
     */
    @DataProvider(parallel = true)
    public Object[][] isContinuedOnNextLine() {
        return new Object[][] { { "a line with no continuation", false },
            { "a line with continuation\\", true },
            { "a line with no continuation\\\\", false },
            { "a line with continuation\\\\\\", true }, };
    }

    /**
     * Tests isContinuedOnNextLine.
     *
     * @param s
     *            The line.
     * @param isContinuedOnNextLine
     *            The expected result.
     */
    @Test(dataProvider = "isContinuedOnNextLine")
    public void testIsContinuedOnNextLine(final String s,
            final boolean isContinuedOnNextLine) {
        CleanMessagesMojo m = new CleanMessagesMojo();

        assertThat(m.isContinuedOnNextLine(s)).isEqualTo(isContinuedOnNextLine);
    }

}
