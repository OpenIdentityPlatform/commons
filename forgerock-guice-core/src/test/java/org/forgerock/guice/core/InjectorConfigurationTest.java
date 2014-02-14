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

import com.google.inject.Module;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.annotation.Annotation;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class InjectorConfigurationTest {

    private Class<? extends Annotation> defaultAnnotation;
    private GuiceModuleLoader defaultModuleLoader;

    @BeforeClass
    public void setUpClass() {
        defaultAnnotation = InjectorConfiguration.getModuleAnnotation();
        defaultModuleLoader = InjectorConfiguration.getGuiceModuleLoader();
    }

    @BeforeMethod
    public void setUp() {
        InjectorConfiguration.setModuleAnnotation(defaultAnnotation);
        InjectorConfiguration.setGuiceModuleLoader(defaultModuleLoader);
    }

    @Test
    public void shouldGetGuiceModuleAsDefaultConfiguredModuleAnnotation() {

        //Given

        //When
        Class<? extends Annotation> moduleAnnotation = InjectorConfiguration.getModuleAnnotation();

        //Then
        assertEquals(moduleAnnotation, GuiceModule.class);
    }

    @Test
    public void shouldGetGuiceModuleServiceLoaderAsDefaultConfiguredGuiceModuleLoader() {

        //Given

        //When
        GuiceModuleLoader moduleLoader = InjectorConfiguration.getGuiceModuleLoader();

        //Then
        assertTrue(GuiceModuleServiceLoader.class.isAssignableFrom(moduleLoader.getClass()));
    }

    @Test
    public void shouldSetModuleAnnotation() {

        //Given

        //When
        InjectorConfiguration.setModuleAnnotation(Annotation.class);

        //Then
        Class<? extends Annotation> moduleAnnotation = InjectorConfiguration.getModuleAnnotation();
        assertEquals(moduleAnnotation, Annotation.class);
    }

    @Test
    public void shouldSetGuiceModuleLoader() {

        //Given
        GuiceModuleLoader testModuleLoader = new TestGuiceModuleLoader();

        //When
        InjectorConfiguration.setGuiceModuleLoader(testModuleLoader);

        //Then
        GuiceModuleLoader moduleLoader = InjectorConfiguration.getGuiceModuleLoader();
        assertEquals(moduleLoader, testModuleLoader);
    }

    private static final class TestGuiceModuleLoader implements GuiceModuleLoader {

        public Set<Class<? extends Module>> getGuiceModules(Class<? extends Annotation> moduleAnnotation) {
            return null;
        }
    }
}
