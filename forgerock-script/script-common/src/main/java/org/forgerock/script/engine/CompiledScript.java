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
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public interface CompiledScript {

    /**
     * Evaluated the script stored in this {@code CompiledScript} object.
     * <p/>
     *
     * @param context
     *            A {@code context} associated with the
     *            {@link org.forgerock.json.resource.Request}.
     *
     * @param request
     *            A <code>ScriptContext</code> that is used in the same way as
     *            the <code>ScriptContext</code> passed to the <code>eval</code>
     *            methods of <code>ScriptEngine</code>.
     * @param scopes
     *            Additional {@code Bindings} extending the {@code request}
     *            with.
     *
     * @return The value returned by the script execution, if any. Should return
     *         <code>null</code> if no value is returned by the script
     *         execution.
     *
     * @throws javax.script.ScriptException
     *             if an error occurs.
     */
    public Object eval(Context context, Bindings request, Bindings... scopes)
            throws ScriptException;

    public Bindings prepareBindings(Context context, Bindings request, Bindings... scopes);

}
