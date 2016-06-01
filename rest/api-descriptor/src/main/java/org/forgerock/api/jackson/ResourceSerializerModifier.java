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

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;

/**
 * Custom Jackson serializer modifier that instantiates the custom ResourceSerializer.
 */
public class ResourceSerializerModifier extends BeanSerializerModifier {

    /**
     * Default constructor.
     */
    public ResourceSerializerModifier() {
        super();
    }

    /**
     * Method called by BeanSerializerFactory after constructing default bean serializer instance with properties
     * collected and ordered earlier. Implementations can modify or replace given serializer
     * and return serializer to use.
     * Note that although initial serializer being passed is of type BeanSerializer,
     * modifiers may return serializers of other types; and this is why implementations must check
     * for type before casting.
     * @param config The config
     * @param beanDesc The bean description
     * @param serializer The serializer
     * @return JsonSerializer
     */
    @Override
    public JsonSerializer<?> modifySerializer(
            SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {

        return (serializer instanceof BeanSerializerBase)
                ? new ResourceSerializer((BeanSerializerBase) serializer)
                : serializer;
    }
}