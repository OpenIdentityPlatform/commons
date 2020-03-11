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
import static org.forgerock.selfservice.stages.CommonStateFields.USERNAME_FIELD;
import static org.forgerock.selfservice.stages.user.RetrieveUsernameStage.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import org.forgerock.json.JsonValue;
import org.forgerock.selfservice.core.ProcessContext;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link RetrieveUsernameStage}.
 *
 * @since 0.7.0
 */
public final class RetrieveUsernameStageTest {

    private static final String TEST_USERNAME = "Alice";

    private RetrieveUsernameStage retrieveUsernameStage;
    @Mock
    private ProcessContext context;

    private RetrieveUsernameConfig config;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        config = new RetrieveUsernameConfig();

        retrieveUsernameStage = new RetrieveUsernameStage();
    }

    @Test (expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Retrieve username stage expects user name in the context")
    public void testGatherInitialRequirementsNoUserName() throws Exception {
        // When
        retrieveUsernameStage.gatherInitialRequirements(context, config);
    }

    @Test
    public void testGatherInitialRequirements() throws Exception {
        // When
        given(context.containsState(USERNAME_FIELD)).willReturn(true);
        given(context.getState(USERNAME_FIELD)).willReturn(newJsonValueUsername());

        JsonValue jsonValue = retrieveUsernameStage.gatherInitialRequirements(context, config);

        // Then
        assertThat(jsonValue).isEmpty();
    }

    @Test
    public void testAdvance() throws Exception {
        // Given
        given(context.containsState(USERNAME_FIELD)).willReturn(true);
        given(context.getState(USERNAME_FIELD)).willReturn(newJsonValueUsername());

        // When
        retrieveUsernameStage.advance(context, config);

        // Then
        ArgumentCaptor<String> putStateArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(context).putSuccessAddition(eq(KEY_ADDITIONS_USERNAME), putStateArgumentCaptor.capture());
        assertThat(putStateArgumentCaptor.getValue()).isEqualTo(TEST_USERNAME);
    }

    private JsonValue newJsonValueUsername() {
        return json(TEST_USERNAME);
    }

}

