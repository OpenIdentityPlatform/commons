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
 * Portions Copyrighted 2026 3A Systems, LLC
 **/
package com.savage7.maven.plugin.dependency;

import java.io.File;
import java.util.Objects;

import org.apache.maven.artifact.Artifact;

/**
 * ArtifactItem represents information specified in the plugin configuration
 * section for each artifact.
 * 
 * @author <a href="mailto:robert@savage7.com">Robert Savage</a>
 * @see <a href="http://code.google.com/p/maven-external-dependency-plugin/">http://code.google.com/p/maven-external-dependency-plugin/</a>
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
     * Timeout in millis allowed for artifact download
     *
     * @parameter
     */
    private Integer timeout;

    /**
     * Per-artifact override for the number of attempts to download this
     * artifact in case of transient network failures. When unset, the
     * Mojo-level {@code downloadRetryAttempts} parameter (default 5) is used.
     *
     * @parameter
     */
    private Integer retryAttempts;

    /**
     * Per-artifact override for the delay in millis between download retry
     * attempts. When unset, the Mojo-level {@code downloadRetryDelay}
     * parameter (default 2000 ms) is used.
     *
     * @parameter
     */
    private Integer retryDelay;

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
     * If this property is set to true, then the 
     * downloaded file's checksum will not be 
     * verified using the Sonatype artifact query
     * by checksum validation routine.
     * 
     * @parameter default-value="false"
     */
    private Boolean skipChecksumVerification = false;

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
     * File checksum from file that was extracted from downloaded ZIP file.
     * 
     * @parameter
     */
    private String extractFileChecksum;
    
    /**
     * In case you need to repack an directory as a new artifact
     * 
     * @parameter
     */
    private boolean repack = false;

    /**
     * Flag whether to attempt extracting POM from the JAR's META-INF directory.
     * When true (default), the plugin will look for an embedded POM at
     * META-INF/maven/{groupId}/{artifactId}/pom.xml inside the JAR and use it
     * instead of generating a minimal one.
     *
     * @parameter default-value="true"
     */
    private Boolean extractPom = true;
    
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
        if (in == null || in.isEmpty())
        {
            return null;
        }
        else
        {
            return in;
        }
    }

    /**
     * Returns the artifact identifier.
     *
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
     * Returns the group identifier.
     *
     * @return Returns the groupId.
     */
    public final String getGroupId()
    {
        return groupId;
    }

    /**
     * Sets the group identifier.
     *
     * @param groupId
     *            The groupId to set.
     */
    public final void setGroupId(final String groupId)
    {
        this.groupId = filterEmptyString(groupId);
    }

    /**
     * Returns the artifact type (its packaging).
     *
     * @return Returns the type.
     */
    public final String getType()
    {
        return getPackaging();
    }

    /**
     * Returns the artifact version.
     *
     * @return Returns the version.
     */
    public final String getVersion()
    {
        return version;
    }

    /**
     * Sets the artifact version.
     *
     * @param version
     *            The version to set.
     */
    public final void setVersion(final String version)
    {
        this.version = filterEmptyString(version);
    }

    /**
     * Returns the artifact classifier.
     *
     * @return Classifier.
     */
    public final String getClassifier()
    {
        return classifier;
    }

    /**
     * Sets the artifact classifier.
     *
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
    @Override
    public final String toString()
    {
        if (this.classifier == null)
        {
            return groupId + ":" + artifactId + ":"
                + Objects.toString(version, "?") + ":" + packaging;
        }
        else
        {
            return groupId + ":" + artifactId + ":" + classifier + ":"
                + Objects.toString(version, "?") + ":" + packaging;
        }
    }

    /**
     * Returns the local file path, with tokens resolved.
     *
     * @return Returns the location.
     */
    public final String getLocalFile()
    {
        return replaceTokens(localFile);
    }

    /**
     * Sets the local file path.
     *
     * @param localFile
     *            The localFile to set.
     */
    public final void setLocalFile(final String localFile)
    {
        this.localFile = filterEmptyString(localFile);
    }

    /**
     * Returns the staging directory, with tokens resolved.
     *
     * @return Returns the stagingDirectory.
     */
    public final String getStagingDirectory()
    {
        return replaceTokens(stagingDirectory);
    }

    /**
     * Sets the staging directory.
     *
     * @param stagingDirectory
     *            The stagingDirectory to set.
     */
    public final void setStagingDirectory(final String stagingDirectory)
    {
        this.stagingDirectory = filterEmptyString(stagingDirectory);
    }

    /**
     * Returns the source URL the artifact is downloaded from.
     *
     * @return Returns the source URL to download the artifact.
     */
    public final String getDownloadUrl()
    {
        return replaceTokens(downloadUrl);
    }

    /**
     * Sets the source URL to download the artifact from.
     *
     * @param downloadUrl
     *            Set the URL to download the artifact from.
     */
    public final void setDownloadUrl(final String downloadUrl)
    {
        this.downloadUrl = filterEmptyString(downloadUrl);
    }

    /**
     * Returns the download timeout in milliseconds.
     *
     * @return Returns the timeout in millis allowed for artifact download.
     */
    public final Integer getTimeout()
    {
        return (timeout==null||timeout<=0)?10000:timeout;
    }

    /**
     * Returns the raw, unresolved download timeout.
     *
     * @return Raw timeout value as configured (may be null) so callers can
     *         distinguish an explicitly set per-artifact timeout from the
     *         default fallback returned by {@link #getTimeout()}.
     */
    public final Integer getTimeoutRaw()
    {
        return timeout;
    }

    /**
     * Sets the download timeout in milliseconds.
     *
     * @param timeout
     *            Set the timeout in millis allowed for artifact download.
     */
    public final void setTimeout(final Integer timeout)
    {
        this.timeout = timeout;
    }

    /**
     * Returns the configured number of download retry attempts.
     *
     * @return Raw retry attempts value as configured (may be null).
     */
    public final Integer getRetryAttempts()
    {
        return retryAttempts;
    }

    /**
     * Sets the number of download retry attempts.
     *
     * @param retryAttempts
     *            Number of attempts to download the artifact in case of
     *            transient network failures.
     */
    public final void setRetryAttempts(final Integer retryAttempts)
    {
        this.retryAttempts = retryAttempts;
    }

    /**
     * Returns the configured delay between download retries.
     *
     * @return Raw retry delay value (in millis) as configured (may be null).
     */
    public final Integer getRetryDelay()
    {
        return retryDelay;
    }

    /**
     * Sets the delay between download retries.
     *
     * @param retryDelay
     *            Delay in millis between download retry attempts.
     */
    public final void setRetryDelay(final Integer retryDelay)
    {
        this.retryDelay = retryDelay;
    }

    /**
     * Returns the artifact packaging.
     *
     * @return Packaging.
     */
    public final String getPackaging()
    {
        return packaging;
    }

    /**
     * Sets the artifact packaging.
     *
     * @param packaging
     *            Packaging.
     */
    public final void setPackaging(final String packaging)
    {
        this.packaging = filterEmptyString(packaging);
    }

    /**
     * Returns whether the artifact download is forced.
     *
     * @return Force.
     */
    public final Boolean getForce()
    {
        return force;
    }

    /**
     * Sets whether the artifact download is forced.
     *
     * @param force
     *            Force.
     */
    public final void setForce(final Boolean force)
    {
        this.force = force;
    }

    /**
     * Returns whether the artifact is installed.
     *
     * @return Install.
     */
    public final Boolean getInstall()
    {
        return install;
    }

    /**
     * Sets whether the artifact is installed.
     *
     * @param install
     *            Install.
     */
    public final void setInstall(final Boolean install)
    {
        this.install = install;
    }

    /**
     * Returns whether the artifact is deployed.
     *
     * @return Deploy.
     */
    public final Boolean getDeploy()
    {
        return deploy;
    }

    /**
     * Sets whether the artifact is deployed.
     *
     * @param deploy
     *            Deploy.
     */
    public final void setDeploy(final Boolean deploy)
    {
        this.deploy = deploy;
    }

    /**
     * Returns the POM file associated with the artifact.
     *
     * @return PomFile.
     */
    public final File getPomFile()
    {
        return pomFile;
    }

    /**
     * Sets the POM file associated with the artifact.
     *
     * @param pomFile
     *            PomFile.
     */
    public final void setPomFile(final File pomFile)
    {
        this.pomFile = pomFile;
    }

    /**
     * Returns whether a POM is generated for the artifact.
     *
     * @return GeneratePom.
     */
    public final Boolean getGeneratePom()
    {
        return generatePom;
    }

    /**
     * Sets whether a POM is generated for the artifact.
     *
     * @param generatePom
     *            GeneratePom.
     */
    public final void setGeneratePom(final Boolean generatePom)
    {
        this.generatePom = generatePom;
    }

    /**
     * Returns the checksum-creation setting.
     *
     * @return CreateChecksum.
     */
    public final String getCreateChecksum()
    {
        return createChecksum;
    }

    /**
     * Sets the checksum-creation setting.
     *
     * @param createChecksum
     *            CreateChecksum.
     */
    public final void setCreateChecksum(final String createChecksum)
    {
        this.createChecksum = filterEmptyString(createChecksum);
    }

    /**
     * Returns the expected checksum of the artifact.
     *
     * @return Checksum.
     */
    public final String getChecksum()
    {
        return checksum;
    }

    /**
     * Returns whether a checksum is defined.
     *
     * @return true is a checksum was defined.
     */
    public final boolean hasChecksum()
    {
        return (checksum != null && !checksum.isEmpty());
    }

    /**
     * Returns whether a checksum is defined for an extracted file.
     *
     * @return true is a checksum was defined for an extracted file.
     */
    public final boolean hasExtractFileChecksum()
    {
        return (hasChecksum() && extractFileChecksum != null && !extractFileChecksum.isEmpty());
    }
    
    /**
     * Returns the expected checksum of the extracted file.
     *
     * @return Extracted File Checksum.
     */    
    public final String getExtractFileChecksum()
    {
        return extractFileChecksum;
    }
    
    
    /**
     * Sets the expected checksum of the artifact.
     *
     * @param checksum
     *            Checksum
     */
    public final void setChecksum(final String checksum)
    {
        this.checksum = filterEmptyString(checksum);
    }

    /**
     * Returns whether checksum verification is skipped.
     *
     * @return SkipChecksumVerification.
     */
    public final Boolean getSkipChecksumVerification()
    {
        return skipChecksumVerification;
    }

    /**
     * Sets whether checksum verification is skipped.
     *
     * @param skipChecksumVerification
     *            SkipChecksumVerification.
     */
    public final void setSkipChecksumVerification(final Boolean skipChecksumVerification)
    {
        this.skipChecksumVerification = skipChecksumVerification;
    }

    
    
    /**
     * Returns the path of the file to extract, with tokens resolved.
     *
     * @return ExtractFile.
     */
    public final String getExtractFile()
    {
        return replaceTokens(extractFile);
    }

    /**
     * Returns whether a file to extract is defined.
     *
     * @return true is an extractFile was defined.
     */
    public final boolean hasExtractFile()
    {
        return (extractFile != null && !extractFile.isEmpty());
    }

    /**
     * Sets the path of the file to extract.
     *
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

    /**
     * Returns whether the artifact should be repacked.
     *
     * @return <code>true</code> if the artifact should be repacked
     */
    public boolean isRepack()
    {
        return repack;
    }

    /**
     * Sets whether the artifact should be repacked.
     *
     * @param repack
     *            <code>true</code> if the artifact should be repacked
     */
    public void setRepack(boolean repack)
    {
        this.repack = repack;
    }

    /**
     * Returns whether the POM is extracted from the artifact.
     *
     * @return ExtractPom.
     */
    public final Boolean getExtractPom()
    {
        return extractPom;
    }

    /**
     * Sets whether the POM is extracted from the artifact.
     *
     * @param extractPom
     *            ExtractPom.
     */
    public final void setExtractPom(final Boolean extractPom)
    {
        this.extractPom = extractPom;
    }

}
