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

import org.forgerock.api.ApiValidationException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ReferenceTest {

    @DataProvider(name = "buildValidationData")
    public Object[][] buildValidationData() {
        return new Object[][]{
                {null, ApiValidationException.class},
                {"", ApiValidationException.class},
                {"\t", ApiValidationException.class},
                {"has space", ApiValidationException.class},
                {"frapi:common", null},
        };
    }

    @Test(dataProvider = "buildValidationData")
    public void testBuildValidation(final String value, final Class<? extends Throwable> expectedException) {
        final Reference.Builder builder = Reference.reference();
        final Reference reference;
        try {
            reference = builder.value(value).build();
        } catch (final Exception e) {
            if (expectedException != null) {
                assertThat(e).isInstanceOf(expectedException);
            }
            return;
        }

        if (expectedException != null) {
            failBecauseExceptionWasNotThrown(expectedException);
        }

        assertThat(reference.getValue()).isEqualTo(value);
    }
}
