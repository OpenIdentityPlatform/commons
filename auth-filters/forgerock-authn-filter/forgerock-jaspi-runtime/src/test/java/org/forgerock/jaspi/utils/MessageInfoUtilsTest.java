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

package org.forgerock.jaspi.utils;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.message.MessageInfo;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class MessageInfoUtilsTest {

    private MessageInfoUtils messageInfoUtils;

    @BeforeMethod
    public void setUp() {
        messageInfoUtils = new MessageInfoUtils();
    }

    @Test
    public void shouldGetNewMap() {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        String mapKey = "MAP_KEY";
        Map<String, Object> messageInfoMap = new HashMap<String, Object>();

        given(messageInfo.getMap()).willReturn(messageInfoMap);

        //When
        Map<String, Object> map = messageInfoUtils.getMap(messageInfo, mapKey);

        //Then
        assertNotNull(map);
    }

    @Test
    public void shouldGetExistingMap() {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        String mapKey = "MAP_KEY";
        Map<String, Object> messageInfoMap = new HashMap<String, Object>();
        Map<String, Object> expectedMap = new HashMap<String, Object>();

        given(messageInfo.getMap()).willReturn(messageInfoMap);
        messageInfoMap.put(mapKey, expectedMap);

        //When
        Map<String, Object> map = messageInfoUtils.getMap(messageInfo, mapKey);

        //Then
        assertEquals(map, expectedMap);
    }

    @Test
    public void should() {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        String mapKey = "MAP_KEY";
        Map<String, Object> messageInfoMap = new HashMap<String, Object>();

        given(messageInfo.getMap()).willReturn(messageInfoMap);

        //When
        messageInfoUtils.addMap(messageInfo, mapKey);

        //Then

    }
}
