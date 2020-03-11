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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.audit.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class JsonValueUtilsTest {

    @Test
    public void extractValueAsStringCanExtractNull() {
        JsonValue jsonValue = json(object(field("field", null)));
        String value = JsonValueUtils.extractValueAsString(jsonValue, "field");
        assertThat(value).isNull();
    }

    @Test
    public void testJsonValueExpander() {
        //given
        Map<String, Object> flatObject = new HashMap<String, Object>();
        flatObject.put("/string", "string");
        flatObject.put("/number", 0);
        flatObject.put("/boolean", true);
        flatObject.put("/array/0", "value1");
        flatObject.put("/array/1", "value2");
        flatObject.put("/arrayObjects/0/string", "string");
        flatObject.put("/arrayObjects/0/number", 0);
        flatObject.put("/arrayObjects/0/boolean", true);
        flatObject.put("/arrayObjects/0/array/0", "value1");
        flatObject.put("/arrayObjects/0/array/1", "value2");
        flatObject.put("/nestedObject/string", "string");
        flatObject.put("/nestedObject/number", 0);
        flatObject.put("/nestedObject/boolean", true);
        flatObject.put("/nestedObject/array/0", "value1");
        flatObject.put("/nestedObject/array/1", "value2");

        //when
        JsonValue jsonValue = JsonValueUtils.expand(flatObject);
        //then

        assertThat(jsonValue != null);
        assertThat(!jsonValue.isNull());
        assertThat(jsonValue.get(new JsonPointer("/string")).asString().equals("string"));
        assertThat(jsonValue.get(new JsonPointer("/number")).asNumber().equals(0));
        assertThat(jsonValue.get(new JsonPointer("/boolean")).asBoolean().equals(true));
        assertThat(jsonValue.get(new JsonPointer("/array/0")).asString().equals("value1"));
        assertThat(jsonValue.get(new JsonPointer("/array/1")).asString().equals("value2"));
        assertThat(jsonValue.get(new JsonPointer("/arrayObjects/0/string")).asString().equals("string"));
        assertThat(jsonValue.get(new JsonPointer("/arrayObjects/0/number")).asNumber().equals(0));
        assertThat(jsonValue.get(new JsonPointer("/arrayObjects/0/boolean")).asBoolean().equals(true));
        assertThat(jsonValue.get(new JsonPointer("/arrayObjects/0/array/0")).asString().equals("value1"));
        assertThat(jsonValue.get(new JsonPointer("/arrayObjects/0/array/1")).asString().equals("value2"));
        assertThat(jsonValue.get(new JsonPointer("/nestedObject/string")).asString().equals("string"));
        assertThat(jsonValue.get(new JsonPointer("/nestedObject/number")).asNumber().equals(0));
        assertThat(jsonValue.get(new JsonPointer("/nestedObject/boolean")).asBoolean().equals(true));
        assertThat(jsonValue.get(new JsonPointer("/nestedObject/array/0")).asString().equals("value1"));
        assertThat(jsonValue.get(new JsonPointer("/nestedObject/array/1")).asString().equals("value2"));
    }

    @Test
    public void testFlatteningJsonValue() {
        // given
        JsonValue jsonValue =
                json(
                        object(
                                field("string", "string"),
                                field("number", 0),
                                field("boolean", true),
                                field("array", array("value1", "value2")),
                                field("arrayObjects", array(object(
                                        field("string", "string"),
                                        field("number", 0),
                                        field("boolean", true),
                                        field("array", array("value1", "value2"))
                                ))),
                                field("nestedObject", object(
                                        field("string", "string"),
                                        field("number", 0),
                                        field("boolean", true),
                                        field("array", array("value1", "value2"))
                                ))
                        )
                );

        // when
        Map<String, Object> flatObject = JsonValueUtils.flatten(jsonValue);

        // then
        assertThat(flatObject != null);
        assertThat(flatObject.get("/string").equals("string"));
        assertThat(flatObject.get("/number").equals(0));
        assertThat(flatObject.get("/boolean").equals(true));
        assertThat(flatObject.get("/array/0").equals("value1"));
        assertThat(flatObject.get("/array/1").equals("value2"));
        assertThat(flatObject.get("/arrayObjects/0/string").equals("string"));
        assertThat(flatObject.get("/arrayObjects/0/number").equals(0));
        assertThat(flatObject.get("/arrayObjects/0/boolean").equals(true));
        assertThat(flatObject.get("/arrayObjects/0/array/0").equals("value1"));
        assertThat(flatObject.get("/arrayObjects/0/array/1").equals("value2"));
        assertThat(flatObject.get("/nestedObject/string").equals("string"));
        assertThat(flatObject.get("/nestedObject/number").equals(0));
        assertThat(flatObject.get("/nestedObject/boolean").equals(true));
        assertThat(flatObject.get("/nestedObject/array/0").equals("value1"));
        assertThat(flatObject.get("/nestedObject/array/1").equals("value2"));
    }
}
