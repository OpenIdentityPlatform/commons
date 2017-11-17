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
package org.forgerock.selfservice.core.config;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.impl.AsPropertyTypeDeserializer;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * A {@link AsPropertyTypeDeserializer} that will use a "class" attribute if the normal TypeDeserializer fails
 * to locate the property attribute or if a NamedType has not been registered for that type.
 *
 * @since 0.9.0
 */
class ClassNameFallbackPropertyTypeDeserializer extends AsPropertyTypeDeserializer {
    public ClassNameFallbackPropertyTypeDeserializer(JavaType bt, TypeIdResolver idRes, String typePropertyName,
            boolean typeIdVisible, JavaType defaultImpl) {
        super(bt, idRes, typePropertyName, typeIdVisible, defaultImpl);
    }

    public ClassNameFallbackPropertyTypeDeserializer(AsPropertyTypeDeserializer src, BeanProperty property) {
        super(src, property);
    }

    @Override
    public TypeDeserializer forProperty(final BeanProperty prop) {
        return (prop == _property) ? this : new ClassNameFallbackPropertyTypeDeserializer(this, prop);
    }

    /**
     * Custom deserialization method to first try the built-in {@link AsPropertyTypeDeserializer} approach, but
     * attempt to deserialize using a class attribute if the built-in approach fails.
     *
     * {@inheritDoc}
     */
    @Override
    protected Object _deserializeTypedUsingDefaultImpl(JsonParser jsonParser, DeserializationContext context,
            TokenBuffer tokenBuffer) throws IOException {
        try {
            return super._deserializeTypedUsingDefaultImpl(jsonParser, context, tokenBuffer);
        } catch (JsonMappingException e) {
            // fallback
            if (tokenBuffer != null) {
                // first reset the TokenBuffer so we can parse it again
                tokenBuffer.writeEndObject();
                jsonParser = tokenBuffer.asParser(jsonParser);
                jsonParser.nextToken();

                JsonNode node = jsonParser.readValueAsTree();
                try {
                    // look for class attribute and attempt to construct a type from it
                    JavaType type = context.getTypeFactory().constructFromCanonical(node.get("class").textValue());
                    // ensure the type we found is assignable to the base type requested by deserialization;
                    // typically this is the interface or base class
                    if (_baseType != null && _baseType.getClass() == type.getClass()) {
                        type = _baseType.forcedNarrowBy(type.getRawClass());
                    }
                    // find an appropriate deserializer for this type and deserialize
                    JsonDeserializer<Object> deser = context.findContextualValueDeserializer(type, _property);
                    if (deser != null) {
                        // create a new parser to re-parse this json node-tree
                        JsonParser newParser = new TreeTraversingParser(node, jsonParser.getCodec());
                        if (newParser.getCurrentToken() == null) {
                            newParser.nextToken();
                        }
                        return deser.deserialize(newParser, context);
                    }
                } catch (Exception ex) {
                    throw new JsonMappingException("Unable to load class for " + node.toString(), ex);
                }
            }

            // throw the original exception if the classname fallback did not work
            throw e;
        }
    }
}
