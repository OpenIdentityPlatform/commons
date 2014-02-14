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

import com.google.inject.AbstractModule;
import org.forgerock.guice.core.test.TestModule4;
import org.forgerock.guice.core.test.TestModule5;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class ServiceLoaderTest {

    private ServiceLoader serviceLoader;

    @BeforeClass
    public void setUp() {
        serviceLoader = new ServiceLoader();
    }

    @Test
    public void shouldLoadService() {

        //Given
        Class<AbstractModule> service = AbstractModule.class;

        //When
        final Iterable<AbstractModule> services = serviceLoader.load(service);

        //Then
        boolean foundModule4 = false;
        boolean foundModule5 = false;
        for (final AbstractModule module : services) {
            if (module.getClass().equals(TestModule4.class)) {
                foundModule4 = true;
                continue;
            }
            if (module.getClass().equals(TestModule5.class)) {
                foundModule5 = true;
            }
        }

        assertTrue(foundModule4 && foundModule5);
    }
}
