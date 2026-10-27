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

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;

import org.testng.annotations.Test;

public class GuiceTestCaseFailedSetupTest {

    @Test
    public void shouldUnregisterTheInjectorWhenTheSetupOfASubclassFails() throws Exception {
        try (URLClassLoader isolated = new URLClassLoader(testClassPath(), ClassLoader.getPlatformClassLoader())) {
            Class<?> probe = Class.forName(GuiceTestCaseFailedSetupProbe.class.getName(), true, isolated);

            Object result = probe.getMethod("run").invoke(null);

            assertThat(probe).isNotSameAs(GuiceTestCaseFailedSetupProbe.class);
            assertThat(result)
                    .isEqualTo("setup failed: true, registered in setup: true, registered after the run: false");
        }
    }

    private static URL[] testClassPath() throws Exception {
        String[] entries = System.getProperty("java.class.path").split(File.pathSeparator);
        URL[] urls = new URL[entries.length];
        for (int i = 0; i < entries.length; i++) {
            urls[i] = Paths.get(entries[i]).toUri().toURL();
        }
        return urls;
    }
}
