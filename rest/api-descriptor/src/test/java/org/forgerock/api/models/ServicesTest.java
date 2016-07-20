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

import static org.assertj.core.api.Assertions.*;
import static org.forgerock.api.models.Read.*;
import static org.forgerock.api.models.Resource.*;
import static org.forgerock.api.models.Services.services;

import org.forgerock.util.i18n.LocalizableString;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ServicesTest {


    private static final Resource SERVICE = resource().title(new LocalizableString("Title")).description(
            new LocalizableString("Desc"))
            .read(read().description("read").build()).mvccSupported(true).build();

    private static final Resource OTHER_EQUAL_SERVICE = resource().title(new LocalizableString("Title")).description(
            new LocalizableString("Desc"))
            .read(read().description("read").build()).mvccSupported(true).build();

    private static final Resource OTHER_NON_EQUAL_SERVICE = resource().title(new LocalizableString("Different Title"))
            .description(new LocalizableString("Desc"))
            .read(read().description("read").build()).mvccSupported(true).build();

    @DataProvider(name = "putValidationData")
    public Object[][] putValidationData() {
        return new Object[][]{
                {null, null, Exception.class},
                {null, SERVICE, IllegalArgumentException.class},
                {"", SERVICE, IllegalArgumentException.class},
                {"\t", SERVICE, IllegalArgumentException.class},
                {"contains space", SERVICE, IllegalArgumentException.class},
                {"uniqueName", null, NullPointerException.class},
                {"notUniqueName", SERVICE, null},
                {"notUniqueName", OTHER_EQUAL_SERVICE, null},
                {"notUniqueName", OTHER_NON_EQUAL_SERVICE, IllegalStateException.class},
                {"uniqueName", SERVICE, null},
        };
    }

    @Test(dataProvider = "putValidationData")
    public void testPut(final String name, final Resource resource,
                        final Class<? extends Throwable> expectedException) {
        final Services.Builder builder = services();

        // add a resource, so that we can test for name-uniqueness (resource values do NOT need to be unique)
        builder.put("notUniqueName", SERVICE);

        final Services services;
        try {
            builder.put(name, resource);
            services = builder.build();
        } catch (final Exception e) {
            if (expectedException != null) {
                assertThat(e).isInstanceOf(expectedException);
                return;
            }
            throw e;
        }

        if (expectedException != null) {
            failBecauseExceptionWasNotThrown(expectedException);
        }

        assertThat(services.get(name)).isNotNull();
        assertThat(services.getNames()).contains(name, "notUniqueName");
        assertThat(services.getServices()).isNotEmpty();
    }

    @Test
    public void testValidWhenEmpty() {
        services().build();
    }

}
