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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.factories.ObjectVisitor;
import com.fasterxml.jackson.module.jsonSchema.factories.ObjectVisitorDecorator;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import com.fasterxml.jackson.module.jsonSchema.factories.VisitorContext;
import com.fasterxml.jackson.module.jsonSchema.factories.WrapperFactory;
import org.forgerock.api.annotations.EnumTitle;
import org.forgerock.api.annotations.PropertyOrder;
import org.forgerock.api.annotations.PropertyPolicies;
import org.forgerock.api.enums.WritePolicy;

/**
 * A {@code SchemaFactoryWrapper} that adds the extra CREST schema attributes once the Jackson schema generation has
 * been completed.
 */
public class CrestPropertyDetailsSchemaFactoryWrapper extends SchemaFactoryWrapper {

    private static final WrapperFactory WRAPPER_FACTORY = new WrapperFactory() {
        @Override
        public SchemaFactoryWrapper getWrapper(SerializerProvider provider) {
            SchemaFactoryWrapper wrapper = new CrestPropertyDetailsSchemaFactoryWrapper();
            wrapper.setProvider(provider);
            return wrapper;
        }

        @Override
        public SchemaFactoryWrapper getWrapper(SerializerProvider provider, VisitorContext rvc) {
            SchemaFactoryWrapper wrapper = new CrestPropertyDetailsSchemaFactoryWrapper();
            wrapper.setProvider(provider);
            wrapper.setVisitorContext(rvc);
            return wrapper;
        }
    };

    /**
     * Create a new wrapper. Sets the {@link CrestJsonSchemaFactory} in the parent class's {@code schemaProvider} so
     * that all of the schema objects that are created support the appropriate API Descriptor extensions.
     */
    public CrestPropertyDetailsSchemaFactoryWrapper() {
        super(WRAPPER_FACTORY);
        this.schemaProvider = new CrestJsonSchemaFactory();
    }

    @Override
    public JsonObjectFormatVisitor expectObjectFormat(JavaType convertedType) {
        return new ObjectVisitorDecorator((ObjectVisitor) super.expectObjectFormat(convertedType)) {
            @Override
            public JsonSchema getSchema() {
                return super.getSchema();
            }

            @Override
            public void optionalProperty(BeanProperty writer) throws JsonMappingException {
                super.optionalProperty(writer);
                JsonSchema schema = schemaFor(writer);
                addFieldPolicies(writer, schema);
                addPropertyOrder(writer, schema);
                addEnumTitles(writer, schema);
            }

            private void addEnumTitles(BeanProperty writer, JsonSchema schema) {
                JavaType type = writer.getType();
                if (type.isEnumType()) {
                    Class<? extends Enum> enumClass = type.getRawClass().asSubclass(Enum.class);
                    Enum[] enumConstants = enumClass.getEnumConstants();
                    List<String> titles = new ArrayList<>(enumConstants.length);
                    boolean foundTitle = false;
                    for (Enum<?> value : enumConstants) {
                        try {
                            EnumTitle title = enumClass.getField(value.name()).getAnnotation(EnumTitle.class);
                            if (title != null) {
                                titles.add(title.value());
                                foundTitle = true;
                            } else {
                                titles.add(null);
                            }
                        } catch (NoSuchFieldException e) {
                            throw new IllegalStateException("Enum doesn't have its own value as a field", e);
                        }
                    }
                    if (foundTitle) {
                        ((EnumSchema) schema).setEnumTitles(titles);
                    }
                }
            }

            private void addPropertyOrder(BeanProperty writer, JsonSchema schema) {
                PropertyOrder order = annotationFor(writer, PropertyOrder.class);
                if (order != null) {
                    ((OrderedFieldSchema) schema).setPropertyOrder(order.value());
                }
            }

            private void addFieldPolicies(BeanProperty writer, JsonSchema schema) {
                PropertyPolicies policies = annotationFor(writer, PropertyPolicies.class);
                if (policies != null) {
                    if (policies.write() != WritePolicy.WRITABLE) {
                        CrestReadWritePoliciesSchema schemaPolicies = (CrestReadWritePoliciesSchema) schema;
                        schemaPolicies.setWritePolicy(policies.write());
                        schemaPolicies.setErrorOnWritePolicyFailure(policies.errorOnWritePolicyFailure());
                    }
                }
            }

            private <T extends Annotation> T annotationFor(BeanProperty writer, Class<T> type) {
                return writer.getMember().getAnnotation(type);
            }

            private JsonSchema schemaFor(BeanProperty writer) {
                return getSchema().asObjectSchema().getProperties().get(writer.getName());
            }
        };
    }
}
