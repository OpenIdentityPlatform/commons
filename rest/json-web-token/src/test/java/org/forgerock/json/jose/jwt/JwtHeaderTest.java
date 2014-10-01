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
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.JwsHeader;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class JwtHeaderTest {

    @Test
    public void shouldSetAlgorithm() {

        //Given
        JwtHeader header = new JwsHeader();

        //When
        header.setAlgorithm(JwsAlgorithm.NONE);

        //Then
        assertTrue(header.get("alg").required().isString());
        assertEquals(header.get("alg").asString(), JwsAlgorithm.NONE.toString());
    }

    @Test
    public void shouldGetAlgorithmString() {

        //Given
        JwtHeader header = new JwsHeader();
        header.setParameter("alg", "NONE");

        //When
        String algorithm = header.getAlgorithmString();

        //Then
        assertEquals(algorithm, "NONE");
    }

    @Test
    public void shouldGetJwtType() {

        //Given
        JwtHeader header = new JwsHeader();

        //When
        JwtType jwtType = header.getType();

        //Then
        assertEquals(jwtType, JwtType.JWT);
    }

    @Test
    public void shouldSetHeader() {

        //Given
        JwtHeader header = new JwsHeader();

        //When
        header.setParameter("KEY", "VALUE");

        //Then
        assertTrue(header.isDefined("KEY"));
        assertTrue(header.get("KEY").required().isString());
        assertEquals(header.get("KEY").asString(), "VALUE");
    }

    @Test
    public void shouldSetHeaders() {

        //Given
        JwtHeader header = new JwsHeader();
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("KEY1", "HEADER1");
        headers.put("KEY2", true);
        headers.put("KEY3", 1234L);
        headers.put("KEY4", 1234);

        //When
        header.setParameters(headers);

        //Then
        assertTrue(header.isDefined("KEY1"));
        assertTrue(header.get("KEY1").required().isString());
        assertEquals(header.get("KEY1").asString(), "HEADER1");

        assertTrue(header.isDefined("KEY2"));
        assertTrue(header.get("KEY2").required().isBoolean());
        assertEquals(header.get("KEY2").asBoolean(), (Boolean) true);

        assertTrue(header.isDefined("KEY3"));
        assertTrue(header.get("KEY3").required().isNumber());
        assertEquals(header.get("KEY3").asLong(), (Long) 1234L);

        assertTrue(header.isDefined("KEY4"));
        assertTrue(header.get("KEY4").required().isNumber());
        assertEquals(header.get("KEY4").asInteger(), (Integer) 1234);
    }

    @Test
    public void shouldGetHeader() {

        //Given
        JwtHeader header = new JwsHeader();
        header.put("KEY1", "HEADER1");
        header.put("KEY2", true);
        header.put("KEY3", 1234L);
        header.put("KEY4", 1234);

        //When
        Object key1 = header.getParameter("KEY1");
        Object key2 = header.getParameter("KEY2");
        Object key3 = header.getParameter("KEY3");
        Object key4 = header.getParameter("KEY4");

        //Then
        assertEquals(key1, "HEADER1");
        assertEquals(key2, true);
        assertEquals(key3, 1234L);
        assertEquals(key4, 1234);
    }

    @Test
    public void shouldGetHeaderUsingClass() {

        //Given
        JwtHeader header = new JwsHeader();
        header.put("KEY1", "HEADER1");
        header.put("KEY2", true);
        header.put("KEY3", 1234L);
        header.put("KEY4", 1234);

        //When
        String key1 = header.getParameter("KEY1", String.class);
        boolean key2 = header.getParameter("KEY2", Boolean.class);
        long key3 = header.getParameter("KEY3", Long.class);
        int key4 = header.getParameter("KEY4", Integer.class);

        //Then
        assertEquals(key1, "HEADER1");
        assertEquals(key2, true);
        assertEquals(key3, 1234L);
        assertEquals(key4, 1234);
    }

    @Test
    public void shouldBuildJwtHeaderToJsonString() {

        //Given
        JwtHeader header = new JwsHeader();
        header.setAlgorithm(JwsAlgorithm.NONE);
        header.setParameter("KEY1", "HEADER1");
        header.setParameter("KEY2", true);

        //When
        String jsonString = header.build();

        //Then
        assertThat(jsonString).contains("\"alg\": \"NONE\"", "\"KEY2\": true", "\"KEY1\": \"HEADER1\"",
                "\"typ\": \"jwt\"");
    }

    @Test
    public void shouldCreateJwtHeaderWithMap() {

        //Given
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("typ", "JWT");
        headers.put("alg", JwsAlgorithm.NONE);
        headers.put("KEY1", "HEADER1");
        headers.put("KEY2", true);
        headers.put("KEY3", 1234L);
        headers.put("KEY4", 1234);

        //When
        JwtHeader header = new JwsHeader(headers);

        //Then
        assertTrue(header.isDefined("typ"));
        assertTrue(header.get("typ").required().isString());
        assertEquals(header.get("typ").asString(), "jwt");

        assertTrue(header.get("alg").required().isString());
        assertEquals(header.get("alg").asString(), JwsAlgorithm.NONE.toString());

        assertTrue(header.isDefined("KEY1"));
        assertTrue(header.get("KEY1").required().isString());
        assertEquals(header.get("KEY1").asString(), "HEADER1");

        assertTrue(header.isDefined("KEY2"));
        assertTrue(header.get("KEY2").required().isBoolean());
        assertEquals(header.get("KEY2").asBoolean(), (Boolean) true);

        assertTrue(header.isDefined("KEY3"));
        assertTrue(header.get("KEY3").required().isNumber());
        assertEquals(header.get("KEY3").asLong(), (Long) 1234L);

        assertTrue(header.isDefined("KEY4"));
        assertTrue(header.get("KEY4").required().isNumber());
        assertEquals(header.get("KEY4").asInteger(), (Integer) 1234);
    }
}
