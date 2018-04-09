/*
 * Original source;
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 * Use is subject to license terms.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met: Redistributions of source code
 * must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials
 * provided with the distribution. Neither the name of the Sun Microsystems nor the names of
 * is contributors may be used to endorse or promote products derived from this software
 * without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Subsequent changes:
 * Copyright 2006-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 *  Portions Copyrighted 2012-2014 ForgeRock AS.
 */

package org.forgerock.script.groovy;

import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.codehaus.groovy.runtime.MetaClassHelper;
import org.codehaus.groovy.runtime.MethodClosure;
import org.codehaus.groovy.control.CompilationFailedException;
import org.forgerock.services.context.Context;
import org.forgerock.script.engine.CompiledScript;
import org.forgerock.script.exception.ScriptCompilationException;
import org.forgerock.script.exception.ScriptThrownException;
import org.forgerock.script.scope.AbstractFactory;
import org.forgerock.script.scope.Function;
import org.forgerock.script.scope.OperationParameter;
import org.forgerock.script.scope.Parameter;
import org.forgerock.util.Factory;
import org.forgerock.util.LazyMap;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.DelegatingMetaClass;
import groovy.lang.MetaClass;
import groovy.lang.MissingMethodException;
import groovy.lang.Script;
import groovy.lang.Tuple;
import groovy.util.ResourceException;

/**
 * A JavaScript script.
 * <p>
 * This implementation pre-compiles the provided script. Any syntax errors in
 * the source code will throw an exception during construction of the object.
 * <p>
 *
 * @author Paul C. Bryan
 * @author aegloff
 */
public class GroovyScript implements CompiledScript {

    private final String scriptName;

    private final GroovyScriptEngineImpl engine;

    public GroovyScript(String scriptName, final GroovyScriptEngineImpl groovyEngine)
            throws IllegalAccessException, InstantiationException, ResourceException,
            groovy.util.ScriptException {
        this.scriptName = scriptName;
        engine = groovyEngine;
        engine.createScript(scriptName, new Binding());
    }

    public Bindings prepareBindings(final Context context, final Bindings request,
            final Bindings... scopes) {
        final Map<String, Object> b = mergeBindings(context, request, scopes);
        return b instanceof Bindings ? (Bindings) b : new SimpleBindings(b);
    }

    public Object eval(final Context context, final Bindings request, final Bindings... scopes)
            throws ScriptException {

        final Map<String, Object> bindings = mergeBindings(context, request, scopes);

        // Bindings so script has access to this environment.
        // Only initialize once.
        if (null == bindings.get("context")) {
            // add context to bindings
            // ctx.setAttribute("context", ctx);

            // direct output to ctx.getWriter
            // If we're wrapping with a PrintWriter here,
            // enable autoFlush because otherwise it might not get done!
            final Writer writer = engine.getWriter();
            bindings.put("out", (writer instanceof PrintWriter) ? writer : new PrintWriter(writer,
                    true));

            // Not going to do this after all (at least for now).
            // Scripts can use context.{reader, writer, errorWriter}.
            // That is a modern version of System.{in, out, err} or
            // Console.{reader, writer}().
            //
            // // New I/O names consistent with ScriptContext and
            // java.io.Console.
            //
            // ctx.setAttribute("writer", writer, ScriptContext.ENGINE_SCOPE);
            //
            // // Direct errors to ctx.getErrorWriter
            // final Writer errorWriter = ctx.getErrorWriter();
            // ctx.setAttribute("errorWriter", (errorWriter instanceof
            // PrintWriter) ?
            // errorWriter :
            // new PrintWriter(errorWriter),
            // ScriptContext.ENGINE_SCOPE);
            //
            // // Get input from ctx.getReader
            // // We don't wrap with BufferedReader here because we expect that
            // if
            // // the host wants that they do it. Either way Groovy scripts will
            // // always have readLine because the GDK supplies it for Reader.
            // ctx.setAttribute("reader", ctx.getReader(),
            // ScriptContext.ENGINE_SCOPE);
        }

        // Fix for GROOVY-3669: Can't use several times the same JSR-223
        // ScriptContext for differents groovy script
        // if (ctx.getWriter() != null) {
        // ctx.setAttribute("out", new PrintWriter(ctx.getWriter(), true),
        // DefaultScriptContext.REQUEST_SCOPE);
        // }

        try {
            Script scriptObject = engine.createScript(scriptName, new Binding(bindings));

            // create a Map of MethodClosures from this new script object
            Method[] methods = scriptObject.getClass().getMethods();
            final Map<String, Closure> closures = new HashMap<String, Closure>();
            for (Method m : methods) {
                String name = m.getName();
                closures.put(name, new MethodClosure(scriptObject, name));
            }

            MetaClass oldMetaClass = scriptObject.getMetaClass();

            /*
             * We override the MetaClass of this script object so that we can
             * forward calls to global closures (of previous or future "eval"
             * calls) This gives the illusion of working on the same "global"
             * scope.
             */
            scriptObject.setMetaClass(new DelegatingMetaClass(oldMetaClass) {
                @Override
                public Object invokeMethod(Object object, String name, Object args) {
                    if (args == null) {
                        return invokeMethod(object, name, MetaClassHelper.EMPTY_ARRAY);
                    }
                    if (args instanceof Tuple) {
                        return invokeMethod(object, name, ((Tuple) args).toArray());
                    }
                    if (args instanceof Object[]) {
                        return invokeMethod(object, name, (Object[]) args);
                    } else {
                        return invokeMethod(object, name, new Object[] { args });
                    }
                }

                @Override
                public Object invokeMethod(Object object, String name, Object[] args) {
                    try {
                        return super.invokeMethod(object, name, args);
                    } catch (MissingMethodException mme) {
                        return callGlobal(name, args, bindings);
                    }
                }

                @Override
                public Object invokeStaticMethod(Object object, String name, Object[] args) {
                    try {
                        return super.invokeStaticMethod(object, name, args);
                    } catch (MissingMethodException mme) {
                        return callGlobal(name, args, bindings);
                    }
                }

                private Object callGlobal(String name, Object[] args, Map<String, Object> ctx) {
                    Closure closure = closures.get(name);
                    if (closure != null) {
                        return closure.call(args);
                    } else {
                        // Look for closure valued variable in the
                        // given ScriptContext. If available, call it.
                        Object value = ctx.get(name);
                        if (value instanceof Closure) {
                            return ((Closure) value).call(args);
                        } // else fall thru..
                    }
                    throw new MissingMethodException(name, getClass(), args);
                }
            });

            try {
                return scriptObject.run();
            } catch (UndeclaredThrowableException e){
                if (e.getUndeclaredThrowable() instanceof org.forgerock.json.resource.ResourceException) {
                    org.forgerock.json.resource.ResourceException resourceException =
                            (org.forgerock.json.resource.ResourceException) e
                                    .getUndeclaredThrowable();
                    throw new ScriptThrownException(resourceException, resourceException
                            .toJsonValue().asMap());
                } else if (e.getUndeclaredThrowable() instanceof ScriptException) {
                    throw (ScriptException) e.getUndeclaredThrowable();
                } else if (e.getUndeclaredThrowable() instanceof Exception) {
                    throw new ScriptThrownException((Exception) e.getUndeclaredThrowable(), null);
                } else {
                    throw new ScriptThrownException(e, e.getUndeclaredThrowable());
                }
            } catch (Exception e) {
                throw new ScriptThrownException(e.getMessage(), e);
            }
        } catch (ScriptException e) {
            throw e;
        } catch (CompilationFailedException e) {
            throw new ScriptCompilationException(e.getMessage(), e);
        } catch (Exception e) {
            throw new ScriptException(e);
        } finally {
            // Fix for GROOVY-3669: Can't use several times the same JSR-223
            // ScriptContext for different groovy script
            // Groovy's scripting engine implementation adds those two variables
            // in the binding
            // but should clean up afterwards
            // ctx.removeAttribute("context",
            // DefaultScriptContext.REQUEST_SCOPE);
            // ctx.removeAttribute("out", DefaultScriptContext.REQUEST_SCOPE);
        }
    }

    private Map<String, Object> mergeBindings(final Context context, final Bindings request,
            final Bindings... scopes) {
        Set<String> safeAttributes = null != request ? request.keySet() : Collections.EMPTY_SET;
        Map<String, Object> scope = new HashMap<String, Object>();
        for (Map<String, Object> next : scopes) {
            if (null == next) {
                continue;
            }
            for (Map.Entry<String, Object> entry : next.entrySet()) {
                if (scope.containsKey(entry.getKey()) || safeAttributes.contains(entry.getKey())) {
                    continue;
                }
                scope.put(entry.getKey(), entry.getValue());
            }
        }
        // Make lazy deep copy
        if (!scope.isEmpty()) {
            scope = new LazyMap<String, Object>(new InnerMapFactory(scope, new OperationParameter(context)));
        }

        if (null == request || request.isEmpty()) {
            return scope;
        } else if (scope.isEmpty()) {
            return request;
        } else {
            request.putAll(scope);
            return request;
        }
    }

    public static class InnerMapFactory extends AbstractFactory.MapFactory {

        private final Parameter parameter;

        public InnerMapFactory(final Map<String, Object> source, final Parameter parameter) {
            super(source);
            this.parameter = parameter;
        }

        protected Factory<List<Object>> newListFactory(final List<Object> source) {
            return new InnerListFactory(source, parameter);
        }

        protected Factory<Map<String, Object>> newMapFactory(final Map<String, Object> source) {
            return new InnerMapFactory(source, parameter);
        }

        protected Object convertFunction(final Function<?> source) {
            return new FunctionClosure(null, parameter, source);
        }

        public Parameter getParameter() {
            return parameter;
        }
    }

    public static class InnerListFactory extends AbstractFactory.ListFactory {

        private final Parameter parameter;

        public InnerListFactory(final List<Object> source, final Parameter parameter) {
            super(source);
            this.parameter = parameter;
        }

        protected Factory<List<Object>> newListFactory(final List<Object> source) {
            return new InnerListFactory(source, parameter);
        }

        protected Factory<Map<String, Object>> newMapFactory(final Map<String, Object> source) {
            return new InnerMapFactory(source, parameter);
        }

        protected Object convertFunction(final Function<?> source) {
            return new FunctionClosure(null, parameter, source);
        }

        public Parameter getParameter() {
            return parameter;
        }
    }
}
