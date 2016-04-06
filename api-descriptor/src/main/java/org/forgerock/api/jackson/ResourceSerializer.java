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
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.forgerock.api.util.Translator;

import java.io.IOException;

/**
 * Custom Serializer base for custom bean serializers.
 */
public class ResourceSerializer extends StdSerializer<Object> implements ResolvableSerializer {
    private final BeanSerializerBase defaultSerializer;

    /**
     * Copy-constructor that is useful for sub-classes that just
     * want to copy all super-class properties without modifications.
     * @param defaultSerializer The BeanSerializer
     */
    public ResourceSerializer(BeanSerializerBase defaultSerializer) {
        super(Object.class);
        this.defaultSerializer = defaultSerializer;
    }

    /**
     * Serializer for beans that stores the classloader.
     * @param bean The bean to be serialized
     * @param jgen Json Generator
     * @param provider SerializerProvider
     * @throws IOException Thrown if there is any issues with the translation
     */
    @Override
    public void serialize(Object bean, JsonGenerator jgen, SerializerProvider provider)
            throws IOException {

        Translator.setClassLoader(bean.getClass().getClassLoader());
        defaultSerializer.serialize(bean, jgen, provider);
        Translator.removeClassLoader();
    }

    /**
     * Method called after {@link SerializerProvider} has registered
     * the serializer, but before it has returned it to the caller.
     * Called object can then resolve its dependencies to other types,
     * including self-references (direct or indirect).
     *<p>
     * Note that this method does NOT return serializer, since resolution
     * is not allowed to change actual serializer to use.
     *
     * @param serializerProvider Provider that has constructed serializer this method
     *   is called on.
     */
    @Override
    public void resolve(SerializerProvider serializerProvider) throws JsonMappingException {
        defaultSerializer.resolve(serializerProvider);
    }
}