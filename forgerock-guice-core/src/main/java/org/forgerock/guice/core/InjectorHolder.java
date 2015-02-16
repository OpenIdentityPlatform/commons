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
import com.google.inject.Key;
import com.google.inject.Module;

/**
 * <p>A thread-safe singleton holding the Guice Injector instance that other classes can call to get to use dependency
 * injection.</p>
 *
 * <p>The InjectorHolder should be used sparingly, ideally only to be used at the entry points into the system, i.e.
 * Servlets, Filters, etc, and allow Guice at this entry points to construct all of the required dependencies.
 * If introducing into an existing system then as the code is migrated over to be "Guice enabled" form a logical
 * "boundary" in your code which is the entry point(s) into the Guice "world" and at this entry points call the
 * InjectorHolder in the constructors of the boundary entry points to construct its dependencies.</p>
 *
 * <p>As more of the codebase is migrated to use Guice then this logical boundary will expand and the use of the
 * InjectorHolder should be moved further out with the boundary.</p>
 *
 * @see com.google.inject.Injector
 *
 * @since 1.0.0
 */
public enum InjectorHolder {

    /**
     * The Singleton instance of the InjectorHolder.
     */
    INSTANCE;

    private Injector injector;

    /**
     * Constructs an instance of the InjectorHolder and initialises the Guice Injector.
     */
    private InjectorHolder() {
        InjectorFactory injectorFactory = new InjectorFactory(new GuiceModuleCreator(),
                new GuiceInjectorCreator(), InjectorConfiguration.getGuiceModuleLoader());

        try {
            injector = injectorFactory.createInjector(InjectorConfiguration.getModuleAnnotation());
        } catch (Exception e) {
            /*
             * This could occur during application server startup, if there is an error in the Guice module bindings.
             * The applications debugging framework may not be available at this point.
             *
             * The error gets consumed by the container startup, which is why we are printing the stack trace.
             */
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    /**
     * <p>Uses the Guice injector to return the appropriate instance for the given injection type.</p>
     *
     * <p>Avoid using this method, in favour of having Guice inject your dependencies ahead of time.</p>
     *
     * @param clazz The class to get an instance of.
     * @param <T> The type of class to get.
     * @return A non-null instance of the class.
     */
    public static <T> T getInstance(Class<T> clazz) {
        return INSTANCE.injector.getInstance(clazz);
    }

    /**
     * <p>Uses the Guice injector to return the appropriate instance for the given injection key.</p>
     *
     * <p>Avoid using this method, in favour of having Guice inject your dependencies ahead of time.</p>
     *
     * @param key The key that defines the class to get.
     * @param <T> The type of class defined by the key.
     * @return A non-null instance of the class defined by the key.
     */
    public static <T> T getInstance(Key<T> key) {
        return INSTANCE.injector.getInstance(key);
    }

    /**
     * <p>Injects the dependencies of an already constructed instance. This method can be used to inter-operate with
     * objects created by other frameworks or services.</p>
     *
     * <p>Injects dependencies into the fields and methods of {@code instance}. Ignores the presence or absence of an
     * injectable constructor.</p>
     *
     * <p>Preferably let Guice create all your objects for you and you'll never need to use this method.</p>
     *
     * @param instance A non-null instance to inject members on.
     */
    public static void injectMembers(Object instance) {
        INSTANCE.injector.injectMembers(instance);
    }

    /**
     * <p>Returns a new injector that inherits all state from this injector. All bindings, scopes,
     * interceptors and type converters are inherited -- they are visible to the child injector.
     * Elements of the child injector are not visible to its parent.</p>
     *
     * <p>Just-in-time bindings created for child injectors will be created in an ancestor injector
     * whenever possible. This allows for scoped instances to be shared between injectors. Use
     * explicit bindings to prevent bindings from being shared with the parent injector.</p>
     *
     * <p>No key may be bound by both an injector and one of its ancestors. This includes just-in-time
     * bindings. The lone exception is the key for {@code Injector.class}, which is bound by each
     * injector to itself.</p>
     *
     * @param modules An array of Guice modules to use to configure the child injector.
     * @return A non-null child instance of the root injector.
     */
    public static Injector createChildInjector(Module... modules) {
        return INSTANCE.injector.createChildInjector(modules);
    }

    /**
     * <p>Retrieves the Guice injector.</p>
     *
     * <p>Use with care! Always prefer using #getInstance(Class).</p>
     *
     * @return The configured Guice injector.
     */
    static Injector getInjector() {
        return INSTANCE.injector;
    }

    void register(Injector injector) {
        this.injector = injector;
    }
}
