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

package org.forgerock.doc.maven.backstage;

/**
 * Encapsulation of information about doc artifacts to unpack.
 */
public class ArtifactItem {
    /** Artifact group identifier. */
    String groupId;
    /** Artifact identifier. */
    String artifactId;
    /** Artifact version. */
    String version;
    /** Artifact packaging type. */
    String type;
    /** Artifact classifier. */
    String classifier;
    /** Output directory relative to the Backstage apidocs directory. */
    String outputDirectory;
    /** Title of the document. */
    String title;

    /**
     * Get the artifact group identifier.
     *
     * @return The artifact group identifier.
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Set the artifact group identifier.
     *
     * @param groupId The artifact group identifier.
     */
    void setGroupId(final String groupId) {
        this.groupId = groupId;
    }

    /**
     * Get the artifact identifier.
     *
     * @return The artifact identifier.
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * Set the artifact identifier.
     *
     * @param artifactId The artifact identifier.
     */
    public void setArtifactId(final String artifactId) {
        this.artifactId = artifactId;
    }

    /**
     * Get the artifact version.
     *
     * @return The artifact version.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Set the artifact version.
     *
     * @param version The artifact version.
     */
    public void setVersion(final String version) {
        this.version = version;
    }

    /**
     * Get the artifact packaging type.
     *
     * <br>
     *
     * Default: {@code jar}
     *
     * @return The artifact packaging type.
     */
    public String getType() {
        return type != null ? type : "jar";
    }

    /**
     * Set the artifact packaging type.
     *
     * @param type The artifact packaging type.
     */
    public void setType(final String type) {
        this.type = type;
    }

    /**
     * Get the optional artifact classifier.
     *
     * @return The optional artifact classifier.
     */
    public String getClassifier() {
        return classifier;
    }

    /**
     * Set the optional artifact classifier.
     *
     * @param classifier The optional artifact classifier.
     */
    public void setClassifier(final String classifier) {
        this.classifier = classifier;
    }

    /**
     * Get the output directory relative to the Backstage apidocs directory.
     *
     * @return The output directory relative to the Backstage directory.
     */
    public String getOutputDirectory() {
        return outputDirectory;
    }

    /**
     * Set the output directory relative to the Backstage apidocs directory.
     *
     * @param outputDirectory The output directory relative to the Backstage apidocs directory.
     */
    public void setOutputDirectory(final String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    /**
     * Get the title of the document.
     *
     * @return The title of the document.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set the title of the document.
     *
     * <br>
     *
     * Example: {@code OpenAM ${project.version} Javadoc}
     *
     * @param title The title of the document.
     */
    public void setTitle(final String title) {
        this.title = title;
    }
}
