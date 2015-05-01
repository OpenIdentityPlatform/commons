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

package org.forgerock.caf.authentication.framework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthStatus;
import java.util.Map;

import org.forgerock.caf.authentication.api.AsyncServerAuthContext;
import org.forgerock.caf.authentication.api.AuthContextWithState;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.AuthenticationState;
import org.forgerock.caf.authentication.api.MessageContext;
import org.forgerock.http.Context;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class MessageContextImplTest {

    private MessageContext context;

    private Request request;
    private AuditTrail auditTrail;

    @BeforeMethod
    public void setup() {
        Context parent = mock(Context.class);
        request = new Request();
        auditTrail = mock(AuditTrail.class);

        context = new MessageContextImpl(parent, request, auditTrail);
    }

    @Test
    public void getRequestShouldReturnInitialRequest() {

        //Given

        //When
        Request request = context.getRequest();

        //Then
        assertThat(request).isEqualTo(this.request);
    }

    @Test
    public void setRequestShouldReplaceInitialRequest() {

        //Given
        Request request = new Request();

        //When
        context.setRequest(request);

        //Then
        assertThat(context.getRequest()).isEqualTo(request);
    }

    @Test
    public void getResponseShouldReturnSuccessfulResponse() {

        //Given

        //When
        Response response = context.getResponse();

        //Then
        assertThat(response.getStatus()).isEqualTo(Status.OK);
    }

    @Test
    public void setResponseShouldReplaceInitialResponse() {

        //Given
        Response response = new Response().setStatus(Status.UNAUTHORIZED);

        //When
        context.setResponse(response);

        //Then
        assertThat(context.getResponse()).isEqualTo(response);
        assertThat(response.getStatus()).isEqualTo(Status.UNAUTHORIZED);
    }

    @Test
    public void getRequestContextMapShouldReturnEmptyMap() {

        //Given

        //When
        Map<String, Object> requestContextMap = context.getRequestContextMap();

        //Then
        assertThat(requestContextMap).isEmpty();
    }

    @Test
    public void getAuditTrailShouldReturnInitialAuditTrail() {

        //Given

        //When
        AuditTrail auditTrail = context.getAuditTrail();

        //Then
        assertThat(auditTrail).isEqualTo(this.auditTrail);
    }

    @Test
    public void getStateShouldReturnNewAuthenticationStateWhenFirstCalledByAuthContext() {

        //Given
        AsyncServerAuthContext authContext = mock(AsyncServerAuthContext.class);

        //When
        AuthenticationState state = context.getState(authContext);

        //Then
        assertThat(state).isNotNull();
    }

    @Test
    public void getStateShouldReturnSameAuthenticationStateWhenSubsequentlyCalledBySameAuthContext() {

        //Given
        AsyncServerAuthContext authContext = mock(AsyncServerAuthContext.class);
        AuthenticationState stateOne = context.getState(authContext);

        //When
        AuthenticationState stateTwo = context.getState(authContext);

        //Then
        assertThat(stateOne).isEqualTo(stateTwo);
    }

    @Test
    public void getStateShouldReturnDifferentAuthenticationStateWhenCalledByDifferentAuthContext() {

        //Given
        AsyncServerAuthContext authContextOne = mock(AsyncServerAuthContext.class);
        AsyncServerAuthContext authContextTwo = mock(TestAuthContext.class);
        AuthenticationState stateOne = context.getState(authContextOne);

        //When
        AuthenticationState stateTwo = context.getState(authContextTwo);

        //Then
        assertThat(stateOne).isNotEqualTo(stateTwo);
    }

    @Test
    public void getStateShouldReturnSpecificAuthenticationStateSubTypeWhenCalledByAuthContextWithState() {

        //Given
        AsyncServerAuthContext authContext = new TestAuthContext();

        //When
        AuthenticationState state = context.getState(authContext);

        //Then
        assertThat(state).isNotNull().isInstanceOf(TestAuthenticationState.class);
    }

    private static class TestAuthContext implements AsyncServerAuthContext, AuthContextWithState {

        @Override
        public Promise<AuthStatus, AuthenticationException> validateRequest(MessageContext context,
                Subject clientSubject, Subject serviceSubject) {
            return null;
        }

        @Override
        public Promise<AuthStatus, AuthenticationException> secureResponse(MessageContext context,
                Subject serviceSubject) {
            return null;
        }

        @Override
        public Promise<Void, AuthenticationException> cleanSubject(MessageContext context, Subject clientSubject) {
            return null;
        }

        @Override
        public AuthenticationState createAuthenticationState() {
            return new TestAuthenticationState();
        }
    }

    private static final class TestAuthenticationState extends AuthenticationState {
    }
}
