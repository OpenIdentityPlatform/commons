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
 * Copyright 2012 ForgeRock AS.
 */

package org.forgerock.json.resource;

import org.forgerock.json.fluent.JsonValue;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@code ContextName}.
 */
@SuppressWarnings("javadoc")
public final class ContextNameTest {

    @Test
    public void testToString() {
        ContextName name = ContextName.valueOf("test");
        assertThat(name.toString()).isEqualTo("test");
    }

    @Test
    public void testEquals() {
        ContextName name = ContextName.valueOf("test");
        ContextName test = ContextName.valueOf("test");
        assertThat(name.equals(test)).isTrue();
    }

    @Test
    public void testNotEquals() {
        ContextName name = ContextName.valueOf("name");
        ContextName test = ContextName.valueOf("test");
        assertThat(name.equals(test)).isFalse();
    }

    @Test
    public void testNotEqualsNull() {
        ContextName name = ContextName.valueOf("name");
        assertThat(name.equals(null)).isFalse();
    }

    @Test
    public void testNotEqualsOtherType() {
        ContextName name = ContextName.valueOf("name");
        assertThat(name.equals(new Integer(5))).isFalse();
    }

    @Test
    public void testHashCode() {
        ContextName name = ContextName.valueOf("test");
        assertThat(name.hashCode()).isEqualTo("test".hashCode());
    }
}
