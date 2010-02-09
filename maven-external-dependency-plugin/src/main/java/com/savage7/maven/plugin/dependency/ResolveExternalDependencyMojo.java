package com.savage7.maven.plugin.dependency;

import java.io.File;
import java.net.URL;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;


 
/**
 * Download/Acquire external Maven artifacts,   
 * copy to staging directory
 *
 * @goal resolve-external
 * 
 * @author <a href="mailto:robert@savage7.com">Robert Savage</a>
 * @see  http://code.google.com/p/maven-external-dependency-plugin/
 * @version 0.1
 * @category Maven Plugin
 */
public class ResolveExternalDependencyMojo extends AbstractExternalDependencyMojo
{
   /**
    * @parameter expression="${localRepository}"
    * @required
    * @readonly
    */
    protected ArtifactRepository localRepository;
	
    public void execute() throws MojoExecutionException, MojoFailureException  
    {
        try
        {
            // update base configuration parameters
            // (not sure why this is needed, but doesn't see to work otherwise?)
            super.localRepository = this.localRepository;

            getLog().debug("RESOLVING EXTERNAL DEPENDENCIES - <START>");

            // loop over and process all configured artifacts 
            for(ArtifactItem artifactItem : artifactItems)
            {
                getLog().info("RESOLVING ARTIFACT FOR DOWNLOAD: " + artifactItem.toString());
                
                //
                // CREATE MAVEN ARTIFACT
                //
                Artifact artifact = createArtifact(artifactItem);

                // determine if the artifact is already installed in the local Maven repository
                Boolean artifactAlreadyInstalled = getLocalRepoFile(artifact).exists();
                
                // only proceed with this artifact if it is not already 
                // installed or it is configured to be forced.
                if(!artifactAlreadyInstalled || 
                    artifactItem.getForce() ||
                    force)
                {
                	
                    if(artifactItem.getForce())
                    {
                        getLog().debug("FORCING ARTIFACT DOWNLOAD: " + artifactItem.toString());
                    }
                    
                    //
                    // DOWNLOAD FILE FROM URL
                    //
                    if(artifactItem.getDownloadUrl() != null)
                    {
                    	File downloadFile = getFullyQualifiedArtifactFilePath(artifactItem);

                    	getLog().info("DOWNLOADING ARTIFACT FROM: " + artifactItem.getDownloadUrl());
                        getLog().info("DOWNLOADING ARTIFACT TO: " + downloadFile.getCanonicalPath());
                        
                        // download file from URL 
                        FileUtils.copyURLToFile(new URL(artifactItem.getDownloadUrl()), downloadFile);

                        getLog().debug("ARTIFACT FILE DOWNLOADED SUCCESSFULLY.");
                    }
                }
                else
                {
                    getLog().debug("ARTIFACT ALREADY EXISTS IN LOCAL REPO; NO DOWNLOAD NEEDED " + artifactItem.toString());
                }
            }

            getLog().debug("RESOLVE EXTERNAL DEPENDENCIES - <END>'");
            
        } 
        catch (Exception e) 
        {
            getLog().error(e);
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }      
}
