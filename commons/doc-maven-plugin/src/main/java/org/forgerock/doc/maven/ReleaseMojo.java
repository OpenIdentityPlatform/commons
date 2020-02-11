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
 * Copyright 2014-2015 ForgeRock AS
 */

package org.forgerock.doc.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.forgerock.doc.maven.release.Css;
import org.forgerock.doc.maven.release.Favicon;
import org.forgerock.doc.maven.release.IndexHtml;
import org.forgerock.doc.maven.release.Layout;
import org.forgerock.doc.maven.release.PdfNames;
import org.forgerock.doc.maven.release.Robots;
import org.forgerock.doc.maven.release.Zip;

/**
 * Call other classes to prepare release layout documents.
 */
@Mojo(name = "release", defaultPhase = LifecyclePhase.SITE)
public class ReleaseMojo extends AbstractDocbkxMojo {

    /**
     * Call other classes to prepare release layout documents.
     *
     * @throws MojoExecutionException Failed to prepare docs successfully.
     */
    @Override
    public void execute() throws MojoExecutionException {
        new Layout(this).execute();
        new IndexHtml(this).execute();
        new PdfNames(this).execute();
        new Favicon(this).execute();
        new Css(this).execute();
        new Robots(this).execute();
        new Zip(this).execute();
    }
}
