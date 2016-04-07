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
 * Copyright 2016 ForgeRock AS.
 */

package com.forgerock.api.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import com.forgerock.api.ApiValidationException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class VersionedPathTest {

    private Resource resourceV1;
    private Resource resourceV2;

    @BeforeClass
    public void beforeClass() {
        final Schema responseSchema = Schema.schema()
                .schema(json(object()))
                .build();

        final Action action1 = Action.action()
                .name("action1")
                .response(responseSchema)
                .build();
        final Action action2 = Action.action()
                .name("action2")
                .response(responseSchema)
                .build();

        resourceV1 = Resource.resource()
                .action(action1)
                .build();
        resourceV2 = Resource.resource()
                .action(action1)
                .action(action2)
                .build();
    }

    @DataProvider(name = "putValidationData")
    public Object[][] putValidationData() {
        return new Object[][]{
                {null, null, Exception.class},
                {null, resourceV2, IllegalArgumentException.class},
                {"", resourceV2, IllegalArgumentException.class},
                {"\t", resourceV2, IllegalArgumentException.class},
                {"contains space", resourceV2, IllegalArgumentException.class},
                {"2.0", null, NullPointerException.class},
                {"1.0", resourceV2, IllegalStateException.class},
                {"2.0", resourceV2, null},
        };
    }

    @Test(dataProvider = "putValidationData")
    public void testPut(final String name, final Resource resource,
            final Class<? extends Throwable> expectedException) {
        final VersionedPath.Builder builder = VersionedPath.versionedPath();

        // add a version, so that we can test for version-uniqueness
        builder.put("1.0", resourceV1);

        final VersionedPath versionedPath;
        try {
            builder.put(name, resource);
            versionedPath = builder.build();
        } catch (final Exception e) {
            if (expectedException != null) {
                assertThat(e).isInstanceOf(expectedException);
            }
            return;
        }

        if (expectedException != null) {
            failBecauseExceptionWasNotThrown(expectedException);
        }

        assertThat(versionedPath.get("2.0")).isNotNull();
        assertThat(versionedPath.getVersions()).contains("2.0", "1.0");
        assertThat(versionedPath.getPaths()).isNotEmpty();
    }

    @Test(expectedExceptions = ApiValidationException.class)
    public void testEmptyVersionedPath() {
        VersionedPath.versionedPath().build();
    }
}
