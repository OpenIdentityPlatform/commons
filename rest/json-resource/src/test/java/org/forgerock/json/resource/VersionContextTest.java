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

import static org.testng.Assert.*;

/**
 * Unit test for {@link VersionContext}.
 *
 * @since 2.4.0
 */
public class VersionContextTest {

    @Test(expectedExceptions = NullPointerException.class)
    public void rejectsNullProtocolName() {
        // Given
        new VersionContext(new RootContext(), null, "2.4.0", "1.0");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void rejectsNullProtocolVersion() {
        // Given
        new VersionContext(new RootContext(), "name", null, "1.0");
    }

    @Test
    public void acceptsNullResourceVersion() {
        // Given
        VersionContext context = new VersionContext(new RootContext(), "name", "2.4.0", null);

        // Then
        assertNull(context.getResourceVersion());
    }

    @Test
    public void handlesValidVersions() {
        // Given
        RootContext root = new RootContext();
        VersionContext context = new VersionContext(root, "name", "123.45", "678.90");

        // Then
        assertNotNull(context.getProtocolVersion());
        assertNotNull(context.getResourceVersion());

        assertEquals(123, context.getProtocolVersion().getMajor());
        assertEquals(45, context.getProtocolVersion().getMinor());

        assertEquals(678, context.getResourceVersion().getMajor());
        assertEquals(90, context.getResourceVersion().getMinor());
    }

}