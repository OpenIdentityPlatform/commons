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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.selfservice.stages.CommonStateFields.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.services.context.Context;
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
public final class UserRegistrationStageTest {

    private static final String TEST_EMAIL_ID = "test@forgerock.com";
    private static final String KBA_QUESTION_2 = "Who was your first employer?";
    private static final String KBA_QUESTION_3 = "What is my favorite author?";

    private UserRegistrationStage userRegistrationStage;
    @Mock
    private ProcessContext context;

    private UserRegistrationConfig config;
    @Mock
    private ConnectionFactory factory;
    @Mock
    private Connection connection;
    @Mock
    private ResourceResponse queryResponse;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        config = newUserRegistrationConfig();
        userRegistrationStage = new UserRegistrationStage(factory);
    }

    @Test (expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "User registration stage expects user in the context")
    public void testGatherInitialRequirementsNoUserState() throws Exception {
        // Given
        given(context.getState(USER_FIELD)).willReturn(null);

        // When
        userRegistrationStage.gatherInitialRequirements(context, config);
    }

    @Test
    public void testGatherInitialRequirements() throws Exception {
        // Given
        given(context.containsState(USER_FIELD)).willReturn(true);
        given(context.containsState(USER_ID_FIELD)).willReturn(true);
        given(context.getState(USER_FIELD)).willReturn(newEmptyJsonValue());
        given(context.getState(USER_ID_FIELD)).willReturn(newEmptyJsonValue());

        // When
        JsonValue jsonValue = userRegistrationStage.gatherInitialRequirements(context, config);

        // Then
        assertThat(jsonValue).isEmpty();
    }

    @Test
    public void testAdvance() throws Exception {
        // Given
        given(context.getState(USER_FIELD)).willReturn(newJsonValueUserAndKba());
        given(context.getState(USER_ID_FIELD)).willReturn(newJsonValueEmailId());

        given(context.getInput()).willReturn(newJsonValueInputUser());
        given(context.getState(EMAIL_FIELD)).willReturn(newJsonValueEmailId());
        given(factory.getConnection()).willReturn(connection);

        // When
        userRegistrationStage.advance(context, config);

        // Then
        ArgumentCaptor<CreateRequest> createRequestArgumentCaptor =  ArgumentCaptor.forClass(CreateRequest.class);
        verify(connection).create(any(Context.class), createRequestArgumentCaptor.capture());
        CreateRequest createRequest = createRequestArgumentCaptor.getValue();

        assertThat(createRequest.getContent()).stringAt("givenName").isEqualTo("testUser");
        assertThat(createRequest.getContent()).stringAt("sn").isEqualTo("testUserSecondName");
        assertThat(createRequest.getContent()).stringAt("password").isEqualTo("passwordTobeEncrypted");
        assertThat(createRequest.getContent()).stringAt("mail").isEqualTo(TEST_EMAIL_ID);
        assertThat(createRequest.getContent()).stringAt("kba/0/customQuestion").isEqualTo(KBA_QUESTION_3);
        assertThat(createRequest.getContent()).stringAt("kba/0/answer").isEqualTo("a1");
        assertThat(createRequest.getContent()).stringAt("kba/1/selectedQuestion").isEqualTo(KBA_QUESTION_2);
        assertThat(createRequest.getContent()).stringAt("kba/1/answer").isEqualTo("a2");
        assertThat(createRequest.getResourcePath()).isEqualTo("users");
    }

    private UserRegistrationConfig newUserRegistrationConfig() {
        return new UserRegistrationConfig()
                .setIdentityServiceUrl("/users");
    }

    private JsonValue newJsonValueEmailId() {
        return json(TEST_EMAIL_ID);
    }

    private JsonValue newEmptyJsonValue() {
        return json(object());
    }

    private JsonValue newJsonValueInputUser() {
        return json(
                object(
                        field("userId", TEST_EMAIL_ID),
                                field("user", object(
                                field("givenName", "testUser"),
                                field("sn", "testUserSecondName"),
                                field("password", "passwordTobeEncrypted")))));
    }

    private JsonValue newJsonValueUserAndKba() {
        return json(
                object(
                        field("givenName", "testUser"),
                        field("sn", "testUserSecondName"),
                        field("password", "passwordTobeEncrypted"),
                        field("mail", TEST_EMAIL_ID),
                        field("kba", array(
                                object(
                                        field("customQuestion", KBA_QUESTION_3),
                                        field("answer", "a1")),
                                object(
                                        field("selectedQuestion", KBA_QUESTION_2),
                                        field("answer", "a2"))))));
    }

}
