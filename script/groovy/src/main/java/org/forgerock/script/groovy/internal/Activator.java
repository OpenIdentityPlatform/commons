/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.script.groovy.internal;

import org.forgerock.script.engine.ScriptEngineFactory;
import org.forgerock.script.groovy.GroovyScriptEngineFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public class Activator implements BundleActivator, ServiceFactory<ScriptEngineFactory> {

    /**
     * ScriptEngineFactory registration.
     */
    private ServiceRegistration<?> serviceRegistration = null;

    private static final ConcurrentMap<Bundle, ScriptEngineFactory> REGISTRY =
            new ConcurrentHashMap<Bundle, ScriptEngineFactory>();

    public void start(BundleContext context) throws Exception {
        Dictionary<String, Object> properties = new Hashtable<String, Object>(2);

        properties.put(Constants.SERVICE_VENDOR, "ForgeRock AS");
        properties.put(Constants.SERVICE_DESCRIPTION, "Scripting language support of "
                + GroovyScriptEngineFactory.LANGUAGE_NAME);

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
        ScriptEngineFactory factory = new GroovyScriptEngineFactory();
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
