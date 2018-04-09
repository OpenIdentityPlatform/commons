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

import org.forgerock.services.context.Context;

import javax.script.Bindings;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public interface ScriptEntry extends Scope {

    enum Visibility {
        PUBLIC, PRIVATE, DEFAULT;
    }

    /**
     * Adds an observer to the set of observers for this object, provided that
     * it is not the same as some observer already in the set. The order in
     * which notifications will be delivered to multiple observers is not
     * specified. See the class comment.
     *
     * @param hook
     *            an observer to be added.
     * @throws NullPointerException
     *             if the parameter o is null.
     */
    void addScriptListener(ScriptListener hook);

    /**
     * Deletes an observer from the set of observers of this object. Passing
     * {@code null} to this method will have no effect.
     *
     * @param hook
     *            the observer to be deleted.
     */
    void deleteScriptListener(ScriptListener hook);

    /**
     * Get a new {@literal Non-ThreadSafe} Script instance.
     *
     * @param context
     *            the request {@code Context}
     * @return new Script instance
     */
    Script getScript(Context context);

    /**
     * Get a JSR223 Script Engine aware binding.
     *
     * @param context
     *            the request {@code Context}
     * @param request
     *            the request bindings
     * @return new {@code Bindings} contains the service and global scope.
     */
    Bindings getScriptBindings(Context context, Bindings request);

    ScriptName getName();

    Visibility getVisibility();

    /**
     * Returns <tt>true</tt> if this script can be evaluated.
     * <p/>
     * This method checks the availability of the required
     * {@link org.forgerock.script.engine.ScriptEngine} and the
     * {@link org.forgerock.script.source.SourceUnit}.
     *
     * @return <tt>true</tt> if this script can be evaluated
     */
    boolean isActive();

}
