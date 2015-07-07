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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.json.resource;

import static org.testng.Assert.*;

import org.forgerock.http.context.RootContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link AcceptAPIVersionContext}.
 *
 * @since 2.4.0
 */
@SuppressWarnings("javadoc")
public class AcceptAPIVersionContextTest {

    private AcceptAPIVersion acceptVersion;

    @BeforeMethod
    public void setUp() {
        acceptVersion = AcceptAPIVersion
                .newBuilder()
                .withDefaultProtocolVersion("1.0")
                .withDefaultResourceVersion("2.1")
                .build();
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void rejectsNullProtocolName() {
        // Given
        new AcceptAPIVersionContext(new RootContext(), null, acceptVersion);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void rejectsNullProtocolVersion() {
        // Given
        acceptVersion = AcceptAPIVersion
                .newBuilder()
                .withDefaultResourceVersion("2.1")
                .build();
        new AcceptAPIVersionContext(new RootContext(), "name", acceptVersion);
    }

    @Test
    public void acceptsNullResourceVersion() {
        // Given
        acceptVersion = AcceptAPIVersion
                .newBuilder()
                .withDefaultProtocolVersion("1.0")
                .build();

        AcceptAPIVersionContext context = new AcceptAPIVersionContext(new RootContext(), "name", acceptVersion);

        // Then
        assertNull(context.getResourceVersion());
    }

    @Test
    public void handlesValidVersions() {
        // Given
        RootContext root = new RootContext();
        AcceptAPIVersionContext context = new AcceptAPIVersionContext(root, "name", acceptVersion);

        // Then
        assertNotNull(context.getProtocolVersion());
        assertNotNull(context.getResourceVersion());

        assertEquals(1, context.getProtocolVersion().getMajor());
        assertEquals(0, context.getProtocolVersion().getMinor());

        assertEquals(2, context.getResourceVersion().getMajor());
        assertEquals(1, context.getResourceVersion().getMinor());
    }
}
