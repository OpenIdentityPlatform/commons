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
     * @param callback
     * @param arguments could be a single value or a List of values
     * @return computed result
     * @throws Exception
     *             if unable to compute a result
     * @throws ResourceException
     * @throws NoSuchMethodException
     */
    R call(Parameter scope, Function<?> callback, Object... arguments)
            throws ResourceException, NoSuchMethodException;
}
