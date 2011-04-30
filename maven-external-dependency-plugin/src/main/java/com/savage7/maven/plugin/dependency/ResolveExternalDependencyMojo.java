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
import java.net.URL;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.FileUtils;

/**
 * Download/Acquire external Maven artifacts, copy to staging directory.
 * 
 * @goal resolve-external
 * @author <a href="mailto:robert@savage7.com">Robert Savage</a>
 * @see http://code.google.com/p/maven-external-dependency-plugin/
 * @version 0.1
 * @category Maven Plugin
 * @ThreadSafe
 */
public class ResolveExternalDependencyMojo extends
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
     * List of Remote Repositories used by the resolver
     * 
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    @SuppressWarnings("unchecked")
    protected java.util.List remoteRepositories;

    /**
     * @component
     * @readonly
     */
    protected ArchiverManager archiverManager;

    public void execute() throws MojoExecutionException, MojoFailureException
    {
        try
        {
            // update base configuration parameters
            // (not sure why this is needed, but
            // doesn't see to work otherwise?)
            super.localRepository = this.localRepository;

            getLog().info("starting to resolve external dependencies");

            // get a set of all project artifacts
            // Set<Artifact> projectArtifacts = project.createArtifacts(
            // artifactFactory, null, null );

            // loop over and process all configured artifacts
            for (final ArtifactItem artifactItem : artifactItems)
            {
                getLog().info(
                    "attempting to resolve external artifact: "
                        + artifactItem.toString());

                //
                // CREATE MAVEN ARTIFACT
                //
                Artifact artifact = createArtifact(artifactItem);

                // determine if the artifact is already installed in an
                // existing Maven repository
                // Boolean artifactAlreadyInstalled =
                // getLocalRepoFile(artifact).exists();
                boolean artifactResolved = resolveArtifactItem(artifact);

                // only proceed with this artifact if it is not already
                // installed or it is configured to be forced.
                if (!artifactResolved || artifactItem.getForce() || force)
                {

                    if (artifactItem.getForce())
                    {
                        getLog().debug(
                            "this artifact is flagged as a FORCED download: "
                                + artifactItem.toString());
                    }

                    //
                    // DOWNLOAD FILE FROM URL
                    //
                    if (artifactItem.getDownloadUrl() != null)
                    {
                        URL downloadUrl = new URL(artifactItem.getDownloadUrl());
                        
                        // create a temporary download file
                        File tempDownloadFile = File.createTempFile(
                            artifactItem.getLocalFile(), "." + getExtension(downloadUrl));

                        getLog().info(
                            "downloading artifact from URL: "
                                + artifactItem.getDownloadUrl());
                        getLog().debug(
                            "downloading artifact to temporary file: "
                                + tempDownloadFile.getCanonicalPath());

                        // download file from URL
                        FileUtils.copyURLToFile(downloadUrl, tempDownloadFile);

                        // verify file checksum (if a checksum was defined);
                        // 'MojoFailureException' exception will be thrown if
                        // verification fails
                        verifyArtifactItemChecksum(artifactItem,
                            tempDownloadFile);

                        // now that the file has been successfully downloaded
                        // and the checksum verification
                        // has passed (if required), lets copy the temporary
                        // file to the staging location
                        final File artifactFile = getFullyQualifiedArtifactFilePath(artifactItem);

                        // if this artifact is not configured to extract a file,
                        // then
                        // simply copy the downloaded file to the target
                        // artifact file
                        if (!artifactItem.hasExtractFile())
                        {
                            FileUtils.copyFile(tempDownloadFile, artifactFile);
                            getLog().info(
                                "copied downloaded artifact file to "
                                    + "staging path: "
                                    + artifactFile.getCanonicalPath());
                        }

                        // if this artifact is configured to extract a file,
                        // then
                        // extract the file from the downloaded ZIP file to the
                        // target artifact file
                        else
                        {
                            getLog().info(
                                "extracting target file from downloaded "
                                    + "compressed file: "
                                    + artifactItem.getExtractFile());

                            UnArchiver unarchiver;
                            try
                            {
                                unarchiver = archiverManager.getUnArchiver(tempDownloadFile);
                            }
                            catch (NoSuchArchiverException e)
                            {
                                throw new MojoExecutionException( "Archive type, no unarchiver available for it", e);
                            }
                            
                            // ensure the path exists to write the file to
                            File parentDirectory = artifactFile.getParentFile();
                            if (parentDirectory != null
                                && !parentDirectory.exists())
                            {
                                artifactFile.getParentFile().mkdirs();
                            }

                            File tempOutputDir = FileUtils.createTempFile(tempDownloadFile.getName(), ".dir", null);
                            tempOutputDir.mkdirs();

                            unarchiver.setSourceFile(tempDownloadFile);
                            unarchiver.setDestDirectory(tempOutputDir);
                            unarchiver.extract();//will extract nothing, the file selector will do the trick
                            
                            File extractedFile = new File(tempOutputDir, artifactItem.getExtractFile());

                            // if a zip entry was not found, then throw a Mojo
                            // exception

                            if (extractedFile.isFile())
                            {
                                FileUtils.copyFile(extractedFile, artifactFile);
                            }
                            else if (extractedFile.isDirectory()
                                    && artifactItem.isRepack())
                            {
                                Archiver archiver = archiverManager.getArchiver(artifactFile);
                                archiver.setDestFile(artifactFile);
                                archiver.addDirectory(extractedFile);
                                archiver.createArchive();
                            }
                            else
                            {
                                // checksum verification failed, throw error
                                throw new MojoFailureException(
                                    "Could not find target artifact file to "
                                        + "extract from downloaded resouce: "
                                        + "\r\n   groupId      : "
                                        + artifact.getGroupId()
                                        + "\r\n   artifactId   : "
                                        + artifact.getArtifactId()
                                        + "\r\n   version      : "
                                        + artifact.getVersion()
                                        + "\r\n   extractFile  : "
                                        + artifactItem.getExtractFile()
                                        + "\r\n   download URL : "
                                        + artifactItem.getDownloadUrl());
                            }
                            

                            getLog().info(
                                "extracted target file to staging path: "
                                    + artifactFile.getCanonicalPath());
                        }

                        // now we are done with the temporary file so lets
                        // delete it
                        tempDownloadFile.delete();
                        getLog().debug(
                            "deleting temporary download file: "
                                + tempDownloadFile.getCanonicalPath());

                        // update the artifact items local file property
                        artifactItem.setLocalFile(artifactFile
                            .getCanonicalPath());

                        getLog().info(
                            "external artifact downloaded and staged: "
                                + artifactItem.toString());

                        // if the acquired artifact listed in the project
                        // artifacts collection
                        // if(projectArtifacts.contains(artifact))
                        // {
                        // getLog().info("FOUND ARTIFACT IN PROJECT: " +
                        // artifact.toString());
                        // }
                    }
                }
                else
                {
                    getLog().info(
                        "external artifact resolved in existing repository; "
                            + "no download needed: " + artifactItem.toString());
                }
            }

            getLog().info("finished resolving all external dependencies");

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

    private String getExtension(URL downloadUrl)
    {
        String path = downloadUrl.getPath();
        if(path.endsWith(".tar.gz")) {
            return "tar.gz";
        }
        if(path.endsWith(".tar.bz2")) {
            return "tar.bz2";
        }
        return FileUtils.getExtension(path);
    }

    /**
     * resolve the artifact in local or remote repository
     * 
     * @param artifactItem
     * @param artifact
     * @return
     * @throws MojoFailureException
     */
    protected boolean resolveArtifactItem(Artifact artifact)
        throws MojoFailureException
    {
        // determine if the artifact is already installed in an existing Maven
        // repository
        // Boolean artifactAlreadyInstalled =
        // getLocalRepoFile(artifact).exists();
        boolean artifactResolved = false;
        try
        {
            artifactResolver.resolve(artifact, remoteRepositories,
                localRepository);
            artifactResolved = true;
        }
        catch (ArtifactResolutionException e)
        {
            // REV 0.5-SNAPSHOT; 2011-04-30; RRS
            //
            // AS OF MAVEN V3, THIS EXCEPTION IS GETTING THROWN WHEN 
            // AN ATIFACT CANNOT BE RESOLVED IN THE LOCA REPOSITORY,
            // THUS CAUSING THE MAVEN BUILD TO FAIL AND NOT PERFORM
            // THE EXTERNAL DEPENDENCY DOWNLOAD.
            //
            // checksum verification failed, throw error
//            throw new MojoFailureException(
//                "ArtifactResolutionException encountered while "
//                    + "attempting to resolve artifact: "
//                    + "\r\n   groupId    : " + artifact.getGroupId()
//                    + "\r\n   artifactId : " + artifact.getArtifactId()
//                    + "\r\n   version    : " + artifact.getVersion());
            
            artifactResolved = false;            
        }
        catch (ArtifactNotFoundException e)
        {
            artifactResolved = false;
        }

        return artifactResolved;
    }
}
