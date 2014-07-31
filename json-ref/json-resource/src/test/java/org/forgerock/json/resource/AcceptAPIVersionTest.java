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
        AcceptAPIVersion acceptAPIVersion = AcceptAPIVersion
                .newBuilder(null)
                .build();

        // Then
        assertNull(acceptAPIVersion.getProtocolVersion());
        assertNull(acceptAPIVersion.getResourceVersion());
    }

    @Test
    public void handlesEmptyString() {
        // Given
        AcceptAPIVersion acceptAPIVersion = AcceptAPIVersion
                .newBuilder("")
                .build();

        // Then
        assertNull(acceptAPIVersion.getProtocolVersion());
        assertNull(acceptAPIVersion.getResourceVersion());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void handlesInvalidFormat() {
        // Given
        AcceptAPIVersion
                .newBuilder("someInvalidString");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void unknownVersionType() {
        // Given
        AcceptAPIVersion
                .newBuilder("unknownType=1.0");
    }

    @Test
    public void validVersionStringSingleValue() {
        // Given
        AcceptAPIVersion acceptAPIVersion = AcceptAPIVersion
                .newBuilder("resource=2.1")
                .build();

        // Then
        assertNotNull(acceptAPIVersion);
        assertNull(acceptAPIVersion.getProtocolVersion());
        assertEquals(acceptAPIVersion.getResourceVersion(), resourceVersion);
    }

    @Test
    public void validVersionStringMultipleValues() {
        // Given
        AcceptAPIVersion acceptAPIVersion = AcceptAPIVersion
                .newBuilder("protocol=1.0,resource=2.1")
                .build();

        // Then
        assertNotNull(acceptAPIVersion);
        assertEquals(acceptAPIVersion.getProtocolVersion(), protocolVersion);
        assertEquals(acceptAPIVersion.getResourceVersion(), resourceVersion);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void handlesToManyVersionStrings() {
        // Given
        AcceptAPIVersion
                .newBuilder("protocol=1.0,resource=2.1,resource=3.2");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void invalidDelimiter() {
        // Given
        AcceptAPIVersion
                .newBuilder("resource=2.1;protocol=1.0");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void invalidVersionSchema() {
        // Given
        AcceptAPIVersion
                .newBuilder("resource=1.2.3");
    }

    @Test
    public void defaultsDoNotOverride() {
        // Given
        AcceptAPIVersion acceptAPIVersion = AcceptAPIVersion
                .newBuilder("protocol=1.0,resource=2.1")
                .withDefaultProtocolVersion(Version.valueOf(2, 0))
                .withDefaultProtocolVersion("3.0")
                .withDefaultResourceVersion(Version.valueOf(3, 1))
                .withDefaultResourceVersion("4.1")
                .build();

        // Then
        assertNotNull(acceptAPIVersion);
        assertEquals(acceptAPIVersion.getProtocolVersion(), protocolVersion);
        assertEquals(acceptAPIVersion.getResourceVersion(), resourceVersion);
    }

    @Test
    public void defaultsOnlyConstruction() {
        // Given
        AcceptAPIVersion acceptAPIVersion = AcceptAPIVersion
                .newBuilder()
                .withDefaultProtocolVersion("1.0")
                .withDefaultResourceVersion("2.1")
                .build();

        // Then
        assertNotNull(acceptAPIVersion);
        assertEquals(acceptAPIVersion.getProtocolVersion(), protocolVersion);
        assertEquals(acceptAPIVersion.getResourceVersion(), resourceVersion);
    }

    @Test
    public void expectsSatisfiedWhenVersionsPresent() {
        // Given
        AcceptAPIVersion acceptAPIVersion = AcceptAPIVersion
                .newBuilder("protocol=1.0,resource=2.1")
                .expectsProtocolVersion()
                .expectsResourceVersion()
                .build();

        // Then
        assertNotNull(acceptAPIVersion);
        assertEquals(acceptAPIVersion.getProtocolVersion(), protocolVersion);
        assertEquals(acceptAPIVersion.getResourceVersion(), resourceVersion);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void expectsFailsNoProtocol() {
        // Given
        AcceptAPIVersion
                .newBuilder("resource=2.1")
                .expectsResourceVersion()
                .expectsProtocolVersion()
                .build();
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void expectsFailsNoResource() {
        // Given
        AcceptAPIVersion
                .newBuilder("protocol=1.0")
                .expectsProtocolVersion()
                .expectsResourceVersion()
                .build();
    }

    @Test
    public void objectEquality() {
        // Given
        AcceptAPIVersion acceptAPIVersion1 = AcceptAPIVersion
                .newBuilder("protocol=1.0,resource=2.1")
                .build();

        AcceptAPIVersion acceptAPIVersion2 = AcceptAPIVersion
                .newBuilder("protocol=1.0,resource=2.1")
                .build();

        // Then
        assertEquals(acceptAPIVersion1, acceptAPIVersion2);
        assertEquals(acceptAPIVersion1.hashCode(), acceptAPIVersion2.hashCode());
    }

}
