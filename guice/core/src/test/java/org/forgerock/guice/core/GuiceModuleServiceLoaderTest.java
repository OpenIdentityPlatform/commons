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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.forgerock.guice.core.test.TestModule4;
import org.forgerock.guice.core.test.TestModule5;
import org.forgerock.guice.core.test.TestModule6;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class GuiceModuleServiceLoaderTest {

    private GuiceModuleServiceLoader moduleServiceLoader;

    private ServiceLoaderWrapper serviceLoader;

    @BeforeMethod
    public void setUp() {
        serviceLoader = mock(ServiceLoaderWrapper.class);

        moduleServiceLoader = new GuiceModuleServiceLoader(serviceLoader);
    }

    @Test
    public void getGuiceModulesShouldReturnEmptySetWhenNoModulesFound() {

        //Given
        Class<? extends Annotation> moduleAnnotation = GuiceModule.class;
        Set<Module> modules = new HashSet<Module>();
        Set<AbstractModule> abstractModules = new HashSet<AbstractModule>();

        given(serviceLoader.load(Module.class)).willReturn(modules);
        given(serviceLoader.load(AbstractModule.class)).willReturn(abstractModules);

        //When
        Set<Class<? extends Module>> guiceModules = moduleServiceLoader.getGuiceModules(moduleAnnotation);

        //Then
        assertThat(guiceModules).hasSize(0);
    }

    @Test
    public void getGuiceModulesShouldOnlyReturnAnnotatedAbstractModules() {

        //Given
        Class<? extends Annotation> moduleAnnotation = GuiceModule.class;
        Set<Module> modules = new HashSet<Module>();
        modules.add(new TestModule5());
        Set<AbstractModule> abstractModules = new HashSet<AbstractModule>();
        abstractModules.add(new TestModule4());
        abstractModules.add(new TestModule6());

        given(serviceLoader.load(Module.class)).willReturn(modules);
        given(serviceLoader.load(AbstractModule.class)).willReturn(abstractModules);

        //When
        Set<Class<? extends Module>> guiceModules = moduleServiceLoader.getGuiceModules(moduleAnnotation);

        //Then
        assertThat(guiceModules).hasSize(2);
    }
}
