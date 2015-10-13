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
 * Copyright 2011-2015 ForgeRock AS.
 */

package org.forgerock.json.schema.validator.validators;

import static org.forgerock.json.schema.validator.Constants.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.schema.validator.Constants;
import org.forgerock.json.schema.validator.exceptions.SchemaException;

/**
 * Validator is the abstract base class of all typed validator.
 * <p>
 * Each validator that responsible validate one certain type of object MUST extend this class.
 */
public abstract class Validator implements SimpleValidator<Object> {

    /** Whether the schema represented by this validator is required. */
    protected boolean required = false;
    private JsonPointer pointer;

    /**
     * Default ctor.
     *
     * @param schema the schema holding the reference to this validator
     * @param jsonPointer the JSON pointer locating where this validator was defined in the schema.
     */
    public Validator(Map<String, Object> schema, List<String> jsonPointer) {
        Object o = schema.get(REQUIRED);
        if (o instanceof Boolean) {
            required = ((Boolean) o);
        } else if (o instanceof String) {
            required = Boolean.parseBoolean((String) o);
        }
        if (jsonPointer != null) {
            this.pointer = new JsonPointer(jsonPointer.toArray(new String[jsonPointer.size()]));
        }
    }

    /**
     * Gets the valid JSONPath of the node or the given property.
     * <p/>
     * <p/>
     * Combines the two parameter and generates a valid JSONPath with dot–notation.
     * Simple type: $
     * Array type: $[0]
     * Object type: $.store.book[0].title
     *
     * @param at       JSONPath of the current node. If it's null then the value is {@code /}
     * @param property Property name of the child node.
     * @return JSONPath expressions uses the dot–notation
     *         Example: $.store.book[0].title
     */
    protected final JsonPointer getPath(JsonPointer at, String property) {
        JsonPointer path = null == at ? new JsonPointer() : at;
        if (null == property) {
            return path;
        } else {
            return path.child(property);
        }
    }

    /**
     * Returns a new {@link List} with the additional elements appended at the end.
     *
     * @param list the list to copy
     * @param newElems the new elements to append
     * @return a new {@link List} with the additional elements appended at the end.
     */
    protected List<String> newList(List<String> list, String... newElems) {
        final List<String> results = new ArrayList<>(list.size() + newElems.length);
        results.addAll(list);
        for (String elem : newElems) {
            if (elem == null) {
                throw new IllegalArgumentException();
            }
            results.add(elem);
        }
        return results;
    }

    /**
     * Returns the JSON pointer locating where the validator was defined in the schema.
     *
     * @return the pointer
     */
    public JsonPointer getJsonPointer() {
        return pointer;
    }

    /**
     * Returns whether the schema represented by this validator is required.
     *
     * @return true if the schema represented by this validator is required, false otherwise
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Resolves schema references for this validator.
     *
     * @see Constants#REF
     */
    public void resolveSchemaReferences() {
        final List<Validator> validators = new ArrayList<>();
        collectAllValidators(validators);

        final List<ReferenceTypeValidator> references = new ArrayList<>();
        final Map<JsonPointer, Validator> jsonPointers = new HashMap<>();
        for (Validator validator : validators) {
            if (validator.getJsonPointer() != null) {
                jsonPointers.put(validator.getJsonPointer(), validator);
            }
            if (validator instanceof ReferenceTypeValidator) {
                ReferenceTypeValidator val = (ReferenceTypeValidator) validator;
                if (val.getReference() != null) {
                    references.add(val);
                }
            }
        }

        for (ReferenceTypeValidator v : references) {
            String ref = v.getReference();
            if (ref.startsWith("#")) {
                ref = ref.substring(1);
            }
            final JsonPointer path = new JsonPointer(ref);
            final Validator referencedValidator = jsonPointers.get(path);
            if (referencedValidator == null) {
                throw new SchemaException(new JsonValue(null, path), "Could not dereference JSON reference " + ref);
            }
            v.setReferencedValidator(referencedValidator);
        }
    }

    /**
     * Collects all the sub-validators held in this validator and aggregates them in the passed in Collection.
     *
     * @param results where collected validators are aggregated
     */
    protected void collectAllValidators(Collection<Validator> results) {
        results.add(this);
    }

    /**
     * Collects all the sub-validators held in this validator and aggregates them in the passed in Collection.
     *
     * @param results where collected validators are aggregated
     * @param col the sub-validators for which to collect other sub-validators
     */
    protected static void collectAllValidators(Collection<Validator> results,
            final Collection<? extends Validator> col) {
        if (col != null) {
            for (Validator v : col) {
                v.collectAllValidators(results);
            }
        }
    }

    /**
     * Collects all the sub-validators held in this validator and aggregates them in the passed in Collection.
     *
     * @param results where collected validators are aggregated
     * @param map the sub-validators for which to collect other sub-validators
     */
    protected static void collectAllValidators(final Collection<Validator> results,
            final Map<?, ? extends Validator> map) {
        if (map != null) {
            collectAllValidators(results, map.values());
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + getJsonPointer();
    }
}
