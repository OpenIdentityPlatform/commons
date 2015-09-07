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

import static org.forgerock.selfservice.core.ServiceUtils.INITIAL_TAG;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.assertj.core.api.Assertions.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.*;
import static org.forgerock.json.JsonValue.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.forgerock.http.Context;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link AbstractEmailVerificationStage}.
 *
 * @since 0.2.0
 */
public class AbstractEmailVerificationStageTest {

    private static final String TEST_EMAIL_ID = "test@forgerock.com";

    private AbstractEmailVerificationStage<AbstractEmailVerificationConfig>  verificationStage;
    @Mock
    private ProcessContext context;
    @Mock
    private AbstractEmailVerificationConfig config;
    @Mock
    private ConnectionFactory factory;
    @Mock
    private Connection connection;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        verificationStage = newSpyInstanceVerificationStage();
    }

    @Test (expectedExceptions = InternalServerErrorException.class,
        expectedExceptionsMessageRegExp = "mail should not be empty")
    public void testAdvanceInitialStageWithoutEmailAddress() throws Exception {
        // Given
        given(context.getStageTag()).willReturn(INITIAL_TAG);
        given(verificationStage.getEmailAddress(any(ProcessContext.class), any(AbstractEmailVerificationConfig.class),
            any(StageResponse.Builder.class))).willReturn(null);

        // When
        StageResponse stageResponse = verificationStage.advance(context, config);
    }

    @Test
    public void testAdvanceInitialStage() throws Exception {
        // Given
        given(context.getStageTag()).willReturn(INITIAL_TAG);
        given(verificationStage.getEmailAddress(any(ProcessContext.class), any(AbstractEmailVerificationConfig.class),
            any(StageResponse.Builder.class))).willReturn(TEST_EMAIL_ID);

        // When
        StageResponse stageResponse = verificationStage.advance(context, config);

        // Then
        assertThat(stageResponse.getStageTag()).isSameAs("validateCode");
        assertThat(stageResponse.getRequirements()).stringAt("/description").isEqualTo("Verify emailed code");
        assertThat(stageResponse.getRequirements()).stringAt("properties/code/description")
            .isEqualTo("Enter code emailed");
        assertThat(stageResponse.getCallback()).isNotNull().isInstanceOf(SnapshotTokenCallback.class);
    }

    @Test
    public void testCallbackSendMail() throws Exception {
        // Given
        given(context.getStageTag()).willReturn(INITIAL_TAG);
        given(verificationStage.getEmailAddress(any(ProcessContext.class), any(AbstractEmailVerificationConfig.class),
            any(StageResponse.Builder.class))).willReturn(TEST_EMAIL_ID);
        given(factory.getConnection()).willReturn(connection);
        given(config.getEmailMessage()).willReturn("<h3>This is your reset email.</h3>"
            + "<h4><a href=\"%link%\">Email verification link</a></h4>");
        given(config.getEmailVerificationLinkToken()).willReturn("%link%");
        given(config.getEmailVerificationLink()).willReturn("http://localhost:9999/example/#passwordReset/");
        given(config.getEmailServiceUrl()).willReturn("/email");
        final String infoEmailId = "info@admin.org";
        given(config.getEmailFrom()).willReturn(infoEmailId);
        final String emailSubject = "Reset password email";
        given(config.getEmailSubject()).willReturn(emailSubject);

        // When
        StageResponse stageResponse = verificationStage.advance(context, config);
        SnapshotTokenCallback callback = stageResponse.getCallback();
        callback.snapshotTokenPreview(context, "token1");

        // Then
        ArgumentCaptor<ActionRequest> actionRequestArgumentCaptor =  ArgumentCaptor.forClass(ActionRequest.class);
        verify(connection).action(any(Context.class), actionRequestArgumentCaptor.capture());
        ActionRequest actionRequest = actionRequestArgumentCaptor.getValue();

        assertThat(actionRequest.getAction()).isSameAs("send");
        assertThat(actionRequest.getContent()).stringAt("/to").isEqualTo(TEST_EMAIL_ID);
        assertThat(actionRequest.getContent()).stringAt("/from").isEqualTo(infoEmailId);
        assertThat(actionRequest.getContent()).stringAt("/subject").isEqualTo(emailSubject);
        assertThat(actionRequest.getContent()).stringAt("/message").matches(
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
        StageResponse stageResponse = verificationStage.advance(context, config);
    }

    @Test (expectedExceptions = BadRequestException.class, expectedExceptionsMessageRegExp = "Invalid code")
    public void testAdvanceValidateCodeStageWhenCodeIsInvalid() throws Exception {
        // Given
        given(context.getStageTag()).willReturn("validateCode");
        given(context.getState("code")).willReturn(json("code1"));
        given(context.getInput()).willReturn(json(object(field("code", "code2"))));

        // When
        StageResponse stageResponse = verificationStage.advance(context, config);
    }

    @Test
    public void testAdvanceValidateCodeStage() throws Exception {
        // Given
        given(context.getStageTag()).willReturn("validateCode");
        given(context.getState("code")).willReturn(json("code1"));
        given(context.getInput()).willReturn(json(object(field("code", "code1"))));

        // When
        StageResponse stageResponse = verificationStage.advance(context, config);

        // Then
        assertThat(stageResponse.getStageTag()).isSameAs(INITIAL_TAG);
        assertThat(stageResponse.getRequirements()).isEmpty();
        assertThat(stageResponse.getCallback()).isNull();
    }

    private AbstractEmailVerificationStage<AbstractEmailVerificationConfig> newSpyInstanceVerificationStage() {
        //The object being created and the constructor argument connectionFactory are mock objects.
        return spy(
            new AbstractEmailVerificationStage<AbstractEmailVerificationConfig>(factory) {

                @Override
                protected String getEmailAddress(ProcessContext context,
                                                 AbstractEmailVerificationConfig config,
                                                 StageResponse.Builder builder) throws ResourceException {
                    return null;
                }

                @Override
                public JsonValue gatherInitialRequirements(
                    ProcessContext context, AbstractEmailVerificationConfig config) throws ResourceException {
                    return null;
                }
            }
        );
    }
}
