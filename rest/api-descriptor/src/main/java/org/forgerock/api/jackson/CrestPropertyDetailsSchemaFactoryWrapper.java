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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.forgerock.api.jackson.JacksonUtils.OBJECT_MAPPER;
import static org.forgerock.api.util.ValidationUtil.isEmpty;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.forgerock.api.annotations.Default;
import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.EnumTitle;
import org.forgerock.api.annotations.Example;
import org.forgerock.api.annotations.Format;
import org.forgerock.api.annotations.MultipleOf;
import org.forgerock.api.annotations.PropertyOrder;
import org.forgerock.api.annotations.PropertyPolicies;
import org.forgerock.api.annotations.ReadOnly;
import org.forgerock.api.annotations.Title;
import org.forgerock.api.annotations.UniqueItems;
import org.forgerock.api.enums.WritePolicy;
import com.google.common.io.Resources;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.factories.ObjectVisitor;
import com.fasterxml.jackson.module.jsonSchema.factories.ObjectVisitorDecorator;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import com.fasterxml.jackson.module.jsonSchema.factories.VisitorContext;
import com.fasterxml.jackson.module.jsonSchema.factories.WrapperFactory;
import com.fasterxml.jackson.module.jsonSchema.types.ArraySchema;
import com.fasterxml.jackson.module.jsonSchema.types.NumberSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
import com.fasterxml.jackson.module.jsonSchema.types.SimpleTypeSchema;
import com.fasterxml.jackson.module.jsonSchema.types.StringSchema;
import org.forgerock.api.annotations.AdditionalProperties;

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
    private static final String CLASSPATH_RESOURCE = "classpath:";

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
        final ObjectVisitor objectVisitor = (ObjectVisitor) super.expectObjectFormat(convertedType);
        final Class<?> clazz = convertedType.getRawClass();

        // look for type/class-level annotations
        if (schema instanceof SimpleTypeSchema) {
            final Title title = clazz.getAnnotation(Title.class);
            if (title != null && !isEmpty(title.value())) {
                ((SimpleTypeSchema) schema).setTitle(title.value());
            }
        }

        final Description description = clazz.getAnnotation(Description.class);
        if (description != null && !isEmpty(description.value())) {
            schema.setDescription(description.value());
        }

        final Set<String> requiredFieldNames;
        if (schema instanceof RequiredFieldsSchema) {
            requiredFieldNames = Collections.synchronizedSet(new HashSet<String>());
            ((RequiredFieldsSchema) schema).setRequiredFields(requiredFieldNames);
        } else {
            requiredFieldNames = null;
        }

        final Example example = clazz.getAnnotation(Example.class);
        if (schema instanceof WithExampleSchema && example != null && !isEmpty(example.value())) {
            setExample(clazz, example, (WithExampleSchema<?>) schema);
        }

        // look for field/parameter/method-level annotations
        return new ObjectVisitorDecorator(objectVisitor) {
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
                addRequired(writer, schema);
                addStringPattern(writer, schema);
                addStringMinLength(writer, schema);
                addStringMaxLength(writer, schema);
                addArrayMinItems(writer, schema);
                addArrayMaxItems(writer, schema);
                addNumberMaximum(writer, schema);
                addNumberMinimum(writer, schema);
                addNumberExclusiveMinimum(writer, schema);
                addNumberExclusiveMaximum(writer, schema);
                addReadOnly(writer, schema);
                addTitle(writer, schema);
                addDescription(writer, schema);
                addDefault(writer, schema);
                addUniqueItems(writer, schema);
                addMultipleOf(writer, schema);
                addFormat(writer, schema);
                addExample(writer, schema);
                addAdditionalProperties(writer, schema);
            }

            private void addExample(BeanProperty writer, JsonSchema schema) {
                Example annotation = annotationFor(writer, Example.class);
                if (annotation != null) {
                    WithExampleSchema<?> exampleSchema = (WithExampleSchema<?>) schema;
                    setExample(writer.getType().getRawClass(), annotation, exampleSchema);
                }
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
                    CrestReadWritePoliciesSchema schemaPolicies = (CrestReadWritePoliciesSchema) schema;
                    if (policies.write() != WritePolicy.WRITABLE) {
                        schemaPolicies.setWritePolicy(policies.write());
                        schemaPolicies.setErrorOnWritePolicyFailure(policies.errorOnWritePolicyFailure());
                    }
                    schemaPolicies.setReadPolicy(policies.read());
                    schemaPolicies.setReturnOnDemand(policies.returnOnDemand());
                }
            }

            private void addRequired(BeanProperty writer, JsonSchema schema) {
                NotNull notNull = annotationFor(writer, NotNull.class);
                if (notNull != null) {
                    if (requiredFieldNames != null) {
                        requiredFieldNames.add(writer.getName());
                    } else {
                        // NOTE: this condition may never happen, but is here to deal with unknown edge cases
                        schema.setRequired(true);
                    }
                }
            }

            private void addStringPattern(BeanProperty writer, JsonSchema schema) {
                Pattern pattern = annotationFor(writer, Pattern.class);
                if (pattern != null && !isEmpty(pattern.regexp())) {
                    ((StringSchema) schema).setPattern(pattern.regexp());
                }
            }

            private void addStringMinLength(BeanProperty writer, JsonSchema schema) {
                Integer size = getMinSize(writer);
                if (size != null && schema instanceof StringSchema) {
                    ((StringSchema) schema).setMinLength(size);
                }
            }

            private void addStringMaxLength(BeanProperty writer, JsonSchema schema) {
                Integer size = getMaxSize(writer);
                if (size != null && schema instanceof StringSchema) {
                    ((StringSchema) schema).setMaxLength(size);
                }
            }

            private void addArrayMinItems(BeanProperty writer, JsonSchema schema) {
                Integer size = getMinSize(writer);
                if (size != null && schema instanceof ArraySchema) {
                    ((ArraySchema) schema).setMinItems(size);
                }
            }

            private void addArrayMaxItems(BeanProperty writer, JsonSchema schema) {
                Integer size = getMaxSize(writer);
                if (size != null && schema instanceof ArraySchema) {
                    ((ArraySchema) schema).setMaxItems(size);
                }
            }

            private void addNumberMinimum(BeanProperty writer, JsonSchema schema) {
                Min min = annotationFor(writer, Min.class);
                if (min != null) {
                    ((MinimumMaximumSchema) schema).setPropertyMinimum(new BigDecimal(min.value()));
                }

                DecimalMin decimalMin = annotationFor(writer, DecimalMin.class);
                if (decimalMin != null) {
                    ((MinimumMaximumSchema) schema).setPropertyMinimum(new BigDecimal(decimalMin.value()));
                }
            }

            private void addNumberMaximum(BeanProperty writer, JsonSchema schema) {
                Max max = annotationFor(writer, Max.class);
                if (max != null) {
                    ((MinimumMaximumSchema) schema).setPropertyMaximum(new BigDecimal(max.value()));
                }

                DecimalMax decimalMax = annotationFor(writer, DecimalMax.class);
                if (decimalMax != null) {
                    ((MinimumMaximumSchema) schema).setPropertyMaximum(new BigDecimal(decimalMax.value()));
                }
            }

            private void addNumberExclusiveMinimum(BeanProperty writer, JsonSchema schema) {
                DecimalMin decimalMin = annotationFor(writer, DecimalMin.class);
                if (decimalMin != null && !decimalMin.inclusive()) {
                    ((NumberSchema) schema).setExclusiveMinimum(true);
                }
            }

            private void addNumberExclusiveMaximum(BeanProperty writer, JsonSchema schema) {
                DecimalMax decimalMax = annotationFor(writer, DecimalMax.class);
                if (decimalMax != null && !decimalMax.inclusive()) {
                    ((NumberSchema) schema).setExclusiveMaximum(true);
                }
            }

            private void addReadOnly(BeanProperty writer, JsonSchema schema) {
                ReadOnly readOnly = annotationFor(writer, ReadOnly.class);
                if (readOnly != null) {
                    schema.setReadonly(readOnly.value());
                }
            }

            private void addTitle(BeanProperty writer, JsonSchema schema) {
                Title title = annotationFor(writer, Title.class);
                if (title != null && !isEmpty(title.value())) {
                    ((SimpleTypeSchema) schema).setTitle(title.value());
                }
            }

            private void addDescription(BeanProperty writer, JsonSchema schema) {
                Description description = annotationFor(writer, Description.class);
                if (description != null && !isEmpty(description.value())) {
                    schema.setDescription(description.value());
                }
            }

            private void addDefault(BeanProperty writer, JsonSchema schema) {
                Default defaultAnnotation = annotationFor(writer, Default.class);
                if (defaultAnnotation != null && !isEmpty(defaultAnnotation.value())) {
                    ((SimpleTypeSchema) schema).setDefault(defaultAnnotation.value());
                }
            }

            private void addUniqueItems(BeanProperty writer, JsonSchema schema) {
                UniqueItems uniqueItems = annotationFor(writer, UniqueItems.class);
                if (uniqueItems != null) {
                    ((ArraySchema) schema).setUniqueItems(uniqueItems.value());
                }
            }

            private void addMultipleOf(BeanProperty writer, JsonSchema schema) {
                MultipleOf multipleOf = annotationFor(writer, MultipleOf.class);
                if (multipleOf != null) {
                    ((MultipleOfSchema) schema).setMultipleOf(multipleOf.value());
                }
            }

            private void addFormat(BeanProperty writer, JsonSchema schema) {
                if (schema instanceof PropertyFormatSchema) {
                    Format format = annotationFor(writer, Format.class);
                    if (format != null && !isEmpty(format.value())) {
                        ((PropertyFormatSchema) schema).setPropertyFormat(format.value());
                    } else if (writer.getType() instanceof SimpleType) {
                        // automatically assign 'format' to numeric types
                        final Class rawClass = writer.getType().getRawClass();
                        final String formatValue;
                        if (Integer.class.equals(rawClass) || int.class.equals(rawClass)) {
                            formatValue = "int32";
                        } else if (Long.class.equals(rawClass) || long.class.equals(rawClass)) {
                            formatValue = "int64";
                        } else if (Double.class.equals(rawClass) || double.class.equals(rawClass)) {
                            formatValue = "double";
                        } else if (Float.class.equals(rawClass) || float.class.equals(rawClass)) {
                            formatValue = "float";
                        } else {
                            return;
                        }
                        ((PropertyFormatSchema) schema).setPropertyFormat(formatValue);
                    }
                }
            }

            private void addAdditionalProperties(BeanProperty writer, JsonSchema schema) throws JsonMappingException {
                AdditionalProperties additionalProperties = annotationFor(writer, AdditionalProperties.class);
                if (additionalProperties != null && !additionalProperties.value().isInstance(Void.class)) {
                    CrestPropertyDetailsSchemaFactoryWrapper visitor = new CrestPropertyDetailsSchemaFactoryWrapper();
                    OBJECT_MAPPER.acceptJsonFormatVisitor(additionalProperties.value(), visitor);
                    ObjectSchema.SchemaAdditionalProperties schemaAdditionalProperties =
                            new ObjectSchema.SchemaAdditionalProperties(visitor.finalSchema());
                    ((ObjectSchema) schema).setAdditionalProperties(schemaAdditionalProperties);
                }
            }

            private Integer getMaxSize(BeanProperty writer) {
                Size size = writer.getAnnotation(Size.class);
                if (size != null) {
                    int value = size.max();
                    if (value != Integer.MAX_VALUE) {
                        return value;
                    }
                }
                return null;
            }

            private Integer getMinSize(BeanProperty writer) {
                Size size = writer.getAnnotation(Size.class);
                if (size != null) {
                    int value = size.min();
                    if (value != 0) {
                        return value;
                    }
                }
                return null;
            }

            /**
             * Looks for annotations at the field/method/parameter-level of a Java class.
             *
             * @param writer Jackson {@code BeanProperty} representing the object-instance to scan for annotations
             * @param type Annotation class to find
             * @param <T> Annotation type to find
             * @return Annotation or {@code null}
             */
            private <T extends Annotation> T annotationFor(BeanProperty writer, Class<T> type) {
                return writer.getMember().getAnnotation(type);
            }

            private JsonSchema schemaFor(BeanProperty writer) {
                return getSchema().asObjectSchema().getProperties().get(writer.getName());
            }
        };
    }

    private void setExample(Class<?> contextClass, Example annotation, WithExampleSchema<?> exampleSchema) {
        String example = annotation.value();
        if (example.startsWith(CLASSPATH_RESOURCE)) {
            ClassLoader classLoader = contextClass.getClassLoader();
            try {
                String name = example.substring(CLASSPATH_RESOURCE.length()).trim();
                URL resource = classLoader.getResource(name);
                if (resource != null) {
                    example = Resources.toString(resource, UTF_8);
                } else {
                    throw new IllegalStateException("Cannot read resource: " + example);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read resource: " + example, e);
            }
        }
        try {
            exampleSchema.setExample(example);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not parse example value to type of schema", e);
        }
    }
}
