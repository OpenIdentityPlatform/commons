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

package org.forgerock.selfservice.stages.utils;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.selfservice.core.ServiceUtils.emptyJson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.forgerock.json.JsonValue;
import org.forgerock.util.Reject;

/**
 * Helper class to assist with the building of requirements.
 *
 * @since 0.1.0
 */
public final class RequirementsBuilder {

    private final static String JSON_SCHEMA = "http://json-schema.org/draft-04/schema#";

    private enum BuilderType { JSON_SCHEMA, OBJECT, ARRAY }

    private final JsonValue jsonValue;
    private final List<String> requiredProperties;
    private final Map<String, Object> properties;
    private final BuilderType builderType;

    private RequirementsBuilder(BuilderType type, String description) {
        requiredProperties = new ArrayList<>();
        properties = new HashMap<>();
        jsonValue = json(object());
        builderType = type;

        switch (builderType) {
        case JSON_SCHEMA:
            Reject.ifNull(description);
            jsonValue.add("$schema", JSON_SCHEMA);
        case OBJECT:
            jsonValue
                    .add("description", description)
                    .add("type", "object")
                    .add("required", requiredProperties)
                    .add("properties", properties);
            break;
        case ARRAY:
            jsonValue.add("type", "array");
            break;
        default:
            throw new IllegalArgumentException("Unknown type " + builderType);
        }
    }

    /**
     * Add a required property; default type is string.
     *
     * @param name
     *         property name
     * @param description
     *         property description
     *
     * @return this builder
     */
    public RequirementsBuilder addRequireProperty(String name, String description) {
        addRequireProperty(name, "string", description);
        return this;
    }

    /**
     * Add a required property.
     *
     * @param name
     *         property name
     * @param type
     *         property type
     * @param description
     *         property description
     *
     * @return this builder
     */
    public RequirementsBuilder addRequireProperty(String name, String type, String description) {
        Reject.ifNull(name, description);
        requiredProperties.add(name);
        addProperty(name, type, description);
        return this;
    }

    /**
     * Add a property; default type is string.
     *
     * @param name
     *         property name
     * @param description
     *         property description
     *
     * @return this builder
     */
    public RequirementsBuilder addProperty(String name, String description) {
        addProperty(name, "string", description);
        return this;
    }

    /**
     * Add a property.
     *
     * @param name
     *         property name
     * @param type
     *         property type
     * @param description
     *         property description
     *
     * @return this builder
     */
    public RequirementsBuilder addProperty(String name, String type, String description) {
        Reject.ifNull(name, description);
        Map<String, String> entry = new HashMap<>();
        entry.put("description", description);
        entry.put("type", type);
        properties.put(name, entry);
        return this;
    }

    /**
     * Add a required property of type object.
     *
     * @param name
     *         property name
     * @param builder
     *         property value builder
     *
     * @return this builder
     */
    public RequirementsBuilder addRequireProperty(String name, RequirementsBuilder builder) {
        addProperty(name, builder);
        requiredProperties.add(name);
        return this;
    }

    /**
     * Add a property of type object.
     *
     * @param name
     *         property name
     * @param builder
     *         property value builder
     *
     * @return this builder
     */
    public RequirementsBuilder addProperty(String name, RequirementsBuilder builder) {
        Reject.ifNull(name, builder);
        properties.put(name, prepareChildJsonValue(builder));
        return this;
    }

    /**
     * Add a custom Json snippet.
     *
     * @param name
     *         property name
     * @param customJsonValue
     *         JasonValue instance
     *
     * @return this builder
     */
    public RequirementsBuilder addCustomField(String name, JsonValue customJsonValue) {
        jsonValue.add(name, getUnderlyingObject(customJsonValue));
        return this;
    }

    /**
     * Builds a new json object representing the defined requirements.
     *
     * @return the json requirements
     */
    public JsonValue build() {
        if (BuilderType.JSON_SCHEMA == builderType) {
            Reject.ifTrue(properties.isEmpty(), "There must be at least one property");
        }
        return jsonValue;
    }

    /**
     * Creates a new builder instance for the json schema.
     *
     * @param description
     *         the overall requirements description
     *
     * @return a new builder instance
     */
    public static RequirementsBuilder newInstance(String description) {
        return new RequirementsBuilder(BuilderType.JSON_SCHEMA, description);
    }

    /**
     * Creates a new builder instance for object type creation.
     *
     * @param description
     *         the object requirements description
     *
     * @return a new builder instance
     */
    public static RequirementsBuilder newObject(String description) {
        return new RequirementsBuilder(BuilderType.OBJECT, description);
    }

    /**
     * Creates a new builder instance for array type creation.
     *
     * @param builder
     *         for the array item
     *
     * @return a new builder instance
     */
    public static RequirementsBuilder newArray(RequirementsBuilder builder) {
        RequirementsBuilder newBuilder = new RequirementsBuilder(BuilderType.ARRAY, null);
        newBuilder.addArrayItem(builder);
        return newBuilder;
    }

    private void addArrayItem(RequirementsBuilder builder) {
        jsonValue.add("items", prepareChildJsonValue(builder));
    }

    private Object prepareChildJsonValue(RequirementsBuilder builder) {
        JsonValue jsonValue = builder.build();
        removeRequiredPropertiesIfEmpty(jsonValue);
        return getUnderlyingObject(jsonValue);
    }

    private void removeRequiredPropertiesIfEmpty(JsonValue jsonValue) {
        if (jsonValue.get("required") != null) {
            if (jsonValue.get("required").size() == 0) {
                jsonValue.remove("required");
            }
        }
    }

    private Object getUnderlyingObject(JsonValue jsonValue) {
        return jsonValue.getObject();
    }

    /**
     * Creates an empty requirements json object.
     *
     * @return empty requirements json object
     */
    public static JsonValue newEmptyRequirements() {
        return emptyJson();
    }
}
