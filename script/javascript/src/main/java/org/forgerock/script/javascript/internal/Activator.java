package org.forgerock.script.javascript.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.forgerock.script.engine.ScriptEngineFactory;
import org.forgerock.script.javascript.RhinoScriptEngineFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public class Activator implements BundleActivator, ServiceFactory<ScriptEngineFactory> {

    /**
     * ScriptEngineFactory registration
     */
    private ServiceRegistration<?> serviceRegistration = null;

    private static final ConcurrentMap<Bundle, ScriptEngineFactory> REGISTRY =
            new ConcurrentHashMap<Bundle, ScriptEngineFactory>();

    public void start(BundleContext context) throws Exception {
        Dictionary<String, Object> properties = new Hashtable<String, Object>();

        properties.put(Constants.SERVICE_VENDOR, "ForgeRock AS");
        properties.put(Constants.SERVICE_DESCRIPTION, "Scripting language support of "
                + RhinoScriptEngineFactory.LANGUAGE_NAME);

        serviceRegistration =
                context.registerService(ScriptEngineFactory.class.getName(), this, properties);

    }

    public void stop(BundleContext context) throws Exception {
        if (null != serviceRegistration) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
    }

    public ScriptEngineFactory getService(Bundle bundle,
            ServiceRegistration<ScriptEngineFactory> registration) {
        ScriptEngineFactory factory = new RhinoScriptEngineFactory();
        ScriptEngineFactory result = REGISTRY.putIfAbsent(bundle, factory);
        if (null == result) {
            result = factory;
        }
        return result;
    }

    public void ungetService(Bundle bundle, ServiceRegistration<ScriptEngineFactory> registration,
            ScriptEngineFactory service) {
        REGISTRY.remove(bundle);
    }
}
