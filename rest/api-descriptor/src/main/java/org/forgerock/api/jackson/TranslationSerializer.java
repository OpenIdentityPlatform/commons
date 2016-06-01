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

package org.forgerock.api.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.forgerock.api.util.Translator;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

/**
 * Custom Jackson serializer for i18n translation of descriptions.
 */
public class TranslationSerializer extends StdSerializer<String> {

    private Translator translator;

    /**
     * Default constructor that needs a Translator instance.
     * @param translator Translator instance
     */
    public TranslationSerializer(Translator translator) {
        super(String.class);
        this.translator = translator;
    }

    /**
     * Serializer for translating the description fields.
     * @param description The description to be translated
     * @param jgen Json Generator
     * @param provider SerializerProvider
     * @throws IOException Thrown if there is any issues with the translation
     */
    @Override
    public void serialize(String description, JsonGenerator jgen, SerializerProvider provider)
            throws IOException {
        jgen.writeString(translator.translate(description));
    }
}