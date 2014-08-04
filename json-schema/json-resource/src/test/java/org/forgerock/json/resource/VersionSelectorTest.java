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

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class VersionSelectorTest {

    private VersionSelector versionSelector;

    private Version oneDotZero;
    private Version oneDotOne;
    private Version oneDotFive;
    private Version oneDotNine;
    private Version twoDotOne;
    private Version twoDotFive;

    private Object candiateOneDotZero;
    private Object candiateOneDotOne;
    private Object candiateOneDotFive;
    private Object candiateOneDotNine;
    private Object candiateTwoDotOne;
    private Object candiateTwoDotFive;

    private Map<Version, Object> candidates = new HashMap<Version, Object>();

    @BeforeClass
    public void setUp() {
        oneDotZero = Version.valueOf(1, 0);
        oneDotOne = Version.valueOf(1, 1);
        oneDotFive = Version.valueOf(1, 5);
        oneDotNine = Version.valueOf(1, 9);
        twoDotOne = Version.valueOf(2, 1);
        twoDotFive = Version.valueOf(2, 5);

        candiateOneDotZero = mock(Object.class);
        candiateOneDotOne = mock(Object.class);
        candiateOneDotFive = mock(Object.class);
        candiateOneDotNine = mock(Object.class);
        candiateTwoDotOne = mock(Object.class);
        candiateTwoDotFive = mock(Object.class);

        candidates.put(oneDotZero, candiateOneDotZero);
        candidates.put(oneDotOne, candiateOneDotOne);
        candidates.put(oneDotFive, candiateOneDotFive);
        candidates.put(oneDotNine, candiateOneDotNine);
        candidates.put(twoDotOne, candiateTwoDotOne);
        candidates.put(twoDotFive, candiateTwoDotFive);
    }

    @BeforeMethod
    public void setUpMethod() {
        versionSelector = new VersionSelector();
    }

    @Test (expectedExceptions = ResourceException.class)
    public void selectShouldThrowVersionSelectionExceptionWhenCandidatesNull() throws Exception {

        //Given
        Version requested = Version.valueOf(1, 0);

        //When
        versionSelector.select(requested, null);

        //Then
        //Expected VersionSelectionException
    }

    @Test (expectedExceptions = ResourceException.class)
    public void selectShouldThrowVersionSelectionExceptionWhenCandidatesEmpty() throws Exception {

        //Given
        Version requested = Version.valueOf(1, 0);
        Map<Version, Object> candidates = Collections.emptyMap();

        //When
        versionSelector.select(requested, candidates);

        //Then
        //Expected VersionSelectionException
    }

    @Test
    public void selectShouldReturnLatestWhenRequestVersionIsNullByDefault() throws Exception {

        //Given

        //When
        Object selected = versionSelector.select(null, candidates);

        //Then
        assertThat(selected).isEqualTo(candiateTwoDotFive);
    }

    @Test
    public void selectShouldReturnOldestWhenRequestVersionIsNullWhenOldestBehaviourSet() throws Exception {

        //Given
        versionSelector.defaultToOldest();

        //When
        Object selected = versionSelector.select(null, candidates);

        //Then
        assertThat(selected).isEqualTo(candiateOneDotZero);
    }

    @Test
    public void selectShouldReturnObjectWhenMatchFound() throws ResourceException {

        //Given
        Version requested = Version.valueOf(1, 2);

        //When
        Object selected = versionSelector.select(requested, candidates);

        //Then
        assertThat(selected).isEqualTo(candiateOneDotNine);
    }

    @Test (expectedExceptions = ResourceException.class)
    public void selectShouldThrowVersionSelectionExceptionWhenVersionNotMatched() throws ResourceException {

        //Given
        Version requested = Version.valueOf(2, 6);

        //When
        Object selected = versionSelector.select(requested, candidates);

        //Then
        assertThat(selected).isEqualTo(candiateOneDotFive);
    }
}
