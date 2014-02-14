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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.guice.core;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

/**
 * Will find and load all classes which extend the Guice AbstractModule class and that are annotated with the
 * provided annotation by using the Java ServiceLoader framework.
 * <br/>
 * To register an AbstractModule with the ServiceLoader framework, create a file named,
 * "com.google.inject.AbstractModule", under META-INF/services with a new line delimited list of the fully
 * qualified names of the subtypes, in your jar/project, to be used to configure the Guice injector.
 * <br/>
 * Any AbstractModule subtypes which are registered with the Java ServiceLoader by are not annotated with the
 * provided annotation will be ignored and will not be included in the returned set of Guice Modules.
 *
 * @see java.util.ServiceLoader
 */
public class GuiceModuleServiceLoader implements GuiceModuleLoader {

    private final Logger logger = LoggerFactory.getLogger(GuiceModuleServiceLoader.class);

    private final ServiceLoader serviceLoader;

    /**
     * Constructs an instance of the GuiceModuleServiceLoader.
     *
     * @param serviceLoader An instance of a wrapper around the java.util.ServiceLoader.
     */
    GuiceModuleServiceLoader(final ServiceLoader serviceLoader) {
        this.serviceLoader = serviceLoader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Class<? extends Module>> getGuiceModules(final Class<? extends Annotation> moduleAnnotation) {

        final Set<Class<? extends Module>> moduleClasses = new HashSet<Class<? extends Module>>();

        final Iterable<AbstractModule> abstractModuleLoader = serviceLoader.load(AbstractModule.class);

        for (final AbstractModule module : abstractModuleLoader) {
            if (!hasAnnotation(module.getClass(), moduleAnnotation)) {
                logger.debug(module.getClass().getCanonicalName()
                        + " extends the AbstractModule class but is not annotated with @"
                        + moduleAnnotation.getSimpleName() + ", so will be ignored.");
                continue;
            }
            moduleClasses.add(module.getClass());
        }

        return moduleClasses;
    }

    /**
     * Checks to determine if the given class is annotated with the given annotation.
     *
     * @param clazz The class which should be annotated.
     * @param moduleAnnotation The annotation the class should be annotated with.
     * @return <code>true</code> if the class is annotated with the annotation, otherwise <code>false</code>.
     */
    private boolean hasAnnotation(final Class<? extends AbstractModule> clazz,
            final Class<? extends Annotation> moduleAnnotation) {
        for (final Annotation annotation : clazz.getDeclaredAnnotations()) {
            if (moduleAnnotation.equals(annotation.annotationType())) {
                return true;
            }
        }

        return false;
    }
}
