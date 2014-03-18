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
package org.forgerock.jaspi.modules.openid.resolvers.service;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class OpenIdResolverServiceConfiguratorImplTest {

    OpenIdResolverServiceConfigurator testServiceConfigurator;
    OpenIdResolverService mockService;

    @BeforeMethod
    public void setUp() {
        mockService = mock(OpenIdResolverService.class);
        testServiceConfigurator = new OpenIdResolverServiceConfiguratorImpl();
    }

    @Test
    public void checkConfigureServiceFailsOnEmptyResolverConfig() {
        //given
        List<Map<String, String>> resolverConfigs = new ArrayList<Map<String, String>>();

        OpenIdResolverService service = mock(OpenIdResolverService.class);

        //when
        boolean success = testServiceConfigurator.configureService(service, resolverConfigs);

        //then
        assertFalse(success);
    }

    @Test
    public void checkConfigureServiceFailsOnNullResolverConfig() {
        //given
        List<Map<String, String>> resolverConfigs = null;

        OpenIdResolverService service = mock(OpenIdResolverService.class);

        //when
        boolean success = testServiceConfigurator.configureService(service, resolverConfigs);

        //then
        assertFalse(success);
    }

    @Test //well-known takes priority over JWK
    public void checkConfigureServiceCreatesCorrectOpenIdConfigurationResolver() {
        //given
        List<Map<String, String>> resolverConfigs = new ArrayList<Map<String, String>>();

        Map<String, String> resolverConfigMap1 = new HashMap<String, String>();
        resolverConfigMap1.put(OpenIdResolver.ISSUER_KEY, "issuer");
        resolverConfigMap1.put(OpenIdResolver.WELL_KNOWN_CONFIGURATION, "http://www.google.com");
        resolverConfigMap1.put(OpenIdResolver.JWK, "http://www.google.com");

        resolverConfigs.add(resolverConfigMap1);
        given(mockService.configureResolverWithWellKnownOpenIdConfiguration(any(URL.class))).willReturn(true);

        //when
        boolean success = testServiceConfigurator.configureService(mockService, resolverConfigs);

        //then
        assertTrue(success);
        verify(mockService, times(1)).configureResolverWithWellKnownOpenIdConfiguration(any(URL.class));
    }

    @Test //JWK takes priority over keystore
    public void checkConfigureServiceCreatesCorrectJWKResolver() {
        //given
        List<Map<String, String>> resolverConfigs = new ArrayList<Map<String, String>>();

        Map<String, String> resolverConfigMap1 = new HashMap<String, String>();
        resolverConfigMap1.put(OpenIdResolver.ISSUER_KEY, "issuer");
        resolverConfigMap1.put(OpenIdResolver.JWK, "http://www.google.com");
        resolverConfigMap1.put(OpenIdResolver.CLIENT_SECRET_KEY, "clientSecret");

        resolverConfigs.add(resolverConfigMap1);
        given(mockService.configureResolverWithJWK(anyString(), any(URL.class))).willReturn(true);

        //when
        boolean success = testServiceConfigurator.configureService(mockService, resolverConfigs);

        //then
        assertTrue(success);
        verify(mockService, times(1)).configureResolverWithJWK(anyString(), any(URL.class));
    }

    @Test //keystore takes priority over shared secret
    public void checkConfigureServiceCreatesCorrectKeystoreResolver() {
        //given
        List<Map<String, String>> resolverConfigs = new ArrayList<Map<String, String>>();

        Map<String, String> resolverConfigMap1 = new HashMap<String, String>();
        resolverConfigMap1.put(OpenIdResolver.ISSUER_KEY, "issuer");
        resolverConfigMap1.put(OpenIdResolver.KEY_ALIAS_KEY, "keyAlias");
        resolverConfigMap1.put(OpenIdResolver.KEYSTORE_LOCATION_KEY, "cacerts.jks");
        resolverConfigMap1.put(OpenIdResolver.KEYSTORE_TYPE_KEY, "JKS");
        resolverConfigMap1.put(OpenIdResolver.KEYSTORE_PASS_KEY, "storepass");
        resolverConfigMap1.put(OpenIdResolver.CLIENT_SECRET_KEY, "clientSecret");

        resolverConfigs.add(resolverConfigMap1);
        given(mockService.configureResolverWithKey(anyString(), anyString(), anyString(),
                anyString(), anyString())).willReturn(true);

        //when
        boolean success = testServiceConfigurator.configureService(mockService, resolverConfigs);

        //then
        assertTrue(success);
        verify(mockService, times(1)).configureResolverWithKey(anyString(), anyString(), anyString(),
                anyString(), anyString());
    }

    @Test
    public void checkKeystoreConfiguredResolverFailsWhenMissingKeystoreDetailsType() {
        //given
        List<Map<String, String>> resolverConfigs = new ArrayList<Map<String, String>>();

        Map<String, String> resolverConfigMap1 = new HashMap<String, String>();
        resolverConfigMap1.put(OpenIdResolver.ISSUER_KEY, "issuer");
        resolverConfigMap1.put(OpenIdResolver.KEYSTORE_LOCATION_KEY, "cacerts.jks");
        resolverConfigMap1.put(OpenIdResolver.KEYSTORE_PASS_KEY, "storepass");
        resolverConfigMap1.put(OpenIdResolver.KEY_ALIAS_KEY, "keyAlias");
        resolverConfigMap1.put(OpenIdResolver.CLIENT_SECRET_KEY, "clientSecret");

        resolverConfigs.add(resolverConfigMap1);

        //when
        boolean success = testServiceConfigurator.configureService(mockService, resolverConfigs);

        //then
        assertFalse(success);
        verify(mockService, times(0)).configureResolverWithKey(anyString(), anyString(), anyString(),
                anyString(), anyString());
    }

    @Test
    public void checkKeystoreConfiguredResolverFailsWhenMissingKeystoreDetailsPassword() {
        //given
        List<Map<String, String>> resolverConfigs = new ArrayList<Map<String, String>>();

        Map<String, String> resolverConfigMap1 = new HashMap<String, String>();
        resolverConfigMap1.put(OpenIdResolver.ISSUER_KEY, "issuer");
        resolverConfigMap1.put(OpenIdResolver.KEYSTORE_LOCATION_KEY, "cacerts.jks");
        resolverConfigMap1.put(OpenIdResolver.KEYSTORE_TYPE_KEY, "JKS");
        resolverConfigMap1.put(OpenIdResolver.KEY_ALIAS_KEY, "keyAlias");
        resolverConfigMap1.put(OpenIdResolver.CLIENT_SECRET_KEY, "clientSecret");

        resolverConfigs.add(resolverConfigMap1);

        //when
        boolean success = testServiceConfigurator.configureService(mockService, resolverConfigs);

        //then
        assertFalse(success);
        verify(mockService, times(0)).configureResolverWithKey(anyString(), anyString(), anyString(),
                anyString(), anyString());
    }

    @Test
    public void checkKeystoreConfiguredResolverFailsWhenMissingKeystoreDetailsAlias() {
        //given
        List<Map<String, String>> resolverConfigs = new ArrayList<Map<String, String>>();

        Map<String, String> resolverConfigMap1 = new HashMap<String, String>();
        resolverConfigMap1.put(OpenIdResolver.ISSUER_KEY, "issuer");
        resolverConfigMap1.put(OpenIdResolver.KEYSTORE_LOCATION_KEY, "cacerts.jks");
        resolverConfigMap1.put(OpenIdResolver.KEYSTORE_TYPE_KEY, "JKS");
        resolverConfigMap1.put(OpenIdResolver.KEYSTORE_PASS_KEY, "storepass");
        resolverConfigMap1.put(OpenIdResolver.CLIENT_SECRET_KEY, "clientSecret");

        resolverConfigs.add(resolverConfigMap1);

        //when
        boolean success = testServiceConfigurator.configureService(mockService, resolverConfigs);

        //then
        assertFalse(success);
        verify(mockService, times(0)).configureResolverWithKey(anyString(), anyString(), anyString(),
                anyString(), anyString());
    }

    @Test //keystore takes priority over shared secret
    public void checkKeystoreConfiguredResolverFailsWhenMissingKeystoreDetailsLocation() {
        //given
        List<Map<String, String>> resolverConfigs = new ArrayList<Map<String, String>>();

        Map<String, String> resolverConfigMap1 = new HashMap<String, String>();
        resolverConfigMap1.put(OpenIdResolver.ISSUER_KEY, "issuer");
        resolverConfigMap1.put(OpenIdResolver.JWK, "http://www.google.com");
        resolverConfigMap1.put(OpenIdResolver.KEY_ALIAS_KEY, "keyAlias");
        resolverConfigMap1.put(OpenIdResolver.KEYSTORE_TYPE_KEY, "JKS");
        resolverConfigMap1.put(OpenIdResolver.KEYSTORE_PASS_KEY, "storepass");
        resolverConfigMap1.put(OpenIdResolver.CLIENT_SECRET_KEY, "clientSecret");

        resolverConfigs.add(resolverConfigMap1);

        //when
        boolean success = testServiceConfigurator.configureService(mockService, resolverConfigs);

        //then
        assertFalse(success);
        verify(mockService, times(0)).configureResolverWithKey(anyString(), anyString(), anyString(),
                anyString(), anyString());
    }

    @Test //keystore takes priority over shared secret
    public void checkConfigureServiceCreatesCorrectSharedKeyResolver() {
        //given
        List<Map<String, String>> resolverConfigs = new ArrayList<Map<String, String>>();

        Map<String, String> resolverConfigMap1 = new HashMap<String, String>();
        resolverConfigMap1.put(OpenIdResolver.ISSUER_KEY, "issuer");
        resolverConfigMap1.put(OpenIdResolver.CLIENT_SECRET_KEY, "clientSecret");

        resolverConfigs.add(resolverConfigMap1);
        given(mockService.configureResolverWithSecret(anyString(), anyString())).willReturn(false);

        //when
        boolean success = testServiceConfigurator.configureService(mockService, resolverConfigs);

        //then
        assertFalse(success);
        verify(mockService, times(1)).configureResolverWithSecret(anyString(), anyString());
    }

    @Test
    public void checkConfigurationWithoutIssuerWillNotProceedIfNotOpenIdConnectWellKnown() {
        //given
        List<Map<String, String>> resolverConfigs = new ArrayList<Map<String, String>>();

        Map<String, String> resolverConfigMap1 = new HashMap<String, String>();
        resolverConfigMap1.put(OpenIdResolver.JWK, "http://www.google.com");

        resolverConfigs.add(resolverConfigMap1);

        //when
        boolean success = testServiceConfigurator.configureService(mockService, resolverConfigs);

        //then
        assertFalse(success);
        verify(mockService, times(0)).configureResolverWithJWK(anyString(), any(URL.class));
    }

}
