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
 * Copyright 2012 ForgeRock AS.
 */

package org.forgerock.json.resource;

import org.forgerock.util.Factory;

/**
 * A request context defined attribute. Context attributes are uniquely
 * identified by their ID, which is usually a URN. Two attributes having the
 * same ID are considered to be the same attribute. By default all request
 * contexts have the following attributes whose IDs are reserved:
 * <ul>
 * <li>{@link #TYPE} - a string, usually a URN, indicating the type of the
 * context. The name {@code "root"} is reserved for root contexts.
 * <li>{@link #ID} - a string, usually a UUID, uniquely identifying the context.
 * <li>{@link #PARENT} - the parent context, or {@code null} for root contexts.
 * </ul>
 * Attributes are strongly typed and should be used in a similar style to
 * {@code ThreadLocal}s. For example:
 *
 * <pre>
 * ContextAttribute&lt;String&gt; username =
 *     new ContextAttribute&lt;String&gt;(&quot;com.example.username&quot;);
 * Context context = Context.newRootContext();
 *
 * username.set(context, &quot;bjensen&quot;);
 * String value = username.get(context);
 * </pre>
 *
 * @param <T>
 *            The type of value contained in the context attribute.
 */
public final class ContextAttribute<T> implements Comparable<ContextAttribute<?>> {
    // The factory used to initialize attributes to a fixed initial value.
    private static final class FixedValueFactory<T> implements Factory<T> {
        private final T initialValue;

        private FixedValueFactory(final T initialValue) {
            this.initialValue = initialValue;
        }

        public T newInstance() {
            return initialValue;
        }
    }

    /**
     * The reserved {@code String} valued attribute {@code "id"}, usually a
     * UUID, uniquely identifying the context.
     */
    public static final ContextAttribute<String> ID = new ContextAttribute<String>("id");

    /**
     * The reserved {@code Context} valued attribute {@code "parent"} containing
     * a reference to the parent context, or {@code null} for root contexts.
     */
    public static final ContextAttribute<Context> PARENT = new ContextAttribute<Context>("parent");

    /**
     * The reserved {@code String} valued attribute {@code "type"}, usually a
     * URN, indicating the type of the context. The type {@code "root"} is
     * reserved for root contexts.
     */
    public static final ContextAttribute<String> TYPE = new ContextAttribute<String>("type");

    private final String id;
    private final Factory<T> initialValueFactory;

    /**
     * Creates a new context attribute with the provided class name and no
     * initial value.
     *
     * @param clazz
     *            The class whose name will be used as the attribute ID.
     */
    public ContextAttribute(final Class<?> clazz) {
        this(clazz.getName());
    }

    /**
     * Creates a new context attribute with the provided class name and initial
     * value.
     *
     * @param clazz
     *            The class whose name will be used as the attribute ID.
     * @param initialValueFactory
     *            A factory which will be used to lazily initialize the
     *            attribute on demand.
     */
    public ContextAttribute(final Class<?> clazz, final Factory<T> initialValueFactory) {
        this(clazz.getName(), initialValueFactory);
    }

    /**
     * Creates a new context attribute with the provided class name and initial
     * value.
     *
     * @param clazz
     *            The class whose name will be used as the attribute ID.
     * @param initialValue
     *            The initial value for the attribute.
     */
    public ContextAttribute(final Class<?> clazz, final T initialValue) {
        this(clazz.getName(), initialValue);
    }

    /**
     * Creates a new context attribute with the provided ID and no initial
     * value.
     *
     * @param id
     *            The attribute ID, usually a URN.
     */
    public ContextAttribute(final String id) {
        this.id = id;
        this.initialValueFactory = null;
    }

    /**
     * Creates a new context attribute with the provided ID and initial value.
     *
     * @param id
     *            The attribute ID, usually a URN.
     * @param initialValueFactory
     *            A factory which will be used to lazily initialize the
     *            attribute on demand.
     */
    public ContextAttribute(final String id, final Factory<T> initialValueFactory) {
        this.id = id;
        this.initialValueFactory = initialValueFactory;
    }

    /**
     * Creates a new context attribute with the provided ID and initial value.
     *
     * @param id
     *            The attribute ID, usually a URN.
     * @param initialValue
     *            The initial value for the attribute.
     */
    public ContextAttribute(final String id, final T initialValue) {
        this.id = id;
        this.initialValueFactory = new FixedValueFactory<T>(initialValue);
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo(final ContextAttribute<?> o) {
        return id.compareTo(o.id);
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof ContextAttribute<?>) {
            return id.equals(((ContextAttribute<?>) obj).id);
        } else {
            return false;
        }
    }

    /**
     * Searches the provided context and any parent contexts for this attribute.
     * The value will be lazily initialized if this attribute specifies an
     * initial value or factory.
     *
     * @param context
     *            The context to be searched.
     * @return The attribute value, or {@code null} if the attribute was not
     *         found and was not lazily initialized.
     */
    public T find(final Context context) {
        return asT(context.getAttribute(this, true, initialValueFactory));
    }

    /**
     * Searches the provided context and any parent contexts for this attribute.
     * The value will be lazily initialized to the provided value if it is not
     * found.
     *
     * @param context
     *            The context to be searched.
     * @param initialValue
     *            The initial value for the attribute.
     * @return The attribute value.
     */
    public T find(final Context context, final T initialValue) {
        return asT(context.getAttribute(this, true, new FixedValueFactory<T>(initialValue)));
    }

    /**
     * Returns the value associated with this attribute for the provided
     * context. The value will be lazily initialized if this attribute specifies
     * an initial value or factory.
     *
     * @param context
     *            The context containing the attribute.
     * @return The attribute value, or {@code null} if the attribute was not
     *         found and was not lazily initialized.
     */
    public T get(final Context context) {
        return asT(context.getAttribute(this, false, initialValueFactory));
    }

    /**
     * Returns the value associated with this attribute for the provided
     * context. The value will be lazily initialized to the provided value if it
     * is not found.
     *
     * @param context
     *            The context containing the attribute.
     * @param initialValue
     *            The initial value for the attribute.
     * @return The attribute value.
     */
    public T get(final Context context, final T initialValue) {
        return asT(context.getAttribute(this, false, new FixedValueFactory<T>(initialValue)));
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * Returns the ID of this context attribute.
     *
     * @return The ID of this context attribute.
     */
    public String id() {
        return id;
    }

    /**
     * Removes this attribute from the provided context.
     *
     * @param context
     *            The context from which the attribute should be removed.
     * @return The removed attribute value, or {@code null} if the attribute was
     *         not present.
     */
    public T remove(final Context context) {
        return asT(context.removeAttribute(this));
    }

    /**
     * Sets a value for this attribute in the provided context.
     *
     * @param context
     *            The context in which the attribute should be set.
     * @param newValue
     *            The new attribute value.
     * @return The old attribute value, or {@code null} if the attribute was not
     *         present.
     */
    public T set(final Context context, final T newValue) {
        return asT(context.setAttribute(this, newValue));
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return id;
    }

    @SuppressWarnings("unchecked")
    private T asT(final Object value) {
        return (T) value;
    }

}
