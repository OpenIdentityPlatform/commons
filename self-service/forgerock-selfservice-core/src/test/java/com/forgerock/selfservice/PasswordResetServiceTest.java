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
package com.forgerock.selfservice;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.notNull;
import static org.mockito.Mockito.verify;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.ServerContext;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link PasswordResetService}.
 *
 * @since 0.1.0
 */
public class PasswordResetServiceTest {

    private RequestHandler passwordResetService;

    @Mock
    private ActionRequest actionRequest;
    @Mock
    private ResultHandler<JsonValue> resultHandler;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        passwordResetService = new PasswordResetService();
    }

    @Test
    public void submitRequirementsHandled() {
        // Given
        ServerContext context = new ServerContext(new RootContext());
        given(actionRequest.getAction()).willReturn("submitRequirements");

        // When
        passwordResetService.handleAction(context, actionRequest, resultHandler);

        // Then
        verify(resultHandler).handleResult(notNull(JsonValue.class));
    }

    @Test
    public void unknownActionsHandled() {
        // Given
        ServerContext context = new ServerContext(new RootContext());
        given(actionRequest.getAction()).willReturn("unknown-action");

        // When
        passwordResetService.handleAction(context, actionRequest, resultHandler);

        // Then
        verify(resultHandler).handleError(exp(ResourceException.BAD_REQUEST));
    }

    private static ResourceException exp(int errorCode) {
        return argThat(new ResourceExceptionMatcher(errorCode));
    }

    private static final class ResourceExceptionMatcher extends ArgumentMatcher<ResourceException> {

        private final int errorCode;

        private ResourceExceptionMatcher(int errorCode) {
            this.errorCode = errorCode;
        }

        @Override
        public boolean matches(Object argument) {
            if (!(argument instanceof ResourceException)) {
                return false;
            }

            ResourceException exception = (ResourceException) argument;
            return exception.getCode() == errorCode;
        }

    }

}
