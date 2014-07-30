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

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

/**
 * Unit test for {@link VersionMap}.
 *
 * @since 2.4.0
 */
public class VersionMapTest {

    @Test(expectedExceptions = NullPointerException.class)
    public void nullInstanceWithBlankString() {
        // Given
        VersionMap.valueOf(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void handlesEmptyString() {
        // Given
        VersionMap.valueOf("");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void handlesInvalidFormat() {
        // Given
        VersionMap.valueOf("someRandomString");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void unknownVersionType() {
        // Given
        VersionMap.valueOf("unknownType=1.2");
    }

    @Test
    public void validVersionStringSingleValue() {
        // Given
        VersionMap versionMap = VersionMap.valueOf("resource=1.2");

        // Then
        assertNotNull(versionMap);
        assertNull(versionMap.getVersion(VersionType.PROTOCOL));
        assertEquals(versionMap.getVersion(VersionType.RESOURCE), "1.2");
    }

    @Test
    public void validVersionStringMultipleValues() {
        // Given
        VersionMap versionMap = VersionMap.valueOf("protocol=2.1,resource=1.2");

        // Then
        assertNotNull(versionMap);
        assertEquals(versionMap.getVersion(VersionType.RESOURCE), "1.2");
        assertEquals(versionMap.getVersion(VersionType.PROTOCOL), "2.1");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void invalidDelimiter() {
        // Given
        VersionMap.valueOf("resource=1.2;protocol=2.1");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void invalidVersionSchema() {
        // Given
        VersionMap.valueOf("resource=1.2.3");
    }

}