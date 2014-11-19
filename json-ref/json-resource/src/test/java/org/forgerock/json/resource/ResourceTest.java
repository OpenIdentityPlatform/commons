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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.json.resource;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

import org.forgerock.json.fluent.JsonValue;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashMap;

/**
 * Tests {@link Resource}.
 */
@SuppressWarnings("javadoc")
public final class ResourceTest {

    @Test(dataProvider = "resources")
    public void testEquals(final Resource resource) {
        /*
         * The content is ignored since the id and revision should uniquely
         * identify a resource.
         */
        final Resource similar =
                new Resource(resource.getId(), resource.getRevision(), new JsonValue(singletonMap(
                        "ignored", (Object) "content")));
        assertThat(resource).isEqualTo(similar);
    }

    @Test(dataProvider = "differentResources")
    public void testEqualsNotSame(final Resource first, final Resource second) {
        assertThat(first).isNotEqualTo(second);
    }

    @Test(dataProvider = "resources")
    public void testHashCode(final Resource resource) {
        assertThat(resource.hashCode()).isNotEqualTo(0);
    }

    @Test(dataProvider = "differentResources")
    public void testHashCodeNotSame(final Resource first, final Resource second) {
        assertThat(first.hashCode()).isNotEqualTo(second.hashCode());
    }

    @DataProvider
    Object[][] differentResources() {
        return new Object[][] {
            {
                new Resource("751E0000000oRV1IAM", "123", new JsonValue(
                        new HashMap<String, Object>())),
                new Resource("751E0000000oRV1IAM", "124", new JsonValue(
                        new HashMap<String, Object>())) },
            {
                new Resource("751E0000000oRV1IAM", "123", new JsonValue(
                        new HashMap<String, Object>())),
                new Resource("751E0000000oRV1IAN", "123", new JsonValue(
                        new HashMap<String, Object>())) } };
    }

    @DataProvider
    Object[][] resources() {
        return new Object[][] {
            { new Resource("751E0000000oRV1IAM", "123",
                    new JsonValue(new HashMap<String, Object>())) },
            { new Resource("751E0000000oRV1IAM", null, new JsonValue(new HashMap<String, Object>())) },
            { new Resource(null, "123", new JsonValue(new HashMap<String, Object>())) },
            { new Resource(null, null, new JsonValue(new HashMap<String, Object>())) }, };
    }
}
