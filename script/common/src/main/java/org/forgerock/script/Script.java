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
import javax.script.ScriptException;

/**
 * Interface for all executable script.
 * 
 * @see javax.script.CompiledScript
 * 
 */
public interface Script extends Scope {

    /**
     * Sets a key/value pair in the state of the Request that may either create
     * a Thread-Safe Java Language Binding to be used in the execution of
     * scripts or be used in some other way, depending on whether the key is
     * reserved.
     * <p/>
     * The {@link org.forgerock.script.Scope#put(String, Object)} suppress the
     * value with the same key.
     * 
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
    public void putSafe(String key, Object value);

    /**
     * Executes the program stored in the {@code Script} object using the
     * supplied Bindings of attributes as the ENGINE_SCOPE of the associated
     * ScriptEngine during script execution.
     * 
     * @param bindings
     *            The bindings of attributes used for the ENGINE_SCOPE.
     * @return The return value from the script execution.
     * @throws ScriptException
     *             if an exception occurred during execution of the script.
     */
    Object eval(Bindings bindings) throws ScriptException;

    /**
     * Executes the program stored in the {@code Script} object.
     * 
     * @return The return value from the script execution.
     * @throws ScriptException
     *             if an exception occurred during execution of the script.
     */
    Object eval() throws ScriptException;
}
