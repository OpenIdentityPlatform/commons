/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 ForgeRock AS. All rights reserved.
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

import org.forgerock.json.JsonValue;
import org.forgerock.script.engine.ScriptEngine;
import org.forgerock.script.source.SourceUnitObserver;

import javax.script.ScriptException;
import java.util.Set;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public interface ScriptRegistry extends SourceUnitObserver {

    /**
     * Looks up and creates a <code>ScriptEngine</code> for a given name. The
     * algorithm first searches for a <code>ScriptEngineFactory</code> that has
     * been registered as a handler for the specified name using the
     * <code>registerEngineName</code> method. <br>
     * <br>
     * If one is not found, it searches the array of
     * <code>ScriptEngineFactory</code> instances stored by the constructor for
     * one with the specified name. If a <code>ScriptEngineFactory</code> is
     * found by either method, it is used to create instance of
     * <code>ScriptEngine</code>.
     *
     * @param shortName
     *            The short name of the <code>ScriptEngine</code>
     *            implementation. returned by the <code>getNames</code> method
     *            of its <code>ScriptEngineFactory</code>.
     * @return A <code>ScriptEngine</code> created by the factory located in the
     *         search. Returns null if no such factory was found. The
     *         <code>ScriptEngineManager</code> sets its own
     *         <code>globalScope</code> <code>Bindings</code> as the
     *         <code>GLOBAL_SCOPE</code> <code>Bindings</code> of the newly
     *         created <code>ScriptEngine</code>.
     * @throws NullPointerException
     *             if shortName is null.
     */
    public ScriptEngine getEngineByName(String shortName);

    /**
     * Lists all {@code ScriptName}s available in the registry.
     *
     * @return the list of registered
     */
    public Set<ScriptName> listScripts();

    /**
     * Takes a {@code ScriptEntry} from this {@code ScriptRegistry}.
     *
     * @param script
     *            The identifier of the {@code ScriptEntry}.
     * @return <tt>null</tt> if the registry does not have the script, else the
     *         {@code ScriptEntry} instance.
     * @throws NullPointerException
     *             if script is null.
     * @throws org.forgerock.json.JsonValueException
     * @throws IllegalArgumentException
     * @throws ScriptException
     */
    public ScriptEntry takeScript(JsonValue script) throws ScriptException;

    /**
     * Takes a {@code ScriptEntry} from this {@code ScriptRegistry}.
     *
     * @param name
     *            The identifier of the {@code ScriptEntry}.
     * @return <tt>null</tt> if the registry does not have the script, else the
     *         {@code ScriptEntry} instance.
     * @throws NullPointerException
     *             if script is null.
     */
    public ScriptEntry takeScript(ScriptName name) throws ScriptException;

    public void addScriptListener(ScriptName name, ScriptListener hook);

    public void deleteScriptListener(ScriptName name, ScriptListener hook);

    public ClassLoader getRegistryLevelScriptClassLoader();

    public void setRegistryLevelScriptClassLoader(ClassLoader registryLevelScriptClassLoader);
}
