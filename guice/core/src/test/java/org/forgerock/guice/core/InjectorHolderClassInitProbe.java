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

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs inside a class loader of its own, so that {@link InjectorHolder} is initialised there for the first time.
 * Not a test: {@link InjectorHolderClassInitTest} loads and runs it.
 */
public final class InjectorHolderClassInitProbe {

    private static final AtomicInteger LOADER_CALLS = new AtomicInteger();

    private InjectorHolderClassInitProbe() {
    }

    /**
     * Initialises InjectorHolder while a failing module loader is installed, then uses it twice, the second time
     * with a working module loader installed.
     *
     * @return What each step did and how often the module loader was called.
     */
    public static String run() {
        InjectorConfiguration.setGuiceModuleLoader(moduleAnnotation -> {
            LOADER_CALLS.incrementAndGet();
            throw new UnsupportedOperationException("no modules on this class path");
        });
        String init = outcome(() -> InjectorHolder.INSTANCE.name());
        int loaderCallsAfterInit = LOADER_CALLS.get();
        String firstUse = outcome(() -> InjectorHolder.getInstance(Object.class));
        InjectorConfiguration.setGuiceModuleLoader(moduleAnnotation -> Collections.emptySet());
        String secondUse = outcome(() -> InjectorHolder.getInstance(Object.class));
        return "init: " + init + ", loader calls " + loaderCallsAfterInit
                + "; first use: " + firstUse
                + "; second use: " + secondUse + ", loader calls " + LOADER_CALLS.get();
    }

    private static String outcome(Runnable step) {
        try {
            step.run();
            return "ok";
        } catch (Throwable t) {
            Throwable cause = t.getCause();
            String name = t.getClass().getSimpleName();
            return cause == null ? name : name + " caused by " + cause.getClass().getSimpleName();
        }
    }
}
