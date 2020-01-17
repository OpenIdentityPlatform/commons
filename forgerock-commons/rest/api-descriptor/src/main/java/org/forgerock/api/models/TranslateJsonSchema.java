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

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import org.forgerock.json.JsonValue;
import org.forgerock.util.Function;
import org.forgerock.util.i18n.LocalizableString;
import org.forgerock.util.promise.NeverThrowsException;

/**
 * Iterates over each JsonValue node in the JsonValue structure and if it's a String marked for translation,
 * It replaces the String with a LocalizableString.
 */
public class TranslateJsonSchema implements Function<JsonValue, JsonValue, NeverThrowsException> {

    private final ClassLoader loader;

    /**
     * Constructor which takes a {@code ClassLoader} where the String is defined.
     * @param loader the {@code ClassLoader} where the translation resources are defined
     */
    public TranslateJsonSchema(ClassLoader loader) {
        this.loader = loader;
    }

    /**
     * Applies the translation to string values by converting them to {@code LocalizableString}.
     * It traverses the JsonValue structure, iteratively applying the function to each item
     * in a collection.
     * @param value A JsonValue.
     * @return a transformed copy of the JsonValue input.
     */
    @Override
    public JsonValue apply(JsonValue value) {
        JsonValue returnValue = value;
        if (value.isCollection()) {
            JsonValue transformedValue = json(array());
            for (JsonValue item : value) {
                transformedValue.add(item.as(this).getObject());
            }
            returnValue = transformedValue;
        } else if (value.isMap()) {
            JsonValue transformedValue = json(object());
            for (String key : value.keys()) {
                transformedValue.put(key, value.get(key).as(this).getObject());
            }
            returnValue = transformedValue;
        } else if (value.isString() && value.asString().startsWith(LocalizableString.TRANSLATION_KEY_PREFIX)) {
            returnValue = json(new LocalizableString(value.asString(), loader));
        }
        return returnValue;
    }
}
