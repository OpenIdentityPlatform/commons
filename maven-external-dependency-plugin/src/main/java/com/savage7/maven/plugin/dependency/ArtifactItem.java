/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 **/
package com.savage7.maven.plugin.dependency;

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.util.StringUtils;

/**
 * ArtifactItem represents information specified in the plugin configuration
 * section for each artifact.
 * 
 * @author <a href="mailto:robert@savage7.com">Robert Savage</a>
 * @see http://code.google.com/p/maven-external-dependency-plugin/
 * @version 0.1
 */

public class ArtifactItem
{
    /**
     * Group Id of Artifact.
     * 
     * @parameter
     * @required
     */
    private String groupId;

    /**
     * Name of Artifact.
     * 
     * @parameter
     * @required
     */
    private String artifactId;

    /**
     * Version of Artifact.
     * 
     * @parameter
     */
    private String version = null;

    /**
     * Classifier for Artifact (tests,sources,etc).
     * 
     * @parameter
     */
    private String classifier;

    /**
     * Local file to download artifact to. Location file to install artifact
     * from.
     * 
     * @parameter default-value="{artifactId}-{version}.{packaging}"
     */
    private String localFile = "{artifactId}-{version}-{classifier}.{packaging}";

    /**
     * URL to download artifact from.
     * 
     * @parameter
     * @required
     */
    private String stagingDirectory;

    /**
     * URL to download artifact from.
     * 
     * @parameter
     */
    private String downloadUrl;

    /**
     * Packaging type of the artifact to be installed.
     * 
     * @parameter default-value="jar"
     * @required
     */
    private String packaging;

    /**
     * Installs the artifact into the local maven repository.
     * 
     * @parameter default-value="true"
     */
    private Boolean install = true;

    /**
     * Deploys the artifact to a remote maven repository.
     * 
     * @parameter default-value="true"
     */
    private Boolean deploy = true;

    /**
     * Forces a download, maven install, maven deploy.
     * 
     * @parameter default-value="false"
     */
    private Boolean force = false;

    /**
     * Location of an existing POM file to be installed alongside the main
     * artifact, given by the {@link #file} parameter.
     * 
     * @parameter expression="${pomFile}"
     */
    private File pomFile;

    /**
     * Generate a minimal POM for the artifact if none is supplied via the
     * parameter {@link #pomFile}. Defaults to <code>true</code> if there is no
     * existing POM in the local repository yet.
     * 
     * @parameter expression="${generatePom}" default-value="true"
     */
    private Boolean generatePom = true;

    /**
     * Flag whether to create checksums (MD5, SHA-1) or not.
     * 
     * @parameter expression="${createChecksum}"
     */
    private String createChecksum;

    /**
     * Checksum for Artifact.
     * 
     * @parameter
     */
    private String checksum;

    /**
     * File name to extract from downloaded ZIP file.
     * 
     * @parameter
     */
    private String extractFile;

    /**
     * default constructor.
     */
    public ArtifactItem()
    {
        // default constructor
    }

    /**
     * alternate constructor.
     * 
     * @param artifact
     *            Artifact
     */
    public ArtifactItem(final Artifact artifact)
    {
        this.setArtifactId(artifact.getArtifactId());
        this.setClassifier(artifact.getClassifier());
        this.setGroupId(artifact.getGroupId());
        this.setPackaging(artifact.getType());
        this.setVersion(artifact.getVersion());
    }

    /**
     * filter empty strings.
     * 
     * @param in
     *            input string to test
     * @return if string was empty as null is returned
     */
    private String filterEmptyString(final String in)
    {
        if (in == null || in.equals(""))
        {
            return null;
        }
        else
        {
            return in;
        }
    }

    /**
     * @return Returns the artifactId.
     */
    public final String getArtifactId()
    {
        return artifactId;
    }

    /**
     * The artifactId to set.
     * 
     * @param artifact
     *            item to set
     */
    public final void setArtifactId(final String artifact)
    {
        this.artifactId = filterEmptyString(artifact);
    }

    /**
     * @return Returns the groupId.
     */
    public final String getGroupId()
    {
        return groupId;
    }

    /**
     * @param groupId
     *            The groupId to set.
     */
    public final void setGroupId(final String groupId)
    {
        this.groupId = filterEmptyString(groupId);
    }

    /**
     * @return Returns the type.
     */
    public final String getType()
    {
        return getPackaging();
    }

    /**
     * @return Returns the version.
     */
    public final String getVersion()
    {
        return version;
    }

    /**
     * @param version
     *            The version to set.
     */
    public final void setVersion(final String version)
    {
        this.version = filterEmptyString(version);
    }

    /**
     * @return Classifier.
     */
    public final String getClassifier()
    {
        return classifier;
    }

    /**
     * @param classifier
     *            Classifier.
     */
    public final void setClassifier(final String classifier)
    {
        this.classifier = filterEmptyString(classifier);
    }

    /**
     * returns a string representations of the artifact item.
     * 
     * @return result string
     */
    public final String toString()
    {
        if (this.classifier == null)
        {
            return groupId + ":" + artifactId + ":"
                + StringUtils.defaultString(version, "?") + ":" + packaging;
        }
        else
        {
            return groupId + ":" + artifactId + ":" + classifier + ":"
                + StringUtils.defaultString(version, "?") + ":" + packaging;
        }
    }

    /**
     * @return Returns the location.
     */
    public final String getLocalFile()
    {
        return replaceTokens(localFile);
    }

    /**
     * @param localFile
     *            The localFile to set.
     */
    public final void setLocalFile(final String localFile)
    {
        this.localFile = filterEmptyString(localFile);
    }

    /**
     * @return Returns the stagingDirectory.
     */
    public final String getStagingDirectory()
    {
        return replaceTokens(stagingDirectory);
    }

    /**
     * @param stagingDirectory
     *            The stagingDirectory to set.
     */
    public final void setStagingDirectory(final String stagingDirectory)
    {
        this.stagingDirectory = filterEmptyString(stagingDirectory);
    }

    /**
     * @return Returns the source URL to download the artifact.
     */
    public final String getDownloadUrl()
    {
        return replaceTokens(downloadUrl);
    }

    /**
     * @param downloadUrl
     *            Set the URL to download the artifact from.
     */
    public final void setDownloadUrl(final String downloadUrl)
    {
        this.downloadUrl = filterEmptyString(downloadUrl);
    }

    /**
     * @return Packaging.
     */
    public final String getPackaging()
    {
        return packaging;
    }

    /**
     * @param packaging
     *            Packaging.
     */
    public final void setPackaging(final String packaging)
    {
        this.packaging = filterEmptyString(packaging);
    }

    /**
     * @return Force.
     */
    public final Boolean getForce()
    {
        return force;
    }

    /**
     * @param force
     *            Force.
     */
    public final void setForce(final Boolean force)
    {
        this.force = force;
    }

    /**
     * @return Install.
     */
    public final Boolean getInstall()
    {
        return install;
    }

    /**
     * @param install
     *            Install.
     */
    public final void setInstall(final Boolean install)
    {
        this.install = install;
    }

    /**
     * @return Deploy.
     */
    public final Boolean getDeploy()
    {
        return deploy;
    }

    /**
     * @param deploy
     *            Deploy.
     */
    public final void setDeploy(final Boolean deploy)
    {
        this.deploy = deploy;
    }

    /**
     * @return PomFile.
     */
    public final File getPomFile()
    {
        return pomFile;
    }

    /**
     * @param pomFile
     *            PomFile.
     */
    public final void setPomFile(final File pomFile)
    {
        this.pomFile = pomFile;
    }

    /**
     * @return GeneratePom.
     */
    public final Boolean getGeneratePom()
    {
        return generatePom;
    }

    /**
     * @param generatePom
     *            GeneratePom.
     */
    public final void setGeneratePom(final Boolean generatePom)
    {
        this.generatePom = generatePom;
    }

    /**
     * @return CreateChecksum.
     */
    public final String getCreateChecksum()
    {
        return createChecksum;
    }

    /**
     * @param createChecksum
     *            CreateChecksum.
     */
    public final void setCreateChecksum(final String createChecksum)
    {
        this.createChecksum = filterEmptyString(createChecksum);
    }

    /**
     * @return Checksum.
     */
    public final String getChecksum()
    {
        return checksum;
    }

    /**
     * @return true is a checksum was defined.
     */
    public final boolean hasChecksum()
    {
        return (checksum != null && !checksum.isEmpty());
    }

    /**
     * @param checksum
     *            Checksum
     */
    public final void setChecksum(final String checksum)
    {
        this.checksum = filterEmptyString(checksum);
    }

    /**
     * @return ExtractFile.
     */
    public final String getExtractFile()
    {
        return replaceTokens(extractFile);
    }

    /**
     * @return true is an extractFile was defined.
     */
    public final boolean hasExtractFile()
    {
        return (extractFile != null && !extractFile.isEmpty());
    }

    /**
     * @param extractFile
     *            ExtractFile
     */
    public final void setExtractFile(final String extractFile)
    {
        this.extractFile = filterEmptyString(extractFile);
    }

    /**
     * replace parameterized tokens in string.
     * 
     * @param source
     *            source string to replace tokens in
     * @return parameterized string
     */
    private String replaceTokens(final String source)
    {
        String target = source;
        if (target == null)
        {
            return null;
        }

        if (target.isEmpty())
        {
            return target;
        }

        // replace all tokens
        if (getGroupId() != null)
        {
            target = target.replace("{groupId}", getGroupId());
        }

        if (getArtifactId() != null)
        {
            target = target.replace("{artifactId}", getArtifactId());
        }

        if (getVersion() != null)
        {
            target = target.replace("{version}", getVersion());
        }

        if (getVersion() != null)
        {
            target = target.replace("{_version}", getVersion()
                .replace(".", "_"));
        }

        if (getPackaging() != null)
        {
            target = target.replace("{packaging}", getPackaging());
        }

        if (getClassifier() != null)
        {
            target = target.replace("{classifier}", getClassifier());
        }
        else
        {
            target = target.replace("-{classifier}", "");
        }

        if (getType() != null)
        {
            target = target.replace("{type}", getType());
        }

        return target;
    }

}
