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

import org.forgerock.json.resource.descriptor.Version;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

/**
 * Unit test for {@link ClientVersion}.
 *
 * @since 2.4.0
 */
public class ClientVersionTest {

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
        ClientVersion.Builder builder = new ClientVersion.Builder();
        ClientVersion clientVersion = builder
                .parseVersionString(null)
                .build();

        // Then
        assertNull(clientVersion.getProtocolVersion());
        assertNull(clientVersion.getResourceVersion());
    }

    @Test
    public void handlesEmptyString() {
        // Given
        ClientVersion.Builder builder = new ClientVersion.Builder();
        ClientVersion clientVersion = builder
                .parseVersionString("")
                .build();

        // Then
        assertNull(clientVersion.getProtocolVersion());
        assertNull(clientVersion.getResourceVersion());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void handlesInvalidFormat() {
        // Given
        ClientVersion.Builder builder = new ClientVersion.Builder();
        builder.parseVersionString("someInvalidString");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void unknownVersionType() {
        // Given
        ClientVersion.Builder builder = new ClientVersion.Builder();
        builder.parseVersionString("unknownType=1.0");
    }

    @Test
    public void validVersionStringSingleValue() {
        // Given
        ClientVersion.Builder builder = new ClientVersion.Builder();
        ClientVersion clientVersion = builder
                .parseVersionString("resource=2.1")
                .build();

        // Then
        assertNotNull(clientVersion);
        assertNull(clientVersion.getProtocolVersion());
        assertEquals(clientVersion.getResourceVersion(), resourceVersion);
    }

    @Test
    public void validVersionStringMultipleValues() {
        // Given
        ClientVersion.Builder builder = new ClientVersion.Builder();
        ClientVersion clientVersion = builder
                .parseVersionString("protocol=1.0,resource=2.1")
                .build();

        // Then
        assertNotNull(clientVersion);
        assertEquals(clientVersion.getProtocolVersion(), protocolVersion);
        assertEquals(clientVersion.getResourceVersion(), resourceVersion);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void handlesToManyVersionStrings() {
        // Given
        ClientVersion.Builder builder = new ClientVersion.Builder();
        builder.parseVersionString("protocol=1.0,resource=2.1,resource=3.2");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void invalidDelimiter() {
        // Given
        ClientVersion.Builder builder = new ClientVersion.Builder();
        builder.parseVersionString("resource=2.1;protocol=1.0");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void invalidVersionSchema() {
        // Given
        ClientVersion.Builder builder = new ClientVersion.Builder();
        builder.parseVersionString("resource=1.2.3");
    }

}