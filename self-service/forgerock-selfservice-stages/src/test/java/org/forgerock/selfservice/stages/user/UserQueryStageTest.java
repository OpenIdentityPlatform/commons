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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.selfservice.stages.CommonStateFields.EMAIL_FIELD;
import static org.forgerock.selfservice.stages.CommonStateFields.USER_ID_FIELD;
import static org.forgerock.selfservice.stages.CommonStateFields.USERNAME_FIELD;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.Set;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.services.context.Context;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link UserQueryStage}.
 *
 * @since 0.5.0
 */
public final class UserQueryStageTest {

    private UserQueryStage userQueryStage;
    @Mock
    private ProcessContext context;
    @Mock
    private ConnectionFactory factory;
    @Mock
    private Connection connection;
    @Mock
    private ResourceResponse resourceResponse;

    private UserQueryConfig config;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        config = newUserQueryStageConfig();
        userQueryStage = new UserQueryStage(factory);
    }

    @Test (expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "User query stage expects query fields")
    public void testGatherInitialRequirementsNoQueryFields() throws Exception {
        // Given
        config = new UserQueryConfig();

        // When
        userQueryStage.gatherInitialRequirements(context, config);
    }

    @Test (expectedExceptions = NullPointerException.class,
            expectedExceptionsMessageRegExp = "User query stage expects identity email field")
    public void testGatherInitialRequirementsNoIdentityEmailField() throws Exception {
        // Given
        config = new UserQueryConfig().setValidQueryFields(newQueryFields());

        // When
        userQueryStage.gatherInitialRequirements(context, config);
    }

    @Test (expectedExceptions = NullPointerException.class,
            expectedExceptionsMessageRegExp = "User query stage expects identity id field")
    public void testGatherInitialRequirementsNoIdentityIdField() throws Exception {
        // Given
        config = new UserQueryConfig()
                .setIdentityEmailField("email")
                .setValidQueryFields(newQueryFields());

        // When
        userQueryStage.gatherInitialRequirements(context, config);
    }

    @Test (expectedExceptions = NullPointerException.class,
            expectedExceptionsMessageRegExp = "User query stage expects identity username field")
    public void testGatherInitialRequirementsNoIdentityUsernameField() throws Exception {
        // Given
        config = new UserQueryConfig()
                .setIdentityEmailField("email")
                .setIdentityIdField("_Id")
                .setValidQueryFields(newQueryFields());

        // When
        userQueryStage.gatherInitialRequirements(context, config);
    }

    @Test (expectedExceptions = NullPointerException.class,
            expectedExceptionsMessageRegExp = "User query stage expects identity service url")
    public void testGatherInitialRequirementsNoIdentityServiceUrl() throws Exception {
        // Given
        config = new UserQueryConfig()
                .setIdentityEmailField("email")
                .setIdentityIdField("_Id")
                .setIdentityUsernameField("userName")
                .setValidQueryFields(newQueryFields());

        // When
        userQueryStage.gatherInitialRequirements(context, config);
    }

    @Test
    public void testGatherInitialRequirements() throws Exception {
        // When
        JsonValue jsonValue = userQueryStage.gatherInitialRequirements(context, config);

        // Then
        assertThat(jsonValue).stringAt("description").isEqualTo("Find your account");
        assertThat(jsonValue).stringAt("properties/queryFilter/description").isEqualTo("filter string to find account");
    }

    @Test (expectedExceptions = JsonValueException.class,
            expectedExceptionsMessageRegExp = "/queryFilter: Expecting a value")
    public void testAdvanceNoQueryFields() throws Exception {
        // Given
        given(context.getInput()).willReturn(newEmptyJsonValue());

        // When
        userQueryStage.advance(context, config);
    }

    @Test (expectedExceptions = BadRequestException.class,
            expectedExceptionsMessageRegExp = "Unable to find account")
    public void testAdvanceNoUser() throws Exception {
        // Given
        given(context.getInput()).willReturn(newJsonValueWithQueryFields());
        given(factory.getConnection()).willReturn(connection);

        // When
        userQueryStage.advance(context, config);
    }

    @Test (expectedExceptions = BadRequestException.class,
            expectedExceptionsMessageRegExp = "Unable to find account")
    public void testAdvanceExceptionMultipleUserFound() throws Exception {
        // Given
        given(context.getInput()).willReturn(newJsonValueWithQueryFields());
        given(factory.getConnection()).willReturn(connection);
        given(resourceResponse.getContent()).willReturn(newJsonValueUser());
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((QueryResourceHandler) invocation.getArguments()[2]).handleResource(resourceResponse);
                ((QueryResourceHandler) invocation.getArguments()[2]).handleResource(resourceResponse);
                return null;
            }
        }).when(connection).query(any(Context.class), any(QueryRequest.class), any(QueryResourceHandler.class));

        // When
        userQueryStage.advance(context, config);
    }

    @Test (expectedExceptions = BadRequestException.class, expectedExceptionsMessageRegExp = "Invalid query filter")
    public void testAdvanceInvalidQueryFilter() throws Exception {
        // Given
        given(context.getInput()).willReturn(newJsonValueWithInvalidQueryFields());
        given(factory.getConnection()).willReturn(connection);
        given(resourceResponse.getContent()).willReturn(newJsonValueUser());
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((QueryResourceHandler) invocation.getArguments()[2]).handleResource(resourceResponse);
                return null;
            }
        }).when(connection).query(any(Context.class), any(QueryRequest.class), any(QueryResourceHandler.class));

        // When
        userQueryStage.advance(context, config);
    }

    @Test
    public void testAdvanceUserWithoutEmail() throws Exception {
        // Given
        given(context.getInput()).willReturn(newJsonValueWithQueryFields());
        given(factory.getConnection()).willReturn(connection);
        given(resourceResponse.getContent()).willReturn(newJsonValueUserWithoutEmail());
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((QueryResourceHandler) invocation.getArguments()[2]).handleResource(resourceResponse);
                return null;
            }
        }).when(connection).query(any(Context.class), any(QueryRequest.class), any(QueryResourceHandler.class));

        // When
        userQueryStage.advance(context, config);

        // Then
        ArgumentCaptor<String> putStateArgumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(context).putState(eq(USER_ID_FIELD), putStateArgumentCaptor.capture());
        assertThat(putStateArgumentCaptor.getValue()).isEqualTo("user1");

        verify(context, never()).putState(eq(EMAIL_FIELD), putStateArgumentCaptor.capture());
    }

    @Test
    public void testAdvance() throws Exception {
        // Given
        given(context.getInput()).willReturn(newJsonValueWithQueryFields());
        given(factory.getConnection()).willReturn(connection);
        given(resourceResponse.getContent()).willReturn(newJsonValueUser());
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((QueryResourceHandler) invocation.getArguments()[2]).handleResource(resourceResponse);
                return null;
            }
        }).when(connection).query(any(Context.class), any(QueryRequest.class), any(QueryResourceHandler.class));

        // When
        userQueryStage.advance(context, config);

        // Then
        ArgumentCaptor<String> putStateArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(context).putState(eq(USER_ID_FIELD), putStateArgumentCaptor.capture());
        assertThat(putStateArgumentCaptor.getValue()).isEqualTo("user1");
        verify(context).putState(eq(EMAIL_FIELD), putStateArgumentCaptor.capture());
        assertThat(putStateArgumentCaptor.getValue()).isEqualTo("email1");
        verify(context).putState(eq(USERNAME_FIELD), putStateArgumentCaptor.capture());
        assertThat(putStateArgumentCaptor.getValue()).isEqualTo("Alice");
    }

    private UserQueryConfig newUserQueryStageConfig() {
        return new UserQueryConfig()
                .setIdentityEmailField("/email")
                .setIdentityIdField("_Id")
                .setIdentityUsernameField("username")
                .setIdentityServiceUrl("/users")
                .setValidQueryFields(newQueryFields());
    }

    private Set<String> newQueryFields() {
        Set<String> queryFields = new HashSet<>();
        queryFields.add("username");
        queryFields.add("email");
        return queryFields;
    }

    private JsonValue newEmptyJsonValue() {
        return json(object());
    }

    private JsonValue newJsonValueWithQueryFields() {
        return json(object(field("queryFilter", "username eq 'user1' and email eq 'email1'")));
    }

    private JsonValue newJsonValueWithInvalidQueryFields() {
        return json(object(field("queryFilter", "username eq 'user1' and givenName eq 'first name'")));
    }

    private JsonValue newJsonValueUser() {
        return json(object(
                field("firstName", "first name"),
                field("_Id", "user1"),
                field("username", "Alice"),
                field("email", "email1")));
    }

    private JsonValue newJsonValueUserWithoutEmail() {
        return json(object(
                field("firstName", "first name"),
                field("username", "Alice"),
                field("_Id", "user1")));
    }

}

