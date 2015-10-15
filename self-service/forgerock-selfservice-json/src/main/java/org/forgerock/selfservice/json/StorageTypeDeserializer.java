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
package org.forgerock.selfservice.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.forgerock.selfservice.core.StorageType;

import java.io.IOException;

/**
 * Jackson Deserializer for {@link StorageType}.
 *
 * @since 0.2.0
 */
class StorageTypeDeserializer extends JsonDeserializer<StorageType> {

    @Override
    public StorageType deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        StorageType type = StorageType.valueOf(jsonParser.getValueAsString().toUpperCase());

        if (type != null) {
            return type;
        }

        throw new JsonMappingException("Invalid value for type 'StorageType'");
    }

}
