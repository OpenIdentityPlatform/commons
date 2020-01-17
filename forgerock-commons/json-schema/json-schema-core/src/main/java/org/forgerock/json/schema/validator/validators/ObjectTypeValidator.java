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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2011-2016 ForgeRock AS.
 */

package org.forgerock.json.schema.validator.validators;

import static org.forgerock.json.schema.validator.Constants.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.schema.validator.CollectErrorsHandler;
import org.forgerock.json.schema.validator.ErrorHandler;
import org.forgerock.json.schema.validator.ObjectValidatorFactory;
import org.forgerock.json.schema.validator.exceptions.ValidationException;

/**
 * ObjectTypeValidator applies all the constraints of a <code>object</code> type.
 * <p/>
 * Sample JSON Schema:
 * <code>
 * {
 * "type"        : "object"
 * }
 * </code>
 *
 * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.1">type</a>
 */
public class ObjectTypeValidator extends Validator {

    /**
     * This attribute is an object with property definitions that define the
     * valid values of instance object property values.  When the instance
     * value is an object, the property values of the instance object MUST
     * conform to the property definitions in this object.  In this object,
     * each property definition's value MUST be a schema, and the property's
     * name MUST be the name of the instance property that it defines.  The
     * instance property value MUST be valid according to the schema from
     * the property definition.  Properties are considered unordered, the
     * order of the instance properties MAY be in any order.
     */
    private final Map<String, PropertyValidatorBag> propertyValidators;
    /**
     * An object instance is valid against this keyword if its property set
     * contains all elements in this keyword's array value.
     */
    private final Set<String> requiredPropertyNames = new HashSet<>();
    /**
     * This attribute is an object that defines the requirements of a
     * property on an instance object.  If an object instance has a property
     * with the same name as a property in this attribute's object, then the
     * instance must be valid against the attribute's property value
     * (hereafter referred to as the "dependency value").
     * <p/>
     * The dependency value can take one of two forms:
     * <p/>
     * Simple Dependency  If the dependency value is a string, then the
     * instance object MUST have a property with the same name as the
     * dependency value.  If the dependency value is an array of strings,
     * then the instance object MUST have a property with the same name
     * as each string in the dependency value's array.
     * <p/>
     * Schema Dependency  If the dependency value is a schema, then the
     * instance object MUST be valid against the schema.
     */
    private Map<String, Validator> dependenciesValidators;
    private Map<String, Set<String>> dependencyValues;
    /**
     * This attribute is an object that defines the schema for a set of
     * property names of an object instance.  The name of each property of
     * this attribute's object is a regular expression pattern in the ECMA
     * 262 format, while the value is a schema.  If the pattern
     * matches the name of a property on the instance object, the value of
     * the instance's property MUST be valid against the pattern name's
     * schema value.
     */
    private Map<Pattern, Validator> patternPropertyValidators;
    /**
     * This attribute defines a schema for all properties that are not
     * explicitly defined in an object type definition.  If specified, the
     * value MUST be a schema or a boolean.  If false is provided, no
     * additional properties are allowed beyond the properties defined in
     * the schema.  The default value is an empty schema which allows any
     * value for additional properties.
     */
    private boolean allowAdditionalProperties = true;
    private Validator additionalPropertyValidator = null;
    /**
     * An instance validates successfully against this keyword if it validates
     * successfully against exactly one schema defined by this keyword's value.
     *
     * @see <a
     *      href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-5.5.5">oneOf</a>
     */
    private List<Validator> oneOfValidators;
    /**
     * This keyword plays no role in validation per se. Its role is to provide a
     * standardized location for schema authors to inline JSON Schemas into a more
     * general schema.
     *
     * @see <a
     *      href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-5.5.7">definitions</a>
     */
    private Map<String, Validator> definitionValidators;

    /**
     * Default ctor.
     *
     * @param schema the schema holding the reference to this validator
     * @param jsonPointer the JSON pointer locating where this validator was defined in the schema.
     */
    @SuppressWarnings("unchecked")
    public ObjectTypeValidator(Map<String, Object> schema, List<String> jsonPointer) {
        super(schema, jsonPointer);
        Map<String, Object> objectProperties = (Map<String, Object>) schema.get(PROPERTIES);
        if (null != objectProperties) {
            propertyValidators = new HashMap<>(objectProperties.size());

            for (Map.Entry<String, Object> entry : objectProperties.entrySet()) {
                final List<String> newPointer = newList(jsonPointer, PROPERTIES, entry.getKey());
                Validator validator = ObjectValidatorFactory.getTypeValidator(
                        (Map<String, Object>) entry.getValue(), newPointer);
                propertyValidators.put(entry.getKey(), new PropertyValidatorBag(validator));
            }
        } else {
            propertyValidators = Collections.emptyMap();
        }
        for (Map.Entry<String, Object> e : schema.entrySet()) {
            if (ADDITIONALPROPERTIES.equals(e.getKey())) {
                if (e.getValue() instanceof Boolean && !((Boolean) e.getValue())
                    || (e.getValue() instanceof String && e.getValue().equals("false"))) {
                    allowAdditionalProperties = false;
                } else if (e.getValue() != null && e.getValue() instanceof Map) {
                    final List<String> newPointer = newList(jsonPointer, ADDITIONALPROPERTIES, e.getKey());
                    additionalPropertyValidator = ObjectValidatorFactory.getTypeValidator(
                            (Map<String, Object>) e.getValue(), newPointer);
                }
            } else if (PATTERNPROPERTIES.equals(e.getKey())) {
                if (e.getValue() instanceof Map) {
                    Map<String, Object> properties = (Map<String, Object>) e.getValue();
                    patternPropertyValidators = new HashMap<>(properties.size());

                    for (Map.Entry<String, Object> entry : properties.entrySet()) {
                        try {
                            Pattern p = Pattern.compile(entry.getKey());
                            List<String> newPointer = newList(jsonPointer, PATTERNPROPERTIES, entry.getKey());
                            Validator validator = ObjectValidatorFactory.getTypeValidator(
                                    (Map<String, Object>) entry.getValue(), newPointer);
                            patternPropertyValidators.put(p, validator);
                        } catch (PatternSyntaxException pse) {
                            //LOG.error("Failed to apply pattern on " + at + ":
                            // Invalid RE syntax [" + pattern + "]", pse);
                        }
                    }
                }
            } else if (DEPENDENCIES.equals(e.getKey())) {
                if (e.getValue() instanceof Map) {
                    for (Map.Entry<String, Object> d : ((Map<String, Object>) e.getValue()).entrySet()) {
                        PropertyValidatorBag validator = propertyValidators.get(d.getKey());
                        if (null != validator) {
                            if (d.getValue() instanceof Map) {
                                List<String> newPointer = newList(jsonPointer, DEPENDENCIES, d.getKey());
                                validator.setDependencyValidator(ObjectValidatorFactory.getTypeValidator(
                                        (Map<String, Object>) d.getValue(), newPointer));
                            } else {
                                validator.setDependencyValue(d.getValue());
                            }
                        } else {
                            if (null == dependencyValues) {
                                dependencyValues = new HashMap<>(1);
                            }
                            if (d.getValue() instanceof Map) {
                                // @TODO: Validate additional properties
                                //validator.setDependencyValidator(ObjectValidatorFactory.getTypeValidator(
                                // (Map<String, Object>) d.getValue()));
                            } else if (d.getValue() instanceof String) {
                                dependencyValues.put(d.getKey(), Collections.singleton((String) d.getValue()));
                            } else if (d.getValue() instanceof Collection) {
                                dependencyValues.put(d.getKey(), new HashSet<>((Collection<String>) d.getValue()));
                            }
                        }
                    }
                }
            } else if (REQUIRED.equals(e.getKey())) {
                if (e.getValue() instanceof List) {
                    for (Object o : (List<Object>) e.getValue()) {
                        if (o instanceof String) {
                            requiredPropertyNames.add((String) o);
                        }
                    }
                }
            } else if (ONEOF.equals(e.getKey())) {
                if (e.getValue() instanceof List) {
                    final List<Object> l = (List<Object>) e.getValue();
                    oneOfValidators = new ArrayList<>(l.size());
                    for (int i = 0; i < l.size(); i++) {
                        Object obj = l.get(i);
                        if (obj instanceof Map) {
                            final List<String> newPointer = newList(jsonPointer, ONEOF, Integer.toString(i));
                            oneOfValidators.add(
                                    ObjectValidatorFactory.getTypeValidator((Map<String, Object>) obj, newPointer));
                        }
                    }
                }
            } else if (DEFINITIONS.equals(e.getKey())) {
                if (e.getValue() instanceof Map) {
                    Map<String, Object> definitions = (Map<String, Object>) e.getValue();
                    definitionValidators = new HashMap<>(definitions.size());
                    for (Map.Entry<String, Object> entry : definitions.entrySet()) {
                        if (entry.getValue() instanceof Map) {
                            final List<String> newPointer = newList(jsonPointer, DEFINITIONS, entry.getKey());
                            Validator validator = ObjectValidatorFactory.getTypeValidator(
                                    (Map<String, Object>) entry.getValue(), newPointer);
                            definitionValidators.put(entry.getKey(), validator);
                        }
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(Object value, JsonPointer at, ErrorHandler handler) {
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapValue = (Map<String, Object>) value;

            if (!mapValue.keySet().containsAll(requiredPropertyNames)) {
                Set<String> missingRequiredProperties = new HashSet<>(requiredPropertyNames);
                missingRequiredProperties.removeAll(mapValue.keySet());
                // @TODO: Add exception message: Missing required property names
                handler.error(new ValidationException(
                        "Missing required property names: " + missingRequiredProperties, getPath(at, null)));
            }

            Set<String> additionalPropertyNames = new HashSet<>(mapValue.keySet());
            Set<String> instancePropertyKeySet = Collections.unmodifiableSet(mapValue.keySet());

            for (Map.Entry<String, PropertyValidatorBag> schemaProperty : propertyValidators.entrySet()) {
                final String propertyName = schemaProperty.getKey();
                final PropertyValidatorBag propertyValue = schemaProperty.getValue();
                //null == entry.getValue() can not used for Null type
                // so first need to check the map contains the key before getting the potential null value
                if (mapValue.containsKey(propertyName)) {
                    final Object entryValue = mapValue.get(propertyName);
                    propertyValue.validate(entryValue, instancePropertyKeySet, getPath(at, propertyName), handler);
                    additionalPropertyNames.remove(propertyName);
                } else if (propertyValue.isRequired()) {
                    // @TODO: Add exception message: Required property value is null
                    handler.error(new ValidationException(
                            "Required property value is null", getPath(at, propertyName)));
                }
            }

            for (Iterator<String> iter = additionalPropertyNames.iterator(); iter.hasNext();) {
                String additionalPropertyName = iter.next();
                Object propertyValue = mapValue.get(additionalPropertyName);

                if (null != additionalPropertyValidator) {
                    additionalPropertyValidator.validate(propertyValue, getPath(at, additionalPropertyName), handler);
                }

                // @TODO: Implement Dependency check
                Validator dependencyPropertyValidator = null != dependenciesValidators
                        ? dependenciesValidators.get(additionalPropertyName) : null;
                if (null != dependencyPropertyValidator) {
                    dependencyPropertyValidator.validate(propertyValue, getPath(at, additionalPropertyName), handler);
                }

                if (null != patternPropertyValidators) {
                    for (Map.Entry<Pattern, Validator> v : patternPropertyValidators.entrySet()) {
                        Matcher matcher = v.getKey().matcher(additionalPropertyName);
                        // Quoting "3.3 Regular expressions":
                        // http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-3.3
                        // "Finally, implementations MUST NOT consider that regular expressions
                        // are anchored, neither at the beginning nor at the end.  This means,
                        // for instance, that "es" matches "expression"."
                        if (matcher.find()) {
                            iter.remove();
                            v.getValue().validate(propertyValue, getPath(at, additionalPropertyName), handler);
                            break;
                        }
                    }
                }
            }

            if (null != this.oneOfValidators) {
                boolean oneIsvalid = false;
                for (Validator validator : this.oneOfValidators) {
                    CollectErrorsHandler collectErrorsHandler = new CollectErrorsHandler();
                    validator.validate(mapValue, getPath(at, null), collectErrorsHandler);
                    if (!collectErrorsHandler.hasError()) {
                        oneIsvalid = true;
                        break;
                    }
                }
                if (oneIsvalid) {
                    additionalPropertyNames.remove(mapValue.keySet().iterator().next());
                } else {
                    // @TODO: Add exception message
                    handler.error(new ValidationException(
                            "Error: Expected one of the validators to validate value", getPath(at, null)));
                }
            }

            if (!allowAdditionalProperties && !additionalPropertyNames.isEmpty()) {
                // @TODO: Add exception message: Additional Properties not allowed
                handler.error(new ValidationException(
                        "Error: Additional Properties not allowed: " + additionalPropertyNames, getPath(at, null)));
            }
        } else if (null != value) {
            handler.error(new ValidationException(ERROR_MSG_TYPE_MISMATCH, getPath(at, null)));
        } else if (required) {
            handler.error(new ValidationException(ERROR_MSG_REQUIRED_PROPERTY, getPath(at, null)));
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void collectAllValidators(Collection<Validator> results) {
        results.add(this);
        final ObjectTypeValidator val = this;
        for (PropertyValidatorBag v : val.propertyValidators.values()) {
            v.collectAllValidators(results);
        }
        collectAllValidators(results, val.patternPropertyValidators);
        if (val.additionalPropertyValidator != null) {
            val.additionalPropertyValidator.collectAllValidators(results);
        }
        collectAllValidators(results, val.oneOfValidators);
        collectAllValidators(results, val.definitionValidators);
    }

    private static final class PropertyValidatorBag implements SimpleValidator<Object> {

        private final Validator propertyValidator;
        private Validator dependencyValidator = null;
        private Set<String> requiredProperties = null;

        private PropertyValidatorBag(Validator propertyValidator) {
            this.propertyValidator = propertyValidator;
        }

        private void setDependencyValidator(Validator dependencyValidator) {
            this.dependencyValidator = dependencyValidator;
        }

        @SuppressWarnings("unchecked")
        private void setDependencyValue(Object dependencyValue) {
            if (dependencyValue instanceof String) {
                requiredProperties = Collections.singleton((String) dependencyValue);
            } else if (dependencyValue instanceof Collection) {
                requiredProperties = new HashSet<>((Collection<String>) dependencyValue);
            }
        }

        private boolean isRequired() {
            return propertyValidator.isRequired();
        }

        private void collectAllValidators(Collection<Validator> results) {
            if (this.propertyValidator != null) {
                this.propertyValidator.collectAllValidators(results);
            }
            if (this.dependencyValidator != null) {
                this.dependencyValidator.collectAllValidators(results);
            }
        }

        @Override
        public void validate(Object value, JsonPointer at, ErrorHandler handler) {
            propertyValidator.validate(value, at, handler);
            if (null != dependencyValidator) {
                dependencyValidator.validate(value, at, handler);
            }
        }

        public void validate(Object value, Set<String> propertyKeySet, JsonPointer at, ErrorHandler handler) {
            if (null != requiredProperties && !propertyKeySet.containsAll(requiredProperties)) {
                handler.error(new ValidationException("Dependency ERROR: Missing properties", at));
            }
            validate(value, at, handler);
        }
    }

}
