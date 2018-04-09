/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 ForgeRock AS. All Rights Reserved
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

package org.forgerock.script.registry;

import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.script.Scope;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptContext;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptEvent;
import org.forgerock.script.ScriptListener;
import org.forgerock.script.ScriptName;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.script.engine.CompilationHandler;
import org.forgerock.script.engine.CompiledScript;
import org.forgerock.script.engine.ScriptEngine;
import org.forgerock.script.engine.ScriptEngineFactory;
import org.forgerock.script.engine.ScriptEngineFactoryObserver;
import org.forgerock.script.engine.Utils;
import org.forgerock.script.exception.ScriptCompilationException;
import org.forgerock.script.source.EmbeddedScriptSource;
import org.forgerock.script.source.ScriptEngineFactoryAware;
import org.forgerock.script.source.ScriptSource;
import org.forgerock.script.source.SourceContainer;
import org.forgerock.script.source.SourceUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A ScriptRegistryImpl does ...
 *
 * @author Laszlo Hordos
 */
public class ScriptRegistryImpl implements ScriptRegistry, ScriptEngineFactoryObserver {

    public static final String SCRIPT_CACHE_DIR = "script.cache.dir";

    /**
     * Setup logging for the {@link ScriptRegistryImpl}.
     */
    private static final Logger logger = LoggerFactory.getLogger(ScriptRegistryImpl.class);

    private final Set<ScriptEngineFactory> engineFactories;

    private final Map<ScriptEngineFactory, ScriptEngine> engines =
            new HashMap<ScriptEngineFactory, ScriptEngine>();

    private final ConcurrentHashMap<ScriptName, LibraryRecord> cache =
            new ConcurrentHashMap<ScriptName, LibraryRecord>();

    private final LinkedHashMap<ScriptName, SourceContainer> sourceCache =
            new LinkedHashMap<ScriptName, SourceContainer>();

    private final ReadWriteLock sourceCacheLock = new ReentrantReadWriteLock();

    private Map<String, Object> properties;
    
    private ClassLoader registryLevelScriptClassLoader;

    /**
     * This is the global scope bindings. By default, a null value (which means
     * no global scope) is used.
     */
    protected final AtomicReference<Bindings> globalScope;

    public ScriptRegistryImpl() {
        this(new HashMap<String, Object>(0), null, null, null);
    }
    
    public ScriptRegistryImpl(final Map<String, Object> properties,
            final Iterable<ScriptEngineFactory> engine, final Bindings globalScope) {
        this(properties, engine, globalScope, null);
    }

    public ScriptRegistryImpl(final Map<String, Object> properties,
            final Iterable<ScriptEngineFactory> engine, final Bindings globalScope,
            final ClassLoader registryLevelScriptClassLoader) {
        this.properties = properties;
        this.engineFactories = new HashSet<ScriptEngineFactory>();
        if (null != engine) {
            for (ScriptEngineFactory factory : engine) {
                engineFactories.add(factory);
            }
        }
        this.globalScope =
                globalScope != null ? new AtomicReference<Bindings>(globalScope)
                        : new AtomicReference<Bindings>();

        this.registryLevelScriptClassLoader = registryLevelScriptClassLoader;
                
        // properties.get(SCRIPT_CACHE_DIR);
    }

    protected void setConfiguration(final Map<String, Object> configuration) {
        if (null == configuration) {
            throw new NullPointerException();
        }
        synchronized (engines) {
            properties = configuration;
            for (Map.Entry<ScriptEngineFactory, ScriptEngine> entry : engines.entrySet()) {
                engines.put(entry.getKey(), initializeScriptEngine(entry.getKey()));
            }
        }
    }

    @Override
    public ClassLoader getRegistryLevelScriptClassLoader() {
        return this.registryLevelScriptClassLoader;
    }

    @Override
    public void setRegistryLevelScriptClassLoader(final ClassLoader registryLevelScriptClassLoader) {
        this.registryLevelScriptClassLoader = registryLevelScriptClassLoader;
    }

    public void put(String key, Object value) {
        if (null == globalScope.get()) {
            globalScope.set(createBindings());
        }
        globalScope.get().put(key, value);
    }

    public Object get(String key) {
        if (getBindings() != null) {
            return getBindings().get(key);
        }
        return null;
    }

    public Bindings getBindings() {
        return globalScope.get();
    }

    public void setBindings(Bindings bindings) {
        this.globalScope.set(bindings);
    }

    public Bindings createBindings() {
        return new SimpleBindings();
    }

    // ScriptRegistry Methods

    public Set<ScriptName> listScripts() {
        return Collections.unmodifiableSet(cache.keySet());
    }

    public ScriptEntry takeScript(String name) throws ScriptException {
        return takeScript(new ScriptName(name, SourceUnit.AUTO_DETECT));
    }

    public ScriptEngine getEngineByName(String shortName) {
        return findScriptEngine(shortName);
    }

    public ScriptEntry takeScript(JsonValue script) throws ScriptException {
        if (null == script || script.expect(Map.class).isNull()) {
            throw new NullPointerException("Null scriptValue");
        }
        JsonValue name = script.get(SourceUnit.ATTR_NAME);
        JsonValue type = script.get(SourceUnit.ATTR_TYPE);
        JsonValue source = script.get(SourceUnit.ATTR_SOURCE);
        JsonValue requestBinding = script.get(SourceUnit.ATTR_REQUEST_BINDING);

        if (!source.isNull()
                && (type.isNull() || type.expect(String.class).asString().equals(
                        SourceUnit.AUTO_DETECT))) {
            throw new IllegalArgumentException("Embedded script must have type");
        }

        ScriptName scriptName =
                new ScriptName(
                        name.isNull() || !name.isString() ? UUID.randomUUID().toString() : name.asString(),
                        type.isNull() || !type.isString() ? SourceUnit.AUTO_DETECT : type.asString(),
                        null,
                        requestBinding.isNull() || !requestBinding.isString() ? null : requestBinding.asString());

        if (!source.isNull()) {
            JsonValue visibility = script.get(SourceUnit.ATTR_VISIBILITY);
            if (visibility.isNull()) {
                addSourceUnit(new EmbeddedScriptSource(source.asString(), scriptName));
            } else {
                addSourceUnit(new EmbeddedScriptSource(visibility
                        .asEnum(ScriptEntry.Visibility.class), source.asString(), scriptName));
            }
        }
        ScriptEntry scriptEntry = takeScript(scriptName);

        if (null == scriptEntry) {
            throw new ScriptException("Script source not found:" + scriptName);
        }

        for (Map.Entry<String, Object> entry : script.asMap().entrySet()) {
            if (SourceUnit.ATTR_NAME.equals(entry.getKey())
                    || SourceUnit.ATTR_TYPE.equals(entry.getKey())
                    || SourceUnit.ATTR_SOURCE.equals(entry.getKey())
                    || SourceUnit.ATTR_VISIBILITY.equals(entry.getKey())
                    || entry.getKey().startsWith("_")) {
                continue;
            }
            scriptEntry.put(entry.getKey(), entry.getValue());
        }

        return scriptEntry;
    }

    public synchronized ScriptEntry takeScript(ScriptName name) throws ScriptException {
        LibraryRecord rec = cache.get(name);
        if (null != rec) {
            return rec.getScriptEntry();
        }
        ScriptEntry result = null;
        ScriptSource source = findScriptSource(name);
        if (null != source) {
            addSourceUnit(source);
            result = takeScript(name);
        }
        return result;
    }

    @SuppressWarnings({ "unchecked" })
    private ScriptEngine findScriptEngine(String shortName) {
        ScriptEngine engine = null;
        if (null != shortName) {
            ScriptEngineFactory factory = Utils.findScriptEngineFactory(shortName, engineFactories);
            if (null != factory) {
                engine = engines.get(factory);
                if (null == engine) {
                    synchronized (engines) {
                        if (null == engine) {
                            for (ScriptEngineFactory f : engines.keySet()) {
                                if (f.getLanguageName().equalsIgnoreCase(factory.getLanguageName())) {
                                    // Avoid the duplicated factories for the same language!
                                    return engines.get(f);
                                }
                            }
                            engine = initializeScriptEngine(factory);
                            engines.put(factory, engine);
                        }
                    }
                }
            }
        }
        return engine;
    }

    private ScriptEngine initializeScriptEngine(ScriptEngineFactory factory) {
        // TODO Make the initialization type safe!!
        Object o = properties.get(factory.getLanguageName());
        Map<String, Object> configuration = null;
        if (o instanceof Map) {
            configuration = (Map<String, Object>) o;
        } else {
            configuration = new HashMap<String, Object>();
        }
        configuration.put(Bindings.class.getName(), globalScope);
        sourceCacheLock.readLock().lock();
        try {
            return factory.getScriptEngine(configuration, sourceCache.values(),
                    getRegistryLevelScriptClassLoader());
        } finally {
            sourceCacheLock.readLock().unlock();
        }
    }

    // private classes

    private final class LibraryRecord implements CompilationHandler, InvocationHandler {

        private int status = CompilationHandler.INSTALLED;

        private final Vector<ScriptListener> listeners = new Vector<ScriptListener>();

        private WeakReference<ScriptEngine> scriptEngine = null;

        private String languageName = null;

        private ScriptSource source = null;

        private CompiledScript target = null;

        private ClassLoader scriptClassLoader = null;

        private final ScriptName scriptName;

        private LibraryRecord(ScriptName scriptName) {
            if (null == scriptName) {
                throw new NullPointerException("ScriptName is null");
            }
            this.scriptName = scriptName;
        }

        private LibraryRecord(ScriptSource scriptSource) throws ScriptException {
            if (null == scriptSource) {
                throw new NullPointerException("ScriptSource is null");
            }
            this.scriptName = scriptSource.getName();
            setScriptSource(scriptSource);
        }

        private String getLanguageName() {
            return languageName;
        }

        private boolean isDependOn(ScriptName dependency) {
            if (null != dependency || null != source) {
                ScriptName[] dep = source.getDependencies();
                if (null != dep && dep.length > 0) {
                    for (ScriptName name : dep) {
                        if (dependency.equals(name)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private void setScriptEngine(final ScriptEngine scriptEngine) throws ScriptException {
            synchronized (this) {
                if (null != scriptEngine) {
                    this.scriptEngine = new WeakReference<ScriptEngine>(scriptEngine);
                    languageName = scriptEngine.getFactory().getLanguageName();
                } else {
                    this.scriptEngine = null;
                }
                compile();
            }
        }

        private void setScriptSource(ScriptSource lazySource) throws ScriptException {
            synchronized (this) {
                source = lazySource;
                compile();
            }
        }

        // This method is not Thread-Safe but it's called from synchronized
        // blocks only
        private void compile() throws ScriptException {
            ScriptEngine engine = null != scriptEngine ? scriptEngine.get() : null;
            if (null == source) {
                source = findScriptSource(scriptName);
            }
            if ((null != source) && (engine == null)) {
                engine = findScriptEngine(source.guessType());
                if (null != engine) {
                    scriptEngine = new WeakReference<ScriptEngine>(engine);
                }
            }
            if (null == source || null == engine) {
                try {
                    if (null != target) {
                        status = STOPPING;
                        notifyListeners(ScriptEvent.UNREGISTERING);
                    }
                } finally {
                    target = null;
                    status = INSTALLED;
                }
            } else {
                status = STARTING;
                engine.compileScript(this);
            }
        }

        private void notifyListeners(int type) {
            /*
             * a temporary array buffer, used as a snapshot of the state of
             * current Observers.
             */
            Object[] arrLocal = listeners.toArray();

            for (int i = arrLocal.length - 1; i >= 0; i--) {
                try {
                    ((ScriptListener) arrLocal[i]).scriptChanged(new ScriptEvent(type,
                            ScriptRegistryImpl.this, scriptName));
                } catch (Throwable t) {
                    /* ignore */
                }
            }
        }

        // START CompilationHandler

        public ScriptSource getScriptSource() {
            return source;
        }

        public ClassLoader getParentClassLoader() {
            return ScriptRegistryImpl.this.getRegistryLevelScriptClassLoader();
        }

        public void setCompiledScript(CompiledScript script) {
            int type = null != target ? ScriptEvent.MODIFIED : ScriptEvent.REGISTERED;
            target = script;
            status = ACTIVE;
            notifyListeners(type);
        }

        public void handleException(Exception exception) {
            try {
                if (null != target) {
                    status = STOPPING;
                    notifyListeners(ScriptEvent.UNREGISTERING);
                }
            } finally {
                status = RESOLVED;
                target = null;
            }
            logger.error("Script compilation exception: {}", source.getName().getName(), exception);
        }

        public void setClassLoader(ClassLoader classLoader) {
            this.scriptClassLoader = classLoader;
        }

        // END CompilationHandler

        private synchronized void addScriptListener(ScriptListener o) {
            if (o == null) {
                throw new NullPointerException();
            }
            if (!listeners.contains(o)) {
                listeners.addElement(o);
            }
        }

        private synchronized void deleteScriptListener(ScriptListener o) {
            listeners.removeElement(o);
        }

        private ScriptEntry getScriptEntry() {
            return new ServiceScript(getScriptProxy());
        }

        private CompiledScript getScriptProxy() {
            return (CompiledScript) Proxy.newProxyInstance(CompiledScript.class.getClassLoader(),
                    new Class[] { CompiledScript.class }, this);
        }

        private ClassLoader getRuntimeClassLoader() {
            if (null != scriptClassLoader) {
                return scriptClassLoader;
            }
            if (null != getParentClassLoader()) {
                return getParentClassLoader();
            }
            return Thread.currentThread().getContextClassLoader();
        }

        public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
            // do not proxy equals, hashCode, toString
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, arguments);
            }

            ThreadClassLoaderManager.getInstance().pushClassLoader(getRuntimeClassLoader());
            try {
                if (null != target) {
                    return method.invoke(target, arguments);
                } else {
                    throw new ScriptException("Script status is " + status);
                }
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            } finally {
                ThreadClassLoaderManager.getInstance().popClassLoader();
            }
        }

        private final class ServiceScript extends ScopeHolder implements ScriptEntry {

            private final CompiledScript targetProxy;

            private ServiceScript(final CompiledScript target) {
                this.targetProxy = target;
            }

            public void addScriptListener(ScriptListener o) {
                LibraryRecord.this.addScriptListener(o);
            }

            public void deleteScriptListener(ScriptListener o) {
                LibraryRecord.this.deleteScriptListener(o);
            }

            public Bindings getScriptBindings(Context context, Bindings request) {
                if (null == context) {
                    throw new NullPointerException();
                }
                final ScriptEngine engine = LibraryRecord.this.scriptEngine.get();
                if (null == engine) {
                    throw new IllegalStateException("Engine is not available");
                }
                return target.prepareBindings(context, request, ServiceScript.this.getBindings(),
                        ScriptRegistryImpl.this.globalScope.get());
            }

            public Script getScript(final Context context) {
                if (null == context) {
                    throw new NullPointerException();
                }
                // TODO Decorate the target with the script
                // TODO Decorate with DelegatedCompilationHandler to compile a
                // new instance for debug mode
                return new ScriptImpl(context, targetProxy, getName()) {

                    // protected ScriptEngine getScriptEngine() throws
                    // ScriptException {
                    // ScriptEngine engine =
                    // LibraryRecord.this.scriptEngine.get();
                    // if (null == engine) {
                    // throw new ScriptException("Engine is not available");
                    // }
                    // return engine;
                    // }

                    protected Bindings getGlobalBindings() {
                        return ScriptRegistryImpl.this.globalScope.get();
                    }

                    protected Bindings getServiceBindings() {
                        return ServiceScript.this.getBindings();
                    }
                };
            }

            public ScriptName getName() {
                return scriptName;
            }

            public Visibility getVisibility() {
                return null != source ? source.getVisibility() : Visibility.DEFAULT;
            }

            public boolean isActive() {
                return target != null;
            }
        }
    }

    public void addScriptListener(ScriptName name, ScriptListener hook) {
        if (null != hook && null != name) {
            LibraryRecord record = cache.get(name);
            if (null == record) {
                LibraryRecord newRecord = new LibraryRecord(name);
                record = cache.putIfAbsent(name, newRecord);
                if (record == null) {
                    record = newRecord;
                }
            }
            record.addScriptListener(hook);
        }
    }

    public void deleteScriptListener(ScriptName name, ScriptListener hook) {
        if (null != hook && null != name) {
            LibraryRecord record = cache.get(name);
            if (null != record) {
                record.deleteScriptListener(hook);
            }
        }
    }

    private ScriptSource findScriptSource(ScriptName name) {
        ScriptSource source = null;
        sourceCacheLock.readLock().lock();
        try {
            for (SourceContainer container : sourceCache.values()) {
                if (container instanceof ScriptEngineFactoryAware) {
                    ((ScriptEngineFactoryAware) container).setScriptEngineFactory(engineFactories);
                }
                source = container.findScriptSource(name);
                if (null != source) {
                    break;
                }
            }
        } finally {
            sourceCacheLock.readLock().unlock();
        }
        return source;
    }

    // ScriptEngineFactoryObserver
    public void addingEntries(ScriptEngineFactory factory) throws ScriptException {
        engineFactories.add(factory);
        for (LibraryRecord cacheRecord : cache.values()) {
            if (CompilationHandler.INSTALLED == cacheRecord.status && null != cacheRecord.source) {
                ScriptEngine engine = findScriptEngine(cacheRecord.source.guessType());
                if (null != engine) {
                    cacheRecord.setScriptEngine(engine);
                }
            }
        }
    }

    public void removingEntries(ScriptEngineFactory factory) throws ScriptException {
        engineFactories.remove(factory);
        engines.remove(factory);
        if (null != factory) {
            String languageName = factory.getLanguageName();
            for (LibraryRecord cacheRecord : cache.values()) {
                if (languageName.equals(cacheRecord.getLanguageName())) {
                    cacheRecord.setScriptEngine(null);
                }
            }
        }
    }

    // SourceUnitObserver
    public void addSourceUnit(SourceUnit unit) throws ScriptException {
        try {
            if (unit instanceof ScriptSource) {
                // Cheap: avoid the synchronized block
                LibraryRecord record = new LibraryRecord(unit.getName());
                LibraryRecord cacheRecord = cache.putIfAbsent(unit.getName(), record);
                if (null == cacheRecord) {
                    cacheRecord = record;
                }
                if (null == cacheRecord.getScriptSource()
                        || !cacheRecord.getScriptSource().getName().getRevision().equalsIgnoreCase(
                        unit.getName().getRevision())) {
                    // Expensive: compile the source
                    cacheRecord.setScriptSource((ScriptSource) unit);
                }
            } else if (unit instanceof SourceContainer) {
                SourceContainer container = (SourceContainer) unit;

                sourceCacheLock.writeLock().lock();
                try {
                    sourceCache.put(unit.getName(), container);
                } finally {
                    sourceCacheLock.writeLock().unlock();
                }

                for (LibraryRecord cacheRecord : cache.values()) {
                    if (null == cacheRecord.source) {
                        ScriptSource source = container.findScriptSource(cacheRecord.scriptName);
                        if (null != source) {
                            cacheRecord.setScriptSource(source);
                        }
                    }
                }
            }
        } catch (ScriptCompilationException e) {
            // remove from cache if fails compilation
            cache.remove(unit.getName());
            throw e;
        }
    }

    public void removeSourceUnit(SourceUnit unit) throws ScriptException {
        if (unit instanceof ScriptSource) {
            LibraryRecord cacheRecord = cache.get(unit.getName());
            if (null != cacheRecord) {
                cacheRecord.setScriptSource(null);
            }
        } else if (unit instanceof SourceContainer) {
            sourceCacheLock.writeLock().lock();
            try {
                sourceCache.remove(unit);
            } finally {
                sourceCacheLock.writeLock().lock();
            }

            for (LibraryRecord cacheRecord : cache.values()) {
                if (cacheRecord.isDependOn(unit.getName())) {
                    cacheRecord.setScriptSource(null);
                }
            }
        }
    }

    abstract static class ScriptImpl extends ScopeHolder implements Script {

        private static final Logger logger = LoggerFactory.getLogger(ScriptImpl.class);

        private final CompiledScript target;
        private final Context context;
        private Bindings safeBinding = null;

        ScriptImpl(final Context context, final CompiledScript target, ScriptName scriptName) {
            this.target = target;
            this.context = new ScriptContext(context, scriptName.getName(), scriptName.getType(), scriptName.getRevision());
        }

        public void putSafe(String key, Object value) {
            if (null == safeBinding) {
                safeBinding = new SimpleBindings();
            }
            safeBinding.put(key, value);
        }

        public Object eval(final Bindings bindings) throws ScriptException {
            try {
                return target.eval(context, bindings, safeBinding, getServiceBindings(),
                        getGlobalBindings());
            } catch (ScriptException e) {
                throw e;
            } catch (Throwable t) {
                logger.error("Script invocation error", t);
                throw new ScriptException(t.getMessage());
            }
        }

        public Object eval() throws ScriptException {
            return eval(getBindings());
        }

        protected abstract Bindings getGlobalBindings();

        protected abstract Bindings getServiceBindings();

    }

    abstract static class ScopeHolder implements Scope {

        private Bindings bindings = null;

        public void put(String key, Object value) {
            if (null == bindings) {
                bindings = createBindings();
            }
            bindings.put(key, value);
        }

        public Object get(String key) {
            if (getBindings() != null) {
                return getBindings().get(key);
            }
            return null;
        }

        public Bindings getBindings() {
            return bindings;
        }

        public void setBindings(Bindings bindings) {
            this.bindings = bindings;
        }

        public Bindings createBindings() {
            return new SimpleBindings();
        }

        public void flush() {
            setBindings(null);
        }
    }
}
