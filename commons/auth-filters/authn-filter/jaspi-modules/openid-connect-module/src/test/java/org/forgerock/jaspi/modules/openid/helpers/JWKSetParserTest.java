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
package org.forgerock.jaspi.modules.openid.helpers;

import java.io.IOException;
import java.net.URL;
import java.security.Key;
import java.util.Map;
import org.forgerock.jaspi.modules.openid.exceptions.FailedToLoadJWKException;
import org.forgerock.json.jose.jwk.KeyType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class JWKSetParserTest {

    private final String testJWKSet = "{\n"
            + " \"keys\": [\n"
            + "  {\n"
            + "   \"kty\": \"RSA\",\n"
            + "   \"alg\": \"RS256\",\n"
            + "   \"use\": \"sig\",\n"
            + "   \"kid\": \"4846958145422cb96b4f348e8facd8c0900950ba\",\n"
            + "   \"n\": \"APVklSShb4aHWCh8Gk04ZUN3LpsJnQIFqzolXkSNS0g5BFvOjwzbJFhHGab8dVK+sej3DTHl8fXf/Hlz0"
            + "LUdyl660jqYYeT/dNc15NL/0tuxEIzGizCWmvVR16HeDCMPlb3JnTwo3qzhN4NICBxnwtgoEQobg/5estZtPHaQ0LXz\",\n"
            + "   \"e\": \"AQAB\"\n"
            + "  },\n"
            + "  {\n"
            + "   \"kty\": \"RSA\",\n"
            + "   \"alg\": \"RS256\",\n"
            + "   \"use\": \"sig\",\n"
            + "   \"kid\": \"622185931dc9e3a7bb14d946e1451c4d626d3cd4\",\n"
            + "   \"n\": \"ANmSBPaAqdsnf8SRZTvwYSTD3f7B+Z+VS9+8pUgTOaKV2DS2ousDhEegdYZ6qGYMezxPwskwNOtv4oopJa"
            + "aT4Xc8a+cY5Jj3AHefQme4gFQPhtDE3hO/vtkgwpxj1QhZsjiA7kNWR97ofCto3fnCClxM7KRO7VYyDI6M6073RY7Z\",\n"
            + "   \"e\": \"AQAB\"\n"
            + "  }\n"
            + " ]\n"
            + "}\n";

    private SimpleHTTPClient mockClient;
    private JWKLookup mockLookup;
    private JWKSetParser jwkSetParserTest;

    @BeforeMethod
    public void setUp() {
        mockClient = mock(SimpleHTTPClient.class);
        mockLookup = mock(JWKLookup.class);
        jwkSetParserTest = new JWKSetParser(mockClient, mockLookup);
    }

    @Test
    public void shouldGenerateMapFromProvidedJWK() throws FailedToLoadJWKException, IOException {

        //given
        URL mockURL = new URL("http://www.google.com");
        Key mockKey = mock(Key.class);

        given(mockClient.get(mockURL)).willReturn(testJWKSet);
        given(mockLookup.lookup(anyString(), any(KeyType.class))).willReturn(mockKey);

        //when
        Map<String, Key> validMap = jwkSetParserTest.generateMapFromJWK(mockURL);

        //then
        verify(mockLookup, times(2)).lookup(anyString(), any(KeyType.class)); //2 keys in the JSON above
        assertEquals(2, validMap.size());
    }

    @Test(expectedExceptions = FailedToLoadJWKException.class)
    public void shouldFailWhenURLInvalid() throws FailedToLoadJWKException, IOException {
        //given
        URL mockURL = new URL("http://www.google.com");

        given(mockClient.get(mockURL)).willThrow(IOException.class);

        //when
        jwkSetParserTest.generateMapFromJWK(mockURL);

        //then - checked by exception
    }



}
