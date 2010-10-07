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
import java.io.IOException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;

/**
 * Deploy external dependencies to distribution management defined repository.
 *
 * @goal deploy-external
 * @author <a href="mailto:robert@savage7.com">Robert Savage</a>
 * @see http://code.google.com/p/maven-external-dependency-plugin/
 * @version 0.1
 * @category Maven Plugin
 * @ThreadSafe
 */
public class DeployExternalDependencyMojo extends
        AbstractExternalDependencyMojo
{
    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository localRepository;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @parameter expression=
     *            "${component.org.apache.maven.artifact.resolver.ArtifactResolver}"
     * @required
     * @readonly
     */
    protected ArtifactResolver artifactResolver;

    /**
     * @component
     */
    private ArtifactDeployer artifactDeployer;

    /**
     * Flag whether Maven is currently in online/offline mode.
     *
     * @parameter default-value="${settings.offline}"
     * @readonly
     */
    private boolean offline;

    public void execute() throws MojoExecutionException, MojoFailureException
    {
        // update base configuration parameters
        // (not sure why this is needed, but doesn't see to work otherwise?)
        super.localRepository = this.localRepository;

        getLog()
                .info(
                        "starting to deploy external dependencies to distribution repository");

        // loop over and process all configured artifacts
        for (ArtifactItem artifactItem : artifactItems)
        {
            getLog().info(
                    "resolving artifact in locale repository for deployment: "
                            + artifactItem.toString());

            //
            // CREATE MAVEN ARTIFACT
            //
            Artifact artifact = createArtifact(artifactItem);

            // determine if the artifact is already installed in the local Maven
            // repository
            File installedArtifactFile = getLocalRepoFile(artifact);

            // only proceed with this artifact if it is not already
            // installed or it is configured to be forced.
            if (installedArtifactFile.exists())
            {
                try
                {
                    artifactResolver.resolve(artifact, project.getRemoteArtifactRepositories(), localRepository);

                    //
                    // DEPLOY TO DISTRIBUTION MAVEN REPOSITORY
                    //
                    if (artifactItem.getDeploy())
                    {
                        failIfOffline();

                        ArtifactRepository repo = getDeploymentRepository();

                        String protocol = repo.getProtocol();

                        if (protocol.equalsIgnoreCase("scp"))
                        {
                            File sshFile = new File(System
                                    .getProperty("user.home"), ".ssh");

                            if (!sshFile.exists())
                            {
                                sshFile.mkdirs();
                            }
                        }

                        // create Maven artifact POM file
                        File generatedPomFile = null;

                        // don't generate a POM file for POM artifacts
                        if (!"pom".equals(artifactItem.getPackaging()))
                        {
                            // if a POM file was provided for the artifact item,
                            // then
                            // use that POM file instead of generating a new one
                            if (artifactItem.getPomFile() != null)
                            {
                                ArtifactMetadata pomMetadata = new ProjectArtifactMetadata(
                                        artifact, artifactItem.getPomFile());
                                artifact.addMetadata(pomMetadata);
                            }
                            else
                            {
                                // dynamically create a new POM file for this
                                // artifact
                                generatedPomFile = generatePomFile(artifactItem);
                                ArtifactMetadata pomMetadata = new ProjectArtifactMetadata(
                                        artifact, generatedPomFile);

                                if (artifactItem.getGeneratePom() == true)
                                {
                                    artifact.addMetadata(pomMetadata);
                                }
                            }

                        }

                        // deploy now
                        getLog().info(
                                "deploying artifact to distribution repository: "
                                        + artifactItem.toString());
                        artifactDeployer.deploy(artifact.getFile(), artifact,
                                repo, localRepository);

                        //TODO Deploy Checksums?
                    }
                    else
                    {
                        getLog().debug(
                                "configured to not deploy artifact: "
                                        + artifactItem.toString());
                    }
                }
                catch (MojoFailureException e)
                {
                    throw e;
                }
                catch (ArtifactResolutionException e)
                {
                    throw new MojoExecutionException(
                            "Error occurred while attempting to resolve artifact.",
                            e);
                }
                catch (ArtifactNotFoundException e)
                {
                    throw new MojoExecutionException(
                            "Unable to find external dependency in local repository.",
                            e);
                }
                catch (ArtifactDeploymentException e)
                {
                    throw new MojoExecutionException(
                            "Deployment of external dependency failed.", e);
                }

            }
            else
            {
                // throw error because we were unable to find the installed
                // external dependency
                try
                {
                    throw new MojoFailureException(
                            "Unable to find external dependency '"
                                    + artifactItem.getArtifactId()
                                    + "'; file not found in local repository: "
                                    + installedArtifactFile.getCanonicalPath());
                }
                catch (IOException e)
                {
                    throw new MojoExecutionException(
                            "Unable to resolve dependency path in locale repository.",
                            e);
                }
            }
        }

        getLog()
                .info(
                        "finished deploying external dependencies to distribution repository");
    }

    /**
     * Gets the repository defined in project POM's distribution management
     * section
     *
     * @return deployment repository defined in distribution management
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    private ArtifactRepository getDeploymentRepository()
            throws MojoExecutionException, MojoFailureException
    {
        ArtifactRepository repo = null;

        if (repo == null)
        {
            repo = project.getDistributionManagementArtifactRepository();
        }

        if (repo == null)
        {
            String msg = "Deployment failed: repository element was not specified in the POM inside"
                    + " distributionManagement element";

            throw new MojoExecutionException(msg);
        }

        return repo;
    }

    /**
     * Checks for offline mode; throws exception if offline, deploy goal cannot
     * proceed
     *
     * @throws MojoFailureException
     */
    private void failIfOffline() throws MojoFailureException
    {
        if (offline)
        {
            throw new MojoFailureException(
                    "Cannot deploy artifacts when Maven is in offline mode");
        }
    }
}
