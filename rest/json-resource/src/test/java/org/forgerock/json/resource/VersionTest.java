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

package org.forgerock.json.resource;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @since 2.4.0
 */
@SuppressWarnings("javadoc")
public class VersionTest {

    @Test
    public void shouldMatchVersionsWithSameVersionNumbers() {

        //Given
        Version requestedVersion = Version.valueOf(1, 1);

        //When
        Version version = Version.valueOf(1, 1);

        //Then
        assertThat(version.isCompatibleWith(requestedVersion)).isTrue();
    }

    @Test
    public void shouldMatchVersionsWithSameMajorButRequestedVersionMinorVersionNumberLower() {

        //Given
        Version requestedVersion = Version.valueOf(1, 1);

        //When
        Version version = Version.valueOf(1, 5);

        //Then
        assertThat(version.isCompatibleWith(requestedVersion)).isTrue();
    }

    @Test
    public void shouldNotMatchVersionsWithSameMajorButRequestedVersionMinorVersionNumberHigher() {

        //Given
        Version requestedVersion = Version.valueOf(1, 6);

        //When
        Version version = Version.valueOf(1, 5);

        //Then
        assertThat(version.isCompatibleWith(requestedVersion)).isFalse();
    }

    @Test
    public void shouldNotMatchVersionsWithDifferentMajorVersionNumbers() {

        //Given
        Version requestedVersion = Version.valueOf(2, 0);

        //When
        Version version = Version.valueOf(1, 0);

        //Then
        assertThat(version.isCompatibleWith(requestedVersion)).isFalse();
    }

    @DataProvider
    public Object[][] versionStringsTestData() {
        return new Object[][]{
            /* columns are:
               - version string
 	           - boolean indicating whether parsing should throw an exception
 	           - the major number that should result if parsing succeeds
 	           - the minor number that should result if parsing succeeds
 	        */
                {"1.0", false, 1, 0},
                {"10.10", false, 10, 10},
                {"1.30", false, 1, 30},
                {".3", true, -1, -1},
                {"", true, -1, -1},
                {"N.1", true, -1, -1},
                {"1.2.3", true, -1, -1},
                {"1.2.3.4", true, -1, -1},
                {"1.2..4", true, -1, -1},
                {"1.2..", false, 1, 2},
        };
    }

    @Test(dataProvider = "versionStringsTestData")
    public void shouldParseVersionStrings(String versionString, boolean shouldThrowException, int major, int minor) {
        try {
            Version version = Version.valueOf(versionString);

            // if we reach this point, and an exception was expected, we have failed.
            assertThat(shouldThrowException).isFalse();
            assertThat(version.getMajor()).isEqualTo(major);
            assertThat(version.getMinor()).isEqualTo(minor);

        } catch (IllegalArgumentException iae) {
            // if we reach this point, and an exception was not expected, we have failed.
            assertThat(shouldThrowException).isTrue();
        }
    }
}
