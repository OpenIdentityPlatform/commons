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

package org.forgerock.api.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.forgerock.http.routing.Version.version;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import org.forgerock.api.ApiValidationException;
import org.forgerock.http.routing.Version;
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
                .mvccSupported(true)
                .build();
        resourceV2 = Resource.resource()
                .action(action1)
                .action(action2)
                .mvccSupported(true)
                .build();
    }

    @DataProvider(name = "putValidationData")
    public Object[][] putValidationData() {
        return new Object[][]{
                {null, null, Exception.class},
                {null, resourceV2, NullPointerException.class},
                {version(2), null, NullPointerException.class},
                {version(1), resourceV2, IllegalStateException.class},
                {VersionedPath.UNVERSIONED, resourceV2, ApiValidationException.class},
                {version(2), resourceV2, null},
        };
    }

    @Test(dataProvider = "putValidationData")
    public void testPut(final Version version, final Resource resource,
            final Class<? extends Throwable> expectedException) {
        final VersionedPath.Builder builder = VersionedPath.versionedPath();

        // add a version, so that we can test for version-uniqueness
        builder.put(version(1), resourceV1);

        final VersionedPath versionedPath;
        try {
            builder.put(version, resource);
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

        assertThat(versionedPath.get(version(2))).isNotNull();
        assertThat(versionedPath.getVersions()).contains(version(2), version(1));
        assertThat(versionedPath.getPaths()).isNotEmpty();
    }

    @Test(expectedExceptions = ApiValidationException.class)
    public void testEmptyVersionedPath() {
        VersionedPath.versionedPath().build();
    }
}
