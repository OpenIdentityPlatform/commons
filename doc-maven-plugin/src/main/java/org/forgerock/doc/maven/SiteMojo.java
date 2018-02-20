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
import org.forgerock.doc.maven.site.Htaccess;
import org.forgerock.doc.maven.site.Layout;
import org.forgerock.doc.maven.site.LinkTest;
import org.forgerock.doc.maven.site.Redirect;

/**
 * Call other classes to copy docs to site build directory.
 */
@Mojo(name = "site", defaultPhase = LifecyclePhase.SITE)
public class SiteMojo extends AbstractDocbkxMojo {

    /**
     * Call other classes to copy docs to site build directory.
     *
     * @throws MojoExecutionException Failed to copy docs successfully.
     */
    @Override
    public void execute() throws MojoExecutionException {
        new Layout(this).execute();
        new Htaccess(this).execute();
        new Redirect(this).execute();
        new LinkTest(this).execute();
    }
}
