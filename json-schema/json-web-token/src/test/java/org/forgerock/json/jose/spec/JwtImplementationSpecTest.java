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

package org.forgerock.json.jose.spec;

import static org.fest.assertions.Fail.fail;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.exceptions.JwtRuntimeException;
import org.forgerock.json.jose.helper.JwtTestHelper;
import org.forgerock.json.jose.helper.KeysHelper;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.json.jose.jwt.JwtType;
import org.forgerock.json.jose.utils.DuplicateMapEntryException;
import org.forgerock.json.jose.utils.IntDate;
import org.forgerock.json.jose.utils.StringOrURI;
import org.forgerock.util.encode.Base64url;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class JwtImplementationSpecTest {

    /**
     * JWT Spec: http://tools.ietf.org/html/draft-jones-json-web-token-10, Section 2 Terminology, StringOrURI:
     *
     * StringOrURI  A JSON string value, with the additional requirement that while arbitrary string values MAY be used,
     * any value containing a ":" character MUST be a URI as defined in RFC 3986 [RFC3986].
     */
    @Test
    public void shouldValidateArbitaryStringIsOk() {

        //Given
        String arbitaryString = "WHATERVER_;YOU._WANT";

        //When
        try {
            StringOrURI.validateStringOrURI(arbitaryString);
        } catch (JwtRuntimeException e) {
            fail();
        }

        //Then
        // As long it gets here then test has passed.
    }

    /**
     * JWT Spec: http://tools.ietf.org/html/draft-jones-json-web-token-10, Section 2 Terminology, StringOrURI:
     *
     * StringOrURI  A JSON string value, with the additional requirement that while arbitrary string values MAY be used,
     * any value containing a ":" character MUST be a URI as defined in RFC 3986 [RFC3986].
     */
    @Test (expectedExceptions = JwtRuntimeException.class)
    public void shouldValidateStringIsInvalidURI() {

        //Given
        String invalidString = "WHATERVER_:YOU._WANT";

        //When
        StringOrURI.validateStringOrURI(invalidString);

        //Then
        fail();
    }

    /**
     * JWT Spec: http://tools.ietf.org/html/draft-jones-json-web-token-10, Section 2 Terminology, StringOrURI:
     *
     * StringOrURI  A JSON string value, with the additional requirement that while arbitrary string values MAY be used,
     * any value containing a ":" character MUST be a URI as defined in RFC 3986 [RFC3986].
     */
    @Test
    public void shouldValidateStringIsValidURI() {

        //Given
        String validUriString = "urn:ietf:params:oauth:token-type:jwt";

        //When
        try {
            StringOrURI.validateStringOrURI(validUriString);
        } catch (JwtRuntimeException e) {
            fail();
        }

        //Then
        // As long it gets here then test has passed.
    }

    /**
     * JWT Spec: http://tools.ietf.org/html/draft-jones-json-web-token-10, Section 4 JWT Claims:
     *
     * The Claim Names within this object MUST be unique; JWTs with duplicate Claim Names MUST be rejected.
     */
    @Test
    public void shouldRejectJwtWithDuplicateClaimParameterNames() {

        //Given
        StringBuilder claimsSet = new StringBuilder();
        claimsSet.append("{")
                .append("\"KEY\"").append(":").append("\"VALUE1\"").append(",")
                .append("\"OTHER_KEY\"").append(":").append("\"VALUE2\"").append(",")
                .append("\"KEY\"").append(":").append("\"VALUE1A\"")
                .append("}");
        String jwtString = JwtTestHelper.encodedPlaintextJwt("{}", claimsSet.toString());

        //When
        boolean exceptionCaught = false;
        try {
            new JwtBuilderFactory().reconstruct(jwtString, Jwt.class);
        } catch (DuplicateMapEntryException e) {
            exceptionCaught = true;
        }

        //Then
        assertTrue(exceptionCaught);
    }


             //TODO Javadoc with reference to Spec


    @Test
    public void shouldConvertDateToIntDate() {

        //Given
        long intDateLong = 1373896932;
        Date intDate = new Date(intDateLong * 1000L);

        //When
        long convertedDate = IntDate.toIntDate(intDate);

        //Then
        assertEquals(convertedDate, intDateLong);
    }

    @Test
    public void shouldConvertIntDateToDate() {

        //Given
        long intDateLong = 1373896932;
        Date intDate = new Date(intDateLong * 1000L);

        //When
        Date convertedIntDate = IntDate.fromIntDate(intDateLong);

        //Then
        assertEquals(convertedIntDate, intDate);
    }

    @Test
    public void shouldRejectJwtWhenExpiryTimePassed() {
//                 fail(); //TODO
//        //Given
//        Calendar calendar = Calendar.getInstance();
//        calendar.add(Calendar.MINUTE, -1);
//        long expiryTime = calendar.getTime().getTime() / 1000L;
//
//        StringBuilder claimsSet = new StringBuilder();
//        claimsSet.append("{")
//                .append("\"exp\"").append(":").append(expiryTime)
//                .append("}");
//        String jwtString = JwtTestHelper.encodedPlaintextJwt("{}", claimsSet.toString());
//
//        //When
//        boolean exceptionCaught = false;
//        JwtException exception = null;
//        try {
//            new JwtBuilderFactory().reconstruct(jwtString, Jwt.class);
//        } catch (JwtException e) {
//            exceptionCaught = true;
//            exception = e;
//        }
//
//        //Then
//        assertTrue(exceptionCaught);
//        assertTrue(exception.getCause().getClass().isAssignableFrom(RuntimeException.class));//TODO proper exception
    }

    @Test
    public void shouldStoreExpiryDateAsIntDate() throws IOException {

        //Given
        JwtBuilderFactory jwtBuilderFactory = new JwtBuilderFactory();
        long expiryTimeLong = 1373896932;
        Date expiryTime = new Date(expiryTimeLong * 1000L);

        //When
        JwtClaimsSet claimsSet = jwtBuilderFactory.claims()
                .exp(expiryTime)
                .build();
        String jwtString = jwtBuilderFactory.jwt()
                .claims(claimsSet)
                .build();

        //Then
        String[] jwtParts = jwtString.split("\\.", -1);
        String claimsSetString = new String(Base64url.decode(jwtParts[1]));
        Map<String, Object> jwtMap = JwtTestHelper.jsonToMap(claimsSetString);
        long expTime = ((Number) jwtMap.get("exp")).longValue();
        assertEquals(expTime, expiryTimeLong);
    }

    @Test
    public void shouldRejectJwtWhenNotBeforeTimeNotPassed() {
//                       fail();//TODO
//        //Given
//        Calendar calendar = Calendar.getInstance();
//        calendar.add(Calendar.MINUTE, -1);
//        long notBeforeTime = calendar.getTime().getTime() / 1000L;
//
//        StringBuilder claimsSet = new StringBuilder();
//        claimsSet.append("{")
//                .append("\"nbf\"").append(":").append(notBeforeTime)
//                .append("}");
//        String jwtString = JwtTestHelper.encodedPlaintextJwt("{}", claimsSet.toString());
//
//        //When
//        boolean exceptionCaught = false;
//        JwtException exception = null;
//        try {
//            new JwtBuilderFactory().reconstruct(jwtString, Jwt.class);
//        } catch (JwtException e) {
//            exceptionCaught = true;
//            exception = e;
//        }
//
//        //Then
//        assertTrue(exceptionCaught);
//        assertTrue(exception.getCause().getClass().isAssignableFrom(RuntimeException.class));//TODO proper exception
    }

    @Test
    public void shouldStoreNotBeforeTimeAsIntDate() throws IOException {

        //Given
        JwtBuilderFactory jwtBuilderFactory = new JwtBuilderFactory();
        long notBeforeTimeLong = 1373896932;
        Date notBeforeTime = new Date(notBeforeTimeLong * 1000L);

        //When
        JwtClaimsSet claimsSet = jwtBuilderFactory.claims()
                .nbf(notBeforeTime)
                .build();
        String jwtString = jwtBuilderFactory.jwt()
                .claims(claimsSet)
                .build();

        //Then
        String[] jwtParts = jwtString.split("\\.", -1);
        String claimsSetString = new String(Base64url.decode(jwtParts[1]));
        Map<String, Object> jwtMap = JwtTestHelper.jsonToMap(claimsSetString);
        long nbfTime = ((Number) jwtMap.get("nbf")).longValue();
        assertEquals(nbfTime, notBeforeTimeLong);
    }

    @Test
    public void shouldStoreIssuedAtTimeAsIntDate() throws IOException {

        //Given
        JwtBuilderFactory jwtBuilderFactory = new JwtBuilderFactory();
        long issuedAtTimeLong = 1373896932;
        Date issuedAtTime = new Date(issuedAtTimeLong * 1000L);

        //When
        JwtClaimsSet claimsSet = jwtBuilderFactory.claims()
                .iat(issuedAtTime)
                .build();
        String jwtString = jwtBuilderFactory.jwt()
                .claims(claimsSet)
                .build();

        //Then
        String[] jwtParts = jwtString.split("\\.", -1);
        String claimsSetString = new String(Base64url.decode(jwtParts[1]));
        Map<String, Object> jwtMap = JwtTestHelper.jsonToMap(claimsSetString);
        long iatTime = ((Number) jwtMap.get("iat")).longValue();
        assertEquals(iatTime, issuedAtTimeLong);
    }

    @Test
    public void shouldStoreIssuerAsString() throws IOException, URISyntaxException {

        //Given
        JwtBuilderFactory jwtBuilderFactory = new JwtBuilderFactory();
        URI issuer = new URI("urn:ietf:params:oauth:token-type:jwt");

        //When
        JwtClaimsSet claimsSet = jwtBuilderFactory.claims()
                .iss(issuer)
                .build();
        String jwtString = jwtBuilderFactory.jwt()
                .claims(claimsSet)
                .build();

        //Then
        String[] jwtParts = jwtString.split("\\.", -1);
        String claimsSetString = new String(Base64url.decode(jwtParts[1]));
        Map<String, Object> jwtMap = JwtTestHelper.jsonToMap(claimsSetString);
        assertTrue(jwtMap.get("iss") instanceof String);
    }

    @Test
    public void shouldStoreAudienceAsString() throws IOException, URISyntaxException {

        //Given
        JwtBuilderFactory jwtBuilderFactory = new JwtBuilderFactory();
        URI audience = new URI("urn:ietf:params:oauth:token-type:jwt");
        List<String> audienceList = new ArrayList<String>();
        audienceList.add(audience.toString());

        //When
        JwtClaimsSet claimsSet = jwtBuilderFactory.claims()
                .aud(audienceList)
                .build();
        String jwtString = jwtBuilderFactory.jwt()
                .claims(claimsSet)
                .build();

        //Then
        String[] jwtParts = jwtString.split("\\.", -1);
        String claimsSetString = new String(Base64url.decode(jwtParts[1]));
        Map<String, Object> jwtMap = JwtTestHelper.jsonToMap(claimsSetString);
        assertTrue(((List<?>) jwtMap.get("aud")).get(0) instanceof String);
    }

    @Test
    public void shouldStorePrincipalAsString() throws IOException, URISyntaxException {

        //Given
        JwtBuilderFactory jwtBuilderFactory = new JwtBuilderFactory();
        URI principal = new URI("urn:ietf:params:oauth:token-type:jwt");

        //When
        JwtClaimsSet claimsSet = jwtBuilderFactory.claims()
                .prn(principal)
                .build();
        String jwtString = jwtBuilderFactory.jwt()
                .claims(claimsSet)
                .build();

        //Then
        String[] jwtParts = jwtString.split("\\.", -1);
        String claimsSetString = new String(Base64url.decode(jwtParts[1]));
        Map<String, Object> jwtMap = JwtTestHelper.jsonToMap(claimsSetString);
        assertTrue(jwtMap.get("prn") instanceof String);
    }

    @Test
    public void shouldStoreJWTIDAsString() throws IOException, URISyntaxException {

        //Given
        JwtBuilderFactory jwtBuilderFactory = new JwtBuilderFactory();
        String jti = UUID.randomUUID().toString();

        //When
        JwtClaimsSet claimsSet = jwtBuilderFactory.claims()
                .jti(jti)
                .build();
        String jwtString = jwtBuilderFactory.jwt()
                .claims(claimsSet)
                .build();

        //Then
        String[] jwtParts = jwtString.split("\\.", -1);
        String claimsSetString = new String(Base64url.decode(jwtParts[1]));
        Map<String, Object> jwtMap = JwtTestHelper.jsonToMap(claimsSetString);
        assertTrue(jwtMap.get("jti") instanceof String);
    }

    @Test
    public void shouldRejectJwtWithDuplicateHeaderParameterNames() {

        //Given
        StringBuilder header = new StringBuilder();
        header.append("{")
                .append("\"KEY\"").append(":").append("\"VALUE1\"").append(",")
                .append("\"OTHER_KEY\"").append(":").append("\"VALUE2\"").append(",")
                .append("\"KEY\"").append(":").append("\"VALUE1A\"")
              .append("}");
        String jwtString = JwtTestHelper.encodedPlaintextJwt(header.toString(), "{}");


        //When
        boolean exceptionCaught = false;
        try {
            new JwtBuilderFactory().reconstruct(jwtString, Jwt.class);
        } catch (DuplicateMapEntryException e) {
            exceptionCaught = true;
        }

        //Then
        assertTrue(exceptionCaught);
    }

    @Test
    public void shouldStoreTypeAsString() throws IOException, URISyntaxException {

        //Given
        JwtBuilderFactory jwtBuilderFactory = new JwtBuilderFactory();

        //When
        String jwtString = jwtBuilderFactory.jwt()
                .build();

        //Then
        String[] jwtParts = jwtString.split("\\.", -1);
        String headerString = new String(Base64url.decode(jwtParts[0]));
        Map<String, Object> jwtMap = JwtTestHelper.jsonToMap(headerString);
        assertTrue(jwtMap.get("typ") instanceof String);
    }

    @Test
    public void shouldSetJwtTypeTojwt() throws IOException {

        //Given
        JwtBuilderFactory jwtBuilderFactory = new JwtBuilderFactory();

        //When
        String jwtString = jwtBuilderFactory.jwt().build();

        //Then
        String[] jwtParts = jwtString.split("\\.", -1);
        String headerString = new String(Base64url.decode(jwtParts[0]));
        Map<String, Object> jwtMap = JwtTestHelper.jsonToMap(headerString);
        assertTrue(jwtMap.get("typ") instanceof String);
        assertEquals(JwtType.jwtType((String) jwtMap.get("typ")), JwtType.JWT);
    }

    @Test
    public void shouldSetJwsTypeTojwt() throws IOException {

        //Given
        JwtBuilderFactory jwtBuilderFactory = new JwtBuilderFactory();

        //When
        String jwtString = jwtBuilderFactory.jws(KeysHelper.getRSAPrivateKey())
                .headers().alg(JwsAlgorithm.HS256).done()
                .build();

        //Then
        String[] jwtParts = jwtString.split("\\.", -1);
        String headerString = new String(Base64url.decode(jwtParts[0]));
        Map<String, Object> jwtMap = JwtTestHelper.jsonToMap(headerString);
        assertTrue(jwtMap.get("typ") instanceof String);
        assertEquals(JwtType.jwtType((String) jwtMap.get("typ")), JwtType.JWT);
    }

    @Test
    public void shouldSetJweTypeTojwt() throws IOException {

        //Given
        JwtBuilderFactory jwtBuilderFactory = new JwtBuilderFactory();

        //When
        String jwtString = jwtBuilderFactory.jwe(KeysHelper.getRSAPublicKey())
                .headers().alg(JweAlgorithm.RSAES_PKCS1_V1_5).enc(EncryptionMethod.A128CBC_HS256).done()
                .build();

        //Then
        String[] jwtParts = jwtString.split("\\.", -1);
        String headerString = new String(Base64url.decode(jwtParts[0]));
        Map<String, Object> jwtMap = JwtTestHelper.jsonToMap(headerString);
        assertTrue(jwtMap.get("typ") instanceof String);
        assertEquals(JwtType.jwtType((String) jwtMap.get("typ")), JwtType.JWT);
    }

    @Test
    public void shouldSetNestedSignedJweTypeTojwe() throws IOException {

        //Given
        JwtBuilderFactory jwtBuilderFactory = new JwtBuilderFactory();

        //When
        String jwtString = jwtBuilderFactory.jwe(KeysHelper.getRSAPublicKey())
                .headers().alg(JweAlgorithm.RSAES_PKCS1_V1_5).enc(EncryptionMethod.A128CBC_HS256).done()
                .sign(KeysHelper.getRSAPrivateKey(), JwsAlgorithm.HS256)
                .build();

        //Then
        String[] jwtParts = jwtString.split("\\.", -1);
        String headerString = new String(Base64url.decode(jwtParts[0]));
        Map<String, Object> jwtMap = JwtTestHelper.jsonToMap(headerString);
        assertTrue(jwtMap.get("typ") instanceof String);
        assertEquals(JwtType.jwtType((String) jwtMap.get("typ")), JwtType.JWE);
    }

    @Test
    public void shouldRejectJwsWithDuplicateHeaderParameterNames() throws NoSuchAlgorithmException, SignatureException,
            InvalidKeyException {

        //Given
        StringBuilder header = new StringBuilder();
        header.append("{")
                .append("\"KEY\"").append(":").append("\"VALUE1\"").append(",")
                .append("\"OTHER_KEY\"").append(":").append("\"VALUE2\"").append(",")
                .append("\"KEY\"").append(":").append("\"VALUE1A\"")
                .append("}");
        String headerString = header.toString();
        String jwtString = JwtTestHelper.encodedSignedJwt(headerString, "{}",
                JwtTestHelper.signWithRSA(headerString + "." + "{}", "SHA256withRSA", KeysHelper.getRSAPrivateKey()));


        //When
        boolean exceptionCaught = false;
        try {
            new JwtBuilderFactory().reconstruct(jwtString, Jwt.class);
        } catch (DuplicateMapEntryException e) {
            exceptionCaught = true;
        }

        //Then
        assertTrue(exceptionCaught);
    }
}
