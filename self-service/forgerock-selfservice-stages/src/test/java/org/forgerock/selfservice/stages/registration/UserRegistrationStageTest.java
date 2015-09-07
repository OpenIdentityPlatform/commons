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
package org.forgerock.selfservice.stages.registration;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.selfservice.stages.CommonStateFields.EMAIL_FIELD;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import org.assertj.core.api.Assertions;
import org.forgerock.http.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.StageResponse;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link UserRegistrationStage}.
 *
 * @since 0.2.0
 */
public class UserRegistrationStageTest {

    private static final String TEST_EMAIL_ID = "test@forgerock.com";

    private UserRegistrationStage userRegistrationStage;
    @Mock
    private ProcessContext context;

    private UserRegistrationConfig config;
    @Mock
    private ConnectionFactory factory;
    @Mock
    private Connection connection;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        config = newUserRegistrationConfig();
        userRegistrationStage = new UserRegistrationStage(factory);
    }

    @Test
    public void testGatherInitialRequirements() throws Exception {
        // Given
        given(context.containsState(EMAIL_FIELD)).willReturn(true);
        given(context.getState(EMAIL_FIELD)).willReturn(new JsonValue(TEST_EMAIL_ID));

        // When
        JsonValue jsonValue = userRegistrationStage.gatherInitialRequirements(context, config);

        // Then
        assertThat(jsonValue).stringAt("description").isEqualTo("New user details");
        assertThat(jsonValue).stringAt("properties/userId/description").isEqualTo("New user Id");
        assertThat(jsonValue).stringAt("properties/user/description").isEqualTo("User details");
    }

    @Test (expectedExceptions = BadRequestException.class,
        expectedExceptionsMessageRegExp = "userId has not been specified")
    public void testAdvanceUserIdNotSpecified() throws Exception {
        // Given
        given(context.getInput()).willReturn(newEmptyJsonValue());

        // When
        StageResponse stageResponse = userRegistrationStage.advance(context, config);
    }

    @Test (expectedExceptions = BadRequestException.class,
        expectedExceptionsMessageRegExp = "user has not been specified")
    public void testAdvanceUserNotSpecified() throws Exception {
        // Given
        given(context.getInput()).willReturn(newJsonValueWithUserId());

        // When
        StageResponse stageResponse = userRegistrationStage.advance(context, config);
    }

    @Test
    public void testAdvance() throws Exception {
        // Given
        given(context.getInput()).willReturn(newJsonValueWithAllInputs());
        given(context.getState(EMAIL_FIELD)).willReturn(json(TEST_EMAIL_ID));
        given(factory.getConnection()).willReturn(connection);

        // When
        StageResponse stageResponse = userRegistrationStage.advance(context, config);

        // Then
        ArgumentCaptor<CreateRequest> createRequestArgumentCaptor =  ArgumentCaptor.forClass(CreateRequest.class);
        verify(connection).create(any(Context.class), createRequestArgumentCaptor.capture());
        CreateRequest createRequest = createRequestArgumentCaptor.getValue();

        assertThat(createRequest.getContent()).stringAt("givenName").isEqualTo("testUser");
        assertThat(createRequest.getContent()).stringAt("sn").isEqualTo("testUserSecondName");
        assertThat(createRequest.getContent()).stringAt("password").isEqualTo("passwordTobeEncrypted");
        assertThat(createRequest.getContent()).stringAt("mail").isEqualTo(TEST_EMAIL_ID);
        Assertions.assertThat(createRequest.getNewResourceId()).isEqualTo(TEST_EMAIL_ID);
        Assertions.assertThat(createRequest.getResourcePath()).isEqualTo("users");
    }

    private UserRegistrationConfig newUserRegistrationConfig() {
        return new UserRegistrationConfig()
            .setIdentityServiceUrl("/users")
            .setIdentityEmailField("mail");
    }

    private JsonValue newEmptyJsonValue() {
        return json(object());
    }

    private JsonValue newJsonValueWithUserId() {
        return json(object(field("userId", TEST_EMAIL_ID)));
    }

    private JsonValue newJsonValueWithAllInputs() {
        return json(
            object(
                field("userId", TEST_EMAIL_ID)
                , field("user", object(
                    field("givenName", "testUser"),
                    field("sn", "testUserSecondName"),
                    field("password", "passwordTobeEncrypted")
                ))
            )
        );
    }
}
