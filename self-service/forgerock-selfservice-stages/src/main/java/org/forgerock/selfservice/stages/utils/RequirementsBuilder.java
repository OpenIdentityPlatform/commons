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
        requiredProperties = new ArrayList<>();
        properties = new HashMap<>();

        jsonValue = json(
                object(
                        field("$schema", JSON_SCHEMA),
                        field("type", "object"),
                        field("description", description),
                        field("required", requiredProperties),
                        field("properties", properties)
                ));
    }

    public RequirementsBuilder addRequireProperty(String name, String description) {
        requiredProperties.add(name);
        addProperty(name, description);
        return this;
    }

    public RequirementsBuilder addProperty(String name, String description) {
        Map<String, String> entry = new HashMap<>();
        entry.put("description", description);
        entry.put("type", "string");
        properties.put(name, entry);
        return this;
    }

    public JsonValue build() {
        Reject.ifTrue(properties.isEmpty(), "There must be at least one property");
        return jsonValue;
    }

    public static RequirementsBuilder newInstance(String description) {
        return new RequirementsBuilder(description);
    }

    public static JsonValue newEmptyRequirements() {
        return emptyJson();
    }

}
