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

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class InjectorHolderTest {

    @BeforeClass
    public void setUp() {
        InjectorConfiguration.setGuiceModuleLoader(new TestGuiceModuleLoader());
    }

    @Test
    public void shouldAlwaysReturnTheSameInjector() {

        //Given

        //When
        Injector injector = InjectorHolder.getInjector();

        //Then
        Injector sameInjector = InjectorHolder.getInjector();
        assertThat(injector).isEqualTo(sameInjector);
    }

    @Test
    public void shouldGetInstanceWithClass() {

        //Given

        //When
        TestInterface instance = InjectorHolder.getInstance(TestInterface.class);

        //Then
        assertThat(instance).isNotNull().isInstanceOf(TestInterface.class);
    }

    @Test
    public void shouldGetInstanceWithKey() {

        //Given

        //When
        TestInterface instance = InjectorHolder.getInstance(Key.get(TestInterface.class));

        //Then
        assertThat(instance).isNotNull().isInstanceOf(TestInterface.class);
    }

    @Test
    public void shouldInjectMembers() {

        //Given
        TestImplementation testImplementation = new TestImplementation();

        //When
        InjectorHolder.injectMembers(testImplementation);

        //Then
        assertThat(testImplementation.getTestDependency()).isNotNull();
    }

    public static final class TestGuiceModuleLoader implements GuiceModuleLoader {

        public Set<Class<? extends Module>> getGuiceModules(Class<? extends Annotation> moduleAnnotation) {
            Set<Class<? extends Module>> modules = new HashSet<Class<? extends Module>>();
            modules.add(TestModule.class);
            return modules;
        }
    }

    public static final class TestModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(TestInterface.class).to(TestImplementation.class);
        }
    }

    public static interface TestInterface {

    }

    public static final class TestImplementation implements TestInterface {

        private TestDependency testDependency;

        @Inject
        public void setTestDependency(TestDependency testDependency) {
            this.testDependency = testDependency;
        }

        public TestDependency getTestDependency() {
            return testDependency;
        }
    }

    public static final class TestDependency {

    }
}
