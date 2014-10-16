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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.json.jose.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.json.jose.exceptions.JwtRuntimeException;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class JwtClaimsSetTest {

    @Test
    public void shouldSetType() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();

        //When
        claimsSet.setType("TYPE");

        //Then
        assertTrue(claimsSet.get("typ").required().isString());
        assertEquals(claimsSet.get("typ").asString(), "TYPE");
    }

    @Test
    public void shouldSetJwtId() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();

        //When
        claimsSet.setJwtId("JWT_ID");

        //Then
        assertTrue(claimsSet.get("jti").required().isString());
        assertEquals(claimsSet.get("jti").asString(), "JWT_ID");
    }

    @Test
    public void shouldSetIssuer() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();

        //When
        claimsSet.setIssuer("ISSUER");

        //Then
        assertTrue(claimsSet.get("iss").required().isString());
        assertEquals(claimsSet.get("iss").asString(), "ISSUER");
    }

    @Test
    public void shouldSetIssuerURI() throws URISyntaxException {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        URI issuer = new URI("urn:example:animal:ferret:nose");

        //When
        claimsSet.setIssuer(issuer);

        //Then
        assertTrue(claimsSet.get("iss").required().isString());
        assertEquals(claimsSet.get("iss").asString(), "urn:example:animal:ferret:nose");
        assertEquals(new URI(claimsSet.get("iss").asString()), issuer);
    }

    @Test
    public void shouldSetSubject() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();

        //When
        claimsSet.setSubject("SUBJECT");

        //Then
        assertTrue(claimsSet.get("sub").required().isString());
        assertEquals(claimsSet.get("sub").asString(), "SUBJECT");
    }

    @Test
    public void shouldSetSubjectURI() throws URISyntaxException {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        URI subject = new URI("urn:example:animal:ferret:nose");

        //When
        claimsSet.setSubject(subject);

        //Then
        assertTrue(claimsSet.get("sub").required().isString());
        assertEquals(claimsSet.get("sub").asString(), "urn:example:animal:ferret:nose");
        assertEquals(new URI(claimsSet.get("sub").asString()), subject);
    }

    @Test
    public void shouldAddAudience() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();

        //When
        claimsSet.addAudience("AUDIENCE");

        //Then
        assertTrue(claimsSet.get("aud").required().isList());
        assertEquals(claimsSet.get("aud").asList(String.class).size(), 1);
        assertTrue(claimsSet.get("aud").asList(String.class).contains("AUDIENCE"));
    }

    @Test
    public void shouldAddAudienceURI() throws URISyntaxException {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        URI audience = new URI("urn:example:animal:ferret:nose");

        //When
        claimsSet.addAudience(audience);

        //Then
        assertTrue(claimsSet.get("aud").required().isList());
        assertEquals(claimsSet.get("aud").asList(String.class).size(), 1);
        assertTrue(claimsSet.get("aud").asList(String.class).contains("urn:example:animal:ferret:nose"));
        assertEquals(new URI(claimsSet.get("aud").asList(String.class).get(0)), audience);
    }

    @Test
    public void shouldSetIssuedAtTime() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        Date issuedAtTime = new Date();

        //When
        claimsSet.setIssuedAtTime(issuedAtTime);

        //Then
        assertTrue(claimsSet.get("iat").required().isNumber());
        assertEquals(claimsSet.get("iat").asLong(), (Long) (issuedAtTime.getTime() / 1000));
    }

    @Test
    public void shouldSetNotBeforeTime() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        Date notBeforeTime = new Date();

        //When
        claimsSet.setNotBeforeTime(notBeforeTime);

        //Then
        assertTrue(claimsSet.get("nbf").required().isNumber());
        assertEquals(claimsSet.get("nbf").asLong(), (Long) (notBeforeTime.getTime() / 1000));
    }

    @Test
    public void shouldSetExpirationTime() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        Date expirationTime = new Date();

        //When
        claimsSet.setExpirationTime(expirationTime);

        //Then
        assertTrue(claimsSet.get("exp").required().isNumber());
        assertEquals(claimsSet.get("exp").asLong(), (Long) (expirationTime.getTime() / 1000));
    }

    @Test
    public void shouldSetClaim() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();

        //When
        claimsSet.setClaim("KEY", "VALUE");

        //Then
        assertTrue(claimsSet.isDefined("KEY"));
        assertTrue(claimsSet.get("KEY").required().isString());
        assertEquals(claimsSet.get("KEY").asString(), "VALUE");
    }

    @Test
    public void shouldSetClaims() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("KEY1", "CLAIM1");
        claims.put("KEY2", true);
        claims.put("KEY3", 1234L);
        claims.put("KEY4", 1234);

        //When
        claimsSet.setClaims(claims);

        //Then
        assertTrue(claimsSet.isDefined("KEY1"));
        assertTrue(claimsSet.get("KEY1").required().isString());
        assertEquals(claimsSet.get("KEY1").asString(), "CLAIM1");

        assertTrue(claimsSet.isDefined("KEY2"));
        assertTrue(claimsSet.get("KEY2").required().isBoolean());
        assertEquals(claimsSet.get("KEY2").asBoolean(), (Boolean) true);

        assertTrue(claimsSet.isDefined("KEY3"));
        assertTrue(claimsSet.get("KEY3").required().isNumber());
        assertEquals(claimsSet.get("KEY3").asLong(), (Long) 1234L);

        assertTrue(claimsSet.isDefined("KEY4"));
        assertTrue(claimsSet.get("KEY4").required().isNumber());
        assertEquals(claimsSet.get("KEY4").asInteger(), (Integer) 1234);
    }

    @Test
    public void shouldGetType() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        claimsSet.setClaim("typ", "TYPE");

        //When
        String type = claimsSet.getType();

        //Then
        assertEquals(type, "TYPE");
    }

    @Test
    public void shouldGetJwtId() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        claimsSet.setClaim("jti", "JWT_ID");

        //When
        String jwtId = claimsSet.getJwtId();

        //Then
        assertEquals(jwtId, "JWT_ID");
    }

    @Test
    public void shouldGetIssuer() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        claimsSet.setClaim("iss", "ISSUER");

        //When
        String issuer = claimsSet.getIssuer();

        //Then
        assertEquals(issuer, "ISSUER");
    }

    @Test
    public void shouldGetSubject() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        claimsSet.setClaim("sub", "SUBJECT");

        //When
        String subject = claimsSet.getSubject();

        //Then
        assertEquals(subject, "SUBJECT");
    }

    @Test
    public void shouldGetAudience() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();

        List<String> audienceList = new ArrayList<String>();
        audienceList.add("AUDIENCE1");
        audienceList.add("AUDIENCE2");

        claimsSet.setClaim("aud", audienceList);

        //When
        List<String> audience = claimsSet.getAudience();

        //Then
        assertEquals(audience, audienceList);
    }

    @Test
    public void shouldGetIssuedAtTime() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        Calendar calendar = Calendar.getInstance();
        Date issuedAtTime = new Date();
        calendar.setTime(issuedAtTime);
        calendar.set(Calendar.MILLISECOND, 0);
        claimsSet.setClaim("iat", issuedAtTime);

        //When
        Date actualIssuedAtTime = claimsSet.getIssuedAtTime();

        //Then
        assertEquals(actualIssuedAtTime, calendar.getTime());
    }

    @Test
    public void shouldGetNotBeforeTime() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        Calendar calendar = Calendar.getInstance();
        Date notBeforeTime = new Date();
        calendar.setTime(notBeforeTime);
        calendar.set(Calendar.MILLISECOND, 0);
        claimsSet.setClaim("nbf", notBeforeTime);

        //When
        Date actualNotBeforeTime = claimsSet.getNotBeforeTime();

        //Then
        assertEquals(actualNotBeforeTime, calendar.getTime());
    }

    @Test
    public void shouldGetExpirationTime() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        Calendar calendar = Calendar.getInstance();
        Date expirationTime = new Date();
        calendar.setTime(expirationTime);
        calendar.set(Calendar.MILLISECOND, 0);
        claimsSet.setClaim("exp", expirationTime);

        //When
        Date actualExpirationTime = claimsSet.getExpirationTime();

        //Then
        assertEquals(actualExpirationTime, calendar.getTime());
    }

    @Test
    public void shouldGetClaim() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        claimsSet.put("KEY1", "CLAIM1");
        claimsSet.put("KEY2", true);
        claimsSet.put("KEY3", 1234L);
        claimsSet.put("KEY4", 1234);

        //When
        Object key1 = claimsSet.getClaim("KEY1");
        Object key2 = claimsSet.getClaim("KEY2");
        Object key3 = claimsSet.getClaim("KEY3");
        Object key4 = claimsSet.getClaim("KEY4");

        //Then
        assertEquals(key1, "CLAIM1");
        assertEquals(key2, true);
        assertEquals(key3, 1234L);
        assertEquals(key4, 1234);
    }

    @Test
    public void shouldGetClaimUsingClass() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        claimsSet.put("KEY1", "CLAIM1");
        claimsSet.put("KEY2", true);
        claimsSet.put("KEY3", 1234L);
        claimsSet.put("KEY4", 1234);

        //When
        String key1 = claimsSet.getClaim("KEY1", String.class);
        boolean key2 = claimsSet.getClaim("KEY2", Boolean.class);
        long key3 = claimsSet.getClaim("KEY3", Long.class);
        int key4 = claimsSet.getClaim("KEY4", Integer.class);

        //Then
        assertEquals(key1, "CLAIM1");
        assertEquals(key2, true);
        assertEquals(key3, 1234L);
        assertEquals(key4, 1234);
    }

    @Test
    public void shouldBuildJwtClaimSetToJsonString() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        claimsSet.setJwtId("JWT_ID");
        claimsSet.setClaim("KEY1", "CLAIM1");
        claimsSet.setClaim("KEY2", true);

        //When
        String jsonString = claimsSet.build();

        //Then
        assertThat(jsonString).contains("\"KEY2\": true", "\"KEY1\": \"CLAIM1\"", "\"jti\": \"JWT_ID\"");
    }

    @Test
    public void shouldCreateJwtClaimSetWithMap() {

        //Given
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("typ", "TYPE");
        claims.put("jti", "JWT_ID");
        claims.put("iss", "ISSUER");
        claims.put("sub", "SUBJECT");
        List<String> audience = new ArrayList<String>();
        audience.add("AUDIENCE1");
        audience.add("AUDIENCE2");
        claims.put("aud", audience);
        Date issuedAtTime = new Date();
        claims.put("iat", issuedAtTime);
        Date notBeforeTime = new Date();
        claims.put("nbf", notBeforeTime);
        Date expirationTime = new Date();
        claims.put("exp", expirationTime);
        claims.put("KEY1", "CLAIM1");
        claims.put("KEY2", true);
        claims.put("KEY3", 1234L);
        claims.put("KEY4", 1234);

        //When
        JwtClaimsSet claimsSet = new JwtClaimsSet(claims);

        //Then
        assertTrue(claimsSet.get("typ").required().isString());
        assertEquals(claimsSet.get("typ").asString(), "TYPE");

        assertTrue(claimsSet.get("jti").required().isString());
        assertEquals(claimsSet.get("jti").asString(), "JWT_ID");

        assertTrue(claimsSet.get("iss").required().isString());
        assertEquals(claimsSet.get("iss").asString(), "ISSUER");

        assertTrue(claimsSet.get("sub").required().isString());
        assertEquals(claimsSet.get("sub").asString(), "SUBJECT");

        assertTrue(claimsSet.get("aud").required().isList());
        assertEquals(claimsSet.get("aud").asList(String.class).size(), 2);
        assertTrue(claimsSet.get("aud").asList(String.class).contains("AUDIENCE1"));
        assertTrue(claimsSet.get("aud").asList(String.class).contains("AUDIENCE2"));

        assertTrue(claimsSet.get("iat").required().isNumber());
        assertEquals(claimsSet.get("iat").asLong(), (Long) (issuedAtTime.getTime() / 1000));

        assertTrue(claimsSet.get("nbf").required().isNumber());
        assertEquals(claimsSet.get("nbf").asLong(), (Long) (notBeforeTime.getTime() / 1000));

        assertTrue(claimsSet.get("exp").required().isNumber());
        assertEquals(claimsSet.get("exp").asLong(), (Long) (expirationTime.getTime() / 1000));

        assertTrue(claimsSet.isDefined("KEY1"));
        assertTrue(claimsSet.get("KEY1").required().isString());
        assertEquals(claimsSet.get("KEY1").asString(), "CLAIM1");

        assertTrue(claimsSet.isDefined("KEY2"));
        assertTrue(claimsSet.get("KEY2").required().isBoolean());
        assertEquals(claimsSet.get("KEY2").asBoolean(), (Boolean) true);

        assertTrue(claimsSet.isDefined("KEY3"));
        assertTrue(claimsSet.get("KEY3").required().isNumber());
        assertEquals(claimsSet.get("KEY3").asLong(), (Long) 1234L);

        assertTrue(claimsSet.isDefined("KEY4"));
        assertTrue(claimsSet.get("KEY4").required().isNumber());
        assertEquals(claimsSet.get("KEY4").asInteger(), (Integer) 1234);
    }

    @Test
    public void shouldCreateJwtClaimSetUsingURIsWithMap() throws URISyntaxException {

        //Given
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("typ", "TYPE");
        claims.put("jti", "JWT_ID");
        URI issuer = new URI("urn:issuer:animal:ferret:nose");
        claims.put("iss", issuer);
        URI subject = new URI("urn:subject:animal:ferret:nose");
        claims.put("sub", subject);
        List<URI> audienceList = new ArrayList<URI>();
        URI audience1 = new URI("urn:audience1:animal:ferret:nose");
        URI audience2 = new URI("urn:audience2:animal:ferret:nose");
        audienceList.add(audience1);
        audienceList.add(audience2);
        claims.put("aud", audienceList);
        Date issuedAtTime = new Date();
        claims.put("iat", issuedAtTime);
        Date notBeforeTime = new Date();
        claims.put("nbf", notBeforeTime);
        Date expirationTime = new Date();
        claims.put("exp", expirationTime);
        claims.put("KEY1", "CLAIM1");
        claims.put("KEY2", true);
        claims.put("KEY3", 1234L);
        claims.put("KEY4", 1234);

        //When
        JwtClaimsSet claimsSet = new JwtClaimsSet(claims);

        //Then
        assertTrue(claimsSet.get("typ").required().isString());
        assertEquals(claimsSet.get("typ").asString(), "TYPE");

        assertTrue(claimsSet.get("jti").required().isString());
        assertEquals(claimsSet.get("jti").asString(), "JWT_ID");

        assertTrue(claimsSet.get("iss").required().isString());
        assertEquals(claimsSet.get("iss").asString(), "urn:issuer:animal:ferret:nose");
        assertEquals(new URI(claimsSet.get("iss").asString()), issuer);

        assertTrue(claimsSet.get("sub").required().isString());
        assertEquals(claimsSet.get("sub").asString(), "urn:subject:animal:ferret:nose");
        assertEquals(new URI(claimsSet.get("sub").asString()), subject);

        assertTrue(claimsSet.get("aud").required().isList());
        assertEquals(claimsSet.get("aud").asList(String.class).size(), 2);
        assertTrue(claimsSet.get("aud").asList(String.class).contains("urn:audience1:animal:ferret:nose"));
        assertTrue(claimsSet.get("aud").asList(String.class).contains("urn:audience2:animal:ferret:nose"));
        assertEquals(new URI(claimsSet.get("aud").asList(String.class).get(0)), audience1);
        assertEquals(new URI(claimsSet.get("aud").asList(String.class).get(1)), audience2);

        assertTrue(claimsSet.get("iat").required().isNumber());
        assertEquals(claimsSet.get("iat").asLong(), (Long) (issuedAtTime.getTime() / 1000));

        assertTrue(claimsSet.get("nbf").required().isNumber());
        assertEquals(claimsSet.get("nbf").asLong(), (Long) (notBeforeTime.getTime() / 1000));

        assertTrue(claimsSet.get("exp").required().isNumber());
        assertEquals(claimsSet.get("exp").asLong(), (Long) (expirationTime.getTime() / 1000));

        assertTrue(claimsSet.isDefined("KEY1"));
        assertTrue(claimsSet.get("KEY1").required().isString());
        assertEquals(claimsSet.get("KEY1").asString(), "CLAIM1");

        assertTrue(claimsSet.isDefined("KEY2"));
        assertTrue(claimsSet.get("KEY2").required().isBoolean());
        assertEquals(claimsSet.get("KEY2").asBoolean(), (Boolean) true);

        assertTrue(claimsSet.isDefined("KEY3"));
        assertTrue(claimsSet.get("KEY3").required().isNumber());
        assertEquals(claimsSet.get("KEY3").asLong(), (Long) 1234L);

        assertTrue(claimsSet.isDefined("KEY4"));
        assertTrue(claimsSet.get("KEY4").required().isNumber());
        assertEquals(claimsSet.get("KEY4").asInteger(), (Integer) 1234);
    }

    @Test
    public void shouldSetAudience() throws URISyntaxException {

        //Given
        Map<String, Object> claims = new HashMap<String, Object>();
        URI audience = new URI("urn:audience1:animal:ferret:nose");
        claims.put("aud", audience);

        //When
        JwtClaimsSet claimsSet = new JwtClaimsSet(claims);

        //Then
        assertTrue(claimsSet.get("aud").required().isList());
        assertEquals(claimsSet.get("aud").asList(String.class).size(), 1);
        assertTrue(claimsSet.get("aud").asList(String.class).contains("urn:audience1:animal:ferret:nose"));
        assertEquals(new URI(claimsSet.get("aud").asList(String.class).get(0)), audience);
    }

    @Test
    public void shouldSetAudienceWithURIs() {

        //Given
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("aud", "AUDIENCE");

        //When
        JwtClaimsSet claimsSet = new JwtClaimsSet(claims);

        //Then
        assertTrue(claimsSet.get("aud").required().isList());
        assertEquals(claimsSet.get("aud").asList(String.class).size(), 1);
        assertTrue(claimsSet.get("aud").asList(String.class).contains("AUDIENCE"));
    }

    @Test (expectedExceptions = JwtRuntimeException.class)
    public void shouldThrowJwtRuntimeExceptionWhenValueIsOfWrongType() {

        //Given
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("typ", "TYPE");
        claims.put("jti", 1);
        claims.put("iss", "ISSUER");

        //When
        new JwtClaimsSet(claims);

        //Then
        fail("Should have thrown exception!");
    }
}
