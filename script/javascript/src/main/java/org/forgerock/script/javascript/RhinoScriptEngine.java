/*
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
 *
 * Copyright 2012-2016 ForgeRock AS.
 * Portions Copyrighted 2018-2025 3A Systems, LLC
 */

package org.forgerock.script.javascript;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.script.engine.AbstractScriptEngine;
import org.forgerock.script.engine.CompilationHandler;
import org.forgerock.script.engine.ScriptEngineFactory;
import org.forgerock.script.exception.ScriptCompilationException;
import org.forgerock.script.exception.ScriptThrownException;
import org.forgerock.script.scope.OperationParameter;
import org.forgerock.script.source.ScriptSource;
import org.forgerock.script.source.SourceContainer;
import org.forgerock.script.source.URLScriptSource;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.commonjs.module.ModuleScriptProvider;
import org.mozilla.javascript.commonjs.module.RequireBuilder;
import org.mozilla.javascript.commonjs.module.provider.DefaultUrlConnectionExpiryCalculator;
import org.mozilla.javascript.commonjs.module.provider.ModuleSourceProvider;
import org.mozilla.javascript.commonjs.module.provider.SoftCachingModuleScriptProvider;
import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public class RhinoScriptEngine extends AbstractScriptEngine {
    public static final String CONFIG_DEBUG_PROPERTY = "javascript.debug";
    public static final String CONFIG_RECOMPILE_MINIMUM_INTERVAL_PROPERTY =
            "javascript.recompile.minimumInterval";
    public static final String CONFIG_EXCEPTION_DEBUG_INFO = "javascript.exception.debug.info";

    static final Pattern RHINO_EXCEPTION_FILE_INFO_PATTERN = Pattern.compile("[ ][(].+[)]$");

    /**
     * Setup logging for the {@link RhinoScriptEngine}.
     */
    private static final Logger logger = LoggerFactory.getLogger(RhinoScriptEngine.class);

    private ScriptEngineFactory factory;

    private final ConcurrentMap<String, ScriptCacheEntry> scriptCache =
            new ConcurrentHashMap<String, ScriptCacheEntry>();

    private final long minimumRecompilationInterval;

    private RequireBuilder requireBuilder;

    private ClassLoader classLoader;

    private final ScriptExceptionGenerator scriptExceptionGenerator;

    RhinoScriptEngine(final Map<String, Object> configuration, final ScriptEngineFactory factory,
                      final Collection<SourceContainer> sourceContainers, ClassLoader registryLevelClassLoader) {
        this.factory = factory;
        final JsonValue jsonValueConfig = new JsonValue(configuration);
        initDebugListener(jsonValueConfig.get(CONFIG_DEBUG_PROPERTY).defaultTo(null).asString());
        minimumRecompilationInterval = Long.valueOf(
                jsonValueConfig.get(CONFIG_RECOMPILE_MINIMUM_INTERVAL_PROPERTY).defaultTo("-1").asString());
        scriptExceptionGenerator = jsonValueConfig.get(CONFIG_EXCEPTION_DEBUG_INFO).defaultTo(true).asBoolean()
                ? DEBUG_SCRIPT_EXCEPTION_GENERATOR
                : NON_DEBUG_SCRIPT_EXCEPTION_GENERATOR;
        // Use an Iterable over the SourceContainer collection--that way if it
        // changes (adds, removes, changes)--the new collection is reflected in
        // the UrlModuleSourceProvider.
        final Iterable<URI> sourceContainerURIIterable = new Iterable<URI>() {
            @Override
            public Iterator<URI> iterator() {
                final Iterator<SourceContainer> iter = sourceContainers.iterator();
                return new Iterator<URI>() {
                    @Override
                    public boolean hasNext() {
                        return iter.hasNext();
                    }

                    @Override
                    public URI next() {
                        return iter.next().getSourceURI();
                    }

                    @Override
                    public void remove() {
                        // Not supported
                    }
                };
            }
        };

        // Configure the CommonJS module providers per unoffoicial commonjs author documentation
        // https://groups.google.com/d/msg/mozilla-rhino/HCMh_lAKiI4/P1MA3sFsNKQJ
        ModuleSourceProvider sourceProvider = new UrlModuleSourceProvider(
                sourceContainerURIIterable,
                null,
                minimumRecompilationInterval < 0
                        ? new DefaultUrlConnectionExpiryCalculator(0)
                        : new DefaultUrlConnectionExpiryCalculator(minimumRecompilationInterval),
                null);
        ModuleScriptProvider scriptProvider = new SoftCachingModuleScriptProvider(sourceProvider);
        requireBuilder = new RequireBuilder();
        requireBuilder.setModuleScriptProvider(scriptProvider);

        this.classLoader = registryLevelClassLoader != null
                ? registryLevelClassLoader
                : RhinoScriptEngine.class.getClassLoader();
    }

    private static final class ScriptCacheEntry {
        private final Script compiledScript;
        private final ScriptSource scriptSource;
        private final long lastModified;
        private final long lastCheck;

        private ScriptCacheEntry(final Script compiledScript, final ScriptSource scriptSource,
                                 long lastModified, long lastCheck) {
            this.compiledScript = compiledScript;
            this.scriptSource = scriptSource;
            this.lastModified = lastModified;
            this.lastCheck = lastCheck;
        }
    }

    protected ScriptCacheEntry isSourceNewer(final String name, final ScriptCacheEntry entry)
            throws ScriptException {
        if (minimumRecompilationInterval < 0) {
            return entry;
        }
        long nextSourceCheck = entry.lastCheck + minimumRecompilationInterval;
        long now = System.currentTimeMillis();
        if (nextSourceCheck < now) {
            long newModified = URLScriptSource.getURLRevision(entry.scriptSource.getSource(), null);
            if (entry.lastModified < newModified) {
                synchronized (scriptCache) {
                    try {
                        Script recompiled = compileScript(name, entry.scriptSource.getReader());
                        ScriptCacheEntry newEntry =
                                new ScriptCacheEntry(recompiled, entry.scriptSource, newModified,
                                        now);
                        scriptCache.put(name, newEntry);
                        return newEntry;
                    } catch (IOException e) {
                        throw new ScriptException(e);
                    }
                }
            }
        }
        return entry;
    }

    /**
     * Creates a Script with a given scriptName and binding.
     *
     * @param scriptName
     *            name of the script to run
     * @return the script object
     * @throws ResourceException
     *             if there is a problem accessing the script
     */
    Script createScript(String scriptName) throws ScriptException {
        final ScriptCacheEntry entry = scriptCache.get(scriptName);
        if (null != entry) {
            return isSourceNewer(scriptName, entry).compiledScript;
        }
        throw new ScriptException("Script is not found:" + scriptName);
    }

    public void compileScript(CompilationHandler handler) throws ScriptException {
        try {
            boolean sharedScope = true;// config.get("sharedScope").defaultTo(true).asBoolean();
            handler.setClassLoader(classLoader);
            RhinoScript rhinoScript = null;
            if (handler.getScriptSource() instanceof URLScriptSource) {
                URLScriptSource source = (URLScriptSource) handler.getScriptSource();
                String name = source.getName().getName();
                if (null != source.getSource() && "file".equals(source.getSource().getProtocol())) {
                    name = URLDecoder.decode(source.getSource().getFile(), "utf-8");
                }
                Script script = compileScript(name, source.getReader());
                long now = System.currentTimeMillis();
                scriptCache.put(name, new ScriptCacheEntry(script, source, now, now));
                rhinoScript = new RhinoScript(name, this, requireBuilder, sharedScope);
            } else {
                // TODO Cache the source for debugger
                ScriptSource source = handler.getScriptSource();
                String name = source.getName().getName();
                Script script = compileScript(name, source.getReader());
                rhinoScript = new RhinoScript(name, script, this, requireBuilder, sharedScope);
            }
            handler.setCompiledScript(rhinoScript);
        } catch (ScriptException e) {
            handler.handleException(e);
            throw e;
        } catch (Exception e) {
            handler.handleException(e);
            throw new ScriptException(e);
        }
    }

    private Script compileScript(String name, Reader scriptReader) throws ScriptCompilationException {
        Context cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_ES6);
        try {
            return cx.compileReader(scriptReader, name, 1, null);
        } catch (IOException ioe) {
            throw new ScriptCompilationException(ioe.getMessage(), ioe);
        } catch (RhinoException re) {
            throw getScriptExceptionGenerator().newScriptCompilationException(re);
        } finally {
            Context.exit();
            if (scriptReader != null) {
                try {
                    scriptReader.close();
                } catch (IOException e) {
                    // meaningless exception
                }
            }
        }
    }

    public ScriptEngineFactory getFactory() {
        return factory;
    }

    public OperationParameter getOperationParameter(final org.forgerock.services.context.Context context) {
        return new OperationParameter(context);
    }

    private ContextFactory.Listener debugListener = null;

    private volatile Boolean debugInitialised = null;

    private synchronized void initDebugListener(String configString) {
        if (null == debugInitialised) {
            // Get here only once when the first factory initialised.
            if (null != configString) {
                /*try {
                    if (null == debugListener) {
                        debugListener = new org.eclipse.wst.jsdt.debug.rhino.debugger.RhinoDebugger(configString);
                        Context.enter().getFactory().addListener(debugListener);
                        Context.exit();
                    }
                    ((org.eclipse.wst.jsdt.debug.rhino.debugger.RhinoDebugger) debugListener).start();
                    debugInitialised = Boolean.TRUE;
                } catch (Throwable ex) {
                    // Catch NoClassDefFoundError exception
                    if (!(ex instanceof NoClassDefFoundError)) {
                        // TODO What to do if there is an exception?
                        // throw new
                        // ScriptException("Failed to stop RhinoDebugger", ex);
                        logger.error("RhinoDebugger can not be started", ex);
                    } else {
                        // TODO add logging to WARN about the missing
                        // RhinoDebugger class
                        logger.warn("RhinoDebugger can not be started because the JSDT RhinoDebugger and Transport bundles must be deployed.");
                    }
                }*/
                debugInitialised = null == debugInitialised ? Boolean.FALSE : debugInitialised;
            } else if (false /* TODO How to stop */) {
                try {
                    /*((org.eclipse.wst.jsdt.debug.rhino.debugger.RhinoDebugger) debugListener)
                            .stop();*/
                } catch (Throwable ex) {
                    // We do not care about the NoClassDefFoundError when we
                    // "Stop"
                    if (!(ex instanceof NoClassDefFoundError)) {
                        // TODO What to do if there is an exception?
                        // throw new
                        // ScriptException("Failed to stop RhinoDebugger", ex);
                    }
                } finally {
                    debugInitialised = Boolean.FALSE;
                }
            } else {
                debugInitialised = Boolean.FALSE;
            }
        }
    }

    @Override
    public Bindings compileBindings(org.forgerock.services.context.Context context, Bindings request, Bindings... value) {
        return null;
    }

    // private Script initializeScript(String name, File source, boolean
    // sharedScope)
    // throws ScriptException {
    // initDebugListener();
    // if (debugInitialised) {
    // try {
    // FileChannel inChannel = new FileInputStream(source).getChannel();
    // FileChannel outChannel = new
    // FileOutputStream(getTargetFile(name)).getChannel();
    // FileLock outLock = outChannel.lock();
    // FileLock inLock = inChannel.lock(0, inChannel.size(), true);
    // inChannel.transferTo(0, inChannel.size(), outChannel);
    //
    // outLock.release();
    // inLock.release();
    //
    // inChannel.close();
    // outChannel.close();
    // } catch (IOException e) {
    // logger.warn("JavaScript source was not updated for {}", name, e);
    // }
    // }
    // return new RhinoScript(name, source, sharedScope);
    // }
    //
    // private Script initializeScript(String name, String source, boolean
    // sharedScope)
    // throws ScriptException {
    // initDebugListener();
    // if (debugInitialised) {
    // try {
    // FileChannel outChannel = new
    // FileOutputStream(getTargetFile(name)).getChannel();
    // FileLock outLock = outChannel.lock();
    // ByteBuffer buf = ByteBuffer.allocate(source.length());
    // buf.put(source.getBytes("UTF-8"));
    // buf.flip();
    // outChannel.write(buf);
    // outLock.release();
    // outChannel.close();
    // } catch (IOException e) {
    // logger.warn("JavaScript source was not updated for {}", name, e);
    // }
    // }
    // return new RhinoScript(name, source, sharedScope);
    // }

    /**
     * Interface defining factory for exceptions resulting from script compilation or execution.
     */
    interface ScriptExceptionGenerator {
        /**
         * @param e the RhinoException representing the thrown exception
         * @param scriptThrownExceptionValue the actual exception value thrown by the script, converted to a java object
         * @return a ScriptThrownException representing this exception, and encapsulating the correct level of debug information
         */
        ScriptThrownException newScriptThrownException(RhinoException e, Object scriptThrownExceptionValue);

        /**
         *
         * @param e the RhinoException generated by the script compilation failure
         * @return a ScriptCompilationException representing this failure, and encapsulating the correct level of debug information
         */
        ScriptCompilationException newScriptCompilationException(RhinoException e);

        /**
         *
         * @param e the RhinoException thrown by the RhinoEngine during script execution
         * @return a ScriptException representing this exception, and encapsulating the correct level of debug information.
         */
        ScriptException newScriptException(RhinoException e);
    }

    ScriptExceptionGenerator getScriptExceptionGenerator() {
        return scriptExceptionGenerator;
    }

    private static final ScriptExceptionGenerator DEBUG_SCRIPT_EXCEPTION_GENERATOR = new ScriptExceptionGenerator() {
        @Override
        public ScriptThrownException newScriptThrownException(RhinoException e, Object scriptThrownExceptionValue) {
            return new ScriptThrownException(e.getMessage(), e.sourceName(), e.lineNumber(), e.columnNumber(),
                    scriptThrownExceptionValue);
        }

        @Override
        public ScriptCompilationException newScriptCompilationException(RhinoException e) {
            return new ScriptCompilationException(e.getMessage(), e, e.sourceName(), e.lineNumber(), e.columnNumber());
        }

        @Override
        public ScriptException newScriptException(RhinoException e) {
            return new ScriptException(e.getMessage(), e.sourceName(), e.lineNumber(), e.columnNumber());
        }
    };

    private static final ScriptExceptionGenerator NON_DEBUG_SCRIPT_EXCEPTION_GENERATOR = new ScriptExceptionGenerator() {
        @Override
        public ScriptThrownException newScriptThrownException(RhinoException e, Object scriptThrownExceptionValue) {
            return new ScriptThrownException(stripDebugInformationFromRhinoExceptionMessage(e), scriptThrownExceptionValue);
        }

        @Override
        public ScriptCompilationException newScriptCompilationException(RhinoException e) {
            return new ScriptCompilationException(stripDebugInformationFromRhinoExceptionMessage(e));
        }

        @Override
        public ScriptException newScriptException(RhinoException e) {
            return new ScriptException(stripDebugInformationFromRhinoExceptionMessage(e));
        }

        /*
        The Rhino runtime can include script debug information in the exception message. The format of this included information
        can be found in RhinoException#getMessage. The pattern below will match this information, if present, and return only
        the preceeding message state.
         */
        private String stripDebugInformationFromRhinoExceptionMessage(RhinoException e) {
            return RHINO_EXCEPTION_FILE_INFO_PATTERN.split(e.getMessage())[0];
        }
    };
}