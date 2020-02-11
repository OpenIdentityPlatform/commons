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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.authz.modules.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.authz.modules.oauth2.OAuth2Module.AccessTokenValidationCacheFactory;
import static org.forgerock.authz.modules.oauth2.OAuth2Module.OAUTH2_PROFILE_INFO_CONTEXT_KEY;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.forgerock.authz.filter.api.AuthorizationContext;
import org.forgerock.authz.filter.api.AuthorizationException;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class OAuth2ModuleTest {

    private OAuth2Module oAuth2Module;

    private AccessTokenValidationCacheFactory cacheFactory;
    private OAuth2AccessTokenValidator tokenValidator;
    private AccessTokenValidationCache cache;

    @BeforeMethod
    public void setUp() throws AuthorizationException {

        cacheFactory = mock(AccessTokenValidationCacheFactory.class);
        tokenValidator = mock(OAuth2AccessTokenValidator.class);

        cache = mock(AccessTokenValidationCache.class);
        given(cacheFactory.getCache(anyInt())).willReturn(cache);
    }

    private void createOAuth2Module(boolean cacheEnabled, String... requiredScopes) {
        oAuth2Module = new OAuth2Module(cacheFactory, tokenValidator,
                new HashSet<>(Arrays.asList(requiredScopes)), cacheEnabled, 10);
    }

    @Test
    public void shouldReturnTrueWhenUsingCacheWithEntryPresentAndTokenValid() {

        //Given
        String accessToken = "ACCESS_TOKEN";
        AuthorizationContext context = mock(AuthorizationContext.class);
        AccessTokenValidationResponse validationResponse = mock(AccessTokenValidationResponse.class);

        createOAuth2Module(true);
        given(cache.get("ACCESS_TOKEN")).willReturn(validationResponse);
        given(validationResponse.isTokenValid()).willReturn(true);
        given(validationResponse.getTokenScopes()).willReturn(Collections.singleton("SCOPE_A"));

        //When
        AuthorizationResult result = oAuth2Module.authorize(accessToken, context).getOrThrowUninterruptibly();

        //Then
        assertThat(result.isAuthorized()).isTrue();
    }

    @Test
    public void shouldReturnFalseWhenTokenHasExpired() {

        //Given
        String accessToken = "ACCESS_TOKEN";
        AuthorizationContext context = mock(AuthorizationContext.class);
        AccessTokenValidationResponse validationResponse = mock(AccessTokenValidationResponse.class);

        createOAuth2Module(true);
        given(cache.get("ACCESS_TOKEN")).willReturn(validationResponse);
        given(validationResponse.isTokenValid()).willReturn(false);
        given(validationResponse.getTokenScopes()).willReturn(Collections.singleton("SCOPE_A"));

        //When
        AuthorizationResult result = oAuth2Module.authorize(accessToken, context).getOrThrowUninterruptibly();

        //Then
        assertThat(result.isAuthorized()).isFalse();
    }

    @Test
    public void shouldReturnFalseWhenMissingRequiredScope() {

        //Given
        String accessToken = "ACCESS_TOKEN";
        AuthorizationContext context = mock(AuthorizationContext.class);
        AccessTokenValidationResponse validationResponse = mock(AccessTokenValidationResponse.class);

        createOAuth2Module(true, "SCOPE_A", "SCOPE_B");
        given(cache.get("ACCESS_TOKEN")).willReturn(validationResponse);
        given(validationResponse.isTokenValid()).willReturn(true);
        given(validationResponse.getTokenScopes()).willReturn(Collections.singleton("SCOPE_A"));

        //When
        AuthorizationResult result = oAuth2Module.authorize(accessToken, context).getOrThrowUninterruptibly();

        //Then
        assertThat(result.isAuthorized()).isFalse();
    }

    @Test
    public void shouldReturnTrueWhenUsingCacheWithoutEntryPresentAndTokenValid() {

        //Given
        String accessToken = "ACCESS_TOKEN";
        AuthorizationContext context = mock(AuthorizationContext.class);
        AccessTokenValidationResponse validationResponse = mock(AccessTokenValidationResponse.class);

        createOAuth2Module(true);
        given(cache.get("ACCESS_TOKEN")).willReturn(null);
        given(tokenValidator.validate("ACCESS_TOKEN")).willReturn(
                Promises.<AccessTokenValidationResponse, OAuth2Exception>newResultPromise(validationResponse));
        given(validationResponse.isTokenValid()).willReturn(true);
        given(validationResponse.getTokenScopes()).willReturn(Collections.singleton("SCOPE_A"));

        //When
        Promise<AuthorizationResult, AuthorizationException> promise = oAuth2Module.authorize(accessToken, context);

        //Then
        assertTrue(promise.isDone());
        assertTrue(promise.getOrThrowUninterruptibly().isAuthorized());
    }

    @Test
    public void shouldUseCacheOnSubsequentRequestsWhenCacheEnabled() {

        //Given
        String accessToken = "ACCESS_TOKEN";
        AuthorizationContext context = mock(AuthorizationContext.class);
        AccessTokenValidationResponse validationResponse = mock(AccessTokenValidationResponse.class);
        Promise<AccessTokenValidationResponse, OAuth2Exception> validationResponsePromise =
                newResultPromise(validationResponse);

        createOAuth2Module(true);
        given(cache.get("ACCESS_TOKEN"))
                .willReturn(null)
                .willReturn(validationResponse);
        given(tokenValidator.validate("ACCESS_TOKEN")).willReturn(validationResponsePromise);
        given(validationResponse.isTokenValid()).willReturn(true);
        given(validationResponse.getTokenScopes()).willReturn(Collections.singleton("SCOPE_A"));

        oAuth2Module.authorize(accessToken, context);

        //When
        oAuth2Module.authorize(accessToken, context);

        //Then
        verify(tokenValidator, times(1)).validate("ACCESS_TOKEN");
        verify(cache, times(2)).get("ACCESS_TOKEN");
    }

    @Test
    public void shouldReturnTrueWhenNotUsingCacheTokenValid() {

        //Given
        String accessToken = "ACCESS_TOKEN";
        AuthorizationContext context = mock(AuthorizationContext.class);
        AccessTokenValidationResponse validationResponse = mock(AccessTokenValidationResponse.class);
        Promise<AccessTokenValidationResponse, OAuth2Exception> validationResponsePromise =
                newResultPromise(validationResponse);

        createOAuth2Module(false);
        given(tokenValidator.validate("ACCESS_TOKEN")).willReturn(validationResponsePromise);
        given(validationResponse.isTokenValid()).willReturn(true);
        given(validationResponse.getTokenScopes()).willReturn(Collections.singleton("SCOPE_A"));

        //When
        Promise<AuthorizationResult, AuthorizationException> promise = oAuth2Module.authorize(accessToken, context);

        //Then
        assertTrue(promise.isDone());
        assertTrue(promise.getOrThrowUninterruptibly().isAuthorized());
        verifyZeroInteractions(cache);
    }

    @Test
    public void shouldAddProfileInfoToContext() {

        //Given
        String accessToken = "ACCESS_TOKEN";
        AuthorizationContext context = mock(AuthorizationContext.class);
        AccessTokenValidationResponse validationResponse = mock(AccessTokenValidationResponse.class);
        Promise<AccessTokenValidationResponse, OAuth2Exception> validationResponsePromise =
                newResultPromise(validationResponse);
        Map<String, Object> profileInfo = new HashMap<>();

        createOAuth2Module(false);
        given(tokenValidator.validate("ACCESS_TOKEN")).willReturn(validationResponsePromise);
        given(validationResponse.isTokenValid()).willReturn(true);
        given(validationResponse.getProfileInformation()).willReturn(profileInfo);

        //When
        oAuth2Module.authorize(accessToken, context);

        //Then
        verify(context).setAttribute(OAUTH2_PROFILE_INFO_CONTEXT_KEY, profileInfo);
    }

    @Test
    public void shouldNotUseCacheOnSubsequentRequestsWhenCacheNotEnabled() {

        //Given
        String accessToken = "ACCESS_TOKEN";
        AuthorizationContext context = mock(AuthorizationContext.class);
        AccessTokenValidationResponse validationResponse = mock(AccessTokenValidationResponse.class);
        Promise<AccessTokenValidationResponse, OAuth2Exception> validationResponsePromise =
                newResultPromise(validationResponse);

        createOAuth2Module(false);
        given(tokenValidator.validate("ACCESS_TOKEN")).willReturn(validationResponsePromise);
        given(validationResponse.isTokenValid()).willReturn(true);
        given(validationResponse.getTokenScopes()).willReturn(Collections.singleton("SCOPE_A"));

        oAuth2Module.authorize(accessToken, context);

        //When
        oAuth2Module.authorize(accessToken, context);

        //Then
        verify(tokenValidator, times(2)).validate("ACCESS_TOKEN");
        verifyZeroInteractions(cache);
    }

    @Test (expectedExceptions = AuthorizationException.class)
    public void shouldThrowAuthorizationExceptionWhenOAuth2ExceptionThrown() {

        //Given
        String accessToken = "ACCESS_TOKEN";
        AuthorizationContext context = mock(AuthorizationContext.class);

        createOAuth2Module(false);
        OAuth2Exception mock = mock(OAuth2Exception.class);
        given(tokenValidator.validate("ACCESS_TOKEN")).willReturn(
                Promises.<AccessTokenValidationResponse, OAuth2Exception>newExceptionPromise(mock));

        //When
        oAuth2Module.authorize(accessToken, context).getOrThrowUninterruptibly();

        //Then
        // Expected AuthorizationException
    }
}
