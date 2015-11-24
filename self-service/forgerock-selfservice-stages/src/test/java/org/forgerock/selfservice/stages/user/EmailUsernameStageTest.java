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
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.selfservice.stages.CommonStateFields.EMAIL_FIELD;
import static org.forgerock.selfservice.stages.CommonStateFields.USERNAME_FIELD;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Request;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.services.context.Context;
import org.forgerock.util.i18n.PreferredLocales;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link EmailUsernameStage}.
 *
 * @since 0.8.0
 */
public final class EmailUsernameStageTest {

    private static final String TEST_EMAIL_ID = "test@forgerock.com";
    private static final String INFO_EMAIL_ID = "info@forgerock.com";
    private static final String TEST_USER_ID = "user1";

    private EmailUsernameStage emailUsernameStage;
    @Mock
    private ProcessContext context;
    @Mock
    private ConnectionFactory factory;
    @Mock
    private Connection connection;

    private EmailUsernameConfig config;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        config = newEmailUsernameConfig();
        emailUsernameStage = new EmailUsernameStage(factory);
    }

    @Test (expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Retrieve username stage expects user Id in the context")
    public void testGatherInitialRequirementsNoUserIdInContext() throws Exception {
        // Given
        given(context.containsState(EMAIL_FIELD)).willReturn(true);
        given(context.getState(EMAIL_FIELD)).willReturn(newJsonValueWithEmail());

        // When
        emailUsernameStage.gatherInitialRequirements(context, config);
    }

    @Test (expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Retrieve username stage expects email Id in the context")
    public void testGatherInitialRequirementsNoEmailInContext() throws Exception {
        // Given
        given(context.containsState(USERNAME_FIELD)).willReturn(true);
        given(context.getState(USERNAME_FIELD)).willReturn(newJsonValueWithUserId());

        // When
        emailUsernameStage.gatherInitialRequirements(context, config);
    }

    @Test
    public void testGatherInitialRequirements() throws Exception {
        // Given
        given(context.containsState(EMAIL_FIELD)).willReturn(true);
        given(context.getState(EMAIL_FIELD)).willReturn(newJsonValueWithEmail());

        given(context.containsState(USERNAME_FIELD)).willReturn(true);
        given(context.getState(USERNAME_FIELD)).willReturn(newJsonValueWithUserId());

        // When
        JsonValue jsonValue = emailUsernameStage.gatherInitialRequirements(context, config);

        // Then
        assertThat(jsonValue).isEmpty();
    }

    @Test
    public void testAdvance() throws Exception {
        // Given
        given(context.containsState(EMAIL_FIELD)).willReturn(true);
        given(context.getState(EMAIL_FIELD)).willReturn(newJsonValueWithEmail());

        given(context.containsState(USERNAME_FIELD)).willReturn(true);
        given(context.getState(USERNAME_FIELD)).willReturn(newJsonValueWithUserId());

        List<Locale> locales = new ArrayList<>();
        locales.add(Locale.ENGLISH);
        PreferredLocales preferredLocales = new PreferredLocales(locales);

        Request request = mock(Request.class);
        given(request.getPreferredLocales()).willReturn(preferredLocales);
        given(context.getRequest()).willReturn(request);

        given(factory.getConnection()).willReturn(connection);

        // When
        StageResponse stageResponse = emailUsernameStage.advance(context, config);

        // Then
        ArgumentCaptor<ActionRequest> actionRequestArgumentCaptor =  ArgumentCaptor.forClass(ActionRequest.class);
        verify(connection).action(any(Context.class), actionRequestArgumentCaptor.capture());
        ActionRequest actionRequest = actionRequestArgumentCaptor.getValue();

        assertThat(actionRequest.getAction()).isSameAs("send");
        assertThat(actionRequest.getContent()).stringAt("/to").isEqualTo(TEST_EMAIL_ID);
        assertThat(actionRequest.getContent()).stringAt("/from").isEqualTo(INFO_EMAIL_ID);
        assertThat(actionRequest.getContent()).stringAt("/subject")
                .isEqualTo(config.getSubjectTranslations().get(Locale.ENGLISH));
        assertThat(actionRequest.getContent()).stringAt("/body")
                .isEqualTo("<h3>Username is:</h3><br />" + TEST_USER_ID);

        assertThat(stageResponse.getRequirements()).isEmpty();
    }

    private JsonValue newJsonValueWithEmail() {
        return json(TEST_EMAIL_ID);
    }

    private JsonValue newJsonValueWithUserId() {
        return json(TEST_USER_ID);
    }

    private EmailUsernameConfig newEmailUsernameConfig() {
        return new EmailUsernameConfig()
                .setMessageTranslations(newMessageMap())
                .setUsernameToken("%username%")
                .setEmailServiceUrl("/email")
                .setFrom(INFO_EMAIL_ID)
                .setSubjectTranslations(newSubjectMap())
                .setMimeType("html");
    }

    private Map<Locale, String> newMessageMap() {
        Map<Locale, String> messageMap = new HashMap<>();
        messageMap.put(Locale.ENGLISH, "<h3>Username is:</h3><br />%username%");
        return messageMap;
    }

    private Map<Locale, String> newSubjectMap() {
        Map<Locale, String> subjectMap = new HashMap<>();
        subjectMap.put(Locale.ENGLISH, "Account Information - username");
        return subjectMap;
    }

}
