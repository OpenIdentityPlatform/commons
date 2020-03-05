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

import java.util.Collection;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.annotation.NoClass;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;

/**
 * A Type-Resolver that can use standard Jackson {@link JsonTypeInfo.As.PROPERTY} @{link JsonTypeInfo}, but failing
 * to find a matching (or registered {@link NamedType} subtype) will look for a "class" attribute that provides
 * the Class name to deserialize into.  Useful in extending polymorphic deserialization from {@link NamedType}s
 * when you can't know the full gamut of possible subclasses at build time.
 *
 * @since 0.9.0
 */
class ClassNameFallbackPropertyTypeResolver extends StdTypeResolverBuilder {

    /**
     * Build a {@link TypeDeserializer} for the provided base type.  Use the {@link TypeIdResolver} that
     * is configured, but create a TypeDeserializer that will fallback to using a class attribute to
     * specify the Class name if normal, configured type resolution fails.
     *
     * @param config the {@link DeserializationConfig}
     * @param baseType the base type to which to deserialize
     * @param subtypes any subtypes that have been defined
     * @return a TypeDeserializer that will attempt type-resolution-by-class-name using a class attribute if
     *      resolution-by-NamedType fails
     */
    @Override
    public TypeDeserializer buildTypeDeserializer(
            final DeserializationConfig config, final JavaType baseType, final Collection<NamedType> subtypes) {

        // important to get the normal TypeIdResolver!
        final TypeIdResolver idRes = this.idResolver(config, baseType,new PolymorphicTypeValidator.Base() {private static final long serialVersionUID = 1L;}, subtypes, false, true);
        
        JavaType defaultImpl;

        if (_defaultImpl == null) {
            defaultImpl = null;
        } else {
            if ((_defaultImpl == Void.class)
                     || (_defaultImpl == NoClass.class)) {
                defaultImpl = config.getTypeFactory().constructType(_defaultImpl);
            } else {
                defaultImpl = config.getTypeFactory()
                    .constructSpecializedType(baseType, _defaultImpl);
            }
        }
        
        return new ClassNameFallbackPropertyTypeDeserializer(baseType, idRes, _typeProperty, _typeIdVisible, defaultImpl);
    }
}


