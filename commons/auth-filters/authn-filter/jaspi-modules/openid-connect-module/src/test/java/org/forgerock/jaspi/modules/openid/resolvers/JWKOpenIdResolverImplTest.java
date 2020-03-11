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

import org.forgerock.jaspi.modules.openid.exceptions.FailedToLoadJWKException;
import org.forgerock.jaspi.modules.openid.exceptions.InvalidIssException;
import org.forgerock.jaspi.modules.openid.exceptions.InvalidSignatureException;
import org.forgerock.jaspi.modules.openid.exceptions.JwtExpiredException;
import org.forgerock.jaspi.modules.openid.helpers.JWKSetParser;
import org.forgerock.json.jose.jws.JwsHeader;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertTrue;

public class JWKOpenIdResolverImplTest {

    JWKOpenIdResolverImpl testResolver;
    JWKSetParser mockParser;
    SigningHandler signingHandler;
    URL mockURL;

    @BeforeMethod
    public void setUp() throws FailedToLoadJWKException, MalformedURLException {
        signingHandler = mock(SigningHandler.class);
        mockParser = mock(JWKSetParser.class);
        mockURL = new URL("http://www.google.com");
        testResolver = new JWKOpenIdResolverImpl("Test", mockURL, mockParser);
    }

    @Test
    public void testResolverReloadsJWKWhenProvidedWithAnInvalidKeyId()
            throws FailedToLoadJWKException, InvalidSignatureException {
        //given
        SignedJwt mockJwt = mock(SignedJwt.class);
        JwsHeader mockHeader = mock(JwsHeader.class);

        given(mockJwt.getHeader()).willReturn(mockHeader);
        given(mockHeader.getKeyId()).willReturn("keyId");

        verify(mockParser, times(1)).generateMapFromJWK(any(URL.class)); //first time occured on creation

        boolean success = false;

        //when
        try {
            testResolver.verifySignature(mockJwt);
        } catch (InvalidSignatureException e) {
            success = true;
        }

        //then
        verify(mockParser, times(2)).generateMapFromJWK(any(URL.class)); //second time when we found no id
        assertTrue(success);
    }

    @Test(expectedExceptions = InvalidSignatureException.class)
    public void testInvalidSignatureThrowsException()
            throws InvalidSignatureException, FailedToLoadJWKException {

        //given
        SignedJwt mockJwt = mock(SignedJwt.class);
        JwsHeader mockHeader = mock(JwsHeader.class);

        given(mockJwt.getHeader()).willReturn(mockHeader);
        given(mockHeader.getKeyId()).willReturn("keyId");

        given(mockJwt.verify(signingHandler)).willReturn(false);

        //when
        testResolver.verifySignature(mockJwt);

        //then checked by exception

    }

    @Test(expectedExceptions = JwtExpiredException.class)
    public void testExpiredTokenThrowsException() throws JwtExpiredException {
        //given
        Date pastDate = new Date();
        pastDate.setTime(1);

        //when
        testResolver.verifyExpiration(pastDate);

        //then checked by exception
    }

    @Test(expectedExceptions = InvalidIssException.class)
    public void testInvalidIssuerThrowsException() throws InvalidIssException {
        //given
        String wrongIssuer = "One";

        //when
        testResolver.verifyIssuer(wrongIssuer);

        //then checked by exception
    }


}
