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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.api.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.JsonValue.set;

import org.forgerock.json.JsonValue;
import org.forgerock.util.i18n.LocalizableString;
import org.testng.annotations.Test;

public class TranslateJsonSchemaTest {

    @Test
    public void testTransformString() {
        TranslateJsonSchema translateFunction = new TranslateJsonSchema(this.getClass().getClassLoader());
        JsonValue jsonValue = json("i18n:here");
        JsonValue result = jsonValue.as(translateFunction);
        assertThat(result.getObject() instanceof LocalizableString);
        assertThat(!(result.getObject() instanceof String));
    }

    @Test
    public void testTransformNumber() {
        TranslateJsonSchema translateFunction = new TranslateJsonSchema(null);
        JsonValue jsonValue = json(123);
        JsonValue result = jsonValue.as(translateFunction);
        assertThat(result.getObject() instanceof Number);
    }

    @Test
    public void testTransformBoolean() {
        TranslateJsonSchema translateFunction = new TranslateJsonSchema(null);
        JsonValue jsonValue = json(false);
        JsonValue result = jsonValue.as(translateFunction);
        assertThat(result.getObject() instanceof Boolean);
    }

    @Test
    public void testTransformNull() {
        TranslateJsonSchema translateFunction = new TranslateJsonSchema(null);
        JsonValue jsonValue = json(null);
        JsonValue result = jsonValue.as(translateFunction);
        assertThat(result.getObject() == null);
    }

    @Test
    public void testTransformObjectTransformsChildren() {
        TranslateJsonSchema translateFunction = new TranslateJsonSchema(null);
        JsonValue jsonValue = json(object(field("first", "i18n:here"), field("second", "i18n:there")));
        JsonValue result = jsonValue.as(translateFunction);
        assertThat(result.get("first").getObject()).isInstanceOf(LocalizableString.class);
        assertThat(result.get("second").getObject()).isInstanceOf(LocalizableString.class);
    }

    @Test
    public void testTransformObjectTransformsChildrenWithNulls() {
        TranslateJsonSchema translateFunction = new TranslateJsonSchema(null);
        JsonValue jsonValue = json(object(field("first", "i18n:here"), field("second", null), null));
        JsonValue result = jsonValue.as(translateFunction);
        assertThat(result.get("first").getObject()).isInstanceOf(LocalizableString.class);
        assertThat(result.get("second").getObject()).isNull();
    }

    @Test
    public void testTransformArrayTransformsChildren() {
        TranslateJsonSchema translateFunction = new TranslateJsonSchema(null);
        JsonValue jsonValue = json(array("i18n:here", "i18n:there"));
        JsonValue result = jsonValue.as(translateFunction);
        assertThat(result.get(0).getObject()).isInstanceOf(LocalizableString.class);
        assertThat(result.get(1).getObject()).isInstanceOf(LocalizableString.class);
    }

    @Test
    public void testTransformSetTransformsChildren() {
        TranslateJsonSchema translateFunction = new TranslateJsonSchema(null);
        JsonValue jsonValue = json(set("i18n:here", "i18n:there"));
        JsonValue result = jsonValue.as(translateFunction);
        assertThat(result.get(0).getObject()).isInstanceOf(LocalizableString.class);
        assertThat(result.get(1).getObject()).isInstanceOf(LocalizableString.class);
    }


}
