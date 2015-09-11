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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.services.context;

import org.forgerock.json.JsonValue;

/**
 * Type-safe contextual information associated with the processing of a request in an application.
 * Contexts are linked together in a stack with a {@link RootContext} at the bottom of the stack. Each context
 * maintains a reference to its parent context which can be accessed using the {@link #getParent()}} method.
 * Context implementations may provide information about the client, the end-user, auditing information, routing
 * decisions, etc. While contexts are arranged in a stack, application code will usually access contexts using the
 * {@link #asContext(Class)} method:
 *
 * <pre>
 *     Context context = ...; // Opaque reference to the context stack
 *
 *     String remoteHost = context.asContext(ClientContext.class).getRemoteHost();
 *     context.asContext(AttributesContext.class).getAttributes().put("key", "value");
 * </pre>
 *
 * Alternatively, scripted applications will usually access contexts by name:
 *
 * <pre>
 *     var remoteHost = context.client.remoteHost;
 *     context.attributes.key = "value";
 * </pre>
 *
 * Context implementations should inherit from {@link AbstractContext} and ensure that they can be serialized to and
 * from JSON.
 */
public interface Context {

    /**
     * Get this Context's name.
     *
     * @return this object's name
     */
    String getContextName();

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
    <T extends Context> T asContext(Class<T> clazz);

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
    Context getContext(String contextName);

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
    boolean containsContext(Class<? extends Context> clazz);

    /**
     * Returns {@code true} if there is a context in the chain whose name
     * matches the provided context name.
     *
     * @param contextName
     *            The name of the context to locate.
     * @return {@code true} if there is a context in the chain whose context name
     *            matches {@code contextName}.
     */
    boolean containsContext(String contextName);

    /**
     * Returns the unique ID identifying this context, usually a UUID.
     *
     * @return The unique ID identifying this context.
     */
    String getId();

    /**
     * Returns the parent of this context.
     *
     * @return The parent of this context, or {@code null} if this context is a
     *         root context.
     */
    Context getParent();

    /**
     * Returns {@code true} if this context is a root context.
     *
     * @return {@code true} if this context is a root context.
     */
    boolean isRootContext();

    /**
     * Return this Context as a JsonValue (for persistence).
     *
     * @return the Context data as a JsonValue.
     */
    JsonValue toJsonValue();
}
