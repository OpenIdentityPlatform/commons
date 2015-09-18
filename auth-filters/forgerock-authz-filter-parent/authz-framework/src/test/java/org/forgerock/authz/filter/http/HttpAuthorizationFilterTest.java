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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.authz.filter.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.testng.Assert.fail;

import org.forgerock.authz.filter.api.AuthorizationContext;
import org.forgerock.authz.filter.api.AuthorizationException;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.authz.filter.http.api.HttpAuthorizationModule;
import org.forgerock.services.context.Context;
import org.forgerock.http.Handler;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.services.context.RootContext;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class HttpAuthorizationFilterTest {

    private HttpAuthorizationFilter authorizationFilter;

    private HttpAuthorizationModule module;

    @BeforeMethod
    public void setUp() {
        module = mock(HttpAuthorizationModule.class);
        authorizationFilter = new HttpAuthorizationFilter(module);
    }

    @Test
    public void shouldFailToConstructWithNullModule() {

        //When
        try {
            new HttpAuthorizationFilter(null);
            fail();
        } catch (NullPointerException e) {
            //Then
        }

    }

    @Test
    public void whenAccessPermittedDownstreamHandlerShouldBeCalled() {

        //Given
        Context context = mockContext();
        Request request = new Request();
        Handler next = mock(Handler.class);

        mockModuleAuthorizationPermitted();

        //When
        authorizationFilter.filter(context, request, next);

        //Then
        verify(next).handle(context, request);
    }

    @Test
    public void whenAccessDeniedForbiddenResponseShouldBeReturned() throws Exception {

        //Given
        Context context = mockContext();
        Request request = new Request();
        Handler next = mock(Handler.class);

        mockModuleAuthorizationDenied();

        //When
        Response response = authorizationFilter.filter(context, request, next).getOrThrowUninterruptibly();

        //Then
        verifyZeroInteractions(next);
        assertThat(response.getStatus()).isEqualTo(Status.FORBIDDEN);
    }

    @Test
    public void whenAuthorizationFailsInternalServerErrorResponseShouldBeReturned() throws Exception {

        //Given
        Context context = mockContext();
        Request request = new Request();
        Handler next = mock(Handler.class);

        mockModuleAuthorizationFailure();

        //When
        Response response = authorizationFilter.filter(context, request, next).getOrThrowUninterruptibly();

        //Then
        verifyZeroInteractions(next);
        assertThat(response.getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR);
    }

    private Context mockContext() {
        return new AttributesContext(new RootContext());
    }

    private void mockModuleAuthorizationPermitted() {
        Promise<AuthorizationResult, AuthorizationException> accessPermittedPromise =
                newResultPromise(AuthorizationResult.accessPermitted());
        given(module.authorize(any(Context.class), any(Request.class), any(AuthorizationContext.class)))
                .willReturn(accessPermittedPromise);
    }

    private void mockModuleAuthorizationDenied() {
        Promise<AuthorizationResult, AuthorizationException> accessDeniedPromise =
                newResultPromise(AuthorizationResult.accessDenied("NOT_ALLOWED"));
        given(module.authorize(any(Context.class), any(Request.class), any(AuthorizationContext.class)))
                .willReturn(accessDeniedPromise);
    }

    private void mockModuleAuthorizationFailure() {
        Promise<AuthorizationResult, AuthorizationException> authorizationFailurePromise =
                newExceptionPromise(new AuthorizationException("EXCEPTION"));
        given(module.authorize(any(Context.class), any(Request.class), any(AuthorizationContext.class)))
                .willReturn(authorizationFailurePromise);
    }
}
