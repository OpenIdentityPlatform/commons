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
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

import com.google.inject.Binder;
import com.google.inject.Module;
import org.forgerock.guice.core.test.TestModule1;
import org.forgerock.guice.core.test.TestModule2;
import org.forgerock.guice.core.test.TestModule3;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GuiceModuleCreatorTest {

    private GuiceModuleCreator guiceModuleCreator;

    @BeforeClass
    public void setUp() {
        guiceModuleCreator = new GuiceModuleCreator();
    }

    @Test
    public void shouldCreateInstanceWithPublicNoArgConstructor() {

        //Given
        Class<? extends Module> moduleClass = TestModule2.class;

        //When
        Object module = guiceModuleCreator.createInstance(moduleClass);

        //Then
        assertThat(module).isNotNull();
    }

    @Test (expectedExceptions = ModuleCreationException.class)
    public void shouldFailToCreateInstanceWithPrivateNoArgConstructor() {

        //Given
        Class<? extends Module> moduleClass = TestModule1.class;

        //When
        guiceModuleCreator.createInstance(moduleClass);

        //Then
        failBecauseExceptionWasNotThrown(ModuleCreationException.class);
    }

    @Test (expectedExceptions = ModuleCreationException.class)
    public void shouldFailToCreateInstanceWithPublicArgsConstructor() {

        //Given
        Class<? extends Module> moduleClass = TestModule3.class;

        //When
        guiceModuleCreator.createInstance(moduleClass);

        //Then
        failBecauseExceptionWasNotThrown(ModuleCreationException.class);
    }

    @Test (expectedExceptions = ModuleCreationException.class)
    public void shouldThrowModuleCreationExceptionWhenAnyExceptionOccurs() {

        //Given
        Class<? extends Module> moduleClass = InstantiationExceptionClass.class;

        //When
        guiceModuleCreator.createInstance(moduleClass);

        //Then
        failBecauseExceptionWasNotThrown(ModuleCreationException.class);
    }

    public static final class InstantiationExceptionClass implements Module {

        public InstantiationExceptionClass() throws InstantiationException {
            throw new InstantiationException();
        }

        public void configure(Binder binder) {
        }
    }
}
