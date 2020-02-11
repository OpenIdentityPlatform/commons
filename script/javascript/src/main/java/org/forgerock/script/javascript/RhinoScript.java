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

package org.forgerock.script.javascript;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureClassLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.forgerock.json.resource.ResourceException;
import org.forgerock.script.engine.CompiledScript;
import org.forgerock.script.engine.Utils;
import org.forgerock.script.exception.ScriptThrownException;
import org.forgerock.script.registry.ThreadClassLoaderManager;
import org.forgerock.script.scope.FunctionFactory;
import org.forgerock.script.scope.OperationParameter;
import org.forgerock.script.scope.Parameter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrappedException;
import org.mozilla.javascript.commonjs.module.RequireBuilder;
import org.mozilla.javascript.tools.shell.Global;
import org.mozilla.javascript.tools.shell.QuitAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class RhinoScript implements CompiledScript {

    /**
     * Setup logging for the {@link RhinoScript}.
     */
    final static Logger logger = LoggerFactory.getLogger(RhinoScript.class);

    /**
     * A sealed shared scope to improve performance; avoids allocating standard
     * objects on every exec call.
     */
    private/* static */ScriptableObject topSharedScope = null; // lazily initialized

    /** The script level scope to use */
    private Scriptable scriptScope = null;

    /** The compiled script to execute. */
    private final Script script;

    /** The script name */
    private final String scriptName;

    /** The parent ScriptEngine */
    private final RhinoScriptEngine engine;

    /** The CommonJS module builder for Require instances. */
    private final RequireBuilder requireBuilder;

    public static final Global GLOBAL = new Global();

    static {
        GLOBAL.initQuitAction(new IProxy());
    }

    /**
     * Proxy class to avoid proliferation of anonymous classes.
     */
    private static class IProxy implements QuitAction {
        public void quit(Context cx, int exitCode) {
            /* no quit :) */
        }
    }

    /** Indicates if this script instance should use the shared scope. */
    private final boolean sharedScope;

    /**
     * Compiles the JavaScript source code into an executable script. If
     * {@code useSharedScope} is {@code true}, then a sealed shared scope
     * containing standard JavaScript objects (Object, String, Number, Date,
     * etc.) will be used for script execution; otherwise a new unsealed scope
     * will be allocated for each execution.
     *
     *
     * @param compiledScript
     *            the source code of the JavaScript script.
     * @param sharedScope
     *            if {@code true}, uses the shared scope, otherwise allocates
     *            new scope.
     * @throws ScriptException
     *             if there was an exception encountered while compiling the
     *             script.
     */
    public RhinoScript(String name, Script compiledScript, final RhinoScriptEngine engine, RequireBuilder requireBuilder,
            boolean sharedScope) throws ScriptException {
        this.scriptName = name;
        this.sharedScope = sharedScope;
        this.engine = engine;
        this.requireBuilder = requireBuilder;
        Context cx = Context.enter();
        try {
            scriptScope = getScriptScope(cx);
            script = compiledScript;
            // script = cx.compileString(source, name, 1, null);
        } catch (RhinoException re) {
            throw new ScriptException(re.getMessage());
        } finally {
            Context.exit();
        }
    }

    /**
     * TEMPORARY
     */
    public RhinoScript(String name, final RhinoScriptEngine engine, RequireBuilder requireBuilder, boolean sharedScope)
            throws ScriptException {
        this.scriptName = name;
        this.sharedScope = sharedScope;
        this.engine = engine;
        this.requireBuilder = requireBuilder;
        Context cx = Context.enter();
        try {
            scriptScope = getScriptScope(cx);
            script = null;// cx.compileReader(reader, name, 1, null);
        } catch (RhinoException re) {
            throw new ScriptException(re);
        } finally {
            Context.exit();
        }
    }

    /**
     * Gets the JavaScript standard objects, either as the shared sealed scope
     * or as a newly allocated set of standard objects, depending on the value
     * of {@code useSharedScope}.
     *
     * @param context
     *            The runtime context of the executing script.
     * @return the JavaScript standard objects.
     */
    // TODO Load RhinoTopLevel scope
    private ScriptableObject getStandardObjects(final Context context) {
        if (!sharedScope) {
            // somewhat expensive
            ScriptableObject scope = context.initStandardObjects();
            installRequire(context, scope);
            return scope;
        }
        // lazy initialization race condition is harmless
        if (topSharedScope == null) {
            Global scope = new Global(context);
            scope.initQuitAction(new IProxy());
            // ScriptableList.init(scope, false);

            context.setApplicationClassLoader(new InnerClassLoader(context.getApplicationClassLoader()));
            InputStream init = RhinoScript.class.getResourceAsStream("/resources/init.js");
            if (null != init) {
                try {
                    context.evaluateString(scope, Utils.readStream(init), "/resources/init.js", 1, null);
                } catch (IOException e) {
                    logger.error("Failed to evaluate init.js", e);
                }
            }
            addLoggerProperty(scope);
            installRequire(context, scope);
            // seal the whole scope (not just standard objects)
            scope.sealObject();
            topSharedScope = scope;
        }
        return topSharedScope;
    }

    // install require function per unofficial CommonJS author documentation
    // https://groups.google.com/d/msg/mozilla-rhino/HCMh_lAKiI4/P1MA3sFsNKQJ
    private void installRequire(final Context context, final ScriptableObject scope) {
        requireBuilder.createRequire(context, scope).install(scope);
    }

    /**
     * Get the scope scriptable re-used for this script Holds common
     * functionality such as the logger
     *
     * @param context
     *            The runtime context of the executing script.
     * @return the context scriptable for this script
     */
    private Scriptable getScriptScope(final Context context) {
        Scriptable topLevel = getStandardObjects(context);
        Scriptable scriptScopeScriptable = context.newObject(topLevel);

        // standard objects included with every box
        scriptScopeScriptable.setPrototype(topLevel);
        scriptScopeScriptable.setParentScope(null);
        return scriptScopeScriptable;
    }

    /**
     * Add the logger property to the JavaScript scope
     *
     * @param scope
     *            to add the property to
     */
    private void addLoggerProperty(Scriptable scope) {
        String loggerName = "org.forgerock.script.javascript.JavaScript." + trimPath(scriptName);
        scope.put("logger", scope, Converter.wrap(null, FunctionFactory.getLogger(loggerName), scope, false));
    }

    private String trimPath(String name) {
        return name.indexOf("/") != -1 ? name.substring(name.lastIndexOf("/") + 1) : name;
    }

    public Bindings prepareBindings(org.forgerock.services.context.Context context, Bindings request, Bindings... scopes) {
        // TODO Fix it later
        return new SimpleBindings();
    }

    @Override
    public Object eval(final org.forgerock.services.context.Context ctx, Bindings request, Bindings... scopes)
            throws ScriptException {

        Context context = Context.enter();
        try {
            Scriptable outer = context.newObject(getStandardObjects(context));

            final OperationParameter operationParameter = engine.getOperationParameter(ctx);
            Context.getCurrentContext().putThreadLocal(Parameter.class.getName(),
                    operationParameter);

            Set<String> safeAttributes = null != request ? request.keySet() : Collections.EMPTY_SET;
            Map<String, Object> scope = new HashMap<String, Object>();
            for (Map<String, Object> next : scopes) {
                if (null == next) {
                    continue;
                }
                for (Map.Entry<String, Object> entry : next.entrySet()) {
                    if (scope.containsKey(entry.getKey())
                            || safeAttributes.contains(entry.getKey())) {
                        continue;
                    }
                    long index = ScriptRuntime.indexFromString(entry.getKey());
                    if (index < 0) {
                        outer.put(entry.getKey(), outer, Converter.wrap(operationParameter, entry
                                .getValue(), outer, true));
                    } else {
                        outer.put((int) index, outer, Converter.wrap(operationParameter, entry
                                .getValue(), outer, true));
                    }
                }
            }

            if (null != request) {
                for (Map.Entry<String, Object> entry : request.entrySet()) {
                    long index = ScriptRuntime.indexFromString(entry.getKey());
                    if (index < 0) {
                        outer.put(entry.getKey(), outer, Converter.wrap(operationParameter, entry
                                .getValue(), outer, false));
                    } else {
                        outer.put((int) index, outer, Converter.wrap(operationParameter, entry
                                .getValue(), outer, false));
                    }
                }
            }

            outer.setPrototype(scriptScope); // script level context and
                                             // standard objects included with
                                             // every box
            outer.setParentScope(null);
            Scriptable inner = context.newObject(outer); // inner transient
                                                         // scope for new
                                                         // properties
            inner.setPrototype(outer);
            inner.setParentScope(null);

            final Script scriptInstance = null != script ? script : engine.createScript(scriptName);
            Object result = Converter.convert(scriptInstance.exec(context, inner));
            return result; // Context.jsToJava(result, Object.class);
        } catch (ScriptException e) {
            throw e;
        } catch (WrappedException e) {
            if (e.getWrappedException() instanceof ResourceException) {
                throw new ScriptThrownException(e.getMessage(), e.sourceName(), e.lineNumber(), e.columnNumber(),
                        ((ResourceException) e.getWrappedException()).toJsonValue().getObject());
            } else {
                ScriptException exception =
                        new ScriptThrownException(e.getMessage(), e.sourceName(), e.lineNumber(), e.columnNumber(),
                                e.getWrappedException());
                exception.initCause(e.getWrappedException());
                throw exception;
            }
        } catch (JavaScriptException e) {
            logger.debug("Failed to evaluate {} script.", scriptName, e);
            ScriptThrownException exception =
                    new ScriptThrownException(e.getMessage(), e.sourceName(), e.lineNumber(), e.columnNumber(),
                            Converter.convert(e.getValue()));
            exception.initCause(e);
            throw exception;
        } catch (RhinoException e) {
            logger.debug("Failed to evaluate {} script.", scriptName, e);
            // some other runtime exception encountered
            final ScriptException exception =
                    new ScriptException(e.getMessage(), e.sourceName(), e.lineNumber(), e.columnNumber());
            exception.initCause(e);
            throw exception;
        } catch (Exception e) {
            logger.debug("Failed to evaluate {} script.", scriptName, e);
            throw new ScriptException(e);
        } finally {
            Context.getCurrentContext().removeThreadLocal(Parameter.class.getName());
            Context.exit();
        }
    }

    private static class InnerClassLoader extends SecureClassLoader {

        public InnerClassLoader(ClassLoader parent) {
            super(parent);
        }

        public Class<?> loadClass(String name) throws ClassNotFoundException {
            // First check whether it's already been loaded, if so use it
            Class loadedClass = Kit.classOrNull(getParent(), name);

            // Not loaded, try to load it
            if (loadedClass == null) {
                loadedClass =
                        ThreadClassLoaderManager.getInstance().getCurrentClassLoader().loadClass(
                                name);
            }
            // will never return null (ClassNotFoundException will be thrown)
            return loadedClass;
        }
    }

}
