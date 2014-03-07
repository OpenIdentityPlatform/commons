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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class OpenIdResolverServiceConfiguratorImplTest {

    OpenIdResolverServiceConfigurator testServiceConfigurator;
    OpenIdResolverService mockService;
    OpenIdResolverServiceFactory mockFactory;

    @BeforeMethod
    public void setUp() {
        mockFactory = mock(OpenIdResolverServiceFactory.class);
        mockService = mock(OpenIdResolverService.class);
        testServiceConfigurator = new OpenIdResolverServiceConfiguratorImpl(mockFactory);
    }

    @Test
    public void setupGeneratesService() throws UnsupportedEncodingException {
        //given
        String keystoreLocation = URLDecoder.decode(ClassLoader.getSystemResource("cacert.jks").getFile(), "UTF-8");
        String keystoreType = "JKS";
        String keystorePass = "storepass";

        given(mockFactory.createOpenIdResolverService(keystoreType, keystoreLocation,
                keystorePass)).willReturn(mockService);

        //when
        OpenIdResolverService service = testServiceConfigurator.setupService(keystoreType,
                keystoreLocation, keystorePass);

        verify(mockFactory, times(1)).createOpenIdResolverService(keystoreType, keystoreLocation, keystorePass);

        //then
        assertNotNull(service);
        assertEquals(mockService, service);
    }

    @Test
    public void invalidSetupGeneratesNullService() throws UnsupportedEncodingException {
        //given
        String keystoreLocation = URLDecoder.decode(ClassLoader.getSystemResource("cacert.jks").getFile(), "UTF-8");
        String keystoreType = "JKS";
        String keystorePass = "wrongpass";

        given(mockFactory.createOpenIdResolverService(keystoreType, keystoreLocation, keystorePass)).willReturn(null);

        //when
        OpenIdResolverService service = testServiceConfigurator.setupService(keystoreType,
                keystoreLocation, keystorePass);

        verify(mockFactory, times(1)).createOpenIdResolverService(keystoreType, keystoreLocation, keystorePass);

        //then
        assertNull(service);
    }

    @Test
    public void checkConfigureServiceDoesNotAllowNeitherSecretNorKey() {
        //given
        List<Map<String, String>> resolverConfigs = new ArrayList<Map<String, String>>();

        Map<String, String> resolverConfigMap1 = new HashMap<String, String>();
        resolverConfigMap1.put(OpenIdResolver.CLIENT_ID_KEY, "clientId");
        resolverConfigMap1.put(OpenIdResolver.ISSUER_KEY, "issuer");

        resolverConfigs.add(resolverConfigMap1);

        OpenIdResolverService service = mock(OpenIdResolverService.class);

        //when
        boolean success = testServiceConfigurator.configureService(service, resolverConfigs);

        //then
        assertFalse(success);
    }

    @Test
    public void checkConfigureServiceDoesNotAllowBothSecretAndKey() {
        //given
        List<Map<String, String>> resolverConfigs = new ArrayList<Map<String, String>>();

        Map<String, String> resolverConfigMap1 = new HashMap<String, String>();
        resolverConfigMap1.put(OpenIdResolver.CLIENT_ID_KEY, "clientId");
        resolverConfigMap1.put(OpenIdResolver.ISSUER_KEY, "issuer");
        resolverConfigMap1.put(OpenIdResolver.KEY_ALIAS_KEY, "keyAlias");
        resolverConfigMap1.put(OpenIdResolver.CLIENT_SECRET_KEY, "keyAlias");

        resolverConfigs.add(resolverConfigMap1);

        OpenIdResolverService service = mock(OpenIdResolverService.class);

        //when
        boolean success = testServiceConfigurator.configureService(service, resolverConfigs);

        //then
        assertFalse(success);
    }

    @Test
    public void checkConfigureServiceCreatesAppropriateResolvers() {
        //given
        List<Map<String, String>> resolverConfigs = new ArrayList<Map<String, String>>();

        Map<String, String> resolverConfigMap1 = new HashMap<String, String>();
        resolverConfigMap1.put(OpenIdResolver.CLIENT_ID_KEY, "clientId");
        resolverConfigMap1.put(OpenIdResolver.ISSUER_KEY, "issuer");
        resolverConfigMap1.put(OpenIdResolver.KEY_ALIAS_KEY, "keyAlias");

        Map<String, String> resolverConfigMap2 = new HashMap<String, String>();
        resolverConfigMap2.put(OpenIdResolver.CLIENT_ID_KEY, "none");
        resolverConfigMap2.put(OpenIdResolver.ISSUER_KEY, "none");
        resolverConfigMap2.put(OpenIdResolver.KEY_ALIAS_KEY, "none");

        resolverConfigs.add(resolverConfigMap1);
        resolverConfigs.add(resolverConfigMap2);

        OpenIdResolverService service = mock(OpenIdResolverService.class);

        given(service.configureResolverWithKey("clientId", "issuer", "keyAlias")).willReturn(true);
        given(service.configureResolverWithKey("none", "none", "none")).willReturn(true);

        //when
        boolean success = testServiceConfigurator.configureService(service, resolverConfigs);

        //then
        verify(service, times(2)).configureResolverWithKey(anyString(), anyString(), anyString());
        verify(service, times(0)).configureResolverWithSecret(anyString(), anyString(), anyString());
        assertTrue(success);
    }

    @Test
    public void checkConfigureServiceCreatesCorrectResolverWithPartiallyInvalidConfig() {
        //given
        List<Map<String, String>> resolverConfigs = new ArrayList<Map<String, String>>();

        Map<String, String> resolverConfigMap = new HashMap<String, String>();
        resolverConfigMap.put(OpenIdResolver.CLIENT_ID_KEY, "clientId");
        resolverConfigMap.put(OpenIdResolver.ISSUER_KEY, "issuer");
        resolverConfigMap.put(OpenIdResolver.KEY_ALIAS_KEY, "keyAlias");

        Map<String, String> resolverConfigMap2 = new HashMap<String, String>();
        resolverConfigMap2.put(OpenIdResolver.CLIENT_ID_KEY, "none");
        resolverConfigMap2.put(OpenIdResolver.ISSUER_KEY, "none");
        resolverConfigMap2.put(OpenIdResolver.KEY_ALIAS_KEY, "keyAlias");
        resolverConfigMap2.put(OpenIdResolver.CLIENT_SECRET_KEY, "keySecret");

        resolverConfigs.add(resolverConfigMap);
        resolverConfigs.add(resolverConfigMap2);

        OpenIdResolverService service = mock(OpenIdResolverService.class);

        given(service.configureResolverWithKey("clientId", "issuer", "keyAlias")).willReturn(true);

        //when
        boolean success = testServiceConfigurator.configureService(service, resolverConfigs);

        //then
        verify(service, times(1)).configureResolverWithKey(anyString(), anyString(), anyString());
        assertTrue(success);
    }

}
