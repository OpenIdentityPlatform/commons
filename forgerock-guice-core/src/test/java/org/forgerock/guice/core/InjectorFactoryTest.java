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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.HashSet;
import java.util.Set;

import com.google.inject.Module;
import org.forgerock.guice.core.test.TestModule1;
import org.forgerock.guice.core.test.TestModule2;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class InjectorFactoryTest {

    private InjectorFactory injectorFactory;

    private GuiceModuleCreator moduleCreator;
    private GuiceInjectorCreator injectorCreator;
    private GuiceModuleLoader moduleLoader;

    @BeforeClass
    public void setUp() {
        moduleCreator = mock(GuiceModuleCreator.class);
        injectorCreator = mock(GuiceInjectorCreator.class);
        moduleLoader = mock(GuiceModuleLoader.class);

        injectorFactory = new InjectorFactory(moduleCreator, injectorCreator, moduleLoader);
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void shouldCreateInjector() {

        //Given
        Class<GuiceModule> moduleAnnotation = GuiceModule.class;
        Set<Class<? extends Module>> moduleClasses = new HashSet<Class<? extends Module>>();
        moduleClasses.add(TestModule1.class);
        moduleClasses.add(TestModule2.class);

        given(moduleLoader.getGuiceModules(moduleAnnotation)).willReturn(moduleClasses);

        TestModule1 testModule1 = mock(TestModule1.class);
        given(moduleCreator.createInstance(TestModule1.class)).willReturn(testModule1);

        TestModule2 testModule2 = mock(TestModule2.class);
        given(moduleCreator.createInstance(TestModule2.class)).willReturn(testModule2);

        //When
        injectorFactory.createInjector(moduleAnnotation);

        //Then
        verify(moduleCreator, times(2)).createInstance(Matchers.<Class<Module>>anyObject());
        ArgumentCaptor<Set> createInjectorCaptor = ArgumentCaptor.forClass(Set.class);
        verify(injectorCreator).createInjector(createInjectorCaptor.capture());
        Set<Module> modules = createInjectorCaptor.getValue();
        assertThat(modules).hasSize(2);
    }
}
