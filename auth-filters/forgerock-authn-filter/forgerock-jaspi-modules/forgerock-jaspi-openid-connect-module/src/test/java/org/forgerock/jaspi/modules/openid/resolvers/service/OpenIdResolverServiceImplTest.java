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
import org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNull;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class OpenIdResolverServiceImplTest {

    OpenIdResolverServiceImpl testResolverService;

    @BeforeTest
    public void setUp() throws UnsupportedEncodingException {
        String keystoreLocation = URLDecoder.decode(ClassLoader.getSystemResource("cacert.jks").getFile(), "UTF-8");
        String keystoreType = "JKS";
        String keystorePass = "storepass";

        testResolverService = new OpenIdResolverServiceImpl(keystoreType, keystoreLocation, keystorePass);

    }

    @Test
    public void checkSharedSecretConfigurationOfValidResolver() {
        //given
        boolean success = testResolverService.configureResolverWithSecret("clientId", "issuer", "string");

        //when
        OpenIdResolver resolver = testResolverService.getResolverForIssuer("issuer");

        //then
        assertTrue(success);
        assertNotNull(resolver);
    }

    @Test
    public void checkSharedSecretConfigurationOfInvalidResolver() {
        //given
        boolean success = testResolverService.configureResolverWithSecret("clientId", "dancer", null);

        //when
        OpenIdResolver resolver = testResolverService.getResolverForIssuer("dancer");

        //then
        assertFalse(success);
        assertNull(resolver);
    }

    @Test
    public void checkPublicKeyConfigurationOfValidResolver() {
        //given
        boolean success = testResolverService.configureResolverWithKey("clientId", "tiny", "google");

        //when
        OpenIdResolver resolver = testResolverService.getResolverForIssuer("tiny");

        //then
        assertTrue(success);
        assertNotNull(resolver);
    }

    @Test
    public void checkPublicKeyConfigurationOfInvalidResolver() {
        //given - no further setup required

        //when
        boolean success = testResolverService.configureResolverWithKey("clientId", "issuer", "invalid_key");

        //then
        assertFalse(success);
    }

    @Test
    public void checkPublicKeyConfigurationOfInvalidLookup() {
        //given
        testResolverService.configureResolverWithKey("clientId", "an_issuer", "google");

        //when
        OpenIdResolver resolver = testResolverService.getResolverForIssuer("different_issuer");

        //then
        assertNull(resolver);
    }

}
