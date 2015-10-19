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

import java.lang.annotation.Annotation;
import java.util.Set;

import com.google.inject.Module;
import com.google.inject.Stage;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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
        assertThat(moduleAnnotation).isEqualTo(GuiceModule.class);
    }

    @Test
    public void shouldGetGuiceModuleServiceLoaderAsDefaultConfiguredGuiceModuleLoader() {

        //Given

        //When
        GuiceModuleLoader moduleLoader = InjectorConfiguration.getGuiceModuleLoader();

        //Then
        assertThat(moduleLoader.getClass()).isAssignableFrom(GuiceModuleServiceLoader.class);
    }

    @Test
    public void shouldGetDefaultInjectorStage() {

        //Given

        //When
        Stage stage = InjectorConfiguration.getStage();

        //Then
        assertThat(stage).isEqualTo(Stage.PRODUCTION);
    }

    @Test
    public void shouldSetModuleAnnotation() {

        //Given

        //When
        InjectorConfiguration.setModuleAnnotation(Annotation.class);

        //Then
        Class<? extends Annotation> moduleAnnotation = InjectorConfiguration.getModuleAnnotation();
        assertThat(moduleAnnotation).isEqualTo(Annotation.class);
    }

    @Test
    public void shouldSetGuiceModuleLoader() {

        //Given
        GuiceModuleLoader testModuleLoader = new TestGuiceModuleLoader();

        //When
        InjectorConfiguration.setGuiceModuleLoader(testModuleLoader);

        //Then
        GuiceModuleLoader moduleLoader = InjectorConfiguration.getGuiceModuleLoader();
        assertThat(moduleLoader).isEqualTo(testModuleLoader);
    }

    @Test
    public void shouldSetInjectorStage() {

        //Given
        Stage stage = Stage.DEVELOPMENT;

        //When
        InjectorConfiguration.setStage(stage);

        //Then
        assertThat(InjectorConfiguration.getStage()).isEqualTo(stage);
    }

    private static final class TestGuiceModuleLoader implements GuiceModuleLoader {

        public Set<Class<? extends Module>> getGuiceModules(Class<? extends Annotation> moduleAnnotation) {
            return null;
        }
    }
}
