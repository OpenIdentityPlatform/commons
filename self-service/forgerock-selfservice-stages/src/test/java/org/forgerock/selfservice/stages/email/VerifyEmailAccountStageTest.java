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

import static org.forgerock.json.JsonValue.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.selfservice.core.ServiceUtils.INITIAL_TAG;
import static org.forgerock.selfservice.stages.CommonStateFields.EMAIL_FIELD;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.assertj.core.api.Assertions.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.*;
import static org.forgerock.json.JsonValue.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.assertj.core.api.Assertions;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
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

        config = new VerifyEmailAccountConfig();
        verifyEmailStage = new VerifyEmailAccountStage(factory);
    }

    @Test
    public void testGatherInitialRequirementsEmptyContext() throws Exception {
        // When
        JsonValue jsonValue = verifyEmailStage.gatherInitialRequirements(context, config);

        // Then
        assertThat(jsonValue).stringAt("description").isEqualTo("Verify your email address");
        assertThat(jsonValue).stringAt("properties/mail/description").isEqualTo("Email address");
    }

    @Test
    public void testGatherInitialRequirementsWithEmailInContext() throws Exception {
        // When
        given(context.containsState(EMAIL_FIELD)).willReturn(true);
        given(context.getState(EMAIL_FIELD)).willReturn(newJsonValueWithEmail());

        JsonValue jsonValue = verifyEmailStage.gatherInitialRequirements(context, config);

        // Then
        assertThat(jsonValue).isEmpty();
    }

    @Test (expectedExceptions = InternalServerErrorException.class,
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
    public void testAdvanceCallbackSendMail() throws Exception {
        // Given
        given(context.getStageTag()).willReturn(INITIAL_TAG);

        List<Locale> locales = new ArrayList<>();
        locales.add(Locale.ENGLISH);
        PreferredLocales preferredLocales = new PreferredLocales(locales);

        given(context.getPreferredLocales()).willReturn(preferredLocales);

        given(context.containsState(EMAIL_FIELD)).willReturn(true);
        given(context.getState(EMAIL_FIELD)).willReturn(newJsonValueWithEmail());

        given(factory.getConnection()).willReturn(connection);

        config.setVerificationLinkToken("%link%");
        config.setVerificationLink("http://localhost:9999/example/#passwordReset/");
        config.setEmailServiceUrl("/email");
        final String infoEmailId = "info@admin.org";
        config.setFrom(infoEmailId);
        final String emailSubject = "Reset password email";

        Map<Locale, String> subjectMap = new HashMap<>();
        Map<Locale, String> messageMap = new HashMap<>();

        subjectMap.put(Locale.ENGLISH, "Reset password email");
        subjectMap.put(Locale.GERMAN, "Deutsch Thema");
        messageMap.put(Locale.ENGLISH, "<h3>This is your reset email.</h3>"
                            + "<h4><a href=\"%link%\">Email verification link</a></h4>");

        config.setSubjectMap(subjectMap);
        config.setMessageMap(messageMap);

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
        assertThat(actionRequest.getContent()).stringAt("/from").isEqualTo(infoEmailId);
        assertThat(actionRequest.getContent()).stringAt("/subject").isEqualTo(subjectMap.get(Locale.ENGLISH));
        assertThat(actionRequest.getContent()).stringAt("/body").matches(
                "<h3>This is your reset email\\.</h3><h4>"
                + "<a href=\"http://localhost:9999/example/#passwordReset/&token=token1&code="
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
}
