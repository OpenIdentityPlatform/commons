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
 * Unit test for {@link ClientVersionContext}.
 *
 * @since 2.4.0
 */
public class ClientVersionContextTest {

    private Version protocolVersion;
    private Version resourceVersion;

    @BeforeMethod
    public void setUp() {
        protocolVersion = Version.valueOf(1, 0);
        resourceVersion = Version.valueOf(2, 1);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void rejectsNullProtocolName() {
        // Given
        new ClientVersionContext(new RootContext(), null, protocolVersion, resourceVersion);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void rejectsNullProtocolVersion() {
        // Given
        new ClientVersionContext(new RootContext(), "name", null, resourceVersion);
    }

    @Test
    public void acceptsNullResourceVersion() {
        // Given
        ClientVersionContext context = new ClientVersionContext(new RootContext(), "name", protocolVersion, null);

        // Then
        assertNull(context.getResourceVersion());
    }

    @Test
    public void handlesValidVersions() {
        // Given
        RootContext root = new RootContext();
        ClientVersionContext context = new ClientVersionContext(root, "name", protocolVersion, resourceVersion);

        // Then
        assertNotNull(context.getProtocolVersion());
        assertNotNull(context.getResourceVersion());

        assertEquals(1, context.getProtocolVersion().getMajor());
        assertEquals(0, context.getProtocolVersion().getMinor());

        assertEquals(2, context.getResourceVersion().getMajor());
        assertEquals(1, context.getResourceVersion().getMinor());
    }

}