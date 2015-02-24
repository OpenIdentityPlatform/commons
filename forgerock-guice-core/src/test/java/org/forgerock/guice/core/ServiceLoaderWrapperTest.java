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

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.forgerock.guice.core.test.TestModule4;
import org.forgerock.guice.core.test.TestModule5;
import org.forgerock.guice.core.test.TestModule7;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ServiceLoaderWrapperTest {

    private ServiceLoaderWrapper serviceLoader;

    @BeforeClass
    public void setUp() {
        serviceLoader = new ServiceLoaderWrapper();
    }

    @Test
    public void shouldLoadModuleService() {

        //Given
        Class<Module> service = Module.class;

        //When
        Iterable<Module> services = serviceLoader.load(service);

        //Then
        assertThat(services)
                .hasSize(3)
                .containsOnly(new TestModule4(), new TestModule5(), new TestModule7());
    }

    @Test
    public void shouldLoadLegacyAbstractModuleService() {

        //Given
        Class<AbstractModule> service = AbstractModule.class;

        //When
        Iterable<AbstractModule> services = serviceLoader.load(service);

        //Then
        assertThat(services)
                .hasSize(1)
                .containsOnly(new TestModule4());
    }
}
