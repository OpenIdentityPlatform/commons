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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.jaspi.runtime;

import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;

public class HttpServletMessageInfoTest {

    @Test
    public void shouldGetRequestMessage() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpServletMessageInfo messageInfo = new HttpServletMessageInfo(request, response);

        //When
        HttpServletRequest returnedRequest = messageInfo.getRequestMessage();

        //Then
        assertNotNull(returnedRequest);
        assertNotEquals(returnedRequest, request);
    }

    @Test
    public void shouldGetResponseMessage() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpServletMessageInfo messageInfo = new HttpServletMessageInfo(request, response);

        //When
        HttpServletResponse returnedResponse = messageInfo.getResponseMessage();

        //Then
        assertNotNull(returnedResponse);
        assertNotEquals(returnedResponse, response);
    }

    @Test
    public void shouldGetMapWithNoMapGivenInConstructor() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpServletMessageInfo messageInfo = new HttpServletMessageInfo(request, response);

        //When
        Map<String, Object> returnedMap = messageInfo.getMap();

        //Then
        assertNotNull(returnedMap);
        assertEquals(returnedMap.size(), 0);
    }

    @Test
    public void shouldSetRequestMessage() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletRequest request2 = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpServletMessageInfo messageInfo = new HttpServletMessageInfo(request, response);

        //When
        messageInfo.setRequestMessage(request2);

        //Then
        HttpServletRequest returnedRequest = messageInfo.getRequestMessage();
        assertNotNull(returnedRequest);
        assertNotEquals(returnedRequest, request2);
    }

    @Test
    public void shouldSetResponseMessage() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpServletResponse response2 = mock(HttpServletResponse.class);
        HttpServletMessageInfo messageInfo = new HttpServletMessageInfo(request, response);

        //When
        messageInfo.setResponseMessage(response2);

        //Then
        HttpServletResponse returnedResponse = messageInfo.getResponseMessage();
        assertNotNull(returnedResponse);
        assertNotEquals(returnedResponse, response2);
    }

    @Test
    public void shouldGetMapWithMapGivenInConstructor() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Map<String, Object> map = new HashMap<String, Object>();
        HttpServletMessageInfo messageInfo = new HttpServletMessageInfo(request, response, map);

        //When
        Map<String, Object> returnedMap = messageInfo.getMap();

        //Then
        assertEquals(returnedMap, map);
    }
}
