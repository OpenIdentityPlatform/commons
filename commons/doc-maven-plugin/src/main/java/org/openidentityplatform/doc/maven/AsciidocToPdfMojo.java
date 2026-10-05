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
 * Copyright 2024-2026 3A Systems LLC.
 * Portions Copyright 2026 3A Systems, LLC.
 */

package org.openidentityplatform.doc.maven;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.ast.Document;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * Documentation site that xrefs leaving the rendered guide point to.
     */
    @Parameter(property = "siteUrl", defaultValue = "https://doc.openidentityplatform.org")
    private String siteUrl;

    /**
     * Antora component of this project on the documentation site, such as {@code openidm}.
     *
     * <br>
     *
     * Default: {@code projectName} in lower case.
     */
    @Parameter(property = "antoraComponent")
    private String antoraComponent;

    public File getPdfOutputDirectory() {
        return new File(buildDirectory, "/pdf");
    }

    public File getPdfSourceDirectory() {
        return new File(buildDirectory, "/pdf-source");
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String pdfPath = getPdfOutputDirectory().getPath();
        String component = antoraComponent != null && !antoraComponent.isEmpty()
                ? antoraComponent : projectName.toLowerCase(Locale.ROOT);
        try {
            preparePdfSource(getAsciidocBuildSourceDirectory(), getPdfSourceDirectory(), siteUrl, component);
        } catch (IOException e) {
            throw new MojoExecutionException("error preparing PDF sources", e);
        }
        for(File docDir : getPdfSourceDirectory().listFiles()) {
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

            final URL nestedOpenBlockExt = AsciidocToPdfMojo.class.getResource("/asciidoc/extenstions/nested-open-block.rb");

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

    /**
     * Copies the sources to {@code target} and rewrites there the xrefs that leave a guide,
     * so that the sources the {@code antora} goal reads stay as they are.
     */
    static void preparePdfSource(File source, File target, String siteUrl, String component) throws IOException {
        if (target.exists()) {
            FileUtils.deleteDirectory(target);
        }
        FileUtils.copyDirectory(source, target);
        for (File adocFile : FileUtils.listFiles(target, new String[] {"adoc"}, true)) {
            String module = adocFile.getParentFile().getName();
            String adoc = FileUtils.readFileToString(adocFile, StandardCharsets.UTF_8);
            String converted = convertXrefsForPdf(adoc, siteUrl, component, module);
            if (!converted.equals(adoc)) {
                FileUtils.writeStringToFile(adocFile, converted, StandardCharsets.UTF_8);
            }
        }
    }

    private static final Pattern XREF_PATTERN = Pattern.compile("xref:([^\\[\\s]+)\\[");
    private static final Pattern RESOURCE_ID_PATTERN = Pattern.compile("(?:([^:/@$]+):)?([^:/@$]*):([^:@$]+)");

    /**
     * Plain Asciidoctor knows neither the other guides nor the other Antora components, so it
     * leaves an xref that leaves the current guide as a dead link in the PDF. Turns such xrefs
     * into links to the page on the documentation site:
     * <ul>
     * <li>{@code xref:component:module:page.adoc#anchor[text]} and {@code xref:module:page.adoc#anchor[text]}</li>
     * <li>{@code xref:../module/page.adoc#anchor[text]}</li>
     * </ul>
     * A relative xref into {@code currentModule} becomes a same-guide xref. Other xrefs are left as they are.
     */
    static String convertXrefsForPdf(String adoc, String siteUrl, String component, String currentModule) {
        String site = siteUrl.replaceAll("/+$", "");
        Matcher m = XREF_PATTERN.matcher(adoc);
        StringBuilder builder = new StringBuilder();
        int i = 0;
        while (m.find()) {
            builder.append(adoc, i, m.start());
            builder.append(convertXrefForPdf(m.group(1), site, component, currentModule)).append("[");
            i = m.end();
        }
        builder.append(adoc.substring(i));
        return builder.toString();
    }

    private static String convertXrefForPdf(String target, String site, String component, String currentModule) {
        int hash = target.indexOf('#');
        String path = hash < 0 ? target : target.substring(0, hash);
        String fragment = hash < 0 ? "" : target.substring(hash);

        String targetComponent;
        String module;
        String page;
        if (path.startsWith("../")) {
            String modulePath = path.substring("../".length()).replaceAll("(^|/)(\\./)+", "$1");
            int slash = modulePath.indexOf('/');
            if (slash <= 0) {
                return "xref:" + target;
            }
            targetComponent = component;
            module = modulePath.substring(0, slash);
            page = modulePath.substring(slash + 1);
            if (module.equals(currentModule)) {
                return "xref:" + page + fragment;
            }
        } else {
            Matcher id = RESOURCE_ID_PATTERN.matcher(path);
            if (!id.matches()) {
                return "xref:" + target;
            }
            targetComponent = id.group(1) != null ? id.group(1) : component;
            module = id.group(2).isEmpty() ? "ROOT" : id.group(2);
            page = id.group(3);
        }
        if (page.isEmpty()) {
            return "xref:" + target;
        }
        page = page.replaceAll("\\.adoc$", "");
        // Antora publishes the pages of the ROOT module directly under the component
        String modulePrefix = module.equals("ROOT") ? "" : module + "/";
        return "link:" + site + "/" + targetComponent + "/" + modulePrefix + page + ".html" + fragment;
    }
}
