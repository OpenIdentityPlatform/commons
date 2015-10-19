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

import java.lang.annotation.Annotation;

import com.google.inject.Stage;

/**
 * <p>A thread-safe singleton holding the configuration information on how the Guice framework finds Guice Modules to
 * configure the Guice Injector instance.</p>
 *
 * <p>This singleton holds the Module annotation that all Guice Modules, that should be used to configure the injector,
 * MUST be annotated with. And the implementation of the {@link GuiceModuleLoader} that will be used to find and load
 * the module classes.</p>
 *
 * <p>By default the module annotation is configured to be {@link GuiceModule} and the GuiceModuleLoader implementation
 * is configured to be {@link GuiceModuleServiceLoader}.
 * These configurations can be changed by calling, {@link #setModuleAnnotation(Class)} and
 * {@link #setGuiceModuleLoader(GuiceModuleLoader)},respectively.</p>
 *
 * @since 1.0.0
 */
public enum InjectorConfiguration {

    /**
     * The Singleton instance of the InjectorConfiguration.
     */
    INSTANCE;

    private volatile Class<? extends Annotation> moduleAnnotation = GuiceModule.class;
    private volatile GuiceModuleLoader guiceModuleLoader = new GuiceModuleServiceLoader(new ServiceLoaderWrapper());
    private volatile Stage stage = Stage.PRODUCTION;

    /**
     * Gets the module annotation that all modules MUST be annotated with.
     *
     * @return The module annotation.
     */
    static Class<? extends Annotation> getModuleAnnotation() {
        return INSTANCE.moduleAnnotation;
    }

    /**
     * Sets the module annotation that all modules MUST be annotated with.
     *
     * @param moduleAnnotation The module annotation.
     */
    public static void setModuleAnnotation(Class<? extends Annotation> moduleAnnotation) {
        INSTANCE.moduleAnnotation = moduleAnnotation;
    }

    /**
     * Gets the {@link GuiceModuleLoader} implementation that will be used to find and load module classes.
     *
     * @return The GuiceModuleLoader implementation.
     */
    static GuiceModuleLoader getGuiceModuleLoader() {
        return INSTANCE.guiceModuleLoader;
    }

    /**
     * Sets the {@link GuiceModuleLoader} implementation that will be used to find and load module classes.
     *
     * @param guiceModuleLoader The GuiceModuleLoader implementation.
     */
    public static void setGuiceModuleLoader(GuiceModuleLoader guiceModuleLoader) {
        INSTANCE.guiceModuleLoader = guiceModuleLoader;
    }

    /**
     * Gets the {@link Stage} that the injector should run in.
     *
     * @return The injector stage.
     */
    static Stage getStage() {
        return INSTANCE.stage;
    }

    /**
     * Sets the {@link Stage} that the injector should run in. See {@link Stage} for available stages
     * and their differences.
     *
     * @param stage The injector stage.
     */
    public static void setStage(Stage stage) {
        INSTANCE.stage = stage;
    }
}
