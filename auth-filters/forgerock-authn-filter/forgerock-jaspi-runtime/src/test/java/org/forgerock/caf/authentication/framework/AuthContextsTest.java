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

import static org.forgerock.caf.authentication.framework.AuthStatusUtils.asString;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthStatus;

import java.security.Principal;

import org.forgerock.caf.authentication.api.AsyncServerAuthContext;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.MessageContext;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.slf4j.Logger;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AuthContextsTest {

    @DataProvider(name = "validValidateRequestResultValidatingContext")
    private Object[][] getValidValidateRequestResultValidatingContextData() {
        return new Object[][]{
            {AuthStatus.SUCCESS},
            {AuthStatus.SEND_SUCCESS},
            {AuthStatus.SEND_CONTINUE},
            {AuthStatus.SEND_FAILURE},
        };
    }

    @Test(dataProvider = "validValidateRequestResultValidatingContext")
    public void validatingAuthContextShouldValidateValidValidateRequestResult(AuthStatus authStatus) {

        //Given
        AsyncServerAuthContext authContext = mock(AsyncServerAuthContext.class);
        MessageContext context = mock(MessageContext.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        given(authContext.validateRequest(context, clientSubject, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(authStatus));

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthContexts.withValidation(authContext)
                .validateRequest(context, clientSubject, serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(authStatus);
    }

    @DataProvider(name = "invalidValidateRequestResultValidatingContext")
    private Object[][] getInvalidValidateRequestResultValidatingContextData() {
        return new Object[][]{
            {AuthStatus.FAILURE},
            {null},
        };
    }

    @Test(dataProvider = "invalidValidateRequestResultValidatingContext")
    public void validatingAuthContextShouldValidateInvalidValidateRequestResult(AuthStatus authStatus) {

        //Given
        AsyncServerAuthContext authContext = mock(AsyncServerAuthContext.class);
        MessageContext context = mock(MessageContext.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        given(authContext.validateRequest(context, clientSubject, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(authStatus));

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthContexts.withValidation(authContext)
                .validateRequest(context, clientSubject, serviceSubject);

        //Then
        assertThat(promise).failedWithException()
                .hasMessageStartingWith("Invalid AuthStatus")
                .hasMessageContaining("validateRequest")
                .hasMessageContaining(asString(authStatus));
    }

    @DataProvider(name = "validSecureResponseResultValidatingContext")
    private Object[][] getValidSecureResponseResultValidatingContextData() {
        return new Object[][]{
            {AuthStatus.SEND_SUCCESS},
            {AuthStatus.SEND_CONTINUE},
            {AuthStatus.SEND_FAILURE},
        };
    }

    @Test(dataProvider = "validSecureResponseResultValidatingContext")
    public void validatingAuthContextShouldValidateValidSecureResponseResult(AuthStatus authStatus) {

        //Given
        AsyncServerAuthContext authContext = mock(AsyncServerAuthContext.class);
        MessageContext context = mock(MessageContext.class);
        Subject serviceSubject = new Subject();

        given(authContext.secureResponse(context, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(authStatus));

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthContexts.withValidation(authContext)
                .secureResponse(context, serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(authStatus);
    }

    @DataProvider(name = "invalidSecureResponseResultValidatingContext")
    private Object[][] getInvalidSecureResponseResultValidatingContextData() {
        return new Object[][]{
            {AuthStatus.SUCCESS},
            {AuthStatus.FAILURE},
            {null},
        };
    }

    @Test(dataProvider = "invalidSecureResponseResultValidatingContext")
    public void validatingAuthContextShouldValidateInvalidSecureResponseResult(AuthStatus authStatus) {

        //Given
        AsyncServerAuthContext authContext = mock(AsyncServerAuthContext.class);
        MessageContext context = mock(MessageContext.class);
        Subject serviceSubject = new Subject();

        given(authContext.secureResponse(context, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(authStatus));

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthContexts.withValidation(authContext)
                .secureResponse(context, serviceSubject);

        //Then
        assertThat(promise).failedWithException()
                .hasMessageStartingWith("Invalid AuthStatus")
                .hasMessageContaining("secureResponse")
                .hasMessageContaining(asString(authStatus));
    }

    @DataProvider(name = "successfulValidateRequestResultAuditingContext")
    private Object[][] getSuccessfulValidateRequestResultAuditingContextData() {
        return new Object[][]{
            {AuthStatus.SUCCESS},
            {AuthStatus.SEND_SUCCESS},
        };
    }

    @Test(dataProvider = "successfulValidateRequestResultAuditingContext")
    public void auditingAuthContextShouldAuditSuccessfulValidateRequestResult(AuthStatus authStatus) {

        //Given
        AsyncServerAuthContext authContext = mock(AsyncServerAuthContext.class);
        MessageContext context = mock(MessageContext.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        AuditTrail auditTrail = mock(AuditTrail.class);
        Principal principalOne = mock(Principal.class);
        Principal principalTwo = mock(Principal.class);

        given(authContext.validateRequest(context, clientSubject, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(authStatus));
        given(context.getAuditTrail()).willReturn(auditTrail);
        clientSubject.getPrincipals().add(principalOne);
        clientSubject.getPrincipals().add(principalTwo);
        given(principalTwo.getName()).willReturn("PRINCIPAL_NAME");

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthContexts.withAuditing(authContext)
                .validateRequest(context, clientSubject, serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(authStatus);
        verify(auditTrail).completeAuditAsSuccessful("PRINCIPAL_NAME");
        verify(auditTrail).audit();
    }

    @DataProvider(name = "failedAndInvalidValidateRequestResultAuditingContext")
    private Object[][] getFailedAndInvalidValidateRequestResultAuditingContextData() {
        return new Object[][]{
            {AuthStatus.SEND_FAILURE},
            {AuthStatus.FAILURE},
            {null},
        };
    }

    @Test(dataProvider = "failedAndInvalidValidateRequestResultAuditingContext")
    public void auditingAuthContextShouldAuditFailedAndInvalidValidateRequestResult(AuthStatus authStatus) {

        //Given
        AsyncServerAuthContext authContext = mock(AsyncServerAuthContext.class);
        MessageContext context = mock(MessageContext.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        AuditTrail auditTrail = mock(AuditTrail.class);

        given(authContext.validateRequest(context, clientSubject, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(authStatus));
        given(context.getAuditTrail()).willReturn(auditTrail);

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthContexts.withAuditing(authContext)
                .validateRequest(context, clientSubject, serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(authStatus);
        verify(auditTrail).completeAuditAsFailure();
        verify(auditTrail).audit();
    }

    @Test
    public void auditingAuthContextShouldAuditValidateRequestAuthenticationException() {

        //Given
        AsyncServerAuthContext authContext = mock(AsyncServerAuthContext.class);
        MessageContext context = mock(MessageContext.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        AuditTrail auditTrail = mock(AuditTrail.class);
        AuthenticationException exception = new AuthenticationException("ERROR");

        given(authContext.validateRequest(context, clientSubject, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newFailedPromise(
                        exception));
        given(context.getAuditTrail()).willReturn(auditTrail);

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthContexts.withAuditing(authContext)
                .validateRequest(context, clientSubject, serviceSubject);

        //Then
        assertThat(promise).failedWithException().hasMessage("ERROR");
        verify(auditTrail).completeAuditAsFailure(exception);
        verify(auditTrail).audit();
    }

    @Test
    public void auditingAuthContextShouldAuditNullPrincipalWhenPrincipalNotSetOnClientSubject() {

        //Given
        AsyncServerAuthContext authContext = mock(AsyncServerAuthContext.class);
        MessageContext context = mock(MessageContext.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        AuditTrail auditTrail = mock(AuditTrail.class);

        given(authContext.validateRequest(context, clientSubject, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(AuthStatus.SUCCESS));
        given(context.getAuditTrail()).willReturn(auditTrail);

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthContexts.withAuditing(authContext)
                .validateRequest(context, clientSubject, serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(AuthStatus.SUCCESS);
        verify(auditTrail).completeAuditAsSuccessful(null);
        verify(auditTrail).audit();
    }

    @DataProvider(name = "validateRequestResultLoggingContext")
    private Object[][] getValidateRequestResultLoggingContextData() {
        return new Object[][]{
            {AuthStatus.SUCCESS, "Success", false},
            {AuthStatus.SEND_SUCCESS, "Success", false},
            {AuthStatus.SEND_CONTINUE, "not finished", false},
            {AuthStatus.SEND_FAILURE, "Failed", false},
            {AuthStatus.FAILURE, "Invalid AuthStatus", true},
            {null, "Invalid AuthStatus", true},
        };
    }

    @Test(dataProvider = "validateRequestResultLoggingContext")
    public void loggingAuthContextShouldLogValidateRequestResult(AuthStatus authStatus, String expectedMessage,
            boolean isError) {

        //Given
        Logger logger = mock(Logger.class);
        AsyncServerAuthContext authContext = mock(AsyncServerAuthContext.class);
        MessageContext context = mock(MessageContext.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        given(authContext.validateRequest(context, clientSubject, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(authStatus));

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthContexts.withLogging(logger, authContext)
                .validateRequest(context, clientSubject, serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(authStatus);
        if (isError) {
            verify(logger).error(contains(expectedMessage), eq(asString(authStatus)));
        } else {
            verify(logger).debug(contains(expectedMessage));
        }
    }

    @Test
    public void loggingAuthContextShouldLogValidateRequestAuthenticationException() {

        //Given
        Logger logger = mock(Logger.class);
        AsyncServerAuthContext authContext = mock(AsyncServerAuthContext.class);
        MessageContext context = mock(MessageContext.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        AuthenticationException exception = new AuthenticationException("ERROR");

        given(authContext.validateRequest(context, clientSubject, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newFailedPromise(exception));

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthContexts.withLogging(logger, authContext)
                .validateRequest(context, clientSubject, serviceSubject);

        //Then
        assertThat(promise).failedWithException().hasMessage("ERROR");
        verify(logger).error("ERROR", exception);
    }

    @DataProvider(name = "secureResponseResultLoggingContext")
    private Object[][] getSecureResponseResultLoggingContextData() {
        return new Object[][]{
            {AuthStatus.SEND_SUCCESS, "Success", false},
            {AuthStatus.SEND_CONTINUE, "not finished", false},
            {AuthStatus.SEND_FAILURE, "Failed", false},
            {AuthStatus.SUCCESS, "Invalid AuthStatus", true},
            {AuthStatus.FAILURE, "Invalid AuthStatus", true},
            {null, "Invalid AuthStatus", true},
        };
    }

    @Test(dataProvider = "secureResponseResultLoggingContext")
    public void loggingAuthContextShouldLogSecureResponseResult(AuthStatus authStatus, String expectedMessage,
            boolean isError) {

        //Given
        Logger logger = mock(Logger.class);
        AsyncServerAuthContext authContext = mock(AsyncServerAuthContext.class);
        MessageContext context = mock(MessageContext.class);
        Subject serviceSubject = new Subject();

        given(authContext.secureResponse(context, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(authStatus));

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthContexts.withLogging(logger, authContext)
                .secureResponse(context, serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(authStatus);
        if (isError) {
            verify(logger).error(contains(expectedMessage), eq(asString(authStatus)));
        } else {
            verify(logger).debug(contains(expectedMessage));
        }
    }

    @Test
    public void loggingAuthContextShouldLogSecureResponseAuthenticationException() {

        //Given
        Logger logger = mock(Logger.class);
        AsyncServerAuthContext authContext = mock(AsyncServerAuthContext.class);
        MessageContext context = mock(MessageContext.class);
        Subject serviceSubject = new Subject();
        AuthenticationException exception = new AuthenticationException("ERROR");

        given(authContext.secureResponse(context, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newFailedPromise(exception));

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthContexts.withLogging(logger, authContext)
                .secureResponse(context, serviceSubject);

        //Then
        assertThat(promise).failedWithException().hasMessage("ERROR");
        verify(logger).error("ERROR", exception);
    }

    @Test
    public void loggingAuthContextShouldLogCleanSubject() {

        //Given
        Logger logger = mock(Logger.class);
        AsyncServerAuthContext authContext = mock(AsyncServerAuthContext.class);
        MessageContext context = mock(MessageContext.class);
        Subject clientSubject = new Subject();

        given(authContext.cleanSubject(context, clientSubject))
                .willReturn(Promises.<Void, AuthenticationException>newSuccessfulPromise(null));

        //When
        Promise<Void, AuthenticationException> promise = AuthContexts.withLogging(logger, authContext)
                .cleanSubject(context, clientSubject);

        //Then
        assertThat(promise).succeeded();
        verifyZeroInteractions(logger);
    }

    @Test
    public void loggingAuthContextShouldLogCleanSubjectAuthenticationException() {

        //Given
        Logger logger = mock(Logger.class);
        AsyncServerAuthContext authContext = mock(AsyncServerAuthContext.class);
        MessageContext context = mock(MessageContext.class);
        Subject clientSubject = new Subject();
        AuthenticationException exception = new AuthenticationException("ERROR");

        given(authContext.cleanSubject(context, clientSubject))
                .willReturn(Promises.<Void, AuthenticationException>newFailedPromise(exception));

        //When
        Promise<Void, AuthenticationException> promise = AuthContexts.withLogging(logger, authContext)
                .cleanSubject(context, clientSubject);

        //Then
        assertThat(promise).failedWithException().hasMessage("ERROR");
        verify(logger).error("ERROR", exception);
    }
}
