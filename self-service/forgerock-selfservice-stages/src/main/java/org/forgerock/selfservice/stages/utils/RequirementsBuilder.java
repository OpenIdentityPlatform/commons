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

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.selfservice.core.ServiceUtils.emptyJson;

import org.forgerock.json.JsonValue;
import org.forgerock.util.Reject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class to assist with the building of requirements.
 *
 * @since 0.1.0
 */
public final class RequirementsBuilder {

    private final static String JSON_SCHEMA = "http://json-schema.org/draft-04/schema#";

    private final JsonValue jsonValue;
    private final List<String> requiredProperties;
    private final Map<String, Map<String, String>> properties;

    private RequirementsBuilder(String description) {
        Reject.ifNull(description);
        requiredProperties = new ArrayList<>();
        properties = new HashMap<>();

        jsonValue = json(
                object(
                        field("$schema", JSON_SCHEMA),
                        field("type", "object"),
                        field("description", description),
                        field("required", requiredProperties),
                        field("properties", properties)));
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
     * Builds a new json object representing the defined requirements.
     *
     * @return the json requirements
     */
    public JsonValue build() {
        Reject.ifTrue(properties.isEmpty(), "There must be at least one property");
        return jsonValue;
    }

    /**
     * Creates a new builder instance.
     *
     * @param description
     *         the overall requirements description
     *
     * @return a new builder instance
     */
    public static RequirementsBuilder newInstance(String description) {
        return new RequirementsBuilder(description);
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
