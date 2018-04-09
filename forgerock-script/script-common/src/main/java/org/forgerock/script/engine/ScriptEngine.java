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

import org.forgerock.services.context.Context;

import javax.script.Bindings;
import javax.script.ScriptException;

/**
 * A ScriptEngine finds the applicable ...
 *
 * @author Laszlo Hordos
 */
public interface ScriptEngine {

    void compileScript(CompilationHandler handler) throws ScriptException;

    /**
     * Returns a <code>ScriptEngineFactory</code> for the class to which this
     * <code>ScriptEngine</code> belongs.
     *
     * @return The <code>ScriptEngineFactory</code>
     */
    ScriptEngineFactory getFactory();

    /**
     * Visits a function instance.
     *
     * @param context
     *            A visitor specified parameter.
     * @param request
     *            The binding of the request
     * @param value
     *            The Bindings instance.
     * @return Returns a visitor specified result.
     */
    Bindings compileBindings(Context context, Bindings request, Bindings... value);

    /**
     * Visits a function instance.
     *
     * @param context
     *            A visitor specified parameter.
     * @param value
     *            The ScriptContext instance.
     * @return Returns a visitor specified result.
     */
    Object compileObject(Context context, Object value);

}
