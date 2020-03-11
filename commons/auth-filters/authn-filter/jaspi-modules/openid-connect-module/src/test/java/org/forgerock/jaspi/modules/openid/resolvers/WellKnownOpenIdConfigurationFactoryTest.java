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
package org.forgerock.jaspi.modules.openid.resolvers;

import java.io.IOException;
import java.net.URL;
import org.forgerock.jaspi.modules.openid.exceptions.FailedToLoadJWKException;
import org.forgerock.jaspi.modules.openid.helpers.SimpleHTTPClient;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class WellKnownOpenIdConfigurationFactoryTest {

    SimpleHTTPClient mockClient;
    WellKnownOpenIdConfigurationFactory configFactoryTest;

    @BeforeMethod
    public void setUp() {
        mockClient = mock(SimpleHTTPClient.class);
        configFactoryTest = new WellKnownOpenIdConfigurationFactory(mockClient);
    }

    @Test(expectedExceptions = FailedToLoadJWKException.class)
    public void shouldErrorWhenURlIsInvalid() throws IOException, FailedToLoadJWKException {
        //given
        URL testURL = new URL("http://www.google.com");

        given(mockClient.get(any(URL.class))).willThrow(IOException.class);

        //when
        configFactoryTest.build(testURL);

        //then - caught by exception
    }

    @Test(expectedExceptions = FailedToLoadJWKException.class)
    public void shouldErrorWhenNoIssuerInConfig() throws IOException, FailedToLoadJWKException {
        //given
        String config = "{}";
        URL testURL = new URL("http://www.google.com");
        given(mockClient.get(any(URL.class))).willReturn(config);

        //when
        configFactoryTest.build(testURL);

        //then - caught by exception
    }

    @Test(expectedExceptions = FailedToLoadJWKException.class)
    public void shouldErrorWhenNoJWKURIInConfig() throws IOException, FailedToLoadJWKException {
        //given
        String config = "{\"issuer\":\"test\"}";
        URL testURL = new URL("http://www.google.com");

        given(mockClient.get(any(URL.class))).willReturn(config);

        //when
        configFactoryTest.build(testURL);

        //then - caubght by exception
    }

    @Test
    public void shouldBuildAResolver() throws IOException, FailedToLoadJWKException {
        //given
        String issuer = "google";
        String config = "{\"issuer\":\"" + issuer + "\", \"jwks_uri\":\"http://www.google.com\"}";
        URL testURL = new URL("http://www.google.com");

        given(mockClient.get(any(URL.class))).willReturn(config);

        //when
        JWKOpenIdResolverImpl resolver = configFactoryTest.build(testURL);

        //then
        assertNotNull(resolver);
        assertEquals(resolver.getIssuer(), issuer);
    }

}
