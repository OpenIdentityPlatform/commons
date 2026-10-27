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

import org.testng.TestNG;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.inject.Injector;

/**
 * Runs {@link FailingSetupTestCase} with TestNG, and reports whether the injector that its setup registered is still
 * registered after the run. {@link GuiceTestCaseFailedSetupTest} loads it through a class loader of its own, so that
 * this run is not reported as part of the run of the build.
 */
public final class GuiceTestCaseFailedSetupProbe {

    private GuiceTestCaseFailedSetupProbe() {
    }

    public static String run() {
        TestNG testNG = new TestNG(false);
        testNG.setVerbose(0);
        testNG.setTestClasses(new Class<?>[] {FailingSetupTestCase.class});
        testNG.run();
        return "setup failed: " + testNG.hasFailure()
                + ", registered in setup: " + (FailingSetupTestCase.registeredInSetup != null)
                + ", registered after the run: " + (InjectorHolder.registeredInjector() != null);
    }

    /** Its own setup fails after GuiceTestCase has registered its injector. */
    public static class FailingSetupTestCase extends GuiceTestCase {

        static volatile Injector registeredInSetup;

        @BeforeMethod
        public void failingSetup() {
            registeredInSetup = InjectorHolder.registeredInjector();
            throw new IllegalStateException("setup failed");
        }

        @Test
        public void test() {
        }
    }
}
