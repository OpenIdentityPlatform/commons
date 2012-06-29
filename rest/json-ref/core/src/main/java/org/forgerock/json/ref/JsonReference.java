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
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.json.ref;

// Java SE
import java.net.URI;
import java.util.HashMap;

// JSON Fluent
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;

/**
 * Represents a JSON Reference value.
 *
 * @author Paul C. Bryan
 */
public class JsonReference {

    /** The URI of the value being referenced. */
    private URI uri;

    /**
     * Constructs an empty JSON Reference object.
     */
    public JsonReference() {
        // empty: bare, hollow, stark, unfulfilled, unoccupied, vacant
    }

    /**
     * Returns {@code true} if the specified JSON value contains a valid {@code $ref}
     * JSON object structure.
     *
     * Note: This method does not suppress transformers in the specified value. Consequently,
     * this method can return {@code false} if members are transformed, for example if a
     * {@link JsonReferenceTransformer} transforms the value as it is being inspected.
     *
     * @param value the value to test for a JSON Reference.
     * @return {@code true} if the value is a {@code $ref} JSON Reference.
     */
    public static boolean isJsonReference(JsonValue value) {
        boolean result = false;
        if (value.isDefined("$ref")) { // avoid transformer endless loops
            JsonValue ref = value.get("$ref");
            result = ref.isString();
        }
        return result;
    }

    /**
     * Returns the URI of the value being referenced.
     */
    public URI getURI() {
        return uri;
    }

    /**
     * Sets the URI of the value being referenced.
     *
     * @param uri the URI of the value being referenced.
     * @return this object.
     */
    public JsonReference setURI(URI uri) {
        this.uri = uri;
        return this;
    }

    /**
     * Initializes this object from a {@code $ref} JSON object value.
     *
     * @param value a JSON value containing a {@code $ref} member.
     * @return this object.
     * @throws JsonValueException if the specified value is malformed.
     * @throws NullPointerException if {@code value} is {@code null}.
     */
    public JsonReference fromJsonValue(JsonValue value) throws JsonValueException {
        this.uri = value.get("$ref").required().asURI();
        return this;
    }

    /**
     * Returns this object as a {@code $ref} JSON object value.
     */
    public JsonValue toJsonValue() {
        HashMap<String, Object> result = new HashMap<String, Object>();
        result.put("$ref", uri != null ? uri.toString() : null);
        return new JsonValue(result);
    }
}
