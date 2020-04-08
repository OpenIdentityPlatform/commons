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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.selfservice.core.ServiceUtils.INITIAL_TAG;
import static org.forgerock.selfservice.stages.CommonStateFields.EMAIL_FIELD;
import static org.forgerock.selfservice.stages.CommonStateFields.USER_FIELD;
import static org.forgerock.selfservice.stages.CommonStateFields.QUERYSTRING_PARAMS_FIELD;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.assertj.core.api.Assertions;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Request;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenCallback;
import org.forgerock.services.context.Context;
import org.forgerock.util.i18n.PreferredLocales;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Unit test for {@link VerifyEmailAccountStage}.
 *
 * @since 0.2.0
 */
public final class VerifyEmailAccountStageTest {

    private static final String TEST_EMAIL_ID = "test@forgerock.com";
    private static final  String INFO_MAIL_ID = "info@admin.org";
    private static final String TEST_QUERYSTRING_PARAMS = "goto=https://www.google.com";

    private VerifyEmailAccountStage verifyEmailStage;
    @Mock
    private ProcessContext context;
    @Mock
    private ConnectionFactory factory;
    @Mock
    private Connection connection;

    private VerifyEmailAccountConfig config;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        config = newVerifyEmailAccountConfig();
        verifyEmailStage = new VerifyEmailAccountStage(factory);
    }

    @Test
    public void testGatherInitialRequirementsNoEmailInContext() throws Exception {
        // When
        JsonValue jsonValue = verifyEmailStage.gatherInitialRequirements(context, config);

        // Then
        assertThat(jsonValue).stringAt("description").isEqualTo("Verify your email address");
        assertThat(jsonValue).stringAt("properties/mail/description").isEqualTo("Email address");
    }

    @Test
    public void testGatherInitialRequirementsWithEmailInContext() throws Exception {
        // Given
        given(context.containsState(EMAIL_FIELD)).willReturn(true);
        given(context.getState(EMAIL_FIELD)).willReturn(newJsonValueWithEmail());

        // When
        JsonValue jsonValue = verifyEmailStage.gatherInitialRequirements(context, config);

        // Then
        assertThat(jsonValue).stringAt("description").isEqualTo("Verify your email address");
        assertThat(jsonValue).stringAt("properties/querystringParams/type").isEqualTo("hidden");
    }

    @Test
    public void testGatherInitialRequirementsWithEmailAndQuerystringParamsInContext() throws Exception {
        // Given
        given(context.containsState(EMAIL_FIELD)).willReturn(true);
        given(context.getState(EMAIL_FIELD)).willReturn(newJsonValueWithEmail());
        given(context.containsState(QUERYSTRING_PARAMS_FIELD)).willReturn(true);
        given(context.getState(QUERYSTRING_PARAMS_FIELD)).willReturn(newJsonValueWithEmail());

        // When
        JsonValue jsonValue = verifyEmailStage.gatherInitialRequirements(context, config);

        // Then
        assertThat(jsonValue).isEmpty();
    }

    @Test (expectedExceptions = BadRequestException.class,
            expectedExceptionsMessageRegExp = "mail should not be empty")
    public void testAdvanceInitialStageWithoutEmailAddress() throws Exception {
        // Given
        given(context.getInput()).willReturn(newEmptyJsonValue());
        given(context.getStageTag()).willReturn(INITIAL_TAG);

        // When
        verifyEmailStage.advance(context, config);
    }

    @Test
    public void testAdvanceInitialStage() throws Exception {
        // Given
        given(context.containsState(EMAIL_FIELD)).willReturn(true);
        given(context.getState(EMAIL_FIELD)).willReturn(newJsonValueWithEmail());

        given(context.getStageTag()).willReturn(INITIAL_TAG);

        // When
        StageResponse stageResponse = verifyEmailStage.advance(context, config);

        // Then
        Assertions.assertThat(stageResponse.getStageTag()).isSameAs("validateCode");
        assertThat(stageResponse.getRequirements()).stringAt("/description").isEqualTo("Verify emailed code");
        assertThat(stageResponse.getRequirements()).stringAt("properties/code/description")
                .isEqualTo("Enter code emailed");
        Assertions.assertThat(stageResponse.getCallback()).isNotNull().isInstanceOf(SnapshotTokenCallback.class);
    }

    @Test
    public void testAdvanceEmailProcessing() throws Exception {
        // Given
        given(context.containsState(EMAIL_FIELD)).willReturn(false);
        given(context.getInput()).willReturn(newJsonValueInputEmail());

        given(context.containsState(USER_FIELD)).willReturn(true);
        JsonValue user = newJsonValueUser();
        given(context.getState(USER_FIELD)).willReturn(user);

        given(context.getStageTag()).willReturn(INITIAL_TAG);

        // When
        verifyEmailStage.advance(context, config);

        // Then
        ArgumentCaptor<String> putStateArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(context).putState(eq(EMAIL_FIELD), putStateArgumentCaptor.capture());
        assertThat(putStateArgumentCaptor.getValue()).isEqualTo(TEST_EMAIL_ID);

        assertThat(user).stringAt(config.getIdentityEmailField()).isEqualTo(TEST_EMAIL_ID);
    }


    @Test
    public void testAdvanceCallbackSendMail() throws Exception {
        // Given
        given(context.getStageTag()).willReturn(INITIAL_TAG);

        List<Locale> locales = new ArrayList<>();
        locales.add(Locale.ENGLISH);
        PreferredLocales preferredLocales = new PreferredLocales(locales);

        Request request = mock(Request.class);
        given(request.getPreferredLocales()).willReturn(preferredLocales);
        given(context.getRequest()).willReturn(request);

        given(context.containsState(EMAIL_FIELD)).willReturn(true);
        given(context.getState(EMAIL_FIELD)).willReturn(newJsonValueWithEmail());

        given(factory.getConnection()).willReturn(connection);

        // When
        StageResponse stageResponse = verifyEmailStage.advance(context, config);
        SnapshotTokenCallback callback = stageResponse.getCallback();
        callback.snapshotTokenPreview(context, "token1");

        // Then
        ArgumentCaptor<ActionRequest> actionRequestArgumentCaptor =  ArgumentCaptor.forClass(ActionRequest.class);
        verify(connection).action(any(Context.class), actionRequestArgumentCaptor.capture());
        ActionRequest actionRequest = actionRequestArgumentCaptor.getValue();

        assertThat(actionRequest.getAction()).isSameAs("send");
        assertThat(actionRequest.getContent()).stringAt("/to").isEqualTo(TEST_EMAIL_ID);
        assertThat(actionRequest.getContent()).stringAt("/from").isEqualTo(INFO_MAIL_ID);
        assertThat(actionRequest.getContent()).stringAt("/subject")
                .isEqualTo(config.getSubjectTranslations().get(Locale.ENGLISH));
        assertThat(actionRequest.getContent()).stringAt("/body").matches(
                "<h3>This is your reset email\\.</h3><h4>"
                + "<a href=\"http://localhost:9999/example/#passwordReset/&token=token1&code="
                + "[\\w\\d]{8}-[\\w\\d]{4}-[\\w\\d]{4}-[\\w\\d]{4}-[\\w\\d]{12}\">Email verification link</a></h4>"
        );
    }

    @Test
    public void testAdvanceCallbackSendMailWithQuerystringParams() throws Exception {
        // Given
        given(context.getStageTag()).willReturn(INITIAL_TAG);

        List<Locale> locales = new ArrayList<>();
        locales.add(Locale.ENGLISH);
        PreferredLocales preferredLocales = new PreferredLocales(locales);

        Request request = mock(Request.class);
        given(request.getPreferredLocales()).willReturn(preferredLocales);
        given(context.getRequest()).willReturn(request);

        given(context.containsState(EMAIL_FIELD)).willReturn(true);
        given(context.getState(EMAIL_FIELD)).willReturn(newJsonValueWithEmail());

        given(context.containsState(QUERYSTRING_PARAMS_FIELD)).willReturn(true);
        given(context.getState(QUERYSTRING_PARAMS_FIELD)).willReturn(newJsonValueWithQuerystringParams());

        given(factory.getConnection()).willReturn(connection);

        // When
        StageResponse stageResponse = verifyEmailStage.advance(context, config);
        SnapshotTokenCallback callback = stageResponse.getCallback();
        callback.snapshotTokenPreview(context, "token1");

        // Then
        ArgumentCaptor<ActionRequest> actionRequestArgumentCaptor =  ArgumentCaptor.forClass(ActionRequest.class);
        verify(connection).action(any(Context.class), actionRequestArgumentCaptor.capture());
        ActionRequest actionRequest = actionRequestArgumentCaptor.getValue();

        assertThat(actionRequest.getAction()).isSameAs("send");
        assertThat(actionRequest.getContent()).stringAt("/to").isEqualTo(TEST_EMAIL_ID);
        assertThat(actionRequest.getContent()).stringAt("/from").isEqualTo(INFO_MAIL_ID);
        assertThat(actionRequest.getContent()).stringAt("/subject")
                .isEqualTo(config.getSubjectTranslations().get(Locale.ENGLISH));
        assertThat(actionRequest.getContent()).stringAt("/body").matches(
                "<h3>This is your reset email\\.</h3><h4>"
                + "<a href=\"http://localhost:9999/example/\\?goto=https://www\\.google\\.com#passwordReset/&token=token1&code="
                + "[\\w\\d]{8}-[\\w\\d]{4}-[\\w\\d]{4}-[\\w\\d]{4}-[\\w\\d]{12}\">Email verification link</a></h4>"
        );
    }

    @Test (expectedExceptions = BadRequestException.class, expectedExceptionsMessageRegExp = "Input code is missing")
    public void testAdvanceValidateCodeStageWhenInputCodeIsMissing() throws Exception {
        // Given
        given(context.getStageTag()).willReturn("validateCode");
        given(context.getState("code")).willReturn(json("code1"));
        given(context.getInput()).willReturn(json(object(field("code", null))));

        // When
        verifyEmailStage.advance(context, config);
    }

    @Test (expectedExceptions = BadRequestException.class, expectedExceptionsMessageRegExp = "Invalid code")
    public void testAdvanceValidateCodeStageWhenCodeIsInvalid() throws Exception {
        // Given
        given(context.getStageTag()).willReturn("validateCode");
        given(context.getState("code")).willReturn(json("code1"));
        given(context.getInput()).willReturn(json(object(field("code", "code2"))));

        // When
        verifyEmailStage.advance(context, config);
    }

    @Test
    public void testAdvanceValidateCodeStage() throws Exception {
        // Given
        given(context.getStageTag()).willReturn("validateCode");
        given(context.getState("code")).willReturn(json("code1"));
        given(context.getInput()).willReturn(json(object(field("code", "code1"))));

        // When
        StageResponse stageResponse = verifyEmailStage.advance(context, config);

        // Then
        assertThat(stageResponse.getStageTag()).isSameAs(INITIAL_TAG);
        assertThat(stageResponse.getRequirements()).isEmpty();
        assertThat(stageResponse.getCallback()).isNull();
    }

    private JsonValue newEmptyJsonValue() {
        return json(object());
    }

    private JsonValue newJsonValueWithEmail() {
        return json(TEST_EMAIL_ID);
    }

    private JsonValue newJsonValueInputEmail() {
        return json(object(
                field(VerifyEmailAccountStage.REQUIREMENT_KEY_EMAIL, TEST_EMAIL_ID)));
    }

    private JsonValue newJsonValueWithQuerystringParams() {
        return json(TEST_QUERYSTRING_PARAMS);
    }

    private JsonValue newJsonValueUser() {
        return json(object(
                field("firstName", "first name"),
                field("_Id", "user1")));
    }

    private VerifyEmailAccountConfig newVerifyEmailAccountConfig() {
        return new VerifyEmailAccountConfig()
                .setIdentityEmailField("mail")
                .setMessageTranslations(newMessageMap())
                .setVerificationLinkToken("%link%")
                .setVerificationLink("http://localhost:9999/example/#passwordReset/")
                .setEmailServiceUrl("/email")
                .setFrom(INFO_MAIL_ID)
                .setSubjectTranslations(newSubjectMap())
                .setMimeType("html");
    }

    private Map<Locale, String> newMessageMap() {
        Map<Locale, String> messageMap = new HashMap<>();
        messageMap.put(Locale.ENGLISH, "<h3>This is your reset email.</h3>"
                + "<h4><a href=\"%link%\">Email verification link</a></h4>");
        return messageMap;
    }

    private Map<Locale, String> newSubjectMap() {
        Map<Locale, String> subjectMap = new HashMap<>();
        subjectMap.put(Locale.ENGLISH, "Reset password email");
        subjectMap.put(Locale.GERMAN, "Deutsch Thema");
        return subjectMap;
    }

}
