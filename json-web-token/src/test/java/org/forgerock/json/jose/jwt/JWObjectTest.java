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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.json.jose.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.forgerock.json.jose.exceptions.JwtRuntimeException;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class JWObjectTest {

    @Test
    public void shouldNotPutNullValues() {

        //Given
        JWObject jwObject = new JWObject() { };

        //When
        jwObject.put("KEY1", null);

        //Then
        assertFalse(jwObject.isDefined("KEY1"));
    }

    @Test
    public void shouldRemoveExistingValueWhenPuttingNewValueIsNull() {

        //Given
        JWObject jwObject = new JWObject() { };
        jwObject.put("KEY1", "VALUE1");

        //When
        jwObject.put("KEY1", null);

        //Then
        assertFalse(jwObject.isDefined("KEY1"));
    }

    @Test
    public void shouldGetValue() {

        //Given
        JWObject jwObject = new JWObject() { };
        jwObject.put("KEY", "VALUE");

        //When
        String value = jwObject.get("KEY").asString();

        //Then
        assertEquals(value, "VALUE");
    }

    @Test
    public void shouldCheckIfIsDefined() {

        //Given
        JWObject jwObject = new JWObject() { };

        //When
        jwObject.put("KEY", "VALUE");

        //Then
        assertTrue(jwObject.isDefined("KEY"));
    }

    @Test
    public void shouldCheckValueIsOfType() {

        //Given
        JWObject jwObject = new JWObject() { };

        //When
        jwObject.checkValueIsOfType("VALUE", String.class);
        jwObject.checkValueIsOfType(new Date(), Date.class);
        jwObject.checkValueIsOfType(1234L, Long.class);

        //Then
    }

    @Test (expectedExceptions = JwtRuntimeException.class)
    public void shouldThrowJwtRuntimeExceptionWhenValueIsOfWrongType() {

        //Given
        JWObject jwObject = new JWObject() { };

        //When
        jwObject.checkValueIsOfType("VALUE", String.class);
        jwObject.checkValueIsOfType(true, Date.class);
        jwObject.checkValueIsOfType(1234L, Long.class);

        //Then
    }

    @Test
    public void shouldCheckListValuesAreOfType() {

        //Given
        JWObject jwObject = new JWObject() { };
        List<String> strings = new ArrayList<>();
        strings.add("STRING");
        List<Date> dates = new ArrayList<>();
        List<Long> longs = new ArrayList<>();
        longs.add(1234L);

        //When
        jwObject.checkListValuesAreOfType(strings, String.class);
        jwObject.checkListValuesAreOfType(dates, Date.class);
        jwObject.checkListValuesAreOfType(longs, Long.class);

        //Then
    }

    @Test
    public void shouldCheckListValuesAreOfTypeOkIfWrongListTypeIsEmpty() {

        //Given
        JWObject jwObject = new JWObject() { };
        List<String> strings = new ArrayList<>();
        strings.add("STRING");
        List<Integer> dates = new ArrayList<>();
        List<Long> longs = new ArrayList<>();
        longs.add(1234L);

        //When
        jwObject.checkListValuesAreOfType(strings, String.class);
        jwObject.checkListValuesAreOfType(dates, Date.class);
        jwObject.checkListValuesAreOfType(longs, Long.class);

        //Then
    }

    @Test (expectedExceptions = JwtRuntimeException.class)
    public void shouldThrowJwtRuntimeExceptionWhenListValuesAreOfWrongType() {

        //Given
        JWObject jwObject = new JWObject() { };
        List<String> strings = new ArrayList<>();
        strings.add("STRING");
        List<Integer> dates = new ArrayList<>();
        dates.add(1234);
        List<Long> longs = new ArrayList<>();
        longs.add(1234L);

        //When
        jwObject.checkListValuesAreOfType(strings, String.class);
        jwObject.checkListValuesAreOfType(dates, Date.class);
        jwObject.checkListValuesAreOfType(longs, Long.class);

        //Then
    }

    @Test
    public void shouldCheckIfIsValueOfType() {

        //Given
        JWObject jwObject = new JWObject() { };

        //When
        boolean isString = jwObject.isValueOfType("VALUE", String.class);
        boolean isDate = jwObject.isValueOfType(true, Date.class);
        boolean isLong = jwObject.isValueOfType(1234L, Long.class);

        //Then
        assertTrue(isString);
        assertFalse(isDate);
        assertTrue(isLong);
    }

    @Test
    public void shouldToString() {

        //Given
        JWObject jwObject = new JWObject() { };
        jwObject.put("KEY1", "VALUE1");
        jwObject.put("KEY2", "VALUE2");

        //When
        String jwString = jwObject.toString();

        //Then
        assertThat(jwString).contains("\"KEY2\": \"VALUE2\"", "\"KEY1\": \"VALUE1\"");
    }
}
