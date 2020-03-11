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

package org.forgerock.script.engine;

import org.forgerock.script.source.ScriptSource;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public interface CompilationHandler {

    /**
     * The script is uninstalled and may not be used.
     * 
     * <p>
     * The {@code UNINSTALLED} state is only visible after a script is
     * uninstalled; the script is in an unusable state but references to the
     * {@code script} object may still be available and used for introspection.
     * <p>
     * The value of {@code UNINSTALLED} is 0x00000001.
     */
    public static final int UNINSTALLED = 0x00000001;

    /**
     * The script is installed but not yet resolved.
     * 
     * <p>
     * A script is in the {@code INSTALLED} state when it has been installed in
     * the {@link org.forgerock.script.ScriptRegistry} but is not or cannot be
     * resolved.
     * <p>
     * This state is visible if the script's code dependencies are not resolved.
     * The {@code ScriptRegistry} may attempt to resolve an {@code INSTALLED}
     * script's code dependencies and move the script to the {@code RESOLVED}
     * state.
     * <p>
     * The value of {@code INSTALLED} is 0x00000002.
     */
    public static final int INSTALLED = 0x00000002;

    /**
     * The script is resolved and is able to be started.
     * 
     * <p>
     * A script is in the {@code RESOLVED} state when the Framework has
     * successfully resolved the script's code dependencies. Note that the
     * script is not active yet. A script must be put in the {@code RESOLVED}
     * state before it can be started. The {@code ScriptRegistry} may attempt to
     * resolve a script at any time.
     * <p>
     * The value of {@code RESOLVED} is 0x00000004.
     */
    public static final int RESOLVED = 0x00000004;

    /**
     * The script is in the process of starting.
     * 
     * <p>
     * A script is in the {@code STARTING} state when its
     * {@link ScriptEngine#compileScript(CompilationHandler)} method is active.
     * A script must be in this state when the script's
     * {@link ScriptEngine#compileScript(CompilationHandler)} is called. If the
     * {@code ScriptEngine.compileScript} method completes without exception,
     * then the script has successfully started and must move to the
     * {@code ACTIVE} state.
     * <p>
     * The value of {@code STARTING} is 0x00000008.
     */
    public static final int STARTING = 0x00000008;

    /**
     * The script is in the process of stopping.
     * 
     * <p>
     * A script is in the {@code STOPPING} state when its {@link #ACTIVE} state
     * is active. A script must be in this state when the script's {@link @link
     * ScriptEngine#compileScript(CompilationHandler)} method is called.
     * <p>
     * When the {@code ScriptEngine.compileScript} method completes the script
     * is stopped and must move to the {@code RESOLVED} state.
     * <p>
     * The value of {@code STOPPING} is 0x00000010.
     */
    public static final int STOPPING = 0x00000010;

    /**
     * The script is now running.
     * 
     * <p>
     * A script is in the {@code ACTIVE} state when it has been successfully
     * started and activated.
     * <p>
     * The value of {@code ACTIVE} is 0x00000020.
     */
    public static final int ACTIVE = 0x00000020;

    public ScriptSource getScriptSource();

    public ClassLoader getParentClassLoader();

    public void setClassLoader(ClassLoader classLoader);

    public void setCompiledScript(CompiledScript script);

    public void handleException(Exception exception);

}
