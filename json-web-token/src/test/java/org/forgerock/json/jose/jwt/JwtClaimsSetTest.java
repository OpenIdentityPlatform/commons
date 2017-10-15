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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.json.jose.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import org.forgerock.json.jose.exceptions.JwtRuntimeException;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("javadoc")
public class JwtClaimsSetTest {

    @Test
    public void shouldSetType() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();

        //When
        claimsSet.setType("TYPE");

        //Then
        assertThat(claimsSet.get("typ").required().isString()).isTrue();
        assertThat(claimsSet.get("typ").asString()).isEqualTo("TYPE");
    }

    @Test
    public void shouldSetJwtId() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();

        //When
        claimsSet.setJwtId("JWT_ID");

        //Then
        assertThat(claimsSet.get("jti").required().isString()).isTrue();
        assertThat(claimsSet.get("jti").asString()).isEqualTo("JWT_ID");
    }

    @Test
    public void shouldSetIssuer() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();

        //When
        claimsSet.setIssuer("ISSUER");

        //Then
        assertThat(claimsSet.get("iss").required().isString()).isTrue();
        assertThat(claimsSet.get("iss").asString()).isEqualTo("ISSUER");
    }

    @Test
    public void shouldSetIssuerURI() throws URISyntaxException {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        URI issuer = new URI("urn:example:animal:ferret:nose");

        //When
        claimsSet.setIssuer(issuer);

        //Then
        assertThat(claimsSet.get("iss").required().isString()).isTrue();
        assertThat(claimsSet.get("iss").asString()).isEqualTo("urn:example:animal:ferret:nose");
        assertThat(new URI(claimsSet.get("iss").asString())).isEqualTo(issuer);
    }

    @Test
    public void shouldSetSubject() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();

        //When
        claimsSet.setSubject("SUBJECT");

        //Then
        assertThat(claimsSet.get("sub").required().isString()).isTrue();
        assertThat(claimsSet.get("sub").asString()).isEqualTo("SUBJECT");
    }

    @Test
    public void shouldSetSubjectURI() throws URISyntaxException {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        URI subject = new URI("urn:example:animal:ferret:nose");

        //When
        claimsSet.setSubject(subject);

        //Then
        assertThat(claimsSet.get("sub").required().isString()).isTrue();
        assertThat(claimsSet.get("sub").asString()).isEqualTo("urn:example:animal:ferret:nose");
        assertThat(new URI(claimsSet.get("sub").asString())).isEqualTo(subject);
    }

    @Test
    public void shouldAddAudience() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();

        //When
        claimsSet.addAudience("AUDIENCE");

        //Then
        assertThat(claimsSet.get("aud").required().isString()).isTrue();
        assertThat(claimsSet.get("aud").asString()).isEqualTo("AUDIENCE");
    }

    @Test
    public void addingSecondAudienceShouldConvertAudienceToList() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        claimsSet.addAudience("AUDIENCE1");

        //When
        claimsSet.addAudience("AUDIENCE2");

        //Then
        assertThat(claimsSet.get("aud").required().isList()).isTrue();
        assertThat(claimsSet.get("aud").asList(String.class)).hasSize(2);
        assertThat(claimsSet.get("aud").asList(String.class)).contains("AUDIENCE1", "AUDIENCE2");
    }

    @Test
    public void shouldAddAudienceURI() throws URISyntaxException {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        URI audience = new URI("urn:example:animal:ferret:nose");

        //When
        claimsSet.addAudience(audience);

        //Then
        assertThat(claimsSet.get("aud").required().isString()).isTrue();
        assertThat(claimsSet.get("aud").asString()).isEqualTo("urn:example:animal:ferret:nose");
        assertThat(new URI(claimsSet.get("aud").asString())).isEqualTo(audience);
    }

    @Test
    public void shouldSetIssuedAtTime() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        Date issuedAtTime = new Date();

        //When
        claimsSet.setIssuedAtTime(issuedAtTime);

        //Then
        assertThat(claimsSet.get("iat").required().isNumber()).isTrue();
        assertThat(claimsSet.get("iat").asLong()).isEqualTo(timeInSeconds(issuedAtTime));
    }

    @Test
    public void shouldSetNotBeforeTime() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        Date notBeforeTime = new Date();

        //When
        claimsSet.setNotBeforeTime(notBeforeTime);

        //Then
        assertThat(claimsSet.get("nbf").required().isNumber()).isTrue();
        assertThat(claimsSet.get("nbf").asLong()).isEqualTo(timeInSeconds(notBeforeTime));
    }

    @Test
    public void shouldSetExpirationTime() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        Date expirationTime = new Date();

        //When
        claimsSet.setExpirationTime(expirationTime);

        //Then
        assertThat(claimsSet.get("exp").required().isNumber()).isTrue();
        assertThat(claimsSet.get("exp").asLong()).isEqualTo(timeInSeconds(expirationTime));
    }

    @Test
    public void shouldSetClaim() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();

        //When
        claimsSet.setClaim("KEY", "VALUE");

        //Then
        assertThat(claimsSet.isDefined("KEY")).isTrue();
        assertThat(claimsSet.get("KEY").required().isString()).isTrue();
        assertThat(claimsSet.get("KEY").asString()).isEqualTo("VALUE");
    }

    @Test
    public void shouldSetClaims() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        Map<String, Object> claims = new HashMap<>();
        claims.put("KEY1", "CLAIM1");
        claims.put("KEY2", true);
        claims.put("KEY3", 1234L);
        claims.put("KEY4", 1234);

        //When
        claimsSet.setClaims(claims);

        //Then
        assertThat(claimsSet.isDefined("KEY1")).isTrue();
        assertThat(claimsSet.get("KEY1").required().isString()).isTrue();
        assertThat(claimsSet.get("KEY1").asString()).isEqualTo("CLAIM1");

        assertThat(claimsSet.isDefined("KEY2")).isTrue();
        assertThat(claimsSet.get("KEY2").required().isBoolean()).isTrue();
        assertThat(claimsSet.get("KEY2").asBoolean()).isEqualTo(true);

        assertThat(claimsSet.isDefined("KEY3")).isTrue();
        assertThat(claimsSet.get("KEY3").required().isNumber()).isTrue();
        assertThat(claimsSet.get("KEY3").asLong()).isEqualTo(1234L);

        assertThat(claimsSet.isDefined("KEY4")).isTrue();
        assertThat(claimsSet.get("KEY4").required().isNumber()).isTrue();
        assertThat(claimsSet.get("KEY4").asInteger()).isEqualTo(1234);
    }

    @Test
    public void shouldGetType() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        claimsSet.setClaim("typ", "TYPE");

        //When
        String type = claimsSet.getType();

        //Then
        assertThat(type)
                .isEqualTo("TYPE")
                .isEqualTo(claimsSet.getClaim("typ"));
    }

    @Test
    public void shouldGetJwtId() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        claimsSet.setClaim("jti", "JWT_ID");

        //When
        String jwtId = claimsSet.getJwtId();

        //Then
        assertThat(jwtId)
                .isEqualTo("JWT_ID")
                .isEqualTo(claimsSet.getClaim("jti"));
    }

    @Test
    public void shouldGetIssuer() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        claimsSet.setClaim("iss", "ISSUER");

        //When
        String issuer = claimsSet.getIssuer();

        //Then
        assertThat(issuer)
                .isEqualTo("ISSUER")
                .isEqualTo(claimsSet.getClaim("iss"));
    }

    @Test
    public void shouldGetSubject() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        claimsSet.setClaim("sub", "SUBJECT");

        //When
        String subject = claimsSet.getSubject();

        //Then
        assertThat(subject)
                .isEqualTo("SUBJECT")
                .isEqualTo(claimsSet.getClaim("sub"));
    }

    @Test
    public void shouldGetAudience() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();

        List<String> audienceList = new ArrayList<>();
        audienceList.add("AUDIENCE1");
        audienceList.add("AUDIENCE2");

        claimsSet.setClaim("aud", audienceList);

        //When
        List<String> audience = claimsSet.getAudience();

        //Then
        assertThat(audience)
                .isEqualTo(audienceList)
                .isEqualTo(claimsSet.getClaim("aud"));
    }

    @Test
    public void shouldGetIssuedAtTimeGivenDate() {

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
        assertThat(actualIssuedAtTime)
                .isEqualTo(calendar.getTime())
                .isEqualTo(claimsSet.getClaim("iat"));
    }

    @Test
    public void shouldGetIssuedAtTimeGivenLong() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        Long issuedAtTime = currentTimeInSeconds();
        claimsSet.setClaim("iat", issuedAtTime);

        //When
        Date actualIssuedAtTime = claimsSet.getIssuedAtTime();

        //Then
        assertThat(actualIssuedAtTime).isEqualTo(new Date(issuedAtTime * 1000L));
    }

    @Test
    public void shouldGetNotBeforeTimeGivenDate() {

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
        assertThat(actualNotBeforeTime)
                .isEqualTo(calendar.getTime())
                .isEqualTo(claimsSet.getClaim("nbf"));
    }

    @Test
    public void shouldGetNotBeforeTimeGivenLong() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        Long notBeforeTime = currentTimeInSeconds();
        claimsSet.setClaim("nbf", notBeforeTime);

        //When
        Date actualNotBeforeTime = claimsSet.getNotBeforeTime();

        //Then
        assertThat(actualNotBeforeTime).isEqualTo(new Date(notBeforeTime * 1000L));
    }

    @Test
    public void shouldGetExpirationTimeGivenDate() {

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
        assertThat(actualExpirationTime)
                .isEqualTo(calendar.getTime())
                .isEqualTo(claimsSet.getClaim("exp"));
    }

    @Test
    public void shouldGetExpirationTimeGivenLong() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();
        Long expirationTime = currentTimeInSeconds();
        claimsSet.setClaim("exp", expirationTime);

        //When
        Date actualExpirationTime = claimsSet.getExpirationTime();

        //Then
        assertThat(actualExpirationTime).isEqualTo(new Date(expirationTime * 1000L));
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
        assertThat(key1).isEqualTo("CLAIM1");
        assertThat(key2).isEqualTo(true);
        assertThat(key3).isEqualTo(1234L);
        assertThat(key4).isEqualTo(1234);
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
        assertThat(key1).isEqualTo("CLAIM1");
        assertThat(key2).isEqualTo(true);
        assertThat(key3).isEqualTo(1234L);
        assertThat(key4).isEqualTo(1234);
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
        assertThat(jsonString).contains("\"jti\":\"JWT_ID\",\"KEY1\":\"CLAIM1\",\"KEY2\":true");
    }

    @Test
    public void shouldCreateJwtClaimSetWithMap() {

        //Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("typ", "TYPE");
        claims.put("jti", "JWT_ID");
        claims.put("iss", "ISSUER");
        claims.put("sub", "SUBJECT");
        List<String> audience = new ArrayList<>();
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
        assertThat(claimsSet.get("typ").required().isString()).isTrue();
        assertThat(claimsSet.get("typ").asString()).isEqualTo("TYPE");

        assertThat(claimsSet.get("jti").required().isString()).isTrue();
        assertThat(claimsSet.get("jti").asString()).isEqualTo("JWT_ID");

        assertThat(claimsSet.get("iss").required().isString()).isTrue();
        assertThat(claimsSet.get("iss").asString()).isEqualTo("ISSUER");

        assertThat(claimsSet.get("sub").required().isString()).isTrue();
        assertThat(claimsSet.get("sub").asString()).isEqualTo("SUBJECT");

        assertThat(claimsSet.get("aud").required().isList()).isTrue();
        assertThat(claimsSet.get("aud").asList(String.class).size()).isEqualTo(2);
        assertThat(claimsSet.get("aud").asList(String.class)).contains("AUDIENCE1");
        assertThat(claimsSet.get("aud").asList(String.class)).contains("AUDIENCE2");

        assertThat(claimsSet.get("iat").required().isNumber()).isTrue();
        assertThat(claimsSet.get("iat").asLong()).isEqualTo(timeInSeconds(issuedAtTime));

        assertThat(claimsSet.get("nbf").required().isNumber()).isTrue();
        assertThat(claimsSet.get("nbf").asLong()).isEqualTo(timeInSeconds(notBeforeTime));

        assertThat(claimsSet.get("exp").required().isNumber()).isTrue();
        assertThat(claimsSet.get("exp").asLong()).isEqualTo(timeInSeconds(expirationTime));

        assertThat(claimsSet.isDefined("KEY1")).isTrue();
        assertThat(claimsSet.get("KEY1").required().isString()).isTrue();
        assertThat(claimsSet.get("KEY1").asString()).isEqualTo("CLAIM1");

        assertThat(claimsSet.isDefined("KEY2")).isTrue();
        assertThat(claimsSet.get("KEY2").required().isBoolean()).isTrue();
        assertThat(claimsSet.get("KEY2").asBoolean()).isEqualTo(true);

        assertThat(claimsSet.isDefined("KEY3")).isTrue();
        assertThat(claimsSet.get("KEY3").required().isNumber()).isTrue();
        assertThat(claimsSet.get("KEY3").asLong()).isEqualTo(1234L);

        assertThat(claimsSet.isDefined("KEY4")).isTrue();
        assertThat(claimsSet.get("KEY4").required().isNumber()).isTrue();
        assertThat(claimsSet.get("KEY4").asInteger()).isEqualTo(1234);
    }

    @Test
    public void shouldCreateJwtClaimSetUsingURIsWithMap() throws URISyntaxException {

        //Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("typ", "TYPE");
        claims.put("jti", "JWT_ID");
        URI issuer = new URI("urn:issuer:animal:ferret:nose");
        claims.put("iss", issuer);
        URI subject = new URI("urn:subject:animal:ferret:nose");
        claims.put("sub", subject);
        List<URI> audienceList = new ArrayList<>();
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
        assertThat(claimsSet.get("typ").required().isString()).isTrue();
        assertThat(claimsSet.get("typ").asString()).isEqualTo("TYPE");

        assertThat(claimsSet.get("jti").required().isString()).isTrue();
        assertThat(claimsSet.get("jti").asString()).isEqualTo("JWT_ID");

        assertThat(claimsSet.get("iss").required().isString()).isTrue();
        assertThat(claimsSet.get("iss").asString()).isEqualTo("urn:issuer:animal:ferret:nose");
        assertThat(new URI(claimsSet.get("iss").asString())).isEqualTo(issuer);

        assertThat(claimsSet.get("sub").required().isString()).isTrue();
        assertThat(claimsSet.get("sub").asString()).isEqualTo("urn:subject:animal:ferret:nose");
        assertThat(new URI(claimsSet.get("sub").asString())).isEqualTo(subject);

        assertThat(claimsSet.get("aud").required().isList()).isTrue();
        assertThat(claimsSet.get("aud").asList(String.class).size()).isEqualTo(2);
        assertThat(claimsSet.get("aud").asList(String.class)).contains("urn:audience1:animal:ferret:nose");
        assertThat(claimsSet.get("aud").asList(String.class)).contains("urn:audience2:animal:ferret:nose");
        assertThat(new URI(claimsSet.get("aud").asList(String.class).get(0))).isEqualTo(audience1);
        assertThat(new URI(claimsSet.get("aud").asList(String.class).get(1))).isEqualTo(audience2);

        assertThat(claimsSet.get("iat").required().isNumber()).isTrue();
        assertThat(claimsSet.get("iat").asLong()).isEqualTo(timeInSeconds(issuedAtTime));

        assertThat(claimsSet.get("nbf").required().isNumber()).isTrue();
        assertThat(claimsSet.get("nbf").asLong()).isEqualTo(timeInSeconds(notBeforeTime));

        assertThat(claimsSet.get("exp").required().isNumber()).isTrue();
        assertThat(claimsSet.get("exp").asLong()).isEqualTo(timeInSeconds(expirationTime));

        assertThat(claimsSet.isDefined("KEY1")).isTrue();
        assertThat(claimsSet.get("KEY1").required().isString()).isTrue();
        assertThat(claimsSet.get("KEY1").asString()).isEqualTo("CLAIM1");

        assertThat(claimsSet.isDefined("KEY2")).isTrue();
        assertThat(claimsSet.get("KEY2").required().isBoolean()).isTrue();
        assertThat(claimsSet.get("KEY2").asBoolean()).isEqualTo(true);

        assertThat(claimsSet.isDefined("KEY3")).isTrue();
        assertThat(claimsSet.get("KEY3").required().isNumber()).isTrue();
        assertThat(claimsSet.get("KEY3").asLong()).isEqualTo(1234L);

        assertThat(claimsSet.isDefined("KEY4")).isTrue();
        assertThat(claimsSet.get("KEY4").required().isNumber()).isTrue();
        assertThat(claimsSet.get("KEY4").asInteger()).isEqualTo(1234);
    }

    @Test
    public void shouldSetAudience() throws URISyntaxException {

        //Given
        Map<String, Object> claims = new HashMap<>();
        URI audience = new URI("urn:example:animal:ferret:nose");
        claims.put("aud", audience);

        //When
        JwtClaimsSet claimsSet = new JwtClaimsSet(claims);

        //Then
        assertThat(claimsSet.get("aud").required().isString()).isTrue();
        assertThat(claimsSet.get("aud").asString()).isEqualTo("urn:example:animal:ferret:nose");
        assertThat(new URI(claimsSet.get("aud").asString())).isEqualTo(audience);
    }

    @Test
    public void shouldSetAudienceWithURIs() {

        //Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("aud", "AUDIENCE");

        //When
        JwtClaimsSet claimsSet = new JwtClaimsSet(claims);

        //Then
        assertThat(claimsSet.get("aud").required().isString()).isTrue();
        assertThat(claimsSet.get("aud").asString()).isEqualTo("AUDIENCE");
    }

    @Test(expectedExceptions = JwtRuntimeException.class)
    public void shouldThrowJwtRuntimeExceptionWhenValueIsOfWrongType() {

        //Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("typ", "TYPE");
        claims.put("jti", 1);
        claims.put("iss", "ISSUER");

        //When
        new JwtClaimsSet(claims);
    }

    @Test
    public void shouldBeOptionalClaims() {

        //Given
        JwtClaimsSet claimsSet = new JwtClaimsSet();

        //Then
        assertThat(claimsSet.getIssuer()).isNull();
        assertThat(claimsSet.getSubject()).isNull();
        assertThat(claimsSet.getAudience()).isNull();
        assertThat(claimsSet.getExpirationTime()).isNull();
        assertThat(claimsSet.getNotBeforeTime()).isNull();
        assertThat(claimsSet.getIssuedAtTime()).isNull();
        assertThat(claimsSet.getJwtId()).isNull();
    }

    private Long timeInSeconds(final Date date) {
        return date.getTime() / 1000L;
    }

    private Long currentTimeInSeconds() {
        return System.currentTimeMillis() / 1000L;
    }
}
