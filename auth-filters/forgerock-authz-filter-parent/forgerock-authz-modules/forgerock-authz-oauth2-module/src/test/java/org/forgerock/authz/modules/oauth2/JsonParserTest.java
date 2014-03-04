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

package org.forgerock.authz.modules.oauth2;

import org.forgerock.json.fluent.JsonValue;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.assertEquals;

public class JsonParserTest {

    private JsonParser jsonParser;

    @BeforeMethod
    public void setUp() {
        jsonParser = new JsonParser();
    }

    @Test
    public void shouldParseJson() throws IOException {

        //Given
        String jsonString = "{\"a\":[]}";
        JsonValue expectedJsonValue = JsonValue.json(JsonValue.object(JsonValue.field("a", JsonValue.array())));

        //When
        final JsonValue jsonValue = jsonParser.parse(jsonString);

        //Then
        assertEquals(jsonValue.toString(), expectedJsonValue.toString());
    }
}
