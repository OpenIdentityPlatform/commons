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

import org.testng.annotations.Test;

import org.forgerock.api.ApiValidationException;

public class ApiDescriptionTest {

    @Test(expectedExceptions = ApiValidationException.class)
    public void testFailedValidationIdMissing() {
        final ApiError apiError = ApiError.apiError()
                .code(500)
                .description("Unexpected apiError")
                .build();

        final Errors errors = Errors.errors()
                .put("internalServerError", apiError)
                .build();

        final ApiDescription apiDescription = ApiDescription.apiDescription()
                .errors(errors)
                .build();
    }

    @Test
    public void testSuccessfulValidationMinimumRequirements() {
        final ApiDescription apiDescription = ApiDescription.apiDescription()
                .id("frapi:test")
                .version("a version")
                .build();
    }

    @Test(expectedExceptions = ApiValidationException.class)
    public void testValidationMinimumRequirementsMissingId() {
        final ApiDescription apiDescription = ApiDescription.apiDescription()
                .version("a version")
                .build();
    }

    @Test(expectedExceptions = ApiValidationException.class)
    public void testValidationMinimumRequirementsMissingVersion() {
        final ApiDescription apiDescription = ApiDescription.apiDescription()
                .id("frapi:test")
                .build();
    }

}
