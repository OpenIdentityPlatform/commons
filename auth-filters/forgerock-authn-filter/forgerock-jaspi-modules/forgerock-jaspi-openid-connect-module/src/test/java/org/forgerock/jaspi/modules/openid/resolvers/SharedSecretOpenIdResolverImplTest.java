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

import java.security.Key;
import java.util.ArrayList;
import java.util.Date;
import org.forgerock.jaspi.modules.openid.exceptions.InvalidAudException;
import org.forgerock.jaspi.modules.openid.exceptions.InvalidIssException;
import org.forgerock.jaspi.modules.openid.exceptions.InvalidSignatureException;
import org.forgerock.jaspi.modules.openid.exceptions.JwtExpiredException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.JwsHeader;
import org.forgerock.json.jose.jws.SignedJwt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class SharedSecretOpenIdResolverImplTest {

    SharedSecretOpenIdResolverImpl testResolver;

    @BeforeTest
    public void setUp() {
        testResolver = new SharedSecretOpenIdResolverImpl("Test", "Test", "Test");
    }

    @Test (expectedExceptions = InvalidSignatureException.class)
    public void testInvalidSignatureThrowsException() throws InvalidSignatureException {

        //given
        Key mockKey = mock(Key.class);
        JwsHeader mockHeader = mock(JwsHeader.class);
        SignedJwt mockJwt = mock(SignedJwt.class);

        given(mockJwt.getHeader()).willReturn(mockHeader);
        given(mockHeader.getAlgorithm()).willReturn(JwsAlgorithm.HS256);
        given(mockJwt.verify(mockKey)).willReturn(false);

        //when
        testResolver.verifySignature(mockJwt);

        //then checked by exception

    }

    @Test(expectedExceptions = InvalidAudException.class)
    public void testInvalidAudienceThrowsException() throws InvalidAudException {
        //given
        JsonValue authorizedParty = new JsonValue("Test");
        ArrayList<String> audiences = new ArrayList<String>();
        audiences.add("One");
        audiences.add("Two");

        //when
        testResolver.verifyAudience(audiences, authorizedParty);

        //then checked by exception
    }

    @Test(expectedExceptions = InvalidAudException.class)
    public void testNullAudienceThrowsException() throws InvalidAudException {
        //given
        JsonValue authorizedParty = new JsonValue("Test");
        ArrayList<String> audiences = null;

        //when
        testResolver.verifyAudience(audiences, authorizedParty);

        //then checked by exception
    }

    @Test(expectedExceptions = InvalidAudException.class)
    public void testInvalidSpecifiedAudienceThrowsException() throws InvalidAudException {
        //given
        JsonValue authorizedParty = new JsonValue("Bob");
        ArrayList<String> audiences = new ArrayList<String>();
        audiences.add("Test");
        audiences.add("TestTwo");

        //when
        testResolver.verifyAudience(audiences, authorizedParty);

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
