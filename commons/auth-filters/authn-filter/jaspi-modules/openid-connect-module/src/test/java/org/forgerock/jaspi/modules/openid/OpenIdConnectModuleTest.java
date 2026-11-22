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
* Portions Copyright 2020-2026 3A Systems, LLC.
*/

package org.forgerock.jaspi.modules.openid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessagePolicy;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.MessageInfoContext;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.jaspi.modules.openid.exceptions.OpenIdConnectVerificationException;
import org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver;
import org.forgerock.jaspi.modules.openid.resolvers.service.OpenIdResolverService;
import org.forgerock.jaspi.modules.openid.resolvers.service.OpenIdResolverServiceConfigurator;
import org.forgerock.jaspi.modules.openid.resolvers.service.OpenIdResolverServiceConfiguratorImpl;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.exceptions.InvalidJwtException;
import org.forgerock.json.jose.exceptions.JwtReconstructionException;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class OpenIdConnectModuleTest {

    private static final String ISSUER = "https://issuer.example";
    private static final String SECOND_ISSUER = "https://second-issuer.example";
    private static final String CLIENT_ID = "client-one";
    private static final String SECOND_CLIENT_ID = "client-two";
    private static final String TEST_SECRET = "unit-test-secret-32-bytes-or-more";

    OpenIdConnectModule testModule;
    OpenIdResolverServiceConfigurator mockConfigurator;
    JwtReconstruction mockReconstruction;
    Map<String, OpenIdResolver> resolvers;
    CallbackHandler mockCallback;

    @BeforeMethod
    public void setUp() {
        mockConfigurator = mock(OpenIdResolverServiceConfigurator.class);
        mockReconstruction = mock(JwtReconstruction.class);
        resolvers = new HashMap<>();
        mockCallback = mock(CallbackHandler.class);
        testModule = new OpenIdConnectModule(mockConfigurator, mockReconstruction, resolvers, mockCallback,
                OpenIdConnectModule.HEADER_KEY);
    }

    private Map<String, Object> getConfig() throws UnsupportedEncodingException {

        Map<String, Object> options = new HashMap<>();
        options.put(OpenIdConnectModule.HEADER_KEY, OpenIdConnectModule.HEADER_KEY);
        options.put(OpenIdConnectModule.RESOLVERS_KEY, List.of(resolverConfig(ISSUER, CLIENT_ID)));

        return options;
    }

    @Test(expectedExceptions = AuthException.class)
    public void shouldThrowAuthExceptionWithNoHeaderInConfig() throws Exception {
        //given
        MessagePolicy requestPolicy = mock(MessagePolicy.class);
        MessagePolicy responsePolicy =  mock(MessagePolicy.class);
        CallbackHandler callback =  mock(CallbackHandler.class);
        Map<String, Object> config = new HashMap<>();

        //when
        testModule.initialize(requestPolicy, responsePolicy, callback, config).getOrThrowUninterruptibly();

        //then - covered by caught exception
    }

    @Test(expectedExceptions = AuthException.class)
    public void shouldThrowAuthExceptionWhenConfigureServiceFails() throws Exception {
        //given
        MessagePolicy requestPolicy = mock(MessagePolicy.class);
        MessagePolicy responsePolicy =  mock(MessagePolicy.class);
        CallbackHandler callback =  mock(CallbackHandler.class);
        Map<String, Object> config = getConfig();

        given(mockConfigurator.configureService(any(OpenIdResolverService.class), any(List.class))).willReturn(false);

        //when
        testModule.initialize(requestPolicy, responsePolicy, callback, config).getOrThrowUninterruptibly();

        //then - covered by caught exception
    }

    @Test
    public void shouldReturnFailureWhenNoJwtInRequest() throws AuthException {
        //given
        Request request = new Request();
        Response response = new Response();

        MessageInfoContext mockMessage = mock(MessageInfoContext.class);
        String jwtInRequest = null;

        given(mockMessage.getRequest()).willReturn(request);
        given(mockMessage.getResponse()).willReturn(response);

        request.getHeaders().put(OpenIdConnectModule.HEADER_KEY, jwtInRequest);

        //when
        AuthStatus res = testModule.validateRequest(mockMessage, null, null).getOrThrowUninterruptibly();

        //then
        assertEquals(res, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void shouldReturnFailureWhenEmptyJwtInRequest() throws AuthException {
        //given
        Request request = new Request();
        Response response = new Response();

        MessageInfoContext mockMessage = mock(MessageInfoContext.class);
        String jwtInRequest = "";

        given(mockMessage.getRequest()).willReturn(request);
        given(mockMessage.getResponse()).willReturn(response);

        request.getHeaders().put(OpenIdConnectModule.HEADER_KEY, jwtInRequest);

        //when
        AuthStatus res = testModule.validateRequest(mockMessage, null, null).getOrThrowUninterruptibly();

        //then
        assertEquals(res, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void shouldReturnFailureWhenInvalidJwtInRequest() throws AuthException {
        //given
        Request request = new Request();
        Response response = new Response();

        MessageInfoContext mockMessage = mock(MessageInfoContext.class);
        String jwtInRequest = "jwt";

        given(mockMessage.getRequest()).willReturn(request);
        given(mockMessage.getResponse()).willReturn(response);

        request.getHeaders().put(OpenIdConnectModule.HEADER_KEY, jwtInRequest);
        doThrow(InvalidJwtException.class).when(mockReconstruction).reconstructJwt(anyString(), any(Class.class));

        //when
        AuthStatus res = testModule.validateRequest(mockMessage, null, null).getOrThrowUninterruptibly();

        //then
        assertEquals(res, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void shouldReturnFailureWhenInvalidJwtPartsInRequest() throws AuthException {
        //given
        Request request = new Request();
        Response response = new Response();

        MessageInfoContext mockMessage = mock(MessageInfoContext.class);
        String jwtInRequest = "jwt";

        given(mockMessage.getRequest()).willReturn(request);
        given(mockMessage.getResponse()).willReturn(response);

        request.getHeaders().put(OpenIdConnectModule.HEADER_KEY, jwtInRequest);
        doThrow(JwtReconstructionException.class)
                .when(mockReconstruction).reconstructJwt(anyString(), any(Class.class));

        //when
        AuthStatus res = testModule.validateRequest(mockMessage, null, null).getOrThrowUninterruptibly();

        //then
        assertEquals(res, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void shouldReturnFailureWhenUnableToFindResolverForIssuer() throws Exception {
        //given
        Request request = new Request();
        Response response = new Response();

        MessageInfoContext mockMessage = mock(MessageInfoContext.class);
        String jwtInRequest = "jwt";
        SignedJwt jws = mock(SignedJwt.class);
        JwtClaimsSet claimSet = mock(JwtClaimsSet.class);

        given(jws.getClaimsSet()).willReturn(claimSet);

        given(mockMessage.getRequest()).willReturn(request);
        given(mockMessage.getResponse()).willReturn(response);

        request.getHeaders().put(OpenIdConnectModule.HEADER_KEY, jwtInRequest);
        given(mockReconstruction.reconstructJwt(anyString(), any(Class.class))).willReturn(jws);
        given(claimSet.getIssuer()).willReturn("");

        //when
        AuthStatus res = testModule.validateRequest(mockMessage, null, null).getOrThrowUninterruptibly();

        //then
        assertEquals(res, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void shouldReturnFailureWhenUnableToValidateJwtWithCorrectResolver() throws Exception {
        //given
        OpenIdResolver mockResolver = mock(OpenIdResolver.class);
        Request request = new Request();
        Response response = new Response();

        MessageInfoContext mockMessage = mock(MessageInfoContext.class);
        String jwtInRequest = "jwt";
        SignedJwt jws = mock(SignedJwt.class);
        JwtClaimsSet claimSet = mock(JwtClaimsSet.class);

        given(jws.getClaimsSet()).willReturn(claimSet);

        given(mockMessage.getRequest()).willReturn(request);
        given(mockMessage.getResponse()).willReturn(response);

        request.getHeaders().put(OpenIdConnectModule.HEADER_KEY, jwtInRequest);
        given(mockReconstruction.reconstructJwt(anyString(), any(Class.class))).willReturn(jws);
        given(claimSet.getIssuer()).willReturn("");

        resolvers.put("", mockResolver);
        doThrow(new OpenIdConnectVerificationException()).when(mockResolver).validateIdentity(jws);

        //when
        AuthStatus res = testModule.validateRequest(mockMessage, null, null).getOrThrowUninterruptibly();

        //then
        assertEquals(res, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void shouldReturnFailureWhenUnableToSetPrincipal() throws Exception {
        //given
        OpenIdResolver mockResolver = mock(OpenIdResolver.class);
        Request request = new Request();
        Response response = new Response();

        MessageInfoContext mockMessage = mock(MessageInfoContext.class);
        String jwtInRequest = "jwt";
        SignedJwt jws = mock(SignedJwt.class);
        JwtClaimsSet claimSet = mock(JwtClaimsSet.class);

        given(jws.getClaimsSet()).willReturn(claimSet);

        given(mockMessage.getRequest()).willReturn(request);
        given(mockMessage.getResponse()).willReturn(response);

        request.getHeaders().put(OpenIdConnectModule.HEADER_KEY, jwtInRequest);
        given(mockReconstruction.reconstructJwt(anyString(), any(Class.class))).willReturn(jws);
        given(claimSet.getIssuer()).willReturn("");
        resolvers.put("", mockResolver);

        doThrow(new IOException()).when(mockCallback).handle(any(Callback[].class));

        boolean errored = false;

        //when
        AuthStatus res = null;
        try {
            res = testModule.validateRequest(mockMessage, null, null).getOrThrowUninterruptibly();
        } catch (AuthenticationException ae) {
            errored = true;
        }

        //then
        assertTrue(errored);
        verify(mockResolver, times(1)).validateIdentity(jws);
        assertNull(res);
    }

    @DataProvider
    public Object[][] audienceClaims() {
        return new Object[][] {
            {ISSUER, CLIENT_ID, Map.of(), AuthStatus.SUCCESS},
            {ISSUER, List.of(CLIENT_ID), Map.of("azp", CLIENT_ID), AuthStatus.SUCCESS},
            {SECOND_ISSUER, SECOND_CLIENT_ID, Map.of(), AuthStatus.SUCCESS},
            {ISSUER, SECOND_CLIENT_ID, Map.of(), AuthStatus.SEND_FAILURE},
            {SECOND_ISSUER, CLIENT_ID, Map.of(), AuthStatus.SEND_FAILURE},
            {ISSUER, null, Map.of(), AuthStatus.SEND_FAILURE},
            {ISSUER, "", Map.of(), AuthStatus.SEND_FAILURE},
            {ISSUER, List.of(), Map.of(), AuthStatus.SEND_FAILURE},
            {ISSUER, 42, Map.of(), AuthStatus.SEND_FAILURE},
            {ISSUER, List.of(CLIENT_ID, 42), Map.of(), AuthStatus.SEND_FAILURE},
            {ISSUER, List.of(CLIENT_ID, SECOND_CLIENT_ID), Map.of("azp", CLIENT_ID), AuthStatus.SEND_FAILURE},
            {ISSUER, CLIENT_ID, Map.of("azp", SECOND_CLIENT_ID), AuthStatus.SEND_FAILURE},
            {ISSUER, CLIENT_ID, Map.of("azp", 42), AuthStatus.SEND_FAILURE}
        };
    }

    @Test(dataProvider = "audienceClaims")
    public void shouldValidateAudienceForTheConfiguredIssuer(String issuer, Object audience,
            Map<String, Object> additionalClaims, AuthStatus expectedStatus) throws Exception {
        Map<String, Object> config = getConfig();
        config.put(OpenIdConnectModule.RESOLVERS_KEY,
                List.of(resolverConfig(ISSUER, CLIENT_ID), resolverConfig(SECOND_ISSUER, SECOND_CLIENT_ID)));
        OpenIdConnectModule module = newModule();
        module.initialize(null, null, mockCallback, config).getOrThrowUninterruptibly();

        JwtClaimsSet claims = new JwtClaimsSet(additionalClaims);
        claims.setIssuer(issuer);
        claims.setSubject("test-subject");
        claims.setExpirationTime(new Date(System.currentTimeMillis() + 60_000));
        if (audience != null) {
            claims.put("aud", audience);
        }
        SignedJwt jwt = mock(SignedJwt.class);
        given(jwt.getClaimsSet()).willReturn(claims);
        given(jwt.verify(any(SigningHandler.class))).willReturn(true);
        given(mockReconstruction.reconstructJwt("test-jwt", SignedJwt.class)).willReturn(jwt);
        Request request = new Request();
        request.getHeaders().put(OpenIdConnectModule.HEADER_KEY, "test-jwt");
        MessageInfoContext message = mock(MessageInfoContext.class);
        given(message.getRequest()).willReturn(request);

        AuthStatus result = module.validateRequest(message, new Subject(), null).getOrThrowUninterruptibly();

        assertEquals(result, expectedStatus);
        if (expectedStatus == AuthStatus.SEND_FAILURE) {
            verifyZeroInteractions(mockCallback);
        }
    }

    @DataProvider
    public Object[][] invalidClientIds() {
        return new Object[][] {{null}, {""}, {"  "}, {42}};
    }

    @Test(dataProvider = "invalidClientIds", expectedExceptions = AuthenticationException.class)
    public void shouldRejectResolverWithoutAClientId(Object clientId) throws Exception {
        Map<String, Object> config = getConfig();
        Map<String, Object> resolver = new HashMap<>(resolverConfig(ISSUER, CLIENT_ID));
        resolver.put(OpenIdConnectModule.CLIENT_ID_KEY, clientId);
        config.put(OpenIdConnectModule.RESOLVERS_KEY, List.of(resolver));

        newModule().initialize(null, null, mockCallback, config).getOrThrowUninterruptibly();
    }

    private OpenIdConnectModule newModule() {
        return new OpenIdConnectModule(new OpenIdResolverServiceConfiguratorImpl(), mockReconstruction,
                Map.of(), mockCallback, OpenIdConnectModule.HEADER_KEY);
    }

    private Map<String, String> resolverConfig(String issuer, String clientId) {
        return Map.of(OpenIdResolver.ISSUER_KEY, issuer, OpenIdResolver.CLIENT_SECRET_KEY, TEST_SECRET,
                OpenIdConnectModule.CLIENT_ID_KEY, clientId);
    }

    @Test
    public void shouldReturnSecureSuccess() throws Exception {

        Subject mockSubject = null;
        MessageInfoContext mockInfo = mock(MessageInfoContext.class);

        AuthStatus response = testModule.secureResponse(mockInfo, mockSubject).getOrThrowUninterruptibly();

        //then
        assertEquals(response, AuthStatus.SEND_SUCCESS);
    }

    @Test
    public void shouldGetSupportedMessageTypes() {
        //Given

        //When
        Collection<Class<?>> supportedMessageTypes = testModule.getSupportedMessageTypes();

        //Then
        assertThat(supportedMessageTypes)
                .hasSize(2)
                .containsExactly(Request.class, Response.class);
    }

}
