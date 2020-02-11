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
 * Copyright 2015 ForgeRock AS
 */

package org.forgerock.doc.maven.pre;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.forgerock.doc.maven.AbstractDocbkxMojo;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * Build Maven artifact from pre-processed documents.
 */
public class ArtifactBuilder {

    /**
     * The Mojo that holds configuration and related methods.
     */
    private AbstractDocbkxMojo m;

    /**
     * The Executor to run plugins.
     */
    private final Executor executor;


    /**
     * FreeMarker configuration for the assembly descriptor template.
     */
    private Configuration configuration;

    /**
     * Constructor setting the Mojo that holds the configuration.
     *
     * @param mojo The Mojo that holds the configuration.
     */
    public ArtifactBuilder(final AbstractDocbkxMojo mojo) {
        m = mojo;
        this.executor = new Executor();

        // Set up the FreeMarker configuration.
        configuration = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        configuration.setClassForTemplateLoading(ArtifactBuilder.class, "/templates");
        configuration.setDefaultEncoding("UTF-8");
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.DEBUG_HANDLER);
    }

    /**
     * Get the path to the assembly descriptor.
     *
     * @return The path to the assembly descriptor.
     */
    private String getDescriptorPath(File descriptorFile) throws IOException, TemplateException {
        Template template = configuration.getTemplate("artifact.ftl");

        FileOutputStream out = FileUtils.openOutputStream(descriptorFile);

        Writer writer = new OutputStreamWriter(out);
        template.process(getModel(), writer);
        writer.close();
        out.close();

        return descriptorFile.getPath();
    }

    /**
     * Get the model used to build the assembly descriptor.
     *
     * @return The model used to build the assembly descriptor.
     */
    private Map<String, String> getModel() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("modifiedSourcesDirectory", m.path(m.getDocbkxModifiableSourcesDirectory()));
        return map;
    }

    /**
     * Build artifact.
     *
     * @throws MojoExecutionException Failed to build artifact.
     */
    public void execute() throws MojoExecutionException {
        executor.build();
    }

    /**
     * Enclose methods to run plugins.
     */
    class Executor extends MojoExecutor {

        /**
         * Build the Maven artifact.
         *
         * @throws MojoExecutionException Failed to build the artifact.
         */
        public void build() throws MojoExecutionException {

            File descriptorFile;
            String descriptorPath;
            try {
                descriptorFile = File.createTempFile("descriptor", "xml");
                descriptorPath = getDescriptorPath(descriptorFile);
            } catch (Exception e) {
                throw new MojoExecutionException("Failed to get assembly descriptor", e);
            }

            executeMojo(
                    plugin(
                            groupId("org.apache.maven.plugins"),
                            artifactId("maven-assembly-plugin"),
                            version(m.getMavenAssemblyVersion())),
                    goal("single"),
                    configuration(
                            element("descriptors",
                                    element("descriptor", descriptorPath))),
                    executionEnvironment(m.getProject(), m.getSession(), m.getPluginManager()));

            if (descriptorFile.exists()) {
                descriptorFile.delete();
            }
        }
    }
}
