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

package org.forgerock.script.javascript;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.script.scope.Function;
import org.forgerock.script.scope.OperationParameter;
import org.forgerock.script.scope.Parameter;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeFunction;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrappedException;
import org.mozilla.javascript.Wrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides a Rhino {@code Function} wrapper for an OpenIDM {@code Function}
 * object.
 *
 * @author Paul C. Bryan
 */
class ScriptableFunction extends BaseFunction implements Wrapper {

    private static final long serialVersionUID = 1L;

    /** The request being wrapped. */
    private final AtomicReference<Parameter> parameter;

    /** The function being wrapped. */
    private final Function<?> function;

    ScriptableFunction(final Parameter operationParameter, final Function<?> function) {
        this.function = function;
        this.parameter = new AtomicReference<Parameter>(operationParameter);
    }

    ScriptableFunction(Scriptable scope, Scriptable prototype,
            final OperationParameter operationParameter, final Function<?> function) {
        super(scope, prototype);
        this.function = function;
        this.parameter = new AtomicReference<Parameter>(operationParameter);
    }

    /**
     * TODO: Description.
     *
     * @param args
     *            TODO.
     * @return TODO.
     */
    private List<Object> convert(Object[] args) {
        ArrayList<Object> list = new ArrayList<Object>();
        for (Object object : args) {
            if (object instanceof BaseFunction) {
                continue;
            }
            list.add(Converter.convert(object));
        }
        return list;
    }

    @Override
    public Object call(final Context cx, final Scriptable scope, final Scriptable thisObj,
            Object[] args) {
        try {
            Object[] arguments = args;
            Function<?> callbackFunction = null;
            if (args.length > 0 && args[args.length - 1] instanceof NativeFunction) {
                final BaseFunction nativeFunction = (BaseFunction) args[args.length - 1];
                if (nativeFunction instanceof ScriptableFunction) {
                    callbackFunction = ((ScriptableFunction) nativeFunction).function;
                } else {
                    callbackFunction = new Function<Void>() {
                        @Override
                        public Void call(final Parameter scope0, final Function<?> callback,
                                final Object... arguments) throws ResourceException,
                                NoSuchMethodException {

                            try {
                                nativeFunction.call(cx, scope, thisObj, arguments);
                            } catch (Exception e) {
                                // TODO log
                            }
                            return null;
                        }
                    };
                }
                arguments = Arrays.copyOfRange(args, 0, args.length - 1);
            }
            Object result =
                    function.call(getParameter(), callbackFunction, convert(arguments).toArray());
            if (null == result) {
                return null;
            } else if (result instanceof JsonValue) {
                return Converter.wrap(getParameter(), ((JsonValue) result).getObject(), scope,
                        false);
            } else {
                return Converter.wrap(getParameter(), result, scope, false);
            }
        } catch (Throwable throwable) {
            throw new WrappedException(throwable);
        }
    }

    protected Parameter getParameter() {
        Parameter p = parameter.get();
        if (null == p) {
            Object o = Context.getCurrentContext().getThreadLocal(Parameter.class.getName());
            if (o instanceof Parameter) {
                parameter.lazySet((Parameter) p);
                return (Parameter) p;
            }
        } else {
            return p;
        }
        return null;
    }

    @Override
    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
        throw Context.reportRuntimeError("functions may not be used as constructors");
    }

    /**
     * Gets the value returned by calling the typeof operator on this object.
     *
     * @see org.mozilla.javascript.ScriptableObject#getTypeOf()
     * @return "function" or "undefined" if {@link #avoidObjectDetection()}
     *         returns <code>true</code>
     */
    @Override
    public String getTypeOf() {
        return avoidObjectDetection() ? "undefined" : "function";
    }

    @Override
    public Object get(int index, Scriptable start) {
        return NOT_FOUND;
    }

    @Override
    public boolean has(String name, Scriptable start) {
        return false;
    }

    @Override
    public boolean has(int index, Scriptable start) {
        return false;
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        throw Context.reportRuntimeError("function prohibits modification");
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        throw Context.reportRuntimeError("function prohibits modification");
    }

    @Override
    public void delete(String name) {
        throw Context.reportRuntimeError("function prohibits modification");
    }

    @Override
    public void delete(int index) {
        throw Context.reportRuntimeError("function prohibits modification");
    }

    @Override
    public Object[] getIds() {
        return new Object[0];
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        return this;
    }

    @Override
    public boolean hasInstance(Scriptable instance) {
        return false;
    }

    @Override
    public Object unwrap() {
        return function;
    }
}
