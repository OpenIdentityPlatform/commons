/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock AS. All rights reserved.
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

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.Script;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceConnector;
import groovy.util.ResourceException;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.forgerock.services.context.Context;
import org.forgerock.script.engine.AbstractScriptEngine;
import org.forgerock.script.engine.CompilationHandler;
import org.forgerock.script.engine.ScriptEngineFactory;
import org.forgerock.script.exception.ScriptCompilationException;
import org.forgerock.script.source.URLScriptSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.ScriptException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public class GroovyScriptEngineImpl extends AbstractScriptEngine {

    /**
     * Setup logging for the {@link GroovyScriptEngineImpl}.
     */
    private static final Logger logger = LoggerFactory.getLogger(GroovyScriptEngineImpl.class);

    private final ScriptEngineFactory factory;

    private final GroovyScriptEngine groovyScriptEngine;

    private final GroovyClassLoader loader;

    private final ConcurrentMap<String, Class> scriptCache = new ConcurrentHashMap<String, Class>();
    private final ConcurrentMap<String, URL> sourceCache = new ConcurrentHashMap<String, URL>();

    private static final String DOT_STAR = ".*";
    private static final String EMPTY_STRING = "";

    GroovyScriptEngineImpl(Map<String, Object> configuration, final ScriptEngineFactory factory) {
        this.factory = factory;

        /*
         * groovy -cp slf4j-api.jar -configscript config.groovy myscript.groovy
         *
         * and in config.groovy:
         *
         * withConfig(configuration) { ast(groovy.util.logging.Slf4j) }
         */

        Properties properties = null;
        for (Map.Entry<String, Object> entry : configuration.entrySet()) {
            if (entry.getKey().startsWith("groovy.") && entry.getValue() instanceof String) {
                if (null == properties) {
                    properties = new Properties();
                }
                properties.put(entry.getKey(), entry.getValue());
            }
        }
        final CompilerConfiguration config =
                null != properties ? new CompilerConfiguration(properties)
                        : new CompilerConfiguration();

        config.addCompilationCustomizers(GroovyScriptEngineImpl
                .getImportCustomizer((ImportCustomizer) configuration.get(ImportCustomizer.class
                        .getCanonicalName())));

        Object jointCompilationOptions = configuration.get("jointCompilationOptions");
        if (jointCompilationOptions instanceof Map) {
            config.setJointCompilationOptions((Map<String, Object>) jointCompilationOptions);
        }
        this.loader = new GroovyClassLoader(getParentLoader(), config, true);
        groovyScriptEngine = new GroovyScriptEngine(new ResourceConnector() {
            @Override
            public URLConnection getResourceConnection(String resourceName)
                    throws ResourceException {
                URL source = sourceCache.get(resourceName);
                if (null != source) {
                    try {
                        URLConnection groovyScriptConn = source.openConnection();

                        // Make sure we can open it, if we can't it doesn't
                        // exist.
                        // Could be very slow if there are any non-file:// URLs
                        // in there

                        groovyScriptConn.getInputStream();
                        return groovyScriptConn;
                    } catch (IOException e) {
                        throw new ResourceException("Cannot open URL: " + source.toString(), e);
                    }
                }

                throw new ResourceException("No resource for " + resourceName + " was found");
            }
        }, loader);
        groovyScriptEngine.setConfig(config);
    }

    /**
     * Creates a Script with a given scriptName and binding.
     *
     * @param scriptName
     *            name of the script to run
     * @param binding
     *            the binding to pass to the script
     * @return the script object
     * @throws ResourceException
     *             if there is a problem accessing the script
     * @throws groovy.util.ScriptException
     *             if there is a problem parsing the script
     */
    Script createScript(String scriptName, Binding binding) throws ResourceException,
            groovy.util.ScriptException {

        Class clazz = scriptCache.get(scriptName);
        if (clazz == null) {
            // Load from URL
            return groovyScriptEngine.createScript(scriptName, binding);
        } else {
            return InvokerHelper.createScript(clazz, binding);
        }
    }

    static ImportCustomizer getImportCustomizer(ImportCustomizer parent) {
        final ImportCustomizer ic = null != parent ? parent : new ImportCustomizer();
        for (final String imp : getImports()) {
            ic.addStarImports(imp.replace(DOT_STAR, EMPTY_STRING));
        }
        // ic.addImports("org.forgerock.script.scope");
        // ic.addStaticImport(ConnectionFunction.class.getName(),
        // ConnectionFunction.CREATE.toString());
        return ic;
    }

    public void compileScript(CompilationHandler handler) throws ScriptException {
        try {
            handler.setClassLoader(groovyScriptEngine.getGroovyClassLoader());

            GroovyCodeSource codeSource = null;
            if (handler.getScriptSource() instanceof URLScriptSource) {
                URL source = ((URLScriptSource) handler.getScriptSource()).getSource();
                try {
                    codeSource = new GroovyCodeSource(source);
                    sourceCache.put(codeSource.getName(), source);
                } catch (IllegalArgumentException e) {
                    logger.trace("Groovy source at {} is not file", source);
                }
            }

            if (null == codeSource) {
                // TODO write to cache file
                codeSource =
                        new GroovyCodeSource(handler.getScriptSource().getReader(), handler
                                .getScriptSource().getName().getName(), handler.getScriptSource()
                                .getName().getName());
                scriptCache.put(codeSource.getName(), groovyScriptEngine.getGroovyClassLoader()
                        .parseClass(codeSource));
            } else {
                groovyScriptEngine.getGroovyClassLoader().parseClass(codeSource);
            }

            handler.setCompiledScript(new GroovyScript(codeSource.getName(), this));
        } catch (CompilationFailedException e) {
            handler.handleException(e);
            throw new ScriptCompilationException(e.getMessage(), e);
        } catch (Exception e) {
            handler.handleException(e);
            throw new ScriptException(e);
        }
    }

    public ScriptEngineFactory getFactory() {
        return factory;
    }

    // determine appropriate class loader to serve as parent loader
    // for GroovyClassLoader instance
    private ClassLoader getParentLoader() {
        // check whether thread context loader can "see" Groovy Script class
        ClassLoader ctxtLoader = Thread.currentThread().getContextClassLoader();
        try {
            Class c = ctxtLoader.loadClass(Script.class.getName());
            if (c == Script.class) {
                return ctxtLoader;
            }
        } catch (ClassNotFoundException cnfe) {
            /* ignore */
        }
        // exception was thrown or we get wrong class
        return Script.class.getClassLoader();
    }

    public Bindings compileBindings(Context context, Bindings request, Bindings... value) {
        return null;
    }

}
