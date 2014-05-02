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

package org.forgerock.jaspi.modules.session.openam;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.json.JSONObject;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.engine.header.Header;
import org.restlet.resource.ClientResource;
import org.restlet.util.Series;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class RestletRestClientTest {

    private RestletRestClient restClient;

    private ClientResource resource;
    private Series<Header> requestHeaders;

    @BeforeMethod
    public void setUp() {

        resource = mock(ClientResource.class);

        restClient = new RestletRestClient() {
            @Override
            ClientResource createResource(final String uri) {
                return resource;
            }
        };

        Request request = mock(Request.class);
        ConcurrentHashMap<String, Object> requestAttributes = new ConcurrentHashMap<String, Object>();
        requestHeaders = mock(Series.class);
        given(resource.getRequest()).willReturn(request);
        given(request.getAttributes()).willReturn(requestAttributes);
        given(resource.getRequestAttributes()).willReturn(requestAttributes);
        requestAttributes.put("org.restlet.http.headers", requestHeaders);
    }

    @Test
    public void shouldSetSSLConfiguration() {

        //Given
        final SslConfiguration sslConfiguration = new SslConfiguration();

        sslConfiguration.setKeyManagerAlgorithm("KEY_MANAGER_ALGORITHM");
        sslConfiguration.setKeyStorePath("KEY_STORE_PATH");
        sslConfiguration.setKeyStoreType("KEY_STORE_TYPE");
        sslConfiguration.setKeyStorePassword("KEY_STORE_PASSWORD".toCharArray());
        sslConfiguration.setKeyStoreKeyPassword("KEY_STORE_KEY_PASSWORD".toCharArray());
        sslConfiguration.setTrustManagerAlgorithm("TRUST_MANAGER_ALGORITHM");
        sslConfiguration.setTrustStorePath("TRUST_STORE_PATH");
        sslConfiguration.setTrustStoreType("TRUST_STORE_TYPE");
        sslConfiguration.setTrustStorePassword("TRUST_STORE_PASSWORD".toCharArray());

        //When
        restClient.setSslConfiguration(sslConfiguration);

        //Then
    }

    private void setSslConfiguration() {

        final SslConfiguration sslConfiguration = new SslConfiguration();

        sslConfiguration.setKeyManagerAlgorithm("KEY_MANAGER_ALGORITHM");
        sslConfiguration.setKeyStorePath("KEY_STORE_PATH");
        sslConfiguration.setKeyStoreType("KEY_STORE_TYPE");
        sslConfiguration.setKeyStorePassword("KEY_STORE_PASSWORD".toCharArray());
        sslConfiguration.setKeyStoreKeyPassword("KEY_STORE_KEY_PASSWORD".toCharArray());
        sslConfiguration.setTrustManagerAlgorithm("TRUST_MANAGER_ALGORITHM");
        sslConfiguration.setTrustStorePath("TRUST_STORE_PATH");
        sslConfiguration.setTrustStoreType("TRUST_STORE_TYPE");
        sslConfiguration.setTrustStorePassword("TRUST_STORE_PASSWORD".toCharArray());

        restClient.setSslConfiguration(sslConfiguration);
    }

    @Test
    public void shouldPostWithEmptyQueryParameters() throws ResourceException {

        //Given
        final String uri = "URI";
        final Map<String, String> queryParameters = new HashMap<String, String>();
        final Map<String, String> headers = new HashMap<String, String>();
        final JSONObject restResponse = mock(JSONObject.class);

        given(resource.post(anyObject(), eq(JSONObject.class))).willReturn(restResponse);
        given(restResponse.toString()).willReturn("{}");

        //When
        final JsonValue response = restClient.post(uri, queryParameters, headers);

        //Then
        verify(resource, never()).addQueryParameter(anyString(), anyString());
        verify(requestHeaders, never()).set(anyString(), anyString());
        verify(resource, never()).getContext();
        assertEquals(response.size(), 0);
    }

    @Test
    public void shouldPostWithQueryParameters() throws ResourceException {

        //Given
        final String uri = "URI";
        final Map<String, String> queryParameters = new HashMap<String, String>();
        final Map<String, String> headers = new HashMap<String, String>();
        final JSONObject restResponse = mock(JSONObject.class);

        queryParameters.put("PARAM1", "VALUE1");
        queryParameters.put("PARAM2", "VALUE2");
        given(resource.post(anyObject(), eq(JSONObject.class))).willReturn(restResponse);
        given(restResponse.toString()).willReturn("{}");

        //When
        final JsonValue response = restClient.post(uri, queryParameters, headers);

        //Then
        verify(resource, times(2)).addQueryParameter(anyString(), anyString());
        verify(requestHeaders, never()).set(anyString(), anyString());
        verify(resource, never()).getContext();
        assertEquals(response.size(), 0);
    }

    @Test
    public void shouldPostWithSSL() throws ResourceException {

        //Given
        setSslConfiguration();
        final String uri = "URI";
        final Map<String, String> queryParameters = new HashMap<String, String>();
        final Map<String, String> headers = new HashMap<String, String>();
        final Context context = mock(Context.class);
        final ConcurrentHashMap<String, Object> requestAttributes = new ConcurrentHashMap<String, Object>();
        final JSONObject restResponse = mock(JSONObject.class);

        given(resource.getContext()).willReturn(context);
        given(context.getAttributes()).willReturn(requestAttributes);
        given(resource.post(anyObject(), eq(JSONObject.class))).willReturn(restResponse);
        given(restResponse.toString()).willReturn("{}");

        //When
        final JsonValue response = restClient.post(uri, queryParameters, headers);

        //Then
        verify(resource, never()).addQueryParameter(anyString(), anyString());
        verify(requestHeaders, never()).set(anyString(), anyString());
        verify(resource).getContext();
        assertTrue(requestAttributes.containsKey("sslContextFactory"));
        assertEquals(response.size(), 0);
    }

    @Test
    public void postShouldFailWhenResourceExceptionThrown() throws ResourceException {

        //Given
        final String uri = "URI";
        final Map<String, String> queryParameters = new HashMap<String, String>();
        final Map<String, String> headers = new HashMap<String, String>();
        final org.restlet.resource.ResourceException exception =
                new org.restlet.resource.ResourceException(500, "EXCEPTION_MESSAGE", "DESCRIPTION", "URI");

        doThrow(exception).when(resource).post(anyObject(), eq(JSONObject.class));

        //When
        try {
            restClient.post(uri, queryParameters, headers);
        } catch (ResourceException e) {
            //Then
            assertEquals(e.getCode(), 500);
            assertEquals(e.getMessage(), "EXCEPTION_MESSAGE");
            verify(resource, never()).addQueryParameter(anyString(), anyString());
            verify(requestHeaders, never()).set(anyString(), anyString());
            verify(resource, never()).getContext();
        }
    }

    @Test
    public void shouldGetWithEmptyQueryParameters() throws ResourceException {

        //Given
        final String uri = "URI";
        final Map<String, String> queryParameters = new HashMap<String, String>();
        final Map<String, String> headers = new HashMap<String, String>();
        final JSONObject restResponse = mock(JSONObject.class);

        given(resource.get(JSONObject.class)).willReturn(restResponse);
        given(restResponse.toString()).willReturn("{}");

        //When
        final JsonValue response = restClient.get(uri, queryParameters, headers);

        //Then
        verify(resource, never()).addQueryParameter(anyString(), anyString());
        verify(requestHeaders, never()).set(anyString(), anyString());
        verify(resource, never()).getContext();
        assertEquals(response.size(), 0);
    }

    @Test
    public void shouldGetWithQueryParameters() throws ResourceException {

        //Given
        final String uri = "URI";
        final Map<String, String> queryParameters = new HashMap<String, String>();
        final Map<String, String> headers = new HashMap<String, String>();
        final JSONObject restResponse = mock(JSONObject.class);

        queryParameters.put("PARAM1", "VALUE1");
        queryParameters.put("PARAM2", "VALUE2");
        given(resource.get(JSONObject.class)).willReturn(restResponse);
        given(restResponse.toString()).willReturn("{}");

        //When
        final JsonValue response = restClient.get(uri, queryParameters, headers);

        //Then
        verify(resource, times(2)).addQueryParameter(anyString(), anyString());
        verify(requestHeaders, never()).set(anyString(), anyString());
        verify(resource, never()).getContext();
        assertEquals(response.size(), 0);
    }

    @Test
    public void shouldGetWithSSL() throws ResourceException {

        //Given
        setSslConfiguration();
        final String uri = "URI";
        final Map<String, String> queryParameters = new HashMap<String, String>();
        final Map<String, String> headers = new HashMap<String, String>();
        final Context context = mock(Context.class);
        final ConcurrentHashMap<String, Object> requestAttributes = new ConcurrentHashMap<String, Object>();
        final JSONObject restResponse = mock(JSONObject.class);

        given(resource.getContext()).willReturn(context);
        given(context.getAttributes()).willReturn(requestAttributes);
        given(resource.get(JSONObject.class)).willReturn(restResponse);
        given(restResponse.toString()).willReturn("{}");

        //When
        final JsonValue response = restClient.get(uri, queryParameters, headers);

        //Then
        verify(resource, never()).addQueryParameter(anyString(), anyString());
        verify(requestHeaders, never()).set(anyString(), anyString());
        verify(resource).getContext();
        assertTrue(requestAttributes.containsKey("sslContextFactory"));
        assertEquals(response.size(), 0);
    }

    @Test
    public void getShouldFailWhenResourceExceptionThrown() throws ResourceException {

        //Given
        final String uri = "URI";
        final Map<String, String> queryParameters = new HashMap<String, String>();
        final Map<String, String> headers = new HashMap<String, String>();
        final org.restlet.resource.ResourceException exception =
                new org.restlet.resource.ResourceException(500, "EXCEPTION_MESSAGE", "DESCRIPTION", "URI");

        doThrow(exception).when(resource).get(JSONObject.class);

        //When
        try {
            restClient.get(uri, queryParameters, headers);
        } catch (ResourceException e) {
            //Then
            assertEquals(e.getCode(), 500);
            assertEquals(e.getMessage(), "EXCEPTION_MESSAGE");
            verify(resource, never()).addQueryParameter(anyString(), anyString());
            verify(requestHeaders, never()).set(anyString(), anyString());
            verify(resource, never()).getContext();
        }
    }
}
