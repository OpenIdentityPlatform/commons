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
 * Copyright 2024 3A Systems LLC.
 */

package org.openidentityplatform.doc.maven;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.ast.Document;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.dependencies;
import static org.twdata.maven.mojoexecutor.MojoExecutor.dependency;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;

@Mojo(name = "asciidoc-to-pdf", defaultPhase = LifecyclePhase.SITE)
public class AsciidocToPdfMojo extends AbstractAsciidocMojo {


    Set<String> skipDirectories = new HashSet<>();
    public AsciidocToPdfMojo() {
        skipDirectories.add("images");
        skipDirectories.add("partials");
        skipDirectories.add("attachments");
    }

    public File getPdfOutputDirectory() {
        return new File(buildDirectory, "/pdf");
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String pdfPath = getPdfOutputDirectory().getPath();
        for(File docDir : getAsciidocBuildSourceDirectory().listFiles()) {
            String document = FilenameUtils.getBaseName(docDir.toString());
            if(skipDirectories.contains(document)) {
                continue;
            }
            if(!getDocuments().contains(document)) {
                getLog().info("Skip document " + document);
                continue;
            }
            final String fileName;
            try(Asciidoctor asciidoctor = Asciidoctor.Factory.create()) {
                Path index = Paths.get(docDir.toString(), "index.adoc");
                Document indexDoc = asciidoctor.loadFile(index.toFile(), Options.builder().build());
                String docTitle = StringEscapeUtils.unescapeHtml4(indexDoc.getDoctitle());
                fileName = docTitle.replace(" ", "_")
                        .replaceAll("[^A-Za-z0-9_]", "")
                        + ".pdf";

            }

            final URL nestedOpenBlockExt = getClass().getResource("/asciidoc/extenstions/nested-open-block.rb");

            executeMojo(
                    plugin(
                            groupId("org.asciidoctor"),
                            artifactId("asciidoctor-maven-plugin"),
                            "2.2.6",
                            dependencies(
                                    dependency(
                                            groupId("org.asciidoctor"),
                                            artifactId("asciidoctorj-pdf"),
                                            "2.3.18"))),
                    goal("process-asciidoc"),
                    configuration(element("requires"),
                            element("doctype", "book"),
                            element("requires", element("require", nestedOpenBlockExt.getFile())),
                            element("sourceDirectory", docDir.getAbsolutePath()),
                            element("sourceDocumentName", "index.adoc"),
                            element("outputDirectory", pdfPath),
                            element("outputFile", fileName),
                            element("backend", "pdf"),
                            element("attributes", element("source-highlighter", "rouge"))
                    ),
                    executionEnvironment(project, session, pluginManager)
            );

        }
    }
}
