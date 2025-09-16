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
 * Copyright 2024-2025 3A Systems LLC.
 */

package org.openidentityplatform.doc.maven;


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

        copyDocsDirectories();
        updateVersionAttributes();
    }

    protected void copyDocsDirectories() throws MojoExecutionException {
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

    protected void updateVersionAttributes() {
        String version;
        String url = String.format("https://api.github.com/repos/OpenIdentityPlatform/%s/releases/latest", this.projectName);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                String pattern = "\"name\"\\s*:\\s*\"([^\"]+)\"";
                Pattern regex = Pattern.compile(pattern);
                Matcher matcher = regex.matcher(responseBody);

                if (!matcher.find()) {
                    return;
                }
                version = matcher.group(1);
                setVersion(version);
            }
        } catch (Exception e) {
            getLog().warn("error occurred while getting version", e);
        }
    }

    protected void setVersion(String version) throws IOException {
        String versionShort = version.replaceAll("(\\.\\d+)(?!.*\\d)", "");
        for(File docDir : getSourceOutputDirectory().listFiles()) {
            System.out.println(docDir);
            for(File docFile : docDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".adoc"))) {
                System.out.println(docFile);
                String adoc = FileUtils.readFileToString(docFile, StandardCharsets.UTF_8);
                String versionRegex = String.format("(:%s-version:)\\s*(.*)\\b", this.projectName.toLowerCase());
                adoc = adoc.replaceAll(versionRegex, "$1 " + version);

                String versionShortRegex = String.format("(:%s-version-short:)\\s*(.*)\\b", this.projectName);
                adoc = adoc.replaceAll(versionShortRegex, "$1 " + versionShort);

                FileUtils.writeStringToFile(docFile, adoc, StandardCharsets.UTF_8);
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
