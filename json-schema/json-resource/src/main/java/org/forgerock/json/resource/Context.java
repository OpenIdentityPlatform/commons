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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.forgerock.util.Factory;

/**
 * The context associated with a request currently being processed by a JSON
 * resource provider. A request context can be used to query state information
 * about the request. Implementations may provide additional information,
 * time-stamp information, HTTP headers, etc using {@code ContextAttributes}.
 */
public final class Context {
    // TODO: methods for determining whether or not the request has been
    // cancelled?

    private static final class ValueHolder {
        private final Object value;

        private ValueHolder(final Object value) {
            this.value = value;
        }
    }

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

    private final Map<ContextAttribute<?>, ValueHolder> attributes =
            new LinkedHashMap<ContextAttribute<?>, ValueHolder>(4);

    private Context(final String type, final String id, final Context parent) {
        TYPE.set(this, type);
        ID.set(this, id);
        PARENT.set(this, parent);
    }

    /**
     * Returns a set containing a copy of the attributes associated with this
     * context. Subsequent changes to the set of attributes associated with this
     * context will not be reflected in the returned set.
     * <p>
     * This method may be used in order to serialize this context.
     * <p>
     * <b>NOTE:</b> The returned set of attributes will include the
     * {@link ContextAttribute#TYPE TYPE}, {@link ContextAttribute#ID ID}, and
     * {@link ContextAttribute#PARENT PARENT}, if present.
     *
     * @return A set containing a copy of the attributes associated with this
     *         context.
     */
    public Set<ContextAttribute<?>> getAttributes() {
        synchronized (attributes) {
            return new LinkedHashSet<ContextAttribute<?>>(attributes.keySet());
        }
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
        // Fast-path: assume that the attribute is present in this context.
        synchronized (attributes) {
            final ValueHolder vh = attributes.get(attribute);
            if (vh != null) {
                return vh.value;
            }
        }

        // Search parents if requested.
        if (checkParent) {
            for (Context parent = getParent(); parent != null; parent = parent.getParent()) {
                synchronized (parent.attributes) {
                    final ValueHolder vh = parent.attributes.get(attribute);
                    if (vh != null) {
                        return vh.value;
                    }
                }
            }
        }

        // The attribute was not present in this context or in the parents
        // (if searched), so initialize it if requested.
        if (initialValueFactory != null) {
            synchronized (attributes) {
                ValueHolder vh = attributes.get(attribute);
                if (vh == null) {
                    vh = new ValueHolder(initialValueFactory.newInstance());
                    attributes.put(attribute, vh);
                }
                return vh.value;
            }
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
        synchronized (attributes) {
            final ValueHolder vh = attributes.remove(attribute);
            return vh != null ? vh.value : null;
        }
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
        synchronized (attributes) {
            final ValueHolder vh = attributes.put(attribute, new ValueHolder(newValue));
            return vh != null ? vh.value : null;
        }
    }
}
