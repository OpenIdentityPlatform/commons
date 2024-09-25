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


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mojo(name = "asciidoc-pre-process", defaultPhase = LifecyclePhase.SITE)
public class AsciidocPreProcessMojo extends AbstractAsciidocMojo {

    public File getSourceOutputDirectory() {
        return new File(buildDirectory,  "/source");
    }

    public File getSourceOutputPartialsDirectory() {
        return new File(getSourceOutputDirectory(),  "/partials");
    }

    public File getSourceOutputAttachmentsDirectory() {
        return new File(getSourceOutputDirectory(),  "/attachments");
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            Files.createDirectories(getSourceOutputPartialsDirectory().toPath());
        } catch (Exception e) {
            throw new MojoExecutionException("error creating source directory ", e);
        }

        try {
            copyResourcesFolder("/asciidoc/partials", getSourceOutputPartialsDirectory());
        } catch (Exception e) {
            throw new MojoExecutionException("error copying partials content", e);
        }

        for(File docDir : getAsciidocSourceDirectory().listFiles()) {
            String document = FilenameUtils.getBaseName(docDir.toString());

            if(!getDocuments().contains(document)
                    && !document.equals("images")
                    && !document.equals("attachments")
                    && !document.equals("partials")) {
                continue;
            }
            try {
                FileUtils.copyDirectory(docDir, new File(getSourceOutputDirectory(), document));
            } catch (IOException e) {
                throw new MojoExecutionException("error copying " + document, e);
            }
        }
    }

    private void copyResourcesFolder(String resFolder, File outputDir) throws Exception {
        List<Path> files;
        URL partials = getClass().getResource(resFolder);
        try (FileSystem fs = FileSystems.newFileSystem(partials.toURI(), Collections.emptyMap());
             Stream<Path> walk = Files.walk(fs.getPath(resFolder))) {
            files = walk.filter(Files::isRegularFile)
                    .collect(Collectors.toList());
        }
        for(Path file : files) {
            try(InputStream stream = getClass().getResourceAsStream(file.toString())) {
                File dest = new File(outputDir, file.getFileName().toString());
                Files.copy(stream, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
