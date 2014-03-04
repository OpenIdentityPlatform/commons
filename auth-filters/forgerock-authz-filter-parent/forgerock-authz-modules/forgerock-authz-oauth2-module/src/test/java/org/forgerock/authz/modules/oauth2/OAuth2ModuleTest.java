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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.authz.modules.oauth2;

import org.forgerock.authz.AuthorizationContext;
import org.forgerock.authz.AuthorizationException;
import org.forgerock.json.fluent.JsonValue;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.forgerock.authz.modules.oauth2.OAuth2Module.AccessTokenValidationCacheFactory;
import static org.forgerock.authz.modules.oauth2.OAuth2Module.OAUTH2_PROFILE_INFO_CONTEXT_KEY;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class OAuth2ModuleTest {

    private OAuth2Module oAuth2Module;

    private OAuth2AccessTokenValidator tokenValidator;
    private AccessTokenValidationCache cache;

    @BeforeMethod
    public void setUp() throws AuthorizationException {

        AccessTokenValidatorFactory validatorFactory = mock(AccessTokenValidatorFactory.class);
        AccessTokenValidationCacheFactory cacheFactory = mock(AccessTokenValidationCacheFactory.class);

        oAuth2Module = new OAuth2Module(validatorFactory, cacheFactory);

        tokenValidator = mock(OAuth2AccessTokenValidator.class);
        cache = mock(AccessTokenValidationCache.class);

        given(validatorFactory.getInstance(anyString(), Matchers.<JsonValue>anyObject())).willReturn(tokenValidator);
        given(cacheFactory.getCache(anyInt())).willReturn(cache);
    }

    private void initOAuth2Module(final boolean cacheEnabled,
            final String... requiredScopes)
            throws AuthorizationException {
        JsonValue config = JsonValue.json(JsonValue.object(
                JsonValue.field("access-token-validator-class", "VALIDATOR_CLASS_NAME"),
                JsonValue.field("required-scopes", new JsonValue(Arrays.asList(requiredScopes))),
                JsonValue.field("cache-enabled", cacheEnabled),
                JsonValue.field("cache-size", 10)
        ));
        oAuth2Module.initialise(config);
    }

    @Test
    public void shouldReturnFalseWhenAuthorizationHeaderNotSet() {

        //Given
        HttpServletRequest req = mock(HttpServletRequest.class);
        AuthorizationContext context = mock(AuthorizationContext.class);

        //When
        final boolean authorized = oAuth2Module.authorize(req, context);

        //Then
        assertFalse(authorized);
    }

    @Test
    public void shouldReturnFalseWhenAuthorizationInvalid() {

        //Given
        HttpServletRequest req = mock(HttpServletRequest.class);
        AuthorizationContext context = mock(AuthorizationContext.class);

        given(req.getHeader("Authorization")).willReturn("INVALID");

        //When
        final boolean authorized = oAuth2Module.authorize(req, context);

        //Then
        assertFalse(authorized);
    }

    @Test
    public void shouldReturnFalseWhenAuthorizationIsNotBearer() {

        //Given
        HttpServletRequest req = mock(HttpServletRequest.class);
        AuthorizationContext context = mock(AuthorizationContext.class);

        given(req.getHeader("Authorization")).willReturn("MAC ACCESS_TOKEN");

        //When
        final boolean authorized = oAuth2Module.authorize(req, context);

        //Then
        assertFalse(authorized);
    }

    @Test
    public void shouldReturnTrueWhenUsingCacheWithEntryPresentAndTokenValid() {

        //Given
        HttpServletRequest req = mock(HttpServletRequest.class);
        AuthorizationContext context = mock(AuthorizationContext.class);
        AccessTokenValidationResponse validationResponse = mock(AccessTokenValidationResponse.class);

        initOAuth2Module(true);
        given(req.getHeader("Authorization")).willReturn("Bearer ACCESS_TOKEN");
        given(cache.get("ACCESS_TOKEN")).willReturn(validationResponse);
        given(validationResponse.isTokenValid()).willReturn(true);
        given(validationResponse.getTokenScopes()).willReturn(Collections.singleton("SCOPE_A"));

        //When
        final boolean authorized = oAuth2Module.authorize(req, context);

        //Then
        assertTrue(authorized);
    }

    @Test
    public void shouldReturnFalseWhenTokenHasExpired() {

        //Given
        HttpServletRequest req = mock(HttpServletRequest.class);
        AuthorizationContext context = mock(AuthorizationContext.class);
        AccessTokenValidationResponse validationResponse = mock(AccessTokenValidationResponse.class);

        initOAuth2Module(true);
        given(req.getHeader("Authorization")).willReturn("Bearer ACCESS_TOKEN");
        given(cache.get("ACCESS_TOKEN")).willReturn(validationResponse);
        given(validationResponse.isTokenValid()).willReturn(false);
        given(validationResponse.getTokenScopes()).willReturn(Collections.singleton("SCOPE_A"));

        //When
        final boolean authorized = oAuth2Module.authorize(req, context);

        //Then
        assertFalse(authorized);
    }

    @Test
    public void shouldReturnFalseWhenMissingRequiredScope() {

        //Given
        HttpServletRequest req = mock(HttpServletRequest.class);
        AuthorizationContext context = mock(AuthorizationContext.class);
        AccessTokenValidationResponse validationResponse = mock(AccessTokenValidationResponse.class);

        initOAuth2Module(true, "SCOPE_A", "SCOPE_B");
        given(req.getHeader("Authorization")).willReturn("Bearer ACCESS_TOKEN");
        given(cache.get("ACCESS_TOKEN")).willReturn(validationResponse);
        given(validationResponse.isTokenValid()).willReturn(true);
        given(validationResponse.getTokenScopes()).willReturn(Collections.singleton("SCOPE_A"));

        //When
        final boolean authorized = oAuth2Module.authorize(req, context);

        //Then
        assertFalse(authorized);
    }

    @Test
    public void shouldReturnTrueWhenUsingCacheWithoutEntryPresentAndTokenValid() {

        //Given
        HttpServletRequest req = mock(HttpServletRequest.class);
        AuthorizationContext context = mock(AuthorizationContext.class);
        AccessTokenValidationResponse validationResponse = mock(AccessTokenValidationResponse.class);

        initOAuth2Module(true);
        given(req.getHeader("Authorization")).willReturn("Bearer ACCESS_TOKEN");
        given(cache.get("ACCESS_TOKEN")).willReturn(null);
        given(tokenValidator.validate("ACCESS_TOKEN")).willReturn(validationResponse);
        given(validationResponse.isTokenValid()).willReturn(true);
        given(validationResponse.getTokenScopes()).willReturn(Collections.singleton("SCOPE_A"));

        //When
        final boolean authorized = oAuth2Module.authorize(req, context);

        //Then
        assertTrue(authorized);
    }

    @Test
    public void shouldUseCacheOnSubsequentRequestsWhenCacheEnabled() {

        //Given
        HttpServletRequest req = mock(HttpServletRequest.class);
        AuthorizationContext context = mock(AuthorizationContext.class);
        AccessTokenValidationResponse validationResponse = mock(AccessTokenValidationResponse.class);

        initOAuth2Module(true);
        given(req.getHeader("Authorization")).willReturn("Bearer ACCESS_TOKEN");
        given(cache.get("ACCESS_TOKEN"))
                .willReturn(null)
                .willReturn(validationResponse);
        given(tokenValidator.validate("ACCESS_TOKEN")).willReturn(validationResponse);
        given(validationResponse.isTokenValid()).willReturn(true);
        given(validationResponse.getTokenScopes()).willReturn(Collections.singleton("SCOPE_A"));

        oAuth2Module.authorize(req, context);

        //When
        oAuth2Module.authorize(req, context);

        //Then
        verify(tokenValidator, times(1)).validate("ACCESS_TOKEN");
        verify(cache, times(2)).get("ACCESS_TOKEN");
    }

    @Test
    public void shouldReturnTrueWhenNotUsingCacheTokenValid() {

        //Given
        HttpServletRequest req = mock(HttpServletRequest.class);
        AuthorizationContext context = mock(AuthorizationContext.class);
        AccessTokenValidationResponse validationResponse = mock(AccessTokenValidationResponse.class);

        initOAuth2Module(false);
        given(req.getHeader("Authorization")).willReturn("Bearer ACCESS_TOKEN");
        given(tokenValidator.validate("ACCESS_TOKEN")).willReturn(validationResponse);
        given(validationResponse.isTokenValid()).willReturn(true);
        given(validationResponse.getTokenScopes()).willReturn(Collections.singleton("SCOPE_A"));

        //When
        final boolean authorized = oAuth2Module.authorize(req, context);

        //Then
        assertTrue(authorized);
        verifyZeroInteractions(cache);
    }

    @Test
    public void shouldAddProfileInfoToContext() {

        //Given
        HttpServletRequest req = mock(HttpServletRequest.class);
        AuthorizationContext context = mock(AuthorizationContext.class);
        AccessTokenValidationResponse validationResponse = mock(AccessTokenValidationResponse.class);
        Map<String, Object> profileInfo = new HashMap<String, Object>();

        initOAuth2Module(false);
        given(req.getHeader("Authorization")).willReturn("Bearer ACCESS_TOKEN");
        given(tokenValidator.validate("ACCESS_TOKEN")).willReturn(validationResponse);
        given(validationResponse.isTokenValid()).willReturn(true);
        given(validationResponse.getProfileInformation()).willReturn(profileInfo);

        //When
        oAuth2Module.authorize(req, context);

        //Then
        verify(context).setAttribute(OAUTH2_PROFILE_INFO_CONTEXT_KEY, profileInfo);
    }

    @Test
    public void shouldNotUseCacheOnSubsequentRequestsWhenCacheNotEnabled() {

        //Given
        HttpServletRequest req = mock(HttpServletRequest.class);
        AuthorizationContext context = mock(AuthorizationContext.class);
        AccessTokenValidationResponse validationResponse = mock(AccessTokenValidationResponse.class);

        initOAuth2Module(false);
        given(req.getHeader("Authorization")).willReturn("Bearer ACCESS_TOKEN");
        given(tokenValidator.validate("ACCESS_TOKEN")).willReturn(validationResponse);
        given(validationResponse.isTokenValid()).willReturn(true);
        given(validationResponse.getTokenScopes()).willReturn(Collections.singleton("SCOPE_A"));

        oAuth2Module.authorize(req, context);

        //When
        oAuth2Module.authorize(req, context);

        //Then
        verify(tokenValidator, times(2)).validate("ACCESS_TOKEN");
        verifyZeroInteractions(cache);
    }

    @Test (expectedExceptions = AuthorizationException.class)
    public void shouldThrowAuthorizationExceptionWhenOAuth2ExceptionThrown() {

        //Given
        HttpServletRequest req = mock(HttpServletRequest.class);
        AuthorizationContext context = mock(AuthorizationContext.class);

        initOAuth2Module(false);
        given(req.getHeader("Authorization")).willReturn("Bearer ACCESS_TOKEN");
        doThrow(OAuth2Exception.class).when(tokenValidator).validate("ACCESS_TOKEN");

        //When
        oAuth2Module.authorize(req, context);

        //Then
        fail();
    }
}
