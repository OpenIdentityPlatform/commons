/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 ForgeRock AS. All rights reserved.
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

package org.forgerock.script.groovy;

import groovy.lang.Closure;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.script.scope.Function;
import org.forgerock.script.scope.Parameter;

import java.util.Arrays;

/**
 * 
 */
public class FunctionClosure extends Closure<JsonValue> {

    private static final long serialVersionUID = -8234912264889627793L;

    /** TODO: Description. */
    private final Function<?> function;
    /** TODO: Description. */
    private final Parameter parameter;

    public FunctionClosure(final Object owner, final Parameter parameter, final Function<?> function) {
        super(owner);
        this.function = function;
        this.parameter = parameter;
    }

    @SuppressWarnings("unchecked")
    public Object doCall(Object... args) {
        try {
            Object[] arguments = args;
            Function<?> callbackFunction = null;
            if (args.length > 0 && args[args.length - 1] instanceof Closure) {
                final Closure nativeClosure = (Closure) args[args.length - 1];
                if (nativeClosure instanceof FunctionClosure) {
                    callbackFunction = ((FunctionClosure) nativeClosure).function;
                } else {
                    callbackFunction = new Function<Object>() {
                        @Override
                        public Object call(final Parameter scope0, final Function<?> callback,
                                final Object... arguments) throws ResourceException,
                                NoSuchMethodException {

                            Class[] paramTypes = nativeClosure.getParameterTypes();
                            Object[] params = new Object[paramTypes.length];
                            for (int i = 0; i < paramTypes.length; i++) {
                                if (i < arguments.length) {
                                    params[i] = arguments[i];
                                } else {
                                    params[i] = null;
                                }
                            }
                            return nativeClosure.call(params);
                        }
                    };
                }
                arguments = Arrays.copyOfRange(args, 0, args.length - 1);
            }
            Object result = function.call(parameter, callbackFunction, arguments);
            if (result instanceof JsonValue) {
                return ((JsonValue) result).getObject();
            }
            return result;
        } catch (Throwable e) {
            return throwRuntimeException(e);
        }
    }
}
