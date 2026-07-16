/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock AS. All Rights Reserved
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
 * Portions Copyrighted 2026 3A Systems, LLC
 */

package org.forgerock.script.scope;

import org.forgerock.json.resource.ResourceException;

import java.io.Serializable;

/**
 * Exposes a function that can be provided to a script to invoke.
 *
 * @param <R> Type of the return value of this function.
 */
public interface Function<R> extends Serializable {

    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @param scope
     *            the parameter providing the scope available to the function.
     * @param callback
     *            the callback function that may be invoked by this function.
     * @param arguments could be a single value or a List of values
     * @return computed result
     * @throws ResourceException
     *             if unable to compute a result
     * @throws NoSuchMethodException
     *             if the requested method cannot be found.
     */
    R call(Parameter scope, Function<?> callback, Object... arguments)
            throws ResourceException, NoSuchMethodException;
}
