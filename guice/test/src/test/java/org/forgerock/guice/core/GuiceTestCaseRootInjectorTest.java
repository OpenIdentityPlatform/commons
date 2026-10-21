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
 * Copyright 2026 3A Systems, LLC.
 */

package org.forgerock.guice.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.inject.Binder;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class GuiceTestCaseRootInjectorTest {

    private GuiceModuleLoader defaultModuleLoader;

    @BeforeMethod
    public void setUp() {
        defaultModuleLoader = InjectorConfiguration.getGuiceModuleLoader();
        InjectorHolder.reset();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        InjectorHolder.reset();
        InjectorConfiguration.setGuiceModuleLoader(defaultModuleLoader);
    }

    @Test
    public void shouldNotBuildTheRootInjectorAroundATestMethod() throws Exception {
        AtomicInteger loaderCalls = new AtomicInteger();
        InjectorConfiguration.setGuiceModuleLoader(moduleAnnotation -> {
            loaderCalls.incrementAndGet();
            throw new UnsupportedOperationException("the root injector must not be built");
        });
        BindingTestCase testCase = new BindingTestCase();

        testCase.setupGuiceModules();
        Bound bound = InjectorHolder.getInstance(Bound.class);
        testCase.teardownGuiceModules();

        assertThat(bound).isInstanceOf(BoundImpl.class);
        assertThat(InjectorHolder.registeredInjector()).isNull();
        assertThat(loaderCalls).hasValue(0);
    }

    public static class BindingTestCase extends GuiceTestCase {

        @Override
        public void configure(Binder binder) {
            binder.bind(Bound.class).to(BoundImpl.class);
        }
    }

    public interface Bound {
    }

    public static class BoundImpl implements Bound {
    }
}
