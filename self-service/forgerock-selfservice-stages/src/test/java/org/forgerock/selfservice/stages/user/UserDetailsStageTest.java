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
package org.forgerock.selfservice.stages.user;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.selfservice.stages.CommonStateFields.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.assertj.core.api.Assertions;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.selfservice.core.ProcessContext;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link UserDetailsStage}.
 *
 * @since 0.2.0
 */
public final class UserDetailsStageTest {

    private static final String TEST_EMAIL_ID = "test@forgerock.com";
    private static final String KBA_QUESTION_2 = "Who was your first employer?";
    private static final String KBA_QUESTION_3 = "What is my favorite author?";

    private UserDetailsStage userDetailsStage;
    @Mock
    private ProcessContext context;

    private UserDetailsConfig config;
    @Mock
    private ConnectionFactory factory;
    @Mock
    private Connection connection;

    @Mock
    private ResourceResponse queryResponse;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        config = newUserDetailsConfig();
        userDetailsStage = new UserDetailsStage(factory);
    }

    @Test
    public void testGatherInitialRequirements() throws Exception {
        // Given
        given(context.containsState(EMAIL_FIELD)).willReturn(true);
        given(context.getState(EMAIL_FIELD)).willReturn(newJsonValueEmailId());

        // When
        JsonValue jsonValue = userDetailsStage.gatherInitialRequirements(context, config);

        // Then
        assertThat(jsonValue).stringAt("description").isEqualTo("New user details");
        assertThat(jsonValue).stringAt("properties/user/description").isEqualTo("User details");
        assertThat(jsonValue).stringAt("properties/user/type").isEqualTo("object");
    }

    @Test (expectedExceptions = BadRequestException.class,
            expectedExceptionsMessageRegExp = "user has not been specified")
    public void testAdvanceUserNotSpecified() throws Exception {
        // Given
        given(context.getInput()).willReturn(newJsonValueUserId());

        // When
        userDetailsStage.advance(context, config);
    }

    @Test
    public void testAdvance() throws Exception {
        // Given
        given(context.getInput()).willReturn(newJsonValueInputsUser());
        given(context.getState(EMAIL_FIELD)).willReturn(newJsonValueEmailId());
        given(factory.getConnection()).willReturn(connection);

        // When
        userDetailsStage.advance(context, config);

        // Then
        ArgumentCaptor<JsonValue> userArgumentCaptor =  ArgumentCaptor.forClass(JsonValue.class);
        verify(context, times(2))   //1. when the empty empty object is pushed 2. when updated user json is pushed
                .putState(eq(USER_FIELD), userArgumentCaptor.capture());
        JsonValue userJson = userArgumentCaptor.getValue();

        assertThat(userJson).stringAt("givenName").isEqualTo("testUser");
        assertThat(userJson).stringAt("sn").isEqualTo("testUserSecondName");
        assertThat(userJson).stringAt("password").isEqualTo("passwordTobeEncrypted");
    }

    @Test
    public void testAdvanceAfterKbaStage() throws Exception {
        // Given
        given(context.getInput()).willReturn(newJsonValueInputsUser());
        given(context.getState(USER_FIELD)).willReturn(newJsonValueKba());
        given(context.getState(EMAIL_FIELD)).willReturn(newJsonValueEmailId());
        given(factory.getConnection()).willReturn(connection);

        // When
        userDetailsStage.advance(context, config);

        // Then
        ArgumentCaptor<JsonValue> createRequestArgumentCaptor =  ArgumentCaptor.forClass(JsonValue.class);
        verify(context, times(1)).putState(eq(USER_FIELD), createRequestArgumentCaptor.capture());
        JsonValue userJson = createRequestArgumentCaptor.getValue();

        assertThat(userJson).stringAt("givenName").isEqualTo("testUser");
        assertThat(userJson).stringAt("sn").isEqualTo("testUserSecondName");
        assertThat(userJson).stringAt("password").isEqualTo("passwordTobeEncrypted");

        assertThat(userJson).stringAt("kba/0/customQuestion").isEqualTo(KBA_QUESTION_3);
        assertThat(userJson).stringAt("kba/0/answer").isEqualTo("a1");
        assertThat(userJson).stringAt("kba/1/selectedQuestion").isEqualTo(KBA_QUESTION_2);
        assertThat(userJson).stringAt("kba/1/answer").isEqualTo("a2");
    }

    private UserDetailsConfig newUserDetailsConfig() {
        return new UserDetailsConfig()
                .setIdentityEmailField("mail");
    }

    private JsonValue newJsonValueEmailId() {
        return json(TEST_EMAIL_ID);
    }

    private JsonValue newEmptyJsonValue() {
        return json(object());
    }

    private JsonValue newJsonValueUserId() {
        return json(object(field("userId", TEST_EMAIL_ID)));
    }

    private JsonValue newJsonValueKba() {
        return json(
                object(
                        field("kba", array(
                                object(
                                        field("customQuestion", KBA_QUESTION_3),
                                        field("answer", "a1")),
                                object(
                                        field("selectedQuestion", KBA_QUESTION_2),
                                        field("answer", "a2"))))));
    }

    private JsonValue newJsonValueInputsUser() {
        return json(
                object(
                        field("userId", TEST_EMAIL_ID),
                        field("user", object(
                                field("givenName", "testUser"),
                                field("sn", "testUserSecondName"),
                                field("password", "passwordTobeEncrypted")))));
    }
}
