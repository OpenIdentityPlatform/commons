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

import java.io.IOException;

import org.forgerock.api.models.Paths;
import org.forgerock.util.Reject;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

/** Jackson Module that adds a serializer modifier for {@link Paths}. */
public final class PathsModule extends SimpleModule {
    /** Default constructor. */
    public PathsModule() {
        setSerializerModifier(new PathsModifier());
    }

    private static final class PathsSerializer extends JsonSerializer<Paths> {
        private final JsonSerializer<Object> defaultSerializer;

        private PathsSerializer(JsonSerializer<Object> defaultSerializer) {
            this.defaultSerializer = Reject.checkNotNull(defaultSerializer);
        }

        @Override
        public void serialize(Paths value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            defaultSerializer.serialize(value, gen, serializers);
        }

        @Override
        public boolean isEmpty(SerializerProvider provider, Paths value) {
            return value.getNames().isEmpty();
        }
    }

    private static final class PathsModifier extends BeanSerializerModifier {
        @Override
        public JsonSerializer<?> modifySerializer(
                SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {
            if (beanDesc.getBeanClass() == Paths.class) {
                // reuse the provided serializer by injecting it
                return new PathsSerializer((JsonSerializer<Object>) serializer);
            }
            return serializer;
        }
    }
}
