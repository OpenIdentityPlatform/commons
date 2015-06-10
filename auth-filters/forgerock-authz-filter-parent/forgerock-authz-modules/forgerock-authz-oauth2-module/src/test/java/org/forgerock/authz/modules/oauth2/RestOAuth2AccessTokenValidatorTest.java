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

import org.forgerock.json.fluent.JsonValue;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class RestOAuth2AccessTokenValidatorTest {

    private RestOAuth2AccessTokenValidator accessTokenValidator;

    @Mock
    private RestResourceFactory restResourceFactory;

    @Mock
    private RestResource tokenInfoRequest;

    @Mock
    private RestResource userProfileRequest;

    @BeforeMethod
    public void setUp() {

        MockitoAnnotations.initMocks(this);

        JsonValue config = JsonValue.json(JsonValue.object(
            JsonValue.field("token-info-endpoint", "TOKEN_INFO"),
            JsonValue.field("user-info-endpoint", "USER-PROFILE")
        ));
        accessTokenValidator = new RestOAuth2AccessTokenValidator(config, restResourceFactory);

        given(restResourceFactory.resource(anyString()))
                .willReturn(tokenInfoRequest)
                .willReturn(userProfileRequest);
    }

    @Test
    public void shouldReturnInvalidAccessTokenResponse() throws Exception {

        //Given
        String accessToken = "ACCESS_TOKEN";
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("error", "ERROR");
        JsonValue tokenInfoResponse = new JsonValue(jsonMap);

        given(tokenInfoRequest.get()).willReturn(tokenInfoResponse);

        //When
        final AccessTokenValidationResponse validate = accessTokenValidator.validate(accessToken);

        //Then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(restResourceFactory).resource(captor.capture());
        assertTrue(captor.getValue().contains("ACCESS_TOKEN"));
        assertFalse(validate.isTokenValid());
        assertTrue(validate.getProfileInformation().isEmpty());
        assertTrue(validate.getTokenScopes().isEmpty());
    }

    @Test
    public void shouldReturnInvalidAccessTokenResponseWhenTokenIsExpired() throws Exception {

        //Given
        String accessToken = "ACCESS_TOKEN";
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("expires_in", -1);
        jsonMap.put("scope", "A B C");
        JsonValue tokenInfoResponse = new JsonValue(jsonMap);

        given(tokenInfoRequest.get()).willReturn(tokenInfoResponse);

        //When
        final AccessTokenValidationResponse validate = accessTokenValidator.validate(accessToken);

        //Then
        assertFalse(validate.isTokenValid());
        assertTrue(validate.getProfileInformation().isEmpty());
        assertEquals(validate.getTokenScopes().size(), 3);
    }

    /**
     * This test mimics a validation response that was placed in cache and re-used after some time.
     */
    @Test
    public void shouldReturnValidAccessTokenResponseWhenTokenIsNotExpired() throws Exception {

        //Given
        JsonValue config = JsonValue.json(JsonValue.object(
                JsonValue.field("token-info-endpoint", "TOKEN_INFO")
                // No user info endpoint
        ));
        accessTokenValidator = new RestOAuth2AccessTokenValidator(config, restResourceFactory);
        String accessToken = "ACCESS_TOKEN";
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("expires_in", 10);
        jsonMap.put("scope", "A B C");
        JsonValue tokenInfoResponse = new JsonValue(jsonMap);

        given(tokenInfoRequest.get()).willReturn(tokenInfoResponse);

        //When
        final AccessTokenValidationResponse validate = accessTokenValidator.validate(accessToken);

        // Mimics a delay before re-using the response
        Thread.sleep(50);

        //Then
        assertTrue(validate.isTokenValid());
        assertTrue(validate.getProfileInformation().isEmpty());
        assertEquals(validate.getTokenScopes().size(), 3);
    }

    @Test
    public void shouldReturnInvalidAccessTokenResponseWithScope() throws Exception {

        //Given
        String accessToken = "ACCESS_TOKEN";
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("expires_in", -1);
        jsonMap.put("scope", "A B C");
        JsonValue tokenInfoResponse = new JsonValue(jsonMap);

        given(tokenInfoRequest.get()).willReturn(tokenInfoResponse);

        //When
        final AccessTokenValidationResponse validate = accessTokenValidator.validate(accessToken);

        //Then
        assertFalse(validate.isTokenValid());
        assertTrue(validate.getProfileInformation().isEmpty());
        assertEquals(validate.getTokenScopes().size(), 3);
    }

    @Test
    public void shouldReturnValidAccessTokenResponseWithScopeAndUserProfile() throws Exception {

        //Given
        String accessToken = "ACCESS_TOKEN";
        Map<String, Object> tokenInfoMap = new HashMap<>();
        tokenInfoMap.put("expires_in", 1);
        tokenInfoMap.put("scope", "A B C");
        JsonValue tokenInfoResponse = new JsonValue(tokenInfoMap);

        given(tokenInfoRequest.get()).willReturn(tokenInfoResponse);

        Map<String, Object> userProfileMap = new HashMap<>();
        userProfileMap.put("KEY1", "VAL");
        userProfileMap.put("KEY2", "VAL");
        JsonValue profileResponse = new JsonValue(userProfileMap);

        given(userProfileRequest.get()).willReturn(profileResponse);

        //When
        final AccessTokenValidationResponse validate = accessTokenValidator.validate(accessToken);

        //Then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(userProfileRequest).addHeader(eq("Authorization"), captor.capture());
        assertTrue(captor.getValue().equals("Bearer ACCESS_TOKEN"));
        assertTrue(validate.isTokenValid());
        assertEquals(validate.getProfileInformation().size(), 2);
        assertEquals(validate.getTokenScopes().size(), 3);
    }

    /**
     * The userInfo endpoint configuration is optional. If it is not specified, then no attempt should be made to fetch
     * any user profile information.
     */
    @Test
    public void shouldNotFetchUserProfileIfEndPointNotConfigured() throws Exception {
        // Given
        JsonValue config = JsonValue.json(JsonValue.object(
                JsonValue.field("token-info-endpoint", "TOKEN_INFO")
                // No user info endpoint
        ));
        accessTokenValidator = new RestOAuth2AccessTokenValidator(config, restResourceFactory);
        String accessToken = "ACCESS_TOKEN";
        Map<String, Object> tokenInfoMap = new HashMap<>();
        tokenInfoMap.put("expires_in", 1);
        tokenInfoMap.put("scope", "A B C");
        JsonValue tokenInfoResponse = new JsonValue(tokenInfoMap);

        given(tokenInfoRequest.get()).willReturn(tokenInfoResponse);


        // When
        AccessTokenValidationResponse response = accessTokenValidator.validate(accessToken);

        // Then
        verifyZeroInteractions(userProfileRequest);
        assertEquals(response.getProfileInformation(), Collections.emptyMap());
    }

    @Test
    public void shouldAcceptScopeAsJsonArray() throws Exception {

        //Given
        String accessToken = "ACCESS_TOKEN";
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("expires_in", -1);
        jsonMap.put("scope", asList("A", "B", "C"));
        JsonValue tokenInfoResponse = new JsonValue(jsonMap);

        given(tokenInfoRequest.get()).willReturn(tokenInfoResponse);

        //When
        final AccessTokenValidationResponse validate = accessTokenValidator.validate(accessToken);

        //Then
        assertEquals(validate.getTokenScopes().size(), 3);
    }

}
