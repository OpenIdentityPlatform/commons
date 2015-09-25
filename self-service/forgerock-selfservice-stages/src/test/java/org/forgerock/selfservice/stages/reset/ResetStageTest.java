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
package org.forgerock.selfservice.stages.reset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.selfservice.stages.CommonStateFields.USER_ID_FIELD;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.services.context.Context;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link ResetStage}.
 *
 * @since 0.2.0
 */
public final class ResetStageTest {

    private static final String TEST_EMAIL_ID = "test@forgerock.com";

    private ResetStage resetStage;
    @Mock
    private ProcessContext context;

    private ResetStageConfig config;
    @Mock
    private ConnectionFactory factory;
    @Mock
    private Connection connection;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        config = newResetStageConfig();
        resetStage = new ResetStage(factory);
    }

    @Test
    public void testGatherInitialRequirements() throws Exception {
        // Given
        given(context.containsState(USER_ID_FIELD)).willReturn(true);
        given(context.getState(USER_ID_FIELD)).willReturn(new JsonValue("testUser"));

        // When
        JsonValue jsonValue = resetStage.gatherInitialRequirements(context, config);

        // Then
        assertThat(jsonValue).stringAt("description").isEqualTo("Reset password");
        assertThat(jsonValue).stringAt("properties/password/description").isEqualTo("Password");
    }

    @Test (expectedExceptions = BadRequestException.class,
        expectedExceptionsMessageRegExp = "password is missing from input")
    public void testAdvancePasswordNotSpecified() throws Exception {
        // Given
        given(context.containsState(USER_ID_FIELD)).willReturn(true);
        given(context.getState(USER_ID_FIELD)).willReturn(new JsonValue("testUser"));
        given(context.getInput()).willReturn(newJsonValueWithUserId());

        // When
        resetStage.advance(context, config);
    }

    @Test
    public void testAdvance() throws Exception {
        // Given
        given(context.containsState(USER_ID_FIELD)).willReturn(true);
        given(context.getState(USER_ID_FIELD)).willReturn(new JsonValue("testUser"));
        given(context.getInput()).willReturn(newJsonValueWithAllInputs());
        given(factory.getConnection()).willReturn(connection);

        // When
        resetStage.advance(context, config);

        // Then
        ArgumentCaptor<PatchRequest> patchRequestArgumentCaptor =  ArgumentCaptor.forClass(PatchRequest.class);
        verify(connection).patch(any(Context.class), patchRequestArgumentCaptor.capture());
        PatchRequest createRequest = patchRequestArgumentCaptor.getValue();

        PatchOperation patchOperation = createRequest.getPatchOperations().get(0);
        assertThat(patchOperation.getOperation()).isEqualTo("replace");
        assertThat(patchOperation.getField().leaf()).isEqualTo("password");
        assertThat(patchOperation.getValue().asString()).isEqualTo("testUserPassword");
        assertThat(createRequest.getResourcePath()).isEqualTo("users/testUser");
    }

    private ResetStageConfig newResetStageConfig() {
        return new ResetStageConfig()
                .setIdentityServiceUrl("/users")
                .setIdentityPasswordField("password");
    }

    private JsonValue newJsonValueWithUserId() {
        return json(object(field("userId", TEST_EMAIL_ID)));
    }

    private JsonValue newJsonValueWithAllInputs() {
        return json(
                object(
                        field("userId", TEST_EMAIL_ID) ,
                        field("password", "testUserPassword")));
    }

}
