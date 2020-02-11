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
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.observers.Debug;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
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
     * @component
     */
    protected ArtifactResolver artifactResolver;

    /**
     * List of Remote Repositories used by the resolver
     * 
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    @SuppressWarnings("rawtypes")
    protected java.util.List remoteRepositories;

    /**
     * @component
     * @readonly
     */
    protected ArchiverManager archiverManager;

    /** 
     * @component 
     * @required 
     * @readonly 
     */ 
    private WagonManager wagonManager; 

    /** 
     * @component 
     * @required 
     * @readonly 
     */ 
    private MavenSettingsBuilder mavenSettingsBuilder; 
    
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

            Map<String, File> cachedDownloads = new HashMap<String, File>();

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

                // now that the file has been successfully downloaded
                // and the checksum verification
                // has passed (if required), lets copy the temporary
                // file to the staging location
                final File artifactFile = getFullyQualifiedArtifactFilePath(artifactItem);
                
                // only proceed with this artifact if it is not already
                // installed or it is configured to be forced.
                if (!artifactResolved || (artifactItem.getForce() && !artifactFile.exists()) || (force && !artifactFile.exists()) )
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
                        final File tempDownloadFile;
                        if (cachedDownloads.containsKey(artifactItem.getDownloadUrl())) {
                            tempDownloadFile = cachedDownloads.get(artifactItem.getDownloadUrl());
                            getLog().info(
                                "Artifact already downloaded from URL: "
                                    + artifactItem.getDownloadUrl());
                            getLog().debug(
                                "Using cached download: "
                                    + tempDownloadFile.getCanonicalPath());
                        } else {
                            // create a temporary download file
                            tempDownloadFile = File.createTempFile(
                            artifactItem.getLocalFile(), "." + getExtension(artifactItem.getDownloadUrl()));

                            getLog().info(
                                "downloading artifact from URL: "
                                    + artifactItem.getDownloadUrl());
                            getLog().debug(
                                "downloading artifact to temporary file: "
                                    + tempDownloadFile.getCanonicalPath());

                            // download file from URL
                            //FileUtils.copyURLToFile(downloadUrl, tempDownloadFile);
                            
                            //vharseko@openam.org.ru
                            if (!new File(artifactItem.getDownloadUrl()).exists()) {
                            	URL downloadUrl = new URL(artifactItem.getDownloadUrl());
	                            String endPointUrl = downloadUrl.getProtocol() + "://"+ downloadUrl.getAuthority();
	                            Repository repository = new Repository("additonal-configs", endPointUrl);
	                            Wagon wagon = wagonManager.getWagon(downloadUrl.getProtocol());
	                            if (getLog().isDebugEnabled())
	                            {
	                                Debug debug = new Debug();
	                                wagon.addSessionListener(debug);
	                                wagon.addTransferListener(debug);
	                            }
	                            wagon.setTimeout(artifactItem.getTimeout());
	                            Settings settings = mavenSettingsBuilder.buildSettings();
	                            ProxyInfo proxyInfo = null;
	                            if (settings != null&& settings.getActiveProxy() != null)
	                            {
	                                Proxy settingsProxy = settings.getActiveProxy();
	                                proxyInfo = new ProxyInfo();
	                                proxyInfo.setHost(settingsProxy.getHost());
	                                proxyInfo.setType(settingsProxy.getProtocol());
	                                proxyInfo.setPort(settingsProxy.getPort());
	                                proxyInfo.setNonProxyHosts(settingsProxy.getNonProxyHosts());
	                                proxyInfo.setUserName(settingsProxy.getUsername());
	                                proxyInfo.setPassword(settingsProxy.getPassword());
	                            }
	
	                            if (proxyInfo != null)
	                                wagon.connect(repository, wagonManager.getAuthenticationInfo(repository.getId()),proxyInfo);
	                            else
	                                wagon.connect(repository, wagonManager.getAuthenticationInfo(repository.getId()));
	                            
	                            wagon.get(downloadUrl.getPath().substring(1), tempDownloadFile);
                            }else {
                            	FileUtils.copyFile(new File(artifactItem.getDownloadUrl()), tempDownloadFile);
                            }
                            	
                            getLog().debug("caching temporary file for later");
                            cachedDownloads.put(artifactItem.getDownloadUrl(), tempDownloadFile);
                        }

                        // verify file checksum (if a checksum was defined);
                        // 'MojoFailureException' exception will be thrown if
                        // verification fails
                        //
                        // Note: In theory, there might be conflicting checksums
                        // configured for the same artifact; checksum
                        // verification may thus be done several times for a
                        // cached download.
                        verifyArtifactItemChecksum(artifactItem,
                            tempDownloadFile);


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

                            File tempOutputDir = FileUtils.createTempFile(tempDownloadFile.getName(), ".dir", null);
                            tempOutputDir.mkdirs();
                            File extractedFile = new File(tempOutputDir, artifactItem.getExtractFile());
                            
                            UnArchiver unarchiver;
                            try
                            {
                                try
                                {
                                    unarchiver = archiverManager.getUnArchiver(tempDownloadFile);
                                }
                                catch (NoSuchArchiverException e){
                                    if (tempDownloadFile.getName().endsWith(".gz")){
                                        unarchiver = archiverManager.getUnArchiver("gzip");
                                        unarchiver.setDestFile(extractedFile);
                                    }else
                                        throw e;
                                }
                            }
                            catch (NoSuchArchiverException e)
                            {
                                throw new MojoExecutionException( "Archive type, no unarchiver available for it", e);
                            }
                            
                            // ensure the path exists to write the file to
                            File parentDirectory = artifactFile.getParentFile();
                            if (parentDirectory != null && !parentDirectory.exists())
                            {
                                artifactFile.getParentFile().mkdirs();
                            }

                            unarchiver.setSourceFile(tempDownloadFile);
                            if (unarchiver.getDestFile()==null)
                                unarchiver.setDestDirectory(tempOutputDir);
                            unarchiver.extract();//will extract nothing, the file selector will do the trick
                            
                            

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

            // now we are done with the temporary files so lets
            // delete them
            for (File tempDownloadFile : cachedDownloads.values()) {
                tempDownloadFile.delete();
                getLog().debug(
                    "deleting temporary download file: "
                        + tempDownloadFile.getCanonicalPath());
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

    private String getExtension(String downloadUrl)
    {
        String path = downloadUrl;
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
            // AN ATIFACT CANNOT BE RESOLVED IN THE LOCAL REPOSITORY,
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
