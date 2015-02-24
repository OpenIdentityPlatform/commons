/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.guice.core;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Will find and load all classes which extend the Guice AbstractModule or PrivateModule class and that are
 * annotated with the provided annotation by using the Java ServiceLoader framework.</p>
 *
 * <p>To register an AbstractModule with the ServiceLoader framework, create a file named,
 * "com.google.inject.AbstractModule", under META-INF/services with a new line delimited list of the fully
 * qualified names of the subtypes, in your jar/project, to be used to configure the Guice injector.</p>
 *
 * <p>Any AbstractModule subtypes which are registered with the Java ServiceLoader by are not annotated with the
 * provided annotation will be ignored and will not be included in the returned set of Guice Modules.</p>
 *
 * @see java.util.ServiceLoader
 * @since 1.0.0
 */
public final class GuiceModuleServiceLoader implements GuiceModuleLoader {

    private static final Class<? extends Module> MODULE_SERVICE_CLASS = Module.class;
    private static final Class<? extends Module> LEGACY_MODULE_SERVICE_CLASS = AbstractModule.class;

    private final Logger logger = LoggerFactory.getLogger(GuiceModuleServiceLoader.class);
    private final ServiceLoaderWrapper serviceLoader;

    /**
     * Constructs an instance of the GuiceModuleServiceLoader.
     *
     * @param serviceLoader An instance of a wrapper around the java.util.ServiceLoader.
     */
    GuiceModuleServiceLoader(ServiceLoaderWrapper serviceLoader) {
        this.serviceLoader = serviceLoader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Class<? extends Module>> getGuiceModules(Class<? extends Annotation> moduleAnnotation) {
        Set<Class<? extends Module>> moduleClasses = new HashSet<Class<? extends Module>>();
        moduleClasses.addAll(getGuiceModules(moduleAnnotation, MODULE_SERVICE_CLASS));
        moduleClasses.addAll(getGuiceModules(moduleAnnotation, LEGACY_MODULE_SERVICE_CLASS));
        return moduleClasses;
    }

    private <T extends Module> Set<Class<? extends Module>> getGuiceModules(
            Class<? extends Annotation> moduleAnnotation, Class<T> serviceClass) {

        Set<Class<? extends Module>> moduleClasses = new HashSet<Class<? extends Module>>();
        Iterable<T> abstractModuleLoader = serviceLoader.load(serviceClass);

        if (abstractModuleLoader == null) {
            return moduleClasses;
        }

        for (T module : abstractModuleLoader) {
            if (!hasAnnotation(module.getClass(), moduleAnnotation)) {
                logger.warn("{} extends the {} class but is not annotated with @{}, so will be ignored.",
                        module.getClass().getCanonicalName(), serviceClass.getSimpleName(),
                        moduleAnnotation.getSimpleName());
                continue;
            }
            moduleClasses.add(module.getClass());
        }
        return moduleClasses;
    }

    private boolean hasAnnotation(Class<? extends Module> clazz, Class<? extends Annotation> moduleAnnotation) {
        for (Annotation annotation : clazz.getDeclaredAnnotations()) {
            if (moduleAnnotation.equals(annotation.annotationType())) {
                return true;
            }
        }
        return false;
    }
}
