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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.selfservice.stages.email;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Arrays;
import java.util.HashSet;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.StageResponse;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link VerifyUserIdStage}.
 *
 * @since 0.2.0
 */
public final class VerifyUserIdStageTest {

    private VerifyUserIdStage verifyUserIdStage;
    @Mock
    private ProcessContext context;
    @Mock
    private ConnectionFactory factory;
    @Mock
    private Connection connection;

    private VerifyUserIdConfig config;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        config = newVerifyUserIdConfig(new EmailAccountConfig());
        verifyUserIdStage = new VerifyUserIdStage(factory);
    }

    @Test
    public void testGatherInitialRequirements() throws Exception {
        // When
        JsonValue jsonValue = verifyUserIdStage.gatherInitialRequirements(context, config);

        // Then
        assertThat(jsonValue).stringAt("description").isEqualTo("Verify your user account");
        assertThat(jsonValue).stringAt("properties/username/description").isEqualTo("Username");
    }

    @Test (expectedExceptions = BadRequestException.class, expectedExceptionsMessageRegExp = "username is missing")
    public void testGetEmailAddressWithoutUsername() throws Exception {
        // Given
        given(context.getInput()).willReturn(newEmptyJsonValue());
        StageResponse.Builder builder = StageResponse.newBuilder();

        // When
        verifyUserIdStage.getEmailAddress(context, config, builder);
    }

    @Test (expectedExceptions = BadRequestException.class,
            expectedExceptionsMessageRegExp = "Unable to find associated account")
    public void testGetEmailAddressInvalidUsername() throws Exception {
        // Given
        given(context.getInput()).willReturn(newJsonValueWithUsername());
        StageResponse.Builder builder = StageResponse.newBuilder();
        given(factory.getConnection()).willReturn(connection);

        // When
        verifyUserIdStage.getEmailAddress(context, config, builder);
    }

    private VerifyUserIdConfig newVerifyUserIdConfig(EmailAccountConfig emailConfig) {
        return new VerifyUserIdConfig(emailConfig)
            .setQueryFields(new HashSet<>(Arrays.asList("_id", "mail")))
            .setIdentityIdField("_id")
            .setIdentityEmailField("mail")
            .setIdentityServiceUrl("/users");
    }

    private JsonValue newEmptyJsonValue() {
        return json(object());
    }

    private JsonValue newJsonValueWithUsername() {
        return json(object(field("username", "user1")));
    }
}

