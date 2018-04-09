/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.script;

import javax.script.Bindings;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public interface Scope {
    /**
     * Sets a key/value pair in the state of the ScriptEngine that may either
     * create a Java Language Binding to be used in the execution of scripts or
     * be used in some other way, depending on whether the key is reserved. Must
     * have the same effect as
     * <code>getBindings(ScriptContext.ENGINE_SCOPE).put</code>.
     * 
     * @param key
     *            The name of named value to add
     * @param value
     *            The value of named value to add.
     * 
     * @throws NullPointerException
     *             if key is null.
     * @throws IllegalArgumentException
     *             if key is empty.
     */
    public void put(String key, Object value);

    /**
     * Retrieves a value set in the state of this engine. The value might be one
     * which was set using <code>setValue</code> or some other value in the
     * state of the <code>ScriptEngine</code>, depending on the implementation.
     * Must have the same effect as
     * <code>getBindings(ScriptContext.ENGINE_SCOPE).get</code>
     * 
     * @param key
     *            The key whose value is to be returned
     * @return the value for the given key
     * 
     * @throws NullPointerException
     *             if key is null.
     * @throws IllegalArgumentException
     *             if key is empty.
     */
    public Object get(String key);

    /**
     * Returns a scope of named values. The possible scopes are: <br>
     * <br>
     * <ul>
     * <li><code>ScriptContext.GLOBAL_SCOPE</code> - The set of named values
     * representing global scope. If this <code>ScriptEngine</code> is created
     * by a <code>ScriptEngineManager</code>, then the manager sets global scope
     * bindings. This may be <code>null</code> if no global scope is associated
     * with this <code>ScriptEngine</code></li>
     * <li><code>ScriptContext.ENGINE_SCOPE</code> - The set of named values
     * representing the state of this <code>ScriptEngine</code>. The values are
     * generally visible in scripts using the associated keys as variable names.
     * </li>
     * <li>Any other value of scope defined in the default
     * <code>ScriptContext</code> of the <code>ScriptEngine</code>.</li>
     * </ul>
     * <br>
     * <br>
     * The <code>Bindings</code> instances that are returned must be identical
     * to those returned by the <code>getBindings</code> method of
     * <code>ScriptContext</code> called with corresponding arguments on the
     * default <code>ScriptContext</code> of the <code>ScriptEngine</code>.
     * 
     * 
     * @return The <code>Bindings</code> with the specified scope.
     * 
     * @throws IllegalArgumentException
     *             if specified scope is invalid
     * 
     */
    public Bindings getBindings();

    /**
     * Sets a scope of named values to be used by scripts. The possible scopes
     * are: <br>
     * <br>
     * <ul>
     * <li><code>ScriptContext.ENGINE_SCOPE</code> - The specified
     * <code>Bindings</code> replaces the engine scope of the
     * <code>ScriptEngine</code>.</li>
     * <li><code>ScriptContext.GLOBAL_SCOPE</code> - The specified
     * <code>Bindings</code> must be visible as the <code>GLOBAL_SCOPE</code>.</li>
     * <li>Any other value of scope defined in the default
     * <code>ScriptContext</code> of the <code>ScriptEngine</code>.</li>
     * </ul>
     * <br>
     * <br>
     * The method must have the same effect as calling the
     * <code>setBindings</code> method of <code>ScriptContext</code> with the
     * corresponding value of <code>scope</code> on the default
     * <code>ScriptContext</code> of the <code>ScriptEngine</code>.
     * 
     * @param bindings
     *            The <code>Bindings</code> for the specified scope.
     * 
     * @throws IllegalArgumentException
     *             if the scope is invalid
     * @throws NullPointerException
     *             if the bindings is null and the scope is
     *             <code>ScriptContext.ENGINE_SCOPE</code>
     */
    public void setBindings(Bindings bindings);

    public void flush();

    /**
     * Returns an uninitialized <code>Bindings</code>.
     * 
     * @return A <code>Bindings</code> that can be used to replace the state of
     *         this <code>ScriptEngine</code>.
     **/
    public Bindings createBindings();

}
