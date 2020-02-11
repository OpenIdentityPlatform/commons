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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.forgerock.doc.maven.backstage.ArtifactDocs;
import org.forgerock.doc.maven.backstage.Pdf;
import org.forgerock.json.fluent.JsonValue;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;

/**
 * Call other classes to prepare documents according to Backstage layout.
 *
 * <br>
 *
 * Backstage layout puts content in specific folders,
 * and includes JSON-based meta-information files.
 *
 * <ul>
 *
 * <li><code>apidocs/</code> contains folders of any generated HTML-based documentation,
 * such as Javadoc, that is not built from normal documentation sources,
 * including a <code>meta.json</code> file inside each folder to specify the name of the document.
 * An example <code>meta.json</code> file looks like this:
 * <pre>
 * {
 *   "title": "OpenAM 12.0.0 Javadoc"
 * }
 * </pre>
 * </li>
 *
 * <li><code>docbook/</code> contains the pre-processed DocBook XML sources
 * suitable for formatting by a separate program.</li>
 *
 * <li><code>docset.json</code> specifies meta information
 * about the documentation set. For example:
 * <pre>
 * {
 *   "product": "OpenAM",
 *   "version": "12.0.0",
 *   "language": "en",
 *   "released": "2014-12-17"
 * }
 * </pre>
 * </li>
 *
 * <li><code>pdf/</code> contains PDF files
 * corresponding to the DocBook XML sources,
 * named as <code><i>Product-from-docset-json</i>-<i>Version</i>-<i>Doc-name</i>.pdf</code>.</li>
 *
 * </ul>
 *
 *
 */
@Mojo(name = "backstage", defaultPhase = LifecyclePhase.SITE)
public class BackstageMojo extends AbstractDocbkxMojo {

    /**
     * Call other classes to prepare documents according to Backstage layout.
     *
     * @throws MojoExecutionException Failed to prepare docs successfully.
     * @throws MojoFailureException   Failed to complete processing.
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (getBackstageProductName().isEmpty()) {
            throw new MojoFailureException("You must set backstageProductName.");
        }

        new ArtifactDocs(this).execute();
        final File docbookDirectory = new File(getBackstageDirectory(), "docbook");
        try {
            FileUtils.copyDirectory(
                    getDocbkxModifiableSourcesDirectory(), docbookDirectory, FileFilterUtils.trueFileFilter());
        } catch (IOException e) {
            throw new MojoFailureException("Failed to copy pre-processed sources.", e);
        }
        writeDocsetJson();
        new Pdf(this).execute();
    }

    /**
     * Writes a {@code docset.json} file to the Backstage directory.
     * @throws MojoFailureException Failed to write {@code docset.json}.
     */
    private void writeDocsetJson() throws MojoFailureException {
        final JsonValue jsonValue = new JsonValue(new LinkedHashMap<String, Object>())
                .add("product", getBackstageProductName())
                .add("version", getProjectVersion())
                .add("language", getLocaleTag())
                .add("released", getReleaseDate());
        final File file = new File(getBackstageDirectory(), "docset.json");
        try {
            FileUtils.writeStringToFile(file, jsonValue.toString());
        } catch (IOException e) {
            throw new MojoFailureException("Failed to write :" + file.getPath());
        }
    }
}
