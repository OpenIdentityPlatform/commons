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
import org.apache.commons.text.TextStringBuilder;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mojo(name = "antora", defaultPhase = LifecyclePhase.SITE)
public class AntoraMojo extends AbstractAsciidocMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {

            String antoraTarget = getAntoraOutputDirectory().toString();
            Path productPath = Paths.get(antoraTarget, "modules");
            if(Files.exists(productPath)) {
                FileUtils.deleteDirectory(productPath.toFile());
            }
            Files.createDirectories(productPath);

            for(File docDir : getAsciidocBuildSourceDirectory().listFiles()) {
                String document = FilenameUtils.getBaseName(docDir.toString());
                if(document.equals("images")) {
                    getLog().info("Copy images to the ROOT module");
                    copyImagesToRoot(docDir);
                    continue;
                }
                if(document.equals("partials")) {
                    getLog().info("Copy partials to the ROOT module");
                    copyPartialsToRoot(docDir);
                    continue;
                }
                if(!getDocuments().contains(document)) {
                    getLog().info("Skip document " + document);
                    continue;
                }
                getLog().info("Convert document " + document);
                convertDocForAntora(productPath, docDir);
            }
            createIndexForRoot();
        } catch (IOException e) {
            throw new MojoExecutionException("Error converting to antora: " + e);
        }
    }

    private void convertDocForAntora(Path productPath, File docDir) throws IOException {

        Path rootPath = Paths.get(productPath.toString(), "ROOT");
        Files.createDirectories(rootPath);

        String document = FilenameUtils.getBaseName(docDir.toString());
        Path docModulePath = Paths.get(productPath.toString(), document);
        Files.createDirectory(docModulePath);

        Path docModulePagesPath = Paths.get(docModulePath.toString(), "pages");
        Files.createDirectory(docModulePagesPath);

        File[] docFiles = docDir.listFiles();
        for (File docFile : docFiles) {
            String adoc = FileUtils.readFileToString(docFile, StandardCharsets.UTF_8);
            if(docFile.getName().equals("index.adoc")) {
                int navStartIndex  = adoc.indexOf("include::./");
                Path indexFilePath = Paths.get(docModulePagesPath.toString(), "index.adoc");
                String index = adoc;
                index = index.replace("include::./", "* xref:");
                FileUtils.writeStringToFile(indexFilePath.toFile(), index, StandardCharsets.UTF_8);

                Path navFilePath = Paths.get(docModulePath.toString(), "nav.adoc");
                String nav = adoc.substring(navStartIndex);
                nav = "* xref:index.adoc[]" + System.lineSeparator() + nav;
                nav = nav.replace("include::./", "** xref:");
                FileUtils.writeStringToFile(navFilePath.toFile(), nav, StandardCharsets.UTF_8);
            } else {
                Path convertedFilePath = Paths.get(docModulePagesPath.toString(), docFile.getName());
                adoc = ":leveloffset: -1" + System.lineSeparator() + adoc;
                adoc = adoc.replace("image::images/", "image::ROOT:");
                adoc = adoc.replace("image:images/", "image:ROOT:");
                adoc = adoc.replace("include::../partials/", "include::ROOT:partial$");

                adoc = convertXrefsToAntora(adoc);

                FileUtils.writeStringToFile(convertedFilePath.toFile(), adoc, StandardCharsets.UTF_8);
            }
        }
    }

    private String convertXrefsToAntora(String adoc) {
        Pattern p = Pattern.compile("xref\\:(.+?)\\[");

        Matcher m = p.matcher(adoc);
        StringBuilder builder = new StringBuilder();
        int i = 0;
        while (m.find()) {
            builder.append(adoc, i, m.start());
            String url = m.group(1);
            url = url.replace("../", "");
            url = url.replace("/", ":");

            builder.append("xref:").append(url).append("[");

            i = m.end();
        }
        builder.append(adoc.substring(i));
        return builder.toString();
    }

    private void createIndexForRoot() throws IOException, MojoExecutionException {
        Path rootPath = getRootModulePath();
        Path pagesPath = Paths.get(rootPath.toString(), "pages");
        if(!Files.exists(pagesPath)) {
            Files.createDirectory(pagesPath);
        }
        Path indexFilePath = Paths.get(pagesPath.toString(), "index.adoc");
        TextStringBuilder builder = new TextStringBuilder();
        builder.append("= ").append(projectName).appendln(" Documentation");
        builder.appendNewLine();
        for(String doc : getDocuments()) {
            builder.append("* xref:").append(doc).appendln(":index.adoc[]");
        }
        builder.appendNewLine();

        FileUtils.writeStringToFile(indexFilePath.toFile(), builder.toString(), StandardCharsets.UTF_8);

    }

    private void copyImagesToRoot(File docDir) throws IOException {
        Path rootPath = getRootModulePath();
        Path rootImagesPath = Paths.get(rootPath.toString(), "images");
        FileUtils.copyDirectory(docDir, rootImagesPath.toFile());
    }
    private void copyPartialsToRoot(File docDir) throws IOException {
        Path rootPath = getRootModulePath();
        Path rootPartialsPath = Paths.get(rootPath.toString(), "partials");
        FileUtils.copyDirectory(docDir, rootPartialsPath.toFile());
    }

    private Path getRootModulePath() throws IOException {
        String antoraTarget = getAntoraOutputDirectory().toString();
        return Paths.get(antoraTarget, "modules", "ROOT");
    }
}
