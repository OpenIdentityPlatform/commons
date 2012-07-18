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
 * Copyright © 2012 ForgeRock AS. All rights reserved.
 */

package org.forgerock.json.resource;

import static org.forgerock.json.resource.ContextAttribute.ID;
import static org.forgerock.json.resource.ContextAttribute.PARENT;
import static org.forgerock.json.resource.ContextAttribute.TYPE;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.util.Factory;

/**
 * The context associated with a request currently being processed by a JSON
 * resource provider. A request context can be used to query state information
 * about the request. Implementations may provide additional information,
 * time-stamp information, HTTP headers, etc using {@code ContextAttributes}.
 */
public final class Context {
    /*
     * TODO: do we need to synchronize access to the underlying attribute map?
     * Doing so would preclude us from sharing the map when converting to and
     * from a JSON value, since the JSON value will not synchronize access. One
     * possibility is to use a CHM.
     */

    /**
     * Creates a new context whose {@link ContextAttribute#TYPE TYPE} is
     * {@code "root"}, an {@link ContextAttribute#ID ID} automatically generated
     * using {@code UUID.randomUUID()}, and a {@code null}
     * {@link ContextAttribute#PARENT PARENT}.
     *
     * @return A new root context with an automatically generated ID.
     */
    public static Context newRootContext() {
        return newRootContext(UUID.randomUUID().toString());
    }

    /**
     * Creates a new context whose {@link ContextAttribute#TYPE TYPE} is
     * {@code "root"}, the provided {@link ContextAttribute#ID ID}, and a
     * {@code null} {@link ContextAttribute#PARENT PARENT}.
     *
     * @param id
     *            The context ID.
     * @return A new root context with the provided ID.
     */
    public static Context newRootContext(final String id) {
        return new Context("root", id, null);
    }

    /**
     * Creates a new context using the provided JSON representation. The JSON
     * representation of a context is typically generated using the method
     * {@link #toJsonValue()}.
     * <p>
     * The returned context will share the same underlying {@code Map} as the
     * provided JSON value, so subsequent changes to either object will be
     * reflected in the other.
     * <p>
     * <b>NOTE:</b> if the provided JSON content is not a valid context then
     * subsequent attempts to access context attributes may fail unpredictably,
     * usually with a {@code ClassCastException}.
     *
     * @param json
     *            The JSON representation of the context to be created.
     * @return A new context whose content is the provided JSON representation.
     */
    public static Context valueOf(final JsonValue json) {
        return new Context(json.asMap());
    }

    private final Map<String, Object> attributes;

    private Context(final Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    private Context(final String type, final String id, final Context parent) {
        this.attributes = new LinkedHashMap<String, Object>(3);

        TYPE.set(this, type);
        ID.set(this, id);
        PARENT.set(this, parent);
    }

    /**
     * Returns the {@link ContextAttribute#ID ID} associated with this context.
     *
     * @return The context ID.
     */
    public String getId() {
        return ID.get(this);
    }

    /**
     * Returns the {@link ContextAttribute#PARENT PARENT} of this context.
     *
     * @return The parent of this context, or {@code null} if this context is a
     *         root context.
     */
    public Context getParent() {
        return PARENT.get(this);
    }

    /**
     * Returns the {@link ContextAttribute#TYPE TYPE} of this context.
     *
     * @return The context type.
     */
    public String getType() {
        return TYPE.get(this);
    }

    /**
     * Returns {@code true} if this context is a root context.
     *
     * @return {@code true} if this context is a root context.
     */
    public boolean isRootContext() {
        return getParent() == null;
    }

    /**
     * Creates a new context having the provided {@link ContextAttribute#TYPE
     * TYPE}, an {@link ContextAttribute#ID ID} automatically generated using
     * {@code UUID.randomUUID()}, and this context as its
     * {@link ContextAttribute#PARENT PARENT}.
     *
     * @param type
     *            The context type.
     * @return A new sub-context with the provided type and an automatically
     *         generated ID.
     */
    public Context newSubContext(final String type) {
        return newSubContext(type, UUID.randomUUID().toString());
    }

    /**
     * Creates a new context having the provided {@link ContextAttribute#TYPE
     * TYPE} and {@link ContextAttribute#ID ID}, and this context as its
     * {@link ContextAttribute#PARENT PARENT}.
     *
     * @param type
     *            The context type.
     * @param id
     *            The context ID.
     * @return A new sub-context with the provided type and ID.
     */
    public Context newSubContext(final String type, final String id) {
        return new Context(type, id, this);
    }

    /**
     * Returns a JSON value whose content is the set of attributes contained
     * within this context.
     * <p>
     * The returned JSON value will share the same underlying {@code Map} as
     * this context, so subsequent changes to either object will be reflected in
     * the other.
     * <p>
     * The returned JSON value may be converted back into a context using the
     * factory method {@link #valueOf(JsonValue)}.
     *
     * @return A JSON value whose content is the set of attributes contained
     *         within this context.
     */
    public JsonValue toJsonValue() {
        return new JsonValue(attributes);
    }

    /**
     * Returns the {@code String} representation of this context. The string
     * representation will be computed as the JSON representation of the
     * attributes contained within this context.
     *
     * @return The {@code String} representation of this context.
     */
    @Override
    public String toString() {
        return new JsonValue(attributes).toString();
    }

    /**
     * Queries this context for the provided attribute, searching the parents if
     * requested, and optionally initializing the attribute if it was not found.
     *
     * @param <T>
     *            The type of value contained in the context attribute.
     * @param attribute
     *            The attribute to be returned.
     * @param checkParent
     *            {@code true} if parent contexts should be searched.
     * @param initialValueFactory
     *            The factory which should be used for lazy initialization, or
     *            {@code null} if the attribute should not be lazily
     *            initialized.
     * @return The attribute value, or {@code null} if it was not found.
     */
    <T> Object getAttribute(final ContextAttribute<T> attribute, final boolean checkParent,
            final Factory<T> initialValueFactory) {
        final String id = attribute.id();

        // Fast-path: assume that the attribute is present and non-null in this context.
        final Object value = attributes.get(id);
        if (value != null) {
            return value;
        }

        // Attribute may be present, but null.
        if (attributes.containsKey(id)) {
            return null;
        }

        // Search parents if requested.
        if (checkParent) {
            for (Context parent = getParent(); parent != null; parent = parent.getParent()) {
                // Fast-path: assume that the attribute is not present.
                if (parent.attributes.containsKey(id)) {
                    return parent.attributes.get(id);
                }
            }
        }

        // The attribute was not present in this context or in the parents
        // (if searched), so initialize it if requested.
        if (initialValueFactory != null) {
            final Object newValue = initialValueFactory.newInstance();
            attributes.put(id, newValue);
            return newValue;
        }

        // Not present.
        return null;
    }

    /**
     * Removes an attribute from this context.
     *
     * @param attribute
     *            The attribute to be removed.
     * @return The removed attribute value or {@code null} if it was not found.
     */
    Object removeAttribute(final ContextAttribute<?> attribute) {
        return attributes.remove(attribute.id());
    }

    /**
     * Sets an attribute in this context.
     *
     * @param attribute
     *            The attribute to be set.
     * @param newValue
     *            The new attribute value.
     * @return The old attribute value or {@code null} if it did not exist
     *         beforehand.
     */
    Object setAttribute(final ContextAttribute<?> attribute, final Object newValue) {
        return attributes.put(attribute.id(), newValue);
    }
}
