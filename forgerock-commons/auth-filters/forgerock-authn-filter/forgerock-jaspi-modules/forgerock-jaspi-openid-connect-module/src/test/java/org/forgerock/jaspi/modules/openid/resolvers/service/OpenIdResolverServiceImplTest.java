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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import org.forgerock.jaspi.modules.openid.exceptions.FailedToLoadJWKException;
import org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver;
import org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolverFactory;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNull;
import org.testng.annotations.Test;

public class OpenIdResolverServiceImplTest {


    @Test
    public void checkJWKConfigurationOfValidResolver() throws FailedToLoadJWKException, MalformedURLException {
        //given
        OpenIdResolverFactory mockFactory = mock(OpenIdResolverFactory.class);
        OpenIdResolverServiceImpl testResolverService = new OpenIdResolverServiceImpl(mockFactory, 0, 0);
        OpenIdResolver mockResolver = mock(OpenIdResolver.class);
        URL testURL = new URL("http://www.google.com");

        given(mockFactory.createJWKResolver("issuer", testURL, 0, 0)).willReturn(mockResolver);

        //when
        boolean success = testResolverService.configureResolverWithJWK("issuer", testURL);
        OpenIdResolver resolver = testResolverService.getResolverForIssuer("issuer");

        //then
        assertTrue(success);
        assertNotNull(resolver);
    }

    @Test
    public void checkJWKConfigurationOfInvalidResolver() throws FailedToLoadJWKException, MalformedURLException {
        //given
        OpenIdResolverFactory mockFactory = mock(OpenIdResolverFactory.class);
        OpenIdResolverServiceImpl testResolverService = new OpenIdResolverServiceImpl(mockFactory, 0, 0);
        URL testURL = new URL("http://www.google.com");

        given(mockFactory.createJWKResolver("issuer", testURL, 0, 0))
                .willThrow(FailedToLoadJWKException.class);

        //when
        boolean success = testResolverService.configureResolverWithJWK("issuer", testURL);
        OpenIdResolver resolver = testResolverService.getResolverForIssuer("issuer");

        //then
        assertFalse(success);
        assertNull(resolver);
    }

    @Test
    public void checkWellKnownConfigurationConfigurationOfValidResolver()
            throws FailedToLoadJWKException, MalformedURLException {
        //given
        OpenIdResolverFactory mockFactory = mock(OpenIdResolverFactory.class);
        OpenIdResolverServiceImpl testResolverService = new OpenIdResolverServiceImpl(mockFactory, 0, 0);
        OpenIdResolver mockResolver = mock(OpenIdResolver.class);
        URL testURL = new URL("http://www.google.com");

        given(mockFactory.createFromOpenIDConfigUrl(testURL)).willReturn(mockResolver);
        given(mockResolver.getIssuer()).willReturn("issuer");

        //when
        boolean success = testResolverService.configureResolverWithWellKnownOpenIdConfiguration(testURL);
        OpenIdResolver resolver = testResolverService.getResolverForIssuer("issuer");

        //then
        assertTrue(success);
        assertNotNull(resolver);
        assertEquals("issuer", resolver.getIssuer());
    }

    @Test
    public void checkWellKnownConfigurationConfigurationOfInvalidResolver()
            throws FailedToLoadJWKException, MalformedURLException {
        //given
        OpenIdResolverFactory mockFactory = mock(OpenIdResolverFactory.class);
        OpenIdResolverServiceImpl testResolverService = new OpenIdResolverServiceImpl(mockFactory, 0, 0);
        URL testURL = new URL("http://www.google.com");

        given(mockFactory.createFromOpenIDConfigUrl(testURL))
                .willThrow(FailedToLoadJWKException.class);

        //when
        boolean success = testResolverService.configureResolverWithWellKnownOpenIdConfiguration(testURL);
        OpenIdResolver resolver = testResolverService.getResolverForIssuer("issuer");

        //then
        assertFalse(success);
        assertNull(resolver);
    }

    @Test
    public void checkSharedSecretConfigurationOfValidResolver() {
        //given
        OpenIdResolverServiceImpl testResolverService = new OpenIdResolverServiceImpl(0, 0);
        boolean success = testResolverService.configureResolverWithSecret("issuer", "string");

        //when
        OpenIdResolver resolver = testResolverService.getResolverForIssuer("issuer");

        //then
        assertTrue(success);
        assertNotNull(resolver);
        assertEquals("issuer", resolver.getIssuer());
    }

    @Test
    public void checkSharedSecretConfigurationOfInvalidResolver() {
        //given
        OpenIdResolverServiceImpl testResolverService = new OpenIdResolverServiceImpl(0, 0);
        boolean success = testResolverService.configureResolverWithSecret("dancer", null);

        //when
        OpenIdResolver resolver = testResolverService.getResolverForIssuer("dancer");

        //then
        assertFalse(success);
        assertNull(resolver);
    }

    @Test
    public void checkPublicKeyConfigurationOfValidResolver() throws UnsupportedEncodingException {
        //given
        OpenIdResolverServiceImpl testResolverService = new OpenIdResolverServiceImpl(0, 0);
        String keystoreLocation = URLDecoder.decode(ClassLoader.getSystemResource("cacert.jks").getFile(), "UTF-8");
        String keystoreType = "JKS";
        String keystorePass = "storepass";

        boolean success = testResolverService.configureResolverWithKey("issuer", "google",
                keystoreLocation, keystoreType, keystorePass);

        //when
        OpenIdResolver resolver = testResolverService.getResolverForIssuer("issuer");

        //then
        assertTrue(success);
        assertNotNull(resolver);
        assertEquals("issuer", resolver.getIssuer());
    }

    @Test
    public void checkPublicKeyConfigurationOfInvalidResolver() throws UnsupportedEncodingException {
        //given
        OpenIdResolverServiceImpl testResolverService = new OpenIdResolverServiceImpl(0, 0);
        String keystoreLocation = URLDecoder.decode(ClassLoader.getSystemResource("cacert.jks").getFile(), "UTF-8");
        String keystoreType = "JKS";
        String keystorePass = "storepass";

        //when
        boolean success = testResolverService.configureResolverWithKey("issuer", "invalid_key",
                keystoreLocation, keystoreType, keystorePass);

        //then
        assertFalse(success);
    }

    @Test
    public void checkPublicKeyConfigurationOfInvalidLookup() throws UnsupportedEncodingException {
        //given
        OpenIdResolverServiceImpl testResolverService = new OpenIdResolverServiceImpl(0, 0);
        String keystoreLocation = URLDecoder.decode(ClassLoader.getSystemResource("cacert.jks").getFile(), "UTF-8");
        String keystoreType = "JKS";
        String keystorePass = "storepass";

        //when
        boolean success = testResolverService.configureResolverWithKey("an_issuer", "google",
                keystoreLocation, keystoreType, keystorePass);
        //when
        OpenIdResolver resolver = testResolverService.getResolverForIssuer("different_issuer");

        //then
        assertNull(resolver);
        assertTrue(success);
    }

    @Test
    public void checkCreatingMultipleResolversForTheSameIssuerOnlySavesTheLatest() {
        //given
        OpenIdResolverServiceImpl testResolverService = new OpenIdResolverServiceImpl(0, 0);
        boolean success1 = testResolverService.configureResolverWithSecret("issuer", "string");
        boolean success2 = testResolverService.configureResolverWithSecret("issuer", "another string");

        //when
        OpenIdResolver resolver = testResolverService.getResolverForIssuer("issuer");

        //then
        assertTrue(success1);
        assertTrue(success2);
        assertNotNull(resolver);
        assertEquals("issuer", resolver.getIssuer());
    }

}
