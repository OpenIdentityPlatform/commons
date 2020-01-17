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
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.codehaus.plexus.digest.Digester;

/**
 * Install external dependencies to local repository.
 * 
 * @goal install-external
 * @author <a href="mailto:robert@savage7.com">Robert Savage</a>
 * @see http://code.google.com/p/maven-external-dependency-plugin/
 * @version 0.1
 * @category Maven Plugin
 * @ThreadSafe
 */
public class InstallExternalDependencyMojo extends
    AbstractExternalDependencyMojo
{
    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository localRepository;

    /**
     * @component
     */
    protected ArtifactInstaller installer;

    /**
     * Digester for MD5.
     * 
     * @component default-value="md5"
     */
    protected Digester md5Digester;

    /**
     * Digester for SHA-1.
     * 
     * @component default-value="sha1"
     */
    protected Digester sha1Digester;

    /**
     * Flag whether to create checksums (MD5, SHA-1) or not.
     * 
     * @parameter expression="${createChecksum}" default-value="true"
     */
    protected boolean createChecksum = true;

    public void execute() throws MojoExecutionException, MojoFailureException
    {
        try
        {
            // update base configuration parameters
            // (not sure why this is needed, but doesn't see to work otherwise?)
            super.localRepository = this.localRepository;
            super.createChecksum = this.createChecksum;
            super.md5Digester = this.md5Digester;
            super.sha1Digester = this.sha1Digester;

            Boolean cachedCreateChecksums = this.createChecksum;

            getLog()
                .info(
                    "starting to install external dependencies into local repository");

            // loop over and process all configured artifacts
            for (ArtifactItem artifactItem : artifactItems)
            {
                getLog().info(
                    "resolving artifact for installation: "
                        + artifactItem.toString());

                //
                // CREATE MAVEN ARTIFACT
                //
                Artifact artifact = createArtifact(artifactItem);

                // determine if the artifact is already installed in the local
                // Maven repository
                Boolean artifactAlreadyInstalled = getLocalRepoFile(artifact)
                    .exists();

                // only proceed with this artifact if it is not already
                // installed or it is configured to be forced.
                if (!artifactAlreadyInstalled || artifactItem.getForce()
                    || force)
                {
                    if (artifactItem.getForce())
                    {
                        getLog().debug(
                            "this artifact is flagged as a FORCED install: "
                                + artifactItem.toString());
                    }

                    // ensure the artifact file is located in the staging
                    // directory
                    File stagedArtifactFile = getFullyQualifiedArtifactFilePath(artifactItem);
                    if (stagedArtifactFile.exists())
                    {
                        // if this artifact is configured to extract a file,
                        // then the checksum verification will need to take
                        // place
                        // if there is a separate extract file checksum property
                        // defined
                        if (artifactItem.hasExtractFile())
                        {
                            if (artifactItem.hasExtractFileChecksum())
                            {
                                // verify extracted file checksum (if an extract
                                // file checksum was defined);
                                // 'MojoFailureException' exception will be
                                // thrown if
                                // verification fails
                                verifyArtifactItemExtractFileChecksum(
                                    artifactItem, stagedArtifactFile);
                            }
                        }

                        // if this is not an extracted file, then verify the
                        // downloaded file using the regular checksum property
                        else
                        {
                            // verify file checksum (if a checksum was defined);
                            // 'MojoFailureException' exception will be thrown
                            // if verification fails
                            verifyArtifactItemChecksum(artifactItem,
                                stagedArtifactFile);
                        }

                        // perform Sonatype REST query to ensure that this
                        // artifacts checksum
                        // is not resolved to an existing artifact already
                        // hosted in another
                        // Maven repository
                        verifyArtifactItemChecksumBySonatypeLookup(
                            artifactItem, stagedArtifactFile);

                        //
                        // INSTALL MAVEN ARTIFACT TO LOCAL REPOSITORY
                        //
                        if (artifact != null && artifactItem.getInstall())
                        {
                            // create Maven artifact POM file
                            File generatedPomFile = null;

                            // don't generate a POM file for POM artifacts
                            if (!"pom".equals(artifactItem.getPackaging()))
                            {
                                // if a POM file was provided for the artifact
                                // item, then
                                // use that POM file instead of generating a new
                                // one
                                if (artifactItem.getPomFile() != null)
                                {
                                    ArtifactMetadata pomMetadata = new ProjectArtifactMetadata(
                                        artifact, artifactItem.getPomFile());
                                    getLog().debug(
                                        "installing defined POM file: "
                                            + artifactItem.getPomFile());
                                    artifact.addMetadata(pomMetadata);
                                }
                                else
                                {
                                    // dynamically create a new POM file for
                                    // this artifact
                                    generatedPomFile = generatePomFile(artifactItem);
                                    ArtifactMetadata pomMetadata = new ProjectArtifactMetadata(
                                        artifact, generatedPomFile);

                                    if (artifactItem.getGeneratePom() == true)
                                    {
                                        getLog().debug(
                                            "installing generated POM file: "
                                                + generatedPomFile
                                                    .getCanonicalPath());
                                        artifact.addMetadata(pomMetadata);
                                    }
                                }
                            }

                            getLog().info(
                                "installing artifact into local repository: "
                                    + localRepository.getId());

                            // install artifact to local repository
                            installer.install(stagedArtifactFile, artifact,
                                localRepository);

                            // install checksum files to local repository
                            if (artifactItem.getCreateChecksum() != null)
                            {
                                super.createChecksum = artifactItem
                                    .getCreateChecksum().equalsIgnoreCase(
                                        "true");
                            }
                            else
                            {
                                super.createChecksum = cachedCreateChecksums;
                            }
                            installChecksums(artifact);
                        }
                        else
                        {
                            getLog().debug(
                                "configured to not install artifact: "
                                    + artifactItem.toString());
                        }
                    }
                    else
                    {
                        // throw error because we were unable to install the
                        // external dependency
                        throw new MojoFailureException(
                            "Unable to install external dependency '"
                                + artifactItem.getArtifactId()
                                + "'; file not found in staging path: "
                                + stagedArtifactFile.getCanonicalPath());
                    }
                }
                else
                {
                    getLog()
                        .info(
                            "this aritifact already exists in the local repository; no download is needed: "
                                + artifactItem.toString());
                }
            }

            getLog()
                .info(
                    "finished installing all external dependencies into local repository");
        }
        catch (MojoFailureException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            getLog().error(e);
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
