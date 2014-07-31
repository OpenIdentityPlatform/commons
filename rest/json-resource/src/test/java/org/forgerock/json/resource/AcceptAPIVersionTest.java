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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

/**
 * Unit test for {@link AcceptAPIVersion}.
 *
 * @since 2.4.0
 */
public class AcceptAPIVersionTest {

    private Version protocolVersion;
    private Version resourceVersion;

    @BeforeMethod
    public void setUp() {
        protocolVersion = Version.valueOf("1.0");
        resourceVersion = Version.valueOf("2.1");
    }

    @Test
    public void nullInstanceWithBlankString() {
        // Given
        AcceptAPIVersion.Builder builder = new AcceptAPIVersion.Builder();
        AcceptAPIVersion acceptAPIVersion = builder
                .parseVersionString(null)
                .build();

        // Then
        assertNull(acceptAPIVersion.getProtocolVersion());
        assertNull(acceptAPIVersion.getResourceVersion());
    }

    @Test
    public void handlesEmptyString() {
        // Given
        AcceptAPIVersion.Builder builder = new AcceptAPIVersion.Builder();
        AcceptAPIVersion acceptAPIVersion = builder
                .parseVersionString("")
                .build();

        // Then
        assertNull(acceptAPIVersion.getProtocolVersion());
        assertNull(acceptAPIVersion.getResourceVersion());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void handlesInvalidFormat() {
        // Given
        AcceptAPIVersion.Builder builder = new AcceptAPIVersion.Builder();
        builder.parseVersionString("someInvalidString");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void unknownVersionType() {
        // Given
        AcceptAPIVersion.Builder builder = new AcceptAPIVersion.Builder();
        builder.parseVersionString("unknownType=1.0");
    }

    @Test
    public void validVersionStringSingleValue() {
        // Given
        AcceptAPIVersion.Builder builder = new AcceptAPIVersion.Builder();
        AcceptAPIVersion acceptAPIVersion = builder
                .parseVersionString("resource=2.1")
                .build();

        // Then
        assertNotNull(acceptAPIVersion);
        assertNull(acceptAPIVersion.getProtocolVersion());
        assertEquals(acceptAPIVersion.getResourceVersion(), resourceVersion);
    }

    @Test
    public void validVersionStringMultipleValues() {
        // Given
        AcceptAPIVersion.Builder builder = new AcceptAPIVersion.Builder();
        AcceptAPIVersion acceptAPIVersion = builder
                .parseVersionString("protocol=1.0,resource=2.1")
                .build();

        // Then
        assertNotNull(acceptAPIVersion);
        assertEquals(acceptAPIVersion.getProtocolVersion(), protocolVersion);
        assertEquals(acceptAPIVersion.getResourceVersion(), resourceVersion);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void handlesToManyVersionStrings() {
        // Given
        AcceptAPIVersion.Builder builder = new AcceptAPIVersion.Builder();
        builder.parseVersionString("protocol=1.0,resource=2.1,resource=3.2");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void invalidDelimiter() {
        // Given
        AcceptAPIVersion.Builder builder = new AcceptAPIVersion.Builder();
        builder.parseVersionString("resource=2.1;protocol=1.0");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void invalidVersionSchema() {
        // Given
        AcceptAPIVersion.Builder builder = new AcceptAPIVersion.Builder();
        builder.parseVersionString("resource=1.2.3");
    }

    @Test
    public void objectEquality() {
        // Given
        AcceptAPIVersion.Builder builder1 = new AcceptAPIVersion.Builder();
        AcceptAPIVersion acceptAPIVersion1 = builder1
                .parseVersionString("protocol=1.0,resource=2.1")
                .build();

        AcceptAPIVersion.Builder builder2 = new AcceptAPIVersion.Builder();
        AcceptAPIVersion acceptAPIVersion2 = builder1
                .parseVersionString("protocol=1.0,resource=2.1")
                .build();

        // Then
        assertEquals(acceptAPIVersion1, acceptAPIVersion2);
        assertEquals(acceptAPIVersion1.hashCode(), acceptAPIVersion2.hashCode());
    }

}