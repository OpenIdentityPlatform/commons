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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.guice.core;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.util.Modules;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * A test case that allows registration of guice modules for the life of each test method being run.
 *
 * @since 1.1.0
 */
public abstract class GuiceTestCase implements Module {

    private Injector oldInjector;

    /**
     * Binds this object and any modules declared from the GuiceModules annotation in an injector, and registers it
     * in the InjectorHolder.
     * @throws Exception Thrown on failure of any type.
     */
    @BeforeMethod
    public void setupGuiceModules() throws Exception {
        List<Module> modules = new ArrayList<Module>();
        modules.add(this);

        GuiceModules guiceModules = this.getClass().getAnnotation(GuiceModules.class);
        if (guiceModules != null) {
            for (Class<? extends Module> moduleType : guiceModules.value()) {
                modules.add(moduleType.newInstance());
            }
        }

        final GuiceTestCase testCase = this;
        Module overrideModule = new Module() {
            public void configure(Binder binder) {
                testCase.configureOverrideBindings(binder);
            }
        };

        this.oldInjector = InjectorHolder.getInjector();
        Injector injector = Guice.createInjector(Modules.override(modules).with(overrideModule));
        InjectorHolder.INSTANCE.register(injector);
    }

    /**
     * After the test method has run, the injector is reset to its old value.
     * @throws Exception Thrown on failure of any type.
     */
    @AfterMethod
    public void teardownGuiceModules() throws Exception {
        InjectorHolder.INSTANCE.register(oldInjector);
    }

    /**
     * A default, empty implementation is provided as the test may not have any of its own objects to bind.
     * @param binder The Guice binder.
     */
    public void configure(Binder binder) {
    }

    /**
     * Bindings specified on this {@literal binder} will be used to override
     * bindings specified in {@link #configure(Binder)} and via the
     * {@link GuiceModules} annotation.
     *
     * @param binder The Guice binder.
     */
    protected void configureOverrideBindings(Binder binder) {
    }
}
