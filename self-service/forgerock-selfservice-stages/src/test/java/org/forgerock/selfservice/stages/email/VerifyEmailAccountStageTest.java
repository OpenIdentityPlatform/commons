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
import static org.mockito.BDDMockito.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.*;
import static org.assertj.core.api.Assertions.*;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.StageResponse;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link VerifyEmailAccountStage}.
 *
 * @since 0.2.0
 */
public class VerifyEmailAccountStageTest {

    private static final String TEST_EMAIL_ID = "test@forgerock.com";

    private VerifyEmailAccountStage verifyEmailStage;
    @Mock
    private ProcessContext context;
    @Mock
    private ConnectionFactory factory;

    private VerifyEmailAccountConfig config;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        config = new VerifyEmailAccountConfig();
        verifyEmailStage = new VerifyEmailAccountStage(factory);
    }

    @Test
    public void testGatherInitialRequirements() throws Exception {
        // When
        JsonValue jsonValue = verifyEmailStage.gatherInitialRequirements(context, config);

        // Then
        assertThat(jsonValue).stringAt("description").isEqualTo("Verify your email address");
        assertThat(jsonValue).stringAt("properties/mail/description").isEqualTo("Email address");
    }

    @Test (expectedExceptions = BadRequestException.class, expectedExceptionsMessageRegExp = "mail is missing")
    public void testGetEmailAddressWithoutSufficientInput() throws Exception {
        // Given
        given(context.getInput()).willReturn(newEmptyJsonValue());
        StageResponse.Builder builder = StageResponse.newBuilder();

        // When
        String emailAddress = verifyEmailStage.getEmailAddress(context, config, builder);
    }

    @Test
    public void testGetEmailAddress() throws Exception {
        // Given
        given(context.getInput()).willReturn(newJsonValueWithEmail());
        StageResponse.Builder builder = StageResponse.newBuilder();

        // When
        String emailAddress = verifyEmailStage.getEmailAddress(context, config, builder);

        // Then
        assertThat(emailAddress).isSameAs(TEST_EMAIL_ID);
    }

    private JsonValue newEmptyJsonValue() {
        return json(object());
    }

    private JsonValue newJsonValueWithEmail() {
        return json(object(field("mail", TEST_EMAIL_ID)));
    }
}
