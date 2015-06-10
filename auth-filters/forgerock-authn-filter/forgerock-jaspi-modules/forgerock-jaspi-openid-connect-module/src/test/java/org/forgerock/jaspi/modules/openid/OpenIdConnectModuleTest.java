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

package org.forgerock.jaspi.modules.openid;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.forgerock.jaspi.modules.openid.exceptions.OpenIdConnectVerificationException;
import org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver;
import org.forgerock.jaspi.modules.openid.resolvers.service.OpenIdResolverService;
import org.forgerock.jaspi.modules.openid.resolvers.service.OpenIdResolverServiceConfigurator;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.exceptions.InvalidJwtException;
import org.forgerock.json.jose.exceptions.JwtReconstructionException;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class OpenIdConnectModuleTest {

    OpenIdConnectModule testModule;
    OpenIdResolverServiceConfigurator mockConfigurator;
    JwtReconstruction mockReconstruction;
    OpenIdResolverService mockService;
    CallbackHandler mockCallback;

    @BeforeMethod
    public void setUp() {
        mockConfigurator = mock(OpenIdResolverServiceConfigurator.class);
        mockReconstruction = mock(JwtReconstruction.class);
        mockService = mock(OpenIdResolverService.class);
        mockCallback = mock(CallbackHandler.class);
        testModule = new OpenIdConnectModule(mockConfigurator, mockReconstruction, mockService, mockCallback);
    }

    private Map<String, Object> getConfig() throws UnsupportedEncodingException {

        Map<String, Object> options = new HashMap<>();
        options.put(OpenIdConnectModule.HEADER_KEY, "openam-openid-connect-header");

        return options;
    }

    @Test(expectedExceptions = AuthException.class)
    public void shouldThrowAuthExceptionWithNoHeaderInConfig() throws AuthException, UnsupportedEncodingException {
        //given
        MessagePolicy requestPolicy = mock(MessagePolicy.class);
        MessagePolicy responsePolicy =  mock(MessagePolicy.class);
        CallbackHandler callback =  mock(CallbackHandler.class);
        Map<String, Object> config = new HashMap<>();

        //when
        testModule.initialize(requestPolicy, responsePolicy, callback, config);

        //then - covered by caught exception
    }

    @Test(expectedExceptions = AuthException.class)
    public void shouldThrowAuthExceptionWhenConfigureServiceFails() throws AuthException,
            UnsupportedEncodingException {
        //given
        MessagePolicy requestPolicy = mock(MessagePolicy.class);
        MessagePolicy responsePolicy =  mock(MessagePolicy.class);
        CallbackHandler callback =  mock(CallbackHandler.class);
        Map<String, Object> config = getConfig();

        given(mockConfigurator.configureService(any(OpenIdResolverService.class), any(List.class))).willReturn(false);

        //when
        testModule.initialize(requestPolicy, responsePolicy, callback, config);

        //then - covered by caught exception
    }

    @Test
    public void shouldReturnFailureWhenNoJwtInRequest() throws AuthException {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        MessageInfo mockMessage = mock(MessageInfo.class);
        String jwtInRequest = null;

        given(mockMessage.getRequestMessage()).willReturn(mockRequest);
        given(mockMessage.getResponseMessage()).willReturn(mockResponse);

        given(mockRequest.getHeader(anyString())).willReturn(jwtInRequest);

        //when
        AuthStatus res = testModule.validateRequest(mockMessage, null, null);

        //then
        assertEquals(res, AuthStatus.SEND_FAILURE);
        verifyZeroInteractions(mockResponse);
    }

    @Test
    public void shouldReturnFailureWhenEmptyJwtInRequest() throws AuthException {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        MessageInfo mockMessage = mock(MessageInfo.class);
        String jwtInRequest = "";

        given(mockMessage.getRequestMessage()).willReturn(mockRequest);
        given(mockMessage.getResponseMessage()).willReturn(mockResponse);

        given(mockRequest.getHeader(anyString())).willReturn(jwtInRequest);

        //when
        AuthStatus res = testModule.validateRequest(mockMessage, null, null);

        //then
        assertEquals(res, AuthStatus.SEND_FAILURE);
        verifyZeroInteractions(mockResponse);
    }

    @Test
    public void shouldReturnFailureWhenInvalidJwtInRequest() throws AuthException {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        MessageInfo mockMessage = mock(MessageInfo.class);
        String jwtInRequest = "jwt";

        given(mockMessage.getRequestMessage()).willReturn(mockRequest);
        given(mockMessage.getResponseMessage()).willReturn(mockResponse);

        given(mockRequest.getHeader(anyString())).willReturn(jwtInRequest);
        given(mockReconstruction.reconstructJwt(anyString(), any(Class.class))).willThrow(InvalidJwtException.class);

        //when
        AuthStatus res = testModule.validateRequest(mockMessage, null, null);

        //then
        assertEquals(res, AuthStatus.SEND_FAILURE);
        verifyZeroInteractions(mockResponse);
    }



    @Test
    public void shouldReturnFailureWhenInvalidJwtPartsInRequest() throws AuthException {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        MessageInfo mockMessage = mock(MessageInfo.class);
        String jwtInRequest = "jwt";

        given(mockMessage.getRequestMessage()).willReturn(mockRequest);
        given(mockMessage.getResponseMessage()).willReturn(mockResponse);

        given(mockRequest.getHeader(anyString())).willReturn(jwtInRequest);
        given(mockReconstruction.reconstructJwt(anyString(), any(Class.class)))
                .willThrow(JwtReconstructionException.class);

        //when
        AuthStatus res = testModule.validateRequest(mockMessage, null, null);

        //then
        assertEquals(res, AuthStatus.SEND_FAILURE);
        verifyZeroInteractions(mockResponse);
    }

    @Test
    public void shouldReturnFailureWhenUnableToFindResolverForIssuer() throws AuthException,
            UnsupportedEncodingException {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        MessageInfo mockMessage = mock(MessageInfo.class);
        String jwtInRequest = "jwt";
        SignedJwt jws = mock(SignedJwt.class);
        JwtClaimsSet claimSet = mock(JwtClaimsSet.class);

        given(jws.getClaimsSet()).willReturn(claimSet);

        given(mockMessage.getRequestMessage()).willReturn(mockRequest);
        given(mockMessage.getResponseMessage()).willReturn(mockResponse);

        given(mockRequest.getHeader(anyString())).willReturn(jwtInRequest);
        given(mockReconstruction.reconstructJwt(anyString(), any(Class.class))).willReturn(jws);
        given(claimSet.getIssuer()).willReturn("");

        //when
        AuthStatus res = testModule.validateRequest(mockMessage, null, null);

        //then
        assertEquals(res, AuthStatus.SEND_FAILURE);
        verifyZeroInteractions(mockResponse);
    }

    @Test
    public void shouldReturnFailureWhenUnableToValidateJwtWithCorrectResolver() throws UnsupportedEncodingException,
            AuthException, OpenIdConnectVerificationException {
        //given
        OpenIdResolver mockResolver = mock(OpenIdResolver.class);
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        MessageInfo mockMessage = mock(MessageInfo.class);
        String jwtInRequest = "jwt";
        SignedJwt jws = mock(SignedJwt.class);
        JwtClaimsSet claimSet = mock(JwtClaimsSet.class);

        given(jws.getClaimsSet()).willReturn(claimSet);

        given(mockMessage.getRequestMessage()).willReturn(mockRequest);
        given(mockMessage.getResponseMessage()).willReturn(mockResponse);

        given(mockRequest.getHeader(anyString())).willReturn(jwtInRequest);
        given(mockReconstruction.reconstructJwt(anyString(), any(Class.class))).willReturn(jws);
        given(claimSet.getIssuer()).willReturn("");

        doThrow(new OpenIdConnectVerificationException()).when(mockResolver).validateIdentity(jws);

        //when
        AuthStatus res = testModule.validateRequest(mockMessage, null, null);

        //then
        assertEquals(res, AuthStatus.SEND_FAILURE);
        verifyZeroInteractions(mockResponse);
    }

    @Test
    public void shouldReturnFailureWhenUnableToSetPrincipal() throws IOException, AuthException,
            UnsupportedCallbackException, OpenIdConnectVerificationException {
        //given
        OpenIdResolver mockResolver = mock(OpenIdResolver.class);
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        MessageInfo mockMessage = mock(MessageInfo.class);
        String jwtInRequest = "jwt";
        SignedJwt jws = mock(SignedJwt.class);
        JwtClaimsSet claimSet = mock(JwtClaimsSet.class);

        given(jws.getClaimsSet()).willReturn(claimSet);

        given(mockMessage.getRequestMessage()).willReturn(mockRequest);
        given(mockMessage.getResponseMessage()).willReturn(mockResponse);

        given(mockRequest.getHeader(anyString())).willReturn(jwtInRequest);
        given(mockReconstruction.reconstructJwt(anyString(), any(Class.class))).willReturn(jws);
        given(claimSet.getIssuer()).willReturn("");
        given(mockService.getResolverForIssuer(anyString())).willReturn(mockResolver);

        doThrow(new IOException()).when(mockCallback).handle(any(Callback[].class));

        boolean errored = false;

        //when
        AuthStatus res = null;
        try {
            res = testModule.validateRequest(mockMessage, null, null);
        } catch (AuthException ae) {
            errored = true;
        }

        //then
        assertTrue(errored);
        verify(mockResolver, times(1)).validateIdentity(jws);
        verifyZeroInteractions(mockResponse);
        assertNull(res);
    }

    @Test
    public void shouldReturnSecureSuccess() throws AuthException {

        Subject mockSubject = null;
        MessageInfo mockInfo = mock(MessageInfo.class);

        AuthStatus response = testModule.secureResponse(mockInfo, mockSubject);

        //then
        assertEquals(response, AuthStatus.SEND_SUCCESS);
    }

    @Test
    public void shouldGetSupportedMessageTypes() {
        //Given

        //When
        Class[] supportedMessageTypes = testModule.getSupportedMessageTypes();

        //Then
        assertEquals(supportedMessageTypes.length, 2);
        assertEquals(supportedMessageTypes[0], HttpServletRequest.class);
        assertEquals(supportedMessageTypes[1], HttpServletResponse.class);
    }

}
