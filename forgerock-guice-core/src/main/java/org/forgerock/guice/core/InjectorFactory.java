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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.guice.core;

import com.google.inject.Injector;
import com.google.inject.Module;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>Constructs a Guice injector to find all the Guice modules on the classpath, by using the configured
 * {@link GuiceModuleLoader} implementation, and uses them to initalise the Guice injector.</p>
 *
 * <p>The {@link InjectorHolder} is configured with an Annotation which all modules used to configure Guice MUST
 * be annotated with and the GuiceModuleLoader MUST only load modules which are annotated with the annotation.</p>
 *
 * <p>This class should not be used directly, but instead via the {@link InjectorHolder}.</p>
 *
 * @since 1.0.0
 */
final class InjectorFactory {

    private final GuiceModuleCreator moduleCreator;
    private final GuiceInjectorCreator injectorCreator;
    private final GuiceModuleLoader moduleLoader;

    /**
     * Constructs an instance of the InjectorFactory.
     *
     * @param moduleCreator An instance of the GuiceModuleCreator.
     * @param injectorCreator An instance of the GuiceInjectorCreator.
     * @param moduleLoader An instance of the GuiceModuleLoader.
     */
    InjectorFactory(GuiceModuleCreator moduleCreator, GuiceInjectorCreator injectorCreator,
            GuiceModuleLoader moduleLoader) {
        this.moduleCreator = moduleCreator;
        this.injectorCreator = injectorCreator;
        this.moduleLoader = moduleLoader;
    }

    /**
     * Creates a new Guice injector which is configured by all modules found by the {@link GuiceModuleLoader}
     * implementation.
     *
     * @param moduleAnnotation The module annotation.
     * @return A non-null Guice injector.
     */
    Injector createInjector(Class<? extends Annotation> moduleAnnotation) {
        /*
         This does not need to by synchronized as it is only ever called from the constructor of the
         InjectorHolder enum, which is thread-safe so no two threads can create an injector at the same time.

         This does mean that this method MUST not be called/used by another other class!
         */
        return injectorCreator.createInjector(createModules(moduleAnnotation));
    }

    /**
     * Uses the configured {@link GuiceModuleLoader} implementation to find all the Guice modules on the classpath,
     * where all the modules MUST be annotated the given module annotation.
     *
     * @param moduleAnnotation The module annotation.
     * @return A Set of Guice modules.
     */
    @SuppressWarnings("unchecked")
    private Set<Module> createModules(Class<? extends Annotation> moduleAnnotation) {

        Set<Class<? extends Module>> moduleClasses = moduleLoader.getGuiceModules(moduleAnnotation);

        Set<Module> modules = new HashSet<Module>();

        for (Class<? extends Module> moduleClass : moduleClasses) {
            modules.add(moduleCreator.createInstance(moduleClass));
        }

        return modules;
    }
}
