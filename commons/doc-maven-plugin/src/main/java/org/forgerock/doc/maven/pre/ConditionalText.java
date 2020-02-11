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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.doc.maven.pre;

import org.apache.maven.plugin.MojoExecutionException;
import org.forgerock.doc.maven.AbstractDocbkxMojo;
import org.forgerock.doc.maven.utils.Profiler;

import java.io.IOException;

/**
 * Applies profiling to the modifiable copy of DocBook XML sources.
 */
public class ConditionalText {

    /**
     * The Mojo that holds configuration and related methods.
     */
    private AbstractDocbkxMojo m;

    /**
     * Constructor setting the Mojo that holds the configuration.
     *
     * @param mojo The Mojo that holds the configuration.
     */
    public ConditionalText(final AbstractDocbkxMojo mojo) {
        m = mojo;
    }

    /**
     * Applies profiling to the modifiable copy of DocBook XML sources.
     *
     * @throws MojoExecutionException   Failed to apply profiling to an XML file.
     */
    public void execute() throws MojoExecutionException {

        try {
            Profiler profiler = new Profiler(m.getInclusions(), m.getExclusions());
            profiler.applyProfiles(m.getDocbkxModifiableSourcesDirectory());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to apply profiling.", e);
        }
    }
}
