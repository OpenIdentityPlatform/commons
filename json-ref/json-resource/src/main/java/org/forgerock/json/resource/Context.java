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

import static org.forgerock.util.Reject.checkNotNull;

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.UUID;

import org.forgerock.json.fluent.JsonValue;

/**
 * The context associated with a request currently being processed by a JSON
 * resource provider. A request context can be used to query state information
 * about the request. Implementations may provide additional information,
 * time-stamp information, HTTP headers, etc. Contexts are linked together to
 * form a parent-child chain of context, whose root is a {@link RootContext}.
 * <p>
 * Contexts <b>MUST</b> support persistence by providing a <b>public</b>
 * constructor having the same declaration as
 * {@link #Context(JsonValue, PersistenceConfig)} and by overriding the
 * {@link #saveToJson(JsonValue, PersistenceConfig)} method. See the method's
 * documentation for more details.
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
public abstract class Context {
    // Persisted attribute names.
    protected static final String ATTR_CLASS = "class";
    protected static final String ATTR_ID = "id";
    private static final String ATTR_PARENT = "parent";

    private static Context load0(final JsonValue savedContext, final PersistenceConfig config)
            throws ResourceException {
        // Determine the context implementation class and instantiate it.
        final String className = savedContext.get(ATTR_CLASS).required().asString();
        try {
            final Class<? extends Context> clazz = Class.forName(className, true,
                    config.getClassLoader()).asSubclass(Context.class);
            final Constructor<? extends Context> constructor = clazz.getDeclaredConstructor(
                    JsonValue.class, PersistenceConfig.class);
            return constructor.newInstance(savedContext, config);
        } catch (final Exception e) {
            throw new IllegalArgumentException(
                    "Unable to instantiate Context implementation class '" + className + "'", e);
        }
    }

    /**
     * A friendly name for the Context.
     */
    private final ContextName contextName;

    /**
     * This should only be accessed via getId() in order to ensure that it is
     * lazily initialized.
     * <p>
     * Benchmarking has shown that generating random UUID is a significant point
     * of contention because the underlying SecureRandom implementation performs
     * single threaded blocking reads to get entropy from /dev/urandom.
     */
    private volatile String id;

    /**
     * The parent Context.
     */
    private final Context parent;

    /**
     * Creates a new context having the provided parent and an ID automatically
     * generated using {@code UUID.randomUUID()}.
     *
     * @param name
     *            The context name.
     * @param parent
     *            The parent context.
     */
    protected Context(final ContextName name, final Context parent) {
        /* ID will be generated lazily in getId() */
        this.contextName = name;
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
     * protected SecurityContext(JsonValue savedContext, PersistenceConfig config)
     *         throws ResourceException {
     *     // Invoke the super-class implementation first.
     *     super.saveToJson(savedContext, config);
     *
     *     // Now parse the attributes for this context.
     *     this.username = savedContext.get(&quot;username&quot;).required().asString();
     *     this.password = savedContext.get(&quot;password&quot;).required().asString();
     * }
     * </pre>
     *
     * In order to creation of a context's persisted JSON representation,
     * implementations must override
     * {@link #saveToJson(JsonValue, PersistenceConfig)}.
     *
     * @param name
     *            The context name.
     * @param savedContext
     *            The JSON representation from which this context's attributes
     *            should be parsed.
     * @param config
     *            The persistence configuration.
     * @throws ResourceException
     *             If the JSON representation could not be parsed.
     * @see #saveToJson(JsonValue, PersistenceConfig)
     * @see ServerContext#loadFromJson(JsonValue, PersistenceConfig)
     */
    protected Context(final ContextName name, final JsonValue savedContext, final PersistenceConfig config)
            throws ResourceException {
        this.contextName = name;
        this.id = savedContext.get(ATTR_ID).required().asString();
        final JsonValue savedParentContext = savedContext.get(ATTR_PARENT);
        this.parent = savedParentContext.isNull() ? null : load0(savedParentContext, config);
    }

    /**
     * Creates a new context having the provided ID, and parent.
     *
     * @param name
     *            The context name.
     * @param id
     *            The context ID.
     * @param parent
     *            The parent context.
     */
    protected Context(final ContextName name, final String id, final Context parent) {
        this.contextName = name;
        this.id = checkNotNull(id, "Cannot create Context with null id");
        this.parent = parent;
    }

    /**
     * Returns the first context in the chain whose type is a sub-type of the
     * provided {@code Context} class. The method first checks this context to
     * see if it has the required type, before proceeding to the parent context,
     * and then continuing up the chain of parents until the root context is
     * reached.
     *
     * @param <T>
     *            The context type.
     * @param clazz
     *            The class of context to be returned.
     * @return The first context in the chain whose type is a sub-type of the
     *         provided {@code Context} class.
     * @throws IllegalArgumentException
     *             If no matching context was found in this context's parent
     *             chain.
     */
    public final <T extends Context> T asContext(final Class<T> clazz) {
        final T context = asContext0(clazz);
        if (context != null) {
            return context;
        } else {
            throw new IllegalArgumentException("No context of type " + clazz.getName() + " found.");
        }
    }

    /**
     * Returns the first context in the chain whose context name matches the
     * provided name.
     *
     * @param contextName
     *            The name of the context to be returned.
     * @return The first context in the chain whose name matches the
     *         provided context name.
     * @throws IllegalArgumentException
     *             If no matching context was found in this context's parent
     *             chain.
     */
    public final Context getContext(final String contextName) {
        final Context context = getContext0(contextName);
        if (context != null) {
            return context;
        } else {
            throw new IllegalArgumentException("No context of named " + contextName + " found.");
        }
    }

    /**
     * Returns {@code true} if there is a context in the chain whose type is a
     * sub-type of the provided {@code Context} class. The method first checks
     * this context to see if it has the required type, before proceeding to the
     * parent context, and then continuing up the chain of parents until the
     * root context is reached.
     *
     * @param clazz
     *            The class of context to be checked.
     * @return {@code true} if there is a context in the chain whose type is a
     *         sub-type of the provided {@code Context} class.
     */
    public final boolean containsContext(final Class<? extends Context> clazz) {
        return asContext0(clazz) != null;
    }

    /**
     * Returns {@code true} if there is a context in the chain whose ContextName is
     * matches the provided context name.
     *
     * @param contextName
     *            The name of the context to locate.
     * @return {@code true} if there is a context in the chain whose context name
     *            matches {@code contextName}.
     */
    public final boolean containsContext(final String contextName) {
        return getContext0(contextName) != null;
    }

    /**
     * Get this Context's {@link ContextName}.
     *
     * @return this object's ContextName
     */
    public final ContextName getContextName() {
        return contextName;
    }

    /**
     * Returns the unique ID identifying this context, usually a UUID.
     *
     * @return The unique ID identifying this context.
     */
    public final String getId() {
        /*
         * Lazily initialize the ID. See field documentation for details.
         */
        if (id == null) {
            synchronized (this) {
                if (id == null) {
                    id = UUID.randomUUID().toString();
                }
            }
        }
        return id;
    }

    /**
     * Returns the parent of this context.
     *
     * @return The parent of this context, or {@code null} if this context is a
     *         root context.
     */
    public final Context getParent() {
        return parent;
    }

    /**
     * Returns {@code true} if this context is a root context.
     *
     * @return {@code true} if this context is a root context.
     */
    public final boolean isRootContext() {
        return getParent() == null;
    }

    /**
     * Creates a JSON representation of this context which is suitable for
     * persistence to long term storage.
     * <p>
     * Sub-classes <b>MUST</b> override this method in order to support
     * persistence. Implementations <b>MUST</b> take care to invoke the super
     * class implementation before creating their own context attributes. Below
     * is an example implementation for a security context which stores the user
     * name and password of the authenticated user:
     *
     * <pre>
     * protected void saveToJson(JsonValue savedContext, PersistenceConfig config)
     *         throws ResourceException {
     *     // Invoke the super-class implementation first.
     *     super.saveToJson(savedContext, config);
     *
     *     // Now create attributes for this context.
     *     savedContext.put(&quot;username&quot;, username);
     *     savedContext.put(&quot;password&quot;, password);
     * }
     * </pre>
     *
     * In order to support reloading of a context's persisted JSON
     * representation, implementations must provide a constructor having the
     * same declaration as {@link #Context(JsonValue, PersistenceConfig)}.
     *
     * @param savedContext
     *            The JSON representation into which this context's attributes
     *            should be placed.
     * @param config
     *            The persistence configuration.
     * @throws ResourceException
     *             If the JSON representation could not be created.
     * @see #Context(JsonValue, PersistenceConfig)
     * @see ServerContext#saveToJson(ServerContext, PersistenceConfig)
     */
    protected void saveToJson(final JsonValue savedContext, final PersistenceConfig config)
            throws ResourceException {
        savedContext.put(ATTR_ID, getId());
        savedContext.put(ATTR_CLASS, getClass().getName());

        // Persist the parent if there is one.
        if (parent != null) {
            final JsonValue savedParentContext = new JsonValue(new LinkedHashMap<String, Object>(4));
            parent.saveToJson(savedParentContext, config);
            savedContext.put(ATTR_PARENT, savedParentContext.asMap());
        }
    }

    private final <T extends Context> T asContext0(final Class<T> clazz) {
        for (Context context = this; context != null; context = context.getParent()) {
            final Class<?> contextClass = context.getClass();
            if (clazz.isAssignableFrom(contextClass)) {
                return contextClass.asSubclass(clazz).cast(context);
            }
        }
        return null;
    }

    private final Context getContext0(final String contextName) {
        for (Context context = this; context != null; context = context.getParent()) {
            if (context.getContextName().toString().equals(contextName)) {
                return context;
            }
        }
        return null;
    }
}
