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
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2012-2015 ForgeRock AS.
 */

package org.forgerock.services.context;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.lang.reflect.Constructor;

import org.forgerock.json.JsonValue;
import org.forgerock.util.Reject;

/**
 * A base implementation of the {@link Context} interface. Derived Contexts <b>MUST</b> support persistence by providing
 * <ul>
 * <li>a <b>public</b> constructor having the same declaration as
 * {@link #AbstractContext(JsonValue, ClassLoader)}</li>
 * <li>a <b>public</b> method having the same declaration as
 * {@link Context#toJsonValue}</li>
 * </ul>
 * See the documentation for more details.
 * <p>
 * Here is an example of the JSON representation of the core attributes of all
 * contexts:
 *
 * <pre>
 * {
 *   "id"     : "56f0fb7e-3837-464d-b9ec-9d3b6af665c3",
 *   "class"  : ...Java class name..,
 *   "parent" : {
 *       ...
 *   }
 * }
 * </pre>
 */
public abstract class AbstractContext implements Context {

    // Persisted attribute names.
    private static final String ATTR_CLASS = "class";
    private static final String ATTR_ID = "id";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_PARENT = "parent";

    /**
     * The parent Context.
     */
    private final Context parent;

    /**
     * The Context data.
     */
    protected final JsonValue data;

    /**
     * Constructs a new {@code AbstractContext} with a {@code null} {@code id}.
     *
     * @param parent The parent context.
     * @param name The name of the context.
     */
    protected AbstractContext(Context parent, String name) {
        this(null, name, parent);
    }

    /**
     * Constructs a new {@code AbstractContext}.
     *
     * @param id The id of the context.
     * @param parent The parent context.
     * @param name The name of the context.
     */
    protected AbstractContext(String id, String name, Context parent) {
        data = json(object());
        data.put(ATTR_CLASS, getClass().getName());
        if (id != null) {
            data.put(ATTR_ID, id);
        }
        data.put(ATTR_NAME, name);
        this.parent = parent;
    }

    /**
     * Creates a new context from the JSON representation of a previously
     * persisted context.
     * <p>
     * Sub-classes <b>MUST</b> provide a constructor having the same declaration
     * as this constructor in order to support persistence. Implementations
     * <b>MUST</b> take care to invoke the super class implementation before
     * parsing their own context attributes. Below is an example implementation
     * for a security context which stores the user name and password of the
     * authenticated user:
     *
     * <pre>
     * protected SecurityContext(JsonValue savedContext, ClassLoader classLoader) {
     *     // Invoke the super-class implementation first.
     *     super(savedContext, classLoader);
     *
     *     // Now parse the attributes for this context.
     *     this.username = savedContext.get(&quot;username&quot;).required().asString();
     *     this.password = savedContext.get(&quot;password&quot;).required().asString();
     * }
     * </pre>
     *
     * In order to create a context's persisted JSON representation,
     * implementations must override.
     *
     * @param savedContext
     *            The JSON representation from which this context's attributes
     *            should be parsed.
     * @param classLoader
     *            The ClassLoader which can properly resolve the persisted class-name.
     */
    public AbstractContext(final JsonValue savedContext, final ClassLoader classLoader) {
        final JsonValue savedParentContext = savedContext.get(ATTR_PARENT);
        savedContext.remove(ATTR_PARENT);
        data = savedContext.copy();
        this.parent = savedParentContext.isNull() ? null : load0(savedParentContext, classLoader);
    }

    private static Context load0(final JsonValue savedContext, final ClassLoader classLoader) {
        // Determine the context implementation class and instantiate it.
        final String className = savedContext.get(ATTR_CLASS).required().asString();
        try {
            final Class<? extends Context> clazz = Class.forName(className, true, classLoader)
                    .asSubclass(AbstractContext.class);
            final Constructor<? extends Context> constructor = clazz.getDeclaredConstructor(
                    JsonValue.class, ClassLoader.class);
            return constructor.newInstance(savedContext, classLoader);
        } catch (final Exception e) {
            throw new IllegalArgumentException(
                    "Unable to instantiate Context implementation class '" + className + "'", e);
        }
    }

    @Override
    public final String getContextName() {
        return data.get(ATTR_NAME).asString();
    }

    @Override
    public final <T extends Context> T asContext(Class<T> clazz) {
        Reject.ifNull(clazz, "clazz cannot be null");
        T context = asContext0(clazz);
        if (context != null) {
            return context;
        } else {
            throw new IllegalArgumentException("No context of type " + clazz.getName() + " found.");
        }
    }

    @Override
    public final Context getContext(String contextName) {
        Context context = getContext0(contextName);
        if (context != null) {
            return context;
        } else {
            throw new IllegalArgumentException("No context of named " + contextName + " found.");
        }
    }

    @Override
    public final boolean containsContext(Class<? extends Context> clazz) {
        return asContext0(clazz) != null;
    }

    @Override
    public final boolean containsContext(String contextName) {
        return getContext0(contextName) != null;
    }

    @Override
    public final String getId() {
        if (data.get(ATTR_ID).isNull() && !isRootContext()) {
            return getParent().getId();
        } else {
            return data.get(ATTR_ID).required().asString();
        }
    }

    @Override
    public final Context getParent() {
        return parent;
    }

    @Override
    public final boolean isRootContext() {
        return getParent() == null;
    }

    @Override
    public JsonValue toJsonValue() {
        final JsonValue value = data.copy();
        value.put(ATTR_PARENT, parent != null ? parent.toJsonValue().getObject() : null);
        return value;
    }

    @Override
    public String toString() {
        return toJsonValue().toString();
    }

    private <T extends Context> T asContext0(final Class<T> clazz) {
        try {
            for (Context context = this; context != null; context = context.getParent()) {
                final Class<?> contextClass = context.getClass();
                if (clazz.isAssignableFrom(contextClass)) {
                    return contextClass.asSubclass(clazz).cast(context);
                }
            }
            return null;
        } catch (final Exception e) {
            throw new IllegalArgumentException(
                    "Unable to instantiate Context implementation class '" + clazz.getName() + "'", e);
        }
    }

    private Context getContext0(final String contextName) {
        for (Context context = this; context != null; context = context.getParent()) {
            if (context.getContextName().equals(contextName)) {
                return context;
            }
        }
        return null;
    }

}
