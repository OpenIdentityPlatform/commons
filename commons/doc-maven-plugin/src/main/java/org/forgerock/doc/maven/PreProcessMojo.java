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

package org.forgerock.doc.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.forgerock.doc.maven.pre.ArtifactBuilder;
import org.forgerock.doc.maven.pre.AsciidocToDocBook;
import org.forgerock.doc.maven.pre.Branding;
import org.forgerock.doc.maven.pre.CommonContent;
import org.forgerock.doc.maven.pre.ConditionalText;
import org.forgerock.doc.maven.pre.CurrentDocId;
import org.forgerock.doc.maven.pre.CustomCss;
import org.forgerock.doc.maven.pre.CreateThumbs;
import org.forgerock.doc.maven.pre.Dpi;
import org.forgerock.doc.maven.pre.Filter;
import org.forgerock.doc.maven.pre.HeaderColor;
import org.forgerock.doc.maven.pre.ImageData;
import org.forgerock.doc.maven.pre.JCite;
import org.forgerock.doc.maven.pre.KeepTogether;
import org.forgerock.doc.maven.pre.ModifiableCopy;
import org.forgerock.doc.maven.pre.PlantUml;
import org.forgerock.doc.maven.pre.XCite;

/**
 * Call other classes to pre-process documentation sources.
 */
@Mojo(name = "process", defaultPhase = LifecyclePhase.PRE_SITE)
public class PreProcessMojo extends AbstractDocbkxMojo {

    /**
     * Call other classes to pre-process documentation sources.
     *
     * @throws MojoExecutionException   Failed to process successfully.
     * @throws MojoFailureException     Failed to process successfully.
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (getBuildDirectory() == null) {
            throw new MojoExecutionException("No build directory available.");
        }

        if (!getBuildDirectory().exists()) {
            if (!getBuildDirectory().mkdir()) {
                throw new MojoExecutionException("Could not create build directory");
            }
        }

        // Get branding.
        new Branding(this).execute();

        // Make a copy of the source files that the plugin can edit.
        new ModifiableCopy(this).execute();
        new ConditionalText(this).execute();

        if (!doUsePreProcessedSources()) {  // Sources require pre-processing.
            new CommonContent(this).execute();
            new AsciidocToDocBook(this).execute();
            new JCite(this).execute();
            new XCite(this).execute();
            new Filter(this).execute();
            new ImageData(this).execute();
            new HeaderColor(this).execute();
            new PlantUml(this).execute();
            if (getFormats().contains(Format.pdf) || getFormats().contains(Format.rtf)) {
                new KeepTogether(this).execute();
                new Dpi(this).execute();
            }
            if (getFormats().contains(Format.bootstrap)) {
                new CreateThumbs(this).execute();
            }
            new CurrentDocId(this).execute();
            new CustomCss(this).execute();
        }

        if (doCreateArtifacts()) {
            new ArtifactBuilder(this).execute();
        }
    }
}
