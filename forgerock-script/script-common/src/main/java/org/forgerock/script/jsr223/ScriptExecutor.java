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

package org.forgerock.script.jsr223;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A ScriptExecutor is a placeholder for sample code. This file will be deleted!
 *
 * @author Laszlo Hordos
 */
@Deprecated
public abstract class ScriptExecutor {
    protected final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
    protected final String language;
    protected final String scriptSource;
    protected CompiledScript script = null;
    protected final Bindings globalScope = new SimpleBindings();

    private final long refreshDelay = -1L;

    private long lastModified = -1;

    private final Object lastModifiedMonitor = new Object();

    private String encoding = "UTF-8";

    private final AtomicLong lastModifiedChecked = new AtomicLong(System.currentTimeMillis());

    /*
     * static { if (Class.forName("org.jruby.embed.jsr223.JRubyEngine")) {
     * System.setProperty("org.jruby.embed.localvariable.behavior",
     * "transient"); System.setProperty("org.jruby.embed.localcontext.scope",
     * "threadsafe"); } }
     *
     * ThreadSafeLocalContextProvider
     */

    public ScriptExecutor(String language, String scriptSource) throws ScriptException {
        this.language = null != language ? language : "JavaScript";
        this.scriptSource = scriptSource;

        ScriptEngine scriptEngine = scriptEngineManager.getEngineByName(this.language);
        if (null == scriptEngine) {
            /* error */
        }
        if (scriptEngine instanceof Compilable) {
            Compilable compEngine = (Compilable) scriptEngine;
            script = compEngine.compile(scriptSource);
        }
    }

    protected void init(Object router) {
        globalScope.put("openidm", router);
    }

    public Object execute(Map<String, Object> variables) throws ScriptException {
        try {
            // Now, pass a different script context
            ScriptContext newContext = new SimpleScriptContext();
            newContext.setBindings(globalScope, ScriptContext.GLOBAL_SCOPE);

            Bindings engineScope = newContext.getBindings(ScriptContext.ENGINE_SCOPE);
            // add new variables to the new engineScope
            if (variables != null) {
                for (Map.Entry<String, Object> entry : variables.entrySet()) {
                    engineScope.put(entry.getKey(), entry.getValue());
                }
            }

            if (null != script) {
                if (isModified()) {
                    /* recompile */
                }
                return postProcess(script.eval(newContext));
            } else {
                ScriptEngine scriptEngine = this.scriptEngineManager.getEngineByName(this.language);
                if (null == scriptEngine) {
                    /* error */
                }
                return postProcess(scriptEngine.eval(scriptSource, newContext));
            }
        } catch (ScriptException e) {
            throw e;
        } catch (Exception e) {
            /* error */
        }
        return null;
    }

    public boolean isModified() {
        if (this.refreshDelay < 0) {
            return false;
        }
        long time = System.currentTimeMillis();
        if (this.refreshDelay == 0 || (time - this.lastModifiedChecked.get()) > this.refreshDelay) {
            this.lastModifiedChecked.set(time);
            return this.isSourceModified();
        }
        return false;
    }

    public boolean isSourceModified() {
        synchronized (this.lastModifiedMonitor) {
            return (this.lastModified < 0 || retrieveLastModifiedTime() > this.lastModified);
        }
    }

    /**
     * Subclasses may implement this to provide any special handling required
     *
     * @return
     */
    protected abstract Object postProcess(Object result);

    protected abstract long retrieveLastModifiedTime();
}
