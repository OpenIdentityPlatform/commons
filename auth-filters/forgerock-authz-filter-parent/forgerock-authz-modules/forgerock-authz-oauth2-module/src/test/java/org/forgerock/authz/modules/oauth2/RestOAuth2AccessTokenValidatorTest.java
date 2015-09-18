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

package org.forgerock.authz.modules.oauth2;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.HashMap;
import java.util.Map;

import org.forgerock.http.Client;
import org.forgerock.services.context.Context;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RestOAuth2AccessTokenValidatorTest {

    private RestOAuth2AccessTokenValidator accessTokenValidator;

    @Mock
    private Handler httpClientHandler;

    @BeforeMethod
    public void setUp() {
        initMocks(this);
        JsonValue config = json(object(
                field("token-info-endpoint", "TOKEN_INFO"),
                field("user-info-endpoint", "USER-PROFILE")
        ));
        accessTokenValidator = new RestOAuth2AccessTokenValidator(config, new Client(httpClientHandler));
    }

    @Test
    public void shouldReturnInvalidAccessTokenResponse() throws Exception {

        //Given
        String accessToken = "ACCESS_TOKEN";
        final Map<String, Object> tokenInfoResponse = new HashMap<>();
        tokenInfoResponse.put("error", "ERROR");

        mockHttpClientHandler(tokenInfoResponse);

        //When
        AccessTokenValidationResponse validate = accessTokenValidator.validate(accessToken).getOrThrowUninterruptibly();

        //Then
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        verify(httpClientHandler).handle(any(Context.class), captor.capture());
        assertThat(captor.getValue().getUri().getRawQuery()).isEqualTo("access_token=ACCESS_TOKEN");
        assertThat(validate.isTokenValid()).isFalse();
        assertThat(validate.getProfileInformation()).isEmpty();
        assertThat(validate.getTokenScopes()).isEmpty();
    }

    private void mockHttpClientHandler(final Map<String, Object>... responses) {
        BDDMockito.BDDMyOngoingStubbing<Promise<Response, NeverThrowsException>> given =
                given(httpClientHandler.handle(any(Context.class), any(Request.class)));
        for (final Map<String, Object> response : responses) {
            given = given.willAnswer(new Answer<Promise<Response, NeverThrowsException>>() {
                @Override
                public Promise<Response, NeverThrowsException> answer(
                        InvocationOnMock invocationOnMock) {
                    return newResultPromise(new Response(Status.OK).setEntity(response));
                }
            });
        }
    }

    @Test
    public void shouldReturnInvalidAccessTokenResponseWhenTokenIsExpired() throws Exception {

        //Given
        String accessToken = "ACCESS_TOKEN";
        Map<String, Object> tokenInfoResponse = new HashMap<>();
        tokenInfoResponse.put("expires_in", -1);
        tokenInfoResponse.put("scope", "A B C");

        mockHttpClientHandler(tokenInfoResponse);

        //When
        AccessTokenValidationResponse validate = accessTokenValidator.validate(accessToken).getOrThrowUninterruptibly();

        //Then
        assertThat(validate.isTokenValid()).isFalse();
        assertThat(validate.getProfileInformation()).isEmpty();
        assertThat(validate.getTokenScopes()).hasSize(3);
    }

    /**
     * This test mimics a validation response that was placed in cache and re-used after some time.
     */
    @Test
    public void shouldReturnValidAccessTokenResponseWhenTokenIsNotExpired() throws Exception {

        //Given
        JsonValue config = json(object(
                field("token-info-endpoint", "TOKEN_INFO")
                // No user info endpoint
        ));
        accessTokenValidator = new RestOAuth2AccessTokenValidator(config, new Client(httpClientHandler));
        String accessToken = "ACCESS_TOKEN";
        Map<String, Object> tokenInfoResponse = new HashMap<>();
        tokenInfoResponse.put("expires_in", 10);
        tokenInfoResponse.put("scope", "A B C");

        mockHttpClientHandler(tokenInfoResponse);

        //When
        AccessTokenValidationResponse validate = accessTokenValidator.validate(accessToken).getOrThrowUninterruptibly();

        // Mimics a delay before re-using the response
        Thread.sleep(50);

        //Then
        assertThat(validate.isTokenValid()).isTrue();
        assertThat(validate.getProfileInformation()).isEmpty();
        assertThat(validate.getTokenScopes()).hasSize(3);
    }

    @Test
    public void shouldReturnInvalidAccessTokenResponseWithScope() throws Exception {

        //Given
        String accessToken = "ACCESS_TOKEN";
        Map<String, Object> tokenInfoResponse = new HashMap<>();
        tokenInfoResponse.put("expires_in", -1);
        tokenInfoResponse.put("scope", "A B C");

        mockHttpClientHandler(tokenInfoResponse);

        //When
        AccessTokenValidationResponse validate = accessTokenValidator.validate(accessToken).getOrThrowUninterruptibly();

        //Then
        assertThat(validate.isTokenValid()).isFalse();
        assertThat(validate.getProfileInformation()).isEmpty();
        assertThat(validate.getTokenScopes()).hasSize(3);
    }

    @Test
    public void shouldReturnValidAccessTokenResponseWithScopeAndUserProfile() throws Exception {

        //Given
        String accessToken = "ACCESS_TOKEN";
        Map<String, Object> tokenInfoResponse = new HashMap<>();
        tokenInfoResponse.put("expires_in", 1);
        tokenInfoResponse.put("scope", "A B C");

        Map<String, Object> profileResponse = new HashMap<>();
        profileResponse.put("KEY1", "VAL");
        profileResponse.put("KEY2", "VAL");

        mockHttpClientHandler(tokenInfoResponse, profileResponse);

        //When
        AccessTokenValidationResponse validate = accessTokenValidator.validate(accessToken).getOrThrowUninterruptibly();

        //Then
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        verify(httpClientHandler, times(2)).handle(any(Context.class), captor.capture());
        assertThat(captor.getAllValues()).hasSize(2);
        assertThat(captor.getAllValues().get(1).getHeaders().getFirst("Authorization"))
                .isEqualTo("Bearer ACCESS_TOKEN");
        assertThat(validate.isTokenValid()).isTrue();
        assertThat(validate.getProfileInformation()).hasSize(2);
        assertThat(validate.getTokenScopes()).hasSize(3);
    }

    /**
     * The userInfo endpoint configuration is optional. If it is not specified, then no attempt should be made to fetch
     * any user profile information.
     */
    @Test
    public void shouldNotFetchUserProfileIfEndPointNotConfigured() throws Exception {
        // Given
        JsonValue config = json(object(
                field("token-info-endpoint", "TOKEN_INFO")
                // No user info endpoint
        ));
        accessTokenValidator = new RestOAuth2AccessTokenValidator(config, new Client(httpClientHandler));
        String accessToken = "ACCESS_TOKEN";
        Map<String, Object> tokenInfoResponse = new HashMap<>();
        tokenInfoResponse.put("expires_in", 1);
        tokenInfoResponse.put("scope", "A B C");

        mockHttpClientHandler(tokenInfoResponse);


        // When
        AccessTokenValidationResponse response = accessTokenValidator.validate(accessToken).getOrThrowUninterruptibly();

        // Then
        verify(httpClientHandler, times(1)).handle(any(Context.class), any(Request.class));
        assertThat(response.getProfileInformation()).isEmpty();
    }

    @Test
    public void shouldAcceptScopeAsJsonArray() throws Exception {

        //Given
        String accessToken = "ACCESS_TOKEN";
        Map<String, Object> tokenInfoResponse = new HashMap<>();
        tokenInfoResponse.put("expires_in", -1);
        tokenInfoResponse.put("scope", asList("A", "B", "C"));

        mockHttpClientHandler(tokenInfoResponse);

        //When
        AccessTokenValidationResponse validate = accessTokenValidator.validate(accessToken).getOrThrowUninterruptibly();

        //Then
        assertThat(validate.getTokenScopes()).hasSize(3);
    }
}
